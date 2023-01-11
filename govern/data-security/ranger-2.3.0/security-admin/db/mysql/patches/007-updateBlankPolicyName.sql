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

-- --------------------------------------------------------------------------------
-- Function which will return tempPolicyCount 
-- which is being used by SP updateBlankPolicyName()
-- --------------------------------------------------------------------------------
DELIMITER $$

DROP PROCEDURE if exists getTempPolicyCount $$
CREATE PROCEDURE `getTempPolicyCount`(IN assetId bigint, IN resId bigint, OUT tempPolicyCount int) 
BEGIN

DECLARE dbResourceId bigint;
DECLARE exitLoop int DEFAULT FALSE;

DECLARE policyList CURSOR FOR 
	SELECT id from x_resource where asset_id = assetId;

DECLARE CONTINUE HANDLER FOR NOT FOUND SET exitLoop = true;
OPEN policyList;
SET tempPolicyCount = 1;
readPolicy : LOOP
	FETCH policyList into dbResourceId;

	IF exitLoop THEN
		set tempPolicyCount = tempPolicyCount + 1;
		LEAVE readPolicy;
	END IF;

	IF (resId = dbResourceId) THEN
		LEAVE readPolicy;
	END IF;
	set tempPolicyCount = tempPolicyCount + 1;

END LOOP;
CLOSE policyList;

END $$


-- --------------------------------------------------------------------------------
-- Procedure that will generate policy name of policies 
-- which were previously created without policy_name
-- --------------------------------------------------------------------------------
DELIMITER $$

DROP PROCEDURE if exists updateBlankPolicyName $$
CREATE PROCEDURE `updateBlankPolicyName`()
BEGIN

DECLARE done INT;
DECLARE resId bigint;
DECLARE assetId bigint;
DECLARE assetName varchar(512);
DECLARE genPolicyName varchar(1000);
DECLARE existPolId varchar(1000);
DECLARE policyCount bigint;
DECLARE currentTime varchar(100);
DECLARE tempPolicyCount int;
DECLARE totalPolicyCount int;
DECLARE resourceName varchar(4000);

-- TrxLog fields
DECLARE createTime datetime;
DECLARE addedById bigint;
DECLARE classType int;
DECLARE objId bigint;
DECLARE parentObjId bigint;
DECLARE parentObjClsType int;
DECLARE parentObjName varchar(1024);
DECLARE objName varchar(1024);
DECLARE attrName varchar(255);
DECLARE prevVal varchar(1024);
DECLARE newVal varchar(1024);
DECLARE trxId varchar(1024);
DECLARE act varchar(255);
DECLARE sessType varchar(30);
DECLARE assetTypeInt int;
DECLARE assetType varchar(50);
DECLARE transId varchar(50);

DECLARE policyList CURSOR FOR 
	select id,asset_id, res_name from x_resource res 
		where res.policy_name is null or res.policy_name = '';

DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;
OPEN policyList;

	SET done = 0;
	
	iLoop : LOOP

	FETCH policyList into resId, assetId, resourceName;

	set assetTypeInt = (select asset_type from x_asset where id = assetId);

	if (assetTypeInt = 1) then
		set assetType = 'HDFS';
	elseif  (assetTypeInt = 2) then
		set assetType = 'HBase';
	elseif ((assetTypeInt = 3)) then
		set assetType = 'Hive';
	elseif (assetTypeInt = 4) then
		set assetType = 'XAAGENT';
	elseif (assetTypeInt = 5) then
		set assetType = 'Knox';
	elseif (assetTypeInt = 6) then
		set assetType = 'Storm';
	end if;

	set totalPolicyCount = (select count(*) from x_resource where asset_id = assetId);
	set assetName = (select asset_name from x_asset asset where asset.id = assetId);
	
	call getTempPolicyCount(assetId, resId, tempPolicyCount);
	set currentTime = DATE_FORMAT(utc_timestamp(), '%Y%m%d%H%i%s');

	set genPolicyName = concat(assetName, '-', tempPolicyCount, '-', currentTime);
	set existPolId = (select id from x_resource where policy_name = genPolicyName);

	if (existPolId != '') then
		set genPolicyName = concat(assetName, '-', totalPolicyCount, '-', currentTime);
	end if;

	if(done = 1) then
		LEAVE iLoop;
	end if;

	UPDATE x_resource set policy_name = genPolicyName where id = resId;

	-- Creating Trx Log
	set createTime = utc_timestamp();
	set addedById = 1;
	set classType = 1001;
	set objId = resId;
	set parentObjId = assetId;
	set parentObjClsType = 1000;
	set objName = resourceName;
	set attrName = 'Policy Name';
	set prevVal = null;
	set newVal = genPolicyName;
	set act = 'update';
	set sessType = 'DB Script';
	set parentObjName = assetName;

	set transId = concat(DATE_FORMAT(utc_timestamp(), '%Y%m%d%H%i%s'), '_', rand());

	insert into x_trx_log (create_time, update_time, added_by_id, upd_by_id, 
		class_type, object_id, parent_object_id, parent_object_class_type, 
		parent_object_name, object_name, attr_name, prev_val, new_val, `action`, 
		trx_id, sess_type) 
		values(createTime, createTime, addedById, addedById, classType, objId, 
		parentObjId, parentObjClsType, parentObjName, objName, attrName, prevVal, 
		newVal, act, transId, sessType);

	insert into x_trx_log (create_time, update_time, added_by_id, upd_by_id, 
		class_type, object_id, parent_object_id, parent_object_class_type, 
		parent_object_name, object_name, attr_name, prev_val, new_val, `action`, 
		trx_id, sess_type) 
		values(createTime, createTime, addedById, addedById, classType, objId, 
		parentObjId, parentObjClsType, parentObjName, objName, 'Repository Type', prevVal, 
		assetType, act, transId, sessType);	
	
	END LOOP;

CLOSE policyList;

END $$
DELIMITER ;
call updateBlankPolicyName();

DROP PROCEDURE if exists getTempPolicyCount;
DROP PROCEDURE if exists updateBlankPolicyName;
