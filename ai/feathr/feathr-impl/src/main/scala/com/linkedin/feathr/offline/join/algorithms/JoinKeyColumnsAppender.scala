package com.linkedin.feathr.offline.join.algorithms



import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrException, FeathrFeatureJoinException}
import com.linkedin.feathr.offline.client.DataFrameColName
import com.linkedin.feathr.offline.job.FeatureTransformation
import com.linkedin.feathr.offline.job.FeatureTransformation.JOIN_KEY_OBSERVATION_PREFIX
import com.linkedin.feathr.offline.join.DataFrameKeyCombiner
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.{col, explode_outer, expr}
import org.apache.spark.sql.types.{ArrayType, DataType}

/**
 * Feathr has different join algorithms for joining feature data with observation.
 * Each algorithm may apply specific extraction / transformation on join columns before join.
 * Such flavors of join column extraction should extend this trait.
 */
private[offline] sealed trait JoinKeyColumnsAppender {

  /**
   * The method transforms the input columns and appends the transformed columns
   * to the input DataFrame and returns a pair of collection of columns and output DataFrame.
   * @param keys feature join keys or key expressions.
   * @param df   input DataFrame.
   * @return left join key columns and DataFrame with the join columns appended.
   */
  def appendJoinKeyColunmns(keys: Seq[String], df: DataFrame): (Seq[String], DataFrame)
}

/**
 * This column appended does not append the columns to the DataFrame because the column is already there.
 * It also validates that the specified key column names are found in the specified DataFrame.
 */
private[offline] object IdentityJoinKeyColumnAppender extends JoinKeyColumnsAppender {
  override def appendJoinKeyColunmns(keys: Seq[String], df: DataFrame): (Seq[String], DataFrame) = {
    val columnsSet = df.columns.toSet
    val (_, keysNotInDf) = keys.partition(columnsSet.contains)
    if (keysNotInDf.nonEmpty) {
      throw new FeathrFeatureJoinException(
        ErrorLabel.FEATHR_ERROR,
        s"Columns [${keysNotInDf.mkString(", ")}] not found in DataFrame (with columns): [${columnsSet.mkString(", ")}]")
    }
    (keys, df)
  }
}

/**
 * This extractor prefixes each join key column with [[FeatureTransformation.JOIN_KEY_OBSERVATION_PREFIX]]
 * and replaces "non-word characters" with "_"(underscore).
 * After appending the new columns to input DataFrame, a new DataFrame is returned.
 */
private[offline] object SqlTransformedLeftJoinKeyColumnAppender extends JoinKeyColumnsAppender {
  override def appendJoinKeyColunmns(keys: Seq[String], df: DataFrame): (Seq[String], DataFrame) = {
    val leftJoinColumns = keys.map(joinKey => JOIN_KEY_OBSERVATION_PREFIX + joinKey.replaceAll("[^\\w]", "_"))
    // append left join key columns
    val leftDF = keys.zip(leftJoinColumns).foldLeft(df)((baseDF, joinKeyPair) => baseDF.withColumn(joinKeyPair._2, expr(joinKeyPair._1)))
    (leftJoinColumns, leftDF)
  }
}

/**
 * The left join key columns for Slick join are available in the observation data, in the required form.
 * So the join columns are not appended to the observation DataFrame.
 */
private[offline] object SlickJoinLeftJoinKeyColumnAppender extends JoinKeyColumnsAppender {
  override def appendJoinKeyColunmns(keys: Seq[String], df: DataFrame): (Seq[String], DataFrame) = {
    val columnName = DataFrameColName.generateJoinKeyColumnName(keys)
    (Seq(columnName), df)
  }
}

/**
 * The right join key columns for Slick join are extracted by combining the input keys
 * using the [[DataFrameKeyCombiner]] in to a single column.
 */
private[offline] object SlickJoinRightJoinKeyColumnAppender extends JoinKeyColumnsAppender {
  override def appendJoinKeyColunmns(keys: Seq[String], df: DataFrame): (Seq[String], DataFrame) = {
    val (newJoinKey, newDF) = DataFrameKeyCombiner().combine(df, keys)
    (Seq(newJoinKey), newDF)
  }
}

/**
 * Salted join has common extraction logic for both left and right join key columns.
 * The join key columns are extracted by combining the join keys.
 */
private[offline] object SaltedJoinKeyColumnAppender extends JoinKeyColumnsAppender {
  override def appendJoinKeyColunmns(keys: Seq[String], df: DataFrame): (Seq[String], DataFrame) = {
    val (newJoinKey, newDF) = DataFrameKeyCombiner().combine(df, keys, filterNull = false)
    (Seq(newJoinKey), newDF)
  }
}

/**
 * Explodes columns in provided DataFrame, if the join key columns are of [[ArrayType]].
 * This method creates a new column with "_exploded" suffix appended to existing column name.
 * The updated dataframe and join keys are returned.
 * For example: consider the following DataFrame
 * +--------+----------+
 * |     key|  feature1|
 * +--------+----------+
 * |       1| [1, 2, 3]|
 * +--------+----------+
 * The output of the join key column appender
 * +-----+----------+-------------------+
 * |  key|  feature1|  feature1_exploded|
 * +-----+----------+-------------------+
 * |    1| [1, 2, 3]|                  1|
 * |    1| [1, 2, 3]|                  2|
 * |    1| [1, 2, 3]|                  3|
 * +-----+----------+-------------------+
 * @param featureName Sequential Join feature name (used for adding context in case of errors).
 */
private[offline] class SeqJoinExplodedJoinKeyColumnAppender(featureName: String) extends JoinKeyColumnsAppender {
  override def appendJoinKeyColunmns(keys: Seq[String], df: DataFrame): (Seq[String], DataFrame) = {
    val dfSchema = df.schema
    val colToTypeMap: Map[String, DataType] = (dfSchema.fieldNames zip dfSchema.toList.map(_.dataType)) toMap
    val arrayTypeColNameMap = keys
      .filter(x =>
        colToTypeMap(x.split("\\.").head) match {
          case _: ArrayType => true
          case _ => false
      })
      .map(x => (x, s"${x}_exploded"))
      .toMap

    /*
     * Exploding multiple columns can have a snowballing effect.
     * This guard is required prevent data from blowing up.
     * We can think about easing this restriction in future if we see strong value add.
     */
    if (arrayTypeColNameMap.size > 1) {
      throw new FeathrException(
        ErrorLabel.FEATHR_ERROR,
        s"Feathr does not support multiple array type keys in sequential join, " +
          s"feature: $featureName, ArrayType keys: [${arrayTypeColNameMap.keySet.mkString(", ")}]")
    }
    val explodedDataFrame = arrayTypeColNameMap.foldLeft(df)((accDataFrame, colNameTuple) => {
      accDataFrame.withColumn(colNameTuple._2, explode_outer(col(colNameTuple._1)))
    })
    val updateJoinKeys = keys.map(key => arrayTypeColNameMap.getOrElse(key, key))
    (updateJoinKeys, explodedDataFrame)
  }
}
