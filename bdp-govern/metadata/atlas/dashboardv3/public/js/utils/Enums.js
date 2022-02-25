/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

define(["require", "backbone"], function(require) {
    'use strict';

    var Enums = {};

    Enums.auditAction = {
        ENTITY_CREATE: "Entity Created",
        ENTITY_UPDATE: "Entity Updated",
        ENTITY_DELETE: "Entity Deleted",
        CLASSIFICATION_ADD: "Classification Added",
        CLASSIFICATION_DELETE: "Classification Deleted",
        CLASSIFICATION_UPDATE: "Classification Updated",
        PROPAGATED_CLASSIFICATION_ADD: "Propagated Classification Added",
        PROPAGATED_CLASSIFICATION_DELETE: "Propagated Classification Deleted",
        PROPAGATED_CLASSIFICATION_UPDATE: "Propagated Classification Updated",
        ENTITY_IMPORT_CREATE: "Entity Created by import",
        ENTITY_IMPORT_UPDATE: "Entity Updated by import",
        ENTITY_IMPORT_DELETE: "Entity Deleted by import",
        TERM_ADD: "Term Added",
        TERM_DELETE: "Term Deleted",
        LABEL_ADD: "Label(s) Added",
        LABEL_DELETE: "Label(s) Deleted",
        ENTITY_PURGE: "Entity Purged",
        BUSINESS_ATTRIBUTE_ADD: "Business Attribute(s) Added",
        BUSINESS_ATTRIBUTE_UPDATE: "Business Attribute(s) Updated",
        BUSINESS_ATTRIBUTE_DELETE: "Business Attribute(s) Deleted",
        CUSTOM_ATTRIBUTE_UPDATE: "User-defined Attribute(s) Updated",
        TYPE_DEF_UPDATE: "Type Updated",
        TYPE_DEF_CREATE: "Type Created",
        TYPE_DEF_DELETE: "Type Deleted",
        IMPORT: "Import",
        EXPORT: "Export"
    }
    Enums.serverAudits = {
        SERVER_START: "Server Start",
        SERVER_STOP: "Server End",
        SERVER_STATE_ACTIVE: "Server State Active",
        SERVER_STATE_PASSIVE: "Server State Passive"
    }

    Enums.category = {
        PRIMITIVE: "Primitive",
        OBJECT_ID_TYPE: "Object Id type",
        ENUM: "Enum",
        STRUCT: "Struct",
        CLASSIFICATION: "Classification",
        ENTITY: "Entity",
        ARRAY: "Array",
        MAP: "Map",
        RELATIONSHIP: "Relationship",
        BUSINESS_METADATA: "Business Metadata",
        PURGE: "Purge Entities",
        IMPORT: "Import Entities",
        EXPORT: "Export Entities"
    }

    Enums.entityStateReadOnly = {
        ACTIVE: false,
        DELETED: true,
        STATUS_ACTIVE: false,
        STATUS_DELETED: true
    }

    Enums.isEntityPurged = {
        PURGED: true
    }

    Enums.lineageUrlType = {
        INPUT: 'inputs',
        OUTPUT: 'outputs',
        SCHEMA: 'schema'
    }

    Enums.searchUrlType = {
        DSL: 'dsl',
        FULLTEXT: 'basic'
    }

    Enums.profileTabType = {
        'count-frequency': "Count Frequency Distribution",
        'decile-frequency': "Decile Frequency Distribution",
        'annual': "Annual Distribution"
    }
    Enums.extractFromUrlForSearch = {
        "searchParameters": {
            "pageLimit": "limit",
            "type": "typeName",
            "tag": "classification",
            "query": "query",
            "pageOffset": "offset",
            "includeDE": "excludeDeletedEntities",
            "excludeST": "includeSubTypes",
            "excludeSC": "includeSubClassifications",
            "tagFilters": "tagFilters",
            "entityFilters": "entityFilters",
            "attributes": "attributes",
            "term": "termName"
        },
        "uiParameters": "uiParameters"
    }
    Enums.regex = {
        RANGE_CHECK: {
            "byte": {
                min: -128,
                max: 127
            },
            "short": {
                min: -32768,
                max: 32767
            },
            "int": {
                min: -2147483648,
                max: 2147483647
            },
            "long": {
                min: -9223372036854775808,
                max: 9223372036854775807
            },
            "float": {
                min: -3.4028235E38,
                max: 3.4028235E38
            },
            "double": {
                min: -1.7976931348623157E308,
                max: 1.7976931348623157E308
            }
        }
    }
    Enums.graphIcon = {
        // hive_db: { icon: "fa-database", textContent: '\uf1c0' },
        // hive_column: { icon: "fa-columns", textContent: '\uf0db' },
        // hive_table: { icon: "fa-table", textContent: '\uf0ce' }
    }
    var getTermRelationAttributes = function() {
        return {
            description: null,
            expression: null,
            steward: null,
            source: null
        }
    }
    Enums.termRelationAttributeList = {
        seeAlso: getTermRelationAttributes(),
        synonyms: getTermRelationAttributes(),
        antonyms: getTermRelationAttributes(),
        preferredTerms: getTermRelationAttributes(),
        preferredToTerms: getTermRelationAttributes(),
        replacementTerms: getTermRelationAttributes(),
        replacedBy: getTermRelationAttributes(),
        translationTerms: getTermRelationAttributes(),
        translatedTerms: getTermRelationAttributes(),
        isA: getTermRelationAttributes(),
        classifies: getTermRelationAttributes(),
        validValues: getTermRelationAttributes(),
        validValuesFor: getTermRelationAttributes()
    }
    Enums.addOnClassification = ["_ALL_CLASSIFICATION_TYPES", "_CLASSIFIED", "_NOT_CLASSIFIED"];
    Enums.addOnEntities = ["_ALL_ENTITY_TYPES"];

    Enums.stats = {
        generalData: {
            "collectionTime": "day"
        },
        Server: {
            "startTimeStamp": "day",
            "activeTimeStamp": "day",
            "upTime": "none"
        },
        ConnectionStatus: {
            "statusBackendStore": "status-html",
            "statusIndexStore": "status-html"
        },
        Notification: {
            "currentDay": "number",
            "currentDayAvgTime": "number",
            "currentDayEntityCreates": "number",
            "currentDayEntityDeletes": "number",
            "currentDayEntityUpdates": "number",
            "currentDayFailed": "number",
            "currentDayStartTime": "day",
            "currentHour": "number",
            "currentHourAvgTime": "millisecond",
            "currentHourEntityCreates": "number",
            "currentHourEntityDeletes": "number",
            "currentHourEntityUpdates": "number",
            "currentHourFailed": "number",
            "currentHourStartTime": "day",
            "lastMessageProcessedTime": "day",
            "offsetCurrent": "number",
            "offsetStart": "number",
            "previousDay": "number",
            "previousDayAvgTime": "millisecond",
            "previousDayEntityCreates": "number",
            "previousDayEntityDeletes": "number",
            "previousDayEntityUpdates": "number",
            "previousDayFailed": "number",
            "previousHour": "number",
            "previousHourAvgTime": "millisecond",
            "previousHourEntityCreates": "number",
            "previousHourEntityDeletes": "number",
            "previousHourEntityUpdates": "number",
            "previousHourFailed": "number",
            "total": "number",
            "totalAvgTime": "millisecond",
            "totalCreates": "number",
            "totalDeletes": "number",
            "totalFailed": "number",
            "totalUpdates": "number",
            "processedMessageCount": "number",
            "failedMessageCount": "number"
        }
    };
    Enums.systemAttributes = {
        "__classificationNames": "Classification(s)",
        "__createdBy": "Created By User",
        "__customAttributes": "User-defined Properties",
        "__guid": "Guid",
        "__isIncomplete": "IsIncomplete",
        "__labels": "Label(s)",
        "__modificationTimestamp": "Last Modified Timestamp",
        "__modifiedBy": "Last Modified User",
        "__propagatedClassificationNames": "Propagated Classification(s)",
        "__state": "Status",
        "__entityStatus": "Entity Status",
        "__timestamp": "Created Timestamp",
        "__typeName": "Type Name"
    };
    Enums.__isIncomplete = {
        0: "false",
        1: "true"
    };

    Enums.queryBuilderUIOperatorToAPI = {
        "=": "eq",
        "!=": "neq",
        "<": "lt",
        "<=": "lte",
        ">": "gt",
        ">=": "gte",
        "begins_with": "startsWith",
        "ends_with": "endsWith",
        "not_null": "notNull",
        "is_null": "isNull",
        "TIME_RANGE": "timerange"
    };

    Enums.queryBuilderApiOperatorToUI = _.invert(Enums.queryBuilderUIOperatorToAPI);

    Enums.queryBuilderDateRangeUIValueToAPI = {
        "Today": "TODAY",
        "Yesterday": "YESTERDAY",
        "Last 7 Days": "LAST_7_DAYS",
        "Last 30 Days": "LAST_30_DAYS",
        "This Month": "THIS_MONTH",
        "Last Month": "LAST_MONTH",
        "This Quarter": "THIS_QUARTER",
        "Last Quarter": "LAST_QUARTER",
        "This Year": "THIS_YEAR",
        "Last Year": "LAST_YEAR",
        "Last 3 Months": "LAST_3_MONTHS",
        "Last 6 Months": "LAST_6_MONTHS",
        "Last 12 Months": "LAST_12_MONTHS"
    };

    Enums.queryBuilderDateRangeAPIValueToUI = _.invert(Enums.queryBuilderDateRangeUIValueToAPI);

    return Enums;
});