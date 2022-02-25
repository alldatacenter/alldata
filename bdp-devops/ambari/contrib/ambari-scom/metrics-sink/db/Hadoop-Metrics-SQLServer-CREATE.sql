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
Schema population script for $(METRICSDBNAME)

Use this script in sqlcmd mode, setting the environment variables like this:
set METRICSDBNAME=HadoopMetrics

sqlcmd -S localhost\SQLEXPRESS -i C:\app\ambari-server-1.3.0-SNAPSHOT\resources\Hadoop-Metrics-SQLServer-CREATE.sql
*/

USE [$(METRICSDBNAME)]
GO

SET QUOTED_IDENTIFIER ON;
GO

IF NOT EXISTS(SELECT name FROM sys.objects WHERE name = N'CompletedJob' and type_desc = N'USER_TABLE')
BEGIN
	PRINT N'Creating [dbo].[CompletedJob]...';
	CREATE TABLE [dbo].[CompletedJob] (
		[ClusterNodeID]          INT      NOT NULL,
		[TagSetID]               INT      NOT NULL,
		[MapProgressPercent]     INT      NOT NULL,
		[CleanupProgressPercent] INT      NOT NULL,
		[SetupProgressPercent]   INT      NOT NULL,
		[ReduceProgressPercent]  INT      NOT NULL,
		[RunState]               INT      NOT NULL,
		[StartTime]              DATETIME NOT NULL,
		[EndTime]                DATETIME NOT NULL
	);
END
GO

IF NOT EXISTS(SELECT name FROM sys.indexes WHERE name = N'PK_CompletedJob_ClusterNodeID_TagSetID' AND type_desc = N'CLUSTERED' AND IS_PRIMARY_KEY=N'1')
BEGIN
	PRINT N'Creating [dbo].[CompletedJob].[PK_CompletedJob_ClusterNodeID_TagSetID]...';
	ALTER TABLE [dbo].[CompletedJob]
		ADD CONSTRAINT [PK_CompletedJob_ClusterNodeID_TagSetID] PRIMARY KEY CLUSTERED ([ClusterNodeID] ASC, [TagSetID] ASC);
END
GO

IF NOT EXISTS(SELECT name FROM sys.indexes WHERE name = N'IX_CompletedJob_EndTime' AND type_desc = N'NONCLUSTERED')
BEGIN
	PRINT N'Creating [dbo].[CompletedJob].[IX_CompletedJob_EndTime]...';
	CREATE NONCLUSTERED INDEX [IX_CompletedJob_EndTime]
		ON [dbo].[CompletedJob]([EndTime] ASC) ;
END
GO

IF NOT EXISTS(SELECT name FROM sys.indexes WHERE name = N'IX_CompletedJob_TagSetID' AND type_desc = N'NONCLUSTERED')
BEGIN
	PRINT N'Creating [dbo].[CompletedJob].[IX_CompletedJob_TagSetID]...';
	CREATE NONCLUSTERED INDEX [IX_CompletedJob_TagSetID]
		ON [dbo].[CompletedJob]([TagSetID] ASC) ;
END
GO

IF NOT EXISTS(SELECT name FROM sys.objects WHERE name = N'Configuration' and type_desc = N'USER_TABLE')
BEGIN
	PRINT N'Creating [dbo].[Configuration]...';
	CREATE TABLE [dbo].[Configuration] (
		[RequestedRefreshRate] INT NOT NULL
	);
END
GO

IF NOT EXISTS(SELECT name FROM sys.objects WHERE name = N'DatabaseVersion' and type_desc = N'USER_TABLE')
BEGIN
	PRINT N'Creating [dbo].[DatabaseVersion]...';
	CREATE TABLE [dbo].[DatabaseVersion] (
		[Major]    INT NOT NULL,
		[Minor]    INT NOT NULL,
		[Build]    INT NOT NULL,
		[Revision] INT NOT NULL
	);
END
GO

IF NOT EXISTS(SELECT name FROM sys.objects WHERE name = N'MetricName' and type_desc = N'USER_TABLE')
BEGIN
	PRINT N'Creating [dbo].[MetricName]...';
	CREATE TABLE [dbo].[MetricName] (
		[MetricID] INT            IDENTITY (1, 1) NOT NULL,
		[Name]     NVARCHAR (256) NOT NULL,
		PRIMARY KEY CLUSTERED ([MetricID] ASC)
	);
END
GO

IF NOT EXISTS(SELECT name FROM sys.indexes WHERE name = N'IX_MetricName_Name' AND type_desc = N'NONCLUSTERED')
BEGIN
	PRINT N'Creating [dbo].[MetricName].[IX_MetricName_Name]...';
	CREATE UNIQUE NONCLUSTERED INDEX [IX_MetricName_Name]
		ON [dbo].[MetricName]([Name] ASC) ;
END
GO

IF NOT EXISTS(SELECT name FROM sys.objects WHERE name = N'MetricPair' and type_desc = N'USER_TABLE')
BEGIN
	PRINT N'Creating [dbo].[MetricPair]...';
	CREATE TABLE [dbo].[MetricPair] (
		[RecordID]    BIGINT         NOT NULL,
		[MetricID]    INT            NOT NULL,
		[MetricValue] NVARCHAR (512) NOT NULL
	);
END
GO

IF NOT EXISTS(SELECT name FROM sys.indexes WHERE name = N'UX_MetricPair_RecordID_MetricID' AND type_desc = N'CLUSTERED')
BEGIN
	PRINT N'Creating [dbo].[MetricPair].[UX_MetricPair_RecordID_MetricID]...';
	CREATE UNIQUE CLUSTERED INDEX [UX_MetricPair_RecordID_MetricID]
		ON [dbo].[MetricPair]([RecordID] ASC, [MetricID] ASC) ;
END
GO

IF NOT EXISTS(SELECT name FROM sys.objects WHERE name = N'MetricRecord' and type_desc = N'USER_TABLE')
BEGIN
	PRINT N'Creating [dbo].[MetricRecord]...';
	CREATE TABLE [dbo].[MetricRecord] (
		[RecordID]        BIGINT         IDENTITY (1, 1) NOT NULL,
		[RecordTypeID]    INT            NOT NULL,
		[NodeID]          INT            NOT NULL,
		[SourceIP]        NVARCHAR (256) NULL,
		[ClusterNodeID]   INT            NOT NULL,
		[ServiceID]       INT            NOT NULL,
		[TagSetID]        INT            NOT NULL,
		[RecordTimestamp] BIGINT         NOT NULL,
		[RecordDate]      AS             DATEADD(second, CONVERT (INT, RecordTimestamp / 1000), CONVERT (DATETIME, '1970-01-01T00:00:00.000', 126)) PERSISTED,
		PRIMARY KEY CLUSTERED ([RecordID] ASC)
	);
END
GO


IF NOT EXISTS(SELECT name FROM sys.indexes WHERE name = N'IX_MetricRecord_ClusterNodeID' AND type_desc = N'NONCLUSTERED')
BEGIN
	PRINT N'Creating [dbo].[MetricRecord].[IX_MetricRecord_ClusterNodeID]...';
	CREATE NONCLUSTERED INDEX [IX_MetricRecord_ClusterNodeID]
		ON [dbo].[MetricRecord]([ClusterNodeID] ASC) ;
END
GO

IF NOT EXISTS(SELECT name FROM sys.indexes WHERE name = N'IX_MetricRecord_NodeID_RecordID' AND type_desc = N'NONCLUSTERED')
BEGIN
	PRINT N'Creating [dbo].[MetricRecord].[IX_MetricRecord_NodeID_RecordID]...';
	CREATE NONCLUSTERED INDEX [IX_MetricRecord_NodeID_RecordID]
		ON [dbo].[MetricRecord]([NodeID] ASC, [RecordID] ASC) ;
END
GO

IF NOT EXISTS(SELECT name FROM sys.indexes WHERE name = N'IX_MetricRecord_NodeID_RecordTypeID_ClusterNodeID' AND type_desc = N'NONCLUSTERED')
BEGIN
	PRINT N'Creating [dbo].[MetricRecord].[IX_MetricRecord_NodeID_RecordTypeID_ClusterNodeID]...';
	CREATE NONCLUSTERED INDEX [IX_MetricRecord_NodeID_RecordTypeID_ClusterNodeID]
		ON [dbo].[MetricRecord]([NodeID] ASC, [RecordTypeID] ASC, [ClusterNodeID] ASC)
		INCLUDE([RecordDate]) ;
END
GO

IF NOT EXISTS(SELECT name FROM sys.indexes WHERE name = N'IX_MetricRecord_NodeID_TagSetID' AND type_desc = N'NONCLUSTERED')
BEGIN
	PRINT N'Creating [dbo].[MetricRecord].[IX_MetricRecord_NodeID_TagSetID]...';
	CREATE NONCLUSTERED INDEX [IX_MetricRecord_NodeID_TagSetID]
		ON [dbo].[MetricRecord]([NodeID] ASC, [TagSetID] ASC) ;
END
GO

IF NOT EXISTS(SELECT name FROM sys.indexes WHERE name = N'IX_MetricRecord_RecordDate' AND type_desc = N'NONCLUSTERED')
BEGIN
	PRINT N'Creating [dbo].[MetricRecord].[IX_MetricRecord_RecordDate]...';
	CREATE NONCLUSTERED INDEX [IX_MetricRecord_RecordDate]
		ON [dbo].[MetricRecord]([RecordDate] ASC) ;
END
GO

IF NOT EXISTS(SELECT name FROM sys.indexes WHERE name = N'IX_MetricRecord_RecordTimestamp_NodeID_RecordTypeID' AND type_desc = N'NONCLUSTERED')
BEGIN
	PRINT N'Creating [dbo].[MetricRecord].[IX_MetricRecord_RecordTimestamp_NodeID_RecordTypeID]...';
	CREATE NONCLUSTERED INDEX [IX_MetricRecord_RecordTimestamp_NodeID_RecordTypeID]
		ON [dbo].[MetricRecord]([RecordTimestamp] DESC, [NodeID] ASC, [RecordTypeID] ASC)
		INCLUDE([RecordID]) ;
END
GO

IF NOT EXISTS(SELECT name FROM sys.indexes WHERE name = N'IX_MetricRecord_RecordTypeID' AND type_desc = N'NONCLUSTERED')
BEGIN
	PRINT N'Creating [dbo].[MetricRecord].[IX_MetricRecord_RecordTypeID]...';
	CREATE NONCLUSTERED INDEX [IX_MetricRecord_RecordTypeID]
		ON [dbo].[MetricRecord]([RecordTypeID] ASC) ;
END
GO

IF NOT EXISTS(SELECT name FROM sys.indexes WHERE name = N'IX_MetricRecord_RecordTypeID_ClusterNodeID_ServiceID_TagSetID_RecordTimestamp' AND type_desc = N'NONCLUSTERED')
BEGIN
	PRINT N'Creating [dbo].[MetricRecord].[IX_MetricRecord_RecordTypeID_ClusterNodeID_ServiceID_TagSetID_RecordTimestamp]...';
	CREATE NONCLUSTERED INDEX [IX_MetricRecord_RecordTypeID_ClusterNodeID_ServiceID_TagSetID_RecordTimestamp]
		ON [dbo].[MetricRecord]([RecordTypeID] ASC, [ClusterNodeID] ASC, [ServiceID] ASC, [TagSetID] ASC, [RecordTimestamp] DESC) ;
END
GO

IF NOT EXISTS(SELECT name FROM sys.indexes WHERE name = N'IX_MetricRecord_TagSetID' AND type_desc = N'NONCLUSTERED')
BEGIN
	PRINT N'Creating [dbo].[MetricRecord].[IX_MetricRecord_TagSetID]...';
	CREATE NONCLUSTERED INDEX [IX_MetricRecord_TagSetID]
		ON [dbo].[MetricRecord]([TagSetID] ASC) ;
END
GO

IF NOT EXISTS(SELECT name FROM sys.indexes WHERE name = N'UX_MetricRecord_RecordTypeID_NodeID_TagSetID_RecordTimestamp' AND type_desc = N'NONCLUSTERED')
BEGIN
	PRINT N'Creating [dbo].[MetricRecord].[UX_MetricRecord_RecordTypeID_NodeID_TagSetID_RecordTimestamp]...';
	CREATE UNIQUE NONCLUSTERED INDEX [UX_MetricRecord_RecordTypeID_NodeID_TagSetID_RecordTimestamp]
		ON [dbo].[MetricRecord]([RecordTypeID] ASC, [NodeID] ASC, [TagSetID] ASC, [RecordTimestamp] ASC) ;
END
GO

IF NOT EXISTS(SELECT name FROM sys.objects WHERE name = N'Service' and type_desc = N'USER_TABLE')
BEGIN
	PRINT N'Creating [dbo].[Service]...';
	CREATE TABLE [dbo].[Service] (
		[ServiceID]        BIGINT         IDENTITY (1, 1) NOT NULL,
		[Name]             NVARCHAR (256),
		PRIMARY KEY CLUSTERED ([ServiceID] ASC)
	);
END
GO

IF NOT EXISTS(SELECT name FROM sys.objects WHERE name = N'Node' and type_desc = N'USER_TABLE')
BEGIN
	PRINT N'Creating [dbo].[Node]...';
	CREATE TABLE [dbo].[Node] (
		[NodeID]                   INT            IDENTITY (1, 1) NOT NULL,
		[Name]                     NVARCHAR (256) NOT NULL,
		[LastKnownIP]              NVARCHAR (256) NULL,
		[LastNameNodeHeartBeat]    DATETIME       NULL,
		[LastJobTrackerHeartBeat]  DATETIME       NULL,
		[LastDataNodeHeartBeat]    DATETIME       NULL,
		[LastTaskTrackerHeartBeat] DATETIME       NULL,
		PRIMARY KEY CLUSTERED ([NodeID] ASC)
	);
END
GO

IF NOT EXISTS(SELECT name FROM sys.indexes WHERE name = N'IX_Node_Name' AND type_desc = N'NONCLUSTERED')
BEGIN
	PRINT N'Creating [dbo].[Node].[IX_Node_Name]...';
	CREATE UNIQUE NONCLUSTERED INDEX [IX_Node_Name]
		ON [dbo].[Node]([Name] ASC) ;
END
GO

IF NOT EXISTS(SELECT name FROM sys.objects WHERE name = N'RecordType' and type_desc = N'USER_TABLE')
BEGIN
	PRINT N'Creating [dbo].[RecordType]...';
	CREATE TABLE [dbo].[RecordType] (
		[RecordTypeID] INT            IDENTITY (1, 1) NOT NULL,
		[Name]         NVARCHAR (225) NOT NULL,
		[Context]      NVARCHAR (225) NOT NULL,
		PRIMARY KEY CLUSTERED ([RecordTypeID] ASC)
	);
END
GO

IF NOT EXISTS(SELECT name FROM sys.indexes WHERE name = N'IX_RecordType_Context_Name' AND type_desc = N'NONCLUSTERED')
BEGIN
	PRINT N'Creating [dbo].[RecordType].[IX_RecordType_Context_Name]...';
	CREATE UNIQUE NONCLUSTERED INDEX [IX_RecordType_Context_Name]
		ON [dbo].[RecordType]([Context] ASC, [Name] ASC) ;
END
GO

IF NOT EXISTS(SELECT name FROM sys.objects WHERE name = N'TagSet' and type_desc = N'USER_TABLE')
BEGIN
	PRINT N'Creating [dbo].[TagSet]...';
	CREATE TABLE [dbo].[TagSet] (
		[TagSetID] INT            IDENTITY (1, 1) NOT NULL,
		[TagPairs] NVARCHAR (450) NOT NULL,
		PRIMARY KEY CLUSTERED ([TagSetID] ASC)
	);
END
GO

IF NOT EXISTS(SELECT name FROM sys.indexes WHERE name = N'IX_TagSet_TagPairs' AND type_desc = N'NONCLUSTERED')
BEGIN
	PRINT N'Creating [dbo].[TagSet].[IX_TagSet_TagPairs]...';
	CREATE UNIQUE NONCLUSTERED INDEX [IX_TagSet_TagPairs]
		ON [dbo].[TagSet]([TagPairs] ASC) ;
END
GO

IF NOT EXISTS (SELECT name FROM sys.foreign_keys WHERE name = N'FK_CompletedJob_TagSet_TagSetID')
BEGIN
	PRINT N'Creating FK_CompletedJob_TagSet_TagSetID...';
	ALTER TABLE [dbo].[CompletedJob] WITH NOCHECK
		ADD CONSTRAINT [FK_CompletedJob_TagSet_TagSetID] FOREIGN KEY ([TagSetID]) REFERENCES [dbo].[TagSet] ([TagSetID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
END
GO

IF NOT EXISTS (SELECT name FROM sys.foreign_keys WHERE name = N'FK_MetricPair_MetricName_MetricID')
BEGIN
	PRINT N'Creating FK_MetricPair_MetricName_MetricID...';
	ALTER TABLE [dbo].[MetricPair] WITH NOCHECK
		ADD CONSTRAINT [FK_MetricPair_MetricName_MetricID] FOREIGN KEY ([MetricID]) REFERENCES [dbo].[MetricName] ([MetricID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
END
GO

IF NOT EXISTS (SELECT name FROM sys.foreign_keys WHERE name = N'FK_MetricPair_MetricRecord_RecordID')
BEGIN
	PRINT N'Creating FK_MetricPair_MetricRecord_RecordID...';
	ALTER TABLE [dbo].[MetricPair] WITH NOCHECK
		ADD CONSTRAINT [FK_MetricPair_MetricRecord_RecordID] FOREIGN KEY ([RecordID]) REFERENCES [dbo].[MetricRecord] ([RecordID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
END
GO

IF NOT EXISTS (SELECT name FROM sys.foreign_keys WHERE name = N'FK_MetricRecord_Node_NodeID')
BEGIN
	PRINT N'Creating FK_MetricRecord_Node_NodeID...';
	ALTER TABLE [dbo].[MetricRecord] WITH NOCHECK
		ADD CONSTRAINT [FK_MetricRecord_Node_NodeID] FOREIGN KEY ([NodeID]) REFERENCES [dbo].[Node] ([NodeID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
END
GO

IF NOT EXISTS (SELECT name FROM sys.foreign_keys WHERE name = N'FK_MetricRecord_RecordType_RecordTypeID')
BEGIN
	PRINT N'Creating FK_MetricRecord_RecordType_RecordTypeID...';
	ALTER TABLE [dbo].[MetricRecord] WITH NOCHECK
		ADD CONSTRAINT [FK_MetricRecord_RecordType_RecordTypeID] FOREIGN KEY ([RecordTypeID]) REFERENCES [dbo].[RecordType] ([RecordTypeID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
END
GO

IF NOT EXISTS (SELECT name FROM sys.foreign_keys WHERE name = N'FK_MetricRecord_TagSet_TagSetID')
BEGIN
	PRINT N'Creating FK_MetricRecord_TagSet_TagSetID...';
	ALTER TABLE [dbo].[MetricRecord] WITH NOCHECK
		ADD CONSTRAINT [FK_MetricRecord_TagSet_TagSetID] FOREIGN KEY ([TagSetID]) REFERENCES [dbo].[TagSet] ([TagSetID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
END
GO

IF NOT EXISTS(SELECT name FROM sys.objects WHERE name = N'uspInsertMetricValue' and type_desc = N'SQL_STORED_PROCEDURE')
BEGIN
	PRINT N'Creating [dbo].[uspInsertMetricValue]...';
	exec('CREATE PROCEDURE [dbo].[uspInsertMetricValue]
		@recordID bigint,
		@metricName nvarchar(256),
		@metricValue nvarchar(512)
	AS
	BEGIN
		SET NOCOUNT ON;

		DECLARE @metricID int;
		DECLARE @err int;

		IF @recordID IS NULL OR @metricName IS NULL RETURN;

		BEGIN TRANSACTION;
		SELECT @metricID = MetricID FROM MetricName WHERE Name = @metricName;
		IF @metricID IS NULL
		BEGIN
			INSERT INTO MetricName (Name) VALUES (@metricName);
			SELECT @err = @@ERROR, @metricID = SCOPE_IDENTITY();
			IF @err <> 0 GOTO Abort;
		END
		COMMIT TRANSACTION;

		INSERT INTO MetricPair (RecordID, MetricID, MetricValue) VALUES (@recordID, @metricID, @metricValue);
		RETURN;

	Abort:
		ROLLBACK TRANSACTION;
		RETURN;
	END')
END
GO

IF NOT EXISTS(SELECT name FROM sys.objects WHERE name = N'uspUpdateHeartBeats' and type_desc = N'SQL_STORED_PROCEDURE')
BEGIN
	PRINT N'Creating [dbo].[uspUpdateHeartBeats]...';
	exec('CREATE PROCEDURE [dbo].[uspUpdateHeartBeats]
		@NodeID int,
		@SourceIP nvarchar(256),
		@NameNodeLast datetime,
		@JobTrackerLast datetime,
		@DataNodeLast datetime,
		@TaskTrackerLast datetime,
		@LastKnownIP nvarchar(256)
	AS
	BEGIN
		IF @NodeID IS NOT NULL
		BEGIN
			IF @NameNodeLast IS NOT NULL
			BEGIN
				UPDATE Node SET LastNameNodeHeartBeat = @NameNodeLast WHERE NodeID = @NodeID;
			END
			IF @JobTrackerLast IS NOT NULL
			BEGIN
				UPDATE Node SET LastJobTrackerHeartBeat = @JobTrackerLast WHERE NodeID = @NodeID;
			END
			IF @DataNodeLast IS NOT NULL
			BEGIN
				UPDATE Node SET LastDataNodeHeartBeat = @DataNodeLast WHERE NodeID = @NodeID;
			END
			IF @TaskTrackerLast IS NOT NULL
			BEGIN
				UPDATE Node SET LastTaskTrackerHeartBeat = @TaskTrackerLast WHERE NodeID = @NodeID;
			END
			IF @LastKnownIP IS NULL OR @SourceIP <> @LastKnownIP
			BEGIN
				UPDATE Node SET LastKnownIP = @SourceIP WHERE NodeID = @NodeID;
			END
		END
	END')
END
GO

IF NOT EXISTS(SELECT name FROM sys.objects WHERE name = N'uspGetMetricRecord' and type_desc = N'SQL_STORED_PROCEDURE')
BEGIN
	PRINT N'Creating [dbo].[uspGetMetricRecord]...';
	exec('CREATE PROCEDURE [dbo].[uspGetMetricRecord]
		@recordTypeContext nvarchar(256),
		@recordTypeName nvarchar(256),
		@nodeName nvarchar(256),
		@sourceIP nvarchar(256),
		@clusterNodeName nvarchar(256),
		@serviceName nvarchar(256),
		@tagPairs nvarchar(512),
		@recordTimestamp bigint,
		@metricRecordID bigint OUTPUT
	AS
	BEGIN
		SET NOCOUNT ON;

		DECLARE @recordTypeID int
		DECLARE @nodeID int
		DECLARE @clusterNodeID int
		DECLARE @tagSetID int
		DECLARE @serviceID int
		DECLARE @err int
		DECLARE @recordIDCutoff bigint

		BEGIN TRANSACTION;
		SELECT @recordTypeID = RecordTypeID FROM RecordType WHERE Context = @recordTypeContext AND Name = @recordTypeName;
		IF @recordTypeID IS NULL
			BEGIN
				INSERT INTO RecordType (Context, Name) VALUES (@recordTypeContext, @recordTypeName);
				SELECT @err = @@ERROR, @recordTypeID = SCOPE_IDENTITY();
				IF @err <> 0 GOTO Abort;
			END
		COMMIT TRANSACTION;

		BEGIN TRANSACTION;
		SELECT @serviceID = serviceID FROM Service WHERE Name = @serviceName;
		IF @serviceID IS NULL
			BEGIN
				INSERT INTO Service (Name) VALUES (@serviceName);
				SELECT @err = @@ERROR, @serviceID = SCOPE_IDENTITY();
				IF @err <> 0 GOTO Abort;
			END
		COMMIT TRANSACTION;

		BEGIN TRANSACTION;
		SELECT @nodeID = NodeID FROM Node WHERE Name = @nodeName;

		IF @nodeID IS NULL
			BEGIN

			/* Start with a node type of uninitialized.  HealthNode will determine node type based on metrics delivered over time. */
				INSERT INTO Node (Name, LastKnownIP) VALUES (@nodeName, @sourceIP);
				SELECT @err = @@ERROR, @nodeID = SCOPE_IDENTITY();
				IF @err <> 0 GOTO Abort;
			END

		COMMIT TRANSACTION;

		-- Do our best to determine the cluster node ID based on completely flakey input from user which might be an IP address, a non-FQDN,
		-- or an FQDN.  Note that worker nodes may have a completely different idea about the name of the namenode (which is the node
		-- which represents the cluster) compared with the namenode itself

		BEGIN TRANSACTION;
		IF ((SELECT [dbo].[ufnIsIPAddress](@clusterNodeName)) = 1)
		BEGIN
			SELECT TOP 1 @clusterNodeID = NodeID from Node WHERE LastKnownIP = @clusterNodeName ORDER BY LastNameNodeHeartBeat DESC;
			IF @clusterNodeID IS NULL
			BEGIN
				INSERT INTO Node (Name, LastKnownIP) VALUES (@clusterNodeName, @sourceIP);
				SELECT @err = @@ERROR, @clusterNodeID = SCOPE_IDENTITY();
				IF @err <> 0 GOTO Abort;
			END
		END
		ELSE
		IF ((SELECT CHARINDEX(@clusterNodeName, ''.'', 1)) > 0)
		BEGIN
			-- IF this is not an IP address, but there is a dot in the name we assume we are looking at an FQDN
		SELECT @clusterNodeID = NodeID FROM Node WHERE Name = @clusterNodeName;
		IF @clusterNodeID IS NULL
			BEGIN
				INSERT INTO Node (Name, LastKnownIP) VALUES (@clusterNodeName, @sourceIP);
				SELECT @err = @@ERROR, @clusterNodeID = SCOPE_IDENTITY();
				IF @err <> 0 GOTO Abort;
			END
		END
		ELSE
		BEGIN
			-- We have got a non-FQDN, but the NameNode might know its FQDN, so be careful! We must prefer the FQDN if we can find one.
			-- Sadly, yes, this could break things if we are monitoring clusters from different domains.  This is now by design!
			SELECT TOP 1 @clusterNodeID = NodeID FROM Node WHERE Name LIKE @clusterNodeName + ''.%'' ORDER BY LastNameNodeHeartBeat DESC;
			IF @clusterNodeID IS NULL
				BEGIN
					SELECT @clusterNodeID = NodeID FROM Node WHERE Name = @clusterNodeName;
					if @clusterNodeID IS NULL
					BEGIN
						INSERT INTO Node (Name, LastKnownIP) VALUES (@clusterNodeName, @sourceIP);
						SELECT @err = @@ERROR, @clusterNodeID = SCOPE_IDENTITY();
						IF @err <> 0 GOTO Abort;
					END
				END
		END
		COMMIT TRANSACTION;

		-- Cleanup older metric records and pairs if necessary
		-- Policy is to keep between 60000 and 90000 metric records and associated metric pairs per node.
		IF (SELECT COUNT(*) FROM MetricRecord WHERE NodeID = @nodeID) > 90000
		BEGIN
			SELECT @recordIDCutoff = MIN(RecordID) FROM MetricRecord WHERE RecordID IN (SELECT TOP 60000 RecordID FROM MetricRecord WHERE NodeID = @nodeID ORDER BY RecordDate DESC);
			IF @recordIDCutoff IS NOT NULL
			BEGIN
				DELETE FROM MetricPair
				FROM MetricPair as mp
				JOIN MetricRecord as mr ON mp.RecordID = mr.RecordID
				WHERE mr.RecordID < @recordIDCutoff AND mr.NodeID = @nodeID;

				DELETE FROM MetricRecord
				WHERE RecordID < @recordIDCutoff AND NodeID = @nodeID;
			END;
		END;


		BEGIN TRANSACTION;
		SELECT @tagSetID = TagSetID FROM TagSet WHERE TagPairs = @tagPairs;
		IF @tagSetID IS NULL
			BEGIN
				INSERT INTO TagSet (TagPairs) VALUES (@tagPairs);
				SELECT @err = @@ERROR, @tagSetID = SCOPE_IDENTITY();
				IF @err <> 0 GOTO Abort;
			END
		COMMIT TRANSACTION;

		BEGIN TRANSACTION;
		SELECT @metricRecordID = RecordID FROM MetricRecord WHERE RecordTypeID = @recordTypeID AND NodeID = @nodeID AND ServiceID = @serviceID AND TagSetID = @tagSetID AND RecordTimestamp = @recordTimestamp;
		IF @metricRecordID IS NULL
		BEGIN
			INSERT INTO MetricRecord (RecordTypeID, NodeID, SourceIP, ClusterNodeID, ServiceID, TagSetID, RecordTimestamp) VALUES (@recordTypeID, @nodeID, @sourceIP, @clusterNodeID, @serviceID, @tagSetID, @recordTimestamp);
			SELECT @err = @@ERROR, @metricRecordID = SCOPE_IDENTITY();
			IF @err <> 0 GOTO Abort;
		END
		COMMIT TRANSACTION;

		GOTO Success;

	Abort:
		ROLLBACK TRANSACTION;
		SET @metricRecordID = NULL;
		RETURN;

	Success:
		RETURN;

	END')
END
GO

IF NOT EXISTS(SELECT name FROM sys.objects WHERE name = N'ufnIsIPAddress' and type_desc = N'SQL_SCALAR_FUNCTION')
BEGIN
	PRINT N'Creating [dbo].[ufnIsIPAddress]...';
	exec('CREATE FUNCTION [dbo].[ufnIsIPAddress]
	(
		@inputString nvarchar(max)
	)
	RETURNS BIT
	AS
	BEGIN
		DECLARE @currentPos bigint = 1;
		DECLARE @nextPos bigint = 0;
		DECLARE @count int = 0;

		if (LEN(@inputString) = 0) RETURN 0;

		SELECT @nextPos = CHARINDEX(''.'', @inputString, @currentPos);

		WHILE (@nextPos < LEN(@inputString) AND @count < 4)
		BEGIN
			IF (@nextPos = 0) SET @nextPos = LEN(@inputString);
			IF ((SELECT ISNUMERIC(SUBSTRING(@inputString, @currentPos, @nextPos - @currentPos))) = 1)
			BEGIN
				SET @count = @count + 1;
				SET @currentPos = @nextPos
				SELECT @nextPos = CHARINDEX(''.'', @inputString, @currentPos + 1);
			END
			ELSE BREAK;
		END

		IF (@count = 4) RETURN 1;

		SET @currentPos = 1;
		SET @nextPos = 0;
		SET @count = 0;

		WHILE (@currentPos <= LEN(@inputString))
		BEGIN
			IF EXISTS (SELECT 1 WHERE SUBSTRING(@inputString, @currentPos, 1) LIKE ''[0-9A-Fa-f:]'')
			BEGIN
				IF (SUBSTRING(@inputString, @currentPos, 1) = N'':'') SET @count = @count + 1;
				SET @currentPos = @currentPos + 1;
			END
			ELSE RETURN 0;
		END
		IF @count >= 4 return 1;

		RETURN 0;
	END')
END
GO

IF NOT EXISTS(SELECT name FROM sys.objects WHERE name = N'RethrowError' and type_desc = N'SQL_STORED_PROCEDURE')
BEGIN
	PRINT N'Creating Stored Proc: [dbo].[RethrowError]...';
	exec('CREATE PROCEDURE [dbo].[RethrowError]
	AS
	BEGIN
		DECLARE @ErrorMessage NVARCHAR(4000);
		DECLARE @ErrorSeverity INT;
		DECLARE @ErrorState INT;

		SELECT
			@ErrorMessage = ERROR_MESSAGE(),
			@ErrorSeverity = ERROR_SEVERITY(),
			@ErrorState = ERROR_STATE();

		RAISERROR (@ErrorMessage, -- Message text.
				   @ErrorSeverity, -- Severity.
				   @ErrorState -- State.
				   );
	END
	')
END

IF NOT EXISTS(SELECT name FROM sys.objects WHERE name = N'uspPurgeMetrics' and type_desc = N'SQL_STORED_PROCEDURE')
BEGIN
    -- purge metrics older than @noOfDays
	PRINT N'Creating [dbo].[uspPurgeMetrics]...';
	exec('CREATE PROCEDURE [dbo].[uspPurgeMetrics]
			@noOfDays bigint
	AS
	BEGIN

		IF @noOfDays IS NULL OR @noOfDays < 1
		BEGIN
			RAISERROR(''INVALID_ARGUMENT'', 15, 1)
			RETURN
		END;

		DECLARE @recordIDCutOff BIGINT
		SELECT @recordIDCutoff = MAX(RecordID) FROM MetricRecord WHERE DateDiff(day, RecordDate, CURRENT_TIMESTAMP) >= @noOfDays

		IF @recordIDCutoff IS NOT NULL
		BEGIN
			BEGIN TRY
				BEGIN TRANSACTION

				DELETE FROM MetricPair WHERE RecordID <= @recordIDCutoff

				DELETE FROM MetricRecord WHERE RecordID <= @recordIDCutoff

				IF @@TRANCOUNT > 0
				BEGIN
					COMMIT TRANSACTION
				END

			END TRY
			BEGIN CATCH
				IF @@TRANCOUNT > 0
				BEGIN
					ROLLBACK TRANSACTION;
				END

				 -- get error infromation and raise error
				EXECUTE [dbo].[RethrowError]
				RETURN

			END CATCH
		END;
	END');
END

IF NOT EXISTS(SELECT name FROM sys.objects WHERE name = N'ufGetMetrics' and type_desc = N'SQL_TABLE_VALUED_FUNCTION')
BEGIN
	PRINT N'Creating [dbo].[ufGetMetrics]...';
    exec('CREATE FUNCTION dbo.ufGetMetrics
		(@startTimeStamp bigint,
		 @endTimeStamp bigint,
		 @recordTypeContext NVARCHAR(256),
		 @recordTypeName NVARCHAR(256),
		 @metricName NVARCHAR(256),
		 @serviceComponentName NVARCHAR(256),
		 @nodeName NVARCHAR(256)
		)
		RETURNS TABLE --(MetricTimeStamp bigint, MetricValue NVARCHAR(512))
		AS
		RETURN
		(
			SELECT  s.RecordTimeStamp AS RecordTimeStamp,
					mp.MetricValue AS MetricValue
			FROM MetricPair mp
			INNER JOIN (SELECT	mr.RecordID AS RecordID,
								mr.RecordTimeStamp AS RecordTimeStamp
						FROM MetricRecord mr
						INNER JOIN RecordType rt ON (mr.RecordTypeId = rt.RecordTypeId)
						INNER JOIN Node nd ON (mr.NodeID = nd.NodeID)
						INNER JOIN Service sr ON (mr.ServiceID = sr.ServiceID)
						WHERE rt.Context = @recordTypeContext
						AND rt.Name = @recordTypeName
						AND (nd.Name = @nodeName)
						AND (sr.Name = @serviceComponentName)
						AND mr.RecordTimestamp >= @startTimeStamp
						AND mr.RecordTimestamp <= @endTimeStamp
						) s ON (mp.RecordID = s.RecordID)
			INNER JOIN MetricName mn ON (mp.MetricID = mn.MetricID)
			WHERE (mn.Name = @metricName)
		)'
)
END

GO

IF NOT EXISTS(SELECT name FROM sys.objects WHERE name = N'ufGetAggregatedServiceMetrics' and type_desc = N'SQL_TABLE_VALUED_FUNCTION')
BEGIN
	PRINT N'Creating [dbo].[ufGetAggregatedServiceMetrics]...';
    exec( 'CREATE FUNCTION [dbo].[ufGetAggregatedServiceMetrics]
		(@startTimeStamp bigint,
		 @endTimeStamp bigint,
		 @recordTypeContext NVARCHAR(256),
		 @recordTypeName NVARCHAR(256),
		 @metricName NVARCHAR(256),
		 @serviceComponentName NVARCHAR(256),
		 @period integer
		)
		RETURNS TABLE ----(TimeStampBlock integer, MetricTimeStamp bigint, MetricValue NVARCHAR(512))
		AS
		RETURN
		(
			SELECT FLOOR ((mr.RecordTimeStamp - @startTimeStamp) / @period) TimeStampBlock, MAX(mr.RecordTimeStamp) RecordTimeStamp,  SUM(CONVERT(NUMERIC(18,4), MetricValue)) AggMetricValue
			FROM MetricPair mp
			INNER JOIN MetricRecord mr ON (mp.RecordID = mr.RecordID)
			INNER JOIN RecordType rt ON (rt.RecordTypeID = mr.RecordTypeID)
			INNER JOIN MetricName mn ON (mn.MetricID = mp.MetricID)
			INNER JOIN Service sr ON (sr.ServiceID = mr.ServiceID)
			WHERE mr.RecordTimestamp >= @startTimeStamp
			AND mr.RecordTimestamp <= @endTimeStamp
			AND mn.Name = @metricName
			AND rt.Context = @recordTypeContext
			AND rt.Name = @recordTypeName
			AND sr.Name = @serviceComponentName
			GROUP BY FLOOR ((mr.RecordTimeStamp - @startTimeStamp) / @period)
		)'
	    )
END
GO
