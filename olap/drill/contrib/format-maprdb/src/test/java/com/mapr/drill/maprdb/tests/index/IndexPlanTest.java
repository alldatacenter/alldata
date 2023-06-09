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
package com.mapr.drill.maprdb.tests.index;

import com.mapr.db.Admin;
import com.mapr.drill.maprdb.tests.MaprDBTestsSuite;
import com.mapr.drill.maprdb.tests.json.BaseJsonTest;
import com.mapr.tests.annotations.ClusterTest;
import org.apache.drill.PlanTestBase;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.apache.drill.common.config.DrillConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import java.util.Properties;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Category(ClusterTest.class)
public class IndexPlanTest extends BaseJsonTest {

  final static String PRIMARY_TABLE_NAME = "/tmp/index_test_primary";

  final static int PRIMARY_TABLE_SIZE = 10000;
  private static final String sliceTargetSmall = "alter session set `planner.slice_target` = 1";
  private static final String sliceTargetDefault = "alter session reset `planner.slice_target`";
  private static final String noIndexPlan = "alter session set `planner.enable_index_planning` = false";
  private static final String defaultHavingIndexPlan = "alter session reset `planner.enable_index_planning`";
  private static final String disableHashAgg = "alter session set `planner.enable_hashagg` = false";
  private static final String enableHashAgg =  "alter session set `planner.enable_hashagg` = true";
  private static final String lowNonCoveringSelectivityThreshold = "alter session set `planner.index.noncovering_selectivity_threshold` = 0.00001";
  private static final String defaultnonCoveringSelectivityThreshold = "alter session set `planner.index.noncovering_selectivity_threshold` = 0.025";
  private static final String incrnonCoveringSelectivityThreshold = "alter session set `planner.index.noncovering_selectivity_threshold` = 0.25";
  private static final String disableFTS = "alter session set `planner.disable_full_table_scan` = true";
  private static final String enableFTS = "alter session reset `planner.disable_full_table_scan`";
  private static final String preferIntersectPlans = "alter session set `planner.index.prefer_intersect_plans` = true";
  private static final String defaultIntersectPlans = "alter session reset `planner.index.prefer_intersect_plans`";
  private static final String lowRowKeyJoinBackIOFactor
      = "alter session set `planner.index.rowkeyjoin_cost_factor` = 0.01";
  private static final String defaultRowKeyJoinBackIOFactor
      = "alter session reset `planner.index.rowkeyjoin_cost_factor`";
  private static final String incrRowKeyJoinConvSelThreshold = "alter session set `planner.rowkeyjoin_conversion_selectivity_threshold` = 1.0";
  private static final String defaultRowKeyConvSelThreshold = "alter session reset `planner.rowkeyjoin_conversion_selectivity_threshold`";
  private static final String forceRowKeyJoinConversionUsingHashJoin = "alter session set `planner.rowkeyjoin_conversion_using_hashjoin` = true";
  private static final String defaultRowKeyJoinConversionUsingHashJoin = "alter session reset `planner.rowkeyjoin_conversion_using_hashjoin`";
  /**
   *  A sample row of this 10K table:
   ------------------+-----------------------------+--------+
   | 1012  | {"city":"pfrrs","state":"pc"}  | {"email":"KfFzKUZwNk@gmail.com","phone":"6500005471"}  |
   {"ssn":"100007423"}  | {"fname":"KfFzK","lname":"UZwNk"}  | {"age":53.0,"income":45.0}  | 1012   |
   *
   * This test suite generate random content to fill all the rows, since the random function always start from
   * the same seed for different runs, when the row count is not changed, the data in table will always be the same,
   * thus the query result could be predicted and verified.
   */

  @BeforeClass
  public static void setupTableIndexes() throws Exception {

    Properties overrideProps = new Properties();
    overrideProps.setProperty("format-maprdb.json.useNumRegionsForDistribution", "true");
    updateTestCluster(1, DrillConfig.create(overrideProps));

    MaprDBTestsSuite.setupTests();
    MaprDBTestsSuite.createPluginAndGetConf(getDrillbitContext());

    test(incrnonCoveringSelectivityThreshold);

    System.out.print("setupTableIndexes begins");
    Admin admin = MaprDBTestsSuite.getAdmin();
    if (admin != null) {
      if (admin.tableExists(PRIMARY_TABLE_NAME)) {
        admin.deleteTable(PRIMARY_TABLE_NAME);
      }
    }

    LargeTableGen gen = new LargeTableGen(MaprDBTestsSuite.getAdmin());
    /**
     * indexDef is an array of string, LargeTableGen.generateTableWithIndex will take it as parameter to generate indexes
     * for primary table.
     * indexDef[3*i] defines i-th index's indexName, NOTE: IF the name begins with "hash", it is a hash index
     * indexDef[3*i+1] indexed field,
     * and indexDef[3*i+2] defines i-th index's non-indexed fields
     */
    final String[] indexDef = //null;
        {"i_ssn", "id.ssn", "contact.phone",
            "i_state_city", "address.state,address.city", "name.fname,name.lname",//mainly for composite key test
            "i_age", "personal.age", "",
            "i_age_desc", "personal.age:desc", "",
            "i_income", "personal.income", "",
            "i_lic", "driverlicense", "reverseid",
            "i_state_city_dl", "address.state,address.city", "driverlicense",
            "i_cast_int_ssn", "$CAST(id.ssn@INT)", "contact.phone",
            "i_cast_vchar_lic", "$CAST(driverlicense@STRING)","contact.email",
            "i_state_age_phone", "address.state,personal.age,contact.phone", "name.fname",
            "i_cast_age_income_phone", "$CAST(personal.age@INT),$CAST(personal.income@INT),contact.phone", "name.fname",
            "i_age_with_fname", "personal.age", "name.fname",
            "i_rowid_cast_date_birthdate", "rowid", "$CAST(personal.birthdate@DATE)",
            "hash_i_reverseid", "reverseid", "",
            "hash_i_cast_timestamp_firstlogin", "$CAST(activity.irs.firstlogin@TIMESTAMP)", "id.ssn"
        };
    gen.generateTableWithIndex(PRIMARY_TABLE_NAME, PRIMARY_TABLE_SIZE, indexDef);
  }

  @AfterClass
  public static void cleanupTableIndexes() throws Exception {
    Admin admin = MaprDBTestsSuite.getAdmin();
    if (admin != null) {
      if (admin.tableExists(PRIMARY_TABLE_NAME)) {
   //     admin.deleteTable(PRIMARY_TABLE_NAME);
      }
    }
    test(defaultnonCoveringSelectivityThreshold);
  }

  @Test
  public void CTASTestTable() throws Exception {
    String ctasQuery = "CREATE TABLE hbase.tmp.`backup_index_test_primary` " +
        "AS SELECT * FROM hbase.`index_test_primary` as t ";
    test(ctasQuery);
    test("DROP TABLE IF EXISTS hbase.tmp.`backup_index_test_primary`");
  }

  @Test
  public void CoveringPlanWithNonIndexedField() throws Exception {

    String query = "SELECT t.`contact`.`phone` AS `phone` FROM hbase.`index_test_primary` as t " +
        " where t.id.ssn = '100007423'";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {".*JsonTableGroupScan.*tableName=.*index_test_primary.*indexName=i_ssn"},
        new String[]{"RowKeyJoin"}
    );

    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("phone").baselineValues("6500005471")
        .go();

  }

  @Test
  public void CoveringPlanWithOnlyIndexedField() throws Exception {
    String query = "SELECT t.`id`.`ssn` AS `ssn` FROM hbase.`index_test_primary` as t " +
        " where t.id.ssn = '100007423'";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {".*JsonTableGroupScan.*tableName=.*index_test_primary.*indexName=i_ssn"},
        new String[]{"RowKeyJoin"}
    );

    testBuilder()
        .optionSettingQueriesForTestQuery(defaultHavingIndexPlan)
        .sqlQuery(query)
        .ordered()
        .baselineColumns("ssn").baselineValues("100007423")
        .go();

  }

  @Test
  public void NoIndexPlanForNonIndexField() throws Exception {

    String query = "SELECT t.`id`.`ssn` AS `ssn` FROM hbase.`index_test_primary` as t " +
        " where t.contact.phone = '6500005471'";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {".*JsonTableGroupScan.*tableName=.*index_test_primary"},
        new String[]{"RowKeyJoin", "indexName="}
    );

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("ssn").baselineValues("100007423")
        .baselineColumns("ssn").baselineValues("100007632")
        .go();

  }

  @Test
  public void NonCoveringPlan() throws Exception {

    String query = "SELECT t.`name`.`fname` AS `fname` FROM hbase.`index_test_primary` as t " +
        " where t.id.ssn = '100007423'";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {"RowKeyJoin", ".*RestrictedJsonTableGroupScan.*tableName=.*index_test_primary,",
           ".*JsonTableGroupScan.*tableName=.*index_test_primary,.*indexName=i_ssn"},
        new String[]{}
    );

    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("fname").baselineValues("KfFzK")
        .go();

  }

  @Test
  public void RangeConditionIndexPlan() throws Exception {
    String query = "SELECT t.`name`.`lname` AS `lname` FROM hbase.`index_test_primary` as t " +
        " where t.personal.age > 52 AND t.name.fname='KfFzK'";
    try {
      test(defaultHavingIndexPlan + ";" + lowRowKeyJoinBackIOFactor + ";");
      PlanTestBase.testPlanMatchingPatterns(query,
              new String[]{"RowKeyJoin", ".*RestrictedJsonTableGroupScan.*tableName=.*index_test_primary,",
                      ".*JsonTableGroupScan.*tableName=.*index_test_primary,.*indexName=(i_age|i_age_with_fname)"},
              new String[]{}
      );
      testBuilder()
              .optionSettingQueriesForTestQuery(defaultHavingIndexPlan)
              .optionSettingQueriesForTestQuery(lowRowKeyJoinBackIOFactor)
              .optionSettingQueriesForBaseline(noIndexPlan)
              .unOrdered()
              .sqlQuery(query)
              .sqlBaselineQuery(query)
              .build()
              .run();

      testBuilder()
              .optionSettingQueriesForTestQuery(sliceTargetSmall)
              .optionSettingQueriesForBaseline(sliceTargetDefault)
              .unOrdered()
              .sqlQuery(query)
              .sqlBaselineQuery(query)
              .build()
              .run();
    } finally {
      test(defaultRowKeyJoinBackIOFactor);
    }
  }

  @Test
  public void CoveringWithSimpleFieldsOnly() throws Exception {

    String query = "SELECT t._id AS `tid` FROM hbase.`index_test_primary` as t " +
        " where t.driverlicense = 100007423";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {"JsonTableGroupScan.*tableName=.*index_test_primary,.*indexName=i_lic"},
        new String[]{"RowKeyJoin"}
    );

    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("tid").baselineValues("1012")
        .go();

  }

  @Test
  public void NonCoveringWithSimpleFieldsOnly() throws Exception {

    String query = "SELECT t.rowid AS `rowid` FROM hbase.`index_test_primary` as t " +
        " where t.driverlicense = 100007423";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {"RowKeyJoin(.*[\n\r])+.*" +
            "RestrictedJsonTableGroupScan.*tableName=.*index_test_primary(.*[\n\r])+.*" +
            "JsonTableGroupScan.*tableName=.*index_test_primary,.*indexName=i_lic"},
        new String[]{}
    );

    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("rowid").baselineValues("1012")
        .go();

  }

  @Test
  public void NonCoveringWithExtraConditonOnPrimary() throws Exception {

    String query = "SELECT t.`name`.`fname` AS `fname` FROM hbase.`index_test_primary` as t " +
        " where t.personal.age = 53 AND t.name.lname='UZwNk'";
    try {
      test(defaultHavingIndexPlan + ";" + lowRowKeyJoinBackIOFactor + ";");
      PlanTestBase.testPlanMatchingPatterns(query,
              new String[]{"RowKeyJoin", ".*RestrictedJsonTableGroupScan",
                      ".*JsonTableGroupScan.*indexName=i_age",},
              new String[]{}
      );

      testBuilder()
              .sqlQuery(query)
              .ordered()
              .baselineColumns("fname").baselineValues("KfFzK")
              .go();
    } finally {
      test(defaultRowKeyJoinBackIOFactor);
    }

  }

  @Test
  public void Intersect2indexesPlan() throws Exception {

    String query = "SELECT t.`name`.`lname` AS `lname` FROM hbase.`index_test_primary` as t " +
        " where t.personal.age = 53 AND t.personal.income=45";
    try {
      test(defaultHavingIndexPlan);
      test(preferIntersectPlans + ";" + disableFTS);
      PlanTestBase.testPlanMatchingPatterns(query,
              new String[]{"RowKeyJoin(.*[\n\r])+.*RestrictedJsonTableGroupScan(.*[\n\r])+.*HashJoin(.*[\n\r])+.*JsonTableGroupScan.*indexName=(i_age|i_income)(.*[\n\r])+.*JsonTableGroupScan.*indexName=(i_age|i_income)"},
              new String[]{}
      );

      testBuilder()
              .sqlQuery(query)
              .unOrdered()
              .baselineColumns("lname").baselineValues("UZwNk")
              .baselineColumns("lname").baselineValues("foNwtze")
              .baselineColumns("lname").baselineValues("qGZVfY")
              .go();
      testBuilder()
              .optionSettingQueriesForTestQuery(sliceTargetSmall)
              .optionSettingQueriesForBaseline(sliceTargetDefault)
              .unOrdered()
              .sqlQuery(query)
              .sqlBaselineQuery(query)
              .build()
              .run();
    } finally {
      test(defaultIntersectPlans + ";" + enableFTS);
    }

  }

  @Test
  public void CompositeIndexNonCoveringPlan() throws Exception {

    String query = "SELECT t.`id`.`ssn` AS `ssn` FROM hbase.`index_test_primary` as t " +
        " where t.address.state = 'pc' AND t.address.city='pfrrs'";
    try {
      test(defaultHavingIndexPlan + ";" + lowRowKeyJoinBackIOFactor + ";");

      //either i_state_city or i_state_age_phone will be picked depends on cost model, both is fine for testing composite index nonCovering plan
      PlanTestBase.testPlanMatchingPatterns(query,
              new String[]{"RowKeyJoin(.*[\n\r])+.*RestrictedJsonTableGroupScan(.*[\n\r])+.*JsonTableGroupScan.*indexName=i_state_"},
              new String[]{}
      );

      testBuilder()
              .sqlQuery(query)
              .unOrdered()
              .baselineColumns("ssn").baselineValues("100007423")
              .baselineColumns("ssn").baselineValues("100008861")
              .go();

      testBuilder()
              .optionSettingQueriesForTestQuery(sliceTargetSmall)
              .optionSettingQueriesForBaseline(sliceTargetDefault)
              .unOrdered()
              .sqlQuery(query)
              .sqlBaselineQuery(query)
              .build()
              .run();
    } finally {
      test(defaultRowKeyJoinBackIOFactor);
    }

  }

  @Test//filter cover indexed, included and not in index at all filter
  public void CompositeIndexNonCoveringFilterWithAllFieldsPlan() throws Exception {

    String query = "SELECT t.`id`.`ssn` AS `ssn` FROM hbase.`index_test_primary` as t " +
        " where t.address.state = 'pc' AND t.address.city='pfrrs' AND t.driverlicense IN (100007423, 100007424)";
    test(defaultHavingIndexPlan+";"+lowRowKeyJoinBackIOFactor+";");
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {"RowKeyJoin(.*[\n\r])+.*RestrictedJsonTableGroupScan.*condition=.*state.*city.*driverlicense.*or.*driverlicense.*(.*[\n\r])+.*JsonTableGroupScan.*indexName="},
        new String[]{}
    );

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("ssn").baselineValues("100007423")
        .go();

    testBuilder()
        .optionSettingQueriesForTestQuery(sliceTargetSmall)
        .optionSettingQueriesForBaseline(sliceTargetDefault)
        .unOrdered()
        .sqlQuery(query)
        .sqlBaselineQuery(query)
        .build()
        .run();

  }

  @Test
  public void CompositeIndexCoveringPlan() throws Exception {

    String query = "SELECT t.`address`.`city` AS `city` FROM hbase.`index_test_primary` as t " +
        " where t.address.state = 'pc' AND t.address.city='pfrrs'";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {".*JsonTableGroupScan.*indexName=i_state_city"},
        new String[]{"RowKeyJoin", "Filter"}
    );

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("city").baselineValues("pfrrs")
        .baselineColumns("city").baselineValues("pfrrs")
        .go();

    testBuilder()
        .optionSettingQueriesForTestQuery(sliceTargetSmall)
        .optionSettingQueriesForBaseline(sliceTargetDefault)
        .unOrdered()
        .sqlQuery(query)
        .sqlBaselineQuery(query)
        .build()
        .run();

  }

  @Test
  public void TestNonCoveringRangePartition_1() throws Exception {

    String query = "SELECT t.`name`.`lname` AS `lname` FROM hbase.`index_test_primary` as t " +
        " where t.personal.age = 53";
    String[] expectedPlan = new String[] {"RowKeyJoin(.*[\n\r])+.*" +
        "RestrictedJsonTableGroupScan.*tableName=.*index_test_primary(.*[\n\r])+.*" +
        "RangePartitionExchange(.*[\n\r])+.*" +
    "JsonTableGroupScan.*tableName=.*index_test_primary,.*indexName=(i_age|i_age_with_fname)"};
    test(defaultHavingIndexPlan+";"+sliceTargetSmall+";");
    PlanTestBase.testPlanMatchingPatterns(query,
        expectedPlan, new String[]{});

    try {
      testBuilder()
          .optionSettingQueriesForTestQuery(defaultHavingIndexPlan)
          .optionSettingQueriesForBaseline(noIndexPlan)
          .unOrdered()
          .sqlQuery(query)
          .sqlBaselineQuery(query)
          .build()
          .run();
    } finally {
      test(defaultHavingIndexPlan);
      test(sliceTargetDefault);
    }

  }

  @Test
  public void TestCastVarCharCoveringPlan() throws Exception {
    // length 255 is to exact match the casted indexed field's length
    String query = "SELECT t._id as tid, cast(t.driverlicense as varchar(255)) as driverlicense FROM hbase.`index_test_primary` as t " +
        " where cast(t.driverlicense as varchar(255))='100007423'";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {".*JsonTableGroupScan.*tableName=.*index_test_primary.*indexName=i_cast_vchar_lic"},
        new String[]{"RowKeyJoin"}
    );

    testBuilder()
        .optionSettingQueriesForTestQuery(defaultHavingIndexPlan)
        .sqlQuery(query)
        .ordered()
        .baselineColumns("tid", "driverlicense").baselineValues("1012", "100007423")
        .go();

  }

  @Test
  public void TestCastINTCoveringPlan() throws Exception {
    String query = "SELECT t._id as tid, CAST(t.id.ssn as INT) as ssn, t.contact.phone AS `phone` FROM hbase.`index_test_primary` as t " +
        " where CAST(t.id.ssn as INT) = 100007423";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {".*JsonTableGroupScan.*tableName=.*index_test_primary.*indexName=i_cast_int_ssn"},
        new String[]{"RowKeyJoin"}
    );

    testBuilder()
        .optionSettingQueriesForTestQuery(defaultHavingIndexPlan)
        .sqlQuery(query)
        .ordered()
        .baselineColumns("tid", "ssn", "phone").baselineValues("1012", 100007423, "6500005471")
        .go();

  }

  @Test
  public void TestCastNonCoveringPlan() throws Exception {
    String query = "SELECT t.id.ssn AS `ssn` FROM hbase.`index_test_primary` as t " +
        " where CAST(t.id.ssn as INT) = 100007423";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {"RowKeyJoin(.*[\n\r])+.*RestrictedJsonTableGroupScan(.*[\n\r])+.*JsonTableGroupScan.*indexName=i_cast_int_ssn"},
        new String[]{}
    );

    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("ssn").baselineValues("100007423")
        .go();

  }

  @Test
  public void TestCastVarchar_ConvertToRangePlan() throws Exception {
    String query = "SELECT t.id.ssn AS `ssn` FROM hbase.`index_test_primary` as t " +
        " where CAST(driverlicense as VARCHAR(10)) = '100007423'";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {"RowKeyJoin(.*[\n\r])+.*RestrictedJsonTableGroupScan(.*[\n\r])+.*JsonTableGroupScan.*MATCHES \"\\^.*100007423.*E.*\\$\".*indexName=i_cast_vchar_lic"},
        new String[]{}
    );

    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("ssn").baselineValues("100007423")
        .go();

  }

  @Test // cast expression in filter is not indexed, but the same field casted to different type was indexed (CAST id.ssn as INT)
  public void TestCastNoIndexPlan() throws Exception {
    String query = "select t.id.ssn from hbase.`index_test_primary` t where cast(t.id.ssn as varchar(10)) = '100007423'";

    PlanTestBase.testPlanMatchingPatterns(query,
        new String[]{},
        new String[]{"indexName"}
    );

  }

  @Test
  public void TestLongerCastVarCharNoIndex() throws Exception {
    // length 256 is to exact match the casted indexed field's length
    String query = "SELECT t._id as tid, cast(t.driverlicense as varchar(500)) as driverlicense FROM hbase.`index_test_primary` as t " +
        " where cast(t.driverlicense as varchar(500))='100007423'";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {},
        new String[]{"RowKeyJoin", "indexName="}
    );

  }

  @Test
  public void TestCoveringPlanSortRemoved() throws Exception {
    String query = "SELECT t.`contact`.`phone` as phone FROM hbase.`index_test_primary` as t " +
        " where t.id.ssn <'100000003' order by t.id.ssn";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {".*JsonTableGroupScan.*tableName=.*index_test_primary.*indexName=i_ssn"},
        new String[]{"Sort"}
    );

    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("phone").baselineValues("6500008069")
        .baselineColumns("phone").baselineValues("6500001411")
        .baselineColumns("phone").baselineValues("6500001595")
        .go();
  }

  @Test
  public void TestCoveringPlanSortNotRemoved() throws Exception {
    String query = "SELECT t.`contact`.`phone` as phone FROM hbase.`index_test_primary` as t " +
        " where t.id.ssn <'100000003' order by t.contact.phone";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {"Sort", ".*JsonTableGroupScan.*tableName=.*index_test_primary.*indexName=i_ssn"},
        new String[]{"RowkeyJoin"}
    );

    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("phone").baselineValues("6500001411")
        .baselineColumns("phone").baselineValues("6500001595")
        .baselineColumns("phone").baselineValues("6500008069")
        .go();
  }

  @Test
  public void TestCoveringPlanSortRemovedWithSimpleFields() throws Exception {
    String query = "SELECT t.driverlicense as l FROM hbase.`index_test_primary` as t " +
        " where t.driverlicense < 100000003 order by t.driverlicense";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {".*JsonTableGroupScan.*tableName=.*index_test_primary.*indexName=i_lic"},
        new String[]{"Sort"}
    );

    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("l").baselineValues(100000000l)
        .baselineColumns("l").baselineValues(100000001l)
        .baselineColumns("l").baselineValues(100000002l)
        .go();
  }

  @Test
  public void TestNonCoveringPlanSortRemoved() throws Exception {
    String query = "SELECT t.contact.phone as phone FROM hbase.`index_test_primary` as t " +
        " where t.driverlicense < 100000003 order by t.driverlicense";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {"RowKeyJoin(.*[\n\r])+.*RestrictedJsonTableGroupScan(.*[\n\r])+.*JsonTableGroupScan.*indexName=i_lic"},
        new String[]{"Sort"}
    );
    String query2 = "SELECT t.name.fname as fname FROM hbase.`index_test_primary` as t " +
        " where t.id.ssn < '100000003' order by t.id.ssn";
    PlanTestBase.testPlanMatchingPatterns(query2,
        new String[] {"RowKeyJoin(.*[\n\r])+.*RestrictedJsonTableGroupScan(.*[\n\r])+.*JsonTableGroupScan.*indexName="},
        new String[]{"Sort"}
    );

    // simple field, driverlicense
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("phone").baselineValues("6500008069")
        .baselineColumns("phone").baselineValues("6500001411")
        .baselineColumns("phone").baselineValues("6500001595")
        .go();

    // query on field of item expression(having capProject), non-simple field t.id.ssn
    testBuilder()
        .sqlQuery(query2)
        .ordered()
        .baselineColumns("fname").baselineValues("VcFahj")
        .baselineColumns("fname").baselineValues("WbKVK")
        .baselineColumns("fname").baselineValues("vSAEsyFN")
        .go();

    test(sliceTargetSmall);
    try {
      PlanTestBase.testPlanMatchingPatterns(query2,
          new String[]{"SingleMergeExchange(.*[\n\r])+.*"
              + "RowKeyJoin(.*[\n\r])+.*RestrictedJsonTableGroupScan(.*[\n\r])+.*JsonTableGroupScan.*indexName=i_ssn"},
          new String[]{"Sort"}
      );
    } finally {
      test(sliceTargetDefault);
    }
  }

  // test cases are from TestNonCoveringPlanSortRemoved. Sort was removed when force_sort_noncovering was default(false)
  @Test
  public void TestNonCoveringPlanWithNoRemoveSortOption() throws Exception {
    try {
      test("alter session set `planner.index.force_sort_noncovering`=true");
      test(defaultHavingIndexPlan);

      String query = "SELECT t.contact.phone as phone FROM hbase.`index_test_primary` as t " +
          " where t.driverlicense < 100000003 order by t.driverlicense";
      PlanTestBase.testPlanMatchingPatterns(query,
          new String[]{"Sort", "RowKeyJoin(.*[\n\r])+.*RestrictedJsonTableGroupScan(.*[\n\r])+.*JsonTableGroupScan.*indexName=i_lic"},
          new String[]{}
      );

      String query2 = "SELECT t.name.fname as fname FROM hbase.`index_test_primary` as t " +
          " where t.id.ssn < '100000003' order by t.id.ssn";
      PlanTestBase.testPlanMatchingPatterns(query2,
          new String[]{"Sort", "RowKeyJoin(.*[\n\r])+.*RestrictedJsonTableGroupScan(.*[\n\r])+.*JsonTableGroupScan.*indexName="},
          new String[]{}
      );

      // simple field, driverlicense
      testBuilder()
          .sqlQuery(query)
          .ordered()
          .baselineColumns("phone").baselineValues("6500008069")
          .baselineColumns("phone").baselineValues("6500001411")
          .baselineColumns("phone").baselineValues("6500001595")
          .go();

      // query on field of item expression(having capProject), non-simple field t.id.ssn
      testBuilder()
          .sqlQuery(query2)
          .ordered()
          .baselineColumns("fname").baselineValues("VcFahj")
          .baselineColumns("fname").baselineValues("WbKVK")
          .baselineColumns("fname").baselineValues("vSAEsyFN")
          .go();

      test(sliceTargetSmall);
      try {
        PlanTestBase.testPlanMatchingPatterns(query2,
            new String[]{"Sort", "SingleMergeExchange(.*[\n\r])+.*"
                + "RowKeyJoin(.*[\n\r])+.*RestrictedJsonTableGroupScan(.*[\n\r])+.*JsonTableGroupScan.*indexName=i_ssn"},
            new String[]{}
        );
      } finally {
        test(sliceTargetDefault);
      }
    }
    finally {
      test("alter session reset `planner.index.force_sort_noncovering`");
    }
  }

  @Test  // 2 table join, each table has local predicate on top-level column
  public void TestCoveringPlanJoin_1() throws Exception {
    String query = "SELECT count(*) as cnt FROM hbase.`index_test_primary` as t1 " +
        " inner join hbase.`index_test_primary` as t2 on t1.driverlicense = t2.driverlicense " +
        " where t1.driverlicense < 100000003 and t2.driverlicense < 100000003";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {".*JsonTableGroupScan.*tableName=.*index_test_primary.*indexName=",
                      ".*JsonTableGroupScan.*tableName=.*index_test_primary.*indexName="},
        new String[]{}
    );

    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("cnt").baselineValues(3L)
        .go();
  }

  @Test  // 2 table join, each table has local predicate on nested column
  public void TestCoveringPlanJoin_2() throws Exception {
    String query = "SELECT count(*) as cnt FROM hbase.`index_test_primary` as t1 " +
        " inner join hbase.`index_test_primary` as t2 on t1.contact.phone = t2.contact.phone " +
        " where t1.id.ssn < '100000003' and t2.id.ssn < '100000003' ";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {".*JsonTableGroupScan.*tableName=.*index_test_primary.*indexName=",
                      ".*JsonTableGroupScan.*tableName=.*index_test_primary.*indexName="},
        new String[]{}
    );

    testBuilder()
       .sqlQuery(query)
       .ordered()
       .baselineColumns("cnt").baselineValues(3L)
       .go();
  }

  @Test  // leading prefix of index has Equality conditions and ORDER BY last column; Sort SHOULD be dropped
  public void TestCoveringPlanSortPrefix_1() throws Exception {
    String query = "SELECT t.contact.phone FROM hbase.`index_test_primary` as t " +
        " where t.address.state = 'wo' and t.personal.age = 35 and t.contact.phone < '6500003000' order by t.contact.phone";
    test(defaultHavingIndexPlan);

    // we should glue to index i_state_age_phone to make sure we are testing the targeted prefix construction code path
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {".*JsonTableGroupScan.*tableName=.*index_test_primary.*indexName=i_state_age_phone"},
        new String[]{"Sort"}
    );

    // compare the results of index plan with the no-index plan
    testBuilder()
      .optionSettingQueriesForTestQuery(defaultHavingIndexPlan)
      .optionSettingQueriesForBaseline(noIndexPlan)
      .unOrdered()
      .sqlQuery(query)
      .sqlBaselineQuery(query)
      .build()
      .run();
  }

  @Test  // leading prefix of index has Non-Equality conditions and ORDER BY last column; Sort SHOULD NOT be dropped
  public void TestCoveringPlanSortPrefix_2() throws Exception {
    String query = "SELECT t.contact.phone FROM hbase.`index_test_primary` as t " +
        " where t.address.state = 'wo' and t.personal.age < 35 and t.contact.phone < '6500003000' order by t.contact.phone";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {"Sort", ".*JsonTableGroupScan.*tableName=.*index_test_primary.*indexName=i_state_age_phone"},
        new String[]{}
    );

    // compare the results of index plan with the no-index plan
    testBuilder()
      .optionSettingQueriesForTestQuery(defaultHavingIndexPlan)
      .optionSettingQueriesForBaseline(noIndexPlan)
      .unOrdered()
      .sqlQuery(query)
      .sqlBaselineQuery(query)
      .build()
      .run();
  }

  @Test  // ORDER BY last two columns not in the indexed order; Sort SHOULD NOT be dropped
  public void TestCoveringPlanSortPrefix_3() throws Exception {
    String query = "SELECT CAST(t.personal.age as VARCHAR) as age, t.contact.phone FROM hbase.`index_test_primary` as t " +
        " where t.address.state = 'wo' and t.personal.age < 35 and t.contact.phone < '6500003000' order by t.contact.phone, t.personal.age";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {"Sort", ".*JsonTableGroupScan.*tableName=.*index_test_primary.*indexName=i_state_age_phone"},
        new String[]{}
    );

    // compare the results of index plan with the no-index plan
    testBuilder()
        .optionSettingQueriesForTestQuery(defaultHavingIndexPlan)
        .optionSettingQueriesForBaseline(noIndexPlan)
        .unOrdered()
        .sqlQuery(query)
        .sqlBaselineQuery(query)
        .build()
        .run();
  }

  @Test  // last two index fields in non-Equality conditions, ORDER BY last two fields; Sort SHOULD be dropped
  public void TestCoveringPlanSortPrefix_4() throws Exception {
    String query = "SELECT t._id as tid, t.contact.phone, CAST(t.personal.age as VARCHAR) as age FROM hbase.`index_test_primary` as t " +
        " where t.address.state = 'wo' and t.personal.age < 35 and t.contact.phone < '6500003000' order by t.personal.age, t.contact.phone";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {".*JsonTableGroupScan.*tableName=.*index_test_primary.*indexName=i_state_age_phone"},
        new String[]{"Sort"}
    );

    // compare the results of index plan with the no-index plan
    testBuilder()
        .optionSettingQueriesForTestQuery(defaultHavingIndexPlan)
        .optionSettingQueriesForBaseline(noIndexPlan)
        .unOrdered()
        .sqlQuery(query)
        .sqlBaselineQuery(query)
        .build()
        .run();
  }

  @Test  // index field in two or more equality conditions, it is not leading prefix, Sort SHOULD NOT be dropped
  public void TestCoveringPlanSortPrefix_5() throws Exception {
    String query = "SELECT t._id as tid, t.contact.phone, CAST(t.personal.age as VARCHAR) as age FROM hbase.`index_test_primary` as t " +
        " where t.address.state = 'wo' and t.personal.age IN (31, 32, 33, 34) and t.contact.phone < '6500003000' order by t.contact.phone";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {"Sort", ".*JsonTableGroupScan.*tableName=.*index_test_primary.*indexName=i_state_age_phone"},
        new String[]{}
    );

    // compare the results of index plan with the no-index plan
    testBuilder()
        .optionSettingQueriesForTestQuery(defaultHavingIndexPlan)
        .optionSettingQueriesForBaseline(noIndexPlan)
        .unOrdered()
        .sqlQuery(query)
        .sqlBaselineQuery(query)
        .build()
        .run();
  }

  @Test  // last two index fields in non-Equality conditions, ORDER BY last two fields NULLS FIRST; Sort SHOULD NOT be dropped
  public void TestCoveringPlanSortPrefix_6() throws Exception {
    String query = "SELECT t._id as tid, t.contact.phone, CAST(t.personal.age as VARCHAR) as age FROM hbase.`index_test_primary` as t " +
        " where t.address.state = 'wo' and t.personal.age < 35 and t.contact.phone < '6500003000' order by t.personal.age, t.contact.phone NULLS FIRST";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {"Sort", ".*JsonTableGroupScan.*tableName=.*index_test_primary.*indexName=i_state_age_phone"},
        new String[]{}
    );

    // compare the results of index plan with the no-index plan
    testBuilder()
        .optionSettingQueriesForTestQuery(defaultHavingIndexPlan)
        .optionSettingQueriesForBaseline(noIndexPlan)
        .unOrdered()
        .sqlQuery(query)
        .sqlBaselineQuery(query)
        .build()
        .run();
  }

  @Test  // last two index fields in non-Equality conditions, ORDER BY last two fields NULLS LAST; Sort SHOULD be dropped
  public void TestCoveringPlanSortPrefix_7() throws Exception {
    String query = "SELECT t._id as tid, t.contact.phone, CAST(t.personal.age as VARCHAR) as age FROM hbase.`index_test_primary` as t " +
        " where t.address.state = 'wo' and t.personal.age < 35 and t.contact.phone < '6500003000' order by t.personal.age, t.contact.phone NULLS LAST";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {".*JsonTableGroupScan.*tableName=.*index_test_primary.*indexName=i_state_age_phone"},
        new String[]{"Sort"}
    );

    // compare the results of index plan with the no-index plan
    testBuilder()
        .optionSettingQueriesForTestQuery(defaultHavingIndexPlan)
        .optionSettingQueriesForBaseline(noIndexPlan)
        .unOrdered()
        .sqlQuery(query)
        .sqlBaselineQuery(query)
        .build()
        .run();
  }

  @Test
  public void orderByCastCoveringPlan() throws Exception {
    String query = "SELECT t.contact.phone as phone FROM hbase.`index_test_primary` as t " +
        " where CAST(t.id.ssn as INT) < 100000003 order by CAST(t.id.ssn as INT)";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {".*JsonTableGroupScan.*tableName=.*index_test_primary.*indexName="},
        new String[]{"Sort"}
    );

    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("phone").baselineValues("6500008069")
        .baselineColumns("phone").baselineValues("6500001411")
        .baselineColumns("phone").baselineValues("6500001595")
        .go();
  }

  @Test // non-covering plan. sort by the only indexed field, sort SHOULD be removed
  public void orderByNonCoveringPlan() throws Exception {
    String query = "SELECT t.name.lname as lname FROM hbase.`index_test_primary` as t " +
        " where t.id.ssn < '100000003' order by t.id.ssn";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {"RowKeyJoin(.*[\n\r])+.*RestrictedJsonTableGroupScan(.*[\n\r])+.*JsonTableGroupScan.*indexName="},
        new String[]{"Sort"}
    );

    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("lname").baselineValues("iuMG")
        .baselineColumns("lname").baselineValues("KpFq")
        .baselineColumns("lname").baselineValues("bkkAvz")
        .go();
  }

  @Test // non-covering plan. order by cast indexed field, sort SHOULD be removed
  public void orderByCastNonCoveringPlan() throws Exception {
    String query = "SELECT t.name.lname as lname FROM hbase.`index_test_primary` as t " +
        " where CAST(t.id.ssn as INT) < 100000003 order by CAST(t.id.ssn as INT)";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {"RowKeyJoin(.*[\n\r])+.*RestrictedJsonTableGroupScan(.*[\n\r])+.*JsonTableGroupScan.*indexName="},
        new String[]{"Sort"}
    );

    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("lname").baselineValues("iuMG")
        .baselineColumns("lname").baselineValues("KpFq")
        .baselineColumns("lname").baselineValues("bkkAvz")
        .go();
  }


  @Ignore // in statsCache, condition state+city has rowcount 1250, but state only has 1000. so it is picking i_state_age_phone
  @Test // non-covering, order by non leading field, and leading fields are not in equality condition, Sort SHOULD NOT be removed
  public void NonCoveringPlan_SortPrefix_1() throws Exception {

    String query = "SELECT t.`id`.`ssn` AS `ssn` FROM hbase.`index_test_primary` as t " +
        " where t.address.state > 'pc' AND t.address.city>'pfrrr' AND t.address.city<'pfrrt' order by t.adddress.city";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {"Sort",
            "RowKeyJoin(.*[\n\r])+.*RestrictedJsonTableGroupScan(.*[\n\r])+.*JsonTableGroupScan.*indexName=i_state_city"},
        new String[]{}
    );

  }

  @Test // non-covering, order by non leading field, and leading fields are in equality condition, Sort SHOULD be removed
  public void NonCoveringPlan_SortPrefix_2() throws Exception {

    String query = "SELECT t.`id`.`ssn` AS `ssn` FROM hbase.`index_test_primary` as t " +
        " where t.address.state = 'pc' AND t.address.city>'pfrrr' AND t.address.city<'pfrrt' order by t.address.city";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {
            "RowKeyJoin(.*[\n\r])+.*RestrictedJsonTableGroupScan(.*[\n\r])+.*JsonTableGroupScan.*indexName=i_state_city"},
        new String[]{"Sort"}
    );

  }

  @Ignore ("Should be modified to get an index plan; not very useful since most covering plan filters get pushed")
  @Test // Correct projection and results when filter on non-indexed column in covering plan.
  public void nonIndexedColumnFilterCoveringPlan() throws Exception {
    String query = "SELECT t.name.fname as fname FROM hbase.`index_test_primary` as t " +
        " where t.personal.age > 68 and t.name.fname IN ('CnGobfR', 'THOHP')";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {".*Filter.*CnGobfR.*THOHP.*",
            ".*JsonTableGroupScan.*tableName=.*index_test_primary.*indexName="},
        new String[] {".*Filter.*ITEM*CnGobfR.*THOHP.*"});

    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("fname").baselineValues("CnGobfR")
        .baselineColumns("fname").baselineValues("THOHP")
        .baselineColumns("fname").baselineValues("CnGobfR")
        .go();
  }

  @Test
  @Ignore
  public void orderByLimitNonCoveringPlan() throws Exception {
    String query = "SELECT t.name.lname as lname FROM hbase.`index_test_primary` as t " +
        " where t.id.ssn < '100000003' order by t.id.ssn limit 2";
    try {
      test(defaultHavingIndexPlan);
      test(sliceTargetSmall);
      PlanTestBase.testPlanMatchingPatterns(query,
          new String[]{"Limit(.*[\n\r])+.*SingleMergeExchange(.*[\n\r])+.*Limit(.*[\n\r])+.*indexName="},
          new String[]{"Sort"}
      );

      testBuilder()
          .sqlQuery(query)
          .ordered()
          .baselineColumns("lname").baselineValues("iuMG")
          .baselineColumns("lname").baselineValues("KpFq")
          .go();
    } finally {
      test(sliceTargetDefault);
    }
  }

  @Test
  public void orderByLimitCoveringPlan() throws Exception {
    String query = "SELECT t.contact.phone as phone FROM hbase.`index_test_primary` as t " +
        " where t.id.ssn < '100000003' order by t.id.ssn limit 2";
    test(defaultHavingIndexPlan);

    //when index table has only one tablet, the SingleMergeExchange in the middle of two Limits will be removed.
    //The lower limit gets pushed into the scan
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {"Limit(.*[\n\r])+.*indexName=.*limit=2"},
        new String[]{"Sort"}
    );

    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("phone").baselineValues("6500008069")
        .baselineColumns("phone").baselineValues("6500001411")
        .go();
  }

  @Test
  public void pickAnyIndexWithFTSDisabledPlan() throws Exception {
    String lowCoveringSel = "alter session set `planner.index.covering_selectivity_threshold` = 0.025";
    String defaultCoveringSel = "alter session reset `planner.index.covering_selectivity_threshold`";
    String query = "SELECT t.`contact`.`phone` AS `phone` FROM hbase.`index_test_primary` as t " +
        " where t.id.ssn = '100007423'";
    try {
      test(defaultHavingIndexPlan + ";" + lowCoveringSel + ";");
      PlanTestBase.testPlanMatchingPatterns(query,
          new String[]{".*JsonTableGroupScan.*tableName=.*index_test_primary"},
          new String[]{".*indexName=i_ssn"}
      );
      // Must not throw CANNOTPLANEXCEPTION
      test(defaultHavingIndexPlan + ";" + lowCoveringSel + ";" + disableFTS + ";");
      PlanTestBase.testPlanMatchingPatterns(query,
          new String[]{".*JsonTableGroupScan.*tableName=.*index_test_primary.*indexName=i_ssn"},
          new String[]{"RowKeyJoin"}
      );
    } finally {
      test(defaultCoveringSel+";"+enableFTS+";");
    }
  }

  @Test
  public void testCaseSensitive() throws Exception {
    String query = "SELECT t.contact.phone as phone FROM hbase.`index_test_primary` as t " +
        " where t.id.SSN = '100000003' ";
    test(defaultHavingIndexPlan);

    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {""},
        new String[]{"indexName"}
    );

  }

  @Test
  public void testCaseSensitiveIncludedField() throws Exception {

    String query = "SELECT t.`CONTACT`.`phone` AS `phone` FROM hbase.`index_test_primary` as t " +
        " where t.id.ssn = '100007423'";
    test(defaultHavingIndexPlan);

    PlanTestBase.testPlanMatchingPatterns(query,
        new String[]{"RowKeyJoin",
            ".*JsonTableGroupScan.*tableName=.*index_test_primary.*indexName=i_ssn"},
        new String[]{}
    );
  }


  @Test
  public void testHashIndexNoRemovingSort() throws Exception {
    String query = "SELECT t.`contact`.`phone` as phone FROM hbase.`index_test_primary` as t " +
        " where t.reverseid <'10' order by t.reverseid";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {"Sort", "indexName=hash_i_reverseid", "RowKeyJoin"},
        new String[]{}
    );
  }

  @Ignore
  @Test
  public void testCastTimestampPlan() throws Exception {
    String query = "SELECT  t.id.ssn as ssn FROM hbase.`index_test_primary` as t " +
        " where cast(t.activity.irs.firstlogin as timestamp)=to_timestamp('2013-02-04 22:34:38.0', 'YYYY-MM-dd HH:mm:ss.S')";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {"indexName=hash_i_cast_timestamp_firstlogin"},
        new String[]{"RowKeyJoin"}
    );
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("ssn").baselineValues("100007423")
        .go();

  }

  @Test
  public void testNotConditionNoIndexPlan() throws Exception {
    String query = "SELECT t.`id`.`ssn` AS `ssn` FROM hbase.`index_test_primary` as t " +
        " where NOT t.id.ssn = '100007423'";

    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {},
        new String[]{"indexName="}
    );


    String notInQuery = "SELECT t.`id`.`ssn` AS `ssn` FROM hbase.`index_test_primary` as t " +
        " where t.id.ssn NOT IN ('100007423', '100007424')";
    PlanTestBase.testPlanMatchingPatterns(notInQuery,
        new String[] {},
        new String[]{"indexName="}
    );

    String notLikeQuery = "SELECT t.`id`.`ssn` AS `ssn` FROM hbase.`index_test_primary` as t " +
        " where t.id.ssn NOT LIKE '100007423'";
    PlanTestBase.testPlanMatchingPatterns(notLikeQuery,
        new String[] {},
        new String[]{"indexName="}
    );

  }

  @Test
  public void testNoFilterOrderByCoveringPlan() throws Exception {
    String query = "SELECT t.`id`.`ssn` AS `ssn`, t.contact.phone as phone FROM hbase.`index_test_primary` as t " +
        "order by t.id.ssn limit 2";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {"indexName=i_ssn"},
        new String[]{"Sort", "TopN", "RowKeyJoin"}
    );
    testBuilder()
        .ordered()
        .sqlQuery(query)
        .baselineColumns("ssn", "phone").baselineValues("100000000", "6500008069")
        .baselineColumns("ssn", "phone").baselineValues("100000001", "6500001411")
        .build()
        .run();
  }

  @Test
  public void testNoFilterAndLimitOrderByCoveringPlan() throws Exception {
    String query = "SELECT t.`id`.`ssn` AS `ssn`, t.contact.phone as phone FROM hbase.`index_test_primary` as t " +
            "order by t.id.ssn";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
            new String[] {"Sort"},
            new String[]{"indexName=*", "RowKeyJoin", "TopN"}
    );
  }

  @Test
  public void testNoFilterOrderByCast() throws Exception {
    String query = "SELECT CAST(t.id.ssn as INT) AS `ssn`, t.contact.phone as phone FROM hbase.`index_test_primary` as t " +
        "order by CAST(t.id.ssn as INT) limit 2";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {"indexName=i_cast_int_ssn"},
        new String[]{"TopN", "Sort", "RowKeyJoin"}
    );
    testBuilder()
        .ordered()
        .sqlQuery(query)
        .baselineColumns("ssn", "phone").baselineValues(100000000, "6500008069")
        .baselineColumns("ssn", "phone").baselineValues(100000001, "6500001411")
        .build()
        .run();
  }

  @Test
  public void testNoFilterAndLimitOrderByCast() throws Exception {
    String query = "SELECT CAST(t.id.ssn as INT) AS `ssn`, t.contact.phone as phone FROM hbase.`index_test_primary` as t " +
            "order by CAST(t.id.ssn as INT)";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
            new String[] { "Sort"},
            new String[]{"indexName=*","TopN", "RowKeyJoin"}
    );
  }

  @Test
  public void testNoFilterOrderByHashIndex() throws Exception {
    String query = "SELECT cast(t.activity.irs.firstlogin as timestamp) AS `firstlogin`, t.id.ssn as ssn FROM hbase.`index_test_primary` as t " +
        "order by cast(t.activity.irs.firstlogin as timestamp), t.id.ssn limit 2";
    test(defaultHavingIndexPlan);
    // no collation for hash index so Sort or TopN must have been preserved
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {"(Sort|TopN)"},
        new String[]{"indexName="}
    );
    DateTime date = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
        .parseDateTime("2010-01-21 00:12:24");

    testBuilder()
        .ordered()
        .sqlQuery(query)
        .baselineColumns("firstlogin", "ssn").baselineValues(date, "100005592")
        .baselineColumns("firstlogin", "ssn").baselineValues(date, "100005844")
        .build()
        .run();
  }

  @Test
  public void testNoFilterOrderBySimpleField() throws Exception {
    String query = "SELECT t.reverseid as rid, t.driverlicense as lic FROM hbase.`index_test_primary` as t " +
        "order by t.driverlicense limit 2";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {"indexName=i_lic"},
        new String[]{"Sort", "TopN"}
    );
    testBuilder()
        .ordered()
        .sqlQuery(query)
        .baselineColumns("rid", "lic").baselineValues("4539", 100000000L)
        .baselineColumns("rid", "lic").baselineValues("943", 100000001L)
        .build()
        .run();
  }

  @Test // negative case for no filter plan
  public void testNoFilterOrderByNoIndexMatch() throws Exception {
    String query = "SELECT t.`id`.`ssn` AS `ssn`, t.contact.phone as phone FROM hbase.`index_test_primary` as t " +
        "order by t.name.fname limit 2";
    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {"(Sort|TopN)"},
        new String[]{"indexName="}
    );
  }

// This test case encounters an error :
//  "Error: SYSTEM ERROR: IllegalStateException: Batch data read operation (iterator()) attempted when last
//                next() call on batch [#16, ScanBatch] returned NONE (not OK or OK_NEW_SCHEMA)."
// TODO: fix the root cause of the above error then enable the test
  @Test
  @Ignore
  public void IntersectPlanWithOneSideNoRows() throws Exception {
    try {
      String query = "SELECT t.`name`.`lname` AS `lname` FROM hbase.`index_test_primary` as t " +
              " where t.personal.age = 53 AND t.personal.income=111145";
      test(defaultHavingIndexPlan);
      test(preferIntersectPlans + ";" + disableFTS);
      PlanTestBase.testPlanMatchingPatterns(query,
              new String[]{"RowKeyJoin(.*[\n\r])+.*RestrictedJsonTableGroupScan(.*[\n\r])+.*HashJoin(.*[\n\r])+.*JsonTableGroupScan.*indexName=(i_age|i_income)(.*[\n\r])+.*JsonTableGroupScan.*indexName=(i_age|i_income)"},
              new String[]{}
      );

      testNoResult(query);

    } finally {
      test(defaultIntersectPlans + ";" + enableFTS);
    }
  }

  @Test
  public void testTrailingFieldIndexCovering() throws Exception {
    String query = "SELECT t.`name`.`fname` AS `fname` FROM hbase.`index_test_primary` as t " +
        " where cast(t.personal.age as INT)=53 AND t.contact.phone='6500005471' ";

    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {"indexName=i_cast_age_income_phone"},
        new String[]{"RowKeyJoin"}
    );

    testBuilder()
        .ordered()
        .sqlQuery(query)
        .baselineColumns("fname").baselineValues("KfFzK")
        .build()
        .run();
  }

  @Test
  public void testIncludedFieldCovering() throws Exception {
    String query = "SELECT t.`contact`.`phone` AS `phone` FROM hbase.`index_test_primary` as t " +
        " where cast(t.personal.age as INT)=53 AND t.name.fname='KfFzK' ";

    test(defaultHavingIndexPlan);
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[] {"indexName=i_cast_age_income_phone"},
        new String[]{"RowKeyJoin"}
    );

    testBuilder()
        .ordered()
        .sqlQuery(query)
        .baselineColumns("phone").baselineValues("6500005471")
        .build()
        .run();
  }

  @Test
  public void testWithFilterGroupBy() throws Exception {
    String query = " select t1.driverlicense from hbase.`index_test_primary` t1" +
            " where t1.driverlicense > 100000001 group by t1.driverlicense limit 2";
    try {
      test(defaultHavingIndexPlan);
      test(disableHashAgg);
      // no collation for hash index so Sort or TopN must have been preserved
      PlanTestBase.testPlanMatchingPatterns(query,
              new String[]{"indexName=i_lic", "StreamAgg"},
              new String[]{"(Sort|TopN)"}
      );

      testBuilder()
              .ordered()
              .sqlQuery(query)
              .optionSettingQueriesForTestQuery(disableHashAgg)
              .baselineColumns("driverlicense").baselineValues(100000002L)
              .baselineColumns("driverlicense").baselineValues(100000003L)
              .build()
              .run();
    } finally {
      test(enableHashAgg);
    }
  }

  @Test
  public void testNoFilterOrderByDesc() throws Exception {
    String query = " select t1.driverlicense from hbase.`index_test_primary` t1" +
            " order by t1.driverlicense desc limit 2";
    test(defaultHavingIndexPlan);
    // no collation for hash index so Sort or TopN must have been preserved
    PlanTestBase.testPlanMatchingPatterns(query,
            new String[] {"(Sort|TopN)"},
            new String[]{"indexName="}
    );

    testBuilder()
            .unOrdered()
            .sqlQuery(query)
            .baselineColumns("driverlicense").baselineValues(100009999L)
            .baselineColumns("driverlicense").baselineValues(100009998L)
            .build()
            .run();
  }

  @Test
  public void testNoFilterGroupBy() throws Exception {
    String query = " select t1.driverlicense from hbase.`index_test_primary` t1" +
            " group by t1.driverlicense limit 2";
    try {
      test(defaultHavingIndexPlan);
      test(disableHashAgg);
      // no collation for hash index so Sort or TopN must have been preserved
      PlanTestBase.testPlanMatchingPatterns(query,
              new String[]{"indexName=i_lic", "StreamAgg"},
              new String[]{"(Sort|TopN)"}
      );

      testBuilder()
              .ordered()
              .sqlQuery(query)
              .optionSettingQueriesForTestQuery(disableHashAgg)
              .baselineColumns("driverlicense").baselineValues(100000000L)
              .baselineColumns("driverlicense").baselineValues(100000001L)
              .build()
              .run();
    } finally {
      test(enableHashAgg);
    }
  }

  @Test
  public void testNoFilterGroupByCoveringPlan() throws Exception {
    String query = "SELECT t.`id`.`ssn` AS `ssn`, max(t.contact.phone) as phone FROM hbase.`index_test_primary` as t " +
            "group by t.id.ssn limit 2";
    try {
      test(defaultHavingIndexPlan);
      test(disableHashAgg);
      PlanTestBase.testPlanMatchingPatterns(query,
              new String[]{"indexName=i_ssn", "StreamAgg"},
              new String[]{"Sort", "TopN", "RowKeyJoin"}
      );
      testBuilder()
              .ordered()
              .sqlQuery(query)
              .optionSettingQueriesForTestQuery(disableHashAgg)
              .baselineColumns("ssn", "phone").baselineValues("100000000", "6500008069")
              .baselineColumns("ssn", "phone").baselineValues("100000001", "6500001411")
              .build()
              .run();
    } finally {
      test(enableHashAgg);
    }
  }

  @Test
  public void testNoFilterGroupByCast() throws Exception {
    String query = "SELECT CAST(t.id.ssn as INT) AS `ssn`, max(t.contact.phone) as phone FROM hbase.`index_test_primary` as t " +
            "group by CAST(t.id.ssn as INT) limit 2";
    try {
      test(defaultHavingIndexPlan);
      test(disableHashAgg);
      PlanTestBase.testPlanMatchingPatterns(query,
              new String[]{"indexName=i_cast_int_ssn", "StreamAgg"},
              new String[]{"TopN", "Sort", "RowKeyJoin"}
      );
      testBuilder()
              .ordered()
              .sqlQuery(query)
              .optionSettingQueriesForTestQuery(disableHashAgg)
              .baselineColumns("ssn", "phone").baselineValues(100000000, "6500008069")
              .baselineColumns("ssn", "phone").baselineValues(100000001, "6500001411")
              .build()
              .run();
    } finally {
      test(enableHashAgg);
    }
  }

  @Test
  public void testNoFilterGroupByHashIndex() throws Exception {
    String query = "SELECT cast(t.activity.irs.firstlogin as timestamp) AS `firstlogin`, max(t.id.ssn) as ssn FROM hbase.`index_test_primary` as t " +
            "group by cast(t.activity.irs.firstlogin as timestamp) limit 2";
    try {
      test(defaultHavingIndexPlan);
      test(disableHashAgg);
      // no collation for hash index so Sort or TopN must have been preserved
      PlanTestBase.testPlanMatchingPatterns(query,
              new String[]{"(Sort|TopN)", "StreamAgg"},
              new String[]{"indexName="}
      );
      DateTime date1 = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
              .parseDateTime("2010-01-21 00:12:24");

      DateTime date2 = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
              .parseDateTime("2010-01-21 00:24:48");
      testBuilder()
              .unOrdered()
              .sqlQuery(query)
              .optionSettingQueriesForTestQuery(disableHashAgg)
              .baselineColumns("firstlogin", "ssn").baselineValues(date1, "100006852")
              .baselineColumns("firstlogin", "ssn").baselineValues(date2, "100003660")
              .build()
              .run();
    } finally {
      test(enableHashAgg);
    }
  }

  @Test
  public void testNoFilterGroupBySimpleField() throws Exception {
    String query = "SELECT max(t.reverseid) as rid, t.driverlicense as lic FROM hbase.`index_test_primary` as t " +
            "group by t.driverlicense limit 2";
    try {
      test(defaultHavingIndexPlan);
      test(disableHashAgg);
      PlanTestBase.testPlanMatchingPatterns(query,
              new String[]{"indexName=i_lic", "StreamAgg"},
              new String[]{"Sort", "TopN"}
      );
      testBuilder()
              .ordered()
              .sqlQuery(query)
              .optionSettingQueriesForTestQuery(disableHashAgg)
              .baselineColumns("rid", "lic").baselineValues("4539", 100000000L)
              .baselineColumns("rid", "lic").baselineValues("943", 100000001L)
              .build()
              .run();
    } finally {
      test(enableHashAgg);
    }
  }

  @Test // negative case for no filter plan
  public void testNoFilterGroupByNoIndexMatch() throws Exception {
    String query = "SELECT max(t.`id`.`ssn`) AS `ssn`, max(t.contact.phone) as phone FROM hbase.`index_test_primary` as t " +
            "group by t.name.fname limit 2";
    try {
      test(defaultHavingIndexPlan);
      test(disableHashAgg);
      PlanTestBase.testPlanMatchingPatterns(query,
              new String[]{"(Sort|TopN)", "StreamAgg"},
              new String[]{"indexName="}
      );
    } finally {
      test(enableHashAgg);
    }
  }

  @Test
  public void testNoFilterGroupBySimpleFieldParallel() throws Exception {
    String query = "SELECT max(t.reverseid) as rid, t.driverlicense as lic FROM hbase.`index_test_primary` as t " +
        "group by t.driverlicense order by t.driverlicense limit 2";
    try {
      test(defaultHavingIndexPlan);
      test(disableHashAgg);
      test(sliceTargetSmall);
      PlanTestBase.testPlanMatchingPatterns(query,
              new String[]{"indexName=i_lic", "StreamAgg", "HashToMergeExchange"},
              new String[]{"Sort", "TopN"}
      );
      testBuilder()
              .unOrdered()
              .sqlQuery(query)
              .optionSettingQueriesForTestQuery(defaultHavingIndexPlan)
              .optionSettingQueriesForTestQuery(disableHashAgg)
              .optionSettingQueriesForTestQuery(sliceTargetSmall)
              .baselineColumns("rid", "lic").baselineValues("4539", 100000000L)
              .baselineColumns("rid", "lic").baselineValues("943", 100000001L)
              .build()
              .run();
    } finally {
      test(enableHashAgg);
      test(sliceTargetDefault);
    }
  }

  @Test
  public void testLimitPushdownCoveringPlan() throws Exception {
    String query = "SELECT t.`name`.`fname` AS `fname` FROM hbase.`index_test_primary` as t " +
        " where t.personal.age = 53 limit 3";
    try {
      test(defaultHavingIndexPlan + ";" + disableFTS + ";");
      PlanTestBase.testPlanWithAttributesMatchingPatterns(query,
              new String[]{".*JsonTableGroupScan.*indexName=i_age_with_fname.*rowcount = 3.0"},
              new String[]{}
      );
    } finally {
      test(enableFTS);
    }
  }

  @Test
  public void testLimitPushdownOrderByCoveringPlan() throws Exception {
    String query = "SELECT t.`name`.`fname` AS `fname` FROM hbase.`index_test_primary` as t " +
        " where t.personal.age = 53 order by t.personal.age limit 3";
    try {
      test(defaultHavingIndexPlan + ";" + disableFTS + ";");
      PlanTestBase.testPlanWithAttributesMatchingPatterns(query,
              new String[]{".*JsonTableGroupScan.*indexName=i_age_with_fname.*rowcount = 3.0"},
              new String[]{}
      );
    } finally {
      test(enableFTS);
    }
  }

  @Test
  public void testLimitPushdownNonCoveringPlan() throws Exception {
    String query = "SELECT t.`name`.`lname` AS `lname` FROM hbase.`index_test_primary` as t " +
        " where t.personal.age = 53 limit 7";
    try {
      test(defaultHavingIndexPlan+";"+disableFTS+";");
      PlanTestBase.testPlanWithAttributesMatchingPatterns(query,
              new String[]{"RowKeyJoin", ".*RestrictedJsonTableGroupScan.*tableName=.*index_test_primary.*rowcount = 7.0"},
              new String[]{}
      );
    } finally {
      test(enableFTS);
    }
  }

  @Test
  public void testLimitPushdownOrderByNonCoveringPlan() throws Exception {
    // Limit pushdown should NOT happen past rowkey join when ordering is required
    String query = "SELECT t.`name`.`lname` AS `lname` FROM hbase.`index_test_primary` as t " +
        " where t.personal.age = 53 order by t.personal.age limit 7";
    try {
      test(defaultHavingIndexPlan + ";" + disableFTS + ";" + sliceTargetSmall + ";");
      PlanTestBase.testPlanWithAttributesMatchingPatterns(query,
              new String[]{"RowKeyJoin", ".*RestrictedJsonTableGroupScan.*"},
              new String[]{".*tableName=.*index_test_primary.*rowcount = 7.*"}
      );
    } finally {
      test(enableFTS);
    }
  }

  @Test
  public void testLimit0Pushdown() throws Exception {
    // Limit pushdown should NOT happen past project with CONVERT_FROMJSON
    String query = "select convert_from(convert_to(t.`name`.`lname`, 'JSON'), 'JSON') " +
        "from hbase.`index_test_primary` as t limit 0";
    try {
      test(defaultHavingIndexPlan + ";");
      PlanTestBase.testPlanWithAttributesMatchingPatterns(query,
          new String[]{"Limit(.*[\n\r])+.*Project.*CONVERT_FROMJSON(.*[\n\r])+.*Scan"},
          new String[]{}
      );
    } finally {
    }
  }

  @Test
  public void testRemovalOfReduntantHashToMergeExchange() throws Exception {
    String query = "SELECT t.driverlicense as lic FROM hbase.`index_test_primary` as t " +
            "order by t.driverlicense limit 2";
    try {
      test(defaultHavingIndexPlan);
      test(sliceTargetSmall);
      PlanTestBase.testPlanMatchingPatterns(query,
              new String[]{"indexName=i_lic"},
              new String[]{"HashToMergeExchange", "Sort", "TopN"});
      testBuilder()
              .ordered()
              .sqlQuery(query)
              .baselineColumns("lic").baselineValues(100000000L)
              .baselineColumns("lic").baselineValues(100000001L)
              .build()
              .run();
    } finally {
      test(sliceTargetDefault);
    }
  }

  @Test
  public void testMultiPhaseAgg() throws Exception {
    String query = "select count(t.reverseid) from hbase.`index_test_primary` as t " +
            "group by t.driverlicense order by t.driverlicense";
    try {
      test(defaultHavingIndexPlan);
      test(sliceTargetSmall);
      PlanTestBase.testPlanMatchingPatterns(query,
              new String[]{"indexName=i_lic", "HashToMergeExchange", "StreamAgg", "StreamAgg"},
              new String[]{"Sort", "TopN"});
    } finally {
      test(sliceTargetDefault);
    }
  }

  @Test
  public void testHangForSimpleDistinct() throws Exception {
    String query = "select distinct t.driverlicense from hbase.`index_test_primary` as t order by t.driverlicense limit 1";

    try {
      test(sliceTargetSmall);
      testBuilder()
              .ordered()
              .sqlQuery(query)
              .baselineColumns("driverlicense").baselineValues(100000000L)
              .build()
              .run();
    } finally {
      test(sliceTargetDefault);
    }
  }

  @Test
  public void testRowkeyJoinPushdown_1() throws Exception {
    // _id IN (select col ...)
    String query = "select t1.id.ssn as ssn from hbase.`index_test_primary` t1 where _id in (select t2._id " +
        " from hbase.`index_test_primary` t2 where t2.address.city = 'pfrrs' and t2.address.state = 'pc')";
    try {
      test(incrRowKeyJoinConvSelThreshold + ";" + lowNonCoveringSelectivityThreshold + ";");
      PlanTestBase.testPlanMatchingPatterns(query, new String[] {"RowKeyJoin"}, new String[] {});
      testBuilder()
          .sqlQuery(query)
          .ordered()
          .baselineColumns("ssn").baselineValues("100007423")
          .baselineColumns("ssn").baselineValues("100008861")
          .go();
    } finally {
      test(defaultRowKeyConvSelThreshold + ";" + defaultnonCoveringSelectivityThreshold + ";");
    }
  }

  @Test
  public void testRowkeyJoinPushdown_2() throws Exception {
    // _id = col
    String query = "select t1.id.ssn as ssn from hbase.`index_test_primary` t1, hbase.`index_test_primary` t2 " +
        " where t1._id = t2._id and t2.address.city = 'pfrrs' and t2.address.state = 'pc'";
    try {
      test(incrRowKeyJoinConvSelThreshold + ";" + lowNonCoveringSelectivityThreshold + ";");
      PlanTestBase.testPlanMatchingPatterns(query, new String[] {"RowKeyJoin"}, new String[] {});
      testBuilder()
          .sqlQuery(query)
          .ordered()
          .baselineColumns("ssn").baselineValues("100007423")
          .baselineColumns("ssn").baselineValues("100008861")
          .go();
    } finally {
      test(defaultRowKeyConvSelThreshold + ";" + defaultnonCoveringSelectivityThreshold + ";");
    }
  }

  @Test
  public void testRowkeyJoinPushdown_3() throws Exception {
    // filters on both sides of the join
    String query = "select t1.id.ssn as ssn from hbase.`index_test_primary` t1, hbase.`index_test_primary` t2 " +
        " where t1._id = t2._id and t1.address.city = 'pfrrs' and t2.address.city = 'pfrrs'";
    try {
      test(incrRowKeyJoinConvSelThreshold + ";" + lowNonCoveringSelectivityThreshold + ";");
      PlanTestBase.testPlanMatchingPatterns(query, new String[] {"RowKeyJoin"}, new String[] {});
      testBuilder()
          .sqlQuery(query)
          .ordered()
          .baselineColumns("ssn").baselineValues("100007423")
          .baselineColumns("ssn").baselineValues("100008861")
          .go();
    } finally {
      test(defaultRowKeyConvSelThreshold + ";" + defaultnonCoveringSelectivityThreshold + ";");
    }
  }

  @Test
  public void testRowkeyJoinPushdown_4() throws Exception {
    // _id = cast(col as int) works since the rowids are internally cast to string!
    String query = "select t1.id.ssn as ssn from hbase.`index_test_primary` t1, hbase.`index_test_primary` t2 " +
        " where t1._id = cast(t2.rowid as int) and t2.address.city = 'pfrrs'";
    try {
      test(incrRowKeyJoinConvSelThreshold + ";" + lowNonCoveringSelectivityThreshold + ";");
      PlanTestBase.testPlanMatchingPatterns(query, new String[] {"RowKeyJoin"}, new String[] {});
      testBuilder()
          .sqlQuery(query)
          .ordered()
          .baselineColumns("ssn").baselineValues("100007423")
          .baselineColumns("ssn").baselineValues("100008861")
          .go();
    } finally {
      test(defaultRowKeyConvSelThreshold + ";" + defaultnonCoveringSelectivityThreshold + ";");
    }
  }

  @Test
  public void testRowkeyJoinPushdown_5() throws Exception {
    // _id = cast(cast(col as int) as varchar(10)
    String query = "select t1.id.ssn as ssn from hbase.`index_test_primary` t1, hbase.`index_test_primary` t2 " +
        " where t1._id = cast(cast(t2.rowid as int) as varchar(10)) and t2.address.city = 'pfrrs'";
    try {
      test(incrRowKeyJoinConvSelThreshold + ";" + lowNonCoveringSelectivityThreshold + ";");
      PlanTestBase.testPlanMatchingPatterns(query, new String[] {"RowKeyJoin"}, new String[] {});
      testBuilder()
          .sqlQuery(query)
          .ordered()
          .baselineColumns("ssn").baselineValues("100007423")
          .baselineColumns("ssn").baselineValues("100008861")
          .go();
    } finally {
      test(defaultRowKeyConvSelThreshold + ";" + defaultnonCoveringSelectivityThreshold + ";");
    }
  }

  @Test
  public void testRowkeyJoinPushdown_6() throws Exception {
    // _id IN (select cast(cast(col as int) as varchar(10) ... JOIN ...)
    String query = "select t1.id.ssn as ssn from hbase.`index_test_primary` t1 where _id in " +
        "(select cast(cast(t2.rowid as int) as varchar(10)) from hbase.`index_test_primary` t2, hbase.`index_test_primary` t3 " +
        "where t2.address.city = t3.address.city and t2.name.fname = 'ubar')";
    try {
      test(incrRowKeyJoinConvSelThreshold + ";" + lowNonCoveringSelectivityThreshold + ";");
      PlanTestBase.testPlanMatchingPatterns(query, new String[] {"RowKeyJoin"}, new String[] {});
      testBuilder()
          .sqlQuery(query)
          .ordered()
          .baselineColumns("ssn").baselineValues("100001382")
          .go();
    } finally {
      test(defaultRowKeyConvSelThreshold + ";" + defaultnonCoveringSelectivityThreshold + ";");
    }
  }

  @Test
  public void testRowkeyJoinPushdown_7() throws Exception {
    // with non-covering index
    String query = "select t1.id.ssn as ssn from hbase.`index_test_primary` t1, hbase.`index_test_primary` t2 " +
        "where t1._id = t2.rowid and t2.address.city = 'pfrrs'";
    try {
      test(incrRowKeyJoinConvSelThreshold + ";" + incrnonCoveringSelectivityThreshold + ";");
      PlanTestBase.testPlanMatchingPatterns(query,
          new String[] {"RowKeyJoin", "RestrictedJsonTableGroupScan", "RowKeyJoin", "Scan.*condition=\\(address.city = \"pfrrs\"\\)"},
          new String[] {});
      testBuilder()
          .sqlQuery(query)
          .ordered()
          .baselineColumns("ssn").baselineValues("100007423")
          .baselineColumns("ssn").baselineValues("100008861")
          .go();
    } finally {
      test(defaultRowKeyConvSelThreshold + ";" + defaultnonCoveringSelectivityThreshold + ";");
    }
  }

  @Test
  public void testRowkeyJoinPushdown_8() throws Exception {
    // with covering index
    String query = "select t1.id.ssn as ssn from hbase.`index_test_primary` t1, hbase.`index_test_primary` t2 " +
        "where t1._id = t2.rowid and t2.rowid = '1012'";
    try {
      test(incrRowKeyJoinConvSelThreshold);
      PlanTestBase.testPlanMatchingPatterns(query,
          new String[] {"RowKeyJoin", "indexName=i_rowid_cast_date_birthdate"},
          new String[] {});
      testBuilder()
          .sqlQuery(query)
          .ordered()
          .baselineColumns("ssn").baselineValues("100007423")
          .go();
    } finally {
      test(defaultRowKeyConvSelThreshold);
    }
  }

  @Test
  public void testRowkeyJoinPushdown_9() throws Exception {
    // Negative test - rowkey join should not be present
    String query = "select t1.id.ssn as ssn from hbase.`index_test_primary` t1 where cast(_id as varchar(10)) in " +
        "(select t2._id from hbase.`index_test_primary` t2 where t2.address.city = 'pfrrs')";
    try {
      test(incrRowKeyJoinConvSelThreshold + ";" + lowNonCoveringSelectivityThreshold + ";");
      PlanTestBase.testPlanMatchingPatterns(query, new String[] {}, new String[] {"RowKeyJoin"});
      testBuilder()
          .sqlQuery(query)
          .ordered()
          .baselineColumns("ssn").baselineValues("100007423")
          .baselineColumns("ssn").baselineValues("100008861")
          .go();
    } finally {
      test(defaultRowKeyConvSelThreshold + ";" + defaultnonCoveringSelectivityThreshold + ";");
    }
  }

  @Test
  public void testRowkeyJoinPushdown_10() throws Exception {
    // Negative test - rowkey join should not be present
    String query = "select t1.id.ssn as ssn from hbase.`index_test_primary` t1, hbase.`index_test_primary` t2 " +
        " where cast(t1._id as varchar(10)) = cast(t2._id as varchar(10)) and t2.address.city = 'pfrrs'";
    try {
      test(incrRowKeyJoinConvSelThreshold + ";" + lowNonCoveringSelectivityThreshold + ";");
      PlanTestBase.testPlanMatchingPatterns(query, new String[] {}, new String[] {"RowKeyJoin"});
      testBuilder()
          .sqlQuery(query)
          .ordered()
          .baselineColumns("ssn").baselineValues("100007423")
          .baselineColumns("ssn").baselineValues("100008861")
          .go();
    } finally {
      test(defaultRowKeyConvSelThreshold + ";" + defaultnonCoveringSelectivityThreshold + ";");
    }
  }

  @Test
  public void testRowkeyJoinPushdown_11() throws Exception {
    // Negative test - rowkey join should not be present
    String query = "select t1.id.ssn as ssn from hbase.`index_test_primary` t1 where cast(_id as varchar(10)) in " +
        "(select t2._id from hbase.`index_test_primary` t2, hbase.`index_test_primary` t3 where t2.address.city = t3.address.city " +
        "and t2.address.city = 'pfrrs')";
    try {
      test(incrRowKeyJoinConvSelThreshold + ";" + lowNonCoveringSelectivityThreshold + ";");
      PlanTestBase.testPlanMatchingPatterns(query, new String[] {}, new String[] {"RowKeyJoin"});
      testBuilder()
          .sqlQuery(query)
          .ordered()
          .baselineColumns("ssn").baselineValues("100007423")
          .baselineColumns("ssn").baselineValues("100008861")
          .go();
    } finally {
      test(defaultRowKeyConvSelThreshold + ";" + defaultnonCoveringSelectivityThreshold + ";");
    }
  }

  @Ignore
  @Test
  public void testRowkeyJoinPushdown_12() throws Exception {
    // JOIN _id IN (select cast(cast(col as int) as varchar(10) ... JOIN ...) - rowkey join appears in intermediate join order
    String query = "select t1.id.ssn as ssn from hbase.`index_test_primary` t1, hbase.`index_test_primary` t4 " +
        "where t1.address.city = t4.address.city and t1._id in (select cast(cast(t2.rowid as int) as varchar(10)) " +
        "from hbase.`index_test_primary` t2, hbase.`index_test_primary` t3 where t2.address.city = t3.address.city " +
        "and t2.address.state = 'pc') and t4.address.state = 'pc'";
    try {
      test(incrRowKeyJoinConvSelThreshold + ";" + lowNonCoveringSelectivityThreshold + ";");
      PlanTestBase.testPlanMatchingPatterns(query,
          new String[] {"HashJoin(.*[\n\r])+.*Scan.*indexName=i_state_city_dl(.*[\n\r])+.*RowKeyJoin(.*[\n\r])+.*RestrictedJsonTableGroupScan(.*[\n\r])+.*HashAgg\\(group=\\[\\{0\\}\\]\\)(.*[\n\r])+.*HashJoin"},
          new String[] {});
      testBuilder()
          .sqlQuery(query)
          .ordered()
          .baselineColumns("ssn").baselineValues("100007423")
          .baselineColumns("ssn").baselineValues("100007423")
          .go();
    } finally {
      test(defaultRowKeyConvSelThreshold + ";" + defaultnonCoveringSelectivityThreshold + ";");
    }
  }

  @Ignore
  @Test
  public void testRowkeyJoinPushdown_13() throws Exception {
    // Check option planner.rowkeyjoin_conversion_using_hashjoin works as expected!
    String query = "select t1.id.ssn as ssn from hbase.`index_test_primary` t1 where _id in (select t2._id " +
            " from hbase.`index_test_primary` t2 where t2.address.city = 'pfrrs')";
    try {
      test(incrRowKeyJoinConvSelThreshold + ";" + lowNonCoveringSelectivityThreshold + ";");
      PlanTestBase.testPlanMatchingPatterns(query, new String[]{"RowKeyJoin"}, new String[]{});
      testBuilder()
              .sqlQuery(query)
              .ordered()
              .baselineColumns("ssn").baselineValues("100007423")
              .baselineColumns("ssn").baselineValues("100008861")
              .go();
      test(incrRowKeyJoinConvSelThreshold + ";" + lowNonCoveringSelectivityThreshold + ";" +
              forceRowKeyJoinConversionUsingHashJoin + ";");
      PlanTestBase.testPlanMatchingPatterns(query, new String[]{"HashJoin"}, new String[]{"RowKeyJoin"});
      testBuilder()
              .sqlQuery(query)
              .ordered()
              .baselineColumns("ssn").baselineValues("100007423")
              .baselineColumns("ssn").baselineValues("100008861")
              .go();
    } finally {
      test(defaultRowKeyConvSelThreshold + ";" + defaultnonCoveringSelectivityThreshold + ";" +
              defaultRowKeyJoinConversionUsingHashJoin);
    }
  }

  public void TestIndexScanWithDescOrderByNullsFirst() throws Exception {

    String query = "select t.personal.age from hbase.`index_test_primary` t order by t.personal.age desc nulls first limit 1";
    try {
      test(defaultHavingIndexPlan + ";" + lowRowKeyJoinBackIOFactor + ";");
      PlanTestBase.testPlanMatchingPatterns(query,
              new String[]{".*JsonTableGroupScan.*indexName=i_age_desc.*"},
              new String[]{}
      );
    } finally {
      test(defaultRowKeyJoinBackIOFactor);
    }
    return;
  }

  @Test
  public void TestIndexScanWithDescOrderByNullsLast() throws Exception {

    String query = "select t.personal.age from hbase.`index_test_primary` t order by t.personal.age desc nulls last limit 1";
    try {
      test(defaultHavingIndexPlan + ";" + lowRowKeyJoinBackIOFactor + ";");
      PlanTestBase.testPlanMatchingPatterns(query,
              new String[]{},
              new String[]{".*indexName=i_age_desc.*"}
      );
    } finally {
      test(defaultRowKeyJoinBackIOFactor);
    }
    return;
  }
}
