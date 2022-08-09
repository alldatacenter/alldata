/*
 * Copyright 2019 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.absa.spline.persistence.tx

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import za.co.absa.spline.persistence.model.NodeDef

class TxBuilderSpec extends AnyFlatSpec with Matchers with MockitoSugar {

  behavior of "generateJs()"

  it should "generate NATIVE statements" in {
    val generatedJS = new TxBuilder()
      .addQuery(NativeQuery("db.FOO();"))
      .addQuery(NativeQuery("db.BAR();"))
      .generateJs()

    generatedJS should be {
      """
        |function (_params) {
        |  const _db = require('internal').db;
        |  (function(db, params){
        |    db.FOO();
        |  })(_db, _params[0]);
        |  (function(db, params){
        |    db.BAR();
        |  })(_db, _params[1]);
        |}
        |""".stripMargin
    }
  }

  it should "generate INSERT statements" in {
    val generatedJS = new TxBuilder()
      .addQuery(InsertQuery(NodeDef.DataSource))
      .addQuery(InsertQuery(NodeDef.Operation).copy(ignoreExisting = true))
      .generateJs()

    generatedJS should be {
      """
        |function (_params) {
        |  const _db = require('internal').db;
        |  _params[0].forEach(o =>_db._collection("dataSource").insert(o, {silent:true}));
        |  _params[1].forEach(o =>
        |    o._key && _db._collection("operation").exists(o._key) ||
        |    o._from && o._to && _db._query(`
        |      WITH operation
        |      FOR e IN operation
        |          FILTER e._from == @o._from && e._to == @o._to
        |          LIMIT 1
        |          COLLECT WITH COUNT INTO cnt
        |          RETURN !!cnt
        |      `, {o}).next() ||
        |    _db._collection("operation").insert(o, {silent:true}));
        |}
        |""".stripMargin
    }
  }

  it should "generate UPDATE statements" in {
    val generatedJS = new TxBuilder()
      .addQuery(UpdateQuery(NodeDef.DataSource, s"${UpdateQuery.DocWildcard}.foo == 42", Map.empty))
      .addQuery(UpdateQuery(NodeDef.DataSource, s"${UpdateQuery.DocWildcard}.baz == 777", Map.empty))
      .generateJs()

    generatedJS should be {
      """
        |function (_params) {
        |  const _db = require('internal').db;
        |  _db._query(`
        |    WITH dataSource
        |    FOR a IN dataSource
        |        FILTER a.foo == 42
        |        UPDATE a._key WITH @b IN dataSource
        |  `, {"b": _params[0]});
        |  _db._query(`
        |    WITH dataSource
        |    FOR a IN dataSource
        |        FILTER a.baz == 777
        |        UPDATE a._key WITH @b IN dataSource
        |  `, {"b": _params[1]});
        |}
        |""".stripMargin
    }
  }

}
