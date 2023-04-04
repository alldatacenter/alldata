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

import datart.core.common.BeanUtils;
import datart.data.provider.jdbc.JdbcDriverInfo;
import org.apache.calcite.avatica.util.Casing;
import org.apache.calcite.config.NullCollation;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlWriter;
import org.apache.calcite.sql.validate.SqlConformanceEnum;

public class CustomSqlDialect extends SqlDialect {

    private JdbcDriverInfo driverInfo;

    private CustomSqlDialect(Context context) {
        super(context);
    }

    public CustomSqlDialect(JdbcDriverInfo driverInfo) {
        this(createContext(driverInfo));
        this.driverInfo = driverInfo;
    }

    private static Context createContext(JdbcDriverInfo driverInfo) {
        BeanUtils.validate(driverInfo);
        return SqlDialect.EMPTY_CONTEXT
                .withDatabaseProductName(driverInfo.getName())
                .withDatabaseVersion(driverInfo.getVersion())
                .withConformance(SqlConformanceEnum.LENIENT)
                .withIdentifierQuoteString(driverInfo.getIdentifierQuote())
                .withLiteralQuoteString(driverInfo.getLiteralQuote())
                .withUnquotedCasing(Casing.UNCHANGED)
                .withNullCollation(NullCollation.LOW);
    }

    @Override
    public void unparseOffsetFetch(SqlWriter writer, SqlNode offset, SqlNode fetch) {
        if (driverInfo.getSupportSqlLimit() != null && driverInfo.getSupportSqlLimit()) {
            unparseFetchUsingLimit(writer, offset, fetch);
        } else {
            super.unparseOffsetFetch(writer, offset, fetch);
        }
    }
}
