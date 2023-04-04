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

package datart.data.provider.calcite.custom;

import datart.core.common.ReflectUtils;
import org.apache.calcite.sql.SqlCharStringLiteral;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlWriter;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.util.NlsString;

public class SqlSimpleStringLiteral extends SqlCharStringLiteral {

    private String stringVal;

    protected SqlSimpleStringLiteral(NlsString val, SqlParserPos pos) {
        super(val, pos);
    }

    public SqlSimpleStringLiteral(String val) {
        this(new NlsString(val, null, null), SqlParserPos.ZERO);
        this.stringVal = val;
    }

    public SqlSimpleStringLiteral(String val, SqlParserPos pos) {
        this(new NlsString(val, null, null), pos);
        this.stringVal = val;
    }

    @Override
    public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
        SqlDialect dialect = writer.getDialect();
        String literalQuoteString = ReflectUtils.getFieldValue(dialect, "literalQuoteString").toString();
        String literalEndQuoteString = ReflectUtils.getFieldValue(dialect, "literalEndQuoteString").toString();
        String literalEscapedQuote = ReflectUtils.getFieldValue(dialect, "literalEscapedQuote").toString();
        String buf = literalQuoteString +
                stringVal.replace(literalEndQuoteString, literalEscapedQuote) +
                literalEndQuoteString;
        writer.literal(buf);
    }
}
