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
package org.apache.atlas.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.atlas.repository.graph.GraphHelper;

/**
 * Map of attribute values to a collection of IndexedInstances with that attribute value.
 *
 * @see GraphHelper#getVerticesForInstancesByUniqueAttributes
 *
 */
public class AttributeValueMap {

    //need collection in case they are adding the same entity twice?
    private Map<Object,Collection<IndexedInstance>> valueMap_ = new HashMap<>();

    public void put(Object value, Referenceable instance, int index) {
        IndexedInstance wrapper = new IndexedInstance(instance, index);
        Collection<IndexedInstance> existingValues = valueMap_.get(value);
        if(existingValues == null) {
            //only expect 1 value
            existingValues = new HashSet<>(1);
            valueMap_.put(value, existingValues);
        }
        existingValues.add(wrapper);
    }

    public Collection<IndexedInstance> get(Object value) {
        return valueMap_.get(value);
    }


    public Set<Object> getAttributeValues() {
        return valueMap_.keySet();
    }

}