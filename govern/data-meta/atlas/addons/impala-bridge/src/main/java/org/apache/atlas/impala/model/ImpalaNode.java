/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.impala.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Contain vertex info of this node and its children. It is used only internally
 */
public class ImpalaNode {
    LineageVertex ownVertex;
    Map<Long, ImpalaNode> children;

    public ImpalaNode(LineageVertex ownVertex) {
        this.ownVertex = ownVertex;
        children = new HashMap<>();
    }

    public String getNodeName() { return ownVertex.getVertexId(); }
    public ImpalaVertexType getNodeType() { return ownVertex.getVertexType(); }
    public LineageVertex getOwnVertex() { return ownVertex; }
    public Map<Long, ImpalaNode> getChildren() { return children; }

    /**
     * Add child to this node
     * @param child
     * @return the node corresponding to the input child vertex
     */
    public ImpalaNode addChild(LineageVertex child) {
        ImpalaNode exitingChild = children.get(child.getId());
        if (exitingChild != null) {
            return exitingChild;
        }

        ImpalaNode newChild = new ImpalaNode(child);
        return children.put(child.getId(), newChild);
    }
}
