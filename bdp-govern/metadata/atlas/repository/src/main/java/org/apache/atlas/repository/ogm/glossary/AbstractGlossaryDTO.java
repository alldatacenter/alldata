/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.repository.ogm.glossary;

import org.apache.atlas.model.AtlasBaseModelObject;
import org.apache.atlas.model.glossary.enums.AtlasTermRelationshipStatus;
import org.apache.atlas.model.glossary.relations.AtlasGlossaryHeader;
import org.apache.atlas.model.glossary.relations.AtlasRelatedCategoryHeader;
import org.apache.atlas.model.glossary.relations.AtlasRelatedTermHeader;
import org.apache.atlas.model.glossary.relations.AtlasTermCategorizationHeader;
import org.apache.atlas.model.instance.AtlasRelatedObjectId;
import org.apache.atlas.model.instance.AtlasStruct;
import org.apache.atlas.repository.ogm.AbstractDataTransferObject;
import org.apache.atlas.type.AtlasTypeRegistry;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractGlossaryDTO<T extends AtlasBaseModelObject> extends AbstractDataTransferObject<T> {
    protected AbstractGlossaryDTO(final AtlasTypeRegistry typeRegistry, final Class<T> tClass) {
        super(typeRegistry, tClass);
    }

    protected AbstractGlossaryDTO(final AtlasTypeRegistry typeRegistry, final Class<T> tClass, final String entityTypeName) {
        super(typeRegistry, tClass, entityTypeName);
    }

    protected AtlasRelatedTermHeader constructRelatedTermId(AtlasRelatedObjectId relatedObjectId) {
        AtlasRelatedTermHeader ret = new AtlasRelatedTermHeader();

        ret.setTermGuid(relatedObjectId.getGuid());
        ret.setRelationGuid(relatedObjectId.getRelationshipGuid());

        AtlasStruct relationshipAttributes = relatedObjectId.getRelationshipAttributes();
        if (relationshipAttributes != null) {
            ret.setDescription((String) relationshipAttributes.getAttribute("description"));
            ret.setExpression((String) relationshipAttributes.getAttribute("expression"));
            ret.setSource((String) relationshipAttributes.getAttribute("source"));
            ret.setSteward((String) relationshipAttributes.getAttribute("steward"));
            Object status = relationshipAttributes.getAttribute("status");
            if (status instanceof String) {
                ret.setStatus(AtlasTermRelationshipStatus.valueOf((String) status));
            } else if (status instanceof AtlasTermRelationshipStatus) {
                ret.setStatus((AtlasTermRelationshipStatus) status);
            }
        }

        return ret;
    }

    protected AtlasRelatedObjectId termIdToRelatedObjectId(AtlasRelatedTermHeader relatedTermId) {
        AtlasRelatedObjectId ret = new AtlasRelatedObjectId();

        ret.setGuid(relatedTermId.getTermGuid());
        ret.setRelationshipGuid(relatedTermId.getRelationGuid());

        AtlasStruct relationshipAttributes = new AtlasStruct();
        relationshipAttributes.setAttribute("description", relatedTermId.getDescription());
        relationshipAttributes.setAttribute("expression", relatedTermId.getExpression());
        relationshipAttributes.setAttribute("source", relatedTermId.getSource());
        relationshipAttributes.setAttribute("steward", relatedTermId.getSteward());
        relationshipAttributes.setAttribute("status", relatedTermId.getStatus().name());

        ret.setRelationshipAttributes(relationshipAttributes);

        return ret;
    }

    protected AtlasRelatedCategoryHeader constructRelatedCategoryId(AtlasRelatedObjectId relatedObjectId) {
        AtlasRelatedCategoryHeader ret = new AtlasRelatedCategoryHeader();

        ret.setCategoryGuid(relatedObjectId.getGuid());
        ret.setRelationGuid(relatedObjectId.getRelationshipGuid());

        AtlasStruct relationshipAttributes = relatedObjectId.getRelationshipAttributes();
        if (relationshipAttributes != null) {
            ret.setDescription((String) relationshipAttributes.getAttribute("description"));
        }

        return ret;
    }

    protected AtlasRelatedObjectId relatedCategoryIdToRelatedObjectId(AtlasRelatedCategoryHeader relatedCategoryId) {
        AtlasRelatedObjectId ret = new AtlasRelatedObjectId();

        ret.setGuid(relatedCategoryId.getCategoryGuid());
        ret.setRelationshipGuid(relatedCategoryId.getRelationGuid());

        AtlasStruct relationshipAttributes = new AtlasStruct();
        relationshipAttributes.setAttribute("description", relatedCategoryId.getDescription());
        ret.setRelationshipAttributes(relationshipAttributes);

        return ret;
    }

    protected AtlasGlossaryHeader constructGlossaryId(AtlasRelatedObjectId relatedObjectId) {
        AtlasGlossaryHeader ret = new AtlasGlossaryHeader();

        ret.setGlossaryGuid(relatedObjectId.getGuid());
        ret.setRelationGuid(relatedObjectId.getRelationshipGuid());

        return ret;
    }

    protected AtlasRelatedObjectId glossaryIdToRelatedObjectId(AtlasGlossaryHeader glossaryId) {
        AtlasRelatedObjectId ret = new AtlasRelatedObjectId();

        ret.setGuid(glossaryId.getGlossaryGuid());
        ret.setRelationshipGuid(glossaryId.getRelationGuid());

        return ret;
    }

    protected AtlasTermCategorizationHeader constructTermCategorizationId(final AtlasRelatedObjectId category) {
        AtlasTermCategorizationHeader ret = new AtlasTermCategorizationHeader();

        ret.setCategoryGuid(category.getGuid());
        ret.setRelationGuid(category.getRelationshipGuid());

        AtlasStruct relationshipAttributes = category.getRelationshipAttributes();
        if (relationshipAttributes != null) {
            ret.setDescription((String) relationshipAttributes.getAttribute("description"));
            Object status = relationshipAttributes.getAttribute("status");
            if (status instanceof AtlasTermRelationshipStatus) {
                ret.setStatus((AtlasTermRelationshipStatus) status);
            } else if (status instanceof String) {
                ret.setStatus(AtlasTermRelationshipStatus.valueOf((String) status));
            }
        }

        return ret;
    }

    protected Set<AtlasRelatedTermHeader> toRelatedTermIdsSet(Object relatedObjectIds) {
        Set<AtlasRelatedTermHeader> ret = null;

        if (relatedObjectIds instanceof Collection) {
            ret = new HashSet<>();
            for (Object t : (Collection) relatedObjectIds) {
                if (t instanceof AtlasRelatedObjectId) {
                    ret.add(constructRelatedTermId((AtlasRelatedObjectId) t));
                }
            }
        }

        return ret;
    }
}
