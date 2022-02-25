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
package org.apache.atlas.repository.graph;

/**
 * Represents an edge label used in Atlas.
 * The format of an Atlas edge label is EDGE_LABEL_PREFIX<<typeName>>.<<attributeName>>[.mapKey]
 *
 */
public class AtlasEdgeLabel {
    private final String typeName_;
    private final String attributeName_;
    private final String mapKey_;
    private final String edgeLabel_;
    private final String qualifiedMapKey_;
    private final String qualifiedAttributeName_;
    
    public AtlasEdgeLabel(String edgeLabel) {
        String labelWithoutPrefix = edgeLabel.substring(GraphHelper.EDGE_LABEL_PREFIX.length());
        String[] fields = labelWithoutPrefix.split("\\.", 3);
        if (fields.length < 2 || fields.length > 3) {
            throw new IllegalArgumentException("Invalid edge label " + edgeLabel + 
                ": expected 2 or 3 label components but found " + fields.length); 
        }
        typeName_ = fields[0];
        attributeName_ = fields[1];
        if (fields.length == 3) {
            mapKey_ = fields[2];
            qualifiedMapKey_ = labelWithoutPrefix;
            qualifiedAttributeName_ = typeName_ + '.' + attributeName_;
        }
        else {
            mapKey_ = null;
            qualifiedMapKey_ = null;
            qualifiedAttributeName_ = labelWithoutPrefix;
        }
        edgeLabel_ = edgeLabel;
    }
    
    public String getTypeName() {
        return typeName_;
    }
    
    public String getAttributeName() {
        return attributeName_;
    }
    
    public String getMapKey() {
        return mapKey_;
    }
    
    public String getEdgeLabel() {
        return edgeLabel_;
    }

    public String getQualifiedMapKey() {
        return qualifiedMapKey_;
    }
    
    
    public String getQualifiedAttributeName() {
    
        return qualifiedAttributeName_;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('(').append("typeName: ").append(typeName_);
        sb.append(", attributeName: ").append(attributeName_);
        if (mapKey_ != null) {
            sb.append(", mapKey: ").append(mapKey_);
            sb.append(", qualifiedMapKey: ").append(qualifiedMapKey_);
        }
        sb.append(", edgeLabel: ").append(edgeLabel_).append(')');
        return sb.toString();
    }
    
}