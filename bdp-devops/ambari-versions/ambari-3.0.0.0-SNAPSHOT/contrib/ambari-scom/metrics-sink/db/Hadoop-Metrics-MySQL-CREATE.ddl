/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

/*
Deployment script for HadoopMetrics
*/

delimiter ;

# CREATE DATABASE `ambarimetrics` /*!40100 DEFAULT CHARACTER SET utf8 */;
#
# CREATE USER 'ambari' IDENTIFIED BY 'bigdata';

# USE @schema;

CREATE TABLE CompletedJob (ClusterNodeID INTEGER NOT NULL, TagSetID INTEGER NOT NULL, MapProgressPercent INTEGER NOT NULL, CleanupProgressPercent INTEGER NOT NULL, SetupProgressPercent INTEGER NOT NULL, ReduceProgressPercent INTEGER NOT NULL, RunState INTEGER NOT NULL, StartTime DATETIME NOT NULL, EndTime DATETIME NOT NULL, PRIMARY KEY(ClusterNodeID, TagSetID));
create index IX_CompletedJob_EndTime on CompletedJob(EndTime);
create index IX_CompletedJob_TagSetID on CompletedJob(TagSetID);

CREATE TABLE Configuration (RequestedRefreshRate INTEGER NOT NULL);

CREATE TABLE DatabaseVersion (Major INTEGER NOT NULL, Minor INTEGER NOT NULL, Build INTEGER NOT NULL, Revision INTEGER NOT NULL);

CREATE TABLE MetricName (MetricID INTEGER NOT NULL AUTO_INCREMENT, Name VARCHAR(255), PRIMARY KEY(MetricID));
create index IX_MetricName_Name on MetricName(Name);

CREATE TABLE MetricPair (RecordID BIGINT NOT NULL, MetricID INTEGER NOT NULL, MetricValue VARCHAR(512) NOT NULL, PRIMARY KEY(RecordID, MetricID));

CREATE TABLE MetricRecord (RecordID BIGINT NOT NULL AUTO_INCREMENT, RecordTypeID INTEGER NOT NULL, NodeID INTEGER NOT NULL, SourceIP VARCHAR(255), ClusterNodeID INTEGER NOT NULL, ServiceID INTEGER NOT NULL, TagSetID INTEGER NOT NULL, RecordTimestamp BIGINT NOT NULL, RecordDate DATETIME, PRIMARY KEY(RecordID));
create index IX_MetricRecord_ClusterNodeID on MetricRecord(ClusterNodeID);
create index IX_MetricRecord_NodeID_RecordID on MetricRecord(RecordID);
create index IX_MetricRecord_NodeID_RecordTypeID_ClusterNodeID on MetricRecord(NodeID, RecordTypeID, ClusterNodeID);
create index IX_MetricRecord_NodeID_TagSetID on MetricRecord(TagSetID);

CREATE TABLE Service (ServiceID BIGINT NOT NULL AUTO_INCREMENT, Name VARCHAR(255), PRIMARY KEY(ServiceID));

CREATE TABLE Node (NodeID INTEGER NOT NULL AUTO_INCREMENT, Name VARCHAR(255), LastKnownIP VARCHAR(255), LastNameNodeHeartBeat DATETIME, LastJobTrackerHeartBeat DATETIME, LastDataNodeHeartBeat DATETIME, LastTaskTrackerHeartBeat DATETIME, PRIMARY KEY(NodeID));
create index IX_Node_Name on Node(Name);

CREATE TABLE RecordType (RecordTypeID INTEGER NOT NULL AUTO_INCREMENT, Name VARCHAR(255), Context VARCHAR(255), PRIMARY KEY(RecordTypeID));
create index IX_RecordType_Context_Name on RecordType(Context, Name);

CREATE TABLE TagSet (TagSetID INTEGER NOT NULL AUTO_INCREMENT, TagPairs VARCHAR(512), PRIMARY KEY(TagSetID));
create index IX_TagSet_TagPairs on TagSet(TagPairs);

ALTER TABLE CompletedJob ADD CONSTRAINT FK_CompletedJob_TagSet_TagSetID FOREIGN KEY (TagSetID) REFERENCES TagSet (TagSetID) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE MetricPair ADD CONSTRAINT FK_MetricPair_MetricName_MetricID FOREIGN KEY (MetricID) REFERENCES MetricName (MetricID) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE MetricPair ADD CONSTRAINT FK_MetricPair_MetricRecord_RecordID FOREIGN KEY (RecordID) REFERENCES MetricRecord (RecordID) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE MetricRecord ADD CONSTRAINT FK_MetricRecord_Node_NodeID FOREIGN KEY (NodeID) REFERENCES Node (NodeID) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE MetricRecord ADD CONSTRAINT FK_MetricRecord_RecordType_RecordTypeID FOREIGN KEY (RecordTypeID) REFERENCES RecordType (RecordTypeID) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE MetricRecord ADD CONSTRAINT FK_MetricRecord_TagSet_TagSetID FOREIGN KEY (TagSetID) REFERENCES TagSet (TagSetID) ON DELETE NO ACTION ON UPDATE NO ACTION;

DROP procedure IF EXISTS `uspInsertMetricValue`;

DELIMITER $$

CREATE DEFINER=`ambari`@`%` PROCEDURE `uspInsertMetricValue`(recordID bigint, metricName nvarchar(256), metricValue nvarchar(512))
proc_label:BEGIN
    DECLARE metricID INT;
    DECLARE has_error INT DEFAULT 0;

    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION SET has_error = 1;

    IF (recordID IS NULL OR metricName IS NULL) THEN
      LEAVE proc_label;
    END IF;

    START TRANSACTION;
		SELECT MetricID FROM MetricName WHERE `Name` = metricName INTO metricID;
		IF (metricID IS NULL) THEN
			INSERT INTO MetricName (`Name`) VALUES (metricName);
			SELECT LAST_INSERT_ID() INTO metricID;
			IF (has_error = 1) THEN
				ROLLBACK;
				LEAVE proc_label;
			END IF;
		END IF;
		COMMIT;

		INSERT INTO MetricPair (RecordID, MetricID, MetricValue) VALUES (recordID, metricID, metricValue);
		LEAVE proc_label;

	END;
$$
DELIMITER ;

DROP procedure IF EXISTS `uspUpdateHeartBeats`;
DELIMITER $$

CREATE PROCEDURE uspUpdateHeartBeats(NodeID int, SourceIP varchar(256), NameNodeLast datetime, JobTrackerLast datetime,
		DataNodeLast datetime, TaskTrackerLast datetime, LastKnownIP varchar(256))

	BEGIN
		IF (NodeID IS NOT NULL) THEN
			IF (NameNodeLast IS NOT NULL) THEN
				UPDATE Node as n SET n.LastNameNodeHeartBeat = NameNodeLast WHERE n.NodeID = NodeID;
			END IF;
			IF (JobTrackerLast IS NOT NULL) THEN
				UPDATE Node as n SET n.LastJobTrackerHeartBeat = JobTrackerLast WHERE n.NodeID = NodeID;
			END IF;
			IF (DataNodeLast IS NOT NULL) THEN
				UPDATE Node as n SET n.LastDataNodeHeartBeat = DataNodeLast WHERE n.NodeID = NodeID;
			END IF;
			IF (TaskTrackerLast IS NOT NULL) THEN
				UPDATE Node as n SET n.LastTaskTrackerHeartBeat = TaskTrackerLast WHERE n.NodeID = NodeID;
			END IF;
			IF (LastKnownIP IS NULL OR SourceIP <> LastKnownIP) THEN
				UPDATE Node as n SET n.LastKnownIP = SourceIP WHERE n.NodeID = NodeID;
			END IF;
		END IF;
	END;
$$

DELIMITER ;


DROP procedure IF EXISTS `uspGetMetricRecord`;
DELIMITER $$

CREATE DEFINER=`ambari`@`%` PROCEDURE `uspGetMetricRecord`(
		recordTypeContext varchar(255),
		recordTypeName varchar(255),
		nodeName varchar(255),
		sourceIP varchar(255),
		clusterNodeName varchar(255),
		serviceName varchar(255),
		tagPairs varchar(512),
		recordTimestamp bigint,
		OUT metricRecordID bigint)
proc_label: BEGIN

		DECLARE recordTypeID int;
		DECLARE nodeID int;
		DECLARE clusterNodeID int;
		DECLARE tagSetID int;
		DECLARE serviceID int;
		DECLARE err int default 0;
		DECLARE recordIDCutoff bigint;

		DECLARE CONTINUE HANDLER FOR SQLEXCEPTION SET err = 1;

		START TRANSACTION;
		SELECT max(r.RecordTypeID) INTO recordTypeID FROM RecordType as r WHERE r.`Context` = recordTypeContext AND r.`Name` = recordTypeName;

		IF (recordTypeID IS NULL OR recordTypeID = 0) THEN
				INSERT INTO RecordType (`Context`, `Name`) VALUES (recordTypeContext, recordTypeName);
				SELECT LAST_INSERT_ID() INTO recordTypeID;
				IF (err <> 0) THEN
					ROLLBACK;
					SET metricRecordID = NULL;
					LEAVE proc_label;
				END IF;
		END IF;
		COMMIT;

		START TRANSACTION;
		SELECT max(s.serviceID) INTO serviceID FROM Service as s WHERE s.`Name` = serviceName;
		IF (serviceID IS NULL OR serviceID  = 0) THEN
				INSERT INTO Service (`Name`) VALUES (serviceName);
				SELECT LAST_INSERT_ID() INTO serviceID;
				IF (err <> 0) THEN
					ROLLBACK;
					SET metricRecordID = NULL;
					LEAVE proc_label;
				END IF;
		END IF;
		COMMIT;

		START TRANSACTION;
		SELECT max(n.NodeID) INTO nodeID FROM Node as n WHERE n.`Name` = nodeName;

		IF (nodeID IS NULL OR nodeID  = 0) THEN
			/* Start with a node type of uninitialized.  HealthNode will determine node type based on metrics delivered over time. */
				INSERT INTO Node (`Name`, LastKnownIP) VALUES (nodeName, sourceIP);
				SELECT LAST_INSERT_ID() INTO nodeID;
				IF (err <> 0) THEN
					ROLLBACK;
					SET metricRecordID = NULL;
					LEAVE proc_label;
				END IF;
		END IF;
		COMMIT;

		-- Do our best to determine the cluster node ID based on completely flakey input from user which might be an IP address, a non-FQDN,
		-- or an FQDN.  Note that worker nodes may have a completely different idea about the name of the namenode (which is the node
		-- which represents the cluster) compared with the namenode itself

		START TRANSACTION;
		IF ((SELECT ufnIsIPAddress(clusterNodeName)) = 1) THEN
			SELECT n.NodeID from Node as n WHERE n.LastKnownIP = clusterNodeName ORDER BY n.LastNameNodeHeartBeat DESC limit 1 INTO clusterNodeID;
			IF (clusterNodeID IS NULL) THEN
				INSERT INTO Node (`Name`, LastKnownIP) VALUES (clusterNodeName, sourceIP);
				SELECT LAST_INSERT_ID() INTO clusterNodeID;
				IF (err <> 0) THEN
					ROLLBACK;
					SET metricRecordID = NULL;
					LEAVE proc_label;
				END IF;
			END IF;
		ELSEIF ((SELECT LOCATE('.', clusterNodeName, 1)) > 0) THEN
			-- IF this is not an IP address, but there is a dot in the name we assume we are looking at an FQDN
			SELECT max(n.NodeID) FROM Node as n WHERE n.`Name` = clusterNodeName INTO clusterNodeID;
			IF (clusterNodeID IS NULL OR clusterNodeID = 0) THEN
				INSERT INTO Node (`Name`, LastKnownIP) VALUES (clusterNodeName, sourceIP);
				SELECT LAST_INSERT_ID() INTO clusterNodeID;
				IF (err <> 0) THEN
					ROLLBACK;
					SET metricRecordID = NULL;
					LEAVE proc_label;
				END IF;
			END IF;
		ELSE
		BEGIN
			-- We have got a non-FQDN, but the NameNode might know its FQDN, so be careful! We must prefer the FQDN if we can find one.
			-- Sadly, yes, this could break things if we are monitoring clusters from different domains.  This is now by design!
			SELECT n.NodeID FROM Node as n WHERE n.`Name` LIKE CONCAT(clusterNodeName, '%') ORDER BY n.LastNameNodeHeartBeat DESC limit 1 INTO clusterNodeID;
			IF (clusterNodeID IS NULL) THEN
				SELECT n.NodeID FROM Node as n WHERE n.`Name` = clusterNodeName INTO clusterNodeID;
				if (clusterNodeID IS NULL) THEN
					INSERT INTO Node (`Name`, LastKnownIP) VALUES (clusterNodeName, sourceIP);
					SELECT LAST_INSERT_ID() INTO clusterNodeID;
					IF (err <> 0) THEN
						ROLLBACK;
						SET metricRecordID = NULL;
						LEAVE proc_label;
					END IF;
				END IF;
			END IF;
		END;
		END IF;
		COMMIT;

		-- Cleanup older metric records and pairs if necessary
		-- Policy is to keep between 60000 and 90000 metric records and associated metric pairs per node.
		IF ((SELECT COUNT(*) FROM MetricRecord as mr WHERE mr.NodeID = nodeID) > 90000) THEN
			SELECT MIN(mr.RecordID) FROM MetricRecord as mr WHERE mr.RecordID IN (select * from (SELECT r.RecordID FROM MetricRecord as r WHERE r.NodeID = nodeID ORDER BY r.RecordDate DESC limit 60000) as records) INTO recordIDCutoff;
			IF (recordIDCutoff IS NOT NULL) THEN
				DELETE FROM MetricPair WHERE RecordID IN (
				SELECT RecordID FROM MetricPair as mp
				JOIN MetricRecord as mr ON mp.RecordID = mr.RecordID
				WHERE mr.RecordID < @recordIDCutoff AND mr.NodeID = @nodeID);

				DELETE FROM MetricRecord
				WHERE RecordID < recordIDCutoff AND NodeID = nodeID;
			END IF;
		END IF;


		START TRANSACTION;
		SELECT max(t.TagSetID) FROM TagSet as t WHERE t.TagPairs = tagPairs INTO tagSetID;
		IF (tagSetID IS NULL OR tagSetID = 0) THEN
				INSERT INTO TagSet (TagPairs) VALUES (tagPairs);
				SELECT LAST_INSERT_ID() INTO tagSetID;
				IF (err <> 0) THEN
					ROLLBACK;
					SET metricRecordID = NULL;
					LEAVE proc_label;
				END IF;
		END IF;
		COMMIT;

		START TRANSACTION;
		SELECT max(mr.RecordID) FROM MetricRecord as mr WHERE mr.RecordTypeID = recordTypeID AND mr.NodeID = nodeID AND mr.ServiceID = serviceID AND mr.TagSetID = tagSetID AND mr.RecordTimestamp = recordTimestamp INTO metricRecordID;
		IF (metricRecordID IS NULL OR metricRecordID = 0) THEN
			-- insert into temp_log values (CONCAT(metricRecordID, ', ', RecordTimestamp));
			INSERT INTO MetricRecord (RecordTypeID, NodeID, SourceIP, ClusterNodeID, ServiceID, TagSetID, RecordTimestamp) VALUES (recordTypeID, nodeID, sourceIP, clusterNodeID, serviceID, tagSetID, recordTimestamp);
			SELECT LAST_INSERT_ID() INTO metricRecordID;
			IF (err <> 0) THEN
				ROLLBACK;
				SET metricRecordID = NULL;
				LEAVE proc_label;
			END IF;
		END IF;
		COMMIT;

	END;
$$

DELIMITER ;

DELIMITER $$

CREATE DEFINER=`ambari`@`%` FUNCTION `ufnIsIPAddress`(inputString varchar(1024)) RETURNS tinyint(4)
BEGIN
	DECLARE currentPos bigint default 1;
	DECLARE nextPos bigint default 0;
	DECLARE count int default 0;

	if (CHAR_LENGTH(inputString) = 0) THEN
		RETURN 0;
	END IF;

	SELECT LOCATE('.', inputString, currentPos) INTO nextPos;

	while_label: WHILE (nextPos < CHAR_LENGTH(inputString) AND count < 4) DO
		IF (nextPos = 0) THEN
			SET nextPos = CHAR_LENGTH(inputString);
		END IF;
		IF ((SELECT SUBSTRING(inputString, currentPos, nextPos - currentPos) REGEXP '[0-9]+') = 1) THEN
			SET count = count + 1;
			SET currentPos = nextPos;
			SELECT LOCATE(inputString, '.', currentPos + 1) INTO nextPos;
		ELSE
			LEAVE while_label;
		END IF;
	END WHILE;

	IF (count = 4) THEN
		RETURN 1;
	END IF;

	SET currentPos = 1;
	SET nextPos = 0;
	SET count = 0;

	WHILE (currentPos <= CHAR_LENGTH(inputString)) DO
		IF ((SELECT SUBSTRING(inputString, currentPos, 1) REGEXP '[0-9A-Fa-f:]') = 1) THEN
			IF (SUBSTRING(inputString, currentPos, 1) = ':') THEN
				SET count = count + 1;
			END IF;
			SET currentPos = currentPos + 1;
		ELSE RETURN 0;
		END IF;
	END WHILE;
	IF (count >= 4) THEN
		RETURN 1;
	END IF;

	RETURN 0;
END;
$$

DELIMITER ;

DROP procedure IF EXISTS `uspPurgeMetrics`;

DELIMITER $$
CREATE PROCEDURE uspPurgeMetrics(noOfDays bigint)
	proc_label: BEGIN

		DECLARE recordIDCutOff BIGINT;
		DECLARE has_error INT default 0;
		DECLARE CONTINUE HANDLER FOR SQLEXCEPTION SET has_error = 1;

		IF (noOfDays IS NULL OR noOfDays < 1) THEN
			LEAVE proc_label;
		END IF;

		SELECT MAX(RecordID) FROM MetricRecord WHERE DateDiff(RecordDate, CURRENT_TIMESTAMP) >= noOfDays INTO recordIDCutoff;

		IF (recordIDCutoff IS NOT NULL) THEN

			START TRANSACTION;

			DELETE FROM MetricPair WHERE RecordID <= recordIDCutoff;
			DELETE FROM MetricRecord WHERE RecordID <= recordIDCutoff;

			IF (has_error <> 0) then
				ROLLBACK;
				LEAVE proc_label;
			END IF;

			COMMIT;
		END IF;
  END;
$$

DELIMITER ;

DROP procedure IF EXISTS `ufGetMetrics`;
DELIMITER $$

CREATE DEFINER=`ambari`@`%` PROCEDURE `ufGetMetrics`(startTimeStamp bigint,
		 endTimeStamp bigint,
		 recordTypeContext VARCHAR(256),
		 recordTypeName VARCHAR(256),
		 metricName VARCHAR(256),
		 serviceComponentName VARCHAR(256),
		 nodeName VARCHAR(256)
		)
BEGIN
		SELECT * from (
			SELECT  s.RecordTimeStamp AS RecordTimeStamp,
					mp.MetricValue AS MetricValue
			FROM MetricPair mp
			INNER JOIN (SELECT	mr.RecordID AS RecordID,
								mr.RecordTimeStamp AS RecordTimeStamp
						FROM MetricRecord mr
						INNER JOIN RecordType rt ON (mr.RecordTypeId = rt.RecordTypeId)
						INNER JOIN Node nd ON (mr.NodeID = nd.NodeID)
						INNER JOIN Service sr ON (mr.ServiceID = sr.ServiceID)
						WHERE rt.Context = recordTypeContext
						AND rt.Name = recordTypeName
						AND (nd.Name = nodeName)
						AND (sr.Name = serviceComponentName)
						AND mr.RecordTimestamp >= startTimeStamp
						AND mr.RecordTimestamp <= endTimeStamp
						) s ON (mp.RecordID = s.RecordID)
			INNER JOIN MetricName mn ON (mp.MetricID = mn.MetricID)
			WHERE (mn.Name = metricName)
		) as mp_table;
END;
$$

DELIMITER ;

DROP procedure IF EXISTS `ufGetAggregatedServiceMetrics`;
DELIMITER $$

CREATE PROCEDURE ufGetAggregatedServiceMetrics(
		startTimeStamp bigint,
		endTimeStamp bigint,
		recordTypeContext NVARCHAR(256),
		recordTypeName NVARCHAR(256),
		metricName NVARCHAR(256),
		serviceComponentName NVARCHAR(256),
		period integer
		)
BEGIN
		SELECT * FROM
		(
			SELECT FLOOR ((mr.RecordTimeStamp - startTimeStamp) / period) TimeStampBlock, MAX(mr.RecordTimeStamp) RecordTimeStamp,  SUM(CAST(MetricValue AS DECIMAL(18,4))) AggMetricValue
			FROM MetricPair mp
			INNER JOIN MetricRecord mr ON (mp.RecordID = mr.RecordID)
			INNER JOIN RecordType rt ON (rt.RecordTypeID = mr.RecordTypeID)
			INNER JOIN MetricName mn ON (mn.MetricID = mp.MetricID)
			INNER JOIN Service sr ON (sr.ServiceID = mr.ServiceID)
			WHERE mr.RecordTimestamp >= startTimeStamp
			AND mr.RecordTimestamp <= endTimeStamp
			AND mn.Name = metricName
			AND rt.Context = recordTypeContext
			AND rt.Name = recordTypeName
			AND sr.Name = serviceComponentName
			GROUP BY FLOOR ((mr.RecordTimeStamp - startTimeStamp) / period)
		) as mp_table;
END;
$$

DELIMITER ;






