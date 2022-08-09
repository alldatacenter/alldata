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
package org.apache.atlas.store;

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.SearchFilter;
import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.atlas.model.typedef.AtlasBusinessMetadataDef;
import org.apache.atlas.model.typedef.AtlasClassificationDef;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.model.typedef.AtlasEnumDef;
import org.apache.atlas.model.typedef.AtlasRelationshipDef;
import org.apache.atlas.model.typedef.AtlasStructDef;
import org.apache.atlas.model.typedef.AtlasTypesDef;


/**
 * Interface to persistence store of TypeDef
 */
public interface AtlasTypeDefStore {
    void init() throws AtlasBaseException;

    /* EnumDef operations */

    AtlasEnumDef getEnumDefByName(String name) throws AtlasBaseException;

    AtlasEnumDef getEnumDefByGuid(String guid) throws AtlasBaseException;

    AtlasEnumDef updateEnumDefByName(String name, AtlasEnumDef enumDef) throws AtlasBaseException;

    AtlasEnumDef updateEnumDefByGuid(String guid, AtlasEnumDef enumDef) throws AtlasBaseException;

    /* StructDef operations */

    AtlasStructDef getStructDefByName(String name) throws AtlasBaseException;

    AtlasStructDef getStructDefByGuid(String guid) throws AtlasBaseException;

    AtlasStructDef updateStructDefByName(String name, AtlasStructDef structDef) throws AtlasBaseException;

    AtlasStructDef updateStructDefByGuid(String guid, AtlasStructDef structDef) throws AtlasBaseException;

    /* ClassificationDef operations */

    AtlasClassificationDef getClassificationDefByName(String name) throws AtlasBaseException;

    AtlasClassificationDef getClassificationDefByGuid(String guid) throws AtlasBaseException;

    AtlasClassificationDef updateClassificationDefByName(String name, AtlasClassificationDef classificationDef)
            throws AtlasBaseException;

    AtlasClassificationDef updateClassificationDefByGuid(String guid, AtlasClassificationDef classificationDef)
            throws AtlasBaseException;

    /* EntityDef operations */

    AtlasEntityDef getEntityDefByName(String name) throws AtlasBaseException;

    AtlasEntityDef getEntityDefByGuid(String guid) throws AtlasBaseException;

    AtlasEntityDef updateEntityDefByName(String name, AtlasEntityDef entityDef) throws AtlasBaseException;

    AtlasEntityDef updateEntityDefByGuid(String guid, AtlasEntityDef entityDef) throws AtlasBaseException;
    /* RelationshipDef operations */

    AtlasRelationshipDef getRelationshipDefByName(String name) throws AtlasBaseException;

    AtlasRelationshipDef getRelationshipDefByGuid(String guid) throws AtlasBaseException;

    AtlasRelationshipDef updateRelationshipDefByName(String name, AtlasRelationshipDef relationshipDef) throws AtlasBaseException;

    AtlasRelationshipDef updateRelationshipDefByGuid(String guid, AtlasRelationshipDef relationshipDef) throws AtlasBaseException;

    /* business-metadata Def operations */
    AtlasBusinessMetadataDef getBusinessMetadataDefByName(String name) throws AtlasBaseException;

    AtlasBusinessMetadataDef getBusinessMetadataDefByGuid(String guid) throws AtlasBaseException;

    /* Bulk Operations */

    AtlasTypesDef createTypesDef(AtlasTypesDef typesDef) throws AtlasBaseException;

    AtlasTypesDef updateTypesDef(AtlasTypesDef typesDef) throws AtlasBaseException;

    AtlasTypesDef createUpdateTypesDef(AtlasTypesDef typesToCreateUpdate) throws AtlasBaseException;

    AtlasTypesDef createUpdateTypesDef(AtlasTypesDef typesToCreate, AtlasTypesDef typesToUpdate) throws AtlasBaseException;

    void deleteTypesDef(AtlasTypesDef typesDef) throws AtlasBaseException;

    AtlasTypesDef searchTypesDef(SearchFilter searchFilter) throws AtlasBaseException;


    /* Generic operation */
    AtlasBaseTypeDef getByName(String name) throws AtlasBaseException;

    AtlasBaseTypeDef getByGuid(String guid) throws AtlasBaseException;

    void deleteTypeByName(String typeName) throws AtlasBaseException;

    void notifyLoadCompletion();
}
