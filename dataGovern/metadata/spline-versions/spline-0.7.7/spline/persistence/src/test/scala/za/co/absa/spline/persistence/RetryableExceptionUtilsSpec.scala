/*
 * Copyright 2022 ABSA Group Limited
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

package za.co.absa.spline.persistence

import com.arangodb.ArangoDBException
import com.arangodb.entity.ErrorEntity
import com.arangodb.velocypack.{VPack, VPackBuilder, ValueType}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import za.co.absa.spline.persistence.RetryableExceptionUtils.RetryableCodes
import za.co.absa.spline.persistence.RetryableExceptionUtilsSpec._

class RetryableExceptionUtilsSpec
  extends AnyFlatSpec
    with BeforeAndAfterEach
    with Matchers
    with OptionValues {


  behavior of "RetryableException.unapply()"

  RetryableCodes.foreach { errNum =>
    it should s"return Some(ex) when ex is ArangoDBException with error $errNum" in {
      val ex =
        new ArangoDBException(
          new ErrorEntity().copy(errorNum = errNum)
        )
      RetryableExceptionUtils.isRetryable(ex) shouldBe true
    }
  }

  it should s"return Some(ex) when ex is ArangoDBException wrapped with another exception" in {
    val ex =
      new Exception(
        new RuntimeException(
          new ArangoDBException(
            new ErrorEntity().copy(errorNum = RetryableCodes.head)
          )
        )
      )
    RetryableExceptionUtils.isRetryable(ex) shouldBe true
  }

  it should "return None when neither ex nor any of its causes is ArangoDBException with retryable error codes" in {
    RetryableExceptionUtils.isRetryable(new Exception()) shouldBe false
    RetryableExceptionUtils.isRetryable(new ArangoDBException(new ErrorEntity().copy(errorNum = ArangoCode.Internal.code))) shouldBe false
  }

  it should "gracefully handle nulls" in {
    RetryableExceptionUtils.isRetryable(null) shouldBe false
  }

  it should "gracefully handle exceptions with null cause" in {
    RetryableExceptionUtils.isRetryable(new Exception) shouldBe false
  }

  it should "gracefully handle exceptions with looped cause" in {
    lazy val loopedEx: Exception =
      new Exception {
        override def getCause: Throwable =
          new Exception {
            override def getCause: Throwable = loopedEx
          }
      }
    RetryableExceptionUtils.isRetryable(loopedEx) shouldBe false
  }

}

object RetryableExceptionUtilsSpec {

  private val vpack = new VPack.Builder().build

  implicit class ErrorEntityOps(val ee: ErrorEntity) {
    def copy(
      errorMessage: String = ee.getErrorMessage,
      exception: String = ee.getException,
      code: Int = ee.getCode,
      errorNum: Int = ee.getErrorNum
    ): ErrorEntity = {
      vpack.deserialize[ErrorEntity](
        new VPackBuilder()
          .add(ValueType.OBJECT)
          .add("errorMessage", errorMessage)
          .add("exception", exception)
          .add("code", Int.box(code))
          .add("errorNum", Int.box(errorNum))
          .close()
          .slice(),
        classOf[ErrorEntity]
      )
    }
  }
}
