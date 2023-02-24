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
package org.apache.atlas.repository.graphdb.janus;

import org.apache.atlas.repository.graphdb.AtlasCardinality;
import org.apache.atlas.repository.graphdb.AtlasEdgeDirection;
import org.apache.atlas.repository.graphdb.AtlasPropertyKey;
import org.apache.tinkerpop.gremlin.structure.Direction;

import org.janusgraph.core.Cardinality;
import org.janusgraph.core.PropertyKey;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory that serves up instances of Janus/Tinkerpop classes that correspond to
 * graph database abstraction layer/Atlas classes.
 */
public final class AtlasJanusObjectFactory {

    private AtlasJanusObjectFactory() {

    }

    /**
     * Retrieves the Janus direction corresponding to the given
     * AtlasEdgeDirection.
     *
     * @param dir
     * @return
     */
    public static Direction createDirection(AtlasEdgeDirection dir) {

        switch(dir) {
        case IN:
            return Direction.IN;
        case OUT:
            return Direction.OUT;
        case BOTH:
            return Direction.BOTH;
        default:
            throw new RuntimeException("Unrecognized direction: " + dir);
        }
    }


    /**
     * Converts a Multiplicity to a Cardinality.
     *
     * @param cardinality
     * @return
     */
    public static Cardinality createCardinality(AtlasCardinality cardinality) {
        switch(cardinality) {

        case SINGLE:
            return Cardinality.SINGLE;
        case LIST:
            return Cardinality.LIST;
        case SET:
            return Cardinality.SET;
        default:
            throw new IllegalStateException("Unrecognized cardinality: " + cardinality);
        }
    }

    public static PropertyKey createPropertyKey(AtlasPropertyKey key) {
        return ((AtlasJanusPropertyKey)key).getWrappedPropertyKey();
    }

    public static PropertyKey[] createPropertyKeys(List<AtlasPropertyKey> keys) {
        PropertyKey[] ret = new PropertyKey[keys.size()];

        int i = 0;
        for (AtlasPropertyKey key : keys) {
            ret[i] = createPropertyKey(key);

            i++;
        }

        return ret;
    }
}
