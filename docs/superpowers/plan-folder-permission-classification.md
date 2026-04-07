# 文件夹权限控制 + 密级过滤 设计文档

## Context

需要在 MinIO 文件管理系统上增加两个安全功能：
1. **文件夹权限控制** — 管理员可配置用户可见的文件夹，用户只能看到被授权的文件夹
2. **密级控制** — 文件有密级属性（必填，默认"公开"），用户也有密级，文件密级高于用户密级时对用户不可见

## 密级等级定义（从低到高）

公开(1) → 内部(2) → 秘密(3) → 机密(4) → 绝密(5)

---

## 数据库设计

### 新增表：minio_folder_permission

| 字段 | 类型 | 说明 |
|------|------|------|
| id | varchar(36) PK | 主键 |
| user_id | varchar(36) | 用户ID |
| folder_id | varchar(36) | 文件夹ID |
| create_by | varchar(50) | 创建人 |
| create_time | datetime | 创建时间 |

唯一约束: `(user_id, folder_id)`

### 修改表：sys_user

新增字段: `classification_level varchar(50) DEFAULT '公开'`

---

## 后端 API 设计

### 文件夹权限 Controller (`/minio/folderPermission`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/allUsers` | 查询所有用户列表 |
| GET | `/list?userId=xxx` | 查询用户已授权的文件夹 |
| POST | `/save` | 保存权限 { userId, folderIds } |

### MinioFolderController 修改

`/minio/folder/list` 增加权限过滤：非 admin 用户只能看到被授权的文件夹。

### MinioFileController 修改

- `/minio/file/list` 增加双重过滤：文件夹权限 + 密级
- `/minio/file/download/{id}` 增加权限校验（403）
- `/minio/file/upload` 密级校验（默认"公开"）

---

## 前端设计

### 新增页面：FolderPermissionConfig.vue

权限配置页，仅管理员可访问：
- 用户下拉选择（支持搜索）
- 文件夹多选（mode=multiple，回显已选）
- 保存按钮

### 修改弹窗

FileUploadModal / FileEditModal 增加密级下拉选择（必填，默认"公开"）

---

## 文件清单

| 操作 | 文件 |
|------|------|
| 新建 | `docs/superpowers/sql/minio-permission-and-classification.sql` |
| 新建 | `entity/MinioFolderPermission.java` |
| 新建 | `mapper/MinioFolderPermissionMapper.java` |
| 新建 | `service/IMinioFolderPermissionService.java` |
| 新建 | `service/impl/MinioFolderPermissionServiceImpl.java` |
| 新建 | `controller/MinioFolderPermissionController.java` |
| 新建 | `views/miniofile/FolderPermissionConfig.vue` |
| 修改 | `controller/MinioFolderController.java` |
| 修改 | `controller/MinioFileController.java` |
| 修改 | `views/miniofile/FileUploadModal.vue` |
| 修改 | `views/miniofile/FileEditModal.vue` |

---

## 安全策略

- **默认拒绝**：用户无权限记录时，文件夹列表为空
- **Admin 豁免**：admin 用户跳过所有权限检查
- **双重校验**：列表和下载接口均做权限+密级校验
- **跨模块隔离**：使用 JdbcTemplate 查询 sys_user 表，避免引入 system 模块依赖
