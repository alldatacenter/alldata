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

import datart.core.data.provider.StdSqlOperator;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlWriter;
import org.apache.calcite.sql.dialect.MssqlSqlDialect;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import static datart.core.data.provider.StdSqlOperator.*;
import static datart.core.data.provider.StdSqlOperator.COALESCE;

public class MsSqlStdOperatorSupport extends MssqlSqlDialect implements SqlStdOperatorSupport {

    static ConcurrentSkipListSet<StdSqlOperator> OWN_SUPPORTED = new ConcurrentSkipListSet<>(
            EnumSet.of(STDDEV, ABS, MEDIAN, ABS, CEILING, FLOOR, POWER, ROUND, SQRT,
                    EXP, LN, MOD, TRUNC, SIGN, ACOS, ASIN, ATAN, ATAN2, SIN, COS, TAN,
                    LENGTH, CONCAT, REPLACE, SUBSTRING, LOWER, UPPER, LTRIM, RTRIM, TRIM,
                    NOW, COALESCE, AGG_DATE_YEAR, AGG_DATE_QUARTER, AGG_DATE_MONTH, AGG_DATE_WEEK, AGG_DATE_DAY));

    static {
        OWN_SUPPORTED.addAll(SUPPORTED);
    }

    public MsSqlStdOperatorSupport() {
        this(DEFAULT_CONTEXT);
    }

    private MsSqlStdOperatorSupport(Context context) {
        super(context);
    }

    @Override
    public void unparseCall(SqlWriter writer, SqlCall call, int leftPrec, int rightPrec) {
        if (isStdSqlOperator(call) && unparseStdSqlOperator(writer, call, leftPrec, rightPrec)) {
            return;
        }
        super.unparseCall(writer, call, leftPrec, rightPrec);
    }

    @Override
    public boolean unparseStdSqlOperator(SqlWriter writer, SqlCall call, int leftPrec, int rightPrec) {
        StdSqlOperator operator = symbolOf(call.getOperator().getName());
        switch (operator) {
            case AGG_DATE_YEAR:
                writer.print("YEAR(" + call.getOperandList().get(0).toSqlString(this).getSql() + ")");
                return true;
            case AGG_DATE_QUARTER: {
                String columnName = call.getOperandList().get(0).toSqlString(this).getSql();
                writer.print("CONCAT(YEAR("+columnName+"), '-', (MONTH("+columnName+")+2)/3)");
                return true;
            }
            case AGG_DATE_MONTH: {
                String columnName = call.getOperandList().get(0).toSqlString(this).getSql();
                writer.print("CONCAT(YEAR("+columnName+"), '-', MONTH("+columnName+"))");
                return true;
            }
            case AGG_DATE_WEEK: {
                String columnName = call.getOperandList().get(0).toSqlString(this).getSql();
                writer.print("CONCAT(YEAR("+columnName+"), '-', RIGHT(100+DATEPART(ww,"+columnName+"),2))");
                return true;
            }
            case AGG_DATE_DAY: {
                String columnName = call.getOperandList().get(0).toSqlString(this).getSql();
                writer.print("CONCAT(YEAR("+columnName+"), '-', MONTH("+columnName+"), '-', DAY("+columnName+"))");
                return true;
            }
            default:
                break;
        }
        return false;
    }

    @Override
    public Set<StdSqlOperator> supportedOperators() {
        return OWN_SUPPORTED;
    }
}
