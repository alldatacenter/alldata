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
Schema purge script for $(AMBARIDBNAME)

Use this script in sqlcmd mode, setting the environment variables like this:
set AMBARIDBNAME=ambari

sqlcmd -S localhost\SQLEXPRESS -i C:\app\ambari-server-1.3.0-SNAPSHOT\resources\Ambari-DDL-SQLServer-DROP.sql
*/

USE [$(AMBARIDBNAME)];

--Drop all Triggers
DECLARE @STR_TG NVARCHAR(MAX);
DECLARE CR_TRIG CURSOR FOR
SELECT 'DROP TRIGGER ' + '['+SCHEMA_NAME(uid)+'].['+name+']' FROM SYSOBJECTS WHERE xtype = 'tr'
OPEN CR_TRIG
FETCH NEXT FROM CR_TRIG INTO @STR_TG
WHILE (@@FETCH_STATUS = 0) BEGIN
    PRINT @STR_TG
    EXEC (@STR_TG)
    FETCH NEXT FROM CR_TRIG INTO @STR_TG
END
CLOSE CR_TRIG
DEALLOCATE CR_TRIG

--Drop all Foreign Key constraints (to prevent crash during dropping of tables)
declare @STR_FK varchar(max)
declare CUR_FK cursor for
SELECT 'ALTER TABLE ' + '[' + s.[NAME] + '].[' + t.name + '] DROP CONSTRAINT ['+ c.name + ']'
FROM sys.objects c, sys.objects t, sys.schemas s
WHERE
    c.type IN ('F')
    AND c.parent_object_id=t.object_id 
    AND t.type='U' 
    AND t.SCHEMA_ID = s.schema_id
ORDER BY c.type
OPEN CUR_FK
FETCH NEXT FROM CUR_FK INTO @STR_FK
WHILE (@@fetch_status = 0) BEGIN
 PRINT @STR_FK
 EXEC (@STR_FK)
 FETCH NEXT FROM CUR_FK INTO @STR_FK
END
CLOSE CUR_FK
DEALLOCATE CUR_FK

--Drop Tables
EXEC sp_msforeachtable @command1="print 'DROP TABLE ?'", @command2="DROP TABLE ?"
GO