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
package org.apache.atlas.repository.store.graph.v2;


import org.apache.atlas.model.instance.EntityMutations.EntityOperation;
import org.apache.atlas.model.typedef.AtlasStructDef;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef;
import org.apache.atlas.repository.graphdb.AtlasEdge;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.type.AtlasStructType;
import org.apache.atlas.type.AtlasStructType.AtlasAttribute;
import org.apache.atlas.type.AtlasType;


import java.util.Objects;

public class AttributeMutationContext {
    private EntityOperation op;
    /**
     * Atlas Attribute
     */

    private AtlasAttribute attribute;

    /**
     * Overriding type for which elements are being mapped
     */
    private AtlasType currentElementType;

    /**
     * Current attribute value/entity/Struct instance
     */
    private Object value;

    private String vertexProperty;

    /**
     *
     * The vertex which corresponds to the entity/struct for which we are mapping a complex attributes like struct, traits
     */
    AtlasVertex referringVertex;

    /**
     * The current edge(in case of updates) from the parent entity/struct to the complex attribute like struct, trait
     */
    AtlasEdge existingEdge;

    public AttributeMutationContext(EntityOperation op, AtlasVertex referringVertex, AtlasAttribute attribute, Object value) {
        this(op, referringVertex, attribute, value, attribute.getVertexPropertyName(), null, null);
    }

    public AttributeMutationContext(EntityOperation op, AtlasVertex referringVertex, AtlasAttribute attribute, Object value,
                                    String vertexProperty, AtlasType currentElementType, AtlasEdge currentEdge) {
        this.op                 = op;
        this.referringVertex    = referringVertex;
        this.attribute          = attribute;
        this.value              = value;
        this.vertexProperty     = vertexProperty;
        this.currentElementType = currentElementType;
        this.existingEdge       = currentEdge;
    }

    @Override
    public int hashCode() {
        return Objects.hash(op, referringVertex, attribute, value, vertexProperty, currentElementType, existingEdge);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (obj == this) {
            return true;
        } else if (obj.getClass() != getClass()) {
            return false;
        } else {
            AttributeMutationContext rhs = (AttributeMutationContext) obj;
            return Objects.equals(op, rhs.op)
                    && Objects.equals(referringVertex, rhs.referringVertex)
                    && Objects.equals(attribute, rhs.attribute)
                    && Objects.equals(value, rhs.value)
                    && Objects.equals(vertexProperty, rhs.vertexProperty)
                    && Objects.equals(currentElementType, rhs.currentElementType)
                    && Objects.equals(existingEdge, rhs.existingEdge);
        }
    }


    public AtlasStructType getParentType() {
        return attribute.getDefinedInType();
    }

    public AtlasStructDef getStructDef() {
        return attribute.getDefinedInDef();
    }

    public AtlasAttributeDef getAttributeDef() {
        return attribute.getAttributeDef();
    }

    public AtlasType getAttrType() {
        return currentElementType == null ? attribute.getAttributeType() : currentElementType;
    }

    public AtlasType getCurrentElementType() {
        return currentElementType;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getVertexProperty() { return vertexProperty; }

    public AtlasVertex getReferringVertex() { return referringVertex; }

    public AtlasEdge getCurrentEdge() {
        return existingEdge;
    }

    public void setElementType(final AtlasType attrType) {
        this.currentElementType = attrType;
    }

    public AtlasAttribute getAttribute() {
        return attribute;
    }

    public EntityOperation getOp() {
        return op;
    }

    public void setExistingEdge(AtlasEdge existingEdge) { this.existingEdge = existingEdge; }
}
