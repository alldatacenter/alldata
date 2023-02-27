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
package org.apache.atlas.model.lineage;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.atlas.AtlasConfiguration;
import org.apache.atlas.model.lineage.AtlasLineageInfo.LineageDirection;

import java.io.Serializable;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * This is the root class representing the input for lineage search on-demand.
 */
public class LineageOnDemandConstraints implements Serializable {
    private static final long serialVersionUID = 1L;

    private LineageDirection direction;
    private int              inputRelationsLimit;
    private int              outputRelationsLimit;
    private int              depth;

    private static final int LINEAGE_ON_DEMAND_DEFAULT_NODE_COUNT = AtlasConfiguration.LINEAGE_ON_DEMAND_DEFAULT_NODE_COUNT.getInt();
    private static final int LINEAGE_ON_DEMAND_DEFAULT_DEPTH      = 3;

    public LineageOnDemandConstraints() {
        this(LineageDirection.BOTH, LINEAGE_ON_DEMAND_DEFAULT_NODE_COUNT, LINEAGE_ON_DEMAND_DEFAULT_NODE_COUNT, LINEAGE_ON_DEMAND_DEFAULT_DEPTH);
    }

    public LineageOnDemandConstraints(LineageDirection direction, int inputRelationsLimit, int outputRelationsLimit, int depth) {
        this.direction            = direction;
        this.inputRelationsLimit  = inputRelationsLimit;
        this.outputRelationsLimit = outputRelationsLimit;
        this.depth                = depth;
    }

    public LineageDirection getDirection() {
        return direction;
    }

    public void setDirection(LineageDirection direction) {
        this.direction = direction;
    }

    public int getInputRelationsLimit() {
        return inputRelationsLimit;
    }

    public void setInputRelationsLimit(int inputRelationsLimit) {
        this.inputRelationsLimit = inputRelationsLimit;
    }

    public int getOutputRelationsLimit() {
        return outputRelationsLimit;
    }

    public void setOutputRelationsLimit(int outputRelationsLimit) {
        this.outputRelationsLimit = outputRelationsLimit;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

}
