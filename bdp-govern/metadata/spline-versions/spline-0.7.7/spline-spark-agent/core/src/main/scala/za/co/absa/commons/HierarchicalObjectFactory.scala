/*
 * Copyright 2021 ABSA Group Limited
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

package za.co.absa.commons

import org.apache.commons.configuration.Configuration
import org.apache.spark.internal.Logging
import org.apache.spark.sql.SparkSession
import za.co.absa.commons.HierarchicalObjectFactory.ClassName
import za.co.absa.commons.config.ConfigurationImplicits.ConfigurationRequiredWrapper

import java.lang.reflect.InvocationTargetException
import scala.reflect.ClassTag
import scala.util.Try

final class HierarchicalObjectFactory(
  val configuration: Configuration,
  val sparkSession: SparkSession,
  val parentFactory: HierarchicalObjectFactory = null
) extends Logging {

  def child(namespace: String): HierarchicalObjectFactory = {
    new HierarchicalObjectFactory(configuration.subset(namespace), sparkSession, this)
  }

  def instantiate[A: ClassTag](className: String = configuration.getRequiredString(ClassName)): A = {
    logDebug(s"Instantiating $className")
    val clazz = Class.forName(className.trim)
    try {
      Try(clazz.getConstructor(classOf[HierarchicalObjectFactory]).newInstance(this))
        .recover { case _: NoSuchMethodException =>
          clazz
            .getConstructor(classOf[Configuration], classOf[SparkSession])
            .newInstance(configuration, sparkSession)
        }
        .recover { case _: NoSuchMethodException =>
          clazz.getConstructor(classOf[Configuration]).newInstance(configuration)
        }
        .recover { case _: NoSuchMethodException =>
          clazz.getConstructor().newInstance()
        }
        .get
        .asInstanceOf[A]
    } catch {
      case e: InvocationTargetException => throw e.getTargetException
    }
  }

  def createComponentsByKey[A: ClassTag](confKey: String): Seq[A] = {
    val componentNames = configuration
      .getStringArray(confKey)
      .filter(_.nonEmpty)
    logDebug(s"Instantiating components: ${componentNames.mkString(", ")}")
    for (compName <- componentNames) yield {
      val compFactory = parentFactory.child(compName)
      compFactory.instantiate[A]()
    }
  }
}

object HierarchicalObjectFactory {
  final val ClassName = "className"
}
