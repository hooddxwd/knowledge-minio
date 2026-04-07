package org.jeecg.modules.miniofile.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.miniofile.entity.MinioFolder;
import org.jeecg.modules.miniofile.mapper.MinioFolderMapper;
import org.jeecg.modules.miniofile.service.IMinioFolderService;
import org.springframework.stereotype.Service;

@Service
public class MinioFolderServiceImpl extends ServiceImpl<MinioFolderMapper, MinioFolder> implements IMinioFolderService {
}
