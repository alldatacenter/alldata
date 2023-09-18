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
package datart.data.provider.calcite.dialect;

import com.google.common.collect.ImmutableSet;
import datart.core.base.exception.Exceptions;
import datart.core.data.provider.StdSqlOperator;
import org.apache.calcite.sql.*;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import static datart.core.data.provider.StdSqlOperator.*;

public interface SqlStdOperatorSupport {

    ConcurrentSkipListSet<StdSqlOperator> SUPPORTED = new ConcurrentSkipListSet<>(
            EnumSet.of(SUM, AVG, MAX, MIN, COUNT, DISTINCT,
                    ADD, SUBTRACT, MULTIPLY, DIVIDE, EQUALS, NOT_EQUALS, GREETER_THAN, GREETER_THAN_EQ, LESS_THAN, LESS_THAN_EQ));

    ConcurrentSkipListSet<StdSqlOperator> SPECIAL_FUNCTION = new ConcurrentSkipListSet<>();

    default boolean support(StdSqlOperator stdSqlOperator) {
        return SUPPORTED.contains(stdSqlOperator);
    }

    default Set<StdSqlOperator> supportedOperators() {
        return ImmutableSet.copyOf(SUPPORTED);
    }

    default Set<StdSqlOperator> unSupportedOperators() {
        EnumSet<StdSqlOperator> allFunctions = EnumSet.allOf(StdSqlOperator.class);
        allFunctions.removeAll(SUPPORTED);
        return allFunctions;
    }

    default boolean isStdSqlOperator(SqlCall sqlCall) {
        return StdSqlOperator.symbolOf(sqlCall.getOperator().getName()) != null;
    }

    boolean unparseStdSqlOperator(SqlWriter writer, SqlCall call, int leftPrec, int rightPrec);

    default void renameCallOperator(String newName, SqlCall call) {
        try {
            SqlOperator operator = call.getOperator();
            Field nameField = SqlOperator.class.getDeclaredField("name");
            nameField.setAccessible(true);
            nameField.set(operator, newName);
            if (operator instanceof SqlFunction) {
                Field sqlIdentifierField = SqlFunction.class.getDeclaredField("sqlIdentifier");
                sqlIdentifierField.setAccessible(true);
                SqlIdentifier sqlIdentifier = (SqlIdentifier)sqlIdentifierField.get(call.getOperator());
                sqlIdentifierField.set(operator,sqlIdentifier.setName(0, newName));
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Exceptions.e(e);
        }
    }

}
