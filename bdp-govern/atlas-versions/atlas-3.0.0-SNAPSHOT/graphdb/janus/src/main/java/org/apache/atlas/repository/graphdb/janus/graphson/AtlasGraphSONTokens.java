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
package org.apache.atlas.repository.graphdb.janus.graphson;

public final class AtlasGraphSONTokens {

    private AtlasGraphSONTokens() {}

    public static final String VERTEX = "vertex";
    public static final String EDGE = "edge";
    public static final String INTERNAL_ID = "_id";
    public static final String INTERNAL_LABEL = "_label";
    public static final String INTERNAL_TYPE = "_type";
    public static final String INTERNAL_OUT_V = "_outV";
    public static final String INTERNAL_IN_V = "_inV";
    public static final String VALUE = "value";
    public static final String TYPE = "type";
    public static final String TYPE_LIST = "list";
    public static final String TYPE_STRING = "string";
    public static final String TYPE_DOUBLE = "double";
    public static final String TYPE_INTEGER = "integer";
    public static final String TYPE_FLOAT = "float";
    public static final String TYPE_MAP = "map";
    public static final String TYPE_BOOLEAN = "boolean";
    public static final String TYPE_LONG = "long";
    public static final String TYPE_SHORT = "short";
    public static final String TYPE_BYTE = "byte";
    public static final String TYPE_UNKNOWN = "unknown";

    public static final String VERTICES = "vertices";
    public static final String EDGES = "edges";
    public static final String MODE = "mode";
}
