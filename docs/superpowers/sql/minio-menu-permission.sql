-- MinIO 文件管理菜单（添加到 sys_permission 表）
-- 请先执行以下查询确认 ID 不冲突：
-- SELECT MAX(id) FROM sys_permission;
-- SELECT MAX(sort_no) FROM sys_permission WHERE parent_id = '';

-- 一级菜单：文件管理
INSERT INTO sys_permission (id, parent_id, name, url, component, component_name, is_route, is_leaf, keep_alive, hidden, hide_tab, description, del_flag, rule_flag, status, internal_or_external, perms, perms_type, sort_no, always_show, icon, menu_type, create_by, create_time, update_by, update_time)
VALUES ('miniofile_menu', '', '文件管理', '/miniofile', 'layouts/RouteView', NULL, 1, 0, 0, 0, 0, NULL, 0, 0, '1', 0, NULL, '1', 3, NULL, 'folder', 0, 'admin', NOW(), NULL, NULL);

-- 二级菜单：文件夹管理
INSERT INTO sys_permission (id, parent_id, name, url, component, component_name, is_route, is_leaf, keep_alive, hidden, hide_tab, description, del_flag, rule_flag, status, internal_or_external, perms, perms_type, sort_no, always_show, icon, menu_type, create_by, create_time, update_by, update_time)
VALUES ('miniofile_folder', 'miniofile_menu', '文件夹管理', '/miniofile/folder', 'miniofile/MinioFolderList', NULL, 1, 1, 0, 0, 0, NULL, 0, 0, '1', 0, NULL, '1', 1, NULL, 'folder-open', 1, 'admin', NOW(), NULL, NULL);

-- 隐藏路由：文件管理页（通过文件夹列表跳转进入）
INSERT INTO sys_permission (id, parent_id, name, url, component, component_name, is_route, is_leaf, keep_alive, hidden, hide_tab, description, del_flag, rule_flag, status, internal_or_external, perms, perms_type, sort_no, always_show, icon, menu_type, create_by, create_time, update_by, update_time)
VALUES ('miniofile_file', 'miniofile_menu', '文件管理', '/miniofile/file/:folderId', 'miniofile/MinioFileList', NULL, 1, 1, 0, 1, 0, NULL, 0, 0, '1', 0, NULL, '1', 2, NULL, 'file', 1, 'admin', NOW(), NULL, NULL);

-- 给 admin 角色（通常 role_code = 'admin'）授权这些菜单
-- 先查 admin 角色 ID：SELECT id FROM sys_role WHERE role_code = 'admin';
-- 然后插入（假设 admin 角色 ID 为 'admin'）：
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID(), 'f6817f48af4fb3af11b9e8bf182f618b', 'miniofile_menu');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID(), 'f6817f48af4fb3af11b9e8bf182f618b', 'miniofile_folder');
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (UUID(), 'f6817f48af4fb3af11b9e8bf182f618b', 'miniofile_file');
