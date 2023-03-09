/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

SET MODE MYSQL;
-- ----------------------------
-- Records of data_connector
-- ----------------------------
INSERT INTO `data_connector` VALUES ('1', '2017-07-12 11:06:47', null, '{\"database\":\"default\",\"table.name\":\"data_avr\"}', 'HIVE', '1.2');
INSERT INTO `data_connector` VALUES ('2', '2017-07-12 11:06:47', null, '{\"database\":\"default\",\"table.name\":\"cout\"}', 'HIVE', '1.2');
INSERT INTO `data_connector` VALUES ('3', '2017-07-12 17:40:30', null, '{\"database\":\"griffin\",\"table.name\":\"avr_in\"}', 'HIVE', '1.2');
INSERT INTO `data_connector` VALUES ('4', '2017-07-12 17:40:30', null, '{\"database\":\"griffin\",\"table.name\":\"avr_out\"}', 'HIVE', '1.2');


-- ----------------------------
-- Records of evaluate_rule
-- ----------------------------
--INSERT INTO `evaluate_rule` VALUES ('1', '2017-07-12 11:06:47', null, '$source[\'uid\'] == $target[\'url\'] AND $source[\'uage\'] == $target[\'createdts\']', '0');
INSERT INTO `evaluate_rule` VALUES ('1', '2017-07-12 11:06:47', null, '$source[''uid''] == $target[''url''] AND $source[''uage''] == $target[''createdts'']', '0');

--INSERT INTO `evaluate_rule` VALUES ('2', '2017-07-12 17:40:30', null, '$source[\'id\'] == $target[\'id\'] AND $source[\'age\'] == $target[\'age\'] AND $source[\'desc\'] == $target[\'desc\']', '0');
INSERT INTO `evaluate_rule` VALUES ('2', '2017-07-12 17:40:30', null, '$source[''id''] == $target[''id''] AND $source[''age''] == $target[''age''] AND $source[''desc''] == $target[''desc'']', '0');


-- ----------------------------
-- Records of measure
-- ----------------------------
INSERT INTO `measure` VALUES ('1', '2017-07-12 11:06:47', null, '0', 'desc1', 'buy_rates_hourly', 'eBay', 'test', 'batch', 'accuracy', '1', '1', '2');
INSERT INTO `measure` VALUES ('2', '2017-07-12 17:40:30', null, '0', 'desc2', 'griffin_aver', 'eBay', 'test', 'batch', 'accuracy', '2', '3', '4');
