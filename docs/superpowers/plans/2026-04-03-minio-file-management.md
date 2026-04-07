# MinIO 文件管理系统实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 基于 JeecgBoot Vue2 项目，构建 MinIO 存储的文件管理和标签功能模块。

**Architecture:** 后端在 `jeecg-module-demo` 下新建 `miniofile` 包，复用 `MinioUtil` 和 MinIO 配置。两张数据库表（minio_folder、minio_file），文件夹映射 MinIO Bucket。前端使用 Vue2 + Ant Design Vue，`JeecgListMixin` 实现标准 CRUD，自定义组件实现标签页、标签筛选、卡片列表。

**Tech Stack:** Spring Boot 2.6.6, MyBatis Plus, MinIO Java SDK, Vue 2, Ant Design Vue 1.x, Axios

---

## 文件结构总览

### 后端 - 新建文件
- `jeecg-boot/jeecg-module-demo/src/main/java/org/jeecg/modules/miniofile/entity/MinioFolder.java`
- `jeecg-boot/jeecg-module-demo/src/main/java/org/jeecg/modules/miniofile/entity/MinioFile.java`
- `jeecg-boot/jeecg-module-demo/src/main/java/org/jeecg/modules/miniofile/mapper/MinioFolderMapper.java`
- `jeecg-boot/jeecg-module-demo/src/main/java/org/jeecg/modules/miniofile/mapper/MinioFileMapper.java`
- `jeecg-boot/jeecg-module-demo/src/main/java/org/jeecg/modules/miniofile/mapper/xml/MinioFolderMapper.xml`
- `jeecg-boot/jeecg-module-demo/src/main/java/org/jeecg/modules/miniofile/mapper/xml/MinioFileMapper.xml`
- `jeecg-boot/jeecg-module-demo/src/main/java/org/jeecg/modules/miniofile/service/IMinioFolderService.java`
- `jeecg-boot/jeecg-module-demo/src/main/java/org/jeecg/modules/miniofile/service/IMinioFileService.java`
- `jeecg-boot/jeecg-module-demo/src/main/java/org/jeecg/modules/miniofile/service/impl/MinioFolderServiceImpl.java`
- `jeecg-boot/jeecg-module-demo/src/main/java/org/jeecg/modules/miniofile/service/impl/MinioFileServiceImpl.java`
- `jeecg-boot/jeecg-module-demo/src/main/java/org/jeecg/modules/miniofile/controller/MinioFolderController.java`
- `jeecg-boot/jeecg-module-demo/src/main/java/org/jeecg/modules/miniofile/controller/MinioFileController.java`

### 后端 - 修改文件
- `jeecg-boot/jeecg-boot-base-core/src/main/java/org/jeecg/common/util/MinioUtil.java` — 新增 `createBucket`、`removeBucket`、`uploadStream` 方法
- `jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/resources/application-dev.yml` — 更新 MinIO 配置

### SQL
- `docs/superpowers/sql/minio-file-management.sql`

### 前端 - 新建文件
- `ant-design-vue-jeecg/src/views/miniofile/MinioFolderList.vue`
- `ant-design-vue-jeecg/src/views/miniofile/MinioFileList.vue`
- `ant-design-vue-jeecg/src/views/miniofile/DocTypeTagConfig.js`

### 前端 - 修改文件
- `ant-design-vue-jeecg/src/api/api.js` — 新增 minio API 方法
- `ant-design-vue-jeecg/src/config/router.config.js` — 新增 miniofile 路由

---

## Task 1: MinIO 配置更新 & MinioUtil 增强

**Files:**
- Modify: `jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/resources/application-dev.yml:201-206`
- Modify: `jeecg-boot/jeecg-boot-base-core/src/main/java/org/jeecg/common/util/MinioUtil.java`

- [ ] **Step 1: 更新 application-dev.yml 中的 MinIO 配置**

将 `jeecg.minio` 配置改为目标地址：

```yaml
  # minio文件上传
  minio:
    minio_url: http://129.226.204.202:10055
    minio_name: admin
    minio_pass: admin123
    bucketName: jeecg-boot
```

修改位置：`application-dev.yml` 第 201-206 行，将 `minio_url`、`minio_name`、`minio_pass`、`bucketName` 替换为上述值。

- [ ] **Step 2: 增大 multipart 上传限制（支持压缩包上传）**

在 `application-dev.yml` 第 24-26 行，将 `spring.servlet.multipart` 的限制从 10MB 提高到 200MB：

```yaml
  servlet:
    multipart:
      max-file-size: 200MB
      max-request-size: 200MB
```

- [ ] **Step 3: 在 MinioUtil 中新增 `createBucket` 方法**

在 `MinioUtil.java` 的 `removeObject` 方法之后（约第 154 行后）添加：

```java
/**
 * 创建存储桶
 * @param bucketName 存储桶名称
 */
public static void createBucket(String bucketName) {
    try {
        initMinio(minioUrl, minioName, minioPass);
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            log.info("Created bucket: {}", bucketName);
        }
    } catch (Exception e) {
        log.error("创建Bucket失败: " + e.getMessage(), e);
        throw new RuntimeException("创建Bucket失败: " + e.getMessage());
    }
}
```

- [ ] **Step 4: 在 MinioUtil 中新增 `removeBucket` 方法**

紧跟 `createBucket` 方法之后添加：

```java
/**
 * 删除存储桶
 * @param bucketName 存储桶名称
 */
public static void removeBucket(String bucketName) {
    try {
        initMinio(minioUrl, minioName, minioPass);
        minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
        log.info("Removed bucket: {}", bucketName);
    } catch (Exception e) {
        log.error("删除Bucket失败: " + e.getMessage(), e);
        throw new RuntimeException("删除Bucket失败: " + e.getMessage());
    }
}
```

需要新增 import：

```java
import io.minio.RemoveBucketArgs;
```

- [ ] **Step 5: 在 MinioUtil 中新增 `uploadStream` 方法（支持指定 objectName 和 bucket）**

在 `uploadStream` 方法中，用于 ZIP 解压后的文件上传（从 InputStream 上传并指定 objectName 和 bucket）：

```java
/**
 * 上传文件流到指定bucket的指定路径
 * @param stream 文件流
 * @param objectName 对象名（含路径）
 * @param customBucket bucket名称
 * @return 文件完整URL
 */
public static String uploadStream(InputStream stream, String objectName, String customBucket) throws Exception {
    String newBucket = bucketName;
    if (oConvertUtils.isNotEmpty(customBucket)) {
        newBucket = customBucket;
    }
    initMinio(minioUrl, minioName, minioPass);
    if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(newBucket).build())) {
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(newBucket).build());
        log.info("create a new bucket: {}", newBucket);
    }
    // 确保objectName不以/开头
    if (objectName.startsWith(SymbolConstant.SINGLE_SLASH)) {
        objectName = objectName.substring(1);
    }
    PutObjectArgs objectArgs = PutObjectArgs.builder().object(objectName)
            .bucket(newBucket)
            .contentType("application/octet-stream")
            .stream(stream, stream.available(), -1).build();
    minioClient.putObject(objectArgs);
    stream.close();
    return minioUrl + newBucket + "/" + objectName;
}
```

- [ ] **Step 6: 验证 MinioUtil 编译通过**

Run: `cd jeecg-boot && mvn compile -pl jeecg-boot-base-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add jeecg-boot/jeecg-boot-base-core/src/main/java/org/jeecg/common/util/MinioUtil.java \
       jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/resources/application-dev.yml
git commit -m "feat(minio): update config and add createBucket/removeBucket/uploadStream to MinioUtil"
```

---

## Task 2: 数据库表结构

**Files:**
- Create: `docs/superpowers/sql/minio-file-management.sql`

- [ ] **Step 1: 创建 SQL 脚本**

```sql
-- MinIO 文件管理系统数据库脚本

-- 文件夹表
CREATE TABLE IF NOT EXISTS `minio_folder` (
  `id` varchar(36) NOT NULL COMMENT '主键',
  `folder_name` varchar(200) NOT NULL COMMENT '文件夹名称（同时作为MinIO Bucket名）',
  `description` varchar(500) DEFAULT NULL COMMENT '文件夹描述',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_folder_name` (`folder_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MinIO文件夹';

-- 文件表
CREATE TABLE IF NOT EXISTS `minio_file` (
  `id` varchar(36) NOT NULL COMMENT '主键',
  `folder_id` varchar(36) NOT NULL COMMENT '关联文件夹ID',
  `file_name` varchar(500) NOT NULL COMMENT '文档名称',
  `classification_level` varchar(50) DEFAULT '公开' COMMENT '文档密级',
  `summary` text COMMENT '摘要',
  `author` varchar(200) DEFAULT NULL COMMENT '作者',
  `tech_system` varchar(200) DEFAULT NULL COMMENT '技术体系',
  `doc_type` varchar(100) DEFAULT NULL COMMENT '文档类型',
  `doc_source` varchar(200) DEFAULT NULL COMMENT '文档来源',
  `year` varchar(10) DEFAULT NULL COMMENT '年度',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小（字节）',
  `file_type` varchar(100) DEFAULT NULL COMMENT 'MIME类型',
  `minio_object_name` varchar(500) NOT NULL COMMENT 'MinIO中的对象名',
  -- 标签字段（平铺）
  `report_task_source` varchar(200) DEFAULT NULL COMMENT '科技报告-任务来源/渠道',
  `report_model` varchar(200) DEFAULT NULL COMMENT '科技报告-型号',
  `report_major` varchar(200) DEFAULT NULL COMMENT '科技报告-专业',
  `report_category` varchar(200) DEFAULT NULL COMMENT '科技报告-类别',
  `std_level` varchar(200) DEFAULT NULL COMMENT '标准规范-标准层次',
  `std_field` varchar(200) DEFAULT NULL COMMENT '标准规范-标准领域',
  `std_civil_military` varchar(200) DEFAULT NULL COMMENT '标准规范-军民属性',
  `std_aircraft_type` varchar(200) DEFAULT NULL COMMENT '标准规范-适用航空器类型',
  `std_type` varchar(200) DEFAULT NULL COMMENT '标准规范-标准类型',
  `news_type` varchar(200) DEFAULT NULL COMMENT '新闻资讯-新闻类型',
  `news_major` varchar(200) DEFAULT NULL COMMENT '新闻资讯-新闻专业',
  `rule_domain` varchar(200) DEFAULT NULL COMMENT '规章制度-业务域',
  `rule_level` varchar(200) DEFAULT NULL COMMENT '规章制度-制度等级',
  `rule_dept` varchar(200) DEFAULT NULL COMMENT '规章制度-主责部门',
  `journal_db` varchar(200) DEFAULT NULL COMMENT '期刊文献-来源数据库',
  `journal_subject` varchar(200) DEFAULT NULL COMMENT '期刊文献-学科',
  `journal_level` varchar(200) DEFAULT NULL COMMENT '期刊文献-论文级别',
  `journal_industry` varchar(200) DEFAULT NULL COMMENT '期刊文献-行业分类',
  `doc_from_unit` varchar(200) DEFAULT NULL COMMENT '行政公文-来文单位',
  `doc_host_unit` varchar(200) DEFAULT NULL COMMENT '行政公文-主办单位',
  `doc_drafter` varchar(200) DEFAULT NULL COMMENT '行政公文-拟稿人',
  `doc_draft_dept` varchar(200) DEFAULT NULL COMMENT '行政公文-拟稿部门',
  `ip_patent_inventor` varchar(200) DEFAULT NULL COMMENT '知识产权-专利发明人',
  `ip_patent_applicant` varchar(200) DEFAULT NULL COMMENT '知识产权-专利申请人',
  `ip_achievement_declarant` varchar(200) DEFAULT NULL COMMENT '知识产权-所级申报人',
  `ip_achievement_completer` varchar(200) DEFAULT NULL COMMENT '知识产权-所级主要完成人',
  `ip_soft_dev_list` varchar(200) DEFAULT NULL COMMENT '知识产权-软著开发人列表',
  `ip_soft_representative` varchar(200) DEFAULT NULL COMMENT '知识产权-著作权人代表',
  -- 审计字段
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人/上传人',
  `create_time` datetime DEFAULT NULL COMMENT '上传时间',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_folder_id` (`folder_id`),
  KEY `idx_doc_type` (`doc_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MinIO文件';
```

- [ ] **Step 2: 执行 SQL 创建表**

在数据库中执行上述脚本，确认两张表创建成功。

- [ ] **Step 3: Commit**

```bash
git add docs/superpowers/sql/minio-file-management.sql
git commit -m "feat(minio): add database schema for minio_folder and minio_file tables"
```

---

## Task 3: MinioFolder 后端（Entity + Mapper + Service + Controller）

**参考模式：** 参考 `org.jeecg.modules.demo.test` 包中的 JeecgDemo 及其 Controller/Service/Mapper 结构。

**Files:**
- Create: `jeecg-boot/jeecg-module-demo/src/main/java/org/jeecg/modules/miniofile/entity/MinioFolder.java`
- Create: `jeecg-boot/jeecg-module-demo/src/main/java/org/jeecg/modules/miniofile/mapper/MinioFolderMapper.java`
- Create: `jeecg-boot/jeecg-module-demo/src/main/java/org/jeecg/modules/miniofile/mapper/xml/MinioFolderMapper.xml`
- Create: `jeecg-boot/jeecg-module-demo/src/main/java/org/jeecg/modules/miniofile/service/IMinioFolderService.java`
- Create: `jeecg-boot/jeecg-module-demo/src/main/java/org/jeecg/modules/miniofile/service/impl/MinioFolderServiceImpl.java`
- Create: `jeecg-boot/jeecg-module-demo/src/main/java/org/jeecg/modules/miniofile/controller/MinioFolderController.java`

- [ ] **Step 1: 创建 MinioFolder 实体类**

```java
package org.jeecg.modules.miniofile.entity;

import java.io.Serializable;
import org.jeecg.common.system.base.entity.JeecgEntity;
import org.jeecgframework.poi.excel.annotation.Excel;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "minio_folder对象", description = "MinIO文件夹")
@TableName("minio_folder")
public class MinioFolder extends JeecgEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Excel(name = "文件夹名称", width = 25)
    @ApiModelProperty(value = "文件夹名称")
    private String folderName;

    @Excel(name = "文件夹描述", width = 30)
    @ApiModelProperty(value = "文件夹描述")
    private String description;
}
```

- [ ] **Step 2: 创建 MinioFolderMapper 接口**

```java
package org.jeecg.modules.miniofile.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.jeecg.modules.miniofile.entity.MinioFolder;

public interface MinioFolderMapper extends BaseMapper<MinioFolder> {
}
```

- [ ] **Step 3: 创建 MinioFolderMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="org.jeecg.modules.miniofile.mapper.MinioFolderMapper">
</mapper>
```

- [ ] **Step 4: 创建 IMinioFolderService 接口**

```java
package org.jeecg.modules.miniofile.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.miniofile.entity.MinioFolder;

public interface IMinioFolderService extends IService<MinioFolder> {
}
```

- [ ] **Step 5: 创建 MinioFolderServiceImpl 实现类**

```java
package org.jeecg.modules.miniofile.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.miniofile.entity.MinioFolder;
import org.jeecg.modules.miniofile.mapper.MinioFolderMapper;
import org.jeecg.modules.miniofile.service.IMinioFolderService;
import org.springframework.stereotype.Service;

@Service
public class MinioFolderServiceImpl extends ServiceImpl<MinioFolderMapper, MinioFolder> implements IMinioFolderService {
}
```

- [ ] **Step 6: 创建 MinioFolderController**

包含 CRUD 接口，文件夹创建时同步创建 MinIO Bucket，删除时同步删除 Bucket（需校验文件夹内无文件）。

```java
package org.jeecg.modules.miniofile.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.common.util.MinioUtil;
import org.jeecg.modules.miniofile.entity.MinioFolder;
import org.jeecg.modules.miniofile.entity.MinioFile;
import org.jeecg.modules.miniofile.mapper.MinioFileMapper;
import org.jeecg.modules.miniofile.service.IMinioFolderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

@Slf4j
@Api(tags = "MinIO文件夹管理")
@RestController
@RequestMapping("/minio/folder")
public class MinioFolderController extends JeecgController<MinioFolder, IMinioFolderService> {

    @Autowired
    private IMinioFolderService minioFolderService;

    @Autowired
    private MinioFileMapper minioFileMapper;

    @AutoLog(value = "MinIO文件夹-分页列表")
    @ApiOperation(value = "分页列表", notes = "MinIO文件夹-分页列表")
    @GetMapping(value = "/list")
    public Result<?> queryPageList(MinioFolder minioFolder,
                                   @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                   HttpServletRequest req) {
        QueryWrapper<MinioFolder> queryWrapper = new QueryWrapper<>();
        if (minioFolder.getFolderName() != null) {
            queryWrapper.like("folder_name", minioFolder.getFolderName());
        }
        Page<MinioFolder> page = new Page<>(pageNo, pageSize);
        Page<MinioFolder> pageList = minioFolderService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    @AutoLog(value = "MinIO文件夹-添加")
    @ApiOperation(value = "添加", notes = "MinIO文件夹-添加")
    @PostMapping(value = "/add")
    public Result<?> add(@RequestBody MinioFolder minioFolder) {
        // 校验名称唯一性
        LambdaQueryWrapper<MinioFolder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MinioFolder::getFolderName, minioFolder.getFolderName());
        long count = minioFolderService.count(wrapper);
        if (count > 0) {
            return Result.error("文件夹名称已存在");
        }
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
    @ApiOperation(value = "编辑", notes = "MinIO文件夹-编辑")
    @PutMapping(value = "/edit")
    public Result<?> edit(@RequestBody MinioFolder minioFolder) {
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

    /**
     * 批量删除辅助方法
     */
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
```

- [ ] **Step 7: 验证编译通过**

Run: `cd jeecg-boot && mvn compile -pl jeecg-module-demo -q`
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add jeecg-boot/jeecg-module-demo/src/main/java/org/jeecg/modules/miniofile/
git commit -m "feat(minio): add MinioFolder entity, mapper, service and controller"
```

---

## Task 4: MinioFile 实体 & 数据层（Entity + Mapper）

**Files:**
- Create: `jeecg-boot/jeecg-module-demo/src/main/java/org/jeecg/modules/miniofile/entity/MinioFile.java`
- Create: `jeecg-boot/jeecg-module-demo/src/main/java/org/jeecg/modules/miniofile/mapper/MinioFileMapper.java`
- Create: `jeecg-boot/jeecg-module-demo/src/main/java/org/jeecg/modules/miniofile/mapper/xml/MinioFileMapper.xml`

- [ ] **Step 1: 创建 MinioFile 实体类**

包含全部基础字段和 28 个标签字段，继承 `JeecgEntity` 获得 id/createBy/createTime/updateBy/updateTime 审计字段。

```java
package org.jeecg.modules.miniofile.entity;

import java.io.Serializable;
import org.jeecg.common.system.base.entity.JeecgEntity;
import org.jeecgframework.poi.excel.annotation.Excel;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "minio_file对象", description = "MinIO文件")
@TableName("minio_file")
public class MinioFile extends JeecgEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "文件夹ID")
    private String folderId;

    @Excel(name = "文档名称", width = 30)
    @ApiModelProperty(value = "文档名称")
    private String fileName;

    @Excel(name = "文档密级", width = 15)
    @ApiModelProperty(value = "文档密级")
    private String classificationLevel;

    @ApiModelProperty(value = "摘要")
    private String summary;

    @Excel(name = "作者", width = 20)
    @ApiModelProperty(value = "作者")
    private String author;

    @Excel(name = "技术体系", width = 20)
    @ApiModelProperty(value = "技术体系")
    private String techSystem;

    @Excel(name = "文档类型", width = 15)
    @ApiModelProperty(value = "文档类型")
    private String docType;

    @Excel(name = "文档来源", width = 20)
    @ApiModelProperty(value = "文档来源")
    private String docSource;

    @Excel(name = "年度", width = 10)
    @ApiModelProperty(value = "年度")
    private String year;

    @ApiModelProperty(value = "文件大小（字节）")
    private Long fileSize;

    @ApiModelProperty(value = "MIME类型")
    private String fileType;

    @ApiModelProperty(value = "MinIO对象名")
    private String minioObjectName;

    // ========== 科技报告标签 ==========
    @ApiModelProperty(value = "任务来源/渠道")
    private String reportTaskSource;
    @ApiModelProperty(value = "型号")
    private String reportModel;
    @ApiModelProperty(value = "专业")
    private String reportMajor;
    @ApiModelProperty(value = "类别")
    private String reportCategory;

    // ========== 标准规范标签 ==========
    @ApiModelProperty(value = "标准层次")
    private String stdLevel;
    @ApiModelProperty(value = "标准领域")
    private String stdField;
    @ApiModelProperty(value = "军民属性")
    private String stdCivilMilitary;
    @ApiModelProperty(value = "适用航空器类型")
    private String stdAircraftType;
    @ApiModelProperty(value = "标准类型")
    private String stdType;

    // ========== 新闻资讯标签 ==========
    @ApiModelProperty(value = "新闻类型")
    private String newsType;
    @ApiModelProperty(value = "新闻专业")
    private String newsMajor;

    // ========== 规章制度标签 ==========
    @ApiModelProperty(value = "业务域")
    private String ruleDomain;
    @ApiModelProperty(value = "制度等级")
    private String ruleLevel;
    @ApiModelProperty(value = "主责部门")
    private String ruleDept;

    // ========== 期刊文献标签 ==========
    @ApiModelProperty(value = "来源数据库")
    private String journalDb;
    @ApiModelProperty(value = "学科")
    private String journalSubject;
    @ApiModelProperty(value = "论文级别")
    private String journalLevel;
    @ApiModelProperty(value = "行业分类")
    private String journalIndustry;

    // ========== 行政公文标签 ==========
    @ApiModelProperty(value = "来文单位")
    private String docFromUnit;
    @ApiModelProperty(value = "主办单位")
    private String docHostUnit;
    @ApiModelProperty(value = "拟稿人")
    private String docDrafter;
    @ApiModelProperty(value = "拟稿部门")
    private String docDraftDept;

    // ========== 知识产权标签 ==========
    @ApiModelProperty(value = "专利发明人")
    private String ipPatentInventor;
    @ApiModelProperty(value = "专利申请人")
    private String ipPatentApplicant;
    @ApiModelProperty(value = "所级申报人")
    private String ipAchievementDeclarant;
    @ApiModelProperty(value = "所级主要完成人")
    private String ipAchievementCompleter;
    @ApiModelProperty(value = "软著开发人列表")
    private String ipSoftDevList;
    @ApiModelProperty(value = "著作权人代表")
    private String ipSoftRepresentative;
}
```

- [ ] **Step 2: 创建 MinioFileMapper 接口**

```java
package org.jeecg.modules.miniofile.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.miniofile.entity.MinioFile;
import java.util.List;
import java.util.Map;

public interface MinioFileMapper extends BaseMapper<MinioFile> {

    /**
     * 按doc_type统计文件数量
     */
    @Select("SELECT doc_type AS docType, COUNT(*) AS count FROM minio_file WHERE folder_id = #{folderId} GROUP BY doc_type")
    List<Map<String, Object>> countByDocType(@Param("folderId") String folderId);

    /**
     * 按标签字段统计每个标签值的文件数量
     */
    @Select("SELECT ${tagName} AS tagValue, COUNT(*) AS count FROM minio_file WHERE folder_id = #{folderId} AND doc_type = #{docType} AND ${tagName} IS NOT NULL AND ${tagName} != '' GROUP BY ${tagName}")
    List<Map<String, Object>> countByTag(@Param("folderId") String folderId, @Param("docType") String docType, @Param("tagName") String tagName);
}
```

- [ ] **Step 3: 创建 MinioFileMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="org.jeecg.modules.miniofile.mapper.MinioFileMapper">
</mapper>
```

- [ ] **Step 4: Commit**

```bash
git add jeecg-boot/jeecg-module-demo/src/main/java/org/jeecg/modules/miniofile/entity/MinioFile.java \
       jeecg-boot/jeecg-module-demo/src/main/java/org/jeecg/modules/miniofile/mapper/MinioFileMapper.java \
       jeecg-boot/jeecg-module-demo/src/main/java/org/jeecg/modules/miniofile/mapper/xml/MinioFileMapper.xml
git commit -m "feat(minio): add MinioFile entity and mapper with tag field definitions"
```

---

## Task 5: MinioFile Service 层

**Files:**
- Create: `jeecg-boot/jeecg-module-demo/src/main/java/org/jeecg/modules/miniofile/service/IMinioFileService.java`
- Create: `jeecg-boot/jeecg-module-demo/src/main/java/org/jeecg/modules/miniofile/service/impl/MinioFileServiceImpl.java`

- [ ] **Step 1: 创建 IMinioFileService 接口**

```java
package org.jeecg.modules.miniofile.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.miniofile.entity.MinioFile;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

public interface IMinioFileService extends IService<MinioFile> {

    /**
     * 单文件上传
     * @param file 上传的文件
     * @param folderId 文件夹ID
     * @param minioFile 文件属性（含标签字段）
     * @return 保存后的文件记录
     */
    MinioFile uploadFile(MultipartFile file, String folderId, MinioFile minioFile) throws Exception;

    /**
     * 压缩包上传，自动解压入库
     * @param file ZIP压缩包
     * @param folderId 文件夹ID
     * @param minioFile 统一的标签属性（应用到所有解压文件）
     * @return 解压入库的文件数量
     */
    int uploadZipFile(MultipartFile file, String folderId, MinioFile minioFile) throws Exception;

    /**
     * 删除文件（同时删除MinIO对象）
     */
    void deleteFile(String id) throws Exception;

    /**
     * 获取文件下载流
     * @param id 文件ID
     * @param bucketName MinIO Bucket名
     * @return 文件输入流
     */
    java.io.InputStream downloadFile(String id, String bucketName);

    /**
     * 按doc_type统计文件数量
     */
    List<Map<String, Object>> countByDocType(String folderId);

    /**
     * 按标签字段统计
     */
    List<Map<String, Object>> countByTag(String folderId, String docType, String tagName);
}
```

- [ ] **Step 2: 创建 MinioFileServiceImpl 实现类**

```java
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
        // 校验文件夹存在
        MinioFolder folder = minioFolderMapper.selectById(folderId);
        if (folder == null) {
            throw new RuntimeException("文件夹不存在");
        }

        // 上传到MinIO，获取文件URL
        String fileUrl = MinioUtil.upload(file, "", folder.getFolderName());

        // 从URL中提取objectName: minioUrl + bucket + "/" + objectName
        String minioUrl = MinioUtil.getMinioUrl();
        String prefix = minioUrl + folder.getFolderName() + "/";
        String objectName = fileUrl.substring(prefix.length());

        // 设置文件属性
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

        // 保存到数据库
        minioFileMapper.insert(minioFile);
        return minioFile;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int uploadZipFile(MultipartFile file, String folderId, MinioFile minioFile) throws Exception {
        // 校验文件夹存在
        MinioFolder folder = minioFolderMapper.selectById(folderId);
        if (folder == null) {
            throw new RuntimeException("文件夹不存在");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".zip")) {
            throw new RuntimeException("仅支持ZIP格式压缩包");
        }

        // 创建临时目录
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "minio_upload_" + System.currentTimeMillis());
        tempDir.mkdirs();

        int count = 0;
        try {
            // 解压ZIP文件
            try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    // 跳过目录和隐藏文件
                    if (entry.isDirectory() || entry.getName().startsWith("__MACOSX") || entry.getName().startsWith(".")) {
                        continue;
                    }
                    // 跳过超大文件（单文件超过100MB）
                    if (entry.getSize() > 100 * 1024 * 1024) {
                        log.warn("跳过过大文件: {}", entry.getName());
                        continue;
                    }

                    // 解压到临时目录
                    File outFile = new File(tempDir, new File(entry.getName()).getName());
                    // 避免文件名冲突
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

                    // 上传到MinIO（使用uploadStream，指定objectName和bucket）
                    String objectName = outFile.getName();
                    try (FileInputStream fis = new FileInputStream(outFile)) {
                        MinioUtil.uploadStream(fis, objectName, folder.getFolderName());
                    }

                    // 创建数据库记录
                    MinioFile fileRecord = new MinioFile();
                    // 复制标签属性
                    org.springframework.beans.BeanUtils.copyProperties(minioFile, fileRecord, "id", "folderId", "fileName", "fileSize", "fileType", "minioObjectName", "createTime", "updateTime", "createBy", "updateBy");
                    fileRecord.setFolderId(folderId);
                    fileRecord.setFileName(entry.getName());
                    fileRecord.setClassificationLevel(minioFile.getClassificationLevel() != null ? minioFile.getClassificationLevel() : "公开");
                    fileRecord.setFileSize(outFile.length());
                    String contentType = getContentType(outFile.getName());
                    fileRecord.setFileType(contentType);
                    fileRecord.setMinioObjectName(objectName);
                    minioFileMapper.insert(fileRecord);
                    count++;

                    // 删除临时解压文件
                    outFile.delete();
                }
            }
        } finally {
            // 清理临时目录
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
        // 删除数据库记录
        minioFileMapper.deleteById(id);
        // 删除MinIO对象
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
        return minioFileMapper.countByTag(folderId, docType, tagName);
    }

    /**
     * 根据文件扩展名获取ContentType
     */
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
```

- [ ] **Step 3: 验证编译通过**

Run: `cd jeecg-boot && mvn compile -pl jeecg-module-demo -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add jeecg-boot/jeecg-module-demo/src/main/java/org/jeecg/modules/miniofile/service/
git commit -m "feat(minio): add MinioFile service with upload, zip upload, download and delete logic"
```

---

## Task 6: MinioFile Controller

**Files:**
- Create: `jeecg-boot/jeecg-module-demo/src/main/java/org/jeecg/modules/miniofile/controller/MinioFileController.java`

- [ ] **Step 1: 创建 MinioFileController**

包含全部 11 个接口：list、queryById、upload、uploadZip、edit、delete、download、countByType、tagStats。

```java
package org.jeecg.modules.miniofile.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.modules.miniofile.entity.MinioFile;
import org.jeecg.modules.miniofile.entity.MinioFolder;
import org.jeecg.modules.miniofile.mapper.MinioFolderMapper;
import org.jeecg.modules.miniofile.service.IMinioFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

@Slf4j
@Api(tags = "MinIO文件管理")
@RestController
@RequestMapping("/minio/file")
public class MinioFileController extends JeecgController<MinioFile, IMinioFileService> {

    @Autowired
    private IMinioFileService minioFileService;

    @Autowired
    private MinioFolderMapper minioFolderMapper;

    @AutoLog(value = "MinIO文件-分页列表")
    @ApiOperation(value = "分页列表", notes = "按folderId和docType分页查询")
    @GetMapping(value = "/list")
    public Result<?> queryPageList(MinioFile minioFile,
                                   @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                   @RequestParam(name = "pageSize", defaultValue = "12") Integer pageSize,
                                   @RequestParam(name = "tagField", required = false) String tagField,
                                   @RequestParam(name = "tagValue", required = false) String tagValue,
                                   HttpServletRequest req) {
        QueryWrapper<MinioFile> queryWrapper = new QueryWrapper<>();
        // 按文件夹过滤
        queryWrapper.eq("folder_id", minioFile.getFolderId());
        // 按文档类型过滤
        if (minioFile.getDocType() != null && !minioFile.getDocType().isEmpty()) {
            queryWrapper.eq("doc_type", minioFile.getDocType());
        }
        // 按标签筛选
        if (tagField != null && !tagField.isEmpty() && tagValue != null && !tagValue.isEmpty()) {
            queryWrapper.eq(tagField, tagValue);
        }
        // 按文件名模糊搜索
        if (minioFile.getFileName() != null && !minioFile.getFileName().isEmpty()) {
            queryWrapper.like("file_name", minioFile.getFileName());
        }
        queryWrapper.orderByDesc("create_time");
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
                            // 科技报告标签
                            @RequestParam(value = "reportTaskSource", required = false) String reportTaskSource,
                            @RequestParam(value = "reportModel", required = false) String reportModel,
                            @RequestParam(value = "reportMajor", required = false) String reportMajor,
                            @RequestParam(value = "reportCategory", required = false) String reportCategory,
                            // 标准规范标签
                            @RequestParam(value = "stdLevel", required = false) String stdLevel,
                            @RequestParam(value = "stdField", required = false) String stdField,
                            @RequestParam(value = "stdCivilMilitary", required = false) String stdCivilMilitary,
                            @RequestParam(value = "stdAircraftType", required = false) String stdAircraftType,
                            @RequestParam(value = "stdType", required = false) String stdType,
                            // 新闻资讯标签
                            @RequestParam(value = "newsType", required = false) String newsType,
                            @RequestParam(value = "newsMajor", required = false) String newsMajor,
                            // 规章制度标签
                            @RequestParam(value = "ruleDomain", required = false) String ruleDomain,
                            @RequestParam(value = "ruleLevel", required = false) String ruleLevel,
                            @RequestParam(value = "ruleDept", required = false) String ruleDept,
                            // 期刊文献标签
                            @RequestParam(value = "journalDb", required = false) String journalDb,
                            @RequestParam(value = "journalSubject", required = false) String journalSubject,
                            @RequestParam(value = "journalLevel", required = false) String journalLevel,
                            @RequestParam(value = "journalIndustry", required = false) String journalIndustry,
                            // 行政公文标签
                            @RequestParam(value = "docFromUnit", required = false) String docFromUnit,
                            @RequestParam(value = "docHostUnit", required = false) String docHostUnit,
                            @RequestParam(value = "docDrafter", required = false) String docDrafter,
                            @RequestParam(value = "docDraftDept", required = false) String docDraftDept,
                            // 知识产权标签
                            @RequestParam(value = "ipPatentInventor", required = false) String ipPatentInventor,
                            @RequestParam(value = "ipPatentApplicant", required = false) String ipPatentApplicant,
                            @RequestParam(value = "ipAchievementDeclarant", required = false) String ipAchievementDeclarant,
                            @RequestParam(value = "ipAchievementCompleter", required = false) String ipAchievementCompleter,
                            @RequestParam(value = "ipSoftDevList", required = false) String ipSoftDevList,
                            @RequestParam(value = "ipSoftRepresentative", required = false) String ipSoftRepresentative) {
        try {
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
            // 设置响应头
            String encodedFileName = URLEncoder.encode(minioFile.getFileName(), "UTF-8").replaceAll("\\+", "%20");
            response.setContentType(minioFile.getFileType() != null ? minioFile.getFileType() : "application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
            response.setContentLengthLong(minioFile.getFileSize() != null ? minioFile.getFileSize() : 0);
            // 写出文件流
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
        List<Map<String, Object>> list = minioFileService.countByDocType(folderId);
        return Result.OK(list);
    }

    @ApiOperation(value = "标签统计", notes = "按标签字段统计每个标签值的文件数量（用于左侧筛选树）")
    @GetMapping(value = "/tagStats")
    public Result<?> tagStats(@RequestParam(name = "folderId") String folderId,
                              @RequestParam(name = "docType") String docType,
                              @RequestParam(name = "tagName") String tagName) {
        List<Map<String, Object>> list = minioFileService.countByTag(folderId, docType, tagName);
        return Result.OK(list);
    }
}
```

- [ ] **Step 2: 验证编译通过**

Run: `cd jeecg-boot && mvn compile -pl jeecg-module-demo -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add jeecg-boot/jeecg-module-demo/src/main/java/org/jeecg/modules/miniofile/controller/MinioFileController.java
git commit -m "feat(minio): add MinioFile controller with all REST endpoints"
```

---

> **前端实现计划（Task 7-9）见独立文档：** [2026-04-03-minio-file-management-frontend.md](2026-04-03-minio-file-management-frontend.md)

---

## Task 7: 前端配置（API + 路由 + 标签配置）

**Files:**
- Modify: `ant-design-vue-jeecg/src/api/api.js`
- Modify: `ant-design-vue-jeecg/src/config/router.config.js`
- Create: `ant-design-vue-jeecg/src/views/miniofile/DocTypeTagConfig.js`

- [ ] **Step 1: 在 api.js 末尾追加 MinIO 文件管理相关 API 方法**

在 `ant-design-vue-jeecg/src/api/api.js` 文件末尾（`export default` 之前）添加以下代码：

```javascript
// ===================== MinIO 文件管理 =====================

// 文件夹接口
const minioFolderList = (params) => getAction("/minio/folder/list", params);
const minioFolderAdd = (params) => postAction("/minio/folder/add", params);
const minioFolderEdit = (params) => putAction("/minio/folder/edit", params);
const minioFolderDelete = (params) => deleteAction("/minio/folder/delete", params);
const minioFolderDeleteBatch = (params) => deleteAction("/minio/folder/deleteBatch", params);
const minioFolderQueryById = (params) => getAction("/minio/folder/queryById", params);

// 文件接口
const minioFileList = (params) => getAction("/minio/file/list", params);
const minioFileQueryById = (params) => getAction("/minio/file/queryById", params);
const minioFileEdit = (params) => putAction("/minio/file/edit", params);
const minioFileDelete = (params) => deleteAction("/minio/file/delete", params);
const minioFileDownload = (id) => window._CONFIG['domianURL'] + "/minio/file/download/" + id;
const minioFileCountByType = (params) => getAction("/minio/file/countByType", params);
const minioFileTagStats = (params) => getAction("/minio/file/tagStats", params);

// 单文件上传（FormData，不走getAction）
const minioFileUpload = (formData) => {
  return new Promise((resolve, reject) => {
    const baseUrl = window._CONFIG && window._CONFIG['domianURL'] ? window._CONFIG['domianURL'] : '';
    const token = Vue.ls.get(ACCESS_TOKEN) || '';
    fetch(baseUrl + '/minio/file/upload', {
      method: 'POST',
      headers: { 'X-Access-Token': token },
      body: formData,
    })
    .then(res => res.json())
    .then(data => { resolve(data); })
    .catch(err => { reject(err); });
  });
};

// 压缩包上传
const minioFileUploadZip = (formData) => {
  return new Promise((resolve, reject) => {
    const baseUrl = window._CONFIG && window._CONFIG['domianURL'] ? window._CONFIG['domianURL'] : '';
    const token = Vue.ls.get(ACCESS_TOKEN) || '';
    fetch(baseUrl + '/minio/file/uploadZip', {
      method: 'POST',
      headers: { 'X-Access-Token': token },
      body: formData,
    })
    .then(res => res.json())
    .then(data => { resolve(data); })
    .catch(err => { reject(err); });
  });
};

export {
  minioFolderList, minioFolderAdd, minioFolderEdit, minioFolderDelete,
  minioFolderDeleteBatch, minioFolderQueryById,
  minioFileList, minioFileQueryById, minioFileEdit, minioFileDelete,
  minioFileDownload, minioFileCountByType, minioFileTagStats,
  minioFileUpload, minioFileUploadZip,
}
```

> **注意：** 还需在文件顶部确保 `import Vue from 'vue'` 和 `import { ACCESS_TOKEN } from "@/store/mutation-types"` 存在（通常已有 Vue import）。

- [ ] **Step 2: 在 router.config.js 中添加 MinIO 文件管理路由**

在路由数组中添加（建议放在一个合适的位置，与其他业务路由同级）：

```javascript
{
  path: '/miniofile',
  component: RouteView,
  meta: { title: '文件管理', icon: 'folder' },
  children: [
    {
      path: '/miniofile/folder',
      name: 'MinioFolderList',
      component: () => import('@/views/miniofile/MinioFolderList'),
      meta: { title: '文件夹管理' }
    },
    {
      path: '/miniofile/file/:folderId',
      name: 'MinioFileList',
      component: () => import('@/views/miniofile/MinioFileList'),
      meta: { title: '文件管理', hidden: true }
    }
  ]
}
```

> **说明：** `hidden: true` 让文件管理页不在菜单中单独显示，通过文件夹列表点击进入。

- [ ] **Step 3: 创建 DocTypeTagConfig.js 标签配置文件**

定义文档类型、对应的标签字段、以及每个标签的可选值。

```javascript
/**
 * 文档类型与标签字段配置
 * docTypeKey: 用于后端 doc_type 字段的值
 * label: 标签页显示名称
 * tagFields: 该类型下的标签字段列表
 *   - field: 数据库列名（驼峰）
 *   - label: 显示名称
 *   - type: 'select' | 'input' （下拉选择 或 文本输入）
 *   - options: 当 type=select 时的选项列表
 */
export const DOC_TYPES = [
  {
    docTypeKey: '科技报告',
    label: '科技报告',
    tagFields: [
      { field: 'reportTaskSource', label: '任务来源/渠道', type: 'select', options: ['国家任务', '省市任务', '自研项目', '国际合作', '其他'] },
      { field: 'reportModel', label: '型号', type: 'select', options: ['WS-15', 'WS-20', 'CJ-1000', '其他'] },
      { field: 'reportMajor', label: '专业', type: 'select', options: ['材料工程', '结构设计', '气动设计', '控制系统', '推进系统', '其他'] },
      { field: 'reportCategory', label: '类别', type: 'select', options: ['研究报告', '试验报告', '设计报告', '技术报告', '其他'] },
    ]
  },
  {
    docTypeKey: '标准规范',
    label: '标准规范',
    tagFields: [
      { field: 'stdLevel', label: '标准层次', type: 'select', options: ['国家标准', '行业标准', '企业标准', '国际标准', '其他'] },
      { field: 'stdField', label: '标准领域', type: 'select', options: ['航空', '航天', '电子', '机械', '材料', '其他'] },
      { field: 'stdCivilMilitary', label: '军民属性', type: 'select', options: ['军用', '民用', '军民两用'] },
      { field: 'stdAircraftType', label: '适用航空器类型', type: 'select', options: ['固定翼', '旋翼机', '无人机', '通用航空', '其他'] },
      { field: 'stdType', label: '标准类型', type: 'select', options: ['基础标准', '产品标准', '方法标准', '管理标准', '其他'] },
    ]
  },
  {
    docTypeKey: '新闻资讯',
    label: '新闻资讯',
    tagFields: [
      { field: 'newsType', label: '新闻类型', type: 'select', options: ['行业动态', '科研进展', '政策法规', '会议报道', '其他'] },
      { field: 'newsMajor', label: '新闻专业', type: 'select', options: ['气动', '结构', '材料', '控制', '推进', '其他'] },
    ]
  },
  {
    docTypeKey: '规章制度',
    label: '规章制度',
    tagFields: [
      { field: 'ruleDomain', label: '业务域', type: 'select', options: ['科研管理', '人力资源', '财务管理', '质量管理', '其他'] },
      { field: 'ruleLevel', label: '制度等级', type: 'select', options: ['所级', '部门级', '国家级', '行业级', '其他'] },
      { field: 'ruleDept', label: '主责部门', type: 'select', options: ['科研部', '技术部', '质量部', '综合管理部', '其他'] },
    ]
  },
  {
    docTypeKey: '期刊文献',
    label: '期刊文献',
    tagFields: [
      { field: 'journalDb', label: '来源数据库', type: 'select', options: ['CNKI', '万方', '维普', 'Web of Science', '其他'] },
      { field: 'journalSubject', label: '学科', type: 'select', options: ['航空宇航科学与技术', '材料科学与工程', '机械工程', '动力工程', '其他'] },
      { field: 'journalLevel', label: '论文级别', type: 'select', options: ['SCI', 'EI', '核心期刊', '会议论文', '其他'] },
      { field: 'journalIndustry', label: '行业分类', type: 'select', options: ['航空运输', '航天器制造', '航空设备制造', '其他'] },
    ]
  },
  {
    docTypeKey: '行政公文',
    label: '行政公文',
    tagFields: [
      { field: 'docFromUnit', label: '来文单位', type: 'input' },
      { field: 'docHostUnit', label: '主办单位', type: 'input' },
      { field: 'docDrafter', label: '拟稿人', type: 'input' },
      { field: 'docDraftDept', label: '拟稿部门', type: 'input' },
    ]
  },
]

/**
 * 知识产权子类型（知识产权标签页下有3个子标签）
 */
export const IP_SUB_TYPES = [
  {
    docTypeKey: '知识产权-专利标签',
    label: '专利标签',
    tagFields: [
      { field: 'ipPatentInventor', label: '专利发明人', type: 'input' },
      { field: 'ipPatentApplicant', label: '专利申请人', type: 'input' },
    ]
  },
  {
    docTypeKey: '知识产权-科技成果',
    label: '科技成果',
    tagFields: [
      { field: 'ipAchievementDeclarant', label: '所级申报人', type: 'input' },
      { field: 'ipAchievementCompleter', label: '所级主要完成人', type: 'input' },
    ]
  },
  {
    docTypeKey: '知识产权-软件著作权',
    label: '软件著作权',
    tagFields: [
      { field: 'ipSoftDevList', label: '软著开发人列表', type: 'input' },
      { field: 'ipSoftRepresentative', label: '著作权人代表', type: 'input' },
    ]
  },
]

/**
 * 获取指定文档类型的标签字段配置
 * @param {string} docType doc_type值
 * @returns {Array} 标签字段数组
 */
export function getTagFieldsByDocType(docType) {
  const all = [...DOC_TYPES, ...IP_SUB_TYPES]
  const found = all.find(item => item.docTypeKey === docType)
  return found ? found.tagFields : []
}

/**
 * 获取文件大小显示文本
 */
export function formatFileSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return size.toFixed(i === 0 ? 0 : 1) + ' ' + units[i]
}

/**
 * 根据文件名获取文件图标类型
 */
export function getFileIcon(fileName) {
  if (!fileName) return 'file'
  const ext = fileName.split('.').pop().toLowerCase()
  const iconMap = {
    pdf: 'file-pdf', doc: 'file-word', docx: 'file-word',
    xls: 'file-excel', xlsx: 'file-excel',
    ppt: 'file-ppt', pptx: 'file-ppt',
    jpg: 'file-image', jpeg: 'file-image', png: 'file-image', gif: 'file-image',
    zip: 'file-zip', rar: 'file-zip', '7z': 'file-zip',
    txt: 'file-text',
    mp4: 'video-camera', avi: 'video-camera', mov: 'video-camera',
    mp3: 'audio', wav: 'audio',
  }
  return iconMap[ext] || 'file'
}
```

- [ ] **Step 4: Commit**

```bash
git add ant-design-vue-jeecg/src/api/api.js \
       ant-design-vue-jeecg/src/config/router.config.js \
       ant-design-vue-jeecg/src/views/miniofile/DocTypeTagConfig.js
git commit -m "feat(minio-frontend): add API methods, routes and DocTypeTagConfig"
```
