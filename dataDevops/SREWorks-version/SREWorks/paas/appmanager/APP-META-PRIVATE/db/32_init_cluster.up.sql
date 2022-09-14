drop table if exists am_cluster;
CREATE TABLE IF NOT EXISTS `am_cluster`
(
    `id`             bigint      NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`     datetime    DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`   datetime    DEFAULT NULL COMMENT '最后修改时间',
    `cluster_id`     varchar(64) NOT NULL COMMENT '集群标识',
    `cluster_name`   varchar(64) DEFAULT NULL COMMENT '集群名称',
    `cluster_type`   varchar(16) NOT NULL COMMENT '集群类型',
    `cluster_config` longtext COMMENT '集群配置',
    PRIMARY KEY (`id`),
    KEY `idx_cluster_id` (`cluster_id`),
    KEY `idx_cluster_name` (`cluster_name`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='集群表';