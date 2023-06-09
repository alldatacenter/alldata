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
package org.apache.drill.exec.udfs.gis;

import org.apache.drill.categories.SqlFunctionTest;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.test.BaseTestQuery;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category({UnlikelyTest.class, SqlFunctionTest.class})
public class TestGeometryFunctions extends BaseTestQuery {

  private final String wktPoint = "POINT (-121.895 37.339)";
  private final String json = "{\"x\":-121.895,\"y\":37.339,\"spatialReference\":{\"wkid\":4326}}";
  private final String geoJson = "{\"type\":\"Point\",\"coordinates\":[-121.895,37.339],"
    + "\"crs\":{\"type\":\"name\",\"properties\":{\"name\":\"EPSG:4326\"}}}";

  @Test
  public void testGeometryFromTextCreation() throws Exception {
    testBuilder()
    .sqlQuery("select ST_AsText(ST_GeomFromText('" + wktPoint + "')) "
        + "from cp.`sample-data/CA-cities.csv` limit 1")
    .ordered().baselineColumns("EXPR$0")
    .baselineValues(wktPoint)
    .build()
    .run();
  }

  @Test
  public void testGeometryPointCreation() throws Exception {
    testBuilder()
      .sqlQuery("select ST_AsText(ST_Point(-121.895, 37.339)) "
          + "from cp.`sample-data/CA-cities.csv` limit 1")
      .ordered().baselineColumns("EXPR$0")
      .baselineValues(wktPoint)
      .build()
      .run();
  }

  @Test
  public void testJSONFromPointCreation() throws Exception {
    testBuilder()
      .sqlQuery("select ST_AsJson(ST_Point(-121.895, 37.339)) "
        + "from cp.`/sample-data/CA-cities.csv` limit 1")
      .ordered()
      .baselineColumns("EXPR$0")
      .baselineValues(json)
      .build()
      .run();
  }

  @Test
  public void testJSONFromTextCreation() throws Exception {
    testBuilder()
      .sqlQuery("select ST_AsJson(ST_GeomFromText('" + wktPoint + "')) "
        + "from cp.`/sample-data/CA-cities.csv` limit 1")
      .ordered()
      .baselineColumns("EXPR$0")
      .baselineValues(json)
      .build()
      .run();
  }

  @Test
  public void testNullWkt() throws Exception {
    testBuilder()
      .sqlQuery("select ST_AsText(ST_GeomFromText(columns[4])) " +
              "from cp.`/sample-data/CA-cities-with-nulls.csv` limit 1")
      .ordered()
      .baselineColumns("EXPR$0")
      .baselineValues(new Object[]{null})
      .build()
      .run();
  }

  @Test
  public void testNullGeoJSON() throws Exception {
    testBuilder()
            .sqlQuery("select ST_AsGeoJson(ST_GeomFromText(columns[4])) " +
                    "from cp.`/sample-data/CA-cities-with-nulls.csv` limit 1")
            .ordered()
            .baselineColumns("EXPR$0")
            .baselineValues(new Object[]{null})
            .build()
            .run();
  }

  @Test
  public void testGeoJSONCreationFromPoint() throws Exception {
    testBuilder()
      .sqlQuery("select ST_AsGeoJSON(ST_Point(-121.895, 37.339)) "
        + "from cp.`/sample-data/CA-cities.csv` limit 1")
      .ordered()
      .baselineColumns("EXPR$0")
      .baselineValues(geoJson)
      .build()
      .run();
  }
  @Test
  public void testGeoJSONCreationFromGeom() throws Exception {
    testBuilder()
      .sqlQuery("select ST_AsGeoJSON(ST_GeomFromText('" + wktPoint + "')) "
        + "from cp.`/sample-data/CA-cities.csv` limit 1")
      .ordered()
      .baselineColumns("EXPR$0")
      .baselineValues(geoJson)
      .build()
      .run();
  }

  @Test
  public void testSTWithinQuery() throws Exception {
    testBuilder()
      .sqlQuery("select ST_Within(ST_Point(columns[4], columns[3]),"
          + "ST_GeomFromText('POLYGON((-121.95 37.28, -121.94 37.35, -121.84 37.35, -121.84 37.28, -121.95 37.28))')"
          + ") "
          + "from cp.`sample-data/CA-cities.csv` where columns[2] = 'San Jose'")
      .ordered().baselineColumns("EXPR$0")
      .baselineValues(true)
      .build()
      .run();


    testBuilder()
    .sqlQuery("select ST_Within(" + "ST_Point(columns[4], columns[3]),"
        + "ST_GeomFromText('POLYGON((-121.95 37.28, -121.94 37.35, -121.84 37.35, -121.84 37.28, -121.95 37.28))')"
        + ") "
        + "from cp.`sample-data/CA-cities.csv` where columns[2] = 'San Francisco'")
    .ordered().baselineColumns("EXPR$0")
    .baselineValues(false)
    .build()
    .run();
  }

  @Test
  public void testSTXQuery() throws Exception {
    testBuilder()
      .sqlQuery("select ST_X(ST_Point(-121.895, 37.339)) "
          + "from cp.`/sample-data/CA-cities.csv` limit 1")
      .ordered().baselineColumns("EXPR$0")
      .baselineValues(-121.895)
      .build()
      .run();
  }

  @Test
  public void testSTYQuery() throws Exception {
    testBuilder()
      .sqlQuery("select ST_Y(ST_Point(-121.895, 37.339)) "
          + "from cp.`/sample-data/CA-cities.csv` limit 1")
      .ordered().baselineColumns("EXPR$0")
      .baselineValues(37.339)
      .build()
      .run();
  }

  @Test
  public void testIntersectQuery() throws Exception {
    testBuilder()
      .sqlQuery("SELECT ST_Intersects(ST_GeomFromText('POINT(0 0)'), ST_GeomFromText('LINESTRING(2 0,0 2)')) "
          + "from (VALUES(1))")
      .ordered().baselineColumns("EXPR$0")
      .baselineValues(false)
      .build()
      .run();

    testBuilder()
    .sqlQuery("SELECT ST_Intersects(ST_GeomFromText('POINT(0 0)'), ST_GeomFromText('LINESTRING(0 0,0 2)')) "
        + "from (VALUES(1))")
    .ordered().baselineColumns("EXPR$0")
    .baselineValues(true)
    .build()
    .run();
  }

  @Test
  public void testRelateQuery() throws Exception {
    testBuilder()
      .sqlQuery("SELECT ST_Relate(ST_GeomFromText('POINT(1 2)'), ST_Buffer(ST_GeomFromText('POINT(1 2)'),2), '0FFFFF212') "
          + "from (VALUES(1))")
      .ordered().baselineColumns("EXPR$0")
      .baselineValues(true)
      .build()
      .run();

    testBuilder()
    .sqlQuery("SELECT ST_Relate(ST_GeomFromText('POINT(1 2)'), ST_Buffer(ST_GeomFromText('POINT(1 2)'),2), '*FF*FF212') "
        + "from (VALUES(1))")
    .ordered().baselineColumns("EXPR$0")
    .baselineValues(true)
    .build()
    .run();

    testBuilder()
    .sqlQuery("SELECT ST_Relate(ST_GeomFromText('POINT(0 0)'), ST_Buffer(ST_GeomFromText('POINT(1 2)'),2), '*FF*FF212') "
        + "from (VALUES(1))")
    .ordered().baselineColumns("EXPR$0")
    .baselineValues(false)
    .build()
    .run();
  }

  @Test
  public void testTouchesQuery() throws Exception {
    testBuilder()
      .sqlQuery("SELECT ST_Touches(ST_GeomFromText('LINESTRING(0 0, 1 1, 0 2)'), ST_GeomFromText('POINT(1 1)')) "
          + "from (VALUES(1))")
      .ordered().baselineColumns("EXPR$0")
      .baselineValues(false)
      .build()
      .run();

    testBuilder()
    .sqlQuery("SELECT ST_Touches(ST_GeomFromText('LINESTRING(0 0, 1 1, 0 2)'), ST_GeomFromText('POINT(0 2)')) "
        + "from (VALUES(1))")
    .ordered().baselineColumns("EXPR$0")
    .baselineValues(true)
    .build()
    .run();
  }

  @Test
  public void testEqualsQuery() throws Exception {
    testBuilder()
      .sqlQuery("SELECT ST_Equals(ST_GeomFromText('LINESTRING(0 0, 10 10)'), "
                + "ST_GeomFromText('LINESTRING(0 0, 5 5, 10 10)')) from (VALUES(1))")
      .ordered().baselineColumns("EXPR$0")
      .baselineValues(true)
      .build()
      .run();
  }

  @Test
  public void testContainsQuery() throws Exception {
    testBuilder()
      .sqlQuery("SELECT ST_Contains(smallc, bigc) As smallcontainsbig, "
                     + "ST_Contains(bigc,smallc) As bigcontainssmall, "
                     + "ST_Contains(bigc, ST_Union(smallc, bigc)) as bigcontainsunion, "
                     + "ST_Equals(bigc, ST_Union(smallc, bigc)) as bigisunion "
                + "FROM (SELECT ST_Buffer(ST_GeomFromText('POINT(1 2)'), 10) As smallc, "
                       + "ST_Buffer(ST_GeomFromText('POINT(1 2)'), 20) As bigc from (VALUES(1)) ) As foo")
      .ordered().baselineColumns("smallcontainsbig", "bigcontainssmall", "bigcontainsunion", "bigisunion")
      .baselineValues(false, true, true, true)
      .build()
      .run();
  }

  @Test
  public void testOverlapsCrossesIntersectsContainsQuery() throws Exception {
    testBuilder()
      .sqlQuery("SELECT ST_Overlaps(a,b) As a_overlap_b, "
                  + "ST_Crosses(a,b) As a_crosses_b, "
                  + "ST_Intersects(a, b) As a_intersects_b, "
                  + "ST_Contains(b,a) As b_contains_a "
                + "FROM (SELECT ST_GeomFromText('POINT(1 0.5)') As a, ST_GeomFromText('LINESTRING(1 0, 1 1, 3 5)')  As b "
                  + "from (VALUES(1)) ) As foo")
      .ordered().baselineColumns("a_overlap_b", "a_crosses_b", "a_intersects_b", "b_contains_a")
      .baselineValues(false, false, true, true)
      .build()
      .run();
  }

  @Test
  public void testDisjointQuery() throws Exception {
    testBuilder()
      .sqlQuery("SELECT ST_Disjoint(ST_GeomFromText('POINT(0 0)'), ST_GeomFromText('LINESTRING( 2 0, 0 2 )')) "
                + "from (VALUES(1))")
      .ordered().baselineColumns("EXPR$0")
      .baselineValues(true)
      .build()
      .run();

    testBuilder()
    .sqlQuery("SELECT ST_Disjoint(ST_GeomFromText('POINT(0 0)'), ST_GeomFromText('LINESTRING( 0 0, 0 2 )')) "
              + "from (VALUES(1))")
    .ordered().baselineColumns("EXPR$0")
    .baselineValues(false)
    .build()
    .run();
  }

  @Test
  public void testTransformQuery() throws Exception {
    double targetX = -71.1776848522251;
    double targetY = 42.3902896512902;

    testBuilder()
      .sqlQuery("SELECT round(st_x(st_transform(st_geomfromtext('POINT (743238 2967416)'), 2249, 4326)), 13),"
          + " round(st_y(st_transform(st_geomfromtext('POINT (743238 2967416)'), 2249, 4326)), 13) from (VALUES(1))")
      .ordered().baselineColumns("EXPR$0", "EXPR$1")
      .baselineValues(targetX, targetY)
      .build()
      .run();
  }

  @Test
  public void testUnionAggregateQuery() throws Exception {
    String targetAll = "MULTIPOLYGON (((0 -1, 1 -1, 1 0, 1 1, 0 1, 0 0, 0 -1)), "
                        + "((10 9, 11 9, 11 10, 11 11, 10 11, 10 10, 10 9)))";
    String targetFirstGroup = "POLYGON ((0 -1, 1 -1, 1 0, 1 1, 0 1, 0 0, 0 -1))";
    String targetSecondGroup = "POLYGON ((10 9, 11 9, 11 10, 11 11, 10 11, 10 10, 10 9))";

    testBuilder()
      .sqlQuery("select ST_AsText(ST_UnionAggregate(ST_GeomFromText(columns[1]))) from cp.`sample-data/polygons.tsv`")
      .ordered().baselineColumns("EXPR$0")
      .baselineValues(targetAll)
      .build()
      .run();

    testBuilder()
      .sqlQuery("select columns[0], ST_AsText(ST_UnionAggregate(ST_GeomFromText(columns[1])))"
          + " from cp.`sample-data/polygons.tsv` group by columns[0] having columns[0] = '1'")
      .ordered().baselineColumns("EXPR$0", "EXPR$1")
      .baselineValues("1", targetFirstGroup)
      .build()
      .run();

    testBuilder()
      .sqlQuery("select columns[0], ST_AsText(ST_UnionAggregate(ST_GeomFromText(columns[1])))"
          + " from cp.`sample-data/polygons.tsv` group by columns[0] having columns[0] = '2'")
      .ordered().baselineColumns("EXPR$0", "EXPR$1")
      .baselineValues("2", targetSecondGroup)
      .build()
      .run();

    testBuilder()
    .sqlQuery("select count(*) from (select columns[0], ST_AsText(ST_UnionAggregate(ST_GeomFromText(columns[1])))"
        + " from cp.`sample-data/polygons.tsv` group by columns[0])")
    .ordered().baselineColumns("EXPR$0")
    .baselineValues(3L)
    .build()
    .run();
  }
}
