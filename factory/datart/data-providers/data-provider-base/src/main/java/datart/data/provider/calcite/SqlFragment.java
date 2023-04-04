package datart.data.provider.calcite;

import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlWriter;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.type.SqlTypeName;

public class SqlFragment extends SqlLiteral {

    /**
     * Creates a <code> Sql Snippet</code>.
     *
     * @param value String value
     */
    public SqlFragment(String value) {
        super(value, SqlTypeName.MULTISET, SqlParserPos.ZERO);
    }

    @Override
    public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
        writer.print(" ");
        writer.print(value.toString());
        writer.print(" ");
    }
}
