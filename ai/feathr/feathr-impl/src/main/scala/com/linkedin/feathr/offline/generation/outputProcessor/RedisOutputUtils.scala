package com.linkedin.feathr.offline.generation.outputProcessor

import com.linkedin.feathr.common.types.protobuf.FeatureValueOuterClass
import org.apache.spark.sql.catalyst.encoders.RowEncoder
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.functions.{concat_ws, expr, when}
import org.apache.spark.sql.types._
import org.apache.spark.sql.{DataFrame, Row, SaveMode, SparkSession}

import java.util.Base64
import scala.collection.JavaConverters._
import scala.collection.mutable

object RedisOutputUtils {
  def writeToRedis(ss: SparkSession, df: DataFrame, tableName: String, keyColumns: Seq[String], allFeatureCols: Set[String], saveMode: SaveMode): Unit = {
    val nullElementGuardString = "_null_"
    val newColExpr = concat_ws("#", keyColumns.map(c => {
      val casted = expr(s"CAST (${c} as string)")
      // If any key in the keys is null, replace with special value and remove the row later
      when(casted.isNull, nullElementGuardString).otherwise(casted)
    }): _*)
    val encodedDf = encodeDataFrame(allFeatureCols, df)

    val outputKeyColumnName = "feature_key"
    val decoratedDf = encodedDf.withColumn(outputKeyColumnName, newColExpr)
      .drop(keyColumns: _*)
    // set the host/post/auth/ssl configs in Redis again in the output directly
    // otherwise, in some environment (like databricks), the configs from the active spark session is not passed here.
    decoratedDf.write
      .format("org.apache.spark.sql.redis")
      .option("table", tableName)
      .option("key.column", outputKeyColumnName)
      .option("host", ss.conf.get("spark.redis.host"))
      .option("port", ss.conf.get("spark.redis.port"))
      .option("auth", ss.conf.get("spark.redis.auth"))
      .option("ssl", ss.conf.get("spark.redis.ssl"))
      .mode(saveMode)
      .save()
  }

  private[feathr] def encodeDataFrame(allFeatureCols: Set[String], df: DataFrame): DataFrame = {
    val schema = df.schema
    val newStructType = getRedisSparkSchema(allFeatureCols, schema)
    val encoder = RowEncoder(newStructType)

    val mappingFunc = getConversionFunction(schema, allFeatureCols)
    val encodedDf = df.map(row => {
      Row.fromSeq(schema.indices.map { i =>
      {
        val func = mappingFunc(i)
        val converted = func(row.get(i))
        converted
      }
      })
    })(encoder)
    encodedDf
  }

  /**
   * Gets the new dataframe schema after protobuf encoding.
   */
  private[feathr] def getRedisSparkSchema(
                                           allFeatureCols: Set[String] = Set(), // feature column name to feature type
                                           dfSchema: StructType
                                         ): StructType = {
    val newDfSchemaFields: Array[StructField] = dfSchema.indices.map {
      i => {
        val structField = dfSchema.fields(i)
        if (allFeatureCols.contains(structField.name)) {
          // we use protobuf byte string representation, so for feature, it's always StringType
          StructField(structField.name, StringType, structField.nullable, structField.metadata)
        } else {
          structField
        }
      }
    }.toArray
    StructType(newDfSchemaFields)
  }

  /**
   * Gets the function that converts the original data into protobuf data. The index of the schema is fixed so we
   * map each index to a fixed function.
   * We are supporting:
   * 1. scalar values
   * 2. dense 1-dimension tensor
   * 3. sparse 1-dimension tensor from integer to various types. Mostly support embedding use cases.
   * (more types can be added if there are actual popular use cases)
   */
  private[feathr] def getConversionFunction(dfSchema: StructType, allFeatureCols: Set[String] = Set()): Map[Int, Any => Any] = {
    dfSchema.indices.map(index => {
      val field = dfSchema.fields(index)
      val fieldName = field.name
      val func = if (allFeatureCols.contains(fieldName)) {
        field.dataType match {
          case FloatType =>
            (rowData: Any) => {
              val stringFeature = rowData.asInstanceOf[Float]
              val res = FeatureValueOuterClass.FeatureValue.newBuilder().setFloatValue(stringFeature).build()
              Base64.getEncoder.encodeToString(res.toByteArray)
            }
          case DoubleType =>
            (rowData: Any) => {
              val stringFeature = rowData.asInstanceOf[Double]
              val res = FeatureValueOuterClass.FeatureValue.newBuilder().setDoubleValue(stringFeature).build()
              Base64.getEncoder.encodeToString(res.toByteArray)
            }
          case StringType =>
            (rowData: Any) => {
              val stringFeature = rowData.asInstanceOf[String]
              val res = FeatureValueOuterClass.FeatureValue.newBuilder().setStringValue(stringFeature).build()
              Base64.getEncoder.encodeToString(res.toByteArray)
            }
          case BooleanType =>
            (rowData: Any) => {
              val stringFeature = rowData.asInstanceOf[Boolean]
              val res = FeatureValueOuterClass.FeatureValue.newBuilder().setBooleanValue(stringFeature).build()
              Base64.getEncoder.encodeToString(res.toByteArray)
            }
          case IntegerType =>
            (rowData: Any) => {
              val stringFeature = rowData.asInstanceOf[Integer]
              val res = FeatureValueOuterClass.FeatureValue.newBuilder().setIntValue(stringFeature).build()
              Base64.getEncoder.encodeToString(res.toByteArray)
            }
          case LongType =>
            (rowData: Any) => {
              val stringFeature = rowData.asInstanceOf[Long]
              val res = FeatureValueOuterClass.FeatureValue.newBuilder().setLongValue(stringFeature).build()
              Base64.getEncoder.encodeToString(res.toByteArray)
            }
          case ArrayType(IntegerType, _) =>
            (rowData: Any) => {
              val genericRow = rowData.asInstanceOf[mutable.WrappedArray[java.lang.Integer]]
              val allElements = genericRow.asJava
              val protoStringArray = FeatureValueOuterClass.IntegerArray.newBuilder().addAllIntegers(allElements)
              val res = FeatureValueOuterClass.FeatureValue.newBuilder().setIntArray(protoStringArray).build()
              Base64.getEncoder.encodeToString(res.toByteArray)
            }
          case ArrayType(FloatType, _) =>
            (rowData: Any) => {
              val genericRow = rowData.asInstanceOf[mutable.WrappedArray[java.lang.Float]]
              val allElements = genericRow.asJava
              val protoStringArray = FeatureValueOuterClass.FloatArray.newBuilder().addAllFloats(allElements)
              val res = FeatureValueOuterClass.FeatureValue.newBuilder().setFloatArray(protoStringArray).build()
              Base64.getEncoder.encodeToString(res.toByteArray)
            }
          case ArrayType(DoubleType, _) =>
            (rowData: Any) => {
              val genericRow = rowData.asInstanceOf[mutable.WrappedArray[java.lang.Double]]
              val allElements = genericRow.asJava
              val protoStringArray = FeatureValueOuterClass.DoubleArray.newBuilder().addAllDoubles(allElements)
              val res = FeatureValueOuterClass.FeatureValue.newBuilder().setDoubleArray(protoStringArray).build()
              Base64.getEncoder.encodeToString(res.toByteArray)
            }
          case ArrayType(StringType, _) =>
            (rowData: Any) => {
              val genericRow = rowData.asInstanceOf[mutable.WrappedArray[java.lang.String]]
              val allElements = genericRow.asJava
              val protoStringArray = FeatureValueOuterClass.StringArray.newBuilder().addAllStrings(allElements)
              val res = FeatureValueOuterClass.FeatureValue.newBuilder().setStringArray(protoStringArray).build()
              Base64.getEncoder.encodeToString(res.toByteArray)
            }
          case ArrayType(BooleanType, _) =>
            (rowData: Any) => {
              val genericRow = rowData.asInstanceOf[mutable.WrappedArray[java.lang.Boolean]]
              val allElements = genericRow.asJava
              val protoStringArray = FeatureValueOuterClass.BooleanArray.newBuilder().addAllBooleans(allElements)
              val res = FeatureValueOuterClass.FeatureValue.newBuilder().setBooleanArray(protoStringArray).build()
              Base64.getEncoder.encodeToString(res.toByteArray)
            }
          case StructType(Array(StructField("indices0", ArrayType(IntegerType, _), _, _), StructField("values", ArrayType(StringType, _), _, _)))=>
            (rowData: Any) => {
              val genericRow = rowData.asInstanceOf[GenericRowWithSchema]
              val indexArray = genericRow(0).asInstanceOf[mutable.WrappedArray[Integer]]
              val valueArray = genericRow(1).asInstanceOf[mutable.WrappedArray[String]]
              val protoStringArray = FeatureValueOuterClass.SparseStringArray.newBuilder()
                .addAllIndexIntegers(indexArray.asJava)
                .addAllValueStrings(valueArray.asJava).build()
              val proto = FeatureValueOuterClass.FeatureValue.newBuilder()
                .setSparseStringArray(protoStringArray).build()
              Base64.getEncoder.encodeToString(proto.toByteArray)
            }
          case StructType(Array(StructField("indices0", ArrayType(IntegerType, _), _, _), StructField("values", ArrayType(BooleanType, _), _, _)))=>
            (rowData: Any) => {
              val genericRow = rowData.asInstanceOf[GenericRowWithSchema]
              val indexArray = genericRow(0).asInstanceOf[mutable.WrappedArray[Integer]]
              val valueArray = genericRow(1).asInstanceOf[mutable.WrappedArray[java.lang.Boolean]]
              val protoBoolArray = FeatureValueOuterClass.SparseBoolArray.newBuilder()
                .addAllIndexIntegers(indexArray.asJava)
                .addAllValueBooleans(valueArray.asJava).build()
              val proto = FeatureValueOuterClass.FeatureValue.newBuilder()
                .setSparseBoolArray(protoBoolArray).build()
              Base64.getEncoder.encodeToString(proto.toByteArray)
            }
          case StructType(Array(StructField("indices0", ArrayType(IntegerType, _), _, _), StructField("values", ArrayType(DoubleType, _), _, _)))=>
            (rowData: Any) => {
              val genericRow = rowData.asInstanceOf[GenericRowWithSchema]
              val indexArray = genericRow(0).asInstanceOf[mutable.WrappedArray[Integer]]
              val valueArray = genericRow(1).asInstanceOf[mutable.WrappedArray[java.lang.Double]]
              val protoArray = FeatureValueOuterClass.SparseDoubleArray.newBuilder()
                .addAllIndexIntegers(indexArray.asJava)
                .addAllValueDoubles(valueArray.asJava).build()
              val proto = FeatureValueOuterClass.FeatureValue.newBuilder()
                .setSparseDoubleArray(protoArray).build()
              Base64.getEncoder.encodeToString(proto.toByteArray)
            }
          case StructType(Array(StructField("indices0", ArrayType(IntegerType, _), _, _), StructField("values", ArrayType(FloatType, _), _, _)))=>
            (rowData: Any) => {
              val genericRow = rowData.asInstanceOf[GenericRowWithSchema]
              val indexArray = genericRow(0).asInstanceOf[mutable.WrappedArray[Integer]]
              val valueArray = genericRow(1).asInstanceOf[mutable.WrappedArray[java.lang.Float]]
              val protoArray = FeatureValueOuterClass.SparseFloatArray.newBuilder()
                .addAllIndexIntegers(indexArray.asJava)
                .addAllValueFloats(valueArray.asJava).build()
              val proto = FeatureValueOuterClass.FeatureValue.newBuilder()
                .setSparseFloatArray(protoArray).build()
              Base64.getEncoder.encodeToString(proto.toByteArray)
            }
          case StructType(Array(StructField("indices0", ArrayType(IntegerType, _), _, _), StructField("values", ArrayType(IntegerType, _), _, _)))=>
            (rowData: Any) => {
              val genericRow = rowData.asInstanceOf[GenericRowWithSchema]
              val indexArray = genericRow(0).asInstanceOf[mutable.WrappedArray[Integer]]
              val valueArray = genericRow(1).asInstanceOf[mutable.WrappedArray[java.lang.Integer]]
              val protoArray = FeatureValueOuterClass.SparseIntegerArray.newBuilder()
                .addAllIndexIntegers(indexArray.asJava)
                .addAllValueIntegers(valueArray.asJava).build()
              val proto = FeatureValueOuterClass.FeatureValue.newBuilder()
                .setSparseIntegerArray(protoArray).build()
              Base64.getEncoder.encodeToString(proto.toByteArray)
            }
          case StructType(Array(StructField("indices0", ArrayType(IntegerType, _), _, _), StructField("values", ArrayType(LongType, _), _, _)))=>
            (rowData: Any) => {
              val genericRow = rowData.asInstanceOf[GenericRowWithSchema]
              val indexArray = genericRow(0).asInstanceOf[mutable.WrappedArray[Integer]]
              val valueArray = genericRow(1).asInstanceOf[mutable.WrappedArray[java.lang.Long]]
              val protoArray = FeatureValueOuterClass.SparseLongArray.newBuilder()
                .addAllIndexIntegers(indexArray.asJava)
                .addAllValueLongs(valueArray.asJava).build()
              val proto = FeatureValueOuterClass.FeatureValue.newBuilder()
                .setSparseLongArray(protoArray).build()
              Base64.getEncoder.encodeToString(proto.toByteArray)
            }
          case _ =>
            (rowData: Any) => {
              throw new RuntimeException(f"The data type(${field.dataType}) and data (${rowData}) is not supported in Redis push yet so it can't be encoded.")
            }
        }
      } else {
        (rowData: Any) => rowData
      }
      (index, func)
    }).toMap
  }
}
