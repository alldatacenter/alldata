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

import datart.core.data.provider.SingleTypedValue;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Arrays;

@Data
@EqualsAndHashCode(callSuper = true)
public class FilterOperator extends ColumnOperator {

    private AggregateOperator.SqlOperator aggOperator;

    private SqlOperator sqlOperator;

    private SingleTypedValue[] values;

    public enum SqlOperator {

        EQ, //EQUAL
        NE, //NOT EQUAL
        GT, //GREATER THAN
        LT, //LESS THAN
        GTE, //GREATER THAN OR EQUAL
        LTE, //LESS THAN OR EQUAL

        IN,
        NOT_IN,

        LIKE,
        PREFIX_LIKE,
        SUFFIX_LIKE,
        NOT_LIKE,
        PREFIX_NOT_LIKE,
        SUFFIX_NOT_LIKE,

        IS_NULL,
        NOT_NULL,

        BETWEEN,
        NOT_BETWEEN,
    }

    @Override
    public String toString() {
        return "FilterOperator{" +
                "column='" + getColumnKey() + '\'' +
                ", aggOperator=" + aggOperator +
                ", sqlOperator=" + sqlOperator +
                ", values=" + Arrays.toString(values) +
                '}';
    }
}