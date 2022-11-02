LOCK TABLES `ta_app` WRITE;
INSERT IGNORE INTO `ta_app` VALUES
(1,'bcc','bd-peconsole-y999zdKcwiAIn6Zbq','bcc_admin',NULL,NOW(),NOW(),'${NETWORK_PROTOCOL}://${DNS_PAAS_HOME}',0,'${LOGIN_URL}');
UNLOCK TABLES;

LOCK TABLES `ta_app_ext` WRITE;
INSERT IGNORE INTO `ta_app_ext` VALUES
(1,'bcc','e01c1ad6-e84b-44bc-b3df-d58573c79bae',NULL,NOW(),NOW()),
(2,'common','common-9efab2399c7c560b34de477b9aa0a465',NULL,NOW(),NOW());
UNLOCK TABLES;

LOCK TABLES `ta_permission_res` WRITE;
INSERT IGNORE INTO `ta_permission_res` VALUES
(27,'bcc','ADS','BCCAPI产品ADS权限','26842:bcc:/api/product/ads/',NULL,NULL),
(28,'bcc','ODPS','BCCAPI产品ODPS权限','26842:bcc:/api/product/odps/',NULL,NULL),
(29,'bcc','APSARA','BCCAPI产品APSARA权限','26842:bcc:/api/product/apsara/',NULL,NULL),
(30,'bcc','STREAMCOMPUTE','BCCAPI产品STREAMCOMPUTE权限','26842:bcc:/api/product/streamcompute/',NULL,NULL),
(31,'bcc','DATAWORKS','BCCAPI产品DATAWORKS权限','26842:bcc:/api/product/dataworks/',NULL,NULL),
(32,'bcc','MINIRDS','BCCAPI产品MINIRDS权限','26842:bcc:/api/product/minirds/',NULL,NULL),
(33,'bcc','MINILVS','BCCAPI产品MINILVS权限','26842:bcc:/api/product/minilvs/',NULL,NULL),
(34,'bcc','BCC','BCCAPI产品BCC权限','26842:bcc:/api/product/bcc/',NULL,NULL),
(35,'bcc','IPLUS','BCCAPI产品IPLUS权限','26842:bcc:/api/product/iplus/',NULL,NULL),
(36,'bcc','DATAV','BCCAPI产品DATAV权限','26842:bcc:/api/product/datav/',NULL,NULL),
(37,'bcc','DTBOOST','BCCAPI产品DTBOOST权限','26842:bcc:/api/product/dtboost/',NULL,NULL),
(38,'bcc','PAI','BCCAPI产品PAI权限','26842:bcc:/api/product/pai/',NULL,NULL),
(39,'bcc','QUICKBI','BCCAPI产品QUICKBI权限','26842:bcc:/api/product/quickbi/',NULL,NULL),
(40,'bcc','BIGGRAPH','BCCAPI产品BIGGRAPH权限','26842:bcc:/api/product/biggraph/',NULL,NULL),
(41,'bcc','ES','BCCAPI产品ES权限','26842:bcc:/api/product/es/',NULL,NULL),
(42,'bcc','ELASTICSEARCH','BCCAPI产品ELASTICSEARCH权限','26842:bcc:/api/product/elasticsearch/',NULL,NULL),
(43,'bcc','ASAP','BCCAPI产品ASAP权限','26842:bcc:/api/product/asap/',NULL,NULL),
(44,'bcc','DATAPHIN','BCCAPI产品DATAPHIN权限','26842:bcc:/api/product/dataphin/',NULL,NULL),
(45,'bcc','DATAHUB','BCCAPI产品DATAHUB权限','26842:bcc:/api/product/datahub/',NULL,NULL);
UNLOCK TABLES;

LOCK TABLES `ta_config` WRITE;
INSERT IGNORE INTO `ta_config` VALUES
(1, UTC_TIMESTAMP(), UTC_TIMESTAMP(), "private_account_validation", "password");
UNLOCK TABLES;

TRUNCATE TABLE `ta_user_callback`;
LOCK TABLES `ta_user_callback` WRITE;
DELETE FROM ta_user_callback;
INSERT IGNORE INTO `ta_user_callback` VALUES
(1, UTC_TIMESTAMP(), UTC_TIMESTAMP(), "USER_CREATED", "http://${ENDPOINT_SAAS_DATAWORKS}/base/user_add"),
(2, UTC_TIMESTAMP(), UTC_TIMESTAMP(), "USER_CREATED", "http://${ENDPOINT_PAAS_AUTHPROXY}/auth/private/thirdparty/ads/permission"),
(3, UTC_TIMESTAMP(), UTC_TIMESTAMP(), "USER_CREATED", "http://${ENDPOINT_PAAS_AUTHPROXY}/auth/private/thirdparty/ots/permission"),
(4, UTC_TIMESTAMP(), UTC_TIMESTAMP(), "USER_DELETED", "http://${ENDPOINT_SAAS_TESLA}/account/delete_check");
UNLOCK TABLES;

LOCK TABLES `ta_user` WRITE;
INSERT IGNORE INTO ta_user
(id, bid, emp_id, aliyun_pk, login_name, nick_name, gmt_create, gmt_modified, last_login_time, is_first_login, is_locked, access_key_id, access_key_secret, status, is_immutable, secret_key)
VALUES
(1, "26842", "${ACCOUNT_SUPER_PK}", "${ACCOUNT_SUPER_PK}", "${ACCOUNT_SUPER_ID}", "${ACCOUNT_SUPER_ID}", UTC_TIMESTAMP(), UTC_TIMESTAMP(), UTC_TIMESTAMP(), 0, 0, "${ACCOUNT_SUPER_ACCESS_ID}", "${ACCOUNT_SUPER_ACCESS_KEY}", 0, 0, "${ACCOUNT_SUPER_SECRET_KEY}");
UNLOCK TABLES;
