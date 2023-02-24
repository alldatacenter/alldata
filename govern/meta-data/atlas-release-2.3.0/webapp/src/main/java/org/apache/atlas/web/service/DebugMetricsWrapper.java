/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.atlas.web.service;

import org.apache.hadoop.metrics2.MetricsSystem;
import org.apache.hadoop.metrics2.lib.DefaultMetricsSystem;
import org.aspectj.lang.Signature;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import static org.apache.atlas.web.service.DebugMetricsWrapper.Constants.*;

@Lazy()
@Service
public class DebugMetricsWrapper {
	private AtlasDebugMetricsSource debugMetricsSource = new AtlasDebugMetricsSource();

	@Inject
	AtlasDebugMetricsSink debugMetricsRESTSink;

	@PostConstruct
	public void init() {
		MetricsSystem metricsSystem = DefaultMetricsSystem.initialize(DEBUG_METRICS_CONTEXT);
		metricsSystem.register(debugMetricsSource);
        metricsSystem.register(DEBUG_METRICS_REST_SINK, "", debugMetricsRESTSink);
    }

	public void update(Signature name, long timeConsumed) {
		debugMetricsSource.update(name, timeConsumed);
	}

	class Constants {
		public static final String NUM_OPS                       = "numops";
		public static final String MIN_TIME                      = "mintime";
		public static final String MAX_TIME                      = "maxtime";
		public static final String STD_DEV_TIME                  = "stdevtime";
		public static final String AVG_TIME                      = "avgtime";

		public static final String DEBUG_METRICS_SOURCE          = "AtlasDebugMetricsSource";
		public static final String DEBUG_METRICS_CONTEXT         = "atlas-debug-metrics-context";
		public static final String DEBUG_METRICS_REST_SINK       = "AtlasDebugMetricsRESTSink";

		public static final String EntityRESTPrefix              = "EntityREST.";
		public static final String TypesRESTPrefix               = "TypesREST.";
		public static final String GlossaryRESTPrefix            = "GlossaryREST.";
		public static final String DiscoveryRESTPrefix           = "DiscoveryREST.";
		public static final String LineageRESTPrefix             = "LineageREST.";
		public static final String RelationshipRESTPrefix        = "RelationshipREST.";

		public static final String EntityREST_getById                        = EntityRESTPrefix + "getById(..)";
		public static final String EntityREST_createOrUpdate                 = EntityRESTPrefix + "createOrUpdate(..)";
		public static final String EntityREST_partialUpdateEntityAttrByGuid  = EntityRESTPrefix + "partialUpdateEntityAttrByGuid(..)";
		public static final String EntityREST_deleteByGuid                   = EntityRESTPrefix + "deleteByGuid(..)";
		public static final String EntityREST_getClassification              = EntityRESTPrefix + "getClassification(..)";
		public static final String EntityREST_getClassifications             = EntityRESTPrefix + "getClassifications(..)";
		public static final String EntityREST_addClassificationsByUA         = EntityRESTPrefix + "addClassificationsByUniqueAttribute(..)";
		public static final String EntityREST_addClassifications             = EntityRESTPrefix + "addClassifications(..)";
		public static final String EntityREST_deleteClassificationByUA       = EntityRESTPrefix + "deleteClassificationByUniqueAttribute(..)";
		public static final String EntityREST_deleteClassification           = EntityRESTPrefix + "deleteClassification(..)";
		public static final String EntityREST_getHeaderById                  = EntityRESTPrefix + "getHeaderById(..)";
		public static final String EntityREST_getEntityHeaderByUA            = EntityRESTPrefix + "getEntityHeaderByUniqueAttributes(..)";
		public static final String EntityREST_getByUniqueAttributes          = EntityRESTPrefix + "getByUniqueAttributes(..)";
		public static final String EntityREST_partialUpdateEntityByUA        = EntityRESTPrefix + "partialUpdateEntityByUniqueAttrs(..)";
		public static final String EntityREST_deleteByUA                     = EntityRESTPrefix + "deleteByUniqueAttribute(..)";
		public static final String EntityREST_updateClassificationsByUA      = EntityRESTPrefix + "updateClassificationsByUniqueAttribute(..)";
		public static final String EntityREST_updateClassifications          = EntityRESTPrefix + "updateClassifications(..)";
		public static final String EntityREST_getEntitiesByUA                = EntityRESTPrefix + "getEntitiesByUniqueAttributes(..)";
		public static final String EntityREST_getByGuids                     = EntityRESTPrefix + "getByGuids(..)";
		public static final String EntityREST_deleteByGuids                  = EntityRESTPrefix + "deleteByGuids(..)";
		public static final String EntityREST_addClassification              = EntityRESTPrefix + "addClassification(..)";
		public static final String EntityREST_getAuditEvents                 = EntityRESTPrefix + "getAuditEvents(..)";
		public static final String EntityREST_getEntityHeaders               = EntityRESTPrefix + "getEntityHeaders(..)";
		public static final String EntityREST_setClassifications             = EntityRESTPrefix + "setClassifications(..)";
		public static final String EntityREST_addOrUpdateBusinessAttributes  = EntityRESTPrefix + "addOrUpdateBusinessAttributes(..)";
		public static final String EntityREST_removeBusinessAttributes       = EntityRESTPrefix + "removeBusinessAttributes(..)";
		public static final String EntityREST_setLabels                      = EntityRESTPrefix + "setLabels(..)";
		public static final String EntityREST_addLabels                      = EntityRESTPrefix + "addLabels(..)";
		public static final String EntityREST_removeLabels                   = EntityRESTPrefix + "removeLabels(..)";
		public static final String EntityREST_importBMAttributes             = EntityRESTPrefix + "importBMAttributes(..)";

		//TypesREST
		public static final String TypesREST_getTypeDefByName                    = TypesRESTPrefix + "getTypeDefByName(..)";
		public static final String TypesREST_getTypeDefByGuid                    = TypesRESTPrefix + "getTypeDefByGuid(..)";
		public static final String TypesREST_getTypeDefHeaders                   = TypesRESTPrefix + "getTypeDefHeaders(..)";
		public static final String TypesREST_getAllTypeDefs                      = TypesRESTPrefix + "getAllTypeDefs(..)";
		public static final String TypesREST_getEnumDefByName                    = TypesRESTPrefix + "getEnumDefByName(..)";
		public static final String TypesREST_getEnumDefByGuid                    = TypesRESTPrefix + "getEnumDefByGuid(..)";
		public static final String TypesREST_getStructDefByName                  = TypesRESTPrefix + "getStructDefByName(..)";
		public static final String TypesREST_getStructDefByGuid                  = TypesRESTPrefix + "getStructDefByGuid(..)";
		public static final String TypesREST_getClassificationDefByName          = TypesRESTPrefix + "getClassificationDefByName(..)";
		public static final String TypesREST_getClassificationDefByGuid          = TypesRESTPrefix + "getClassificationDefByGuid(..)";
		public static final String TypesREST_getEntityDefByName                  = TypesRESTPrefix + "getEntityDefByName(..)";
		public static final String TypesREST_getEntityDefByGuid                  = TypesRESTPrefix + "getEntityDefByGuid(..)";
		public static final String TypesREST_getRelationshipDefByName            = TypesRESTPrefix + "getRelationshipDefByName(..)";
		public static final String TypesREST_getRelationshipDefByGuid            = TypesRESTPrefix + "getRelationshipDefByGuid(..)";
		public static final String TypesREST_getBusinessMetadataDefByGuid        = TypesRESTPrefix + "getBusinessMetadataDefByGuid(..)";
		public static final String TypesREST_getBusinessMetadataDefByName        = TypesRESTPrefix + "getBusinessMetadataDefByName(..)";
		public static final String TypesREST_createAtlasTypeDefs                 = TypesRESTPrefix + "createAtlasTypeDefs(..)";
		public static final String TypesREST_updateAtlasTypeDefs                 = TypesRESTPrefix + "updateAtlasTypeDefs(..)";
		public static final String TypesREST_deleteAtlasTypeDefs                 = TypesRESTPrefix + "deleteAtlasTypeDefs(..)";
		public static final String TypesREST_deleteAtlasTypeByName               = TypesRESTPrefix + "deleteAtlasTypeByName(..)";

		//GlossaryREST
		public static final String GlossaryREST_getGlossaries                          = GlossaryRESTPrefix + "getGlossaries(..)";
		public static final String GlossaryREST_getGlossary                            = GlossaryRESTPrefix + "getGlossary(..)";
		public static final String GlossaryREST_getDetailedGlossary                    = GlossaryRESTPrefix + "getDetailedGlossary(..)";
		public static final String GlossaryREST_getGlossaryTerm                        = GlossaryRESTPrefix + "getGlossaryTerm(..)";
		public static final String GlossaryREST_getGlossaryCategory                    = GlossaryRESTPrefix + "getGlossaryCategory(..)";
		public static final String GlossaryREST_createGlossary                         = GlossaryRESTPrefix + "createGlossary(..)";
		public static final String GlossaryREST_createGlossaryTerm                     = GlossaryRESTPrefix + "createGlossaryTerm(..)";
		public static final String GlossaryREST_createGlossaryTerms                    = GlossaryRESTPrefix + "createGlossaryTerms(..)";
		public static final String GlossaryREST_createGlossaryCategory                 = GlossaryRESTPrefix + "createGlossaryCategory(..)";
		public static final String GlossaryREST_createGlossaryCategories               = GlossaryRESTPrefix + "createGlossaryCategories(..)";
		public static final String GlossaryREST_updateGlossary                         = GlossaryRESTPrefix + "updateGlossary(..)";
		public static final String GlossaryREST_partialUpdateGlossary                  = GlossaryRESTPrefix + "partialUpdateGlossary(..)";
		public static final String GlossaryREST_updateGlossaryTerm                     = GlossaryRESTPrefix + "updateGlossaryTerm(..)";
		public static final String GlossaryREST_partialUpdateGlossaryTerm              = GlossaryRESTPrefix + "partialUpdateGlossaryTerm(..)";
		public static final String GlossaryREST_updateGlossaryCategory                 = GlossaryRESTPrefix + "updateGlossaryCategory(..)";
		public static final String GlossaryREST_partialUpdateGlossaryCategory          = GlossaryRESTPrefix + "partialUpdateGlossaryCategory(..)";
		public static final String GlossaryREST_deleteGlossary                         = GlossaryRESTPrefix + "deleteGlossary(..)";
		public static final String GlossaryREST_deleteGlossaryTerm                     = GlossaryRESTPrefix + "deleteGlossaryTerm(..)";
		public static final String GlossaryREST_deleteGlossaryCategory                 = GlossaryRESTPrefix + "deleteGlossaryCategory(..)";
		public static final String GlossaryREST_getGlossaryTerms                       = GlossaryRESTPrefix + "getGlossaryTerms(..)";
		public static final String GlossaryREST_getGlossaryTermHeaders                 = GlossaryRESTPrefix + "getGlossaryTermHeaders(..)";
		public static final String GlossaryREST_getGlossaryCategories                  = GlossaryRESTPrefix + "getGlossaryCategories(..)";
		public static final String GlossaryREST_getGlossaryCategoriesHeaders           = GlossaryRESTPrefix + "getGlossaryCategoriesHeaders(..)";
		public static final String GlossaryREST_getCategoryTerms                       = GlossaryRESTPrefix + "getCategoryTerms(..)";
		public static final String GlossaryREST_getRelatedTerms                        = GlossaryRESTPrefix + "getRelatedTerms(..)";
		public static final String GlossaryREST_getEntitiesAssignedWithTerm            = GlossaryRESTPrefix + "getEntitiesAssignedWithTerm(..)";
		public static final String GlossaryREST_assignTermToEntities                   = GlossaryRESTPrefix + "assignTermToEntities(..)";
		public static final String GlossaryREST_removeTermAssignmentFromEntities       = GlossaryRESTPrefix + "removeTermAssignmentFromEntities(..)";
		public static final String GlossaryREST_disassociateTermAssignmentFromEntities = GlossaryRESTPrefix + "disassociateTermAssignmentFromEntities(..)";
		public static final String GlossaryREST_getRelatedCategories                   = GlossaryRESTPrefix + "getRelatedCategories(..)";
		public static final String GlossaryREST_importGlossaryData                     = GlossaryRESTPrefix + "importGlossaryData(..)";

		//DiscoveryREST
		public static final String DiscoveryREST_searchUsingDSL                        = DiscoveryRESTPrefix + "searchUsingDSL(..)";
		public static final String DiscoveryREST_searchUsingBasic                      = DiscoveryRESTPrefix + "searchUsingBasic(..)";
		public static final String DiscoveryREST_quickSearch                           = DiscoveryRESTPrefix + "quickSearch(..)";
		public static final String DiscoveryREST_addSavedSearch                        = DiscoveryRESTPrefix + "addSavedSearch(..)";
		public static final String DiscoveryREST_searchUsingFullText                   = DiscoveryRESTPrefix + "searchUsingFullText(..)";
		public static final String DiscoveryREST_searchUsingAttribute                  = DiscoveryRESTPrefix + "searchUsingAttribute(..)";
		public static final String DiscoveryREST_searchWithParameters                  = DiscoveryRESTPrefix + "searchWithParameters(..)";
		public static final String DiscoveryREST_searchRelatedEntities                 = DiscoveryRESTPrefix + "searchRelatedEntities(..)";
		public static final String DiscoveryREST_updateSavedSearch                     = DiscoveryRESTPrefix + "updateSavedSearch(..)";
		public static final String DiscoveryREST_getSavedSearch                        = DiscoveryRESTPrefix + "getSavedSearch(..)";
		public static final String DiscoveryREST_getSavedSearches                      = DiscoveryRESTPrefix + "getSavedSearches(..)";
		public static final String DiscoveryREST_deleteSavedSearch                     = DiscoveryRESTPrefix + "deleteSavedSearch(..)";
		public static final String DiscoveryREST_executeSavedSearchByName              = DiscoveryRESTPrefix + "executeSavedSearchByName(..)";
		public static final String DiscoveryREST_executeSavedSearchByGuid              = DiscoveryRESTPrefix + "executeSavedSearchByGuid(..)";
		public static final String DiscoveryREST_getSuggestions                        = DiscoveryRESTPrefix + "getSuggestions(..)";

		//RelationshipREST
		public static final String RelationshipREST_create                             = RelationshipRESTPrefix + "create(..)";
		public static final String RelationshipREST_update                             = RelationshipRESTPrefix + "update(..)";
		public static final String RelationshipREST_getById                            = RelationshipRESTPrefix + "getById(..)";
		public static final String RelationshipREST_deleteById                         = RelationshipRESTPrefix + "deleteById(..)";

		public static final String LineageREST_getLineageByUA                          = LineageRESTPrefix + "getLineageByUniqueAttribute(..)";
		public static final String LineageREST_getLineageGraph                         = LineageRESTPrefix + "getLineageGraph(..)";
		public static final String NotificationHookConsumer_doWork                     = "NotificationHookConsumer.doWork(..)";

		//duplicate short signature apis
		public static final String EntityREST_createOrUpdateBulk                     = "EntityMutationResponse org.apache.atlas.web.rest.EntityREST.createOrUpdate(AtlasEntitiesWithExtInfo)";
		public static final String EntityREST_removeLabelsByTypeName                 = "void org.apache.atlas.web.rest.EntityREST.removeLabels(String,Set,HttpServletRequest)";
		public static final String EntityREST_setLabelsByTypeName                    = "void org.apache.atlas.web.rest.EntityREST.setLabels(String,Set,HttpServletRequest)";
		public static final String EntityREST_addLabelsByTypeName                    = "void org.apache.atlas.web.rest.EntityREST.addLabels(String,Set,HttpServletRequest)";
		public static final String EntityREST_addOrUpdateBMByName                    = "void org.apache.atlas.web.rest.EntityREST.addOrUpdateBusinessAttributes(String,String,Map)";
		public static final String EntityREST_removeBMByName                         = "void org.apache.atlas.web.rest.EntityREST.removeBusinessAttributes(String,String,Map)";
		public static final String DiscoveryREST_quickSearchQuickSearchParams        = "AtlasQuickSearchResult org.apache.atlas.web.rest.DiscoveryREST.quickSearch(QuickSearchParameters)";
	}
}