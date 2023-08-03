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

package org.apache.celeborn.common.identity

import org.apache.celeborn.CelebornFunSuite
import org.apache.celeborn.common.exception.CelebornException

class UserIdentifierSuite extends CelebornFunSuite {

  test("UserIdentifier's tenantId and name should noy be null or empty") {
    val e1 = intercept[AssertionError] {
      UserIdentifier("", "bb")
    }.getMessage
    assert(e1.contains("UserIdentifier's tenantId should not be null or empty."))
    val e2 = intercept[AssertionError] {
      UserIdentifier("aa", "")
    }.getMessage
    assert(e2.contains("UserIdentifier's name should not be null or empty."))
  }

  test("test UserIdentifier serde") {
    val userIdentifier = UserIdentifier("aa", "bb")
    val userIdentifierStr = "`aa`.`bb`"
    assert(userIdentifier.toString.equals(userIdentifierStr))
    assert(UserIdentifier(userIdentifierStr) == userIdentifier)
  }

  test("Both UserIdentifier's tenantId and name should contains ``") {
    val e1 = intercept[CelebornException] {
      UserIdentifier("aa.bb")
    }.getMessage
    assert(e1.contains("Failed to parse user identifier: aa.bb"))
    val e2 = intercept[CelebornException] {
      UserIdentifier("`aa`.bb")
    }.getMessage
    assert(e2.contains("Failed to parse user identifier: `aa`.bb"))
    val e3 = intercept[CelebornException] {
      UserIdentifier("aa.`bb`")
    }.getMessage
    assert(e3.contains("Failed to parse user identifier: aa.`bb`"))
  }

}
