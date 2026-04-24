-- 技术体系分类数据（sys_category，pcode=C01）
-- 在系统管理 > 分类字典 中可视化管理

-- 根节点：技术体系
INSERT INTO sys_category (id, pid, name, code, has_child, create_by, create_time)
VALUES ('tech_system_root', '0', '技术体系', 'C01', '1', 'admin', NOW());

-- 一级分类
INSERT INTO sys_category (id, pid, name, code, has_child, create_by, create_time)
VALUES ('ts_01', 'tech_system_root', '航天技术', 'C01A01', '1', 'admin', NOW());

INSERT INTO sys_category (id, pid, name, code, has_child, create_by, create_time)
VALUES ('ts_02', 'tech_system_root', '航空技术', 'C01A02', '1', 'admin', NOW());

INSERT INTO sys_category (id, pid, name, code, has_child, create_by, create_time)
VALUES ('ts_03', 'tech_system_root', '电子信息', 'C01A03', '1', 'admin', NOW());

-- 二级分类：航天技术
INSERT INTO sys_category (id, pid, name, code, has_child, create_by, create_time)
VALUES ('ts_01_01', 'ts_01', '运载火箭', 'C01A01A01', '0', 'admin', NOW());

INSERT INTO sys_category (id, pid, name, code, has_child, create_by, create_time)
VALUES ('ts_01_02', 'ts_01', '卫星技术', 'C01A01A02', '0', 'admin', NOW());

INSERT INTO sys_category (id, pid, name, code, has_child, create_by, create_time)
VALUES ('ts_01_03', 'ts_01', '空间站技术', 'C01A01A03', '0', 'admin', NOW());

-- 二级分类：航空技术
INSERT INTO sys_category (id, pid, name, code, has_child, create_by, create_time)
VALUES ('ts_02_01', 'ts_02', '战斗机', 'C01A02A01', '0', 'admin', NOW());

INSERT INTO sys_category (id, pid, name, code, has_child, create_by, create_time)
VALUES ('ts_02_02', 'ts_02', '运输机', 'C01A02A02', '0', 'admin', NOW());

INSERT INTO sys_category (id, pid, name, code, has_child, create_by, create_time)
VALUES ('ts_02_03', 'ts_02', '无人机', 'C01A02A03', '0', 'admin', NOW());

-- 二级分类：电子信息
INSERT INTO sys_category (id, pid, name, code, has_child, create_by, create_time)
VALUES ('ts_03_01', 'ts_03', '雷达技术', 'C01A03A01', '0', 'admin', NOW());

INSERT INTO sys_category (id, pid, name, code, has_child, create_by, create_time)
VALUES ('ts_03_02', 'ts_03', '通信技术', 'C01A03A02', '0', 'admin', NOW());

INSERT INTO sys_category (id, pid, name, code, has_child, create_by, create_time)
VALUES ('ts_03_03', 'ts_03', '网络安全', 'C01A03A03', '0', 'admin', NOW());
