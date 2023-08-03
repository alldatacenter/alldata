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

package org.apache.celeborn.common.util

import java.util.function.Consumer

import scala.language.implicitConversions

/**
 * Implicit conversion for scala(2.11) function to java function
 */
object FunctionConverter {

  implicit def scalaFunctionToJava[From, To](function: From => To)
      : java.util.function.Function[From, To] = {
    new java.util.function.Function[From, To] {
      override def apply(input: From): To = function(input)
    }
  }

  implicit def scalaConsumerToJava[T](consumer: T => AnyVal): java.util.function.Consumer[T] = {
    new Consumer[T] {
      override def accept(t: T): Unit = consumer(t)
    }
  }
}
