package org.jeecg.modules.miniofile.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.MinioUtil;
import org.jeecg.modules.miniofile.entity.MinioFile;
import org.jeecg.modules.miniofile.entity.MinioFolder;
import org.jeecg.modules.miniofile.mapper.MinioFileMapper;
import org.jeecg.modules.miniofile.mapper.MinioFolderMapper;
import org.jeecg.modules.miniofile.service.IMinioFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
public class MinioFileServiceImpl extends ServiceImpl<MinioFileMapper, MinioFile> implements IMinioFileService {

    @Autowired
    private MinioFileMapper minioFileMapper;

    @Autowired
    private MinioFolderMapper minioFolderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MinioFile uploadFile(MultipartFile file, String folderId, MinioFile minioFile) throws Exception {
        MinioFolder folder = minioFolderMapper.selectById(folderId);
        if (folder == null) {
            throw new RuntimeException("文件夹不存在");
        }
        String fileUrl = MinioUtil.upload(file, "", folder.getFolderName());
        String minioUrl = MinioUtil.getMinioUrl();
        String prefix = minioUrl + folder.getFolderName() + "/";
        String objectName = fileUrl.substring(prefix.length());

        minioFile.setId(null);
        minioFile.setFolderId(folderId);
        if (minioFile.getClassificationLevel() == null || minioFile.getClassificationLevel().isEmpty()) {
            minioFile.setClassificationLevel("公开");
        }
        minioFile.setFileSize(file.getSize());
        minioFile.setFileType(file.getContentType());
        minioFile.setMinioObjectName(objectName);
        if (minioFile.getFileName() == null || minioFile.getFileName().isEmpty()) {
            minioFile.setFileName(file.getOriginalFilename());
        }
        minioFileMapper.insert(minioFile);
        return minioFile;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int uploadZipFile(MultipartFile file, String folderId, MinioFile minioFile) throws Exception {
        MinioFolder folder = minioFolderMapper.selectById(folderId);
        if (folder == null) {
            throw new RuntimeException("文件夹不存在");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".zip")) {
            throw new RuntimeException("仅支持ZIP格式压缩包");
        }

        File tempDir = new File(System.getProperty("java.io.tmpdir"), "minio_upload_" + System.currentTimeMillis());
        tempDir.mkdirs();
        int count = 0;
        try {
            try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory() || entry.getName().startsWith("__MACOSX") || entry.getName().startsWith(".")) {
                        continue;
                    }
                    if (entry.getSize() > 100 * 1024 * 1024) {
                        log.warn("跳过过大文件: {}", entry.getName());
                        continue;
                    }
                    File outFile = new File(tempDir, new File(entry.getName()).getName());
                    if (outFile.exists()) {
                        String baseName = outFile.getName();
                        String name = baseName.contains(".") ?
                                baseName.substring(0, baseName.lastIndexOf(".")) + "_" + System.currentTimeMillis() + baseName.substring(baseName.lastIndexOf(".")) :
                                baseName + "_" + System.currentTimeMillis();
                        outFile = new File(tempDir, name);
                    }
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    String objectName = outFile.getName();
                    try (FileInputStream fis = new FileInputStream(outFile)) {
                        MinioUtil.uploadStream(fis, objectName, folder.getFolderName());
                    }
                    MinioFile fileRecord = new MinioFile();
                    org.springframework.beans.BeanUtils.copyProperties(minioFile, fileRecord, "id", "folderId", "fileName", "fileSize", "fileType", "minioObjectName", "createTime", "updateTime", "createBy", "updateBy");
                    fileRecord.setFolderId(folderId);
                    fileRecord.setFileName(entry.getName());
                    fileRecord.setClassificationLevel(minioFile.getClassificationLevel() != null ? minioFile.getClassificationLevel() : "公开");
                    fileRecord.setFileSize(outFile.length());
                    fileRecord.setFileType(getContentType(outFile.getName()));
                    fileRecord.setMinioObjectName(objectName);
                    minioFileMapper.insert(fileRecord);
                    count++;
                    outFile.delete();
                }
            }
        } finally {
            tempDir.delete();
        }
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(String id) throws Exception {
        MinioFile minioFile = minioFileMapper.selectById(id);
        if (minioFile == null) {
            throw new RuntimeException("文件不存在");
        }
        minioFileMapper.deleteById(id);
        MinioFolder folder = minioFolderMapper.selectById(minioFile.getFolderId());
        if (folder != null) {
            try {
                MinioUtil.removeObject(folder.getFolderName(), minioFile.getMinioObjectName());
            } catch (Exception e) {
                log.error("删除MinIO文件失败: {}", e.getMessage(), e);
                throw new RuntimeException("删除MinIO文件失败: " + e.getMessage());
            }
        }
    }

    @Override
    public java.io.InputStream downloadFile(String id, String bucketName) {
        MinioFile minioFile = minioFileMapper.selectById(id);
        if (minioFile == null) {
            throw new RuntimeException("文件不存在");
        }
        return MinioUtil.getMinioFile(bucketName, minioFile.getMinioObjectName());
    }

    @Override
    public List<Map<String, Object>> countByDocType(String folderId) {
        return minioFileMapper.countByDocType(folderId);
    }

    @Override
    public List<Map<String, Object>> countByTag(String folderId, String docType, String tagName) {
        // camelCase 转 snake_case（reportModel → report_model）
        String columnName = camelToSnake(tagName);
        return minioFileMapper.countByTag(folderId, docType, columnName);
    }

    private String camelToSnake(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) return camelCase;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append('_').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String getContentType(String fileName) {
        if (fileName == null) return "application/octet-stream";
        String ext = fileName.toLowerCase();
        if (ext.endsWith(".pdf")) return "application/pdf";
        if (ext.endsWith(".doc")) return "application/msword";
        if (ext.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (ext.endsWith(".xls")) return "application/vnd.ms-excel";
        if (ext.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (ext.endsWith(".ppt")) return "application/vnd.ms-powerpoint";
        if (ext.endsWith(".pptx")) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        if (ext.endsWith(".jpg") || ext.endsWith(".jpeg")) return "image/jpeg";
        if (ext.endsWith(".png")) return "image/png";
        if (ext.endsWith(".gif")) return "image/gif";
        if (ext.endsWith(".txt")) return "text/plain";
        if (ext.endsWith(".zip")) return "application/zip";
        if (ext.endsWith(".rar")) return "application/x-rar-compressed";
        return "application/octet-stream";
    }
}
