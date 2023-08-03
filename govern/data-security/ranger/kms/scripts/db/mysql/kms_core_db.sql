-- Licensed to the Apache Software Foundation (ASF) under one or more
-- contributor license agreements.  See the NOTICE file distributed with
-- this work for additional information regarding copyright ownership.
-- The ASF licenses this file to You under the Apache License, Version 2.0
-- (the "License"); you may not use this file except in compliance with
-- the License.  You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.

DROP TABLE IF EXISTS `ranger_masterkey`;
CREATE TABLE `ranger_masterkey` (
`id` bigint( 20 ) NOT NULL AUTO_INCREMENT ,
`create_time` datetime DEFAULT NULL ,
`update_time` datetime DEFAULT NULL ,
`added_by_id` bigint( 20 ) DEFAULT NULL ,
`upd_by_id` bigint( 20 ) DEFAULT NULL ,
`cipher` varchar( 255 ) DEFAULT NULL UNIQUE,
`bitlength` int DEFAULT NULL UNIQUE,
`masterkey` varchar(2048),
PRIMARY KEY ( `id` )
)ROW_FORMAT=DYNAMIC;

DROP TABLE IF EXISTS `ranger_keystore`;
CREATE TABLE `ranger_keystore` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `kms_alias` varchar(255) NOT NULL,
  `kms_createdDate` bigint(20) DEFAULT NULL,
  `kms_cipher` varchar(255) DEFAULT NULL,
   `kms_bitLength` bigint(20) DEFAULT NULL,
  `kms_description` varchar(512) DEFAULT NULL,
  `kms_version` bigint(20) DEFAULT NULL,
  `kms_attributes` varchar(1024) DEFAULT NULL,
  `kms_encoded`varchar(2048),
  PRIMARY KEY (`id`)
)ROW_FORMAT=DYNAMIC;
