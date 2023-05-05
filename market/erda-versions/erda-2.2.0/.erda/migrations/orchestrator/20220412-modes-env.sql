ALTER TABLE erda_deployment_order ADD COLUMN `modes` varchar(255) NOT NULL DEFAULT '' COMMENT '部署模式';

ALTER TABLE ps_v2_project_runtimes ADD COLUMN `extra_params` text NOT NULL COMMENT '附加参数 json格式';