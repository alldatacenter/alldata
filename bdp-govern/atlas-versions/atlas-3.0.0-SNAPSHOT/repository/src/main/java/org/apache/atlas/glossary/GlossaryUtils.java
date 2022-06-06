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
package org.apache.atlas.glossary;

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.glossary.AtlasGlossary;
import org.apache.atlas.model.glossary.AtlasGlossaryCategory;
import org.apache.atlas.model.glossary.AtlasGlossaryTerm;
import org.apache.atlas.model.glossary.relations.AtlasRelatedTermHeader;
import org.apache.atlas.model.instance.AtlasRelationship;
import org.apache.atlas.repository.ogm.DataAccess;
import org.apache.atlas.repository.store.graph.AtlasRelationshipStore;
import org.apache.atlas.type.AtlasTypeRegistry;

import java.util.Objects;

public abstract class GlossaryUtils {

    public static final String TERM_ASSIGNMENT_ATTR_DESCRIPTION = "description";
    public static final String TERM_ASSIGNMENT_ATTR_EXPRESSION  = "expression";
    public static final String TERM_ASSIGNMENT_ATTR_STATUS      = "status";
    public static final String TERM_ASSIGNMENT_ATTR_CONFIDENCE  = "confidence";
    public static final String TERM_ASSIGNMENT_ATTR_CREATED_BY  = "createdBy";
    public static final String TERM_ASSIGNMENT_ATTR_STEWARD     = "steward";
    public static final String TERM_ASSIGNMENT_ATTR_SOURCE      = "source";

    static final String ATLAS_GLOSSARY_TYPENAME          = "AtlasGlossary";
    static final String ATLAS_GLOSSARY_TERM_TYPENAME     = "AtlasGlossaryTerm";
    static final String ATLAS_GLOSSARY_CATEGORY_TYPENAME = "AtlasGlossaryCategory";

    public static final String NAME                         = "name";
    public static final String QUALIFIED_NAME_ATTR          = "qualifiedName";
    public static final char[] invalidNameChars             = {'@', '.'};

    // Relation name constants
    protected static final String ATLAS_GLOSSARY_PREFIX          = ATLAS_GLOSSARY_TYPENAME;
    protected static final String TERM_ANCHOR                    = ATLAS_GLOSSARY_PREFIX + "TermAnchor";
    protected static final String CATEGORY_ANCHOR                = ATLAS_GLOSSARY_PREFIX + "CategoryAnchor";
    protected static final String CATEGORY_HIERARCHY             = ATLAS_GLOSSARY_PREFIX + "CategoryHierarchyLink";
    protected static final String TERM_CATEGORIZATION            = ATLAS_GLOSSARY_PREFIX + "TermCategorization";
    protected static final String TERM_ASSIGNMENT                = ATLAS_GLOSSARY_PREFIX + "SemanticAssignment";
    protected static final String TERM_RELATION_ATTR_EXPRESSION  = "expression";
    protected static final String TERM_RELATION_ATTR_DESCRIPTION = "description";
    protected static final String TERM_RELATION_ATTR_STEWARD     = "steward";
    protected static final String TERM_RELATION_ATTR_SOURCE      = "source";
    protected static final String TERM_RELATION_ATTR_STATUS      = "status";

    protected final AtlasRelationshipStore relationshipStore;
    protected final AtlasTypeRegistry      typeRegistry;
    protected final DataAccess             dataAccess;

    protected GlossaryUtils(final AtlasRelationshipStore relationshipStore, final AtlasTypeRegistry typeRegistry, final DataAccess dataAccess) {
        this.relationshipStore = relationshipStore;
        this.typeRegistry = typeRegistry;
        this.dataAccess = dataAccess;
    }

    public static AtlasGlossary getGlossarySkeleton(String glossaryGuid) {
        AtlasGlossary glossary = new AtlasGlossary();
        glossary.setGuid(glossaryGuid);
        return glossary;
    }

    public static AtlasGlossaryTerm getAtlasGlossaryTermSkeleton(final String termGuid) {
        AtlasGlossaryTerm glossaryTerm = new AtlasGlossaryTerm();
        glossaryTerm.setGuid(termGuid);
        return glossaryTerm;
    }

    public static AtlasGlossaryCategory getAtlasGlossaryCategorySkeleton(final String categoryGuid) {
        AtlasGlossaryCategory glossaryCategory = new AtlasGlossaryCategory();
        glossaryCategory.setGuid(categoryGuid);
        return glossaryCategory;
    }



    protected void createRelationship(AtlasRelationship relationship) throws AtlasBaseException {
        relationshipStore.getOrCreate(relationship);
    }

    protected void updateRelationshipAttributes(AtlasRelationship relationship, AtlasRelatedTermHeader relatedTermHeader) {
        if (Objects.nonNull(relationship)) {
            relationship.setAttribute(TERM_RELATION_ATTR_EXPRESSION, relatedTermHeader.getExpression());
            relationship.setAttribute(TERM_RELATION_ATTR_DESCRIPTION, relatedTermHeader.getDescription());
            relationship.setAttribute(TERM_RELATION_ATTR_STEWARD, relatedTermHeader.getSteward());
            relationship.setAttribute(TERM_RELATION_ATTR_SOURCE, relatedTermHeader.getSource());
            if (Objects.nonNull(relatedTermHeader.getStatus())) {
                relationship.setAttribute(TERM_RELATION_ATTR_STATUS, relatedTermHeader.getStatus().name());
            }
        }
    }

    enum RelationshipOperation {
        CREATE, UPDATE, DELETE
    }
}
