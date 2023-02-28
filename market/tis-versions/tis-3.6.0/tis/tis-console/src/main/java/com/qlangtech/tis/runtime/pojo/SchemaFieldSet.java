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
package com.qlangtech.tis.runtime.pojo;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2012-12-19
 */
public class SchemaFieldSet extends HashSet<SchemaField> {

    private static final long serialVersionUID = 6434092100460517602L;

    private final Addable addable;

    private final HashSet<SchemaField> allfields = new HashSet<SchemaField>();

    public abstract static class Addable {

        public abstract boolean can(SchemaField e);
    }

    public SchemaFieldSet(Addable addable) {
        super();
        this.addable = addable;
    }

    @Override
    public boolean addAll(Collection<? extends SchemaField> c) {
        for (SchemaField f : c) {
            this.add(f);
        }
        allfields.addAll(c);
        return true;
    }

    public final Set<SchemaField> getSolrSchema() {
        return this.allfields;
    }

    @Override
    public boolean add(SchemaField e) {
        if (addable.can(e)) {
            return super.add(e);
        }
        return true;
    }
}
