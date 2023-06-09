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
package org.apache.drill.exec.fn.hive;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.drill.categories.HiveStorageTest;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.compile.ClassCompilerSelector;
import org.apache.drill.exec.compile.ClassTransformer;
import org.apache.drill.exec.hive.HiveTestBase;
import org.apache.drill.test.TestBuilder;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

@Category({SlowTest.class, HiveStorageTest.class})
public class TestInbuiltHiveUDFs extends HiveTestBase {

  @Test // DRILL-3273
  public void testConcatWS() throws Exception {
    testBuilder()
        .sqlQuery("SELECT concat_ws(string_field, string_part, '|') as rst from hive.readtest")
        .unOrdered()
        .baselineColumns("rst")
        .baselineValues("stringstringfield|")
        .baselineValues(new Object[] { null })
        .go();
  }

  @Test // DRILL-3273
  public void testEncode() throws Exception {
    testBuilder()
        .sqlQuery("SELECT encode(varchar_field, 'UTF-8') as rst from hive.readtest")
        .unOrdered()
        .baselineColumns("rst")
        .baselineValues("varcharfield".getBytes())
        .baselineValues(new Object[] { null })
        .go();
  }

  @Test
  public void testXpath_Double() throws Exception {
    final String query = "select xpath_double ('<a><b>20</b><c>40</c></a>', 'a/b * a/c') as col \n" +
        "from hive.kv \n" +
        "limit 0";

    final TypeProtos.MajorType majorType = TypeProtos.MajorType.newBuilder()
        .setMinorType(TypeProtos.MinorType.FLOAT8)
        .setMode(TypeProtos.DataMode.OPTIONAL)
        .build();

    final List<Pair<SchemaPath, TypeProtos.MajorType>> expectedSchema = Lists.newArrayList();
    expectedSchema.add(Pair.of(SchemaPath.getSimplePath("col"), majorType));

    testBuilder()
        .sqlQuery(query)
        .schemaBaseLine(expectedSchema)
        .build()
        .run();
  }

  @Test // DRILL-4459
  public void testGetJsonObject() throws Exception {
    testBuilder()
        .sqlQuery("select convert_from(json, 'json') as json from hive.simple_json " +
            "where GET_JSON_OBJECT(simple_json.json, '$.employee_id') like 'Emp2'")
        .ordered()
        .baselineColumns("json")
        .baselineValues(TestBuilder.mapOf("employee_id","Emp2","full_name","Kamesh",
            "first_name","Bh","last_name","Venkata","position","Store"))
        .go();
  }

  @Test // DRILL-3272
  public void testIf() throws Exception {
    testBuilder()
        .sqlQuery("select `if`(1999 > 2000, 'latest', 'old') `Period` from hive.kv limit 1")
        .ordered()
        .baselineColumns("Period")
        .baselineValues("old")
        .go();
  }

  @Test // DRILL-4618
  public void testRand() throws Exception {
    String query = "select 2*rand()=2*rand() col1 from (values (1))";
    testBuilder()
            .sqlQuery(query)
            .unOrdered()
            .baselineColumns("col1")
            .baselineValues(false)
            .go();
  }

  @Test //DRILL-4868 & DRILL-2326
  public void testEmbeddedHiveFunctionCall() throws Exception {
    String query =
        "SELECT convert_from(unhex(key2), 'INT_BE') as intkey \n" +
        "FROM cp.`functions/conv/conv.json`";

    List<String> compilers = Arrays.asList(ClassCompilerSelector.CompilerPolicy.JANINO.name(),
        ClassCompilerSelector.CompilerPolicy.JDK.name());

    try {
      setSessionOption(ExecConstants.SCALAR_REPLACEMENT_OPTION, ClassTransformer.ScalarReplacementOption.ON.name());
      for (String compilerName : compilers) {
        setSessionOption(ClassCompilerSelector.JAVA_COMPILER_OPTION, compilerName);

        testBuilder()
            .sqlQuery(query)
            .unOrdered()
            .baselineColumns("intkey")
            .baselineValuesForSingleColumn(1244739896, null, 1313814865, 1852782897)
            .build()
            .run();
      }
    } finally {
      resetSessionOption(ExecConstants.SCALAR_REPLACEMENT_OPTION);
      resetSessionOption(ClassCompilerSelector.JAVA_COMPILER_OPTION);
    }
  }

  @Test
  public void testLastDay() throws Exception {
    testBuilder()
        .sqlQuery("select last_day(to_date('1994-02-01','yyyy-MM-dd')) as `LAST_DAY` from (VALUES(1))")
        .unOrdered()
        .baselineColumns("LAST_DAY")
        .baselineValues(LocalDate.parse("1994-02-28"))
        .go();
  }

  @Test
  public void testDatediff() throws Exception {
    testBuilder()
        .sqlQuery("select datediff(date '1996-03-01', timestamp '1997-02-10 17:32:00.0') as `DATEDIFF` from (VALUES(1))")
        .unOrdered()
        .baselineColumns("DATEDIFF")
        .baselineValues(-346)
        .go();
  }

  @Test
  public void testFromUTCTimestamp() throws Exception {
    testBuilder()
        .sqlQuery("select from_utc_timestamp('1970-01-01 08:00:00','PST') as PST_TIMESTAMP from (VALUES(1))")
        .unOrdered()
        .baselineColumns("PST_TIMESTAMP")
        .baselineValues(LocalDateTime.parse("1970-01-01T00:00:00.0"))
        .go();
  }

  @Test
  public void testToUTCTimestamp() throws Exception {
    testBuilder()
        .sqlQuery("select to_utc_timestamp('1970-01-01 00:00:00','PST') as UTC_TIMESTAMP from (VALUES(1))")
        .unOrdered()
        .baselineColumns("UTC_TIMESTAMP")
        .baselineValues(LocalDateTime.parse("1970-01-01T08:00:00.0"))
        .go();
  }

  @Test // DRILL-4456
  public void testTranslate3() throws Exception {
    testBuilder()
        .sqlQuery("SELECT translate(string_field, 's', 'S') as ts," +
            "translate(varchar_field, 'v', 'V') as tv,\n" +
            "translate('literal', 'l', 'L') as tl from hive.readtest")
        .unOrdered()
        .baselineColumns("ts", "tv", "tl")
        .baselineValues("Stringfield", "Varcharfield", "LiteraL")
        .baselineValues(null, null, "LiteraL")
        .go();
  }

}
