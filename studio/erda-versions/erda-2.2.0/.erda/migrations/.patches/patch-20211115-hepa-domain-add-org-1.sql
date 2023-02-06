alter table tb_gateway_domain character set utf8mb4;

alter table tb_gateway_domain modify column `project_id` varchar(32) NOT NULL DEFAULT '' COMMENT '项目标识id';
