# MinIO 文件管理系统设计文档

## 概述

基于 JeecgBoot Vue2 项目，开发一个 MinIO 存储的文件管理和标签功能模块。文件夹映射 MinIO Bucket，文件存储在对应 Bucket 中，文件属性存入数据库。支持单文件上传、压缩包批量上传（自动解压入库）、文件标签设置、增删改查。

## 架构决策

- **模块位置**：在 `jeecg-module-demo` 下新建独立模块 `jeecg-demo-miniofile`，复用现有 `MinioUtil` 和 MinIO 配置
- **MinIO 连接**：`http://129.226.204.202:10055`，用户名 `admin`，密码 `admin123`
- **标签方案**：标签字段平铺在文件表中（对接外部系统），标签选项在前端硬编码
- **密级**：仅保留"公开"

## 数据库设计

### minio_folder（文件夹表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | varchar(36) | 主键，ASSIGN_ID |
| folder_name | varchar(200) | 文件夹名称（唯一，同时作为 MinIO Bucket 名） |
| description | varchar(500) | 文件夹描述 |
| create_by | varchar(50) | 创建人 |
| create_time | datetime | 创建时间 |
| update_by | varchar(50) | 更新人 |
| update_time | datetime | 更新时间 |

### minio_file（文件表）

**基础字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | varchar(36) | 主键，ASSIGN_ID |
| folder_id | varchar(36) | 关联文件夹 ID |
| file_name | varchar(500) | 文档名称 |
| classification_level | varchar(50) | 文档密级（默认"公开"） |
| summary | text | 摘要 |
| author | varchar(200) | 作者 |
| tech_system | varchar(200) | 技术体系 |
| doc_type | varchar(100) | 文档类型（决定显示哪些标签字段） |
| doc_source | varchar(200) | 文档来源 |
| year | varchar(10) | 年度 |
| file_size | bigint | 文件大小（字节） |
| file_type | varchar(100) | MIME 类型 |
| minio_object_name | varchar(500) | MinIO 中的对象名 |
| upload_by | varchar(50) | 上传人 |
| create_time | datetime | 上传时间 |
| update_time | datetime | 更新时间 |

**标签字段（平铺）：**

| 字段 | 所属类别 |
|------|----------|
| report_task_source | 科技报告-任务来源/渠道 |
| report_model | 科技报告-型号 |
| report_major | 科技报告-专业 |
| report_category | 科技报告-类别 |
| std_level | 标准规范-标准层次 |
| std_field | 标准规范-标准领域 |
| std_civil_military | 标准规范-军民属性 |
| std_aircraft_type | 标准规范-适用航空器类型 |
| std_type | 标准规范-标准类型 |
| news_type | 新闻资讯-新闻类型 |
| news_major | 新闻资讯-新闻专业 |
| rule_domain | 规章制度-业务域 |
| rule_level | 规章制度-制度等级 |
| rule_dept | 规章制度-主责部门 |
| journal_db | 期刊文献-来源数据库 |
| journal_subject | 期刊文献-学科 |
| journal_level | 期刊文献-论文级别 |
| journal_industry | 期刊文献-行业分类 |
| doc_from_unit | 行政公文-来文单位 |
| doc_host_unit | 行政公文-主办单位 |
| doc_drafter | 行政公文-拟稿人 |
| doc_draft_dept | 行政公文-拟稿部门 |
| ip_patent_inventor | 知识产权-专利标签-专利发明人 |
| ip_patent_applicant | 知识产权-专利标签-专利申请人 |
| ip_achievement_declarant | 知识产权-科技成果-所级申报人 |
| ip_achievement_completer | 知识产权-科技成果-所级主要完成人 |
| ip_soft_dev_list | 知识产权-软件著作权-软著开发人列表 |
| ip_soft_representative | 知识产权-软件著作权-著作权人代表 |

## 后端设计

### 模块结构

```
jeecg-module-demo/jeecg-demo-miniofile/
├── pom.xml
└── src/main/java/org/jeecg/modules/miniofile/
    ├── controller/
    │   ├── MinioFolderController.java
    │   └── MinioFileController.java
    ├── entity/
    │   ├── MinioFolder.java
    │   └── MinioFile.java
    ├── mapper/
    │   ├── MinioFolderMapper.java
    │   └── MinioFileMapper.java
    ├── mapper/xml/
    │   ├── MinioFolderMapper.xml
    │   └── MinioFileMapper.xml
    └── service/
        ├── IMinioFolderService.java
        ├── IMinioFileService.java
        └── impl/
            ├── MinioFolderServiceImpl.java
            └── MinioFileServiceImpl.java
```

### 实体继承

实体类继承 `JeecgEntity`，自动获得 id、create_by、create_time、update_by、update_time 审计字段。

### 接口设计

**文件夹接口：**

| 接口 | 方法 | 说明 |
|------|------|------|
| `/minio/folder/list` | GET | 文件夹分页列表 |
| `/minio/folder/add` | POST | 新建文件夹（同时创建 MinIO Bucket） |
| `/minio/folder/edit` | PUT | 编辑文件夹描述 |
| `/minio/folder/delete` | DELETE | 删除文件夹（同时删除 Bucket，需校验文件夹内无文件） |
| `/minio/folder/queryById` | GET | 根据 ID 查询单个文件夹 |

**文件接口：**

| 接口 | 方法 | 说明 |
|------|------|------|
| `/minio/file/list` | GET | 按 folder_id 和 doc_type 分页查询文件列表 |
| `/minio/file/queryById` | GET | 根据 ID 查询单个文件详情 |
| `/minio/file/upload` | POST (multipart) | 单文件上传到指定文件夹，存入 MinIO，属性入库 |
| `/minio/file/uploadZip` | POST (multipart) | 压缩包上传，自动解压，每个文件独立入库 |
| `/minio/file/edit` | PUT | 编辑文件属性和标签字段 |
| `/minio/file/delete` | DELETE | 删除文件（同时从 MinIO 移除） |
| `/minio/file/download/{id}` | GET | 下载文件（从 MinIO 获取文件流返回） |
| `/minio/file/countByType` | GET | 按 doc_type 统计文件数量（用于标签页角标） |
| `/minio/file/tagStats` | GET | 按标签字段统计每个标签值的文件数量（用于左侧筛选树） |

### 关键业务逻辑

**文件夹创建**：校验名称唯一性 → 创建 `minio_folder` 记录 → 调用 MinIO 创建 Bucket。

**文件夹删除**：校验文件夹下无文件 → 删除 `minio_folder` 记录 → 删除 MinIO Bucket。

**单文件上传**：校验文件夹存在 → 上传文件到 MinIO（bucket=folder_name, object=UUID+扩展名） → 写入 `minio_file` 记录（含所有属性和标签字段）。

**压缩包上传**：校验文件夹存在 → 接收 ZIP 文件 → 解压到临时目录 → 遍历解压文件，逐个上传到 MinIO 并写入 `minio_file` 记录（标签字段使用上传时统一设置的值）→ 清理临时文件。

**文件删除**：删除 `minio_file` 记录 → 从 MinIO 删除对应对象。

**文件下载**：根据 file 记录的 folder_id 查 folder_name → 根据 minio_object_name 从 MinIO 获取文件流 → 返回给前端。

## 前端设计

### 页面结构

```
ant-design-vue-jeecg/src/views/miniofile/
├── MinioFolderList.vue              # 文件夹列表页（进入后先看到的页面）
└── MinioFileList.vue                # 文件管理页（标签页 + 标签筛选 + 卡片列表，集成在同一组件）
```

`MinioFileList.vue` 内部包含：
- 标签页栏（7 大类 + 上传按钮）
- 左侧标签筛选面板
- 右侧卡片式文件列表
- 上传弹窗（单文件 / 压缩包）
- 文件编辑弹窗

### 路由

```js
{
  path: '/miniofile',
  component: RouteView,
  meta: { title: '文件管理' },
  children: [
    { path: '/miniofile/folder', component: MinioFolderList, meta: { title: '文件夹管理' } },
    { path: '/miniofile/file/:folderId', component: MinioFileList, meta: { title: '文件管理' } }
  ]
}
```

### 页面交互流程

1. 用户进入文件管理 → 看到文件夹列表
2. 点击某个文件夹 → 进入 `MinioFileList` 页面
3. 顶部标签页显示 7 个文档大类，每个标签带文件数量角标
4. 点击标签页 → doc_type 自动切换，左侧标签树只展示该类别的标签字段，右侧显示对应文件
5. 点击标签页右侧"上传文件"或"压缩包上传" → 弹窗中 doc_type 已自动选中，展示该类型的标签字段供用户填写
6. 左侧标签树 → 点击标签值 → 右侧文件列表按该标签筛选
7. 文件卡片 hover → 显示下载、编辑、删除操作

### 文件夹列表页

复用 JeecgListMixin，标准 CRUD 列表表格，包含新建/编辑/删除文件夹操作。

### 文件管理页布局

```
┌─────────────────────────────────────────────────────────┐
│ ← 文件夹列表 / 航空发动机技术资料                         │
├─────────────────────────────────────────────────────────┤
│ [科技报告(85)] [标准规范(34)] ... [知识产权(18)]  [上传] [压缩包上传] │
├────────────┬────────────────────────────────────────────┤
│ 标签筛选     │  搜索栏 + 排序                               │
│            │  活跃筛选标签                                  │
│ 任务来源/渠道  │                                            │
│  · 国家任务   │  ┌─ 文件卡片 1 ─────────────────────────┐  │
│  · 省市任务   │  │ 科技报告 | 公开 | 2024年度              │  │
│  · 自研项目   │  │ 标题                                   │  │
│            │  │ 作者 | 技术体系 | 来源 | 日期              │  │
│ 型号        │  │ 摘要...                                │  │
│  · WS-15    │  │ [标签1] [标签2] [标签3]         文件图标  │  │
│  · WS-20    │  └──────────────────────────────────────┘  │
│            │                                            │
│ 专业        │  ┌─ 文件卡片 2 ─────────────────────────┐  │
│  · 材料工程   │  │ ...                                   │  │
│  · 结构设计   │  └──────────────────────────────────────┘  │
│            │                                            │
│ 类别        │  分页                                       │
│  · 研究报告   │                                            │
│  · 试验报告   │                                            │
└────────────┴────────────────────────────────────────────┘
```

### 文档类型与标签字段映射

硬编码在前端 `DocTypeTagConfig.js` 中：

| doc_type | 显示的标签字段 |
|----------|---------------|
| 科技报告 | 任务来源/渠道、型号、专业、类别 |
| 标准规范 | 标准层次、标准领域、军民属性、适用航空器类型、标准类型 |
| 新闻资讯 | 新闻类型、新闻专业 |
| 规章制度 | 业务域、制度等级、主责部门 |
| 期刊文献 | 来源数据库、学科、论文级别、行业分类 |
| 行政公文 | 来文单位、主办单位、拟稿人、拟稿部门 |
| 知识产权-专利标签 | 专利发明人、专利申请人 |
| 知识产权-科技成果 | 所级申报人、所级主要完成人 |
| 知识产权-软件著作权 | 软著开发人列表、著作权人代表 |

### API 调用

在 `src/api/api.js` 中添加对应接口方法，使用 `getAction`、`postAction`、`putAction`、`deleteAction` 等标准方法。上传接口使用原生 `axios` + `FormData`。

## MinIO 集成

复用 `jeecg-boot-base-core` 中的 `MinioUtil`，修改 `application-dev.yml` 中 MinIO 配置指向目标地址。

```yaml
jeecg:
  minio:
    minio_url: http://129.226.204.202:10055
    minio_name: admin
    minio_pass: admin123
    bucketName: jeecg-boot
```

上传时使用文件夹的 `folder_name` 作为 bucket 名（需确保 bucket 存在，创建文件夹时自动创建）。
