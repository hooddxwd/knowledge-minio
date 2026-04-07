package org.jeecg.modules.miniofile.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.miniofile.entity.MinioFolderPermission;

import java.util.List;

public interface IMinioFolderPermissionService extends IService<MinioFolderPermission> {

    /**
     * 获取用户授权的文件夹ID列表
     */
    List<String> getFolderIdsByUserId(String userId);

    /**
     * 保存用户的文件夹权限（先删后插）
     */
    void saveUserFolders(String userId, List<String> folderIds);
}
