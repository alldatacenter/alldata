/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql

import org.apache.spark.internal.Logging
import java.lang.{Boolean => JBoolean}

object KylinSparkEnv extends Logging {
	@volatile
	private var spark: SparkSession = _

	val _needCompute = new ThreadLocal[JBoolean] {
		override protected def initialValue = false
	}

	def getSparkSession: SparkSession = withClassLoad {
		spark
	}

	def setSparkSession(sparkSession: SparkSession): Unit = {
		spark = sparkSession
	}

	def withClassLoad[T](body: => T): T = {
		// val originClassLoad = Thread.currentThread().getContextClassLoader
		// fixme aron
		// Thread.currentThread().setContextClassLoader(ClassLoaderUtils.getSparkClassLoader)
		val t = body
		// Thread.currentThread().setContextClassLoader(originClassLoad)
		t
	}

	def skipCompute(): Unit = {
		_needCompute.set(true)
	}
}
