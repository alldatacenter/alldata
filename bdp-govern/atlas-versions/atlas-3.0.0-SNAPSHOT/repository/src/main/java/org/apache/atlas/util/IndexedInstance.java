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

import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.atlas.repository.graph.GraphHelper;

/**
 * Data structure that stores an IReferenceableInstance and its location within
 * a list.
 *
 * @see GraphHelper#getVerticesForInstancesByUniqueAttributes
 */
public class IndexedInstance {

    private final Referenceable instance_;
    private final int index_;

    public IndexedInstance(Referenceable instance, int index) {
        super();
        this.instance_ = instance;
        this.index_ = index;
    }

    public Referenceable getInstance() {
        return instance_;
    }

    public int getIndex() {
        return index_;
    }

    @Override
    public int hashCode() {
        return instance_.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if(!(other instanceof IndexedInstance)) {
            return false;
        }
        IndexedInstance otherInstance = (IndexedInstance)other;
        return instance_.equals(otherInstance.getInstance());
    }

}