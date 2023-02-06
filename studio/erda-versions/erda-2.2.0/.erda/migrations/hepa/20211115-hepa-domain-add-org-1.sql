alter table `tb_gateway_domain`
      add column `org_id` varchar(32)  NOT NULL DEFAULT '' COMMENT '企业id',
      add key `idx_org` (`org_id`, `is_deleted`);

alter table tb_gateway_domain character set utf8mb4;

alter table tb_gateway_domain modify column `project_id` varchar(32) NOT NULL DEFAULT '' COMMENT '项目标识id';



