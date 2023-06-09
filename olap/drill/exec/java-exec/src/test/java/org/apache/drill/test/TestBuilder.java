/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.parser.LogicalExpressionParser;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.expr.fn.impl.DateUtility;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.proto.UserBitShared.QueryType;
import org.apache.drill.exec.proto.UserProtos.PreparedStatementHandle;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.util.JsonStringArrayList;
import org.apache.drill.exec.util.JsonStringHashMap;
import org.apache.drill.exec.util.Text;
import org.apache.drill.test.DrillTestWrapper.TestServices;
import org.joda.time.DateTimeZone;

import org.apache.drill.shaded.guava.com.google.common.base.Joiner;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

public class TestBuilder {

  /**
   * Test query to run. Type of object depends on the {@link #queryType}
   */
  private Object query;
  // the type of query for the test
  private UserBitShared.QueryType queryType;
  // should the validation enforce ordering
  private Boolean ordered;
  private boolean approximateEquality;
  private double tolerance;
  private final TestServices services;
  // Used to pass the type information associated with particular column names rather than relying on the
  // ordering of the columns in the CSV file, or the default type inferences when reading JSON, this is used for the
  // case where results of the test query are adding type casts to the baseline queries, this saves a little bit of
  // setup in cases where strict type enforcement is not necessary for a given test
  protected Map<SchemaPath, TypeProtos.MajorType> baselineTypeMap;
  // queries to run before the baseline or test queries, can be used to set options
  private String baselineOptionSettingQueries;
  private String testOptionSettingQueries = "";
  // two different methods are available for comparing ordered results, the default reads all of the records
  // into giant lists of objects, like one giant on-heap batch of 'vectors'
  // this flag enables the other approach which iterates through a hyper batch for the test query results and baseline
  // while this does work faster and use less memory, it can be harder to debug as all of the elements are not in a
  // single list
  private boolean highPerformanceComparison;
  // column names for use with the baseline values
  protected String[] baselineColumns;
  // In cases where we need to verify larger datasets without the risk of running the baseline data through
  // the drill engine, results can be provided in a list of maps. While this model does make a lot of sense, there is a
  // lot of work to make the type handling/casting work correctly, and making robust complex type handling work completely outside
  // of the drill engine for generating baselines would likely be more work than it would be worth. For now we will be
  // going with an approach of using this facility to validate the parts of the drill engine that could break in ways
  // that would affect the reading of baseline files (i.e. we need robust test for storage engines, project and casting that
  // use this interface) and then rely on the engine for the rest of the tests that will use the baseline queries.
  private List<Map<String, Object>> baselineRecords;

  private int expectedNumBatches = DrillTestWrapper.EXPECTED_BATCH_COUNT_NOT_SET;
  private int expectedNumRecords = DrillTestWrapper.EXPECTED_NUM_RECORDS_NOT_SET;

  public TestBuilder(TestServices services) {
    this.services = services;
    reset();
  }

  public TestBuilder(TestServices services, Object query, UserBitShared.QueryType queryType, Boolean ordered,
                     boolean approximateEquality, Map<SchemaPath, TypeProtos.MajorType> baselineTypeMap,
                     String baselineOptionSettingQueries, String testOptionSettingQueries, boolean highPerformanceComparison,
                     int expectedNumBatches) {
    this(services);
    if (ordered == null) {
      throw new RuntimeException("Ordering not set, when using a baseline file or query you must explicitly call the ordered() or unOrdered() method on the " + this.getClass().getSimpleName());
    }
    this.query = query;
    this.queryType = queryType;
    this.ordered = ordered;
    this.approximateEquality = approximateEquality;
    this.baselineTypeMap = baselineTypeMap;
    this.baselineOptionSettingQueries = baselineOptionSettingQueries;
    this.testOptionSettingQueries = StringUtils.isNotEmpty(testOptionSettingQueries)
        ? testOptionSettingQueries.concat(" ; ")
        : testOptionSettingQueries;
    this.highPerformanceComparison = highPerformanceComparison;
    this.expectedNumBatches = expectedNumBatches;
  }

  protected TestBuilder reset() {
    query = "";
    ordered = null;
    approximateEquality = false;
    tolerance = 0.1;
    highPerformanceComparison = false;
    testOptionSettingQueries = "";
    baselineOptionSettingQueries = "";
    baselineRecords = null;
    return this;
  }

  public DrillTestWrapper build() {
    return new DrillTestWrapper(this, services, query, queryType, baselineOptionSettingQueries, testOptionSettingQueries,
        getValidationQueryType(), ordered, approximateEquality, tolerance, highPerformanceComparison, baselineColumns,
            baselineRecords, expectedNumBatches, expectedNumRecords);
  }

  public List<Pair<SchemaPath, TypeProtos.MajorType>> getExpectedSchema() {
    return null;
  }

  public void go() throws Exception {
    build().run();
  }

  public TestBuilder sqlQuery(String query) {
    this.query = QueryTestUtil.normalizeQuery(query);
    this.queryType = UserBitShared.QueryType.SQL;
    return this;
  }

  public TestBuilder sqlQuery(String query, Object... replacements) {
    return sqlQuery(String.format(query, replacements));
  }

  public TestBuilder preparedStatement(PreparedStatementHandle preparedStatementHandle) {
    queryType = QueryType.PREPARED_STATEMENT;
    query = preparedStatementHandle;
    return this;
  }

  public TestBuilder sqlQueryFromFile(String queryFile) throws IOException {
    this.query = BaseTestQuery.getFile(queryFile);
    queryType = UserBitShared.QueryType.SQL;
    return this;
  }

  public TestBuilder physicalPlanFromFile(String queryFile) throws IOException {
    this.query =  BaseTestQuery.getFile(queryFile);
    queryType = UserBitShared.QueryType.PHYSICAL;
    return this;
  }

  public TestBuilder ordered() {
    ordered = true;
    return this;
  }

  public TestBuilder unOrdered() {
    ordered = false;
    return this;
  }

  // this can only be used with ordered verifications, it does run faster and use less memory but may be
  // a little harder to debug as it iterates over a hyper batch rather than reading all of the values into
  // large on-heap lists
  public TestBuilder highPerformanceComparison() throws Exception {
    highPerformanceComparison = true;
    return this;
  }

  // list of queries to run before the baseline query, can be used to set several options
  // list takes the form of a semi-colon separated list
  public TestBuilder optionSettingQueriesForBaseline(String queries) {
    baselineOptionSettingQueries = queries;
    return this;
  }

  public TestBuilder optionSettingQueriesForBaseline(String queries, Object... args) {
    baselineOptionSettingQueries = String.format(queries, args);
    return this;
  }

  /**
   *  list of queries to run before the test query, can be used to set several options
   *  list takes the form of a semi-colon separated list.
   * @param queries queries that set session and system options
   * @return this test builder
   */

  public TestBuilder optionSettingQueriesForTestQuery(String queries) {
    testOptionSettingQueries += queries.concat(" ; ");
    return this;
  }

  public TestBuilder optionSettingQueriesForTestQuery(String query, Object... args) throws Exception {
    testOptionSettingQueries += String.format(query, args).concat(" ; ");
    return this;
  }

  public TestBuilder approximateEquality() {
    return approximateEquality(0.1);
  }

  public TestBuilder approximateEquality(double tolerance) {
    approximateEquality = true;
    this.tolerance = tolerance;
    return this;
  }

  // modified code from SchemaPath.De class. This should be used sparingly and only in tests if absolutely needed.
  public static SchemaPath parsePath(String path) {
    LogicalExpression expr = LogicalExpressionParser.parse(path);
    if (expr instanceof SchemaPath) {
      return (SchemaPath) expr;
    } else {
      throw new IllegalStateException(String.format("Schema path is not a valid format: %s.", expr));
    }
  }

  Object getValidationQuery() throws Exception {
    throw new RuntimeException("Must provide some kind of baseline, either a baseline file or another query");
  }

  protected UserBitShared.QueryType getValidationQueryType() {
    return null;
  }

  public JSONTestBuilder jsonBaselineFile(String filePath) {
    return new JSONTestBuilder(filePath, services, query, queryType, ordered, approximateEquality,
        baselineTypeMap, baselineOptionSettingQueries, testOptionSettingQueries, highPerformanceComparison,
        expectedNumBatches);
  }

  public CSVTestBuilder csvBaselineFile(String filePath) {
    return new CSVTestBuilder(filePath, services, query, queryType, ordered, approximateEquality,
        baselineTypeMap, baselineOptionSettingQueries, testOptionSettingQueries, highPerformanceComparison,
        expectedNumBatches);
  }

  public SchemaTestBuilder schemaBaseLine(BatchSchema batchSchema) {
    List<Pair<SchemaPath, TypeProtos.MajorType>> expectedSchema = new ArrayList<>();
    for (final MaterializedField field : batchSchema) {
      expectedSchema.add(Pair.of(SchemaPath.getSimplePath(field.getName()), field.getType()));
    }
    return schemaBaseLine(expectedSchema);
  }

  public SchemaTestBuilder schemaBaseLine(List<Pair<SchemaPath, TypeProtos.MajorType>> expectedSchema) {
    assert expectedSchema != null : "The expected schema can be provided once";
    assert baselineColumns == null : "The column information should be captured in expected schema, not baselineColumns";

    return new SchemaTestBuilder(
        services,
        query,
        queryType,
        baselineOptionSettingQueries,
        testOptionSettingQueries,
        expectedSchema);
  }

  public TestBuilder baselineTypes(Map<SchemaPath, TypeProtos.MajorType> baselineTypeMap) {
    this.baselineTypeMap = baselineTypeMap;
    return this;
  }

  boolean typeInfoSet() {
    return baselineTypeMap != null;
  }

  /**
   * Indicate that the tests query should be checked for an empty result set.
   * @return the test builder
   */
  public TestBuilder expectsEmptyResultSet() {
    unOrdered();
    baselineRecords = new ArrayList<>();
    return this;
  }

  /**
   * Sets the number of expected batch count for this query. The test will fail if the query returns a different number
   * of batches
   *
   * @param expectedNumBatches expected batch count
   * @return this test builder
   */
  public TestBuilder expectsNumBatches(int expectedNumBatches) {
    this.expectedNumBatches = expectedNumBatches;
    return this;
  }

  public TestBuilder expectsNumRecords(int expectedNumRecords) {
    this.expectedNumRecords = expectedNumRecords;
    this.ordered = false;
    return this;
  }

  /**
   * This method is used to pass in a simple list of values for a single record verification without
   * the need to create a CSV or JSON file to store the baseline.
   *
   * This can be called repeatedly to pass a list of records to verify. It works for both ordered and unordered
   * checks.
   *
   * @param baselineValues - the baseline values to validate
   * @return the test builder
   */
  public TestBuilder baselineValues(Object... baselineValues) {
    assert getExpectedSchema() == null : "The expected schema is not needed when baselineValues are provided ";
    if (ordered == null) {
      throw new RuntimeException("Ordering not set, before specifying baseline data you must explicitly call the ordered() or unOrdered() method on the " + this.getClass().getSimpleName());
    }
    if (baselineRecords == null) {
      baselineRecords = new ArrayList<>();
    }
    Map<String, Object> ret = new HashMap<>();
    int i = 0;
    if(baselineValues != null) {
      assertEquals("Must supply the same number of baseline values as columns.", baselineValues.length, baselineColumns.length);
      for (String s : baselineColumns) {
        ret.put(s, baselineValues[i]);
        i++;
      }
    } else {
      assertEquals("Must supply the same number of baseline values as columns.", 1, baselineColumns.length);
      ret.put(baselineColumns[0], null);
    }


    this.baselineRecords.add(ret);
    return this;
  }

  /**
   * This method is used to pass in an array of values for records verification in case if
   * {@link #baselineColumns(String...)} specifies one column only without
   * the need to create a CSV or JSON file to store the baseline.
   *
   * This can be called repeatedly to pass an array of records to verify. It works for both ordered and unordered
   * checks.
   *
   * @param baselineValues baseline values for a single column to validate
   * @return {@code this} test builder
   */
  public TestBuilder baselineValuesForSingleColumn(Object... baselineValues) {
    assertEquals("Only one column should be specified", 1, baselineColumns.length);
    Arrays.stream(baselineValues)
        .forEach(this::baselineValues);
    return this;
  }

  /**
   * This can be used in cases where we want to avoid issues with the assumptions made by the test framework.
   * Most of the methods for verification in the framework run drill queries to generate the read baseline files or
   * execute alternative baseline queries. This model relies on basic functionality of reading files with storage
   * plugins and applying casts/projects to be stable.
   *
   * This method can be used to verify the engine for these cases and any other future execution paths that would
   * be used by both the test query and baseline. Without tests like this it is possible that some tests
   * could falsely report as passing, as both the test query and baseline query could run into the same problem
   * with an assumed stable code path and produce the same erroneous result.
   *
   * @param materializedRecords - a list of maps representing materialized results
   * @return the test builder
   */
  public TestBuilder baselineRecords(List<Map<String, Object>> materializedRecords) {
    this.baselineRecords = materializedRecords;
    return this;
  }

  /**
   * This setting has a slightly different impact on the test depending on some of the other
   * configuration options are set.
   *
   * If a JSON baseline file is given, this list will act as a project list to verify the
   * test query against a subset of the columns in the file.
   *
   * For a CSV baseline file, these will act as aliases for columns [0 .. n] in the repeated
   * varchar column that is read out of CSV.
   *
   * For a baseline sql query, this currently has no effect.
   *
   * For explicit baseline values given in java code with the baselineValues() method, these will
   * be used to create a map for the one record verification.
   */
  public TestBuilder baselineColumns(String... columns) {
    assert getExpectedSchema() == null : "The expected schema is not needed when baselineColumns are provided ";
    for (int i = 0; i < columns.length; i++) {
      columns[i] = parsePath(columns[i]).toExpr();
    }
    this.baselineColumns = columns;
    return this;
  }

  /**
   * Provide a SQL query to validate against.
   * @param baselineQuery
   * @return the test builder
   */
  public BaselineQueryTestBuilder sqlBaselineQuery(Object baselineQuery) {
    return new BaselineQueryTestBuilder(baselineQuery, UserBitShared.QueryType.SQL, services, query, queryType, ordered, approximateEquality,
        baselineTypeMap, baselineOptionSettingQueries, testOptionSettingQueries, highPerformanceComparison, expectedNumBatches);
  }

  public BaselineQueryTestBuilder sqlBaselineQuery(String query, String... replacements) {
    return sqlBaselineQuery(String.format(query, (Object[]) replacements));
  }

  // provide a path to a file containing a SQL query to use as a baseline
  public BaselineQueryTestBuilder sqlBaselineQueryFromFile(String baselineQueryFilename) throws IOException {
    String baselineQuery = BaseTestQuery.getFile(baselineQueryFilename);
    return new BaselineQueryTestBuilder(baselineQuery, UserBitShared.QueryType.SQL, services, query, queryType, ordered, approximateEquality,
        baselineTypeMap, baselineOptionSettingQueries, testOptionSettingQueries, highPerformanceComparison, expectedNumBatches);
  }

  // as physical plans are verbose, this is the only option provided for specifying them, we should enforce
  // that physical plans, or any large JSON strings do not live in the Java source as literals
  public BaselineQueryTestBuilder physicalPlanBaselineQueryFromFile(String baselinePhysicalPlanPath) throws IOException {
    String baselineQuery = BaseTestQuery.getFile(baselinePhysicalPlanPath);
    return new BaselineQueryTestBuilder(baselineQuery, UserBitShared.QueryType.PHYSICAL, services, query, queryType, ordered, approximateEquality,
        baselineTypeMap, baselineOptionSettingQueries, testOptionSettingQueries, highPerformanceComparison, expectedNumBatches);
  }

  public BaselineQueryTestBuilder physicalPlanBaseline(String physicalPlan) {
    return new BaselineQueryTestBuilder(physicalPlan, UserBitShared.QueryType.PHYSICAL, services, query, queryType, ordered, approximateEquality,
      baselineTypeMap, baselineOptionSettingQueries, testOptionSettingQueries, highPerformanceComparison, expectedNumBatches);
  }

  private String getDecimalPrecisionScaleInfo(TypeProtos.MajorType type) {
    String precision = "";
    switch(type.getMinorType()) {
      case VARDECIMAL:
      case DECIMAL18:
      case DECIMAL28SPARSE:
      case DECIMAL38SPARSE:
      case DECIMAL38DENSE:
      case DECIMAL28DENSE:
      case DECIMAL9:
        precision = String.format("(%d,%d)", type.getPrecision(), type.getScale());
        break;
      default:
        // do nothing empty string set above
    }
    return precision;
  }

  public class CSVTestBuilder extends TestBuilder {

    // path to the baseline file that will be inserted into the validation query
    private String baselineFilePath;
    // use to cast the baseline file columns, if not set the types
    // that come out of the test query drive interpretation of baseline
    private TypeProtos.MajorType[] baselineTypes;

    CSVTestBuilder(String baselineFile, TestServices services, Object query, UserBitShared.QueryType queryType, Boolean ordered,
                   boolean approximateEquality, Map<SchemaPath, TypeProtos.MajorType> baselineTypeMap,
                   String baselineOptionSettingQueries, String testOptionSettingQueries, boolean highPerformanceComparison,
                   int expectedNumBatches) {
      super(services, query, queryType, ordered, approximateEquality, baselineTypeMap, baselineOptionSettingQueries, testOptionSettingQueries,
          highPerformanceComparison, expectedNumBatches);
      this.baselineFilePath = baselineFile;
    }

    public CSVTestBuilder baselineTypes(TypeProtos.MajorType... baselineTypes) {
      this.baselineTypes = baselineTypes;
      this.baselineTypeMap = null;
      return this;
    }

    // convenience method to convert minor types to major types if no decimals with precisions are needed
    public CSVTestBuilder baselineTypes(TypeProtos.MinorType... baselineTypes) {
      TypeProtos.MajorType[] majorTypes = new TypeProtos.MajorType[baselineTypes.length];
      int i = 0;
      for(TypeProtos.MinorType minorType : baselineTypes) {
        majorTypes[i] = Types.required(minorType);
        i++;
      }
      this.baselineTypes = majorTypes;
      this.baselineTypeMap = null;
      return this;
    }

    @Override
    protected TestBuilder reset() {
      super.reset();
      baselineTypeMap = null;
      baselineTypes = null;
      baselineFilePath = null;
      return this;
    }

    @Override
    boolean typeInfoSet() {
      return super.typeInfoSet() || baselineTypes != null;
    }

    @Override
    String getValidationQuery() throws Exception {
      if (baselineColumns.length == 0) {
        throw new Exception("Baseline CSV files require passing column names, please call the baselineColumns() method on the test builder.");
      }

      if (baselineTypes != null) {
        assertEquals("Must pass the same number of types as column names if types are provided.", baselineTypes.length, baselineColumns.length);
      }

      String[] aliasedExpectedColumns = new String[baselineColumns.length];
      for (int i = 0; i < baselineColumns.length; i++) {
        aliasedExpectedColumns[i] = "columns[" + i + "] ";
        TypeProtos.MajorType majorType;
        if (baselineTypes != null) {
          majorType = baselineTypes[i];
        } else if (baselineTypeMap != null) {
          majorType = baselineTypeMap.get(parsePath(baselineColumns[i]));
        } else {
          throw new Exception("Type information not set for interpreting csv baseline file.");
        }
        String precision = getDecimalPrecisionScaleInfo(majorType);
        // TODO - determine if there is a better behavior here, if we do not specify a length the default behavior is
        // to cast to varchar with length 1
        // set default cast size for varchar, the cast function will take the lesser of this passed value and the
        // length of the incoming data when choosing the length for the outgoing data
        if (majorType.getMinorType() == TypeProtos.MinorType.VARCHAR ||
            majorType.getMinorType() == TypeProtos.MinorType.VARBINARY) {
          precision = "(65000)";
        }
        aliasedExpectedColumns[i] = "cast(" + aliasedExpectedColumns[i] + " as " +
            Types.getNameOfMinorType(majorType.getMinorType()) + precision +  " ) " + baselineColumns[i];
      }
      String query = "select " + Joiner.on(", ").join(aliasedExpectedColumns) + " from cp.`" + baselineFilePath + "`";
      return query;
    }

    @Override
    protected UserBitShared.QueryType getValidationQueryType() {
      return UserBitShared.QueryType.SQL;
    }
  }

  public class SchemaTestBuilder extends TestBuilder {
    private final List<Pair<SchemaPath, TypeProtos.MajorType>> expectedSchema;
    SchemaTestBuilder(TestServices services, Object query, UserBitShared.QueryType queryType,
        String baselineOptionSettingQueries, String testOptionSettingQueries, List<Pair<SchemaPath, TypeProtos.MajorType>> expectedSchema) {
      super(services, query, queryType, false, false, null, baselineOptionSettingQueries, testOptionSettingQueries, false, -1);
      expectsEmptyResultSet();
      this.expectedSchema = expectedSchema;
    }

    @Override
    public TestBuilder baselineColumns(String... columns) {
      assert false : "The column information should be captured in expected scheme, not baselineColumns";
      return this;
    }

    @Override
    public TestBuilder baselineRecords(List<Map<String, Object>> materializedRecords) {
      assert false : "Since only schema will be compared in this test, no record is expected";
      return this;
    }

    @Override
    public TestBuilder baselineValues(Object... objects) {
      assert false : "Since only schema will be compared in this test, no record is expected";
      return this;
    }

    @Override
    protected UserBitShared.QueryType getValidationQueryType() {
      return null;
    }

    @Override
    public List<Pair<SchemaPath, TypeProtos.MajorType>> getExpectedSchema() {
      return expectedSchema;
    }
  }

  public class JSONTestBuilder extends TestBuilder {

    // path to the baseline file that will be inserted into the validation query
    private final String baselineFilePath;

    JSONTestBuilder(String baselineFile, TestServices services, Object query, UserBitShared.QueryType queryType, Boolean ordered,
                    boolean approximateEquality, Map<SchemaPath, TypeProtos.MajorType> baselineTypeMap,
                    String baselineOptionSettingQueries, String testOptionSettingQueries, boolean highPerformanceComparison,
                    int expectedNumBatches) {
      super(services, query, queryType, ordered, approximateEquality, baselineTypeMap, baselineOptionSettingQueries, testOptionSettingQueries,
          highPerformanceComparison, expectedNumBatches);
      this.baselineFilePath = baselineFile;
      this.baselineColumns = new String[] {"*"};
    }

    @Override
    String getValidationQuery() {
      return "select " + Joiner.on(", ").join(baselineColumns) + " from cp.`" + baselineFilePath + "`";
    }

    @Override
    protected UserBitShared.QueryType getValidationQueryType() {
      return UserBitShared.QueryType.SQL;
    }

  }

  public class BaselineQueryTestBuilder extends TestBuilder {

    /**
     * Baseline query. Type of object depends on {@link #baselineQueryType}
     */
    private final Object baselineQuery;
    private final UserBitShared.QueryType baselineQueryType;

    BaselineQueryTestBuilder(Object baselineQuery, UserBitShared.QueryType baselineQueryType, TestServices services,
                             Object query, UserBitShared.QueryType queryType, Boolean ordered,
                             boolean approximateEquality, Map<SchemaPath, TypeProtos.MajorType> baselineTypeMap,
                             String baselineOptionSettingQueries, String testOptionSettingQueries, boolean highPerformanceComparison,
                             int expectedNumBatches) {
      super(services, query, queryType, ordered, approximateEquality, baselineTypeMap, baselineOptionSettingQueries, testOptionSettingQueries,
          highPerformanceComparison, expectedNumBatches);
      this.baselineQuery = baselineQuery;
      this.baselineQueryType = baselineQueryType;
    }

    @Override
    Object getValidationQuery() {
      return baselineQuery;
    }

    @Override
    protected UserBitShared.QueryType getValidationQueryType() {
      return baselineQueryType;
    }

    // This currently assumes that all explicit baseline queries will have fully qualified type information
    // if this changes, the baseline query can be run in a sub query with the implicit or explicit type passing
    // added on top of it, as is currently when done when reading a baseline file
    @Override
    boolean typeInfoSet() {
      return true;
    }

  }

  /**
   * Convenience method to create a {@link JsonStringArrayList list} from the given values.
   */
  public static JsonStringArrayList<Object> listOf(Object... values) {
    final JsonStringArrayList<Object> list = new JsonStringArrayList<>();
    for (Object value:values) {
      if (value instanceof CharSequence) {
        list.add(new Text(value.toString()));
      } else {
        list.add(value);
      }
    }
    return list;
  }

  /**
   * Convenience method to create a {@link JsonStringHashMap<String, Object>} map instance with the given key value sequence.
   *
   * Key value sequence consists of key - value pairs such that a key precedes its value. For instance:
   *
   * mapOf("name", "Adam", "age", 41) corresponds to {"name": "Adam", "age": 41} in JSON.
   */
  public static JsonStringHashMap<String, Object> mapOf(Object... keyValueSequence) {
    Preconditions.checkArgument(keyValueSequence.length%2==0, "Length of key value sequence must be even");
    final JsonStringHashMap<String, Object> map = new JsonStringHashMap<>();
    for (int i=0; i<keyValueSequence.length; i+=2) {
      Object value = keyValueSequence[i+1];
      if (value instanceof CharSequence) {
        value = new Text(value.toString());
      }
      map.put(String.class.cast(keyValueSequence[i]), value);
    }
    return map;
  }

  /**
   * Convenience method to create an instance of {@link JsonStringHashMap}{@code <Object, Object>} with the given key-value sequence.
   *
   * By default, any {@link String} instance will be wrapped by {@link Text} instance. To disable wrapping pass
   * {@code false} as the first object to key-value sequence.
   *
   * @param keyValueSequence sequence of key-value pairs with optional boolean
   *                         flag which disables wrapping String instances by {@link Text}.
   * @return map consisting of entries given in the key-value sequence.
   */
  public static JsonStringHashMap<Object, Object> mapOfObject(Object... keyValueSequence) {
    boolean convertStringToText = true;
    final int startIndex;
    if (keyValueSequence.length % 2 == 1) {
      convertStringToText = (boolean) keyValueSequence[0];
      startIndex = 1;
    } else {
      startIndex = 0;
    }

    final JsonStringHashMap<Object, Object> map = new JsonStringHashMap<>();
    for (int i = startIndex; i < keyValueSequence.length; i += 2) {
      Object key = keyValueSequence[i];
      if (convertStringToText && key instanceof CharSequence) {
        key = new Text(key.toString());
      }
      Object value = keyValueSequence[i + 1];
      if (value instanceof CharSequence) {
        value = new Text(value.toString());
      }
      map.put(key, value);
    }
    return map;
  }

  /**
   * Helper method for the timestamp values that depend on the local timezone
   * @param value expected timestamp value in UTC
   * @return timestamp value for the local timezone
   */
  public static Timestamp convertToLocalTimestamp(String value) {
    long UTCTimestamp = Timestamp.valueOf(value).getTime();
    return new Timestamp(DateTimeZone.getDefault().convertUTCToLocal(UTCTimestamp));
  }

  /**
   * Helper method for the timestamp values that depend on the local timezone
   * @param value expected timestamp value in UTC
   * @return LocalDateTime value for the local timezone
   */
  public static LocalDateTime convertToLocalDateTime(String value) {
    OffsetDateTime utcDateTime = LocalDateTime.parse(value, DateUtility.getDateTimeFormatter()).atOffset(ZoneOffset.UTC);
    return utcDateTime.atZoneSameInstant(ZoneOffset.systemDefault()).toLocalDateTime();
  }

}
