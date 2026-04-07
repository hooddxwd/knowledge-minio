package org.jeecg.modules.miniofile.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.miniofile.entity.MinioFolderPermission;
import org.jeecg.modules.miniofile.mapper.MinioFolderPermissionMapper;
import org.jeecg.modules.miniofile.service.IMinioFolderPermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MinioFolderPermissionServiceImpl extends ServiceImpl<MinioFolderPermissionMapper, MinioFolderPermission> implements IMinioFolderPermissionService {

    @Override
    public List<String> getFolderIdsByUserId(String userId) {
        LambdaQueryWrapper<MinioFolderPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MinioFolderPermission::getUserId, userId);
        wrapper.select(MinioFolderPermission::getFolderId);
        List<MinioFolderPermission> list = this.list(wrapper);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream().map(MinioFolderPermission::getFolderId).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveUserFolders(String userId, List<String> folderIds) {
        // 删除旧权限
        LambdaQueryWrapper<MinioFolderPermission> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(MinioFolderPermission::getUserId, userId);
        this.remove(deleteWrapper);
        // 插入新权限
        if (folderIds != null && !folderIds.isEmpty()) {
            for (String folderId : folderIds) {
                MinioFolderPermission perm = new MinioFolderPermission();
                perm.setUserId(userId);
                perm.setFolderId(folderId);
                this.save(perm);
            }
        }
    }
}
