package org.jeecg.modules.miniofile.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.MinioUtil;
import org.jeecg.modules.miniofile.entity.MinioFolder;
import org.jeecg.modules.miniofile.entity.MinioFile;
import org.jeecg.modules.miniofile.mapper.MinioFileMapper;
import org.jeecg.modules.miniofile.service.IMinioFolderPermissionService;
import org.jeecg.modules.miniofile.service.IMinioFolderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Api(tags = "MinIO文件夹管理")
@RestController
@RequestMapping("/minio/folder")
public class MinioFolderController extends JeecgController<MinioFolder, IMinioFolderService> {

    private static final Pattern BUCKET_NAME_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9\\-]{1,61}[a-z0-9]$");

    @Autowired
    private IMinioFolderService minioFolderService;

    @Autowired
    private MinioFileMapper minioFileMapper;

    @Autowired
    private IMinioFolderPermissionService folderPermissionService;

    @AutoLog(value = "MinIO文件夹-分页列表")
    @ApiOperation(value = "分页列表", notes = "MinIO文件夹-分页列表")
    @GetMapping(value = "/list")
    public Result<?> queryPageList(MinioFolder minioFolder,
                                   @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                   HttpServletRequest req) {
        QueryWrapper<MinioFolder> queryWrapper = new QueryWrapper<>();
        if (minioFolder.getDisplayName() != null && StringUtils.isNotBlank(minioFolder.getDisplayName())) {
            queryWrapper.like("display_name", minioFolder.getDisplayName());
        }
        if (minioFolder.getFolderName() != null && StringUtils.isNotBlank(minioFolder.getFolderName())) {
            queryWrapper.like("folder_name", minioFolder.getFolderName());
        }
        // 权限过滤：非admin用户只能看到被授权的文件夹
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        if (loginUser != null && !"admin".equals(loginUser.getUsername())) {
            List<String> allowedFolderIds = folderPermissionService.getFolderIdsByUserId(loginUser.getId());
            if (allowedFolderIds == null || allowedFolderIds.isEmpty()) {
                Page<MinioFolder> emptyPage = new Page<>(pageNo, pageSize);
                emptyPage.setTotal(0);
                emptyPage.setRecords(new ArrayList<>());
                return Result.OK(emptyPage);
            }
            queryWrapper.in("id", allowedFolderIds);
        }
        Page<MinioFolder> page = new Page<>(pageNo, pageSize);
        Page<MinioFolder> pageList = minioFolderService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    @AutoLog(value = "MinIO文件夹-添加")
    @ApiOperation(value = "添加", notes = "MinIO文件夹-添加")
    @PostMapping(value = "/add")
    public Result<?> add(@RequestBody MinioFolder minioFolder) {
        // 校验显示名称
        if (StringUtils.isBlank(minioFolder.getDisplayName())) {
            return Result.error("显示名称不能为空");
        }
        LambdaQueryWrapper<MinioFolder> displayNameWrapper = new LambdaQueryWrapper<>();
        displayNameWrapper.eq(MinioFolder::getDisplayName, minioFolder.getDisplayName());
        if (minioFolderService.count(displayNameWrapper) > 0) {
            return Result.error("显示名称已存在");
        }
        // 校验文件夹名称（Bucket名）
        if (StringUtils.isBlank(minioFolder.getFolderName())) {
            return Result.error("文件夹名称不能为空");
        }
        String folderName = minioFolder.getFolderName().toLowerCase();
        if (!BUCKET_NAME_PATTERN.matcher(folderName).matches()) {
            return Result.error("文件夹名称不符合S3标准：3-63位小写字母、数字、短横线，且不能以短横线开头或结尾");
        }
        LambdaQueryWrapper<MinioFolder> folderNameWrapper = new LambdaQueryWrapper<>();
        folderNameWrapper.eq(MinioFolder::getFolderName, folderName);
        if (minioFolderService.count(folderNameWrapper) > 0) {
            return Result.error("文件夹名称已存在");
        }
        minioFolder.setFolderName(folderName);
        minioFolderService.save(minioFolder);
        // 同步创建 MinIO Bucket
        try {
            MinioUtil.createBucket(minioFolder.getFolderName());
        } catch (Exception e) {
            // Bucket创建失败，回滚数据库记录
            minioFolderService.removeById(minioFolder.getId());
            log.error("创建Bucket失败: {}", e.getMessage(), e);
            return Result.error("创建Bucket失败: " + e.getMessage());
        }
        return Result.OK("添加成功");
    }

    @AutoLog(value = "MinIO文件夹-编辑")
    @ApiOperation(value = "编辑", notes = "MinIO文件夹-编辑（文件夹名称不可修改）")
    @PutMapping(value = "/edit")
    public Result<?> edit(@RequestBody MinioFolder minioFolder) {
        // 校验显示名称唯一性
        if (StringUtils.isNotBlank(minioFolder.getDisplayName())) {
            LambdaQueryWrapper<MinioFolder> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MinioFolder::getDisplayName, minioFolder.getDisplayName());
            wrapper.ne(MinioFolder::getId, minioFolder.getId());
            if (minioFolderService.count(wrapper) > 0) {
                return Result.error("显示名称已存在");
            }
        }
        // 清空folderName，防止前端篡改
        minioFolder.setFolderName(null);
        minioFolderService.updateById(minioFolder);
        return Result.OK("编辑成功");
    }

    @AutoLog(value = "MinIO文件夹-删除")
    @ApiOperation(value = "删除", notes = "MinIO文件夹-删除")
    @DeleteMapping(value = "/delete")
    public Result<?> delete(@RequestParam(name = "id") String id) {
        MinioFolder folder = minioFolderService.getById(id);
        if (folder == null) {
            return Result.error("文件夹不存在");
        }
        // 校验文件夹下是否有文件
        LambdaQueryWrapper<MinioFile> fileWrapper = new LambdaQueryWrapper<>();
        fileWrapper.eq(MinioFile::getFolderId, id);
        long fileCount = minioFileMapper.selectCount(fileWrapper);
        if (fileCount > 0) {
            return Result.error("文件夹下存在文件，无法删除");
        }
        minioFolderService.removeById(id);
        // 同步删除 MinIO Bucket
        try {
            MinioUtil.removeBucket(folder.getFolderName());
        } catch (Exception e) {
            log.warn("删除Bucket失败（数据库记录已删除）: {}", e.getMessage(), e);
        }
        return Result.OK("删除成功");
    }

    @AutoLog(value = "MinIO文件夹-批量删除")
    @ApiOperation(value = "批量删除", notes = "MinIO文件夹-批量删除")
    @DeleteMapping(value = "/deleteBatch")
    public Result<?> deleteBatch(@RequestParam(name = "ids") String ids) {
        this.delete(Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功");
    }

    @ApiOperation(value = "通过ID查询", notes = "通过ID查询")
    @GetMapping(value = "/queryById")
    public Result<?> queryById(@RequestParam(name = "id") String id) {
        MinioFolder folder = minioFolderService.getById(id);
        return Result.OK(folder);
    }

    private void delete(java.util.List<String> idList) {
        for (String id : idList) {
            MinioFolder folder = minioFolderService.getById(id);
            if (folder != null) {
                LambdaQueryWrapper<MinioFile> fileWrapper = new LambdaQueryWrapper<>();
                fileWrapper.eq(MinioFile::getFolderId, id);
                long fileCount = minioFileMapper.selectCount(fileWrapper);
                if (fileCount == 0) {
                    minioFolderService.removeById(id);
                    try {
                        MinioUtil.removeBucket(folder.getFolderName());
                    } catch (Exception e) {
                        log.warn("删除Bucket失败: {}", e.getMessage(), e);
                    }
                }
            }
        }
    }
}
