package datart.data.provider.calcite;

import datart.data.provider.calcite.parser.impl.SqlParserImpl;
import org.apache.calcite.avatica.util.Casing;
import org.apache.calcite.avatica.util.Quoting;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.validate.SqlConformanceEnum;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SqlParserUtils {

    private static final String SELECT_SQL = "SELECT %s FROM DATART_VTABLE";

    public static SqlNode parseSnippet(String snippet) throws SqlParseException {
        String sql = String.format(SELECT_SQL, snippet);
        SqlParser.Config config = SqlParser.config()
                .withParserFactory(SqlParserImpl.FACTORY)
                .withQuotedCasing(Casing.UNCHANGED)
                .withUnquotedCasing(Casing.UNCHANGED)
                .withConformance(SqlConformanceEnum.LENIENT)
                .withCaseSensitive(true)
                .withQuoting(Quoting.BRACKET);
        return SqlParser.create(sql, config).parseQuery();

    }

    public static SqlParser createParser(SqlDialect sqlDialect) {
        return createParser("", sqlDialect);
    }

    public static SqlParser createParser(String sql, SqlDialect sqlDialect) {
        SqlParser.Config config = SqlParser.config()
                .withParserFactory(SqlParserImpl.FACTORY)
                .withConformance(SqlConformanceEnum.LENIENT)
                .withUnquotedCasing(sqlDialect.getUnquotedCasing())
                .withQuotedCasing(sqlDialect.getQuotedCasing())
                .withQuoting(getQuoting(sqlDialect));
        return SqlParser.create(sql, config);
    }

    private static Quoting getQuoting(SqlDialect sqlDialect) {
        try {
            Method method = SqlDialect.class.getDeclaredMethod("getQuoting");
            method.setAccessible(true);
            Object invoke = method.invoke(sqlDialect);
            return (Quoting) invoke;
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }


}
