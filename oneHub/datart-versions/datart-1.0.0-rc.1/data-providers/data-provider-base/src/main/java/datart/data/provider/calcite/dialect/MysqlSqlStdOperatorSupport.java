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
import org.apache.calcite.sql.dialect.MysqlSqlDialect;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import static datart.core.data.provider.StdSqlOperator.*;

public class MysqlSqlStdOperatorSupport extends MysqlSqlDialect implements SqlStdOperatorSupport, FetchAndOffsetSupport {

    static ConcurrentSkipListSet<StdSqlOperator> OWN_SUPPORTED = new ConcurrentSkipListSet<>(
            EnumSet.of(STDDEV, ABS, CEILING, FLOOR, POWER, ROUND, SQRT, EXP, LOG10, LN, MOD, RAND, DEGREES, RADIANS, TRUNC, SIGN,
            ACOS, ASIN, ATAN, ATAN2, SIN, COS, TAN, COT, LENGTH, CONCAT, REPLACE, SUBSTRING, LOWER, UPPER, LTRIM, RTRIM, TRIM,
            NOW, DAY, SECOND, MINUTE, HOUR, DAY, WEEK, QUARTER, MONTH, YEAR, DAY_OF_WEEK, DAY_OF_MONTH, DAY_OF_YEAR,
            IF, COALESCE, AGG_DATE_YEAR, AGG_DATE_QUARTER, AGG_DATE_MONTH, AGG_DATE_WEEK, AGG_DATE_DAY));

    static {
        OWN_SUPPORTED.addAll(SUPPORTED);
    }

    public MysqlSqlStdOperatorSupport() {
        this(DEFAULT_CONTEXT);
    }

    private MysqlSqlStdOperatorSupport(Context context) {
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
            case TRUNC:
                renameCallOperator("TRUNCATE", call);
                break;
            case DAY_OF_MONTH:
                renameCallOperator("DAYOFMONTH", call);
                break;
            case AGG_DATE_YEAR:
                writer.print("YEAR(" + call.getOperandList().get(0).toSqlString(this).getSql() + ")");
                return true;
            case AGG_DATE_QUARTER: {
                String columnName = call.getOperandList().get(0).toSqlString(this).getSql();
                writer.print("CONCAT(DATE_FORMAT("+columnName+",'%Y-'),QUARTER("+columnName+"))");
                return true;
            }
            case AGG_DATE_MONTH:
                writer.print("DATE_FORMAT(" + call.getOperandList().get(0).toSqlString(this).getSql() + ",'%Y-%m')");
                return true;
            case AGG_DATE_WEEK:
                writer.print("DATE_FORMAT(" + call.getOperandList().get(0).toSqlString(this).getSql() + ",'%x-%v')");
                return true;
            case AGG_DATE_DAY:
                writer.print("DATE_FORMAT(" + call.getOperandList().get(0).toSqlString(this).getSql() + ",'%Y-%m-%d')");
                return true;
            default:
                break;
        }
        return false;
    }

    @Override
    public void quoteStringLiteral(StringBuilder buf, String charsetName, String val) {
        buf.append(literalQuoteString);
        buf.append(val.replace(literalEndQuoteString, literalEscapedQuote));
        buf.append(literalEndQuoteString);
    }

    @Override
    public Set<StdSqlOperator> supportedOperators() {
        return OWN_SUPPORTED;
    }
}
