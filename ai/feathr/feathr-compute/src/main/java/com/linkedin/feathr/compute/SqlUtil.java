package com.linkedin.feathr.compute;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;


/**
 * Class for SQL utilities
 */
public class SqlUtil {
  private SqlUtil() { }

  /**
   * Try to find the input feature names from a sqlExpr derived feature.
   * (Without depending on Spark and Scala.)
   *
   * @param sql a sql expression
   * @return list of input feature names (without any duplicates)
   */
  public static List<String> getInputsFromSqlExpression(String sql) {
    Set<String> inputs = new HashSet<>();
    ExpressionVisitorAdapter visitor = new ExpressionVisitorAdapter() {
      @Override
      public void visit(Column column) {
        inputs.add(column.getColumnName());
      }
    };
    try {
      CCJSqlParserUtil.parseExpression(sql).accept(visitor);
    } catch (JSQLParserException e) {
      throw new RuntimeException(e);
    }
    return new ArrayList<>(inputs);
  }
}