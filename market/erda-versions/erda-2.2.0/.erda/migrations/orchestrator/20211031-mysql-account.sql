UPDATE tb_addon_instance SET kms_key = '' WHERE kms_key IS NULL;

ALTER TABLE tb_addon_instance MODIFY COLUMN `kms_key` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'kms key id';

ALTER TABLE tb_addon_attachment
    add column mysql_account_id varchar(36) NOT NULL DEFAULT '' COMMENT 'mysql account in use';
ALTER TABLE tb_addon_attachment
    add column previous_mysql_account_id varchar(36) NOT NULL DEFAULT '' COMMENT 'mysql account previously in use';
ALTER TABLE tb_addon_attachment
    add column mysql_account_state varchar(10) NOT NULL DEFAULT 'CUR' COMMENT 'use state of mysql account';

CREATE TABLE `erda_addon_mysql_account`
(
    `id`                  varchar(36)  NOT NULL COMMENT 'primary key',
    `created_at`          datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'entity create time',
    `updated_at`          datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'entity update time',
    `username`            varchar(100) NOT NULL COMMENT 'mysql username',
    `password`            text         NOT NULL COMMENT 'mysql password',
    `kms_key`             varchar(64)  NOT NULL DEFAULT '' COMMENT 'kms key id',
    `instance_id`         varchar(64)  NOT NULL DEFAULT '' COMMENT 'real instance',
    `routing_instance_id` varchar(64)  NOT NULL DEFAULT '' COMMENT 'routing instance',
    `creator`             varchar(36)  NOT NULL COMMENT 'user who create',
    `is_deleted`          TINYINT(1)   NOT NULL DEFAULT 0 COMMENT 'is deleted',
    PRIMARY KEY (`id`),
    KEY `idx_routing_instance_id` (`routing_instance_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='mysql account (tenant) for addon';

-- prepare mysql account
insert into erda_addon_mysql_account
(`id`, `username`, `password`, `kms_key`, `instance_id`, `routing_instance_id`, `creator`,
 `is_deleted`)
select uuid(),
       'mysql',
       e.value,
       i.kms_key,
       i.id,
       r.id,
       '',
       0
from tb_addon_instance_routing as r
         inner join tb_addon_instance as i on r.real_instance = i.id
         inner join tb_middle_instance_extra as e on r.real_instance = e.instance_id
where r.is_deleted = 'N'
  and r.addon_name = 'mysql'
  and r.status = 'ATTACHED'
  and e.field = 'password';

-- fix attachment
update tb_addon_attachment as att
    inner join erda_addon_mysql_account as acc on att.routing_instance_id = acc.routing_instance_id
set att.mysql_account_state       = 'CUR',
    att.mysql_account_id          = acc.id,
    att.previous_mysql_account_id = ''
where att.is_deleted = 'N';
