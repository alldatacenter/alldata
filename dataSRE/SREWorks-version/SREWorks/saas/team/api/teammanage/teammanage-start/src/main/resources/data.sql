REPLACE INTO `team` (
  `id`, `avatar`, `creator`, `description`, `gmt_create`, `gmt_modified`, `last_modifier`, `name`, `visible_scope`
) VALUES (
   1,'https://sreworks.oss-cn-beijing.aliyuncs.com/logo/test.png','999999999','','1646115762','1646115762','999999999','默认团队','INTERNAL'
);

REPLACE INTO `team_user` (
  `id`, `creator`, `gmt_access`, `gmt_create`, `gmt_modified`, `is_concern`, `last_modifier`, `role`, `team_id`, `user`
) VALUES (
   1,'999999999','1646237216','1646237216','1646237216','0','999999999','ADMIN','1','999999999'
 );

REPLACE INTO `team_repo` (
  `id`, `ci_token`, `creator`, `description`, `gmt_create`, `gmt_modified`, `last_modifier`, `name`, `team_id`, `url`, `ci_account`
) VALUES (
   1,'','999999999',NULL,'1644377915','1644377915','999999999','default','1','https://code.aliyun.com/sreworks',''
 );

REPLACE INTO `team_registry` (
  `id`, `auth`, `creator`, `description`, `gmt_create`, `gmt_modified`,`last_modifier`, `name`, `team_id`, `url`
) VALUES (
    1,'sreworks:sreworksDocker123q','999999999','',1646750564,1646750564,'999999999','sreworks-builds',1,'registry.cn-zhangjiakou.aliyuncs.com/builds'
);