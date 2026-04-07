package org.jeecg.modules.miniofile.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.modules.miniofile.entity.MinioFolderPermission;
import org.jeecg.modules.miniofile.service.IMinioFolderPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Api(tags = "MinIO文件夹权限")
@RestController
@RequestMapping("/minio/folderPermission")
public class MinioFolderPermissionController extends JeecgController<MinioFolderPermission, IMinioFolderPermissionService> {

    @Autowired
    private IMinioFolderPermissionService folderPermissionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @ApiOperation(value = "所有用户列表", notes = "获取所有用户（id, username, realname）用于权限配置下拉")
    @GetMapping(value = "/allUsers")
    public Result<?> allUsers() {
        String sql = "SELECT id, username, realname FROM sys_user WHERE del_flag = 0 ORDER BY realname";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        return Result.OK(list);
    }

    @ApiOperation(value = "查询用户已授权文件夹", notes = "根据userId查询其拥有的文件夹权限列表")
    @GetMapping(value = "/list")
    public Result<?> list(@RequestParam(name = "userId") String userId) {
        LambdaQueryWrapper<MinioFolderPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MinioFolderPermission::getUserId, userId);
        List<MinioFolderPermission> list = folderPermissionService.list(wrapper);
        return Result.OK(list);
    }

    @AutoLog(value = "MinIO文件夹权限-保存")
    @ApiOperation(value = "保存权限", notes = "保存用户的文件夹权限（先删后插）")
    @PostMapping(value = "/save")
    public Result<?> save(@RequestBody Map<String, Object> params) {
        String userId = (String) params.get("userId");
        @SuppressWarnings("unchecked")
        List<String> folderIds = (List<String>) params.get("folderIds");
        if (userId == null || userId.isEmpty()) {
            return Result.error("用户ID不能为空");
        }
        folderPermissionService.saveUserFolders(userId, folderIds != null ? folderIds : new ArrayList<>());
        return Result.OK("保存成功");
    }

    @ApiOperation(value = "查询用户密级", notes = "获取指定用户的密级等级")
    @GetMapping(value = "/userLevel")
    public Result<?> userLevel(@RequestParam(name = "userId") String userId) {
        String sql = "SELECT classification_level FROM sys_user WHERE id = ?";
        String level = null;
        try {
            level = jdbcTemplate.queryForObject(sql, String.class, userId);
        } catch (Exception e) {
            // ignore
        }
        Map<String, String> result = new HashMap<>();
        result.put("classificationLevel", level != null ? level : "公开");
        return Result.OK(result);
    }

    @AutoLog(value = "MinIO文件夹权限-保存用户密级")
    @ApiOperation(value = "保存用户密级", notes = "更新用户的密级标识")
    @PostMapping(value = "/saveUserLevel")
    public Result<?> saveUserLevel(@RequestBody Map<String, Object> params) {
        String userId = (String) params.get("userId");
        String classificationLevel = (String) params.get("classificationLevel");
        if (userId == null || classificationLevel == null) {
            return Result.error("参数不完整");
        }
        jdbcTemplate.update("UPDATE sys_user SET classification_level = ? WHERE id = ?", classificationLevel, userId);
        return Result.OK("保存成功");
    }
}
