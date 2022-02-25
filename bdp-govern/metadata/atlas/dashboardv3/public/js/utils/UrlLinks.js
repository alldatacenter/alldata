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

define(['require', 'utils/Enums', 'utils/Utils', 'underscore'], function(require, Enums, Utils) {
    'use strict';

    var UrlLinks = {
        apiBaseUrl: Utils.getBaseUrl(window.location.pathname)
    };
    _.extend(UrlLinks, {
        baseUrl: UrlLinks.apiBaseUrl + '/api/atlas',
        baseUrlV2: UrlLinks.apiBaseUrl + '/api/atlas/v2',
        typedefsUrl: function() {
            return {
                defs: this.baseUrlV2 + '/types/typedefs',
                def: this.baseUrlV2 + '/types/typedef'
            };
        },
        entitiesDefApiUrl: function(name) {
            return this.getDefApiUrl('entity', name);
        },
        classificationDefApiUrl: function(name) {
            return this.getDefApiUrl('classification', name);
        },
        businessMetadataDefApiUrl: function(name) {
            return this.getDefApiUrl('business_metadata', name);
        },
        enumDefApiUrl: function(name) {
            return this.getDefApiUrl('enum', name);
        },
        metricsApiUrl: function() {
            return this.baseUrl + '/admin/metrics'
        },
        pendingTaskApiUrl: function() {
            return this.baseUrl + '/admin/tasks'
        },
        debugMetricsApiUrl: function() {
            return this.baseUrl + '/admin/debug/metrics'
        },
        regitrydataDefApiUrl: function(name) {
            return this.baseUrlV2 + '/entity/getRegistryData';
        },
        rootEntityDefUrl: function(name) {
            return this.baseUrlV2 + '/types/entitydef/name/' + name;
        },
        rootClassificationDefUrl: function(name) {
            return this.baseUrlV2 + '/types/classificationdef/name/' + name;
        },
        getDefApiUrl: function(type, name) {
            var defApiUrl = this.typedefsUrl(),
                defUrl;
            if (name) {
                defUrl = defApiUrl.def + '/name/' + name;
            } else {
                defUrl = defApiUrl.defs;
            }
            if (type) {
                return defUrl += '?type=' + type;
            } else {
                return defUrl;
            }
        },
        entitiesApiUrl: function(options) {
            var entitiesUrl = this.baseUrlV2 + '/entity';
            if (options) {
                var guid = options.guid,
                    associatedGuid = options.associatedGuid,
                    name = options.name,
                    minExtInfo = options.minExtInfo;
                if (guid && name && associatedGuid) {
                    return entitiesUrl + '/guid/' + guid + '/classification/' + name + '?associatedEntityGuid=' + associatedGuid;
                } else if (guid && name) {
                    entitiesUrl += '/guid/' + guid + '/classification/' + name;
                } else if (guid && !name) {
                    entitiesUrl += '/guid/' + guid;
                }
            }

            if (!minExtInfo) {
                return entitiesUrl;
            } else {
                return entitiesUrl += '?minExtInfo=' + (minExtInfo);
            }
        },
        entityLabelsAPIUrl: function(guid) {
            return this.entitiesApiUrl({ guid: guid }) + "/labels";
        },
        entityHeaderApiUrl: function(guid) {
            return this.entitiesApiUrl({ guid: guid }) + "/header"
        },
        entitiesTraitsApiUrl: function(token) {
            if (token) {
                return this.baseUrlV2 + '/entity/guid/' + token + '/classifications';
            } else {
                // For Multiple Assignment
                return this.baseUrlV2 + '/entity/bulk/classification';
            }
        },
        entitiesBusinessMetadataApiUrl: function(guid) {
            if (guid) {
                return this.baseUrlV2 + '/entity/guid/' + guid + '/businessmetadata?isOverwrite=true';
            }
        },
        entityCollectionaudit: function(guid) {
            return this.baseUrlV2 + '/entity/' + guid + '/audit';
        },
        expimpAudit: function(options) {
            var url = this.baseUrl + '/admin/expimp/audit',
                queryParam = [];
            if (options) {
                var serverName = options.serverName,
                    limit = options.limit,
                    offset = options.offset;
            }

            if (serverName) {
                queryParam.push("serverName=" + serverName);
            }
            if (limit) {
                queryParam.push("limit=" + limit);
            }
            if (offset) {
                queryParam.push("offset=" + offset);
            }
            if (queryParam.length > 0) {
                url = url + "?" + queryParam.join("&");
            }
            return url;
        },
        typesApiUrl: function() {
            return this.typedefsUrl().defs + '/headers?excludeInternalTypesAndReferences=true'
        },
        lineageApiUrl: function(guid) {
            var lineageUrl = this.baseUrlV2 + '/lineage';
            if (guid) {
                return lineageUrl + '/' + guid;
            } else {
                return lineageUrl
            }
        },
        relationshipApiUrl: function(guid) {
            var relationshipUrl = this.baseUrlV2 + '/relationship';
            if (guid) {
                return relationshipUrl + '/guid/' + guid + '?extendedInfo=true';
            } else {
                return relationshipUrl
            }
        },
        schemaApiUrl: function(guid) {
            var lineageUrl = this.baseUrl + '/lineage';
            if (guid) {
                return lineageUrl + '/' + guid + '/schema'
            } else {
                return lineageUrl
            }
        },
        searchApiUrl: function(searchtype) {
            var searchUrl = this.baseUrlV2 + '/search';
            if (searchtype) {
                return searchUrl + '/' + searchtype;
            } else {
                return searchUrl;
            }
        },
        saveSearchApiUrl: function(saveSearchType) {
            var saveSearchUrl = this.searchApiUrl() + "/saved";
            if (saveSearchType) {
                return saveSearchUrl + '/' + saveSearchType;
            } else {
                return saveSearchUrl;
            }
        },
        glossaryApiUrl: function(options) {
            var guid = options && options.guid,
                glossaryUrl = this.baseUrlV2 + '/glossary';
            if (guid) {
                return glossaryUrl + '/' + guid;
            } else {
                return glossaryUrl;
            }
        },
        glossaryImportTempUrl: function() {
            return this.glossaryApiUrl() + '/import/template';
        },
        glossaryImportUrl: function() {
            return this.glossaryApiUrl() + '/import';
        },
        businessMetadataImportTempUrl: function() {
            return this.entitiesApiUrl() + '/businessmetadata/import/template';
        },
        businessMetadataImportUrl: function() {
            return this.entitiesApiUrl() + '/businessmetadata/import';
        },
        apiDocUrl: function() {
            return this.apiBaseUrl + '/apidocs/index.html';
        },
        categoryApiUrl: function(options) {
            var guid = options && options.guid,
                list = options && options.list,
                related = options && options.related,
                categoryUrl = this.glossaryApiUrl() + '/' + (list ? 'categories' : 'category');
            if (guid) {
                if (related) {
                    return categoryUrl + '/' + guid + "/related";
                } else {
                    return categoryUrl + '/' + guid;
                }
            } else {
                return categoryUrl;
            }
        },
        termApiUrl: function(options) {
            var guid = options && options.guid,
                list = options && options.list,
                related = options && options.related,
                termUrl = this.glossaryApiUrl() + '/' + (list ? 'terms' : 'term');
            if (guid) {
                if (related) {
                    return termUrl + '/' + guid + "/related";
                } else {
                    return termUrl + '/' + guid;
                }
            } else {
                return termUrl;
            }
        },
        termToEntityApiUrl: function(guid) {
            var termUrl = this.termApiUrl({ list: true });
            if (guid) {
                return termUrl + '/' + guid + '/assignedEntities';
            }
        },
        versionApiUrl: function() {
            return this.baseUrl + '/admin/version';
        },
        sessionApiUrl: function() {
            return this.baseUrl + '/admin/session';
        },
        adminApiUrl: function() {
            return this.baseUrl + '/admin/audits';
        }
    });

    return UrlLinks;
});