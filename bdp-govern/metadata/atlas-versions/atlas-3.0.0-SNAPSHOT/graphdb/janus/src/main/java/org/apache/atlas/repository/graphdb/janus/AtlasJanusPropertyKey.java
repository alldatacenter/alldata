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
import org.apache.atlas.repository.graphdb.AtlasPropertyKey;

import org.janusgraph.core.PropertyKey;

/**
 *
 */
public class AtlasJanusPropertyKey implements AtlasPropertyKey {

    private PropertyKey wrapped;

    public AtlasJanusPropertyKey(PropertyKey toWrap) {
        wrapped = toWrap;
    }

    /* (non-Javadoc)
     * @see org.apache.atlas.repository.graphdb.AtlasPropertyKey#getName()
     */
    @Override
    public String getName() {
        return wrapped.name();
    }

    /**
     * @return
     */
    public PropertyKey getWrappedPropertyKey() {
        return wrapped;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 37*result + wrapped.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AtlasJanusPropertyKey)) {
            return false;
        }
        AtlasJanusPropertyKey otherKey = (AtlasJanusPropertyKey)other;
        return otherKey.getWrappedPropertyKey().equals(getWrappedPropertyKey());

    }

    /* (non-Javadoc)
     * @see org.apache.atlas.repository.graphdb.AtlasPropertyKey#getCardinality()
     */
    @Override
    public AtlasCardinality getCardinality() {
        return GraphDbObjectFactory.createCardinality(wrapped.cardinality());
    }

}
