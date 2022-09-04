CREATE TABLE `am_unit`
(
    `id`            bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`    datetime     DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`  datetime     DEFAULT NULL COMMENT '最后修改时间',
    `unit_id`       varchar(32)  DEFAULT NULL COMMENT '单元 ID (唯一标识)',
    `unit_name`     varchar(64)  DEFAULT NULL COMMENT '单元名称',
    `endpoint`      varchar(128) DEFAULT NULL COMMENT '单元地址',
    `proxy_ip`      varchar(32)  DEFAULT NULL COMMENT '代理 IP',
    `proxy_port`    varchar(8)   DEFAULT NULL COMMENT '代理 Port',
    `client_id`     varchar(32)  DEFAULT NULL COMMENT 'Client ID',
    `client_secret` varchar(32)  DEFAULT NULL COMMENT 'Client Secret',
    `username`      varchar(32)  DEFAULT NULL COMMENT 'Username',
    `password`      varchar(64)  DEFAULT NULL COMMENT 'Password',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_unit_id` (`unit_id`),
    KEY `idx_unit_name` (`unit_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='管控单元表';
