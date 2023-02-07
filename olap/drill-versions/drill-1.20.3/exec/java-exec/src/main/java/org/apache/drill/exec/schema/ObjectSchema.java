/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.schema;

import java.util.List;
import java.util.Map;

import org.apache.drill.shaded.guava.com.google.common.base.Predicate;
import org.apache.drill.shaded.guava.com.google.common.collect.Iterables;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;

public class ObjectSchema implements RecordSchema {
    private final Map<String, Field> fields;

    public ObjectSchema() {
        fields = Maps.newHashMap();
    }

    @Override
    public void addField(Field field) {
        fields.put(field.getFieldName(), field);
    }

    @Override
    public Field getField(String fieldName, int index) {
        return fields.get(fieldName);
    }

    @Override
    public void removeField(Field field, int index) {
        fields.remove(field.getFieldName());
    }

    @Override
    public Iterable<? extends Field> getFields() {
        return fields.values();
    }

    @Override
    public String toSchemaString() {
        StringBuilder builder = new StringBuilder("Object_fields:[");
        for (Field field : fields.values()) {
            builder.append(field.toString()).append(" ");
        }
        builder.append("]");
        return builder.toString();
    }

    @Override
    public void resetMarkedFields() {
        for (Field field : fields.values()) {
            field.setRead(false);
        }
    }

    @Override
    public Iterable<? extends Field> removeUnreadFields() {
        final List<Field> removedFields = Lists.newArrayList();
        Iterables.removeIf(fields.values(), new Predicate<Field>() {
            @Override
            public boolean apply(Field field) {
                if (!field.isRead()) {
                    removedFields.add(field);
                    return true;
                } else if (field.hasSchema()) {
                    Iterables.addAll(removedFields, field.getAssignedSchema().removeUnreadFields());
                }

                return false;
            }
        });
        return removedFields;
    }
}
