-- =============================================================
-- 文件夹权限控制 + 密级过滤 数据库脚本
-- =============================================================

-- 1. 文件夹权限表
CREATE TABLE IF NOT EXISTS `minio_folder_permission` (
  `id` varchar(36) NOT NULL COMMENT '主键',
  `user_id` varchar(36) NOT NULL COMMENT '用户ID',
  `folder_id` varchar(36) NOT NULL COMMENT '文件夹ID',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_folder` (`user_id`, `folder_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_folder_id` (`folder_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MinIO文件夹权限';

-- 2. sys_user 增加密级字段
ALTER TABLE `sys_user` ADD COLUMN `classification_level` varchar(50) DEFAULT '公开' COMMENT '用户密级（公开/内部/秘密/机密/绝密）';

-- 3. 菜单权限：文件夹权限配置页（挂在系统管理下）
-- 先删除旧记录（如果存在），避免重复
DELETE FROM sys_permission WHERE id = 'miniofile_permission_config';
DELETE FROM sys_role_permission WHERE permission_id = 'miniofile_permission_config';

-- 插入菜单（parent_id 使用子查询自动获取系统管理的ID）
-- menu_type=0 是目录, menu_type=1 是菜单页面
INSERT INTO sys_permission (id, parent_id, name, url, component, component_name, is_route, is_leaf, keep_alive, hidden, hide_tab, description, del_flag, rule_flag, status, internal_or_external, perms, perms_type, sort_no, always_show, icon, menu_type, create_by, create_time, update_by, update_time)
SELECT 'miniofile_permission_config', p.id, '文件夹权限配置', '/system/folderPermission', 'system/folderpermission/FolderPermissionConfig', NULL, 1, 1, 0, 0, 0, '文件夹权限与密级配置', 0, 0, '1', 0, NULL, '1', 99, NULL, 'safety-certificate', 1, 'admin', NOW(), NULL, NULL
FROM sys_permission p WHERE p.name = '系统管理' AND p.parent_id = '' LIMIT 1;

-- 4. 给 admin 角色授权
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT REPLACE(UUID(), '-', ''), 'f6817f48af4fb3af11b9e8bf182f618b', 'miniofile_permission_config'
FROM dual WHERE NOT EXISTS (
  SELECT 1 FROM sys_role_permission WHERE role_id = 'f6817f48af4fb3af11b9e8bf182f618b' AND permission_id = 'miniofile_permission_config'
);
