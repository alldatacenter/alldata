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
package org.apache.atlas.query;

import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.TypeCategory;
import org.apache.atlas.model.typedef.AtlasStructDef;
import org.apache.atlas.query.antlr4.AtlasDSLParser;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasStructType;
import org.apache.atlas.type.AtlasStructType.AtlasAttribute.AtlasRelationshipEdgeDirection;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.commons.lang.StringUtils;

import static org.apache.atlas.type.AtlasStructType.AtlasAttribute.AtlasRelationshipEdgeDirection.IN;
import static org.apache.atlas.type.AtlasStructType.AtlasAttribute.AtlasRelationshipEdgeDirection.OUT;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.FileAssert.fail;

public class BaseDSLComposer {
    protected final AtlasTypeRegistry registry = mock(AtlasTypeRegistry.class);

    protected AtlasDSLParser.QueryContext getParsedQuery(String query) {
        AtlasDSLParser.QueryContext queryContext = null;

        try {
            queryContext = AtlasDSL.Parser.parse(query);
        } catch (AtlasBaseException e) {
            fail(e.getMessage());
        }

        return queryContext;
    }

    public static class TestLookup implements org.apache.atlas.query.Lookup {
        final AtlasTypeRegistry registry;

        TestLookup(AtlasTypeRegistry typeRegistry) {
            this.registry = typeRegistry;
        }

        @Override
        public AtlasType getType(String typeName) throws AtlasBaseException {
            final AtlasType type;

            if(typeName.equals("PII") || typeName.equals("Dimension")) {
                type = mock(AtlasType.class);

                when(type.getTypeCategory()).thenReturn(TypeCategory.CLASSIFICATION);
            } else {
                type = mock(AtlasEntityType.class);

                when(type.getTypeCategory()).thenReturn(TypeCategory.ENTITY);

                AtlasStructType.AtlasAttribute   attr = mock(AtlasStructType.AtlasAttribute.class);
                AtlasStructDef.AtlasAttributeDef def  = mock(AtlasStructDef.AtlasAttributeDef.class);

                when(def.getIndexType()).thenReturn(AtlasStructDef.AtlasAttributeDef.IndexType.DEFAULT);
                when(attr.getAttributeDef()).thenReturn(def);

                AtlasStructType.AtlasAttribute attr_s = mock(AtlasStructType.AtlasAttribute.class);
                AtlasStructDef.AtlasAttributeDef def_s = mock(AtlasStructDef.AtlasAttributeDef.class);

                when(def_s.getIndexType()).thenReturn(AtlasStructDef.AtlasAttributeDef.IndexType.STRING);
                when(attr_s.getAttributeDef()).thenReturn(def_s);
                when(((AtlasEntityType) type).getAttribute(anyString())).thenReturn(attr);
                when(((AtlasEntityType) type).getAttribute(eq("name"))).thenReturn(attr_s);
            }

            if(typeName.equals("PIII")) {
                throw new AtlasBaseException(AtlasErrorCode.TYPE_NAME_NOT_FOUND);
            }

            when(type.getTypeName()).thenReturn(typeName);

            return type;
        }

        @Override
        public String getQualifiedName(GremlinQueryComposer.Context context, String name) throws AtlasBaseException {
            if(name.startsWith("__")) {
                return name.equals("__state") || name.equals("__guid") ? name : "";
            }

            if(!hasAttribute(context, name)) {
                throw new AtlasBaseException("Invalid attribute");
            }

            if(name.contains(".")) {
                return name;
            }

            if(!context.getActiveTypeName().equals(name)) {
                return String.format("%s.%s", context.getActiveTypeName(), name);
            } else {
                return name;
            }
        }

        @Override
        public boolean isPrimitive(GremlinQueryComposer.Context context, String attributeName) {
            return attributeName.equals("name") ||
                    attributeName.equals("owner") ||
                    attributeName.equals("createTime") ||
                    attributeName.equals("clusterName") ||
                    attributeName.equals("__guid") ||
                    attributeName.equals("__state") ||
                    attributeName.equals("partitionSize");
        }

        @Override
        public String getRelationshipEdgeLabel(GremlinQueryComposer.Context context, String attributeName) {
            if (attributeName.equalsIgnoreCase("columns")) {
                return "__Table.columns";
            } else if (attributeName.equalsIgnoreCase("db")) {
                return "__Table.db";
            } else if (attributeName.equalsIgnoreCase("meanings")) {
                return "r:AtlasGlossarySemanticAssignment";
            } else {
                return "__DB.Table";
            }
        }

        @Override
        public AtlasRelationshipEdgeDirection getRelationshipEdgeDirection(GremlinQueryComposer.Context context, String attributeName) {
            if (attributeName.equalsIgnoreCase("meanings")) {
                return IN;
            }

            return OUT;
        }

        @Override
        public boolean hasAttribute(GremlinQueryComposer.Context context, String attributeName) {
            return (context.getActiveTypeName().equals("Table") && attributeName.equals("db")) ||
                    (context.getActiveTypeName().equals("Table") && attributeName.equals("columns")) ||
                    (context.getActiveTypeName().equals("Table") && attributeName.equals("createTime")) ||
                    (context.getActiveTypeName().equals("Table") && attributeName.equals("name")) ||
                    (context.getActiveTypeName().equals("Table") && attributeName.equals("owner")) ||
                    (context.getActiveTypeName().equals("Table") && attributeName.equals("clusterName")) ||
                    (context.getActiveTypeName().equals("Table") && attributeName.equals("isFile")) ||
                    (context.getActiveTypeName().equals("Table") && attributeName.equals("__guid")) ||
                    (context.getActiveTypeName().equals("Table") && attributeName.equals("__state")) ||
                    (context.getActiveTypeName().equals("Table") && attributeName.equals("partitionSize")) ||
                    (context.getActiveTypeName().equals("Table") && attributeName.equals("meanings")) ||
                    (context.getActiveTypeName().equals("hive_db") && attributeName.equals("name")) ||
                    (context.getActiveTypeName().equals("hive_db") && attributeName.equals("owner")) ||
                    (context.getActiveTypeName().equals("hive_db") && attributeName.equals("createTime")) ||
                    (context.getActiveTypeName().equals("hive_db") && attributeName.equals("description")) ||
                    (context.getActiveTypeName().equals("hive_db") && attributeName.equals("userDescription")) ||
                    (context.getActiveTypeName().equals("DB") && attributeName.equals("name")) ||
                    (context.getActiveTypeName().equals("DB") && attributeName.equals("owner")) ||
                    (context.getActiveTypeName().equals("DB") && attributeName.equals("clusterName")) ||
                    (context.getActiveTypeName().equals("Asset") && attributeName.equals("name")) ||
                    (context.getActiveTypeName().equals("Asset") && attributeName.equals("owner")) ||
                    (context.getActiveTypeName().equals("AtlasGlossaryTerm") && attributeName.equals("name")) ||
                    (context.getActiveTypeName().equals("AtlasGlossaryTerm") && attributeName.equals("qualifiedName"));
        }

        @Override
        public boolean doesTypeHaveSubTypes(GremlinQueryComposer.Context context) {
            return context.getActiveTypeName().equalsIgnoreCase("Asset");
        }

        @Override
        public String getTypeAndSubTypes(GremlinQueryComposer.Context context) {
            String[] str = new String[]{"'Asset'", "'Table'"};

            return StringUtils.join(str, ",");
        }

        @Override
        public boolean isTraitType(String typeName) {
            return typeName.equals("PII") || typeName.equals("Dimension");
        }

        @Override
        public String getTypeFromEdge(GremlinQueryComposer.Context context, String item) {
            if (context.getActiveTypeName().equals("DB") && item.equals("Table")) {
                return "Table";
            } else if (context.getActiveTypeName().equals("Table") && item.equals("Column")) {
                return "Column";
            } else if (context.getActiveTypeName().equals("Table") && item.equals("db")) {
                return "DB";
            } else if (context.getActiveTypeName().equals("Table") && item.equals("columns")) {
                return "Column";
            } else if (context.getActiveTypeName().equals("Table") && item.equals("meanings")) {
                return "AtlasGlossaryTerm";
            } else if (context.getActiveTypeName().equals(item)) {
                return null;
            } else {
                return context.getActiveTypeName();
            }
        }

        @Override
        public boolean isDate(GremlinQueryComposer.Context context, String attributeName) {
            return attributeName.equals("createTime");
        }

        @Override
        public boolean isNumeric(GremlinQueryComposer.Context context, String attrName) {
            context.setNumericTypeFormatter("f");
            return attrName.equals("partitionSize");
        }

        @Override
        public String getVertexPropertyName(String typeName, String attrName) {
            if (typeName.equals("Asset")) {
                if (attrName.equals("name") || attrName.equals("owner")) {
                    return String.format("%s.__s_%s", typeName, attrName);
                }
            }

            return null;
        }
    }
}
