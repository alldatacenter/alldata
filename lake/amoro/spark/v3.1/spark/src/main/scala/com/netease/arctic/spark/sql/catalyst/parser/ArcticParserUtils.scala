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

package com.netease.arctic.spark.sql.catalyst.parser

import org.antlr.v4.runtime.{ParserRuleContext, Token}
import org.antlr.v4.runtime.misc.Interval
import org.apache.spark.sql.catalyst.trees.{CurrentOrigin, Origin}

/* Partially copied from Apache Spark's Parser to avoid dependency on Spark Internals */
object ArcticParserUtils {
  def withOrigin[T](ctx: ParserRuleContext)(f: => T): T = {
    val current = CurrentOrigin.get
    CurrentOrigin.set(position(ctx.getStart))
    try {
      f
    } finally {
      CurrentOrigin.set(current)
    }
  }

  def position(token: Token): Origin = {
    val opt = Option(token)
    Origin(opt.map(_.getLine), opt.map(_.getCharPositionInLine))
  }

  /** Get the command which created the token. */
  def command(ctx: ParserRuleContext): String = {
    val stream = ctx.getStart.getInputStream
    stream.getText(Interval.of(0, stream.size() - 1))
  }
}
