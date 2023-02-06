# MIGRATION_BASE

CREATE TABLE IF NOT EXISTS dice_api_doc_lock (
  id bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  session_id char(36) NOT NULL COMMENT '会话标识',
  is_locked tinyint(1) NOT NULL DEFAULT '0' COMMENT '会话所有者是否持有文档锁',
  expired_at datetime NOT NULL COMMENT '会话过期时间',
  application_id bigint(20) NOT NULL COMMENT '应用 id',
  branch_name varchar(191) NOT NULL COMMENT '分支名',
  doc_name varchar(191) NOT NULL COMMENT '文档名, 也即服务名',
  creator_id varchar(191) NOT NULL COMMENT '创建者 id',
  updater_id varchar(191) NOT NULL COMMENT '更新者 id',
  PRIMARY KEY (id),
  UNIQUE KEY uk_doc (application_id, branch_name,doc_name)
) ENGINE=InnoDB AUTO_INCREMENT=399 DEFAULT CHARSET=utf8mb4 COMMENT='API 设计中心文档锁表';