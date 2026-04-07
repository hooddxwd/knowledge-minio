package org.jeecg.modules.miniofile.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.miniofile.entity.MinioFile;
import org.jeecg.modules.miniofile.entity.MinioFolder;
import org.jeecg.modules.miniofile.mapper.MinioFolderMapper;
import org.jeecg.modules.miniofile.service.IMinioFileService;
import org.jeecg.modules.miniofile.service.IMinioFolderPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.*;

@Slf4j
@Api(tags = "MinIO文件管理")
@RestController
@RequestMapping("/minio/file")
public class MinioFileController extends JeecgController<MinioFile, IMinioFileService> {

    /** 密级等级（数字越大等级越高，最多支持到内部级别） */
    private static final LinkedHashMap<String, Integer> LEVEL_ORDER = new LinkedHashMap<>();
    static {
        LEVEL_ORDER.put("公开", 1);
        LEVEL_ORDER.put("内部", 2);
    }

    @Autowired
    private IMinioFileService minioFileService;

    @Autowired
    private MinioFolderMapper minioFolderMapper;

    @Autowired
    private IMinioFolderPermissionService folderPermissionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AutoLog(value = "MinIO文件-分页列表")
    @ApiOperation(value = "分页列表", notes = "按folderId和docType分页查询")
    @GetMapping(value = "/list")
    public Result<?> queryPageList(MinioFile minioFile,
                                   @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                   @RequestParam(name = "pageSize", defaultValue = "12") Integer pageSize,
                                   @RequestParam(name = "tagField", required = false) String tagField,
                                   @RequestParam(name = "tagValue", required = false) String tagValue,
                                   @RequestParam(name = "sortBy", defaultValue = "time_desc") String sortBy,
                                   HttpServletRequest req) {
        QueryWrapper<MinioFile> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("folder_id", minioFile.getFolderId());
        if (minioFile.getDocType() != null && !minioFile.getDocType().isEmpty()) {
            queryWrapper.eq("doc_type", minioFile.getDocType());
        }
        if (tagField != null && !tagField.isEmpty() && tagValue != null && !tagValue.isEmpty()) {
            // camelCase 转 snake_case（stdLevel → std_level）
            StringBuilder col = new StringBuilder();
            for (int i = 0; i < tagField.length(); i++) {
                char c = tagField.charAt(i);
                if (Character.isUpperCase(c)) col.append('_').append(Character.toLowerCase(c));
                else col.append(c);
            }
            queryWrapper.eq(col.toString(), tagValue);
        }
        if (minioFile.getFileName() != null && !minioFile.getFileName().isEmpty()) {
            queryWrapper.like("file_name", minioFile.getFileName());
        }
        // 排序
        switch (sortBy) {
            case "time_asc": queryWrapper.orderByAsc("create_time"); break;
            case "name_asc": queryWrapper.orderByAsc("file_name"); break;
            case "size_desc": queryWrapper.orderByDesc("file_size"); break;
            default: queryWrapper.orderByDesc("create_time"); break;
        }
        // 权限 + 密级过滤（admin跳过）
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        if (loginUser != null && !"admin".equals(loginUser.getUsername())) {
            // 文件夹权限过滤
            List<String> allowedFolderIds = folderPermissionService.getFolderIdsByUserId(loginUser.getId());
            if (allowedFolderIds.isEmpty()) {
                Page<MinioFile> emptyPage = new Page<>(pageNo, pageSize);
                emptyPage.setTotal(0);
                emptyPage.setRecords(new ArrayList<>());
                return Result.OK(emptyPage);
            }
            queryWrapper.in("folder_id", allowedFolderIds);
            // 密级过滤
            int userLevel = getUserClassificationLevel(loginUser.getId());
            List<String> allowedLevels = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : LEVEL_ORDER.entrySet()) {
                if (entry.getValue() <= userLevel) {
                    allowedLevels.add(entry.getKey());
                }
            }
            queryWrapper.in("classification_level", allowedLevels);
        }
        Page<MinioFile> page = new Page<>(pageNo, pageSize);
        Page<MinioFile> pageList = minioFileService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    @ApiOperation(value = "通过ID查询", notes = "文件详情")
    @GetMapping(value = "/queryById")
    public Result<?> queryById(@RequestParam(name = "id") String id) {
        MinioFile file = minioFileService.getById(id);
        return Result.OK(file);
    }

    @AutoLog(value = "MinIO文件-上传")
    @ApiOperation(value = "单文件上传", notes = "上传文件到指定文件夹")
    @PostMapping(value = "/upload")
    public Result<?> upload(@RequestParam("file") MultipartFile file,
                            @RequestParam("folderId") String folderId,
                            @RequestParam(value = "fileName", required = false) String fileName,
                            @RequestParam(value = "classificationLevel", required = false) String classificationLevel,
                            @RequestParam(value = "summary", required = false) String summary,
                            @RequestParam(value = "author", required = false) String author,
                            @RequestParam(value = "techSystem", required = false) String techSystem,
                            @RequestParam(value = "docType", required = false) String docType,
                            @RequestParam(value = "docSource", required = false) String docSource,
                            @RequestParam(value = "year", required = false) String year,
                            @RequestParam(value = "reportTaskSource", required = false) String reportTaskSource,
                            @RequestParam(value = "reportModel", required = false) String reportModel,
                            @RequestParam(value = "reportMajor", required = false) String reportMajor,
                            @RequestParam(value = "reportCategory", required = false) String reportCategory,
                            @RequestParam(value = "stdLevel", required = false) String stdLevel,
                            @RequestParam(value = "stdField", required = false) String stdField,
                            @RequestParam(value = "stdCivilMilitary", required = false) String stdCivilMilitary,
                            @RequestParam(value = "stdAircraftType", required = false) String stdAircraftType,
                            @RequestParam(value = "stdType", required = false) String stdType,
                            @RequestParam(value = "newsType", required = false) String newsType,
                            @RequestParam(value = "newsMajor", required = false) String newsMajor,
                            @RequestParam(value = "ruleDomain", required = false) String ruleDomain,
                            @RequestParam(value = "ruleLevel", required = false) String ruleLevel,
                            @RequestParam(value = "ruleDept", required = false) String ruleDept,
                            @RequestParam(value = "journalDb", required = false) String journalDb,
                            @RequestParam(value = "journalSubject", required = false) String journalSubject,
                            @RequestParam(value = "journalLevel", required = false) String journalLevel,
                            @RequestParam(value = "journalIndustry", required = false) String journalIndustry,
                            @RequestParam(value = "docFromUnit", required = false) String docFromUnit,
                            @RequestParam(value = "docHostUnit", required = false) String docHostUnit,
                            @RequestParam(value = "docDrafter", required = false) String docDrafter,
                            @RequestParam(value = "docDraftDept", required = false) String docDraftDept,
                            @RequestParam(value = "ipPatentInventor", required = false) String ipPatentInventor,
                            @RequestParam(value = "ipPatentApplicant", required = false) String ipPatentApplicant,
                            @RequestParam(value = "ipAchievementDeclarant", required = false) String ipAchievementDeclarant,
                            @RequestParam(value = "ipAchievementCompleter", required = false) String ipAchievementCompleter,
                            @RequestParam(value = "ipSoftDevList", required = false) String ipSoftDevList,
                            @RequestParam(value = "ipSoftRepresentative", required = false) String ipSoftRepresentative) {
        try {
            // 密级校验
            if (classificationLevel != null && !classificationLevel.isEmpty() && !LEVEL_ORDER.containsKey(classificationLevel)) {
                return Result.error("无效的密级: " + classificationLevel);
            }
            if (classificationLevel == null || classificationLevel.isEmpty()) {
                classificationLevel = "公开";
            }
            MinioFile minioFile = new MinioFile();
            minioFile.setFileName(fileName);
            minioFile.setClassificationLevel(classificationLevel);
            minioFile.setSummary(summary);
            minioFile.setAuthor(author);
            minioFile.setTechSystem(techSystem);
            minioFile.setDocType(docType);
            minioFile.setDocSource(docSource);
            minioFile.setYear(year);
            minioFile.setReportTaskSource(reportTaskSource);
            minioFile.setReportModel(reportModel);
            minioFile.setReportMajor(reportMajor);
            minioFile.setReportCategory(reportCategory);
            minioFile.setStdLevel(stdLevel);
            minioFile.setStdField(stdField);
            minioFile.setStdCivilMilitary(stdCivilMilitary);
            minioFile.setStdAircraftType(stdAircraftType);
            minioFile.setStdType(stdType);
            minioFile.setNewsType(newsType);
            minioFile.setNewsMajor(newsMajor);
            minioFile.setRuleDomain(ruleDomain);
            minioFile.setRuleLevel(ruleLevel);
            minioFile.setRuleDept(ruleDept);
            minioFile.setJournalDb(journalDb);
            minioFile.setJournalSubject(journalSubject);
            minioFile.setJournalLevel(journalLevel);
            minioFile.setJournalIndustry(journalIndustry);
            minioFile.setDocFromUnit(docFromUnit);
            minioFile.setDocHostUnit(docHostUnit);
            minioFile.setDocDrafter(docDrafter);
            minioFile.setDocDraftDept(docDraftDept);
            minioFile.setIpPatentInventor(ipPatentInventor);
            minioFile.setIpPatentApplicant(ipPatentApplicant);
            minioFile.setIpAchievementDeclarant(ipAchievementDeclarant);
            minioFile.setIpAchievementCompleter(ipAchievementCompleter);
            minioFile.setIpSoftDevList(ipSoftDevList);
            minioFile.setIpSoftRepresentative(ipSoftRepresentative);
            MinioFile saved = minioFileService.uploadFile(file, folderId, minioFile);
            return Result.OK("上传成功", saved);
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    @AutoLog(value = "MinIO文件-压缩包上传")
    @ApiOperation(value = "压缩包上传", notes = "上传ZIP压缩包，自动解压入库")
    @PostMapping(value = "/uploadZip")
    public Result<?> uploadZip(@RequestParam("file") MultipartFile file,
                               @RequestParam("folderId") String folderId,
                               @RequestParam(value = "docType", required = false) String docType,
                               @RequestParam(value = "classificationLevel", required = false) String classificationLevel,
                               @RequestParam(value = "summary", required = false) String summary,
                               @RequestParam(value = "author", required = false) String author,
                               @RequestParam(value = "techSystem", required = false) String techSystem,
                               @RequestParam(value = "docSource", required = false) String docSource,
                               @RequestParam(value = "year", required = false) String year,
                               @RequestParam(value = "reportTaskSource", required = false) String reportTaskSource,
                               @RequestParam(value = "reportModel", required = false) String reportModel,
                               @RequestParam(value = "reportMajor", required = false) String reportMajor,
                               @RequestParam(value = "reportCategory", required = false) String reportCategory,
                               @RequestParam(value = "stdLevel", required = false) String stdLevel,
                               @RequestParam(value = "stdField", required = false) String stdField,
                               @RequestParam(value = "stdCivilMilitary", required = false) String stdCivilMilitary,
                               @RequestParam(value = "stdAircraftType", required = false) String stdAircraftType,
                               @RequestParam(value = "stdType", required = false) String stdType,
                               @RequestParam(value = "newsType", required = false) String newsType,
                               @RequestParam(value = "newsMajor", required = false) String newsMajor,
                               @RequestParam(value = "ruleDomain", required = false) String ruleDomain,
                               @RequestParam(value = "ruleLevel", required = false) String ruleLevel,
                               @RequestParam(value = "ruleDept", required = false) String ruleDept,
                               @RequestParam(value = "journalDb", required = false) String journalDb,
                               @RequestParam(value = "journalSubject", required = false) String journalSubject,
                               @RequestParam(value = "journalLevel", required = false) String journalLevel,
                               @RequestParam(value = "journalIndustry", required = false) String journalIndustry,
                               @RequestParam(value = "docFromUnit", required = false) String docFromUnit,
                               @RequestParam(value = "docHostUnit", required = false) String docHostUnit,
                               @RequestParam(value = "docDrafter", required = false) String docDrafter,
                               @RequestParam(value = "docDraftDept", required = false) String docDraftDept,
                               @RequestParam(value = "ipPatentInventor", required = false) String ipPatentInventor,
                               @RequestParam(value = "ipPatentApplicant", required = false) String ipPatentApplicant,
                               @RequestParam(value = "ipAchievementDeclarant", required = false) String ipAchievementDeclarant,
                               @RequestParam(value = "ipAchievementCompleter", required = false) String ipAchievementCompleter,
                               @RequestParam(value = "ipSoftDevList", required = false) String ipSoftDevList,
                               @RequestParam(value = "ipSoftRepresentative", required = false) String ipSoftRepresentative) {
        try {
            // 密级校验
            if (classificationLevel != null && !classificationLevel.isEmpty() && !LEVEL_ORDER.containsKey(classificationLevel)) {
                return Result.error("无效的密级: " + classificationLevel);
            }
            if (classificationLevel == null || classificationLevel.isEmpty()) {
                classificationLevel = "公开";
            }
            MinioFile minioFile = new MinioFile();
            minioFile.setDocType(docType);
            minioFile.setClassificationLevel(classificationLevel);
            minioFile.setSummary(summary);
            minioFile.setAuthor(author);
            minioFile.setTechSystem(techSystem);
            minioFile.setDocSource(docSource);
            minioFile.setYear(year);
            minioFile.setReportTaskSource(reportTaskSource);
            minioFile.setReportModel(reportModel);
            minioFile.setReportMajor(reportMajor);
            minioFile.setReportCategory(reportCategory);
            minioFile.setStdLevel(stdLevel);
            minioFile.setStdField(stdField);
            minioFile.setStdCivilMilitary(stdCivilMilitary);
            minioFile.setStdAircraftType(stdAircraftType);
            minioFile.setStdType(stdType);
            minioFile.setNewsType(newsType);
            minioFile.setNewsMajor(newsMajor);
            minioFile.setRuleDomain(ruleDomain);
            minioFile.setRuleLevel(ruleLevel);
            minioFile.setRuleDept(ruleDept);
            minioFile.setJournalDb(journalDb);
            minioFile.setJournalSubject(journalSubject);
            minioFile.setJournalLevel(journalLevel);
            minioFile.setJournalIndustry(journalIndustry);
            minioFile.setDocFromUnit(docFromUnit);
            minioFile.setDocHostUnit(docHostUnit);
            minioFile.setDocDrafter(docDrafter);
            minioFile.setDocDraftDept(docDraftDept);
            minioFile.setIpPatentInventor(ipPatentInventor);
            minioFile.setIpPatentApplicant(ipPatentApplicant);
            minioFile.setIpAchievementDeclarant(ipAchievementDeclarant);
            minioFile.setIpAchievementCompleter(ipAchievementCompleter);
            minioFile.setIpSoftDevList(ipSoftDevList);
            minioFile.setIpSoftRepresentative(ipSoftRepresentative);
            int count = minioFileService.uploadZipFile(file, folderId, minioFile);
            return Result.OK("压缩包解压上传成功，共入库 " + count + " 个文件");
        } catch (Exception e) {
            log.error("压缩包上传失败: {}", e.getMessage(), e);
            return Result.error("压缩包上传失败: " + e.getMessage());
        }
    }

    @AutoLog(value = "MinIO文件-编辑")
    @ApiOperation(value = "编辑文件属性", notes = "编辑文件属性和标签字段")
    @PutMapping(value = "/edit")
    public Result<?> edit(@RequestBody MinioFile minioFile) {
        minioFileService.updateById(minioFile);
        return Result.OK("编辑成功");
    }

    @AutoLog(value = "MinIO文件-删除")
    @ApiOperation(value = "删除文件", notes = "删除文件同时删除MinIO对象")
    @DeleteMapping(value = "/delete")
    public Result<?> delete(@RequestParam(name = "id") String id) {
        try {
            minioFileService.deleteFile(id);
            return Result.OK("删除成功");
        } catch (Exception e) {
            log.error("文件删除失败: {}", e.getMessage(), e);
            return Result.error("文件删除失败: " + e.getMessage());
        }
    }

    @ApiOperation(value = "下载文件", notes = "从MinIO获取文件流返回")
    @GetMapping(value = "/download/{id}")
    public void download(@PathVariable("id") String id, HttpServletResponse response) {
        try {
            MinioFile minioFile = minioFileService.getById(id);
            if (minioFile == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            // 权限校验：文件夹权限 + 密级
            LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
            if (loginUser != null && !"admin".equals(loginUser.getUsername())) {
                List<String> allowedFolderIds = folderPermissionService.getFolderIdsByUserId(loginUser.getId());
                if (allowedFolderIds == null || !allowedFolderIds.contains(minioFile.getFolderId())) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                int userLevel = getUserClassificationLevel(loginUser.getId());
                int fileLevel = LEVEL_ORDER.getOrDefault(
                    minioFile.getClassificationLevel() != null ? minioFile.getClassificationLevel() : "公开", 1);
                if (fileLevel > userLevel) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
            }

            MinioFolder folder = minioFolderMapper.selectById(minioFile.getFolderId());
            if (folder == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            InputStream inputStream = minioFileService.downloadFile(id, folder.getFolderName());
            if (inputStream == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            String encodedFileName = URLEncoder.encode(minioFile.getFileName(), "UTF-8").replaceAll("\\+", "%20");
            response.setContentType(minioFile.getFileType() != null ? minioFile.getFileType() : "application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
            response.setContentLengthLong(minioFile.getFileSize() != null ? minioFile.getFileSize() : 0);
            OutputStream outputStream = response.getOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, len);
            }
            outputStream.flush();
            inputStream.close();
        } catch (Exception e) {
            log.error("文件下载失败: {}", e.getMessage(), e);
        }
    }

    @ApiOperation(value = "按类型统计", notes = "按doc_type统计文件数量（用于标签页角标）")
    @GetMapping(value = "/countByType")
    public Result<?> countByType(@RequestParam(name = "folderId") String folderId) {
        // 构建带权限过滤的查询
        QueryWrapper<MinioFile> wrapper = new QueryWrapper<>();
        wrapper.eq("folder_id", folderId);
        applyPermissionFilter(wrapper);
        wrapper.select("doc_type AS docType", "COUNT(*) AS count").groupBy("doc_type");
        List<Map<String, Object>> list = minioFileService.listMaps(wrapper);
        return Result.OK(list);
    }

    @ApiOperation(value = "标签统计", notes = "按标签字段统计每个标签值的文件数量（用于左侧筛选树）")
    @GetMapping(value = "/tagStats")
    public Result<?> tagStats(@RequestParam(name = "folderId") String folderId,
                              @RequestParam(name = "docType") String docType,
                              @RequestParam(name = "tagName") String tagName) {
        // camelCase 转 snake_case
        StringBuilder col = new StringBuilder();
        for (int i = 0; i < tagName.length(); i++) {
            char c = tagName.charAt(i);
            if (Character.isUpperCase(c)) col.append('_').append(Character.toLowerCase(c));
            else col.append(c);
        }
        String columnName = col.toString();
        QueryWrapper<MinioFile> wrapper = new QueryWrapper<>();
        wrapper.eq("folder_id", folderId);
        wrapper.eq("doc_type", docType);
        wrapper.isNotNull(columnName).ne(columnName, "");
        applyPermissionFilter(wrapper);
        wrapper.select(columnName + " AS tagValue", "COUNT(*) AS count").groupBy(columnName);
        List<Map<String, Object>> list = minioFileService.listMaps(wrapper);
        return Result.OK(list);
    }

    /**
     * 对查询应用权限过滤（文件夹权限 + 密级），admin 跳过
     */
    private void applyPermissionFilter(QueryWrapper<MinioFile> wrapper) {
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        if (loginUser == null || "admin".equals(loginUser.getUsername())) return;
        // 文件夹权限过滤
        List<String> allowedFolderIds = folderPermissionService.getFolderIdsByUserId(loginUser.getId());
        if (allowedFolderIds.isEmpty()) {
            // 无权限：加一个不可能满足的条件使结果为空
            wrapper.eq("id", "__NONE__");
            return;
        }
        wrapper.in("folder_id", allowedFolderIds);
        // 密级过滤
        int userLevel = getUserClassificationLevel(loginUser.getId());
        List<String> allowedLevels = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : LEVEL_ORDER.entrySet()) {
            if (entry.getValue() <= userLevel) allowedLevels.add(entry.getKey());
        }
        wrapper.in("classification_level", allowedLevels);
    }

    /**
     * 获取用户密级等级（从sys_user表查询）
     */
    private int getUserClassificationLevel(String userId) {
        try {
            String level = jdbcTemplate.queryForObject(
                "SELECT classification_level FROM sys_user WHERE id = ?", String.class, userId);
            return LEVEL_ORDER.getOrDefault(level != null ? level : "公开", 1);
        } catch (Exception e) {
            log.warn("获取用户密级失败，默认为公开: {}", e.getMessage());
            return 1;
        }
    }
}
