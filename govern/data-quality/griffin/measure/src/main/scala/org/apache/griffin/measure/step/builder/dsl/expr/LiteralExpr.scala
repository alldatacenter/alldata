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

package org.apache.griffin.measure.step.builder.dsl.expr

import org.apache.griffin.measure.utils.TimeUtil

trait LiteralExpr extends Expr {
  def coalesceDesc: String = desc
}

case class LiteralNullExpr(str: String) extends LiteralExpr {
  def desc: String = "NULL"
}

case class LiteralNanExpr(str: String) extends LiteralExpr {
  def desc: String = "NaN"
}

case class LiteralStringExpr(str: String) extends LiteralExpr {
  def desc: String = str
}

case class LiteralNumberExpr(str: String) extends LiteralExpr {
  def desc: String = {
    try {
      if (str.contains(".")) {
        str.toDouble.toString
      } else {
        str.toLong.toString
      }
    } catch {
      case _: Throwable => throw new Exception(s"$str is invalid number")
    }
  }
}

case class LiteralTimeExpr(str: String) extends LiteralExpr {
  def desc: String = {
    TimeUtil.milliseconds(str) match {
      case Some(t) => t.toString
      case _ => throw new Exception(s"$str is invalid time")
    }
  }
}

case class LiteralBooleanExpr(str: String) extends LiteralExpr {
  final val TrueRegex = """(?i)true""".r
  final val FalseRegex = """(?i)false""".r
  def desc: String = {
    str match {
      case TrueRegex() => true.toString
      case FalseRegex() => false.toString
      case _ => throw new Exception(s"$str is invalid boolean")
    }
  }
}
