/******************************************/
/*   数据库全名 = tesla_authproxy   */
/*   表名称 = ta_service_meta   */
/*   对内需求增加该字段标识服务是否对外开发-应用管理场景需求   */
/******************************************/
ALTER TABLE `ta_service_meta`
  ADD COLUMN `is_open` tinyint NOT NULL DEFAULT 1 COMMENT '是否开放';

/******************************************/
/*   数据库全名 = tesla_authproxy   */
/*   表名称 = ta_app   */
/*   对内需求线上缓存acl的accessKey长度原45不够用，调整长度为128   */
/******************************************/
ALTER TABLE `ta_app`
	MODIFY COLUMN `app_accesskey` varchar(128) NOT NULL COMMENT '在ACL上注册的accessKey';