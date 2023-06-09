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

import static org.apache.drill.shaded.guava.com.google.common.base.Preconditions.checkArgument;

import java.util.List;

import org.apache.drill.common.types.TypeProtos.DataMode;

import org.apache.drill.shaded.guava.com.google.common.base.Predicate;
import org.apache.drill.shaded.guava.com.google.common.collect.Iterables;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

public class ListSchema implements RecordSchema {
    private List<Field> fields;

    public ListSchema() {
        this.fields = Lists.newArrayList();
    }

    @Override
    public void addField(Field field) {
        if (field.getFieldType().getMode() == DataMode.REPEATED || fields.isEmpty() || !isSingleTyped() ||
                !Iterables.getOnlyElement(fields).equals(field.getFieldType())) {
            fields.add(field);
        }
    }

    @Override
    public Field getField(String fieldName, int index) {
        Field field;
        if (isSingleTyped()) {
            field = Iterables.getOnlyElement(fields, null);
        } else {
            field = index < fields.size() ? fields.get(index) : null;
        }

        return field;
    }

    @Override
    public void removeField(Field field, int index) {
        checkArgument(fields.size() > index);
//        checkArgument(checkNotNull(fields.get(index)).getFieldId() == field.getFieldId());
        fields.remove(index);
    }

    @Override
    public Iterable<? extends Field> getFields() {
        return fields;
    }

    public boolean isSingleTyped() {
        return fields.size() <= 1;
    }

    @Override
    public String toSchemaString() {
        StringBuilder builder = new StringBuilder("List_fields:[");
        for (Field field : fields) {
            builder.append(field.toString());
        }
        builder.append("]");
        return builder.toString();
    }

    @Override
    public void resetMarkedFields() {
        for (Field field : fields) {
            field.setRead(false);
        }
    }

    @Override
    public Iterable<? extends Field> removeUnreadFields() {
        final List<Field> removedFields = Lists.newArrayList();
        Iterables.removeIf(fields, new Predicate<Field>() {
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
