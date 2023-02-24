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
package org.apache.atlas.repository.store.graph;

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.atlas.repository.graphdb.AtlasVertex;

import java.util.List;

/**
 * Interface for graph persistence store for AtlasTypeDef
 */
public interface AtlasDefStore<T extends AtlasBaseTypeDef> {
    AtlasVertex preCreate(T typeDef) throws AtlasBaseException;

    T create(T typeDef, AtlasVertex preCreateResult) throws AtlasBaseException;

    List<T> getAll() throws AtlasBaseException;

    T getByName(String name) throws AtlasBaseException;

    T getByGuid(String guid) throws AtlasBaseException;

    T update(T typeDef) throws AtlasBaseException;

    T updateByName(String name, T typeDef) throws AtlasBaseException;

    T updateByGuid(String guid, T typeDef) throws AtlasBaseException;

    AtlasVertex preDeleteByName(String name) throws AtlasBaseException;

    void deleteByName(String name, AtlasVertex preDeleteResult) throws AtlasBaseException;

    AtlasVertex preDeleteByGuid(String guid) throws AtlasBaseException;

    void deleteByGuid(String guid, AtlasVertex preDeleteResult) throws AtlasBaseException;
}
