/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datart.core.data.provider.sql;

import org.apache.commons.lang.ArrayUtils;

import java.util.Arrays;

public abstract class ColumnOperator implements Operator {

    protected String[] column;

    public String getColumnKey() {
        if (column == null) {
            return null;
        }
        return String.join(".", column);
    }


    public String[] getColumnNames(boolean withDefaultColumnPrefix, String defaultColumnPrefix) {
        if (withDefaultColumnPrefix) {
            String[] names = new String[column.length + 1];
            names[0] = defaultColumnPrefix;
            return (String[]) ArrayUtils.add(column, 0, defaultColumnPrefix);
        } else {
            return column;
        }
    }

    public void setColumn(String... column) {
        this.column = column;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ColumnOperator)) return false;
        ColumnOperator that = (ColumnOperator) o;
        return Arrays.equals(column, that.column);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(column);
    }
}
