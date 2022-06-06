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
package org.apache.atlas.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.atlas.model.annotation.AtlasJSON;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

@AtlasJSON
public abstract class AtlasBaseModelObject implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonIgnore
    private static AtomicLong s_nextId = new AtomicLong(System.nanoTime());

    private String guid;

    protected void init() {
        setGuid("-" + Long.toString(s_nextId.incrementAndGet()));
    }

    protected AtlasBaseModelObject() {
        init();
    }

    public String getGuid() {
        return this.guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public AtlasBaseModelObject(final AtlasBaseModelObject other) {
        this.guid = other.guid;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName());
        sb.append("{");
        sb.append("guid=").append(guid);
        toString(sb);
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof AtlasBaseModelObject)) return false;
        final AtlasBaseModelObject that = (AtlasBaseModelObject) o;
        return Objects.equals(guid, that.guid);
    }

    @Override
    public int hashCode() {

        return Objects.hash(guid);
    }

    protected abstract StringBuilder toString(StringBuilder sb);
}
