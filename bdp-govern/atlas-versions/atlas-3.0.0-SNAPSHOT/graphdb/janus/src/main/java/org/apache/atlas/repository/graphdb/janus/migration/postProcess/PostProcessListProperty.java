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

package org.apache.atlas.repository.graphdb.janus.migration.postProcess;

import org.apache.atlas.repository.Constants;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostProcessListProperty {
    private static final Logger LOG = LoggerFactory.getLogger(PostProcessListProperty.class);

    public void process(Vertex v, String typeName, String propertyName) {
        try {
            if (doesNotHaveProperty(v, typeName) || !hasProperty(v, propertyName)) {
                return;
            }

            removeProperty(v, propertyName);
        } catch (IllegalArgumentException ex) {
            LOG.error("process: IllegalArgumentException: v[{}] error!", v.id(), ex);
        }
    }

    protected void removeProperty(Vertex v, String propertyName) {
        v.property(propertyName).remove();
    }

    protected boolean doesNotHaveProperty(Vertex v, String typeName) {
        return v.property(Constants.TYPENAME_PROPERTY_KEY).isPresent() || !isInstanceVertexOfType(v, typeName);
    }

    private boolean hasProperty(Vertex v, String propertyName) {
        try {
            return v.property(propertyName).isPresent();
        } catch(Exception ex) {
            // ...
        }

        return false;
    }

    private boolean isInstanceVertexOfType(Vertex v, String typeName) {
        if(v.property(Constants.ENTITY_TYPE_PROPERTY_KEY).isPresent()) {
            String s = (String) v.property(Constants.ENTITY_TYPE_PROPERTY_KEY).value();

            return s.equals(typeName);
        }

        return false;
    }
}
