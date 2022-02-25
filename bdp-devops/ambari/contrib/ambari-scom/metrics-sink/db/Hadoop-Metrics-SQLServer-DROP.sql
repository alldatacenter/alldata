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
Schema purge script for $(METRICSDBNAME)

Use this script in sqlcmd mode, setting the environment variables like this:
set METRICSDBNAME=HadoopMetrics

sqlcmd -S localhost\SQLEXPRESS -i C:\app\ambari-server-1.3.0-SNAPSHOT\resources\Hadoop-Metrics-SQLServer-DROP.sql
*/

USE [$(METRICSDBNAME)]
GO

SET QUOTED_IDENTIFIER ON;
GO

IF EXISTS(SELECT name FROM sys.objects WHERE name = N'ufGetAggregatedServiceMetrics')
BEGIN
	PRINT N'Dropping [dbo].[ufGetAggregatedServiceMetrics]...';
    exec('DROP FUNCTION [dbo].[ufGetAggregatedServiceMetrics]')
END
GO

IF EXISTS(SELECT name FROM sys.objects WHERE name = N'ufGetMetrics')
BEGIN
	PRINT N'Dropping [dbo].[ufGetMetrics]...';
    exec('DROP FUNCTION dbo.ufGetMetrics')
END
GO

IF EXISTS(SELECT name FROM sys.objects WHERE name = N'uspPurgeMetrics')
BEGIN
    -- purge metrics older than @noOfDays
	PRINT N'Dropping [dbo].[uspPurgeMetrics]...';
	exec('DROP PROCEDURE [dbo].[uspPurgeMetrics]')
END
GO

IF EXISTS(SELECT name FROM sys.objects WHERE name = N'RethrowError')
BEGIN
	PRINT N'Dropping Stored Proc: [dbo].[RethrowError]...';
	exec('DROP PROCEDURE [dbo].[RethrowError]')
END
GO

IF EXISTS(SELECT name FROM sys.objects WHERE name = N'ufnIsIPAddress')
BEGIN
	PRINT N'Dropping [dbo].[ufnIsIPAddress]...';
	exec('DROP FUNCTION [dbo].[ufnIsIPAddress]')
END
GO

IF EXISTS(SELECT name FROM sys.objects WHERE name = N'uspGetMetricRecord')
BEGIN
	PRINT N'Dropping [dbo].[uspGetMetricRecord]...';
	exec('DROP PROCEDURE [dbo].[uspGetMetricRecord]')
END
GO

IF EXISTS(SELECT name FROM sys.objects WHERE name = N'uspUpdateHeartBeats')
BEGIN
	PRINT N'Dropping [dbo].[uspUpdateHeartBeats]...';
	exec('DROP PROCEDURE [dbo].[uspUpdateHeartBeats]')
END
GO

IF EXISTS(SELECT name FROM sys.objects WHERE name = N'uspInsertMetricValue')
BEGIN
	PRINT N'Dropping [dbo].[uspInsertMetricValue]...';
	exec('DROP PROCEDURE [dbo].[uspInsertMetricValue]')
END
GO

--Dropping the tables

IF EXISTS(SELECT name FROM sys.objects WHERE name = N'CompletedJob')
BEGIN
	PRINT N'Dropping [dbo].[CompletedJob]...';
	DROP TABLE [dbo].[CompletedJob]
END
GO

IF EXISTS(SELECT name FROM sys.objects WHERE name = N'Service')
BEGIN
	PRINT N'Dropping [dbo].[Service]...';
	DROP TABLE [dbo].[Service]
END
GO

IF EXISTS(SELECT name FROM sys.objects WHERE name = N'MetricPair')
BEGIN
	PRINT N'Dropping [dbo].[MetricPair]...';
	DROP TABLE [dbo].[MetricPair]
END
GO

IF EXISTS(SELECT name FROM sys.objects WHERE name = N'MetricRecord')
BEGIN
	PRINT N'Dropping [dbo].[MetricRecord]...';
	DROP TABLE [dbo].[MetricRecord]
END
GO

IF EXISTS(SELECT name FROM sys.objects WHERE name = N'MetricName')
BEGIN
	PRINT N'Dropping [dbo].[MetricName]...';
	DROP TABLE [dbo].[MetricName]
END
GO

IF EXISTS(SELECT name FROM sys.objects WHERE name = N'RecordType')
BEGIN
	PRINT N'Dropping [dbo].[RecordType]...';
	DROP TABLE [dbo].[RecordType]
END
GO

IF EXISTS(SELECT name FROM sys.objects WHERE name = N'TagSet')
BEGIN
	PRINT N'Dropping [dbo].[TagSet]...';
	DROP TABLE [dbo].[TagSet]
END
GO

IF EXISTS(SELECT name FROM sys.objects WHERE name = N'Node')
BEGIN
	PRINT N'Dropping [dbo].[Node]...';
	DROP TABLE [dbo].[Node]
END
GO

IF EXISTS(SELECT name FROM sys.objects WHERE name = N'DatabaseVersion')
BEGIN
	PRINT N'Dropping [dbo].[DatabaseVersion]...';
	DROP TABLE [dbo].[DatabaseVersion]
END
GO

IF EXISTS(SELECT name FROM sys.objects WHERE name = N'Configuration')
BEGIN
	PRINT N'Dropping [dbo].[Configuration]...';
	DROP TABLE [dbo].[Configuration]
END
GO
