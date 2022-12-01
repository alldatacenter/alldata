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
package org.apache.atlas.utils;

import org.apache.atlas.model.Clearable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class FixedBufferList<T extends Clearable> {
    private static final Logger LOG = LoggerFactory.getLogger(FixedBufferList.class);

    private final Class<T> itemClass;
    private final ArrayList<T> buffer;
    private final int incrementCapacityBy;

    private int length;

    public FixedBufferList(Class<T> clazz) {
        this(clazz, 1);
    }

    public FixedBufferList(Class<T> clazz, int incrementCapacityBy) {
        this.incrementCapacityBy = (incrementCapacityBy <= 0 ? 1 : incrementCapacityBy);
        this.itemClass = clazz;
        this.buffer = new ArrayList<>();
    }

    public T next() {
        request(length + 1);
        return buffer.get(length++);
    }

    public List<T> toList() {
        return this.buffer.subList(0, this.length);
    }

    public void reset() {
        for (int i = 0; i < buffer.size(); i++) {
            buffer.get(i).clear();
        }

        this.length = 0;
    }

    private void request(int requestedCapacity) {
        if (requestedCapacity <= this.buffer.size()) {
            return;
        }

        int oldCapacity = this.buffer.size();
        int newCapacity = oldCapacity + this.incrementCapacityBy;
        this.buffer.ensureCapacity(newCapacity);
        instantiateItems(oldCapacity, newCapacity);

        if (LOG.isDebugEnabled()) {
            LOG.debug("FixedBufferList: Requested: {} From: {} To:{}", requestedCapacity, oldCapacity, newCapacity);
        }
    }

    private void instantiateItems(int startIndex, int maxSize) {
        for (int i = startIndex; i < maxSize; i++) {
            try {
                this.buffer.add(itemClass.newInstance());
            } catch (InstantiationException e) {
                LOG.error("FixedBufferList: InstantiationException: Instantiation failed!", e);
            } catch (IllegalAccessException e) {
                LOG.error("FixedBufferList: IllegalAccessException: Instantiation failed!", e);
            }
        }
    }
}
