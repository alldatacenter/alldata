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

package com.twitter.finatra.jackson

import com.fasterxml.jackson.module.scala.JacksonModule
import com.twitter.finatra.jackson.caseclass.CaseClassJacksonModule

/**
  * This object serves the only purpose - to make access to some of the com.twitter.finatra Jackson modules,
  * that are declared private even though are perfectly reusable.
  */
object FinatraInternalModules {

  def caseClassModule: JacksonModule = new CaseClassJacksonModule(
    ScalaObjectMapper.DefaultInjectableTypes,
    Some(ScalaObjectMapper.DefaultValidator)
  )
}
