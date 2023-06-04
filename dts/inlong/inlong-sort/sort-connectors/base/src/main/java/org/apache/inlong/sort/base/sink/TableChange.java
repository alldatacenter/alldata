/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.base.sink;

import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.util.Preconditions;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;

/**
 * TableChange represent requested changes to a table.
 */
public interface TableChange {

    final class First implements ColumnPosition {

        private static final First INSTANCE = new First();

        private First() {

        }

        @Override
        public String toString() {
            return "FIRST";
        }
    }

    final class After implements ColumnPosition {

        private final String column;

        private After(String column) {
            assert column != null;
            this.column = column;
        }

        public String column() {
            return column;
        }

        @Override
        public String toString() {
            return "AFTER " + column;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            After after = (After) o;
            return column.equals(after.column);
        }

        @Override
        public int hashCode() {
            return Objects.hash(column);
        }
    }

    interface ColumnPosition {

        static ColumnPosition first() {
            return First.INSTANCE;
        }

        static ColumnPosition after(String column) {
            return new After(column);
        }
    }

    interface ColumnChange extends TableChange {

        String[] fieldNames();
    }

    final class AddColumn implements ColumnChange {

        private final String[] fieldNames;
        private final LogicalType dataType;
        private final boolean isNullable;
        private final String comment;
        private final ColumnPosition position;

        public AddColumn(
                String[] fieldNames,
                LogicalType dataType,
                boolean isNullable,
                String comment,
                ColumnPosition position) {
            Preconditions.checkArgument(fieldNames.length > 0,
                    "Invalid field name: at least one name is required");
            this.fieldNames = fieldNames;
            this.dataType = dataType;
            this.isNullable = isNullable;
            this.comment = comment;
            this.position = position;
        }

        @Override
        public String[] fieldNames() {
            return fieldNames;
        }

        public LogicalType dataType() {
            return dataType;
        }

        public boolean isNullable() {
            return isNullable;
        }

        @Nullable
        public String comment() {
            return comment;
        }

        @Nullable
        public ColumnPosition position() {
            return position;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            AddColumn addColumn = (AddColumn) o;
            return isNullable == addColumn.isNullable
                    && Arrays.equals(fieldNames, addColumn.fieldNames)
                    && dataType.equals(addColumn.dataType)
                    && Objects.equals(comment, addColumn.comment)
                    && Objects.equals(position, addColumn.position);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(dataType, isNullable, comment, position);
            result = 31 * result + Arrays.hashCode(fieldNames);
            return result;
        }

        @Override
        public String toString() {
            return String.format("ADD COLUMNS `%s` %s %s %s %s",
                    fieldNames[fieldNames.length - 1],
                    dataType,
                    isNullable ? "" : "NOT NULL",
                    comment,
                    position);
        }
    }

    final class DeleteColumn implements ColumnChange {

        @Override
        public String[] fieldNames() {
            return new String[0];
        }
    }

    /**
     * Represents a column change that is not recognized by the connector.
     */
    final class UnknownColumnChange implements ColumnChange {

        private String description;

        public UnknownColumnChange(String description) {
            this.description = description;
        }

        @Override
        public String[] fieldNames() {
            return new String[0];
        }

        @Override
        public String toString() {
            return description;
        }
    }
}
