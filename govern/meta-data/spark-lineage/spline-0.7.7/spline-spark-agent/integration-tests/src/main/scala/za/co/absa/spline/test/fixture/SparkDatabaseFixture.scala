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

package za.co.absa.spline.test.fixture

import org.apache.spark.sql.SparkSession
import org.scalatest.{Assertion, AsyncTestSuite}

import scala.concurrent.Future

trait SparkDatabaseFixture {
  this: AsyncTestSuite =>

  private type DatabaseName = String
  private type TableName = String
  private type TableDef = String
  private type TableData = Seq[Any]

  def withDatabase
      (databaseName: DatabaseName, tableDefs: (TableName, TableDef, TableData)*)
      (testBody: => Future[Assertion])
      (implicit spark: SparkSession): Future[Assertion] = {

    val innerSpark = spark.newSession()

    prepareDatabase(innerSpark, databaseName, tableDefs)
    spark.sql(s"USE $databaseName")

    testBody.andThen { case _ => dropDatabase(innerSpark, databaseName) }
  }

  private def prepareDatabase(
    spark: SparkSession,
    databaseName: DatabaseName,
    tableDefs: Seq[(TableName, TableDef, TableData)]
  ): Unit = {
    dropDatabase(spark, databaseName)
    spark.sql(s"CREATE DATABASE $databaseName")
    spark.sql(s"USE $databaseName")

    tableDefs.foreach({
      case (tableName, tableDef, rows) =>
        spark.sql(s"CREATE TABLE $tableName $tableDef")
        rows
          .map(sqlizeRow)
          .foreach(values =>
            spark.sql(s"INSERT INTO $tableName VALUES (${values mkString ","})"))
    })
  }

  private def dropDatabase(spark: SparkSession, databaseName: DatabaseName) :Unit = {
    spark.sql(s"DROP DATABASE IF EXISTS $databaseName CASCADE")
  }

  private def sqlizeRow[T](row: Any) = {
    val product: Product = row match {
      case p: Product => p
      case v: Any => Tuple1(v)
    }
    product.productIterator.map({
      case s: String => s"'$s'"
      case v => v
    })
  }
}
