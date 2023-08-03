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

DROP TABLE IF EXISTS ranger_masterkey CASCADE;
DROP SEQUENCE IF EXISTS RANGER_MASTERKEY_SEQ;
CREATE SEQUENCE RANGER_MASTERKEY_SEQ;
CREATE TABLE ranger_masterkey(
id BIGINT DEFAULT nextval('RANGER_MASTERKEY_SEQ'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
cipher VARCHAR(255) DEFAULT NULL NULL ,
bitlength INT DEFAULT NULL NULL,
masterkey VARCHAR(2048),
PRIMARY KEY (id),
CONSTRAINT ranger_masterkey_cipher UNIQUE(cipher),
CONSTRAINT ranger_masterkey_bitlength UNIQUE(bitlength)
);

DROP TABLE IF EXISTS ranger_keystore CASCADE;
DROP SEQUENCE IF EXISTS RANGER_KEYSTORE_SEQ;
CREATE SEQUENCE RANGER_KEYSTORE_SEQ;
CREATE TABLE ranger_keystore(
id BIGINT DEFAULT nextval('RANGER_KEYSTORE_SEQ'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
kms_alias VARCHAR(255) NOT NULL,
kms_createdDate BIGINT DEFAULT NULL NULL,
kms_cipher VARCHAR(255) DEFAULT NULL NULL,
kms_bitLength BIGINT DEFAULT NULL NULL,
kms_description VARCHAR(512) DEFAULT NULL NULL,
kms_version BIGINT DEFAULT NULL NULL,
kms_attributes VARCHAR(1024) DEFAULT NULL NULL,
kms_encoded VARCHAR(2048),
PRIMARY KEY (id)
);
