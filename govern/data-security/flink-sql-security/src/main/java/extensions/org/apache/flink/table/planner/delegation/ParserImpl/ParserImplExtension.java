package extensions.org.apache.flink.table.planner.delegation.ParserImpl;

import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.Jailbreak;
import manifold.ext.rt.api.This;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.flink.table.api.SqlParserException;
import org.apache.flink.table.planner.delegation.ParserImpl;
import org.apache.flink.table.planner.parse.CalciteParser;
import org.apache.flink.util.Preconditions;

import java.util.List;

/**
 * Extend {@link ParserImpl} with manifold to add method parseExpression(String sqlExpression) and parseSql(String)
 *
 * @author: HamaWhite
 */
@Extension
public class ParserImplExtension {

    private ParserImplExtension() {
        throw new IllegalStateException("Extension class");
    }

    /**
     * Parses a SQL expression into a {@link SqlNode}. The {@link SqlNode} is not yet validated.
     *
     * @param sqlExpression a SQL expression string to parse
     * @return a parsed SQL node
     * @throws SqlParserException if an exception is thrown when parsing the statement
     */
    public static SqlNode parseExpression(@This @Jailbreak ParserImpl thiz,String sqlExpression) {
        // add @Jailbreak annotation to access private variables
        CalciteParser parser = thiz.calciteParserSupplier.get();
        return parser.parseExpression(sqlExpression);
    }

    /**
     * Entry point for parsing SQL queries and return the abstract syntax tree
     *
     * @param statement the SQL statement to evaluate
     * @return abstract syntax tree
     * @throws org.apache.flink.table.api.SqlParserException when failed to parse the statement
     */
    public static SqlNode parseSql(@This @Jailbreak ParserImpl thiz, String statement) {
        // add @Jailbreak annotation to access private variables
        CalciteParser parser = thiz.calciteParserSupplier.get();

        // use parseSqlList here because we need to support statement end with ';' in sql client.
        SqlNodeList sqlNodeList = parser.parseSqlList(statement);
        List<SqlNode> parsed = sqlNodeList.getList();
        Preconditions.checkArgument(parsed.size() == 1, "only single statement supported");
        return parsed.get(0);
    }
}
