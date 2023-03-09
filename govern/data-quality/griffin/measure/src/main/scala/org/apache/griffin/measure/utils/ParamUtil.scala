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

import scala.reflect.ClassTag

object ParamUtil {

  object TransUtil {
    def toAny(value: Any): Option[Any] = Some(value)
    def toAnyRef[T: ClassTag](value: Any): Option[T] = {
      value match {
        case v: T => Some(v)
        case _ => None
      }
    }
    def toStringOpt(value: Any): Option[String] = {
      value match {
        case v: String => Some(v)
        case v => Some(v.toString)
      }
    }
    def toByte(value: Any): Option[Byte] = {
      try {
        value match {
          case v: String => Some(v.toByte)
          case v: Byte => Some(v.toByte)
          case v: Short => Some(v.toByte)
          case v: Int => Some(v.toByte)
          case v: Long => Some(v.toByte)
          case v: Float => Some(v.toByte)
          case v: Double => Some(v.toByte)
          case _ => None
        }
      } catch {
        case _: NumberFormatException => None
      }
    }
    def toShort(value: Any): Option[Short] = {
      try {
        value match {
          case v: String => Some(v.toShort)
          case v: Byte => Some(v.toShort)
          case v: Short => Some(v.toShort)
          case v: Int => Some(v.toShort)
          case v: Long => Some(v.toShort)
          case v: Float => Some(v.toShort)
          case v: Double => Some(v.toShort)
          case _ => None
        }
      } catch {
        case _: NumberFormatException => None
      }
    }
    def toInt(value: Any): Option[Int] = {
      try {
        value match {
          case v: String => Some(v.toInt)
          case v: Byte => Some(v.toInt)
          case v: Short => Some(v.toInt)
          case v: Int => Some(v.toInt)
          case v: Long => Some(v.toInt)
          case v: Float => Some(v.toInt)
          case v: Double => Some(v.toInt)
          case _ => None
        }
      } catch {
        case _: NumberFormatException => None
      }
    }
    def toLong(value: Any): Option[Long] = {
      try {
        value match {
          case v: String => Some(v.toLong)
          case v: Byte => Some(v.toLong)
          case v: Short => Some(v.toLong)
          case v: Int => Some(v.toLong)
          case v: Long => Some(v.toLong)
          case v: Float => Some(v.toLong)
          case v: Double => Some(v.toLong)
          case _ => None
        }
      } catch {
        case _: NumberFormatException => None
      }
    }
    def toFloat(value: Any): Option[Float] = {
      try {
        value match {
          case v: String => Some(v.toFloat)
          case v: Byte => Some(v.toFloat)
          case v: Short => Some(v.toFloat)
          case v: Int => Some(v.toFloat)
          case v: Long => Some(v.toFloat)
          case v: Float => Some(v.toFloat)
          case v: Double => Some(v.toFloat)
          case _ => None
        }
      } catch {
        case _: NumberFormatException => None
      }
    }
    def toDouble(value: Any): Option[Double] = {
      try {
        value match {
          case v: String => Some(v.toDouble)
          case v: Byte => Some(v.toDouble)
          case v: Short => Some(v.toDouble)
          case v: Int => Some(v.toDouble)
          case v: Long => Some(v.toDouble)
          case v: Float => Some(v.toDouble)
          case v: Double => Some(v.toDouble)
          case _ => None
        }
      } catch {
        case _: NumberFormatException => None
      }
    }
    def toBoolean(value: Any): Option[Boolean] = {
      try {
        value match {
          case v: String => Some(v.toBoolean)
          case v: Boolean => Some(v)
          case _ => None
        }
      } catch {
        case _: IllegalArgumentException => None
      }
    }
  }
  import TransUtil._

  implicit class ParamMap(params: Map[String, Any]) {
    def getAny(key: String, defValue: Any): Any = {
      params.get(key).flatMap(toAny).getOrElse(defValue)
    }

    def getAnyRef[T: ClassTag](key: String, defValue: T): T = {
      params.get(key).flatMap(toAnyRef[T]).getOrElse(defValue)
    }

    def getString(key: String, defValue: String): String = {
      params.get(key).flatMap(toStringOpt).getOrElse(defValue)
    }

    def getLazyString(key: String, defValue: () => String): String = {
      params.get(key).flatMap(toStringOpt).getOrElse(defValue())
    }

    def getStringOrKey(key: String): String = getString(key, key)

    def getByte(key: String, defValue: Byte): Byte = {
      params.get(key).flatMap(toByte).getOrElse(defValue)
    }

    def getShort(key: String, defValue: Short): Short = {
      params.get(key).flatMap(toShort).getOrElse(defValue)
    }

    def getInt(key: String, defValue: Int): Int = {
      params.get(key).flatMap(toInt).getOrElse(defValue)
    }

    def getLong(key: String, defValue: Long): Long = {
      params.get(key).flatMap(toLong).getOrElse(defValue)
    }

    def getFloat(key: String, defValue: Float): Float = {
      params.get(key).flatMap(toFloat).getOrElse(defValue)
    }

    def getDouble(key: String, defValue: Double): Double = {
      params.get(key).flatMap(toDouble).getOrElse(defValue)
    }

    def getBoolean(key: String, defValue: Boolean): Boolean = {
      params.get(key).flatMap(toBoolean).getOrElse(defValue)
    }

    def getParamAnyMap(
        key: String,
        defValue: Map[String, Any] = Map[String, Any]()): Map[String, Any] = {
      params.get(key) match {
        case Some(v: Map[_, _]) => v.map(pair => (pair._1.toString, pair._2))
        case _ => defValue
      }
    }

    def getParamStringMap(
        key: String,
        defValue: Map[String, String] = Map[String, String]()): Map[String, String] = {
      params.get(key) match {
        case Some(v: Map[_, _]) => v.map(pair => (pair._1.toString, pair._2.toString))
        case _ => defValue
      }
    }

    def getStringArr(key: String, defValue: Seq[String] = Nil): Seq[String] = {
      params.get(key) match {
        case Some(seq: Seq[_]) => seq.flatMap(toStringOpt)
        case _ => defValue
      }
    }

    def getDoubleArr(key: String, defValue: Seq[Double] = Nil): Seq[Double] = {
      params.get(key) match {
        case Some(seq: Seq[_]) => seq.flatMap(toDouble)
        case _ => defValue
      }
    }

    def addIfNotExist(key: String, value: Any): Map[String, Any] = {
      params.get(key) match {
        case None => params + (key -> value)
        case _ => params
      }
    }

    def removeKeys(keys: Iterable[String]): Map[String, Any] = {
      params -- keys
    }
  }

}
