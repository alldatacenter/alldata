/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

-- This is the SQL change file from version 1.7.0 to the current version 1.8.0.
-- When upgrading to version 1.8.0, please execute those SQLs in the DB (such as MySQL) used by the Manager module.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

USE `apache_inlong_manager`;

ALTER TABLE inlong_group
    CHANGE lightweight inlong_group_mode tinyint(1) DEFAULT 0 NULL COMMENT 'InLong group mode, Standard mode(include Data Ingestion and Synchronization): 0, DataSync mode(only Data Synchronization): 1';

-- To support multi-tenant management in InLong, see https://github.com/apache/inlong/issues/7914
CREATE TABLE IF NOT EXISTS `inlong_tenant`
(
    `id`           int(11)      NOT NULL AUTO_INCREMENT,
    `name`         varchar(256) NOT NULL COMMENT 'Tenant name, not support modification',
    `description`  varchar(256) DEFAULT '' COMMENT 'Description of tenant',
    `is_deleted`   int(11)      DEFAULT '0' COMMENT 'Whether to delete, 0 is not deleted, if greater than 0, delete',
    `creator`      varchar(256) NOT NULL COMMENT 'Creator name',
    `modifier`     varchar(256) DEFAULT NULL COMMENT 'Modifier name',
    `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `modify_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Modify time',
    `version`      int(11)      NOT NULL DEFAULT '1' COMMENT 'Version number, which will be incremented by 1 after modification',
    PRIMARY KEY (`id`),
    UNIQUE KEY `unique_tenant_key` (`name`, `is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8 COMMENT ='Inlong tenant table';

INSERT INTO `inlong_tenant`(`name`, `description`, `creator`, `modifier`)
VALUES ('public', 'Default tenant', 'inlong_init', 'inlong_init');

-- To support distinguish inlong user permission and tenant permission control, please see https://github.com/apache/inlong/issues/8098
CREATE TABLE IF NOT EXISTS `inlong_user_role`
(
    `id`          int(11)      NOT NULL AUTO_INCREMENT,
    `username`   varchar(256)  NOT NULL COMMENT 'Username',
    `role_code`   varchar(256) NOT NULL COMMENT 'User role code',
    `disabled`    tinyint(1)   NOT NULL DEFAULT '0' COMMENT 'Whether to disabled, 0: enabled, 1: disabled',
    `is_deleted`  int(11)               DEFAULT '0' COMMENT 'Whether to delete, 0 is not deleted, if greater than 0, delete',
    `creator`     varchar(256) NOT NULL COMMENT 'Creator name',
    `modifier`    varchar(256)          DEFAULT NULL COMMENT 'Modifier name',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `modify_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Modify time',
    `version`     int(11)      NOT NULL DEFAULT '1' COMMENT 'Version number, which will be incremented by 1 after modification',
    PRIMARY KEY (`id`),
    UNIQUE KEY `unique_inlong_user_role` (`username`, `role_code`, `is_deleted`)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4 COMMENT ='Inlong User Role Table';

INSERT INTO `inlong_user_role` (`username`, `role_code`, `creator`)
VALUES ('admin', 'INLONG_ADMIN', 'inlong_init');

RENAME TABLE user_role TO tenant_user_role;
ALTER TABLE tenant_user_role
    CHANGE `user_name` `username` varchar(256) NOT NULL COMMENT 'Username';

ALTER TABLE tenant_user_role
    ADD tenant VARCHAR(256) DEFAULT 'public' NOT NULL comment 'User tenant';
ALTER TABLE tenant_user_role
    ADD CONSTRAINT unique_tenant_user UNIQUE (username, tenant, is_deleted);
CREATE INDEX index_tenant
    ON tenant_user_role (tenant, is_deleted);

INSERT INTO tenant_user_role(username, role_code, tenant, creator)
    SELECT name, 'TENANT_ADMIN', 'public', 'inlong_init'
    FROM user;

-- To avoid the ambiguity, rename "tenant" in PulsarGroup & PulsarCluster to "pulsarTenant"
UPDATE inlong_group SET ext_params = replace(ext_params, '"tenant"', '"pulsarTenant"');
UPDATE inlong_cluster SET ext_params = replace(ext_params, '"tenant"', '"pulsarTenant"');

ALTER TABLE `inlong_stream` MODIFY COLUMN `name` varchar(256) DEFAULT NULL COMMENT 'The name of the inlong stream page display, can be Chinese';

ALTER TABLE `inlong_group`
    ADD `tenant` VARCHAR(256) DEFAULT 'public' NOT NULL comment 'Inlong tenant of group' after `ext_params`;
CREATE INDEX tenant_index
    ON inlong_group (`tenant`, `is_deleted`);

-- To support multi-tenancy of datanode. Please see #8349
ALTER TABLE `data_node`
    ADD `tenant` VARCHAR(256) DEFAULT 'public' NOT NULL comment 'Inlong tenant of datanode' after `description`;
CREATE INDEX datanode_tenant_index
    ON data_node (`tenant`, `is_deleted`);

-- To support multi-tenancy of cluster. Please see #8365
ALTER TABLE `inlong_cluster`
    ADD `tenant` VARCHAR(256) DEFAULT 'public' NOT NULL comment 'Inlong tenant of cluster' after `heartbeat`;
CREATE INDEX cluster_tenant_index
    ON inlong_cluster (`tenant`, `is_deleted`);

-- To support multi-tenancy of cluster tag. Please see #8378
ALTER TABLE `inlong_cluster_tag`
    ADD `tenant` VARCHAR(256) DEFAULT 'public' NOT NULL comment 'Inlong tenant of inlong cluster tag' after `description`;

-- To support multi-tenancy of inlong consume. Please see #8378
ALTER TABLE `inlong_consume`
    ADD `tenant` VARCHAR(256) DEFAULT 'public' NOT NULL comment 'Inlong tenant of consume' after `ext_params`;
CREATE INDEX consume_tenant_index
    ON inlong_consume (`tenant`, `is_deleted`);

-- To support multi-tenancy of workflow. Please see #8404
ALTER TABLE `workflow_approver`
    ADD `tenant` VARCHAR(256) DEFAULT 'public' NOT NULL comment 'Inlong tenant of workflow approver' after `task_name`;
ALTER TABLE `workflow_process`
    ADD `tenant` VARCHAR(256) DEFAULT 'public' NOT NULL comment 'Inlong tenant of workflow process' after `inlong_stream_id`;
ALTER TABLE `workflow_task`
    ADD `tenant` VARCHAR(256) DEFAULT 'public' NOT NULL comment 'Inlong tenant of workflow task' after `display_name`;

-- Alter heartbeat related tables
ALTER TABLE component_heartbeat DROP COLUMN id;
ALTER TABLE `component_heartbeat` ADD PRIMARY KEY (`component`, `instance`);
DROP INDEX unique_component_heartbeat on component_heartbeat;

ALTER TABLE group_heartbeat DROP COLUMN id;
ALTER TABLE `group_heartbeat` ADD PRIMARY KEY  (`component`, `instance`, `inlong_group_id`);
DROP INDEX unique_group_heartbeat on group_heartbeat;

ALTER TABLE stream_heartbeat DROP COLUMN id;
ALTER TABLE `stream_heartbeat` ADD PRIMARY KEY (`component`, `instance`, `inlong_group_id`, `inlong_stream_id`);
DROP INDEX unique_stream_heartbeat on stream_heartbeat;

-- Create audit_source table
CREATE TABLE IF NOT EXISTS `audit_source`
(
    `id`          int(11)      NOT NULL AUTO_INCREMENT,
    `name`        varchar(128) NOT NULL COMMENT 'Audit source name',
    `type`        varchar(20)  NOT NULL COMMENT 'Audit source type, including: MYSQL, CLICKHOUSE, ELASTICSEARCH',
    `url`         varchar(256) NOT NULL COMMENT 'Audit source URL, for MYSQL or CLICKHOUSE, is jdbcUrl, and for ELASTICSEARCH is the access URL with hostname:port',
    `enable_auth` tinyint(1)            DEFAULT '1' COMMENT 'Enable auth or not, 0: disable, 1: enable',
    `username`    varchar(128)          COMMENT 'Audit source username, needed if auth_enable is 1' ,
    `token`       varchar(512)          DEFAULT NULL COMMENT 'Audit source token, needed if auth_enable is 1',
    `status`      smallint(4)  NOT NULL DEFAULT '1' COMMENT 'Whether the audit source is online or offline, 0: offline, 1: online' ,
    `is_deleted`  int(11)               DEFAULT '0' COMMENT 'Whether to delete, 0: not deleted, > 0: deleted',
    `creator`     varchar(64)  NOT NULL COMMENT 'Creator name',
    `modifier`    varchar(64)  NOT NULL COMMENT 'Modifier name',
    `create_time` timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `modify_time` timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Modify time',
    `version`     int(11)      NOT NULL DEFAULT '1' COMMENT 'Version number, which will be incremented by 1 after modification',
    PRIMARY KEY (`id`),
    UNIQUE KEY `unique_audit_source` (url, `is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Audit source table';
