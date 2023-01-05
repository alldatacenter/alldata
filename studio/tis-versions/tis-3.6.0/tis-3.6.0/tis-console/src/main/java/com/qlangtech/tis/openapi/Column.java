/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.openapi;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2013-12-20
 */
public class Column {

    private static final long serialVersionUID = 1L;

    private final String name;

    private Type fieldType;

    private boolean index = true;

    private boolean stored = true;

    public Column(String name) {
        super();
        this.name = name;
    }

    public boolean isIndex() {
        return index;
    }

    public void setIndex(boolean index) {
        this.index = index;
    }

    public boolean isStored() {
        return stored;
    }

    public void setStored(boolean stored) {
        this.stored = stored;
    }

    public Type getFieldType() {
        return fieldType;
    }

    public void setFieldType(Type fieldType) {
        this.fieldType = fieldType;
    }

    public String getName() {
        return name;
    }

    private boolean unique;

    public boolean isUnique() {
        return unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public enum Type {

        STRING("str"),
        INT("tint"),
        FLOAT("tfloat"),
        DOUBLE("tdouble"),
        SHORT("tshort"),
        LONG("tlong");

        private final String solrType;

        private Type(String solrType) {
            this.solrType = solrType;
        }

        public String getSolrType() {
            return solrType;
        }
    }
}
