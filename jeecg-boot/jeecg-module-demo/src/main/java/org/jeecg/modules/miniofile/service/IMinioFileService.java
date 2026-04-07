package org.jeecg.modules.miniofile.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.miniofile.entity.MinioFile;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

public interface IMinioFileService extends IService<MinioFile> {

    MinioFile uploadFile(MultipartFile file, String folderId, MinioFile minioFile) throws Exception;

    int uploadZipFile(MultipartFile file, String folderId, MinioFile minioFile) throws Exception;

    void deleteFile(String id) throws Exception;

    java.io.InputStream downloadFile(String id, String bucketName);

    List<Map<String, Object>> countByDocType(String folderId);

    List<Map<String, Object>> countByTag(String folderId, String docType, String tagName);
}
