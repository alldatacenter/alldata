package com.linkedin.feathr.offline.derived

import com.linkedin.feathr.common.FeatureAggregationType
import com.linkedin.feathr.common.FeatureAggregationType._
import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrConfigException, FeathrException}
import com.linkedin.feathr.offline.anchored.feature.FeatureAnchorWithSource
import com.linkedin.feathr.offline.config.{BaseTaggedDependency, TaggedDependency}
import com.linkedin.feathr.offline.derived.functions.SeqJoinDerivationFunction
import com.linkedin.feathr.offline.derived.strategies.SequentialJoinAsDerivation
import com.linkedin.feathr.offline.job.FeatureTransformation.FEATURE_NAME_PREFIX
import com.linkedin.feathr.offline.join.algorithms.{SeqJoinExplodedJoinKeyColumnAppender, SequentialJoinConditionBuilder, SparkJoinWithJoinCondition}
import com.linkedin.feathr.offline.logical.FeatureGroups
import com.linkedin.feathr.offline.mvel.plugins.FeathrExpressionExecutionContext
import com.linkedin.feathr.offline.{TestFeathr, TestUtils}
import org.apache.spark.SparkException
import org.apache.spark.sql.functions.{when => _, _}
import org.apache.spark.sql.types._
import org.apache.spark.sql.{AnalysisException, DataFrame, Row, SparkSession}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.testng.Assert._
import org.testng.annotations.{DataProvider, Test}
import org.apache.spark.sql.internal.SQLConf

class TestSequentialJoinAsDerivation extends TestFeathr with MockitoSugar {
  val mvelContext = new FeathrExpressionExecutionContext()
  private def getSampleEmployeeDF = {
    val schema = {
      StructType(
        Array(
          StructField("viewer", StringType, nullable = false),
          StructField("as", DataTypes.createArrayType(StringType), nullable = false),
          StructField(
            "exp",
            DataTypes.createStructType(Array(StructField("name", StringType, nullable = false), StructField("years", IntegerType, nullable = false))),
            nullable = false)))
    }
    val data = List(Row("emp1", List("java", "scala"), Row("li", 2)), Row("emp2", List("python", "nodejs"), Row("li", 3)))
    val rdd = ss.sparkContext.parallelize(data)
    ss.createDataFrame(rdd, schema)
  }

  private def getSampleDF = {
    val schema = {
      StructType(
        Array(
          StructField("name", StringType, nullable = false),
          StructField(
            "staff",
            DataTypes.createStructType(Array(StructField("name", StringType, nullable = false), StructField("location", StringType, nullable = false))),
            nullable = false),
          StructField("asMap", DataTypes.createMapType(StringType, IntegerType), nullable = false)))
    }

    val data = List(Row("li", Row("emp1", "SV"), Map("java" -> 1000, "scala" -> 10)), Row("gb", Row("emp2", "MPK"), Map("nodejs" -> 500, "scala" -> 20)))
    val rdd = ss.sparkContext.parallelize(data)
    ss.createDataFrame(rdd, schema)
  }

  /**
   * This test verifies that when expansion key contains a "." (dot)
   * in its name, the join fails with [[AnalysisException]].
   * The test also verifies that the utility method [[SequentialJoinAsDerivation.replaceDotInColumnName()]]
   * solves this issue.
   */
  @Test
  def testStringTypeLeftJoinKeyWithDotInRightJoinKey(): Unit = {
    val leftColName = "asAsString"
    val rightColName = "a_map.a_name" // Column with "."
    val employeeDF = getSampleEmployeeDF.withColumn(leftColName, explode(col("as")))
    employeeDF.printSchema()

    val DF = getSampleDF.withColumn(rightColName, explode(map_keys(col("asMap"))))
    DF.printSchema()

    val leftJoinKey = Seq(leftColName)
    val rightJoinKey = Seq(rightColName)

    try {
      SequentialJoinConditionBuilder.buildJoinCondition(leftJoinKey, employeeDF, rightJoinKey, DF)
      fail(s"Expected join to fail when column names have a 'dot' leftJoinKey=$leftJoinKey, rightJoinKey=$rightJoinKey")
    } catch {
      case _: AnalysisException => // expected exception
      case e: Exception => throw e
    }
    val modifiedColName = SequentialJoinAsDerivation.replaceDotInColumnName(rightColName)
    val modifiedDF = DF.withColumn(modifiedColName, explode(map_keys(col("asMap"))))
    val modifiedJoinKey = Seq(modifiedColName)

    val joinCondition = SequentialJoinConditionBuilder.buildJoinCondition(leftJoinKey, employeeDF, modifiedJoinKey, modifiedDF)
    assertEquals(joinCondition, employeeDF(leftColName) === modifiedDF(modifiedColName))

    val expectedas = Set("scala", "java", "nodejs")
    val actualas = employeeDF.join(modifiedDF, joinCondition).collect().map(row => row.getAs[String](rightColName)).toSet
    assertTrue(actualas.diff(expectedas).isEmpty)
  }

  /**
   * This test verifies that when expansion key contains a "." (dot)
   * in its name, the join fails with [[AnalysisException]].
   * The test also verifies that the utility method [[SequentialJoinAsDerivation.replaceDotInColumnName()]]
   * solves this issue.
   */
  @Test
  def testArrayTypeLeftJoinKeyWithDotInRightJoinKey(): Unit = {
    val leftColName = "as"
    val rightColName = "a_map.a_name" // Column with "."
    val employeeDF = getSampleEmployeeDF
    employeeDF.printSchema()

    val DF = getSampleDF.withColumn(rightColName, explode(map_keys(col("asMap"))))
    DF.printSchema()

    val leftJoinKey = Seq(leftColName)
    val rightJoinKey = Seq(rightColName)

    val joinCondition = SequentialJoinConditionBuilder.buildJoinCondition(leftJoinKey, employeeDF, rightJoinKey, DF)
    assertEquals(joinCondition, expr(s"array_contains($leftColName, $rightColName)"))

    try {
      employeeDF.join(DF, joinCondition).collect()
      fail(s"Expected join to fail when column names have a 'dot' leftJoinKey=$leftJoinKey, rightJoinKey=$rightJoinKey")
    } catch {
      case _: AnalysisException => // expected exception
      case e: Exception => throw e
    }
    val modifiedColName = SequentialJoinAsDerivation.replaceDotInColumnName(rightColName)
    val modifiedDF = DF.withColumn(modifiedColName, explode(map_keys(col("asMap"))))
    val modifiedJoinKey = Seq(modifiedColName)

    val joinCondition2 = SequentialJoinConditionBuilder.buildJoinCondition(leftJoinKey, employeeDF, modifiedJoinKey, modifiedDF)
    assertEquals(joinCondition2, expr(s"array_contains($leftColName, $modifiedColName)"))

    val expectedas = Set("scala", "java", "nodejs")
    val actualas = employeeDF.join(modifiedDF, joinCondition2).collect().map(row => row.getAs[String](rightColName)).toSet

    assertTrue(actualas.diff(expectedas).isEmpty)
  }

  /**
   * Provides default embedding data, with specified type for embedding column.
   */
  private def defaultEmbeddingDataProvider(embeddingType: DataType): DataFrame = {
    def embeddingSchema(): StructType = {
      val colField1 = StructField("Id", StringType, nullable = false)
      val colField2 = StructField("version", StringType, nullable = false)
      val colField3 = StructField("values", DataTypes.createArrayType(embeddingType, true), nullable = true)
      StructType(List(colField1, colField2, colField3))
    }

    val row1Embedding = List(0.6399446379991766, 0.5422204391080193, 0.015207317453907687, 0.03283037229968777)
    val row2Embedding = List(0.5765849143826666, 0.8317135675005226, 0.34560786534872645, 0.21220228259597274)

    val data = List(
      Row("a:1", "0.0.1", if (embeddingType == FloatType) row1Embedding map (_.floatValue()) else row1Embedding),
      Row("z:1", "0.0.1", if (embeddingType == FloatType) row2Embedding map (_.floatValue()) else row2Embedding))
    val rdd = ss.sparkContext.parallelize(data)
    ss.createDataFrame(rdd, embeddingSchema())
  }

  /**
   * Provides default tensor embedding data, with specified type for dimension type and value type.
   */
  private def defaultTensorEmbeddingDataProvider(dimensionType: DataType, valueType: DataType): DataFrame = {
    def embeddingSchema(): StructType = {
      val colField1 = StructField("Id", StringType, nullable = false)
      val colField2 = StructField("version", StringType, nullable = false)
      val colField3 = StructField(
        "tensorCol",
        StructType(List(
          StructField("indices0", ArrayType(dimensionType, containsNull = false), nullable = false),
          StructField("values", ArrayType(valueType, containsNull = false), nullable = false))),
        nullable = true)
      StructType(List(colField1, colField2, colField3))
    }

    val row1Indices0 = dimensionType match {
      case StringType => List("1", "3")
      case IntegerType => List(1, 3)
      case LongType => List(1L, 3L)
    }
    val row1Values = valueType match {
      case IntegerType => List(10, 30)
      case LongType => List(10L, 30L)
      case FloatType => List(10.0f, 30.0f)
      case DoubleType => List(10.0d, 30.0d)
      case StringType => List("10", "30")
    }
    val row2Indices0 = dimensionType match {
      case StringType => List("1", "2")
      case IntegerType => List(1, 2)
      case LongType => List(1L, 2L)
    }
    val row2Values = valueType match {
      case IntegerType => List(100, 200)
      case LongType => List(100L, 200L)
      case FloatType => List(100.0f, 200.0f)
      case DoubleType => List(100.0d, 200.0d)
      case StringType => List("100", "200")
    }

    val data = List(
      Row("a:1", "0.0.1", Row(row1Indices0, row1Values)),
      Row("z:1", "0.0.1", Row(row2Indices0, row2Values)))
    val rdd = ss.sparkContext.parallelize(data)
    ss.createDataFrame(rdd, embeddingSchema())
  }

  /**
   * Test [[ELEMENTWISE_SUM]] on an column of type Array[Double].
   */
  @Test
  def testElementWiseSumOnArrayOfDouble(): Unit = {
    val aggColumn = "values"
    val groupByColumn = "version"
    val inputDF = defaultEmbeddingDataProvider(DoubleType)
    val mockSparkSession = mock[SparkSession]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockJoiner = mock[SparkJoinWithJoinCondition]
    val seqJoinDerivations = new SequentialJoinAsDerivation(mockSparkSession, mockFeatureGroups, mockJoiner, List())
    val resultDF = seqJoinDerivations.applyElementWiseAggregation(ELEMENTWISE_SUM, aggColumn, inputDF, groupByColumn)

    val expectedResult = inputDF.collect()
    val actualResult = resultDF.collect()
    assertEquals(actualResult.length, 1)

    val expected =
      expectedResult(0).getAs[Seq[Double]](aggColumn) zip
        expectedResult(1).getAs[Seq[Double]](aggColumn) map { case (x, y) => (x + y) }

    val actual = actualResult.head.getAs[Seq[Float]](aggColumn)

    (expected zip actual) foreach { case (a, b) => assertTrue(Math.abs(a - b) < 0.0001) }
    verifyNoInteractions(mockSparkSession)
    verifyNoInteractions(mockFeatureGroups)
    verifyNoInteractions(mockJoiner)
  }

  /**
   * Test [[ELEMENTWISE_AVG]] on an column of type Array[Double].
   */
  @Test
  def testElementWiseAvgOnArrayOfDouble(): Unit = {
    val aggColumn = "values"
    val groupByColumn = "version"
    val inputDF = defaultEmbeddingDataProvider(DoubleType)
    val mockSparkSession = mock[SparkSession]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockJoiner = mock[SparkJoinWithJoinCondition]
    val seqJoinDerivations = new SequentialJoinAsDerivation(mockSparkSession, mockFeatureGroups, mockJoiner, List())
    val resultDF = seqJoinDerivations.applyElementWiseAggregation(ELEMENTWISE_AVG, aggColumn, inputDF, groupByColumn)

    val expectedResult = inputDF.collect()
    val actualResult = resultDF.collect()
    assertEquals(actualResult.length, 1)

    val expected =
      expectedResult(0).getAs[Seq[Double]](aggColumn) zip
        expectedResult(1).getAs[Seq[Double]](aggColumn) map { case (x, y) => (x + y) / 2 }

    val actual = actualResult.head.getAs[Seq[Float]](aggColumn)

    (expected zip actual) foreach { case (a, b) => assertTrue(Math.abs(a - b) < 0.0001) }
    verifyNoInteractions(mockSparkSession)
    verifyNoInteractions(mockFeatureGroups)
    verifyNoInteractions(mockJoiner)
  }

  @Test(
    expectedExceptions = Array(classOf[FeathrConfigException]),
    description = "sequential join should not support union" +
      "aggregation on dense vectors, as the output depends on the order of feature values and hence nondeterministic")
  def testUnionAggOnArrayOfDouble(): Unit = {
    val aggColumn = "values"
    val groupByColumn = "version"
    val inputDF = defaultEmbeddingDataProvider(DoubleType)
    val mockSparkSession = mock[SparkSession]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockJoiner = mock[SparkJoinWithJoinCondition]
    val seqJoinDerivations = new SequentialJoinAsDerivation(mockSparkSession, mockFeatureGroups, mockJoiner, List())
    seqJoinDerivations.applyUnionAggregation(aggColumn, inputDF, groupByColumn).collect()
  }

  /**
   * Test [[ELEMENTWISE_SUM]] on an column of type Array[Float].
   */
  @Test
  def testElementWiseSumOnArrayOfFloat(): Unit = {
    val aggColumn = "values"
    val groupByColumn = "version"
    val inputDF = defaultEmbeddingDataProvider(FloatType)
    val mockSparkSession = mock[SparkSession]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockJoiner = mock[SparkJoinWithJoinCondition]
    val seqJoinDerivations = new SequentialJoinAsDerivation(mockSparkSession, mockFeatureGroups, mockJoiner, List())
    val resultDF = seqJoinDerivations.applyElementWiseAggregation(ELEMENTWISE_SUM, aggColumn, inputDF, groupByColumn)

    val expectedResult = inputDF.collect()
    val actualResult = resultDF.collect()
    assertEquals(actualResult.length, 1)

    val expected =
      expectedResult(0).getAs[Seq[Float]](aggColumn) zip
        expectedResult(1).getAs[Seq[Float]](aggColumn) map { case (x, y) => (x + y) }

    val actual = actualResult.head.getAs[Seq[Float]](aggColumn)

    (expected zip actual) foreach { case (a, b) => assertTrue(Math.abs(a - b) < 0.0001) }
    verifyNoInteractions(mockSparkSession)
    verifyNoInteractions(mockFeatureGroups)
    verifyNoInteractions(mockJoiner)
  }

  /**
   * Test [[ELEMENTWISE_AVG]] on an column of type Array[Float].
   */
  @Test
  def testElementWiseAvgOnArrayOfFloat(): Unit = {
    val aggColumn = "values"
    val groupByColumn = "version"
    val inputDF = defaultEmbeddingDataProvider(FloatType)
    val mockSparkSession = mock[SparkSession]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockJoiner = mock[SparkJoinWithJoinCondition]
    val seqJoinDerivations = new SequentialJoinAsDerivation(mockSparkSession, mockFeatureGroups, mockJoiner, List())
    val resultDF = seqJoinDerivations.applyElementWiseAggregation(ELEMENTWISE_AVG, aggColumn, inputDF, groupByColumn)

    val expectedResult = inputDF.collect()
    val actualResult = resultDF.collect()
    assertEquals(actualResult.length, 1)

    val expected =
      expectedResult(0).getAs[Seq[Float]](aggColumn) zip
        expectedResult(1).getAs[Seq[Float]](aggColumn) map { case (x, y) => (x + y) / 2 }

    val actual = actualResult.head.getAs[Seq[Float]](aggColumn)

    (expected zip actual) foreach { case (a, b) => assertTrue(Math.abs(a - b) < 0.0001) }
    verifyNoInteractions(mockSparkSession)
    verifyNoInteractions(mockFeatureGroups)
    verifyNoInteractions(mockJoiner)
  }

  /**
   * Test [[ELEMENTWISE_MAX]] on an column of type Array[Double].
   */
  @Test
  def testElementWiseMaxOnArrayOfDouble(): Unit = {
    val aggColumn = "values"
    val groupByColumn = "version"
    val inputDF = defaultEmbeddingDataProvider(DoubleType)
    val mockSparkSession = mock[SparkSession]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockJoiner = mock[SparkJoinWithJoinCondition]
    val seqJoinDerivations = new SequentialJoinAsDerivation(mockSparkSession, mockFeatureGroups, mockJoiner, List())
    val resultDF = seqJoinDerivations.applyElementWiseAggregation(ELEMENTWISE_MAX, aggColumn, inputDF, groupByColumn)

    val expectedResult = inputDF.collect()
    val actualResult = resultDF.collect()
    assertEquals(actualResult.length, 1)

    val expected =
      expectedResult(0).getAs[Seq[Double]](aggColumn) zip
        expectedResult(1).getAs[Seq[Double]](aggColumn) map { case (x, y) => Math.max(x, y) }

    val actual = actualResult.head.getAs[Seq[Float]](aggColumn)

    (expected zip actual) foreach { case (a, b) => assertTrue(Math.abs(a - b) < 0.0001) }
    verifyNoInteractions(mockSparkSession)
    verifyNoInteractions(mockFeatureGroups)
    verifyNoInteractions(mockJoiner)
  }

  /**
   * Test [[ELEMENTWISE_MAX]] on an column of type Array[Float].
   */
  @Test
  def testElementWiseMaxOnArrayOfFloat(): Unit = {
    val aggColumn = "values"
    val groupByColumn = "version"
    val inputDF = defaultEmbeddingDataProvider(FloatType)
    val mockSparkSession = mock[SparkSession]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockJoiner = mock[SparkJoinWithJoinCondition]
    val seqJoinDerivations = new SequentialJoinAsDerivation(mockSparkSession, mockFeatureGroups, mockJoiner, List())
    val resultDF = seqJoinDerivations.applyElementWiseAggregation(ELEMENTWISE_MAX, aggColumn, inputDF, groupByColumn)

    val expectedResult = inputDF.collect()
    val actualResult = resultDF.collect()
    assertEquals(actualResult.length, 1)

    val expected =
      expectedResult(0).getAs[Seq[Float]](aggColumn) zip
        expectedResult(1).getAs[Seq[Float]](aggColumn) map { case (x, y) => Math.max(x, y) }

    val actual = actualResult.head.getAs[Seq[Float]](aggColumn)

    (expected zip actual) foreach { case (a, b) => assertTrue(Math.abs(a - b) < 0.0001) }
    verifyNoInteractions(mockSparkSession)
    verifyNoInteractions(mockFeatureGroups)
    verifyNoInteractions(mockJoiner)
  }

  /**
   * Test [[ELEMENTWISE_MIN]] on an column of type Array[Double].
   */
  @Test
  def testElementWiseMinOnArrayOfDouble(): Unit = {
    val aggColumn = "values"
    val groupByColumn = "version"
    val inputDF = defaultEmbeddingDataProvider(DoubleType)
    val mockSparkSession = mock[SparkSession]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockJoiner = mock[SparkJoinWithJoinCondition]
    val seqJoinDerivations = new SequentialJoinAsDerivation(mockSparkSession, mockFeatureGroups, mockJoiner, List())
    val resultDF = seqJoinDerivations.applyElementWiseAggregation(ELEMENTWISE_MIN, aggColumn, inputDF, groupByColumn)

    val expectedResult = inputDF.collect()
    val actualResult = resultDF.collect()
    assertEquals(actualResult.length, 1)

    val expected =
      expectedResult(0).getAs[Seq[Double]](aggColumn) zip
        expectedResult(1).getAs[Seq[Double]](aggColumn) map { case (x, y) => Math.min(x, y) }

    val actual = actualResult.head.getAs[Seq[Float]](aggColumn)

    (expected zip actual) foreach { case (a, b) => assertTrue(Math.abs(a - b) < 0.0001) }
    verifyNoInteractions(mockSparkSession)
    verifyNoInteractions(mockFeatureGroups)
    verifyNoInteractions(mockJoiner)
  }

  /**
   * Test [[ELEMENTWISE_MIN]] on an column of type Array[Float].
   */
  @Test
  def testElementWiseMinOnArrayOfFloat(): Unit = {
    val aggColumn = "values"
    val groupByColumn = "version"
    val inputDF = defaultEmbeddingDataProvider(FloatType)
    val mockSparkSession = mock[SparkSession]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockJoiner = mock[SparkJoinWithJoinCondition]
    val seqJoinDerivations = new SequentialJoinAsDerivation(mockSparkSession, mockFeatureGroups, mockJoiner, List())
    val resultDF = seqJoinDerivations.applyElementWiseAggregation(ELEMENTWISE_MIN, aggColumn, inputDF, groupByColumn)

    val expectedResult = inputDF.collect()
    val actualResult = resultDF.collect()
    assertEquals(actualResult.length, 1)

    val expected =
      expectedResult(0).getAs[Seq[Float]](aggColumn) zip
        expectedResult(1).getAs[Seq[Float]](aggColumn) map { case (x, y) => Math.min(x, y) }

    val actual = actualResult.head.getAs[Seq[Float]](aggColumn)

    (expected zip actual) foreach { case (a, b) => assertTrue(Math.abs(a - b) < 0.0001) }
    verifyNoInteractions(mockSparkSession)
    verifyNoInteractions(mockFeatureGroups)
    verifyNoInteractions(mockJoiner)
  }

  // Return order is: aggregation type, dimension type, expected indices0 value, value type, expected values value
  @DataProvider
  def elementwiseTensorTestCases: Array[Array[Any]] = {
    val dimArray: Array[Array[Any]] = Array(
      Array(StringType, Array("1", "3", "2")),
      Array(IntegerType, Array(1, 3, 2)),
      Array(LongType, Array(1L, 3L, 2L))
    )
    val valuesArrayForSum: Array[Array[Any]] = Array(
      Array(IntegerType, Array(110, 30, 200)),
      Array(LongType, Array(110L, 30L, 200L)),
      Array(FloatType, Array(110.0f, 30.0f, 200.0f)),
      Array(DoubleType, Array(110.0d, 30.0d, 200.0d))
    )
    val valuesArrayForAvg: Array[Array[Any]] = Array(
      Array(IntegerType, Array(55, 15, 100)),
      Array(LongType, Array(55L, 15L, 100L)),
      Array(FloatType, Array(55.0f, 15.0f, 100.0f)),
      Array(DoubleType, Array(55.0d, 15.0d, 100.0d))
    )
    val valuesArrayForMin: Array[Array[Any]] = Array(
      Array(IntegerType, Array(10, 30, 200)),
      Array(LongType, Array(10L, 30L, 200L)),
      Array(FloatType, Array(10.0f, 30.0f, 200.0f)),
      Array(DoubleType, Array(10.0d, 30.0d, 200.0d))
    )
    val valuesArrayForMax: Array[Array[Any]] = Array(
      Array(IntegerType, Array(100, 30, 200)),
      Array(LongType, Array(100L, 30L, 200L)),
      Array(FloatType, Array(100.0f, 30.0f, 200.0f)),
      Array(DoubleType, Array(100.0d, 30.0d, 200.0d))
    )
    val sumCases = for (dim <- dimArray; values <- valuesArrayForSum) yield Array(ELEMENTWISE_SUM) ++ dim ++ values
    val avgCases = for (dim <- dimArray; values <- valuesArrayForAvg) yield Array(ELEMENTWISE_AVG) ++ dim ++ values
    val minCases = for (dim <- dimArray; values <- valuesArrayForMin) yield Array(ELEMENTWISE_MIN) ++ dim ++ values
    val maxCases = for (dim <- dimArray; values <- valuesArrayForMax) yield Array(ELEMENTWISE_MAX) ++ dim ++ values
    sumCases ++ avgCases ++ minCases ++ maxCases
  }

  /**
   * Test all elementwise aggregations against all valid 1d fds sparse tensors. Test cases provided in elementwiseTensorTestCases
   * and defaultTensorEmbeddingDataProvider.
   */
  @Test(description = "test all elementwise aggregations against all valid 1d fds sparse tensors", dataProvider = "elementwiseTensorTestCases")
  def testElementWiseOnTensors(aggType: FeatureAggregationType,
                                  dimType: DataType, expectedIndices0: Array[_],
                                  valType: DataType, expectedValues: Array[_]): Unit = {
    val aggColumn = "tensorCol"
    val groupByColumn = "version"
    val inputDF = defaultTensorEmbeddingDataProvider(dimType, valType)
    val mockSparkSession = mock[SparkSession]
    SQLConf.get.setConfString("spark.sql.legacy.allowUntypedScalaUDF", "true")
    val mockFeatureGroups = mock[FeatureGroups]
    val mockJoiner = mock[SparkJoinWithJoinCondition]
    val seqJoinDerivations = new SequentialJoinAsDerivation(mockSparkSession, mockFeatureGroups, mockJoiner, List())
    val resultDF = seqJoinDerivations.applyElementWiseAggregation(aggType, aggColumn, inputDF, groupByColumn)

    val actualResult = resultDF.collect()
    assertEquals(actualResult.length, 1)

    val actual = actualResult.head.getAs[Row](aggColumn)
    val expected = TestUtils.build1dSparseTensorFDSRow(expectedIndices0, expectedValues)

    assertEquals(actual, expected)
    verifyNoInteractions(mockSparkSession)
    verifyNoInteractions(mockFeatureGroups)
    verifyNoInteractions(mockJoiner)
  }

  @DataProvider
  def elementwiseAggregationTypeProvider: Array[Array[Any]] = {
    Array(
      Array(ELEMENTWISE_AVG),
      Array(ELEMENTWISE_SUM),
      Array(ELEMENTWISE_MIN),
      Array(ELEMENTWISE_MAX)
    )
  }

  /**
   * Test ELEMENTWISE_X on an column of type Tensor but of value Null.
   */
  @Test(description = "test all elementwise aggregations against all null tensors", dataProvider = "elementwiseAggregationTypeProvider")
  def testElementWiseSumOnNullTensor(aggType: FeatureAggregationType): Unit = {
    val aggColumn = "tensorCol"
    val groupByColumn = "version"
    val inputDF = defaultTensorEmbeddingDataProvider(StringType, FloatType).withColumn(aggColumn,
      lit(null).cast("struct<indices0:array<string>, values:array<float>>"))
    inputDF.printSchema()
    val mockSparkSession = mock[SparkSession]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockJoiner = mock[SparkJoinWithJoinCondition]
    val seqJoinDerivations = new SequentialJoinAsDerivation(mockSparkSession, mockFeatureGroups, mockJoiner, List())
    val resultDF = seqJoinDerivations.applyElementWiseAggregation(aggType, aggColumn, inputDF, groupByColumn)
    val actualResult = resultDF.collect()
    assertEquals(actualResult.length, 1)
    assertEquals(actualResult.head.getAs[Row](aggColumn), TestUtils.build1dSparseTensorFDSRow(Array.empty, Array.empty))
    verifyNoInteractions(mockSparkSession)
    verifyNoInteractions(mockFeatureGroups)
    verifyNoInteractions(mockJoiner)
  }


  /**
   * Test all elementwise aggregations against tensor with value type of STRING which should error out.
   */
  @Test(expectedExceptions = Array(classOf[SparkException]),
    description = "test all elementwise aggregations against all null tensors", dataProvider = "elementwiseAggregationTypeProvider")
  def testElementWiseThrowsOnTensorWithStringValues(aggType: FeatureAggregationType): Unit = {
    val aggColumn = "tensorCol"
    val groupByColumn = "version"
    val inputDF = defaultTensorEmbeddingDataProvider(StringType, StringType)
    inputDF.printSchema()
    val mockSparkSession = mock[SparkSession]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockJoiner = mock[SparkJoinWithJoinCondition]
    val seqJoinDerivations = new SequentialJoinAsDerivation(mockSparkSession, mockFeatureGroups, mockJoiner, List())
    val resultDF = seqJoinDerivations.applyElementWiseAggregation(aggType, aggColumn, inputDF, groupByColumn)
    resultDF.collect()
    verifyNoInteractions(mockSparkSession)
    verifyNoInteractions(mockFeatureGroups)
    verifyNoInteractions(mockJoiner)
  }

  /**
   * Test [[ELEMENTWISE_SUM]] on an column of type Array[Double] but of value Null.
   */
  @Test
  def testElementWiseSumOnNullArrayOfDouble(): Unit = {
    val aggColumn = "values"
    val groupByColumn = "version"
    val inputDF = defaultEmbeddingDataProvider(DoubleType).withColumn(aggColumn, lit(null).cast("array<double>"))
    inputDF.printSchema()
    val mockSparkSession = mock[SparkSession]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockJoiner = mock[SparkJoinWithJoinCondition]
    val seqJoinDerivations = new SequentialJoinAsDerivation(mockSparkSession, mockFeatureGroups, mockJoiner, List())
    val resultDF = seqJoinDerivations.applyElementWiseAggregation(ELEMENTWISE_SUM, aggColumn, inputDF, groupByColumn)
    val actualResult = resultDF.collect()
    assertEquals(actualResult.length, 1)
    assertTrue(actualResult.head.getAs[Seq[Float]](aggColumn).isEmpty)
    verifyNoInteractions(mockSparkSession)
    verifyNoInteractions(mockFeatureGroups)
    verifyNoInteractions(mockJoiner)
  }

  /**
   * Test [[ELEMENTWISE_AVG]] on an column of type Array[Double] but of value Null.
   */
  @Test
  def testElementWiseAvgOnNullArrayOfDouble(): Unit = {
    val aggColumn = "values"
    val groupByColumn = "version"
    val inputDF = defaultEmbeddingDataProvider(DoubleType).withColumn(aggColumn, lit(null).cast("array<double>"))
    inputDF.printSchema()
    val mockSparkSession = mock[SparkSession]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockJoiner = mock[SparkJoinWithJoinCondition]
    val seqJoinDerivations = new SequentialJoinAsDerivation(mockSparkSession, mockFeatureGroups, mockJoiner, List())
    val resultDF = seqJoinDerivations.applyElementWiseAggregation(ELEMENTWISE_AVG, aggColumn, inputDF, groupByColumn)
    val actualResult = resultDF.collect()
    assertEquals(actualResult.length, 1)
    assertTrue(actualResult.head.getAs[Seq[Float]](aggColumn).isEmpty)
    verifyNoInteractions(mockSparkSession)
    verifyNoInteractions(mockFeatureGroups)
    verifyNoInteractions(mockJoiner)
  }

  /**
   * Test [[ELEMENTWISE_MAX]] on an column of type Array[Double] but of value Null.
   */
  @Test
  def testElementWiseMaxOnNullArrayOfDouble(): Unit = {
    val aggColumn = "values"
    val groupByColumn = "version"
    val inputDF = defaultEmbeddingDataProvider(DoubleType).withColumn(aggColumn, lit(null).cast("array<double>"))
    inputDF.printSchema()
    val mockSparkSession = mock[SparkSession]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockJoiner = mock[SparkJoinWithJoinCondition]
    val seqJoinDerivations = new SequentialJoinAsDerivation(mockSparkSession, mockFeatureGroups, mockJoiner, List())
    val resultDF = seqJoinDerivations.applyElementWiseAggregation(ELEMENTWISE_MAX, aggColumn, inputDF, groupByColumn)
    val actualResult = resultDF.collect()
    assertEquals(actualResult.length, 1)
    assertTrue(actualResult.head.getAs[Seq[Float]](aggColumn).isEmpty)
    verifyNoInteractions(mockSparkSession)
    verifyNoInteractions(mockFeatureGroups)
    verifyNoInteractions(mockJoiner)
  }

  /**
   * Test [[ELEMENTWISE_MIN]] on an column of type Array[Double] but of value Null.
   */
  @Test
  def testElementWiseMinOnNullArrayOfDouble(): Unit = {
    val aggColumn = "values"
    val groupByColumn = "version"
    val inputDF = defaultEmbeddingDataProvider(DoubleType).withColumn(aggColumn, lit(null).cast("array<double>"))
    inputDF.printSchema()
    val mockSparkSession = mock[SparkSession]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockJoiner = mock[SparkJoinWithJoinCondition]
    val seqJoinDerivations = new SequentialJoinAsDerivation(mockSparkSession, mockFeatureGroups, mockJoiner, List())
    val resultDF = seqJoinDerivations.applyElementWiseAggregation(ELEMENTWISE_MIN, aggColumn, inputDF, groupByColumn)
    val actualResult = resultDF.collect()
    assertEquals(actualResult.length, 1)
    assertTrue(actualResult.head.getAs[Seq[Float]](aggColumn).isEmpty)
    verifyNoInteractions(mockSparkSession)
    verifyNoInteractions(mockFeatureGroups)
    verifyNoInteractions(mockJoiner)
  }

  /**
   * Test [[ELEMENTWISE_SUM]] on an column of type Array[String].
   */
  @Test(expectedExceptions = Array(classOf[UnsupportedOperationException]))
  def testElementWiseSumOnArrayOfString(): Unit = {
    val aggColumn = "values"
    val groupByColumn = "version"
    val inputDF = defaultEmbeddingDataProvider(DoubleType).withColumn(aggColumn, split(lit("1,2,3"), ","))
    inputDF.printSchema()
    val mockSparkSession = mock[SparkSession]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockJoiner = mock[SparkJoinWithJoinCondition]
    val seqJoinDerivations = new SequentialJoinAsDerivation(mockSparkSession, mockFeatureGroups, mockJoiner, List())
    val resultDF = seqJoinDerivations.applyElementWiseAggregation(ELEMENTWISE_SUM, aggColumn, inputDF, groupByColumn)
    resultDF.collect()
    verifyNoInteractions(mockSparkSession)
    verifyNoInteractions(mockFeatureGroups)
    verifyNoInteractions(mockJoiner)
  }

  /**
   * Test [[ELEMENTWISE_SUM]] on an column of type String.
   */
  @Test(expectedExceptions = Array(classOf[UnsupportedOperationException]))
  def testElementWiseSumOnString(): Unit = {
    val aggColumn = "values"
    val groupByColumn = "version"
    val inputDF = defaultEmbeddingDataProvider(DoubleType).withColumn(aggColumn, lit("1,2,3"))
    inputDF.printSchema()
    val mockSparkSession = mock[SparkSession]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockJoiner = mock[SparkJoinWithJoinCondition]
    val seqJoinDerivations = new SequentialJoinAsDerivation(mockSparkSession, mockFeatureGroups, mockJoiner, List())
    val resultDF = seqJoinDerivations.applyElementWiseAggregation(ELEMENTWISE_SUM, aggColumn, inputDF, groupByColumn)
    resultDF.collect()
    verifyNoInteractions(mockSparkSession)
    verifyNoInteractions(mockFeatureGroups)
    verifyNoInteractions(mockJoiner)
  }

  /**
   * Test [[ELEMENTWISE_AVG]] on an column of type Array[String].
   */
  @Test(expectedExceptions = Array(classOf[UnsupportedOperationException]))
  def testElementWiseAvgOnArrayOfString(): Unit = {
    val aggColumn = "values"
    val groupByColumn = "version"
    val inputDF = defaultEmbeddingDataProvider(DoubleType).withColumn(aggColumn, split(lit("1,2,3"), ","))
    inputDF.printSchema()
    val mockSparkSession = mock[SparkSession]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockJoiner = mock[SparkJoinWithJoinCondition]
    val seqJoinDerivations = new SequentialJoinAsDerivation(mockSparkSession, mockFeatureGroups, mockJoiner, List())
    val resultDF = seqJoinDerivations.applyElementWiseAggregation(ELEMENTWISE_AVG, aggColumn, inputDF, groupByColumn)
    resultDF.collect()
    verifyNoInteractions(mockSparkSession)
    verifyNoInteractions(mockFeatureGroups)
    verifyNoInteractions(mockJoiner)
  }

  /**
   * Test [[ELEMENTWISE_AVG]] on an column of type String.
   */
  @Test(expectedExceptions = Array(classOf[UnsupportedOperationException]))
  def testElementWiseAvgOnString(): Unit = {
    val aggColumn = "values"
    val groupByColumn = "version"
    val inputDF = defaultEmbeddingDataProvider(DoubleType).withColumn(aggColumn, lit("1,2,3"))
    inputDF.printSchema()
    val mockSparkSession = mock[SparkSession]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockJoiner = mock[SparkJoinWithJoinCondition]
    val seqJoinDerivations = new SequentialJoinAsDerivation(mockSparkSession, mockFeatureGroups, mockJoiner, List())
    val resultDF = seqJoinDerivations.applyElementWiseAggregation(ELEMENTWISE_AVG, aggColumn, inputDF, groupByColumn)
    resultDF.collect()
    verifyNoInteractions(mockSparkSession)
    verifyNoInteractions(mockFeatureGroups)
    verifyNoInteractions(mockJoiner)
  }

  /**
   * Basic unit test for method SeqJoinExplodedJoinKeyColumnAppender.
   * Focuses on testing the returned DataFrame.
   * - Verifies column is exploded (by verifying row count).
   * - Verifies exploded column name is appended with "_explode".
   * - Verify exactly one column is added.
   */
  @Test
  def testExplodeArrayJoinKeyColumnsForDataFrame(): Unit = {
    val aggColumn = "values"
    val joinKeys = Seq("Id", "version", "values")
    val inputDF = defaultEmbeddingDataProvider(DoubleType)
    val expectedRowCount = inputDF.count() * inputDF.head().getAs[Seq[Float]](aggColumn).length

    val joinKeyColumnAppender = new SeqJoinExplodedJoinKeyColumnAppender("feature1")
    val (_, resultDF) = joinKeyColumnAppender.appendJoinKeyColunmns(joinKeys, inputDF)

    assertEquals(resultDF.count(), expectedRowCount)
    assertEquals(resultDF.columns.length, joinKeys.length + 1)
    assertTrue(resultDF.columns.exists(x => x.contains("_exploded")))
  }

  /**
   * This test verifies the correctness of the keys returned.
   */
  @Test
  def testExplodeArrayJoinKeyColumnsForJoinKeys(): Unit = {
    val joinKeys = Seq("Id", "version", "values")
    val inputDF = defaultEmbeddingDataProvider(DoubleType)

    val joinKeyColumnAppender = new SeqJoinExplodedJoinKeyColumnAppender("feature1")
    val (resultJoinKeys, _) = joinKeyColumnAppender.appendJoinKeyColunmns(joinKeys, inputDF)
    assertEquals(resultJoinKeys.mkString(", "), s"${joinKeys.mkString(", ")}_exploded")
  }

  /**
   * Test that explode column is not created if join key does not have columns of array type.
   */
  @Test
  def testNoExplodeWhenNoArrayKeys(): Unit = {
    val aggColumn = "values"
    val joinKeys = Seq("Id", "version", "values")
    val inputDF = defaultEmbeddingDataProvider(DoubleType).withColumn(aggColumn, lit("1,2,3"))

    val joinKeyColumnAppender = new SeqJoinExplodedJoinKeyColumnAppender("feature1")
    val (resultJoinKeys, resultDF) = joinKeyColumnAppender.appendJoinKeyColunmns(joinKeys, inputDF)

    assertEquals(resultDF.count(), inputDF.count())
    assertEquals(resultDF.columns.mkString(", "), inputDF.columns.mkString(", "))
    assertEquals(resultJoinKeys.mkString(", "), joinKeys.mkString(", "))
  }

  /**
   * Test to verifies that the row is not filtered out by explode if it is empty.
   */
  @Test
  def testExplodeWhenArrayIsEmpty(): Unit = {
    val aggColumn = "values"
    val joinKeys = Seq("Id", "version", "values")
    val inputDF = defaultEmbeddingDataProvider(DoubleType).withColumn(aggColumn, split(lit(""), ","))
    val mockSparkSession = mock[SparkSession]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockJoiner = mock[SparkJoinWithJoinCondition]
    val joinKeyColumnAppender = new SeqJoinExplodedJoinKeyColumnAppender("feature1")
    val (_, resultDF) = joinKeyColumnAppender.appendJoinKeyColunmns(joinKeys, inputDF)

    assertEquals(resultDF.count(), inputDF.count())
    assertEquals(resultDF.columns.length, joinKeys.length + 1)
    assertTrue(resultDF.columns.exists(x => x.contains("_exploded")))

    resultDF.collect.foreach(r => assertTrue(r.getAs[String](s"${aggColumn}_exploded").isEmpty))
    verifyNoInteractions(mockSparkSession)
    verifyNoInteractions(mockFeatureGroups)
    verifyNoInteractions(mockJoiner)
  }

  /**
   * Test to verifies that currently unsupported usecase of having multiple array type columns in join keys
   * throws appropriate error.
   */
  @Test
  def testExceptionThrownWhenMultipleArrayTypeJoinKeys(): Unit = {
    val otherColumn = "otherValues"
    val joinKeys = Seq("Id", "values", otherColumn)
    val inputDF = defaultEmbeddingDataProvider(DoubleType).withColumn(otherColumn, split(lit("1,2,3"), ","))
    val featureName = "feature1"
    val joinKeyColumnAppender = new SeqJoinExplodedJoinKeyColumnAppender(featureName)
    try {
      joinKeyColumnAppender.appendJoinKeyColunmns(joinKeys, inputDF)
    } catch {
      case e: FeathrException =>
        val message = e.getMessage
        assertTrue(message.contains(ErrorLabel.FEATHR_ERROR.name()))
        assertTrue(message.contains("feature1"))
        assertTrue(message.contains("[values, otherValues]"))
      case other => throw other
    }
  }

  /**
   * Test getJoinKeyForAnchoredFeatures when output key is not defined.
   */
  @Test
  def testGetJoinKeyForAnchoredWhenOutputKeyIsNotDefined(): Unit = {
    val mockSparkSession = mock[SparkSession]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockJoiner = mock[SparkJoinWithJoinCondition]
    val seqJoinDerivations = new SequentialJoinAsDerivation(mockSparkSession, mockFeatureGroups, mockJoiner, List())

    val mockDerivedFeature = mock[DerivedFeature]
    val mockDerivationFunction = mock[SeqJoinDerivationFunction]
    val mockBaseTaggedDependency = mock[BaseTaggedDependency]
    val mockTaggedDependency = mock[TaggedDependency]
    // mock derivation function
    when(mockDerivedFeature.derivation.asInstanceOf[SeqJoinDerivationFunction]).thenReturn(mockDerivationFunction)
    when(mockDerivationFunction.left).thenReturn(mockBaseTaggedDependency)
    when(mockDerivationFunction.right).thenReturn(mockTaggedDependency)
    // mock left, right and output keys
    when(mockBaseTaggedDependency.key).thenReturn(Seq("key1", "key2"))
    when(mockBaseTaggedDependency.outputKey).thenReturn(None)
    when(mockTaggedDependency.key).thenReturn(Seq("expansionKey1"))

    val featureValueCol = Seq("featureValueCol1")
    val returnedKeys = seqJoinDerivations.getJoinKeyForAnchoredFeatures(mockDerivedFeature, None, featureValueCol)
    assertEquals(returnedKeys, featureValueCol)
    verify(mockDerivedFeature).derivation
    verify(mockDerivationFunction, atLeast(2)).left
    verify(mockBaseTaggedDependency).outputKey
  }

  /**
   * Test getJoinKeyForAnchoredFeatures when output key is not defined and expansion keys > 1.
   */
  @Test(
    expectedExceptions = Array(classOf[FeathrException]),
    expectedExceptionsMessageRegExp = ".*Output key is required in the base feature if expansion field has more than one key.*")
  def testOutputKeyIsRequiredWhenThereAreMoreThanOneExpansionKeys(): Unit = {
    val mockSparkSession = mock[SparkSession]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockJoiner = mock[SparkJoinWithJoinCondition]
    val seqJoinDerivations = new SequentialJoinAsDerivation(mockSparkSession, mockFeatureGroups, mockJoiner, List())

    val mockDerivedFeature = mock[DerivedFeature]
    val mockDerivationFunction = mock[SeqJoinDerivationFunction]
    val mockBaseTaggedDependency = mock[BaseTaggedDependency]
    val mockTaggedDependency = mock[TaggedDependency]
    // mock derivation function
    when(mockDerivedFeature.derivation.asInstanceOf[SeqJoinDerivationFunction]).thenReturn(mockDerivationFunction)
    when(mockDerivationFunction.left).thenReturn(mockBaseTaggedDependency)
    when(mockDerivationFunction.right).thenReturn(mockTaggedDependency)
    // mock left, right and output keys
    when(mockBaseTaggedDependency.key).thenReturn(Seq("key1", "key2"))
    when(mockBaseTaggedDependency.outputKey).thenReturn(None)
    when(mockTaggedDependency.key).thenReturn(Seq("expansionKey1", "expansionKey2"))

    val featureValueCol = Seq("featureValueCol1")
    seqJoinDerivations.getJoinKeyForAnchoredFeatures(mockDerivedFeature, None, featureValueCol)
  }

  /**
   * Test base feature value column is returned when output key is equal to expansion key.
   */
  @Test
  def testBaseFeatureValueColumnIsReturnedWhenOutputKeyIsEqualToExpansionKey(): Unit = {
    val mockSparkSession = mock[SparkSession]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockJoiner = mock[SparkJoinWithJoinCondition]
    val seqJoinDerivations = new SequentialJoinAsDerivation(mockSparkSession, mockFeatureGroups, mockJoiner, List())

    val mockDerivedFeature = mock[DerivedFeature]
    val mockDerivationFunction = mock[SeqJoinDerivationFunction]
    val mockBaseTaggedDependency = mock[BaseTaggedDependency]
    val mockTaggedDependency = mock[TaggedDependency]
    // mock derivation function
    when(mockDerivedFeature.derivation.asInstanceOf[SeqJoinDerivationFunction]).thenReturn(mockDerivationFunction)
    when(mockDerivationFunction.left).thenReturn(mockBaseTaggedDependency)
    when(mockDerivationFunction.right).thenReturn(mockTaggedDependency)
    // mock left, right and output keys
    when(mockBaseTaggedDependency.key).thenReturn(Seq("key1", "key2"))
    when(mockBaseTaggedDependency.outputKey).thenReturn(Some(Seq("outputKey1")))
    when(mockTaggedDependency.key).thenReturn(Seq("outputKey1"))

    val featureValueCol = Seq("featureValueCol1")
    val returnedKeys = seqJoinDerivations.getJoinKeyForAnchoredFeatures(mockDerivedFeature, None, featureValueCol)
    assertEquals(returnedKeys, featureValueCol)
    verify(mockDerivedFeature).derivation
    verify(mockDerivationFunction, atLeast(2)).left
    verify(mockBaseTaggedDependency).outputKey
  }

  /**
   * Test when expansion keys are not part of output keys or sequential join keyTags.
   */
  @Test(expectedExceptions = Array(classOf[FeathrException]), expectedExceptionsMessageRegExp = ".*does not belong to the output key.*")
  def testWhenExpansionKeysDoNotBelongToOutputKeyOrFeatureKeys(): Unit = {
    val mockSparkSession = mock[SparkSession]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockJoiner = mock[SparkJoinWithJoinCondition]
    val seqJoinDerivations = new SequentialJoinAsDerivation(mockSparkSession, mockFeatureGroups, mockJoiner, List())

    val mockDerivedFeature = mock[DerivedFeature]
    val mockDerivationFunction = mock[SeqJoinDerivationFunction]
    val mockBaseTaggedDependency = mock[BaseTaggedDependency]
    val mockTaggedDependency = mock[TaggedDependency]
    // mock derivation function
    when(mockDerivedFeature.derivation.asInstanceOf[SeqJoinDerivationFunction]).thenReturn(mockDerivationFunction)
    when(mockDerivedFeature.producedFeatureNames).thenReturn(Seq("seqJoinFeature"))
    when(mockDerivationFunction.left).thenReturn(mockBaseTaggedDependency)
    when(mockDerivationFunction.right).thenReturn(mockTaggedDependency)
    // mock left, right and output keys
    when(mockBaseTaggedDependency.key).thenReturn(Seq("key1", "key2"))
    when(mockBaseTaggedDependency.outputKey).thenReturn(Some(Seq("outputKey1")))
    when(mockTaggedDependency.key).thenReturn(Seq("expansionKey1"))

    val featureValueCol = Seq("featureValueCol1")
    seqJoinDerivations.getJoinKeyForAnchoredFeatures(mockDerivedFeature, Some(Seq("key1", "key2", "key3")), featureValueCol)
  }

  /**
   * Test when expansion keys are not part of output keys or sequential join keyTags.
   */
  @Test(expectedExceptions = Array(classOf[FeathrException]), expectedExceptionsMessageRegExp = ".*We do not support multiple output key in sequential join.*")
  def testDoesNotSupportMoreThanOneOutputKey(): Unit = {
    val mockSparkSession = mock[SparkSession]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockJoiner = mock[SparkJoinWithJoinCondition]
    val seqJoinDerivations = new SequentialJoinAsDerivation(mockSparkSession, mockFeatureGroups, mockJoiner, List())

    val mockDerivedFeature = mock[DerivedFeature]
    val mockDerivationFunction = mock[SeqJoinDerivationFunction]
    val mockBaseTaggedDependency = mock[BaseTaggedDependency]
    val mockTaggedDependency = mock[TaggedDependency]
    // mock derivation function
    when(mockDerivedFeature.derivation.asInstanceOf[SeqJoinDerivationFunction]).thenReturn(mockDerivationFunction)
    when(mockDerivedFeature.producedFeatureNames).thenReturn(Seq("seqJoinFeature"))
    when(mockDerivationFunction.left).thenReturn(mockBaseTaggedDependency)
    when(mockDerivationFunction.right).thenReturn(mockTaggedDependency)
    // mock left, right and output keys
    when(mockBaseTaggedDependency.key).thenReturn(Seq("key1", "key2"))
    when(mockBaseTaggedDependency.outputKey).thenReturn(Some(Seq("outputKey1", "outputKey2")))
    when(mockTaggedDependency.key).thenReturn(Seq("expansionKey1"))

    seqJoinDerivations(Seq(0, 1, 2), Seq("keyTag1", "keyTag2", "keyTag3", "keyTag4"), ss.emptyDataFrame, mockDerivedFeature, mockDerivationFunction, Some(mvelContext))
  }

  /**
   * Test prepareLeftForJoin for passthrough features.
   */
  @Test
  def testPrepareLeftJoinForPassthroughFeatures(): Unit = {
    val mockSparkSession = mock[SparkSession]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockJoiner = mock[SparkJoinWithJoinCondition]
    val seqJoinDerivations = new SequentialJoinAsDerivation(mockSparkSession, mockFeatureGroups, mockJoiner, List())

    val mockFeatureAnchorWithSource = mock[FeatureAnchorWithSource]
    val baseFeatureName = "as"
    val allPassthroughFeatures = Map(baseFeatureName -> mockFeatureAnchorWithSource)
    val keyColumn = StructField("x", StringType, nullable = false)
    val asColumn = StructField(FEATURE_NAME_PREFIX.concat("as"), StringType, nullable = false)
    val leftDf = ss.createDataFrame(ss.sparkContext.emptyRDD[Row], StructType(List(keyColumn, asColumn)))

    val (baseFeatureJoinKey, obsWithLeftJoined, toDropColumns) =
      seqJoinDerivations.prepareLeftForJoin(leftDf, baseFeatureName, allPassthroughFeatures)
    val expectedColumn = s"_feathr_seq_join_coerced_left_join_key_$baseFeatureName"
    assertEquals(baseFeatureJoinKey, Seq(expectedColumn))
    assertEquals(toDropColumns, Seq(expectedColumn))
    assertEquals(obsWithLeftJoined.columns.mkString(", "), s"x, ${FEATURE_NAME_PREFIX.concat("as")}, $expectedColumn")
  }

  /**
   * Test prepareLeftForJoin throws an exception when base feature is not in input DataFrame
   */
  @Test(expectedExceptions = Array(classOf[FeathrException]), expectedExceptionsMessageRegExp = ".*Could not find base feature column.*")
  def testPrepareLeftJoinThrowsExceptionWhenBaseFeatureNotFound(): Unit = {
    val mockSparkSession = mock[SparkSession]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockJoiner = mock[SparkJoinWithJoinCondition]
    val seqJoinDerivations = new SequentialJoinAsDerivation(mockSparkSession, mockFeatureGroups, mockJoiner, List())

    val baseFeatureName = "as"
    val keyColumn = StructField("x", StringType, nullable = false)
    val asColumn = StructField(FEATURE_NAME_PREFIX.concat("asNotFound"), StringType, nullable = false)
    val leftDf = ss.createDataFrame(ss.sparkContext.emptyRDD[Row], StructType(List(keyColumn, asColumn)))

    seqJoinDerivations.prepareLeftForJoin(leftDf, baseFeatureName, Map.empty[String, FeatureAnchorWithSource])
  }

  /**
   * Test Sequential Join does not support derived feature as expansion feature.
   */
  @Test(
    expectedExceptions = Array(classOf[FeathrException]),
    expectedExceptionsMessageRegExp = ".*Derived expansion feature is not supported by Sequential Join.*")
  def testDerivedExpansionFeatureIsNotSupported(): Unit = {
    val mockSparkSession = mock[SparkSession]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockJoiner = mock[SparkJoinWithJoinCondition]
    // Mock expansion feature is not in allAnchoredFeature
    when(mockFeatureGroups.allAnchoredFeatures).thenReturn(Map.empty[String, FeatureAnchorWithSource])
    val seqJoinDerivations = new SequentialJoinAsDerivation(mockSparkSession, mockFeatureGroups, mockJoiner, List())

    val mockDerivedFeature = mock[DerivedFeature]
    val mockDerivationFunction = mock[SeqJoinDerivationFunction]
    val mockBaseTaggedDependency = mock[BaseTaggedDependency]
    val mockTaggedDependency = mock[TaggedDependency]
    // mock derivation function
    when(mockDerivedFeature.derivation.asInstanceOf[SeqJoinDerivationFunction]).thenReturn(mockDerivationFunction)
    when(mockDerivedFeature.producedFeatureNames).thenReturn(Seq("seqJoinFeature"))
    when(mockDerivationFunction.left).thenReturn(mockBaseTaggedDependency)
    when(mockDerivationFunction.right).thenReturn(mockTaggedDependency)
    // mock left, right and output keys
    when(mockBaseTaggedDependency.key).thenReturn(Seq("key1", "key2"))
    when(mockBaseTaggedDependency.outputKey).thenReturn(Some(Seq("outputKey1")))
    when(mockTaggedDependency.key).thenReturn(Seq("expansionKey1"))

    seqJoinDerivations(Seq(0, 1, 2), Seq("keyTag1", "keyTag2", "keyTag3", "keyTag4"), ss.emptyDataFrame, mockDerivedFeature, mockDerivationFunction, Some(mvelContext))
  }

  /**
   * Test applyAggregationFunction throws an exception when aggregation function is empty.
   */
  @Test(expectedExceptions = Array(classOf[FeathrConfigException]), expectedExceptionsMessageRegExp = ".*Empty aggregation is not supported for feature.*")
  def testApplyAggregationThrowsErrorWhenEmpty(): Unit = {
    val mockSparkSession = mock[SparkSession]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockJoiner = mock[SparkJoinWithJoinCondition]
    val seqJoinDerivations = new SequentialJoinAsDerivation(mockSparkSession, mockFeatureGroups, mockJoiner, List())

    val mockDerivedFeature = mock[DerivedFeature]
    when(mockDerivedFeature.producedFeatureNames).thenReturn(Seq("seqJoinFeatureName"))
    seqJoinDerivations.applyAggregationFunction(mockDerivedFeature, "seqJoinColName", ss.emptyDataFrame, "", "groupByCol")
  }
}
