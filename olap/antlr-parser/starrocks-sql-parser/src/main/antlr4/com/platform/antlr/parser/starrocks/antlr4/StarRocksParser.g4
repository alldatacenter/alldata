// Copyright 2021-present StarRocks, Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

parser grammar StarRocksParser;
options { tokenVocab = StarRocksLexer; }

sqlStatements
    : singleStatement+ EOF
    ;

singleStatement
    : (statement (SEMICOLON | EOF)) | emptyStatement
    ;
emptyStatement
    : SEMICOLON
    ;

statement
    // Query Statement
    : queryStatement

    // Warehouse Statement
    | createWarehouseStatement
    | dropWarehouseStatement
    | showWarehousesStatement
    | alterWarehouseStatement
    | showClustersStatement
    | suspendWarehouseStatement
    | resumeWarehouseStatement

    // Database Statement
    | useDatabaseStatement
    | useCatalogStatement
    | setCatalogStatement
    | showDatabasesStatement
    | alterDbQuotaStatement
    | createDbStatement
    | dropDbStatement
    | showCreateDbStatement
    | alterDatabaseRenameStatement
    | recoverDbStmt
    | showDataStmt

    // Table Statement
    | createTableStatement
    | createTableAsSelectStatement
    | createTemporaryTableStatement
    | createTableLikeStatement
    | showCreateTableStatement
    | dropTableStatement
    | recoverTableStatement
    | truncateTableStatement
    | showTableStatement
    | descTableStatement
    | showTableStatusStatement
    | showColumnStatement
    | refreshTableStatement
    | alterTableStatement
    | cancelAlterTableStatement
    | showAlterStatement

    // View Statement
    | createViewStatement
    | alterViewStatement
    | dropViewStatement

    // Partition Statement
    | showPartitionsStatement
    | recoverPartitionStatement

    // Index Statement
    | createIndexStatement
    | dropIndexStatement
    | showIndexStatement

    // Task Statement
    | submitTaskStatement

    // Materialized View Statement
    | createMaterializedViewStatement
    | showMaterializedViewsStatement
    | dropMaterializedViewStatement
    | alterMaterializedViewStatement
    | refreshMaterializedViewStatement
    | cancelRefreshMaterializedViewStatement

    // Catalog Statement
    | createExternalCatalogStatement
    | dropExternalCatalogStatement
    | showCatalogsStatement
    | showCreateExternalCatalogStatement

    // DML Statement
    | insertStatement
    | updateStatement
    | deleteStatement

    // Routine Statement
    | createRoutineLoadStatement
    | alterRoutineLoadStatement
    | stopRoutineLoadStatement
    | resumeRoutineLoadStatement
    | pauseRoutineLoadStatement
    | showRoutineLoadStatement
    | showRoutineLoadTaskStatement

    // StreamLoad Statement
    | showStreamLoadStatement

    // Admin Statement
    | adminSetConfigStatement
    | adminSetReplicaStatusStatement
    | adminShowConfigStatement
    | adminShowReplicaDistributionStatement
    | adminShowReplicaStatusStatement
    | adminRepairTableStatement
    | adminCancelRepairTableStatement
    | adminCheckTabletsStatement
    | killStatement
    | syncStatement
    | executeScriptStatement

    // Cluster Management Statement
    | alterSystemStatement
    | cancelAlterSystemStatement
    | showComputeNodesStatement

    // Analyze Statement
    | analyzeStatement
    | dropStatsStatement
    | createAnalyzeStatement
    | dropAnalyzeJobStatement
    | analyzeHistogramStatement
    | dropHistogramStatement
    | showAnalyzeStatement
    | showStatsMetaStatement
    | showHistogramMetaStatement
    | killAnalyzeStatement

    // Resource Group Statement
    | createResourceGroupStatement
    | dropResourceGroupStatement
    | alterResourceGroupStatement
    | showResourceGroupStatement

    // External Resource Statement
    | createResourceStatement
    | alterResourceStatement
    | dropResourceStatement
    | showResourceStatement

    // UDF Statement
    | showFunctionsStatement
    | dropFunctionStatement
    | createFunctionStatement

    // Load Statement
    | loadStatement
    | showLoadStatement
    | showLoadWarningsStatement
    | cancelLoadStatement
    | alterLoadStatement

    // Show Statement
    | showAuthorStatement
    | showBackendsStatement
    | showBrokerStatement
    | showCharsetStatement
    | showCollationStatement
    | showDeleteStatement
    | showDynamicPartitionStatement
    | showEventsStatement
    | showEnginesStatement
    | showFrontendsStatement
    | showPluginsStatement
    | showRepositoriesStatement
    | showOpenTableStatement
    | showPrivilegesStatement
    | showProcedureStatement
    | showProcStatement
    | showProcesslistStatement
    | showStatusStatement
    | showTabletStatement
    | showTransactionStatement
    | showTriggersStatement
    | showUserPropertyStatement
    | showVariablesStatement
    | showWarningStatement
    | helpStatement

    // authz Statement
    | createUserStatement
    | dropUserStatement
    | alterUserStatement
    | showUserStatement
    | showAuthenticationStatement
    | executeAsStatement
    | createRoleStatement
    | dropRoleStatement
    | showRolesStatement
    | grantRoleStatement
    | revokeRoleStatement
    | setRoleStatement
    | setDefaultRoleStatement
    | grantPrivilegeStatement
    | revokePrivilegeStatement
    | showGrantsStatement
    | createSecurityIntegrationStatement
    | alterSecurityIntegrationStatement
    | dropSecurityIntegrationStatement
    | showSecurityIntegrationStatement
    | showCreateSecurityIntegrationStatement
    | createRoleMappingStatement
    | alterRoleMappingStatement
    | dropRoleMappingStatement
    | showRoleMappingStatement
    | refreshRoleMappingStatement

    // Security Policy
    | createMaskingPolicyStatement
    | dropMaskingPolicyStatement
    | alterMaskingPolicyStatement
    | showMaskingPolicyStatement
    | showCreateMaskingPolicyStatement

    | createRowAccessPolicyStatement
    | dropRowAccessPolicyStatement
    | alterRowAccessPolicyStatement
    | showRowAccessPolicyStatement
    | showCreateRowAccessPolicyStatement

    // Backup Restore Statement
    | backupStatement
    | cancelBackupStatement
    | showBackupStatement
    | restoreStatement
    | cancelRestoreStatement
    | showRestoreStatement
    | showSnapshotStatement
    | createRepositoryStatement
    | dropRepositoryStatement

    // Sql BlackList And WhiteList Statement
    | addSqlBlackListStatement
    | delSqlBlackListStatement
    | showSqlBlackListStatement
    | showWhiteListStatement

    // Export Statement
    | exportStatement
    | cancelExportStatement
    | showExportStatement

    // Plugin Statement
    | installPluginStatement
    | uninstallPluginStatement

    // File Statement
    | createFileStatement
    | dropFileStatement
    | showSmallFilesStatement

    // Set Statement
    | setStatement
    | setUserPropertyStatement
    | setWarehouseStatement

    // Storage Volume Statement
    | createStorageVolumeStatement
    | alterStorageVolumeStatement
    | dropStorageVolumeStatement
    | showStorageVolumesStatement
    | descStorageVolumeStatement
    | setDefaultStorageVolumeStatement

    //Unsupported Statement
    | unsupportedStatement
    ;

// ---------------------------------------- DataBase Statement ---------------------------------------------------------

useDatabaseStatement
    : USE qualifiedName
    ;

useCatalogStatement
    : USE string
    ;

setCatalogStatement
    : SET CATALOG identifierOrString
    ;

showDatabasesStatement
    : SHOW DATABASES ((FROM | IN) catalog=qualifiedName)? ((LIKE pattern=string) | (WHERE expression))?
    | SHOW SCHEMAS ((LIKE pattern=string) | (WHERE expression))?
    ;

alterDbQuotaStatement
    : ALTER DATABASE identifier SET DATA QUOTA identifier
    | ALTER DATABASE identifier SET REPLICA QUOTA INTEGER_VALUE
    ;

createDbStatement
    : CREATE (DATABASE | SCHEMA) (IF NOT EXISTS)? (catalog=identifier DOT)? database=identifier charsetDesc? collateDesc? properties?
    ;

dropDbStatement
    : DROP (DATABASE | SCHEMA) (IF EXISTS)? (catalog=identifier DOT)? database=identifier FORCE?
    ;

showCreateDbStatement
    : SHOW CREATE (DATABASE | SCHEMA) identifier
    ;

alterDatabaseRenameStatement
    : ALTER DATABASE identifier RENAME identifier
    ;

recoverDbStmt
    : RECOVER (DATABASE | SCHEMA) identifier
    ;

showDataStmt
    : SHOW DATA
    | SHOW DATA FROM qualifiedName
    ;

// ------------------------------------------- Table Statement ---------------------------------------------------------

createTableStatement
    : CREATE EXTERNAL? TABLE (IF NOT EXISTS)? qualifiedName
          LEFT_PAREN columnDesc (COMMA columnDesc)* (COMMA indexDesc)* RIGHT_PAREN
          engineDesc?
          charsetDesc?
          keyDesc?
          comment?
          partitionDesc?
          distributionDesc?
          orderByDesc?
          rollupDesc?
          properties?
          extProperties?
     ;

columnDesc
    : identifier type charsetName? KEY? aggDesc? (NULL | NOT NULL)? (defaultDesc | AUTO_INCREMENT | materializedColumnDesc)? comment?
    ;

charsetName
    : CHAR SET identifier
    | CHARSET identifier
    | CHARACTER SET identifier
    ;

defaultDesc
    : DEFAULT (string | NULL | CURRENT_TIMESTAMP | LEFT_PAREN qualifiedName LEFT_PAREN RIGHT_PAREN RIGHT_PAREN)
    ;

materializedColumnDesc
    : AS expression
    ;

indexDesc
    : INDEX indexName=identifier identifierList indexType? comment?
    ;

engineDesc
    : ENGINE EQ identifier
    ;

charsetDesc
    : DEFAULT? (CHAR SET | CHARSET | CHARACTER SET) EQ? identifierOrString
    ;

collateDesc
    : DEFAULT? COLLATE EQ? identifierOrString
    ;

keyDesc
    : (AGGREGATE | UNIQUE | PRIMARY | DUPLICATE) KEY identifierList
    ;

orderByDesc
    : ORDER BY identifierList
    ;

aggDesc
    : SUM
    | MAX
    | MIN
    | REPLACE
    | HLL_UNION
    | BITMAP_UNION
    | PERCENTILE_UNION
    | REPLACE_IF_NOT_NULL
    ;

rollupDesc
    : ROLLUP LEFT_PAREN rollupItem (COMMA rollupItem)* RIGHT_PAREN
    ;

rollupItem
    : rollupName=identifier identifierList (dupKeys)? (fromRollup)? properties?
    ;

dupKeys
    : DUPLICATE KEY identifierList
    ;

fromRollup
    : FROM identifier
    ;

createTemporaryTableStatement
    : CREATE TEMPORARY TABLE qualifiedName
        queryStatement
    ;

createTableAsSelectStatement
    : CREATE TABLE (IF NOT EXISTS)? qualifiedName
        (LEFT_PAREN identifier (COMMA identifier)* RIGHT_PAREN)?
        keyDesc?
        comment?
        partitionDesc?
        distributionDesc?
        properties?
        AS queryStatement
    ;

dropTableStatement
    : DROP TEMPORARY? TABLE (IF EXISTS)? qualifiedName FORCE?
    ;

alterTableStatement
    : ALTER TABLE qualifiedName alterClause (COMMA alterClause)*
    | ALTER TABLE qualifiedName ADD ROLLUP rollupItem (COMMA rollupItem)*
    | ALTER TABLE qualifiedName DROP ROLLUP identifier (COMMA identifier)*
    ;

createIndexStatement
    : CREATE INDEX indexName=identifier
        ON qualifiedName identifierList indexType?
        comment?
    ;

dropIndexStatement
    : DROP INDEX indexName=identifier ON qualifiedName
    ;

indexType
    : USING BITMAP
    ;

showTableStatement
    : SHOW FULL? TABLES ((FROM | IN) db=qualifiedName)? ((LIKE pattern=string) | (WHERE expression))?
    ;

showCreateTableStatement
    : SHOW CREATE (TABLE | VIEW | MATERIALIZED VIEW) table=qualifiedName
    ;

showColumnStatement
    : SHOW FULL? (COLUMNS | FIELDS) ((FROM | IN) table=qualifiedName) ((FROM | IN) db=qualifiedName)?
        ((LIKE pattern=string) | (WHERE expression))?
    ;

showTableStatusStatement
    : SHOW TABLE STATUS ((FROM | IN) db=qualifiedName)? ((LIKE pattern=string) | (WHERE expression))?
    ;

refreshTableStatement
    : REFRESH EXTERNAL TABLE qualifiedName (PARTITION LEFT_PAREN string (COMMA string)* RIGHT_PAREN)?
    ;

showAlterStatement
    : SHOW ALTER TABLE (COLUMN | ROLLUP) ((FROM | IN) db=qualifiedName)?
        (WHERE expression)? (ORDER BY sortItem (COMMA sortItem)*)? (limitElement)?
    | SHOW ALTER MATERIALIZED VIEW ((FROM | IN) db=qualifiedName)?
              (WHERE expression)? (ORDER BY sortItem (COMMA sortItem)*)? (limitElement)?
    ;

descTableStatement
    : (DESC | DESCRIBE) table=qualifiedName ALL?
    ;

createTableLikeStatement
    : CREATE (EXTERNAL)? TABLE (IF NOT EXISTS)? qualifiedName LIKE qualifiedName
    ;

showIndexStatement
    : SHOW (INDEX | INDEXES | KEY | KEYS) ((FROM | IN) table=qualifiedName) ((FROM | IN) db=qualifiedName)?
    ;

recoverTableStatement
    : RECOVER TABLE qualifiedName
    ;

truncateTableStatement
    : TRUNCATE TABLE qualifiedName partitionNames?
    ;

cancelAlterTableStatement
    : CANCEL ALTER TABLE (COLUMN | ROLLUP)? FROM qualifiedName (LEFT_PAREN INTEGER_VALUE (COMMA INTEGER_VALUE)* RIGHT_PAREN)?
    | CANCEL ALTER MATERIALIZED VIEW FROM qualifiedName
    ;

showPartitionsStatement
    : SHOW TEMPORARY? PARTITIONS FROM table=qualifiedName
    (WHERE expression)?
    (ORDER BY sortItem (COMMA sortItem)*)? limitElement?
    ;

recoverPartitionStatement
    : RECOVER PARTITION identifier FROM table=qualifiedName
    ;

// ------------------------------------------- View Statement ----------------------------------------------------------

createViewStatement
    : CREATE VIEW (IF NOT EXISTS)? qualifiedName
        (LEFT_PAREN columnNameWithComment (COMMA columnNameWithComment)* RIGHT_PAREN)?
        comment? AS queryStatement
    ;

alterViewStatement
    : ALTER VIEW qualifiedName
    (LEFT_PAREN columnNameWithComment (COMMA columnNameWithComment)* RIGHT_PAREN)?
    AS queryStatement
    ;

dropViewStatement
    : DROP VIEW (IF EXISTS)? qualifiedName
    ;

columnNameWithComment
    : columnName=identifier comment?
    ;

// ------------------------------------------- Task Statement ----------------------------------------------------------

submitTaskStatement
    : SUBMIT setVarHint* TASK qualifiedName?
    AS (createTableAsSelectStatement | insertStatement )
    ;

// ------------------------------------------- Materialized View Statement ---------------------------------------------

createMaterializedViewStatement
    : CREATE MATERIALIZED VIEW (IF NOT EXISTS)? mvName=qualifiedName
    (LEFT_PAREN columnNameWithComment (COMMA columnNameWithComment)* RIGHT_PAREN)?
    comment?
    materializedViewDesc*
    AS queryStatement
    ;

materializedViewDesc
    : (PARTITION BY primaryExpression)
    | distributionDesc
    | orderByDesc
    | refreshSchemeDesc
    | properties
    ;

showMaterializedViewsStatement
    : SHOW MATERIALIZED VIEWS ((FROM | IN) db=qualifiedName)? ((LIKE pattern=string) | (WHERE expression))?
    ;

dropMaterializedViewStatement
    : DROP MATERIALIZED VIEW (IF EXISTS)? mvName=qualifiedName
    ;

alterMaterializedViewStatement
    : ALTER MATERIALIZED VIEW mvName=qualifiedName (refreshSchemeDesc | tableRenameClause | modifyTablePropertiesClause)
    ;

refreshMaterializedViewStatement
    : REFRESH MATERIALIZED VIEW mvName=qualifiedName (PARTITION partitionRangeDesc)? FORCE?
    ;

cancelRefreshMaterializedViewStatement
    : CANCEL REFRESH MATERIALIZED VIEW mvName=qualifiedName
    ;

// ------------------------------------------- Admin Statement ---------------------------------------------------------

adminSetConfigStatement
    : ADMIN SET FRONTEND CONFIG LEFT_PAREN property RIGHT_PAREN
    ;
adminSetReplicaStatusStatement
    : ADMIN SET REPLICA STATUS properties
    ;
adminShowConfigStatement
    : ADMIN SHOW FRONTEND CONFIG (LIKE pattern=string)?
    ;

adminShowReplicaDistributionStatement
    : ADMIN SHOW REPLICA DISTRIBUTION FROM qualifiedName partitionNames?
    ;

adminShowReplicaStatusStatement
    : ADMIN SHOW REPLICA STATUS FROM qualifiedName partitionNames? (WHERE where=expression)?
    ;

adminRepairTableStatement
    : ADMIN REPAIR TABLE qualifiedName partitionNames?
    ;

adminCancelRepairTableStatement
    : ADMIN CANCEL REPAIR TABLE qualifiedName partitionNames?
    ;

adminCheckTabletsStatement
    : ADMIN CHECK tabletList PROPERTIES LEFT_PARENpropertyRIGHT_PAREN
    ;

killStatement
    : KILL (CONNECTION? | QUERY) INTEGER_VALUE
    ;

syncStatement
    : SYNC
    ;

// ------------------------------------------- Cluster Management Statement ---------------------------------------------

alterSystemStatement
    : ALTER SYSTEM alterClause
    ;

cancelAlterSystemStatement
    : CANCEL DECOMMISSION BACKEND string (COMMA string)*
    ;

showComputeNodesStatement
    : SHOW COMPUTE NODES
    ;

// ------------------------------------------- Catalog Statement -------------------------------------------------------

createExternalCatalogStatement
    : CREATE EXTERNAL CATALOG catalogName=identifierOrString comment? properties
    ;

showCreateExternalCatalogStatement
    : SHOW CREATE CATALOG catalogName=identifierOrString
    ;

dropExternalCatalogStatement
    : DROP CATALOG catalogName=identifierOrString
    ;

showCatalogsStatement
    : SHOW CATALOGS
    ;


// ---------------------------------------- Warehouse Statement ---------------------------------------------------------

createWarehouseStatement
    : CREATE (WAREHOUSE) (IF NOT EXISTS)? warehouseName=identifierOrString
    properties?
    ;

showWarehousesStatement
    : SHOW WAREHOUSES ((LIKE pattern=string) | (WHERE expression))?
    ;

dropWarehouseStatement
    : DROP WAREHOUSE (IF EXISTS)? warehouseName=identifierOrString
    ;

alterWarehouseStatement
    : ALTER WAREHOUSE identifier ADD CLUSTER
    | ALTER WAREHOUSE identifier REMOVE CLUSTER
    | ALTER WAREHOUSE identifier SET propertyList
    ;

showClustersStatement
    : SHOW CLUSTERS FROM WAREHOUSE identifier
    ;

suspendWarehouseStatement
    : SUSPEND WAREHOUSE (IF EXISTS)? identifier
    ;

resumeWarehouseStatement
    : RESUME WAREHOUSE (IF EXISTS)? identifier
    ;

// ---------------------------------------- Storage Volume Statement ---------------------------------------------------

createStorageVolumeStatement
    : CREATE STORAGE VOLUME (IF NOT EXISTS)? storageVolumeName=identifierOrString typeDesc locationsDesc
          comment? properties
    ;

typeDesc
    : TYPE EQ identifier
    ;

locationsDesc
    : LOCATIONS EQ stringList
    ;

showStorageVolumesStatement
    : SHOW STORAGE VOLUMES (LIKE pattern=string)?
    ;

dropStorageVolumeStatement
    : DROP STORAGE VOLUME (IF EXISTS)? storageVolumeName=identifierOrString
    ;

alterStorageVolumeStatement
    : ALTER STORAGE VOLUME identifierOrString alterStorageVolumeClause (COMMA alterStorageVolumeClause)*
    ;

alterStorageVolumeClause
    : modifyStorageVolumeCommentClause
    | modifyStorageVolumePropertiesClause
    ;

modifyStorageVolumePropertiesClause
    : SET propertyList
    ;

modifyStorageVolumeCommentClause
    : COMMENT EQ string
    ;

descStorageVolumeStatement
    : (DESC | DESCRIBE) STORAGE VOLUME identifierOrString
    ;

setDefaultStorageVolumeStatement
    : SET identifierOrString AS DEFAULT STORAGE VOLUME
    ;

// ------------------------------------------- Alter Clause ------------------------------------------------------------

alterClause
    //Alter system clause
    : addFrontendClause
    | dropFrontendClause
    | modifyFrontendHostClause
    | addBackendClause
    | dropBackendClause
    | decommissionBackendClause
    | modifyBackendHostClause
    | addComputeNodeClause
    | dropComputeNodeClause
    | modifyBrokerClause
    | alterLoadErrorUrlClause
    | createImageClause
    | cleanTabletSchedQClause

    //Alter table clause
    | createIndexClause
    | dropIndexClause
    | tableRenameClause
    | swapTableClause
    | modifyTablePropertiesClause
    | addColumnClause
    | addColumnsClause
    | dropColumnClause
    | modifyColumnClause
    | columnRenameClause
    | reorderColumnsClause
    | rollupRenameClause
    | compactionClause
    | modifyCommentClause

    //Alter partition clause
    | addPartitionClause
    | dropPartitionClause
    | distributionClause
    | truncatePartitionClause
    | modifyPartitionClause
    | replacePartitionClause
    | partitionRenameClause
    ;

// ---------Alter system clause---------

addFrontendClause
   : ADD (FOLLOWER | OBSERVER) string
   ;

dropFrontendClause
   : DROP (FOLLOWER | OBSERVER) string
   ;

modifyFrontendHostClause
  : MODIFY FRONTEND HOST string TO string
  ;

addBackendClause
   : ADD BACKEND string (COMMA string)*
   ;

dropBackendClause
   : DROP BACKEND string (COMMA string)* FORCE?
   ;

decommissionBackendClause
   : DECOMMISSION BACKEND string (COMMA string)*
   ;

modifyBackendHostClause
   : MODIFY BACKEND HOST string TO string
   ;

addComputeNodeClause
   : ADD COMPUTE NODE string (COMMA string)*
   ;

dropComputeNodeClause
   : DROP COMPUTE NODE string (COMMA string)*
   ;

modifyBrokerClause
    : ADD BROKER identifierOrString string (COMMA string)*
    | DROP BROKER identifierOrString string (COMMA string)*
    | DROP ALL BROKER identifierOrString
    ;

alterLoadErrorUrlClause
    : SET LOAD ERRORS HUB properties?
    ;

createImageClause
    : CREATE IMAGE
    ;

cleanTabletSchedQClause
    : CLEAN TABLET SCHEDULER QUEUE
    ;

// ---------Alter table clause---------

createIndexClause
    : ADD INDEX indexName=identifier identifierList indexType? comment?
    ;

dropIndexClause
    : DROP INDEX indexName=identifier
    ;

tableRenameClause
    : RENAME identifier
    ;

swapTableClause
    : SWAP WITH identifier
    ;

modifyTablePropertiesClause
    : SET propertyList
    ;

modifyCommentClause
    : COMMENT EQ string
    ;

addColumnClause
    : ADD COLUMN columnDesc (FIRST | AFTER identifier)? ((TO | IN) rollupName=identifier)? properties?
    ;

addColumnsClause
    : ADD COLUMN LEFT_PAREN columnDesc (COMMA columnDesc)* RIGHT_PAREN ((TO | IN) rollupName=identifier)? properties?
    ;

dropColumnClause
    : DROP COLUMN identifier (FROM rollupName=identifier)? properties?
    ;

modifyColumnClause
    : MODIFY COLUMN columnDesc (FIRST | AFTER identifier)? (FROM rollupName=identifier)? properties?
    ;

columnRenameClause
    : RENAME COLUMN oldColumn=identifier newColumn=identifier
    ;

reorderColumnsClause
    : ORDER BY identifierList (FROM rollupName=identifier)? properties?
    ;

rollupRenameClause
    : RENAME ROLLUP rollupName=identifier newRollupName=identifier
    ;

compactionClause
    : (BASE | CUMULATIVE)? COMPACT (identifier | identifierList)?
    ;

// ---------Alter partition clause---------

addPartitionClause
    : ADD TEMPORARY? (singleRangePartition | PARTITIONS multiRangePartition) distributionDesc? properties?
    | ADD TEMPORARY? (singleItemListPartitionDesc | multiItemListPartitionDesc) distributionDesc? properties?
    ;

dropPartitionClause
    : DROP TEMPORARY? PARTITION (IF EXISTS)? identifier FORCE?
    ;

truncatePartitionClause
    : TRUNCATE partitionNames
    ;

modifyPartitionClause
    : MODIFY PARTITION (identifier | identifierList | LEFT_PAREN ASTERISK_SYMBOL RIGHT_PAREN) SET propertyList
    | MODIFY PARTITION distributionDesc
    ;

replacePartitionClause
    : REPLACE parName=partitionNames WITH tempParName=partitionNames properties?
    ;

partitionRenameClause
    : RENAME PARTITION parName=identifier newParName=identifier
    ;

// ------------------------------------------- DML Statement -----------------------------------------------------------

insertStatement
    : explainDesc? INSERT (INTO | OVERWRITE) qualifiedName partitionNames?
        (WITH LABEL label=identifier)? columnAliases?
        (queryStatement | (VALUES expressionsWithDefault (COMMA expressionsWithDefault)*))
    ;

updateStatement
    : explainDesc? withClause? UPDATE qualifiedName SET assignmentList fromClause (WHERE where=expression)?
    ;

deleteStatement
    : explainDesc? withClause? DELETE FROM qualifiedName partitionNames? (USING using=relations)? (WHERE where=expression)?
    ;

// ------------------------------------------- Routine Statement -----------------------------------------------------------
createRoutineLoadStatement
    : CREATE ROUTINE LOAD (db=qualifiedName DOT)? name=identifier ON table=qualifiedName
        (loadProperties (COMMA loadProperties)*)?
        jobProperties?
        FROM source=identifier
        dataSourceProperties?
    ;

alterRoutineLoadStatement
    : ALTER ROUTINE LOAD FOR (db=qualifiedName DOT)? name=identifier
        (loadProperties (COMMA loadProperties)*)?
        jobProperties?
        dataSource?
    ;

dataSource
    : FROM source=identifier dataSourceProperties
    ;

loadProperties
    : colSeparatorProperty
    | rowDelimiterProperty
    | importColumns
    | WHERE expression
    | partitionNames
    ;

colSeparatorProperty
    : COLUMNS TERMINATED BY string
    ;

rowDelimiterProperty
    : ROWS TERMINATED BY string
    ;

importColumns
    : COLUMNS columnProperties
    ;

columnProperties
    : LEFT_PAREN
        (qualifiedName | assignment) (COMMA (qualifiedName | assignment))*
      RIGHT_PAREN
    ;

jobProperties
    : properties
    ;

dataSourceProperties
    : propertyList
    ;

stopRoutineLoadStatement
    : STOP ROUTINE LOAD FOR (db=qualifiedName DOT)? name=identifier
    ;

resumeRoutineLoadStatement
    : RESUME ROUTINE LOAD FOR (db=qualifiedName DOT)? name=identifier
    ;

pauseRoutineLoadStatement
    : PAUSE ROUTINE LOAD FOR (db=qualifiedName DOT)? name=identifier
    ;

showRoutineLoadStatement
    : SHOW ALL? ROUTINE LOAD (FOR (db=qualifiedName DOT)? name=identifier)?
        (FROM db=qualifiedName)?
        (WHERE expression)? (ORDER BY sortItem (COMMA sortItem)*)? (limitElement)?
    ;

showRoutineLoadTaskStatement
    : SHOW ROUTINE LOAD TASK
        (FROM db=qualifiedName)?
        WHERE expression
    ;

showStreamLoadStatement
    : SHOW ALL? STREAM LOAD (FOR (db=qualifiedName DOT)? name=identifier)?
        (FROM db=qualifiedName)?
        (WHERE expression)? (ORDER BY sortItem (COMMA sortItem)*)? (limitElement)?
    ;
// ------------------------------------------- Analyze Statement -------------------------------------------------------

analyzeStatement
    : ANALYZE (FULL | SAMPLE)? TABLE qualifiedName (LEFT_PAREN identifier (COMMA identifier)* RIGHT_PAREN)?
        (WITH (SYNC | ASYNC) MODE)?
        properties?
    ;

dropStatsStatement
    : DROP STATS qualifiedName
    ;

analyzeHistogramStatement
    : ANALYZE TABLE qualifiedName UPDATE HISTOGRAM ON identifier (COMMA identifier)*
        (WITH (SYNC | ASYNC) MODE)?
        (WITH bucket=INTEGER_VALUE BUCKETS)?
        properties?
    ;

dropHistogramStatement
    : ANALYZE TABLE qualifiedName DROP HISTOGRAM ON identifier (COMMA identifier)*
    ;

createAnalyzeStatement
    : CREATE ANALYZE (FULL | SAMPLE)? ALL properties?
    | CREATE ANALYZE (FULL | SAMPLE)? DATABASE db=identifier properties?
    | CREATE ANALYZE (FULL | SAMPLE)? TABLE qualifiedName (LEFT_PAREN identifier (COMMA identifier)* RIGHT_PAREN)? properties?
    ;

dropAnalyzeJobStatement
    : DROP ANALYZE INTEGER_VALUE
    ;

showAnalyzeStatement
    : SHOW ANALYZE (JOB | STATUS)? (WHERE expression)?
    ;

showStatsMetaStatement
    : SHOW STATS META (WHERE expression)?
    ;

showHistogramMetaStatement
    : SHOW HISTOGRAM META (WHERE expression)?
    ;

killAnalyzeStatement
    : KILL ANALYZE INTEGER_VALUE
    ;

// ------------------------------------------- Work Group Statement ----------------------------------------------------

createResourceGroupStatement
    : CREATE RESOURCE GROUP (IF NOT EXISTS)? (OR REPLACE)? identifier
        TO classifier (COMMA classifier)*  WITH LEFT_PAREN property (COMMA property)* RIGHT_PAREN
    ;

dropResourceGroupStatement
    : DROP RESOURCE GROUP identifier
    ;

alterResourceGroupStatement
    : ALTER RESOURCE GROUP identifier ADD classifier (COMMA classifier)*
    | ALTER RESOURCE GROUP identifier DROP LEFT_PAREN INTEGER_VALUE (COMMA INTEGER_VALUE)* RIGHT_PAREN
    | ALTER RESOURCE GROUP identifier DROP ALL
    | ALTER RESOURCE GROUP identifier WITH LEFT_PAREN property (COMMA property)* RIGHT_PAREN
    ;

showResourceGroupStatement
    : SHOW RESOURCE GROUP identifier
    | SHOW RESOURCE GROUPS ALL?
    ;

createResourceStatement
    : CREATE EXTERNAL? RESOURCE resourceName=identifierOrString properties?
    ;

alterResourceStatement
    : ALTER RESOURCE resourceName=identifierOrString SET properties
    ;

dropResourceStatement
    : DROP RESOURCE resourceName=identifierOrString
    ;

showResourceStatement
    : SHOW RESOURCES
    ;

classifier
    : LEFT_PAREN expressionList RIGHT_PAREN
    ;

// ------------------------------------------- UDF Statement ----------------------------------------------------

showFunctionsStatement
    : SHOW FULL? (BUILTIN|GLOBAL)? FUNCTIONS ((FROM | IN) db=qualifiedName)? ((LIKE pattern=string) | (WHERE expression))?
    ;

dropFunctionStatement
    : DROP GLOBAL? FUNCTION qualifiedName LEFT_PAREN typeList RIGHT_PAREN
    ;

createFunctionStatement
    : CREATE GLOBAL? functionType=(TABLE | AGGREGATE)? FUNCTION qualifiedName LEFT_PAREN typeList RIGHT_PAREN RETURNS returnType=type (INTERMEDIATE intermediateType =  type)? properties?
    ;

typeList
    : type?  ( COMMA type)* (COMMA DOTDOTDOT) ?
    ;

// ------------------------------------------- Load Statement ----------------------------------------------------------

loadStatement
    : LOAD LABEL label=labelName
        data=dataDescList?
        broker=brokerDesc?
        (BY system=identifierOrString)?
        (PROPERTIES props=propertyList)?
    | LOAD LABEL label=labelName
        data=dataDescList?
        resource=resourceDesc
        (PROPERTIES props=propertyList)?
    ;

labelName
    : (db=identifier DOT)? label=identifier
    ;

dataDescList
    : LEFT_PAREN dataDesc (COMMA dataDesc)* RIGHT_PAREN
    ;

dataDesc
    : DATA INFILE srcFiles=stringList
        NEGATIVE?
        INTO TABLE dstTableName=identifier
        partitions=partitionNames?
        (COLUMNS TERMINATED BY colSep=string)?
        (ROWS TERMINATED BY rowSep=string)?
        format=fileFormat?
        (formatPropsField=formatProps)?
        colList=columnAliases?
        (COLUMNS FROM PATH AS colFromPath=identifierList)?
        (SET colMappingList=classifier)?
        (WHERE where=expression)?
    | DATA FROM TABLE srcTableName=identifier
        NEGATIVE?
        INTO TABLE dstTableName=identifier
        partitions=partitionNames?
        (SET colMappingList=classifier)?
        (WHERE where=expression)?
    ;

formatProps
    :  LEFT_PAREN
            (SKIP_HEADER EQ INTEGER_VALUE)?
            (TRIM_SPACE EQ booleanValue)?
            (ENCLOSE EQ encloseCharacter=string)?
            (ESCAPE EQ escapeCharacter=string)?
        RIGHT_PAREN
    ;

brokerDesc
    : WITH BROKER props=propertyList?
    | WITH BROKER name=identifierOrString props=propertyList?
    ;

resourceDesc
    : WITH RESOURCE name=identifierOrString props=propertyList?
    ;

showLoadStatement
    : SHOW LOAD (ALL)? (FROM identifier)? (WHERE expression)? (ORDER BY sortItem (COMMA sortItem)*)? limitElement?
    ;

showLoadWarningsStatement
    : SHOW LOAD WARNINGS (FROM identifier)? (WHERE expression)? limitElement?
    | SHOW LOAD WARNINGS ON string
    ;

cancelLoadStatement
    : CANCEL LOAD (FROM identifier)? (WHERE expression)?
    ;

alterLoadStatement
    : ALTER LOAD FOR (db=qualifiedName DOT)? name=identifier
        jobProperties?
    ;

// ------------------------------------------- Show Statement ----------------------------------------------------------

showAuthorStatement
    : SHOW AUTHORS
    ;

showBackendsStatement
    : SHOW BACKENDS
    ;

showBrokerStatement
    : SHOW BROKER
    ;

showCharsetStatement
    : SHOW (CHAR SET | CHARSET | CHARACTER SET) ((LIKE pattern=string) | (WHERE expression))?
    ;

showCollationStatement
    : SHOW COLLATION ((LIKE pattern=string) | (WHERE expression))?
    ;

showDeleteStatement
    : SHOW DELETE ((FROM | IN) db=qualifiedName)?
    ;

showDynamicPartitionStatement
    : SHOW DYNAMIC PARTITION TABLES ((FROM | IN) db=qualifiedName)?
    ;

showEventsStatement
    : SHOW EVENTS ((FROM | IN) catalog=qualifiedName)? ((LIKE pattern=string) | (WHERE expression))?
    ;

showEnginesStatement
    : SHOW ENGINES
    ;

showFrontendsStatement
    : SHOW FRONTENDS
    ;

showPluginsStatement
    : SHOW PLUGINS
    ;

showRepositoriesStatement
    : SHOW REPOSITORIES
    ;

showOpenTableStatement
    : SHOW OPEN TABLES
    ;
showPrivilegesStatement
    : SHOW PRIVILEGES
    ;

showProcedureStatement
    : SHOW (PROCEDURE | FUNCTION) STATUS ((LIKE pattern=string) | (WHERE where=expression))?
    ;

showProcStatement
    : SHOW PROC path=string
    ;

showProcesslistStatement
    : SHOW FULL? PROCESSLIST
    ;

showStatusStatement
    : SHOW varType? STATUS ((LIKE pattern=string) | (WHERE expression))?
    ;

showTabletStatement
    : SHOW TABLET INTEGER_VALUE
    | SHOW TABLET FROM qualifiedName partitionNames? (WHERE expression)? (ORDER BY sortItem (COMMA sortItem)*)? (limitElement)?
    ;

showTransactionStatement
    : SHOW TRANSACTION ((FROM | IN) db=qualifiedName)? (WHERE expression)?
    ;

showTriggersStatement
    : SHOW FULL? TRIGGERS ((FROM | IN) catalog=qualifiedName)? ((LIKE pattern=string) | (WHERE expression))?
    ;

showUserPropertyStatement
    : SHOW PROPERTY (FOR string)? (LIKE string)?
    ;

showVariablesStatement
    : SHOW varType? VARIABLES ((LIKE pattern=string) | (WHERE expression))?
    ;

showWarningStatement
    : SHOW (WARNINGS | ERRORS) (limitElement)?
    ;

helpStatement
    : HELP identifierOrString
    ;

// ------------------------------------------- Authz Statement -----------------------------------------------------

createUserStatement
    : CREATE USER (IF NOT EXISTS)? user authOption? (DEFAULT ROLE roleList)?
    ;

dropUserStatement
    : DROP USER (IF EXISTS)? user
    ;

alterUserStatement
    : ALTER USER (IF EXISTS)? user authOption
    | ALTER USER (IF EXISTS)? user DEFAULT ROLE (NONE| ALL | roleList)
    ;

showUserStatement
    : SHOW (USER | USERS)
    ;

showAuthenticationStatement
    : SHOW ALL AUTHENTICATION                                                                           #showAllAuthentication
    | SHOW AUTHENTICATION (FOR user)?                                                                   #showAuthenticationForUser
    ;

executeAsStatement
    : EXECUTE AS user (WITH NO REVERT)?
    ;

createRoleStatement
    : CREATE ROLE (IF NOT EXISTS)? roleList
    ;

dropRoleStatement
    : DROP ROLE (IF EXISTS)? roleList
    ;

showRolesStatement
    : SHOW ROLES
    ;

grantRoleStatement
    : GRANT identifierOrStringList TO USER? user                                                        #grantRoleToUser
    | GRANT identifierOrStringList TO ROLE identifierOrString                                           #grantRoleToRole
    ;

revokeRoleStatement
    : REVOKE identifierOrStringList FROM USER? user                                                     #revokeRoleFromUser
    | REVOKE identifierOrStringList FROM ROLE identifierOrString                                        #revokeRoleFromRole
    ;

setRoleStatement
    : SET ROLE DEFAULT
    | SET ROLE NONE
    | SET ROLE ALL (EXCEPT roleList)?
    | SET ROLE roleList
    ;

setDefaultRoleStatement
    : SET DEFAULT ROLE (NONE | ALL | roleList) TO user;

grantRevokeClause
    : (USER? user | ROLE identifierOrString)
    ;

grantPrivilegeStatement
    : GRANT IMPERSONATE ON USER user (COMMA user)* TO grantRevokeClause (WITH GRANT OPTION)?              #grantOnUser
    | GRANT privilegeTypeList ON privObjectNameList TO grantRevokeClause (WITH GRANT OPTION)?           #grantOnTableBrief

    | GRANT privilegeTypeList ON GLOBAL? FUNCTION privFunctionObjectNameList
        TO grantRevokeClause (WITH GRANT OPTION)?                                                       #grantOnFunc
    | GRANT privilegeTypeList ON SYSTEM TO grantRevokeClause (WITH GRANT OPTION)?                       #grantOnSystem
    | GRANT privilegeTypeList ON privObjectType privObjectNameList
        TO grantRevokeClause (WITH GRANT OPTION)?                                                       #grantOnPrimaryObj
    | GRANT privilegeTypeList ON ALL privObjectTypePlural
        (IN isAll=ALL DATABASES| IN DATABASE identifierOrString)? TO grantRevokeClause
        (WITH GRANT OPTION)?                                                                            #grantOnAll
    ;

revokePrivilegeStatement
    : REVOKE IMPERSONATE ON USER user (COMMA user)* FROM grantRevokeClause                                #revokeOnUser
    | REVOKE privilegeTypeList ON privObjectNameList FROM grantRevokeClause                             #revokeOnTableBrief
    | REVOKE privilegeTypeList ON GLOBAL? FUNCTION privFunctionObjectNameList
        FROM grantRevokeClause                                                                          #revokeOnFunc
    | REVOKE privilegeTypeList ON SYSTEM FROM grantRevokeClause                                         #revokeOnSystem
    | REVOKE privilegeTypeList ON privObjectType privObjectNameList
        FROM grantRevokeClause                                                                          #revokeOnPrimaryObj
    | REVOKE privilegeTypeList ON ALL privObjectTypePlural
        (IN isAll=ALL DATABASES| IN DATABASE identifierOrString)? FROM grantRevokeClause                #revokeOnAll
    ;

showGrantsStatement
    : SHOW GRANTS
    | SHOW GRANTS FOR USER? user
    | SHOW GRANTS FOR ROLE identifierOrString
    ;

createSecurityIntegrationStatement
    : CREATE SECURITY INTEGRATION identifier properties
    ;

alterSecurityIntegrationStatement
    : ALTER SECURITY INTEGRATION identifier SET propertyList
    ;

dropSecurityIntegrationStatement
    : DROP SECURITY INTEGRATION identifier
    ;

showSecurityIntegrationStatement
    : SHOW SECURITY INTEGRATIONS
    ;

showCreateSecurityIntegrationStatement
    : SHOW CREATE SECURITY INTEGRATION identifier
    ;

createRoleMappingStatement
    : CREATE ROLE MAPPING identifier properties
    ;

alterRoleMappingStatement
    : ALTER ROLE MAPPING identifier SET propertyList
    ;

dropRoleMappingStatement
    : DROP ROLE MAPPING identifier
    ;

showRoleMappingStatement
    : SHOW ROLE MAPPINGS
    ;

refreshRoleMappingStatement
    : REFRESH ALL ROLE MAPPINGS
    ;

authOption
    : IDENTIFIED BY PASSWORD? string                                                                    #authWithoutPlugin
    | IDENTIFIED WITH identifierOrString ((BY | AS) string)?                                            #authWithPlugin
    ;

privObjectName
    : identifierOrStringOrStar (DOT identifierOrStringOrStar)?
    ;

privObjectNameList
    : privObjectName (COMMA privObjectName)*
    ;

privFunctionObjectNameList
    : qualifiedName LEFT_PAREN typeList RIGHT_PAREN (COMMA qualifiedName LEFT_PAREN typeList RIGHT_PAREN)*
    ;

privilegeTypeList
    :  privilegeType (COMMA privilegeType)*
    ;

privilegeType
    : ALL PRIVILEGES?
    | ALTER | APPLY | BLACKLIST
    | CREATE (
        DATABASE| TABLE| VIEW| FUNCTION| GLOBAL FUNCTION| MATERIALIZED VIEW| RESOURCE| RESOURCE GROUP| EXTERNAL CATALOG | POLICY)
    | DELETE | DROP | EXPORT | FILE | IMPERSONATE | INSERT | GRANT | NODE | OPERATE
    | PLUGIN | REPOSITORY| REFRESH | SELECT | UPDATE | USAGE
    ;

privObjectType
    : CATALOG | DATABASE | MATERIALIZED VIEW | POLICY | RESOURCE | RESOURCE GROUP| SYSTEM | TABLE| VIEW
    ;

privObjectTypePlural
    : CATALOGS | DATABASES | FUNCTIONS | GLOBAL FUNCTIONS | MATERIALIZED VIEWS | POLICIES | RESOURCES | RESOURCE GROUPS
    | TABLES | USERS | VIEWS
    ;

// ---------------------------------------- Security Policy Statement ---------------------------------------------------

createMaskingPolicyStatement
    : CREATE (OR REPLACE)? MASKING POLICY (IF NOT EXISTS)? policyName=qualifiedName
        AS LEFT_PAREN policySignature (COMMA policySignature)* RIGHT_PAREN RETURNS type ARROW expression comment?
    ;

dropMaskingPolicyStatement
    : DROP (IF EXISTS)? MASKING POLICY policyName=qualifiedName FORCE?
    ;

alterMaskingPolicyStatement
    : ALTER MASKING POLICY (IF EXISTS)? policyName=qualifiedName SET BODY ARROW expression
    | ALTER MASKING POLICY (IF EXISTS)? policyName=qualifiedName SET COMMENT EQ string
    | ALTER MASKING POLICY (IF EXISTS)? policyName=qualifiedName RENAME TO newPolicyName=identifier
    ;

showMaskingPolicyStatement
    : SHOW MASKING POLICIES ((FROM | IN) db=qualifiedName)?
    ;

showCreateMaskingPolicyStatement
    : SHOW CREATE MASKING POLICY policyName=qualifiedName
    ;

createRowAccessPolicyStatement
    : CREATE (OR REPLACE)? ROW ACCESS POLICY (IF NOT EXISTS)? policyName=qualifiedName
      AS LEFT_PAREN policySignature (COMMA policySignature)* RIGHT_PAREN RETURNS BOOLEAN ARROW expression comment?
    ;

dropRowAccessPolicyStatement
    : DROP (IF EXISTS)? ROW ACCESS POLICY policyName=qualifiedName FORCE?
    ;

alterRowAccessPolicyStatement
    : ALTER ROW ACCESS POLICY (IF EXISTS)? policyName=qualifiedName SET BODY ARROW expression
    | ALTER ROW ACCESS POLICY (IF EXISTS)? policyName=qualifiedName SET COMMENT EQ string
    | ALTER ROW ACCESS POLICY (IF EXISTS)? policyName=qualifiedName RENAME TO newPolicyName=identifier
    ;

showRowAccessPolicyStatement
    : SHOW ROW ACCESS POLICIES ((FROM | IN) db=qualifiedName)?
    ;

showCreateRowAccessPolicyStatement
    : SHOW CREATE ROW ACCESS POLICY policyName=qualifiedName
    ;

policySignature : identifier type;

// ---------------------------------------- Backup Restore Statement ---------------------------------------------------

backupStatement
    : BACKUP SNAPSHOT qualifiedName
    TO identifier
    (ON LEFT_PAREN tableDesc (COMMA tableDesc) * RIGHT_PAREN)?
    (PROPERTIES propertyList)?
    ;

cancelBackupStatement
    : CANCEL BACKUP ((FROM | IN) identifier)?
    ;

showBackupStatement
    : SHOW BACKUP ((FROM | IN) identifier)?
    ;

restoreStatement
    : RESTORE SNAPSHOT qualifiedName
    FROM identifier
    (ON LEFT_PAREN restoreTableDesc (COMMA restoreTableDesc) * RIGHT_PAREN)?
    (PROPERTIES propertyList)?
    ;

cancelRestoreStatement
    : CANCEL RESTORE ((FROM | IN) identifier)?
    ;

showRestoreStatement
    : SHOW RESTORE ((FROM | IN) identifier)? (WHERE where=expression)?
    ;

showSnapshotStatement
    : SHOW SNAPSHOT ON identifier
    (WHERE expression)?
    ;

createRepositoryStatement
    : CREATE (READ ONLY)? REPOSITORY identifier
    WITH BROKER identifier?
    ON LOCATION string
    PROPERTIES propertyList
    ;

dropRepositoryStatement
    : DROP REPOSITORY identifier
    ;

// ------------------------------------ Sql BlackList And WhiteList Statement ------------------------------------------

addSqlBlackListStatement
    : ADD SQLBLACKLIST string
    ;

delSqlBlackListStatement
    : DELETE SQLBLACKLIST INTEGER_VALUE (COMMA INTEGER_VALUE)*
    ;

showSqlBlackListStatement
    : SHOW SQLBLACKLIST
    ;

showWhiteListStatement
    : SHOW WHITELIST
    ;

// ------------------------------------------- Export Statement --------------------------------------------------------

exportStatement
    : EXPORT TABLE tableDesc columnAliases? TO string properties? brokerDesc?
    ;

cancelExportStatement
    : CANCEL EXPORT ((FROM | IN) catalog=qualifiedName)? ((LIKE pattern=string) | (WHERE expression))?
    ;

showExportStatement
    : SHOW EXPORT ((FROM | IN) catalog=qualifiedName)?
        ((LIKE pattern=string) | (WHERE expression))?
        (ORDER BY sortItem (COMMA sortItem)*)? (limitElement)?
    ;

// ------------------------------------------- Plugin Statement --------------------------------------------------------

installPluginStatement
    : INSTALL PLUGIN FROM identifierOrString properties?
    ;

uninstallPluginStatement
    : UNINSTALL PLUGIN identifierOrString
    ;

// ------------------------------------------- File Statement ----------------------------------------------------------

createFileStatement
    : CREATE FILE string ((FROM | IN) catalog=qualifiedName)? properties
    ;

dropFileStatement
    : DROP FILE string ((FROM | IN) catalog=qualifiedName)? properties
    ;

showSmallFilesStatement
    : SHOW FILE ((FROM | IN) catalog=qualifiedName)?
    ;

// ------------------------------------------- Set Statement -----------------------------------------------------------

setStatement
    : SET setVar (COMMA setVar)*
    ;

setVar
    : (CHAR SET | CHARSET | CHARACTER SET) (identifierOrString | DEFAULT)                       #setNames
    | NAMES (charset = identifierOrString | DEFAULT)
        (COLLATE (collate = identifierOrString | DEFAULT))?                                     #setNames
    | PASSWORD EQ (string | PASSWORD LEFT_PAREN string RIGHT_PAREN)                                           #setPassword
    | PASSWORD FOR user EQ (string | PASSWORD LEFT_PAREN string RIGHT_PAREN)                                  #setPassword
    | userVariable EQ expression                                                               #setUserVar
    | varType? identifier EQ setExprOrDefault                                                  #setSystemVar
    | systemVariable EQ setExprOrDefault                                                       #setSystemVar
    | varType? TRANSACTION transaction_characteristics                                          #setTransaction
    ;

transaction_characteristics
    : transaction_access_mode
    | isolation_level
    | transaction_access_mode COMMA isolation_level
    | isolation_level COMMA transaction_access_mode
    ;

transaction_access_mode
    : READ ONLY
    | READ WRITE
    ;

isolation_level
    : ISOLATION LEVEL isolation_types
    ;

isolation_types
    : READ UNCOMMITTED
    | READ COMMITTED
    | REPEATABLE READ
    | SERIALIZABLE
    ;

setExprOrDefault
    : DEFAULT
    | ON
    | ALL
    | expression
    ;

setUserPropertyStatement
    : SET PROPERTY (FOR string)? userPropertyList
    ;

roleList
    : identifierOrString (COMMA identifierOrString)*
    ;

setWarehouseStatement
    : SET WAREHOUSE identifierOrString
    ;

executeScriptStatement
    : ADMIN EXECUTE ON (FRONTEND | INTEGER_VALUE) string
    ;

unsupportedStatement
    : START TRANSACTION (WITH CONSISTENT SNAPSHOT)?
    | BEGIN WORK?
    | COMMIT WORK? (AND NO? CHAIN)? (NO? RELEASE)?
    | ROLLBACK WORK? (AND NO? CHAIN)? (NO? RELEASE)?
    | LOCK TABLES lock_item (COMMA lock_item)*
    | UNLOCK TABLES
    ;

lock_item
    : identifier (AS? alias=identifier)? lock_type
    ;

lock_type
    : READ LOCAL?
    | LOW_PRIORITY? WRITE
    ;

// ------------------------------------------- Query Statement ---------------------------------------------------------

queryStatement
    : (explainDesc | optimizerTrace) ? queryRelation outfile?;

queryRelation
    : withClause? queryNoWith
    ;

withClause
    : WITH commonTableExpression (COMMA commonTableExpression)*
    ;

queryNoWith
    : queryPrimary (ORDER BY sortItem (COMMA sortItem)*)? (limitElement)?
    ;

temporalClause
    : AS OF expression
    | FOR SYSTEM_TIME AS OF TIMESTAMP string
    | FOR SYSTEM_TIME BETWEEN expression AND expression
    | FOR SYSTEM_TIME FROM expression TO expression
    | FOR SYSTEM_TIME ALL
    ;

queryPrimary
    : querySpecification                                                                    #queryPrimaryDefault
    | subquery                                                                              #queryWithParentheses
    | left=queryPrimary operator=INTERSECT setQuantifier? right=queryPrimary                #setOperation
    | left=queryPrimary operator=(UNION | EXCEPT | MINUS)
        setQuantifier? right=queryPrimary                                                   #setOperation
    ;

subquery
    : LEFT_PAREN queryRelation RIGHT_PAREN
    ;

rowConstructor
     :LEFT_PAREN expressionList RIGHT_PAREN
     ;

sortItem
    : expression ordering = (ASC | DESC)? (NULLS nullOrdering=(FIRST | LAST))?
    ;

limitElement
    : LIMIT limit =INTEGER_VALUE (OFFSET offset=INTEGER_VALUE)?
    | LIMIT offset =INTEGER_VALUE COMMA limit=INTEGER_VALUE
    ;

querySpecification
    : SELECT setVarHint* setQuantifier? selectItem (COMMA selectItem)*
      fromClause
      ((WHERE where=expression)? (GROUP BY groupingElement)? (HAVING having=expression)?
       (QUALIFY qualifyFunction=selectItem comparisonOperator limit=INTEGER_VALUE)?)
    ;

fromClause
    : (FROM relations)?                                                                 #from
    | FROM DUAL                                                                         #dual
    ;

groupingElement
    : ROLLUP LEFT_PAREN (expressionList)? RIGHT_PAREN                                                  #rollup
    | CUBE LEFT_PAREN (expressionList)? RIGHT_PAREN                                                    #cube
    | GROUPING SETS LEFT_PAREN groupingSet (COMMA groupingSet)* RIGHT_PAREN                              #multipleGroupingSets
    | expressionList                                                                    #singleGroupingSet
    ;

groupingSet
    : LEFT_PAREN expression? (COMMA expression)* RIGHT_PAREN
    ;

commonTableExpression
    : name=identifier (columnAliases)? AS LEFT_PAREN queryRelation RIGHT_PAREN
    ;

setQuantifier
    : DISTINCT
    | ALL
    ;

selectItem
    : expression (AS? (identifier | string))?                                            #selectSingle
    | qualifiedName DOT ASTERISK_SYMBOL                                                  #selectAll
    | ASTERISK_SYMBOL                                                                    #selectAll
    ;

relations
    : relation (COMMA LATERAL? relation)*
    ;

relation
    : relationPrimary joinRelation*
    | LEFT_PAREN relationPrimary joinRelation* RIGHT_PAREN
    ;

relationPrimary
    : qualifiedName temporalClause? partitionNames? tabletList? (
        AS? alias=identifier)? bracketHint?                                             #tableAtom
    | LEFT_PAREN VALUES rowConstructor (COMMA rowConstructor)* RIGHT_PAREN
        (AS? alias=identifier columnAliases?)?                                          #inlineTable
    | subquery (AS? alias=identifier columnAliases?)?                                   #subqueryWithAlias
    | qualifiedName LEFT_PAREN expressionList RIGHT_PAREN
        (AS? alias=identifier columnAliases?)?                                          #tableFunction
    | TABLE LEFT_PAREN qualifiedName LEFT_PAREN expressionList RIGHT_PAREN RIGHT_PAREN
        (AS? alias=identifier columnAliases?)?                                          #normalizedTableFunction
    | LEFT_PAREN relations RIGHT_PAREN                                                                 #parenthesizedRelation
    ;

joinRelation
    : crossOrInnerJoinType bracketHint?
            LATERAL? rightRelation=relationPrimary joinCriteria?
    | outerAndSemiJoinType bracketHint?
            LATERAL? rightRelation=relationPrimary joinCriteria
    ;

crossOrInnerJoinType
    : JOIN | INNER JOIN
    | CROSS | CROSS JOIN
    ;

outerAndSemiJoinType
    : LEFT JOIN | RIGHT JOIN | FULL JOIN
    | LEFT OUTER JOIN | RIGHT OUTER JOIN
    | FULL OUTER JOIN
    | LEFT SEMI JOIN | RIGHT SEMI JOIN
    | LEFT ANTI JOIN | RIGHT ANTI JOIN
    ;

bracketHint
    : LEFT_BRACKET identifier (COMMA identifier)* RIGHT_BRACKET
    ;

setVarHint
    : HENT_START SET_VAR LEFT_PAREN hintMap (COMMA hintMap)* RIGHT_PAREN HENT_ENDHENT_END
    ;

hintMap
    : k=identifierOrString EQ v=literalExpression
    ;

joinCriteria
    : ON expression
    | USING LEFT_PAREN identifier (COMMA identifier)* RIGHT_PAREN
    ;

columnAliases
    : LEFT_PAREN identifier (COMMA identifier)* RIGHT_PAREN
    ;

// partitionNames should not support string, it should be identifier here only for compatibility with historical bugs
partitionNames
    : TEMPORARY? (PARTITION | PARTITIONS) LEFT_PAREN identifierOrString (COMMA identifierOrString)* RIGHT_PAREN
    | TEMPORARY? (PARTITION | PARTITIONS) identifierOrString
    | keyPartitions
    ;

keyPartitions
    : PARTITION LEFT_PAREN keyPartition (COMMA keyPartition)* RIGHT_PAREN                              #keyPartitionList
    ;

tabletList
    : TABLET LEFT_PAREN INTEGER_VALUE (COMMA INTEGER_VALUE)* RIGHT_PAREN
    ;

// ------------------------------------------- Expression --------------------------------------------------------------

/**
 * Operator precedences are shown in the following list, from highest precedence to the lowest.
 *
 * !
 * - (unary minus), ~ (unary bit inversion)
 * ^
 * *, /, DIV, %, MOD
 * -, +
 * &
 * |
 * = (comparison), <=>, >=, >, <=, <, <>, !=, IS, LIKE, REGEXP
 * BETWEEN, CASE WHEN
 * NOT
 * AND, &&
 * XOR
 * OR, ||
 * = (assignment)
 */

expressionsWithDefault
    : LEFT_PAREN expressionOrDefault (COMMA expressionOrDefault)* RIGHT_PAREN
    ;

expressionOrDefault
    : expression | DEFAULT
    ;

mapExpressionList
    : mapExpression (COMMA mapExpression)*
    ;

mapExpression
    : key=expression COLON value=expression
    ;

expressionSingleton
    : expression EOF
    ;

expression
    : booleanExpression                                                                   #expressionDefault
    | NOT expression                                                                      #logicalNot
    | left=expression operator=(AND|LOGICAL_AND) right=expression                         #logicalBinary
    | left=expression operator=(OR|LOGICAL_OR) right=expression                           #logicalBinary
    ;

expressionList
    : expression (COMMA expression)*
    ;

booleanExpression
    : predicate                                                                           #booleanExpressionDefault
    | booleanExpression IS NOT? NULL                                                      #isNull
    | left = booleanExpression comparisonOperator right = predicate                       #comparison
    | booleanExpression comparisonOperator LEFT_PAREN queryRelation RIGHT_PAREN                          #scalarSubquery
    ;

predicate
    : valueExpression (predicateOperations[$valueExpression.ctx])?
    | tupleInSubquery
    ;

tupleInSubquery
    : LEFT_PAREN expression (COMMA expression)+ RIGHT_PAREN NOT? IN LEFT_PAREN queryRelation RIGHT_PAREN
    ;

predicateOperations [ParserRuleContext value]
    : NOT? IN LEFT_PAREN queryRelation RIGHT_PAREN                                                       #inSubquery
    | NOT? IN LEFT_PAREN expressionList RIGHT_PAREN                                                      #inList
    | NOT? BETWEEN lower = valueExpression AND upper = predicate                          #between
    | NOT? (LIKE | RLIKE | REGEXP) pattern=valueExpression                                #like
    ;

valueExpression
    : primaryExpression                                                                   #valueExpressionDefault
    | left = valueExpression operator = BITXOR right = valueExpression                    #arithmeticBinary
    | left = valueExpression operator = (
              ASTERISK_SYMBOL
            | SLASH_SYMBOL
            | PERCENT_SYMBOL
            | INT_DIV
            | MOD)
      right = valueExpression                                                             #arithmeticBinary
    | left = valueExpression operator = (PLUS_SYMBOL | MINUS_SYMBOL)
        right = valueExpression                                                           #arithmeticBinary
    | left = valueExpression operator = BITAND right = valueExpression                    #arithmeticBinary
    | left = valueExpression operator = BITOR right = valueExpression                     #arithmeticBinary
    | left = valueExpression operator = BIT_SHIFT_LEFT right = valueExpression              #arithmeticBinary
    | left = valueExpression operator = BIT_SHIFT_RIGHT right = valueExpression             #arithmeticBinary
    | left = valueExpression operator = BIT_SHIFT_RIGHT_LOGICAL right = valueExpression     #arithmeticBinary
    ;

primaryExpression
    : userVariable                                                                        #userVariableExpression
    | systemVariable                                                                      #systemVariableExpression
    | functionCall                                                                        #functionCallExpression
    | LEFT_BRACE FN functionCall RIGHT_BRACE                                              #odbcFunctionCallExpression
    | primaryExpression COLLATE (identifier | string)                                     #collate
    | literalExpression                                                                   #literal
    | columnReference                                                                     #columnRef
    | base = primaryExpression (DOT_IDENTIFIER | DOT fieldName = identifier )             #dereference
    | left = primaryExpression CONCAT right = primaryExpression                           #concat
    | operator = (MINUS_SYMBOL | PLUS_SYMBOL | BITNOT) primaryExpression                  #arithmeticUnary
    | operator = LOGICAL_NOT primaryExpression                                            #arithmeticUnary
    | LEFT_PAREN expression RIGHT_PAREN                                                                  #parenthesizedExpression
    | EXISTS LEFT_PAREN queryRelation RIGHT_PAREN                                                        #exists
    | subquery                                                                            #subqueryExpression
    | CAST LEFT_PAREN expression AS type RIGHT_PAREN                                                     #cast
    | CONVERT LEFT_PAREN expression COMMA type RIGHT_PAREN                                                 #convert
    | CASE caseExpr=expression whenClause+ (ELSE elseExpression=expression)? END          #simpleCase
    | CASE whenClause+ (ELSE elseExpression=expression)? END                              #searchedCase
    | arrayType? LEFT_BRACKET (expressionList)? RIGHT_BRACKET                             #arrayConstructor
    | mapType? LEFT_BRACE (mapExpressionList)? RIGHT_BRACE                                               #mapConstructor
    | value=primaryExpression LEFT_BRACKET index=valueExpression RIGHT_BRACKET                               #collectionSubscript
    | primaryExpression LEFT_BRACKET start=INTEGER_VALUE? COLON end=INTEGER_VALUE? RIGHT_BRACKET               #arraySlice
    | primaryExpression ARROW string                                                      #arrowExpression
    | (identifier | identifierList) ARROW expression                                       #lambdaFunctionExpr
    | identifierList ARROW LEFT_PAREN (expressionList)? RIGHT_PAREN                       #lambdaFunctionExpr
    ;

literalExpression
    : NULL                                                                                #nullLiteral
    | booleanValue                                                                        #booleanLiteral
    | number                                                                              #numericLiteral
    | (DATE | DATETIME) string                                                            #dateLiteral
    | string                                                                              #stringLiteral
    | interval                                                                            #intervalLiteral
    | unitBoundary                                                                        #unitBoundaryLiteral
    | binary                                                                              #binaryLiteral
    ;

functionCall
    : EXTRACT LEFT_PAREN identifier FROM valueExpression RIGHT_PAREN                                     #extract
    | GROUPING LEFT_PAREN (expression (COMMA expression)*)? RIGHT_PAREN                                    #groupingOperation
    | GROUPING_ID LEFT_PAREN (expression (COMMA expression)*)? RIGHT_PAREN                                 #groupingOperation
    | informationFunctionExpression                                                       #informationFunction
    | specialDateTimeExpression                                                           #specialDateTime
    | specialFunctionExpression                                                           #specialFunction
    | aggregationFunction over?                                                           #aggregationFunctionCall
    | windowFunction over                                                                 #windowFunctionCall
    | qualifiedName LEFT_PAREN (expression (COMMA expression)*)? RIGHT_PAREN  over?                        #simpleFunctionCall
    ;

aggregationFunction
    : AVG LEFT_PAREN setQuantifier? expression RIGHT_PAREN
    | COUNT LEFT_PAREN ASTERISK_SYMBOL? RIGHT_PAREN
    | COUNT LEFT_PAREN (setQuantifier bracketHint?)? (expression (COMMA expression)*)? RIGHT_PAREN
    | MAX LEFT_PAREN setQuantifier? expression RIGHT_PAREN
    | MIN LEFT_PAREN setQuantifier? expression RIGHT_PAREN
    | SUM LEFT_PAREN setQuantifier? expression RIGHT_PAREN
    | ARRAY_AGG LEFT_PAREN expression (ORDER BY sortItem (COMMA sortItem)*)? RIGHT_PAREN
    ;

userVariable
    : AT identifierOrString
    ;

systemVariable
    : AT AT (varType DOT)? identifier
    ;

columnReference
    : identifier
    ;

informationFunctionExpression
    : name = CATALOG LEFT_PAREN RIGHT_PAREN
    | name = DATABASE LEFT_PAREN RIGHT_PAREN
    | name = SCHEMA LEFT_PAREN RIGHT_PAREN
    | name = USER LEFT_PAREN RIGHT_PAREN
    | name = CURRENT_USER (LEFT_PAREN RIGHT_PAREN)?
    | name = CURRENT_ROLE (LEFT_PAREN RIGHT_PAREN)?
    ;

specialDateTimeExpression
    : name = CURRENT_DATE (LEFT_PAREN RIGHT_PAREN)?
    | name = CURRENT_TIME (LEFT_PAREN RIGHT_PAREN)?
    | name = CURRENT_TIMESTAMP (LEFT_PAREN RIGHT_PAREN)?
    | name = LOCALTIME (LEFT_PAREN RIGHT_PAREN)?
    | name = LOCALTIMESTAMP (LEFT_PAREN RIGHT_PAREN)?
    ;

specialFunctionExpression
    : CHAR LEFT_PAREN expression RIGHT_PAREN
    | DAY LEFT_PAREN expression RIGHT_PAREN
    | HOUR LEFT_PAREN expression RIGHT_PAREN
    | IF LEFT_PAREN (expression (COMMA expression)*)? RIGHT_PAREN
    | LEFT LEFT_PAREN expression COMMA expression RIGHT_PAREN
    | LIKE LEFT_PAREN expression COMMA expression RIGHT_PAREN
    | MINUTE LEFT_PAREN expression RIGHT_PAREN
    | MOD LEFT_PAREN expression COMMA expression RIGHT_PAREN
    | MONTH LEFT_PAREN expression RIGHT_PAREN
    | QUARTER LEFT_PAREN expression RIGHT_PAREN
    | REGEXP LEFT_PAREN expression COMMA expression RIGHT_PAREN
    | REPLACE LEFT_PAREN (expression (COMMA expression)*)? RIGHT_PAREN
    | RIGHT LEFT_PAREN expression COMMA expression RIGHT_PAREN
    | RLIKE LEFT_PAREN expression COMMA expression RIGHT_PAREN
    | SECOND LEFT_PAREN expression RIGHT_PAREN
    | TIMESTAMPADD LEFT_PAREN unitIdentifier COMMA expression COMMA expression RIGHT_PAREN
    | TIMESTAMPDIFF LEFT_PAREN unitIdentifier COMMA expression COMMA expression RIGHT_PAREN
    //| WEEK LEFT_PAREN expression RIGHT_PAREN TODO: Support week(expr) function
    | YEAR LEFT_PAREN expression RIGHT_PAREN
    | PASSWORD LEFT_PAREN string RIGHT_PAREN
    | FLOOR LEFT_PAREN expression RIGHT_PAREN
    | CEIL LEFT_PAREN expression RIGHT_PAREN
    ;

windowFunction
    : name = ROW_NUMBER LEFT_PAREN RIGHT_PAREN
    | name = RANK LEFT_PAREN RIGHT_PAREN
    | name = DENSE_RANK LEFT_PAREN RIGHT_PAREN
    | name = NTILE  LEFT_PAREN expression? RIGHT_PAREN
    | name = LEAD  LEFT_PAREN (expression ignoreNulls? (COMMA expression)*)? RIGHT_PAREN ignoreNulls?
    | name = LAG LEFT_PAREN (expression ignoreNulls? (COMMA expression)*)? RIGHT_PAREN ignoreNulls?
    | name = FIRST_VALUE LEFT_PAREN (expression ignoreNulls? (COMMA expression)*)? RIGHT_PAREN ignoreNulls?
    | name = LAST_VALUE LEFT_PAREN (expression ignoreNulls? (COMMA expression)*)? RIGHT_PAREN ignoreNulls?
    ;

whenClause
    : WHEN condition=expression THEN result=expression
    ;

over
    : OVER LEFT_PAREN
        (bracketHint? PARTITION BY partition+=expression (COMMA partition+=expression)*)?
        (ORDER BY sortItem (COMMA sortItem)*)?
        windowFrame?
      RIGHT_PAREN
    ;

ignoreNulls
    : IGNORE NULLS
    ;

windowFrame
    : frameType=RANGE start=frameBound
    | frameType=ROWS start=frameBound
    | frameType=RANGE BETWEEN start=frameBound AND end=frameBound
    | frameType=ROWS BETWEEN start=frameBound AND end=frameBound
    ;

frameBound
    : UNBOUNDED boundType=PRECEDING                 #unboundedFrame
    | UNBOUNDED boundType=FOLLOWING                 #unboundedFrame
    | CURRENT ROW                                   #currentRowBound
    | expression boundType=(PRECEDING | FOLLOWING)  #boundedFrame
    ;

// ------------------------------------------- COMMON AST --------------------------------------------------------------

tableDesc
    : qualifiedName partitionNames?
    ;

restoreTableDesc
    : qualifiedName partitionNames? (AS identifier)?
    ;

explainDesc
    : (DESC | DESCRIBE | EXPLAIN) (LOGICAL | VERBOSE | COSTS)?
    ;

optimizerTrace
    : TRACE OPTIMIZER
    ;

partitionDesc
    : PARTITION BY RANGE identifierList LEFT_PAREN (rangePartitionDesc (COMMA rangePartitionDesc)*)? RIGHT_PAREN
    | PARTITION BY RANGE primaryExpression LEFT_PAREN (rangePartitionDesc (COMMA rangePartitionDesc)*)? RIGHT_PAREN
    | PARTITION BY LIST identifierList LEFT_PAREN (listPartitionDesc (COMMA listPartitionDesc)*)? RIGHT_PAREN
    | PARTITION BY identifierList
    | PARTITION BY functionCall LEFT_PAREN (rangePartitionDesc (COMMA rangePartitionDesc)*)? RIGHT_PAREN
    | PARTITION BY functionCall
    ;

listPartitionDesc
    : singleItemListPartitionDesc
    | multiItemListPartitionDesc
    ;

singleItemListPartitionDesc
    : PARTITION (IF NOT EXISTS)? identifier VALUES IN stringList propertyList?
    ;

multiItemListPartitionDesc
    : PARTITION (IF NOT EXISTS)? identifier VALUES IN LEFT_PAREN stringList (COMMA stringList)* RIGHT_PAREN propertyList?
    ;

stringList
    : LEFT_PAREN string (COMMA string)* RIGHT_PAREN
    ;

rangePartitionDesc
    : singleRangePartition
    | multiRangePartition
    ;

singleRangePartition
    : PARTITION (IF NOT EXISTS)? identifier VALUES partitionKeyDesc propertyList?
    ;

multiRangePartition
    : START LEFT_PAREN string RIGHT_PAREN END LEFT_PAREN string RIGHT_PAREN EVERY LEFT_PAREN interval RIGHT_PAREN
    | START LEFT_PAREN string RIGHT_PAREN END LEFT_PAREN string RIGHT_PAREN EVERY LEFT_PAREN INTEGER_VALUE RIGHT_PAREN
    ;

partitionRangeDesc
    : START LEFT_PAREN string RIGHT_PAREN END LEFT_PAREN string RIGHT_PAREN
    ;

partitionKeyDesc
    : LESS THAN (MAXVALUE | partitionValueList)
    | LEFT_BRACKET partitionValueList COMMA partitionValueList RIGHT_PAREN
    ;

partitionValueList
    : LEFT_PAREN partitionValue (COMMA partitionValue)* RIGHT_PAREN
    ;

keyPartition
    : partitionColName=identifier EQ partitionColValue=literalExpression
    ;

partitionValue
    : MAXVALUE | string
    ;

distributionClause
    : DISTRIBUTED BY HASH identifierList (BUCKETS INTEGER_VALUE)?
    | DISTRIBUTED BY HASH identifierList
    ;

distributionDesc
    : DISTRIBUTED BY HASH identifierList (BUCKETS INTEGER_VALUE)?
    | DISTRIBUTED BY HASH identifierList
    | DISTRIBUTED BY RANDOM (BUCKETS INTEGER_VALUE)?
    ;

refreshSchemeDesc
    : REFRESH (IMMEDIATE | DEFERRED)? (ASYNC
    | ASYNC (START LEFT_PAREN string RIGHT_PAREN)? EVERY LEFT_PAREN interval RIGHT_PAREN
    | INCREMENTAL
    | MANUAL)
    ;

properties
    : PROPERTIES LEFT_PAREN property (COMMA property)* RIGHT_PAREN
    ;

extProperties
    : BROKER properties
    ;

propertyList
    : LEFT_PAREN property (COMMA property)* RIGHT_PAREN
    ;

userPropertyList
    : property (COMMA property)*
    ;

property
    : key=string EQ value=string
    ;

varType
    : GLOBAL
    | LOCAL
    | SESSION
    | VERBOSE
    ;

comment
    : COMMENT string
    ;

outfile
    : INTO OUTFILE file=string fileFormat? properties?
    ;

fileFormat
    : FORMAT AS (identifier | string)
    ;

string
    : SINGLE_QUOTED_TEXT
    | DOUBLE_QUOTED_TEXT
    ;

binary
    : BINARY_SINGLE_QUOTED_TEXT
    | BINARY_DOUBLE_QUOTED_TEXT
    ;

comparisonOperator
    : EQ | NEQ | LT | LTE | GT | GTE | EQ_FOR_NULL
    ;

booleanValue
    : TRUE | FALSE
    ;

interval
    : INTERVAL value=expression from=unitIdentifier
    ;

unitIdentifier
    : YEAR | MONTH | WEEK | DAY | HOUR | MINUTE | SECOND | QUARTER
    ;

unitBoundary
    : FLOOR | CEIL
    ;

type
    : baseType
    | decimalType
    | arrayType
    | structType
    | mapType
    ;

arrayType
    : ARRAY LT type GT
    ;

mapType
    : MAP LT type COMMA type GT
    ;

subfieldDesc
    : identifier type
    ;

subfieldDescs
    : subfieldDesc (COMMA subfieldDesc)*
    ;

structType
    : STRUCT LT subfieldDescs GT
    ;

typeParameter
    : LEFT_PAREN INTEGER_VALUE RIGHT_PAREN
    ;

baseType
    : BOOLEAN
    | TINYINT typeParameter?
    | SMALLINT typeParameter?
    | SIGNED INT?
    | SIGNED INTEGER?
    | UNSIGNED INT?
    | UNSIGNED INTEGER?
    | INT typeParameter?
    | INTEGER typeParameter?
    | BIGINT typeParameter?
    | LARGEINT typeParameter?
    | FLOAT
    | DOUBLE
    | DATE
    | DATETIME
    | TIME
    | CHAR typeParameter?
    | VARCHAR typeParameter?
    | STRING
    | TEXT
    | BITMAP
    | HLL
    | PERCENTILE
    | JSON
    | VARBINARY typeParameter?
    | BINARY typeParameter?
    ;

decimalType
    : (DECIMAL | DECIMALV2 | DECIMAL32 | DECIMAL64 | DECIMAL128 | NUMERIC | NUMBER )
        (LEFT_PAREN precision=INTEGER_VALUE (COMMA scale=INTEGER_VALUE)? RIGHT_PAREN)?
    ;

qualifiedName
    : identifier (DOT_IDENTIFIER | DOT identifier)*
    ;

identifier
    : LETTER_IDENTIFIER      #unquotedIdentifier
    | nonReserved            #unquotedIdentifier
    | DIGIT_IDENTIFIER       #digitIdentifier
    | BACKQUOTED_IDENTIFIER  #backQuotedIdentifier
    ;

identifierList
    : LEFT_PAREN identifier (COMMA identifier)* RIGHT_PAREN
    ;

identifierOrString
    : identifier
    | string
    ;

identifierOrStringList
    : identifierOrString (COMMA identifierOrString)*
    ;

identifierOrStringOrStar
    : ASTERISK_SYMBOL
    | identifier
    | string
    ;

user
    : identifierOrString                                     # userWithoutHost
    | identifierOrString AT identifierOrString              # userWithHost
    | identifierOrString AT LEFT_BRACKET identifierOrString RIGHT_BRACKET      # userWithHostAndBlanket
    ;

assignment
    : identifier EQ expressionOrDefault
    ;

assignmentList
    : assignment (COMMA assignment)*
    ;

number
    : DECIMAL_VALUE  #decimalValue
    | DOUBLE_VALUE   #doubleValue
    | INTEGER_VALUE  #integerValue
    ;

nonReserved
    : ACCESS | AFTER | AGGREGATE | APPLY | ASYNC | AUTHORS | AVG | ADMIN | ANTI | AUTHENTICATION | AUTO_INCREMENT
    | BACKEND | BACKENDS | BACKUP | BEGIN | BITMAP_UNION | BLACKLIST | BINARY | BODY | BOOLEAN | BROKER | BUCKETS
    | BUILTIN | BASE
    | CAST | CANCEL | CATALOG | CATALOGS | CEIL | CHAIN | CHARSET | CLEAN | CLUSTER | CLUSTERS | CURRENT | COLLATION | COLUMNS
    | CUMULATIVE | COMMENT | COMMIT | COMMITTED | COMPUTE | CONNECTION | CONSISTENT | COSTS | COUNT
    | CONFIG | COMPACT
    | DATA | DATE | DATETIME | DAY | DECOMMISSION | DISTRIBUTION | DUPLICATE | DYNAMIC | DISTRIBUTED
    | END | ENGINE | ENGINES | ERRORS | EVENTS | EXECUTE | EXTERNAL | EXTRACT | EVERY | ENCLOSE | ESCAPE | EXPORT
    | FIELDS | FILE | FILTER | FIRST | FLOOR | FOLLOWING | FORMAT | FN | FRONTEND | FRONTENDS | FOLLOWER | FREE
    | FUNCTIONS
    | GLOBAL | GRANTS
    | HASH | HISTOGRAM | HELP | HLL_UNION | HOST | HOUR | HUB
    | IDENTIFIED | IMAGE | IMPERSONATE | INCREMENTAL | INDEXES | INSTALL | INTEGRATION | INTEGRATIONS | INTERMEDIATE
    | INTERVAL | ISOLATION
    | JOB
    | LABEL | LAST | LESS | LEVEL | LIST | LOCAL | LOCATION | LOGICAL | LOW_PRIORITY | LOCK | LOCATIONS
    | MASKING | MANUAL | MAP | MAPPING | MAPPINGS | MATERIALIZED | MAX | META | MIN | MINUTE | MODE | MODIFY | MONTH | MERGE | MINUS
    | NAME | NAMES | NEGATIVE | NO | NODE | NODES | NONE | NULLS | NUMBER | NUMERIC
    | OBSERVER | OF | OFFSET | ONLY | OPTIMIZER | OPEN | OPERATE | OPTION | OVERWRITE
    | PARTITIONS | PASSWORD | PATH | PAUSE | PENDING | PERCENTILE_UNION | PLUGIN | PLUGINS | POLICY | POLICIES
    | PRECEDING | PROC | PROCESSLIST | PRIVILEGES | PROPERTIES | PROPERTY
    | QUARTER | QUERY | QUEUE | QUOTA | QUALIFY
    | REMOVE | RANDOM | RANK | RECOVER | REFRESH | REPAIR | REPEATABLE | REPLACE_IF_NOT_NULL | REPLICA | REPOSITORY
    | REPOSITORIES
    | RESOURCE | RESOURCES | RESTORE | RESUME | RETURNS | REVERT | ROLE | ROLES | ROLLUP | ROLLBACK | ROUTINE | ROW
    | SAMPLE | SCHEDULER | SECOND | SECURITY | SERIALIZABLE |SEMI | SESSION | SETS | SIGNED | SNAPSHOT | SQLBLACKLIST | START
    | STREAM | SUM | STATUS | STOP | SKIP_HEADER | SWAP
    | STORAGE| STRING | STRUCT | STATS | SUBMIT | SUSPEND | SYNC | SYSTEM_TIME
    | TABLES | TABLET | TASK | TEMPORARY | TIMESTAMP | TIMESTAMPADD | TIMESTAMPDIFF | THAN | TIME | TRANSACTION | TRACE
    | TRIM_SPACE
    | TRIGGERS | TRUNCATE | TYPE | TYPES
    | UNBOUNDED | UNCOMMITTED | UNSET | UNINSTALL | USAGE | USER | USERS | UNLOCK
    | VALUE | VARBINARY | VARIABLES | VIEW | VIEWS | VERBOSE | VOLUME | VOLUMES
    | WARNINGS | WEEK | WHITELIST | WORK | WRITE  | WAREHOUSE | WAREHOUSES
    | YEAR
    | DOTDOTDOT
    ;
