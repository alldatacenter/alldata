/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.griffin.measure.utils

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._

object DataFrameUtil {

  def unionDfOpts(dfOpt1: Option[DataFrame], dfOpt2: Option[DataFrame]): Option[DataFrame] = {
    (dfOpt1, dfOpt2) match {
      case (Some(df1), Some(df2)) => Some(unionByName(df1, df2))
      case (Some(_), _) => dfOpt1
      case (_, Some(_)) => dfOpt2
      case _ => None
    }
  }

  def unionByName(a: DataFrame, b: DataFrame): DataFrame = {
    val columns = a.columns.toSet.intersect(b.columns.toSet).map(col).toSeq
    a.select(columns: _*).union(b.select(columns: _*))
  }

}
