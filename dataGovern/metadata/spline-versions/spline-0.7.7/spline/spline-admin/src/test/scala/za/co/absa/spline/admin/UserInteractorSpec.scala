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

package za.co.absa.spline.admin

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import za.co.absa.spline.persistence.ArangoConnectionURL

class UserInteractorSpec
  extends AnyFlatSpec
    with MockitoSugar
    with Matchers
    with BeforeAndAfter {

  private val emptyUrl = ArangoConnectionURL("", None, None, Nil, "")
  private val consoleMock = mock[InputConsole]

  before {
    reset(consoleMock)
  }

  behavior of "credentializeConnectionUrl()"

  it should "prompt for username and password with the correct message" in {
    when(consoleMock.readLine(anyString())) thenReturn "foo"
    when(consoleMock.readPassword(anyString())) thenReturn "bar"

    val resultUrl = new UserInteractor(consoleMock).credentializeConnectionUrl(emptyUrl)

    verify(consoleMock).readLine("Username: ")
    verify(consoleMock).readPassword("Password for foo: ")
    verifyNoMoreInteractions(consoleMock)
  }

  it should "when neither username nor password is provided in URL, add both" in {
    when(consoleMock.readLine(anyString())) thenReturn "foo"
    when(consoleMock.readPassword(anyString())) thenReturn "bar"

    val resultUrl = new UserInteractor(consoleMock).credentializeConnectionUrl(emptyUrl)

    resultUrl.user shouldEqual Some("foo")
    resultUrl.password shouldEqual Some("bar")
  }

  it should "when only username is provided in URL, add password" in {
    when(consoleMock.readPassword(anyString())) thenReturn "bar"

    val givenUrl = emptyUrl.copy(user = Some("foo"))
    val resultUrl = new UserInteractor(consoleMock).credentializeConnectionUrl(givenUrl)

    resultUrl.user shouldEqual Some("foo")
    resultUrl.password shouldEqual Some("bar")

    verify(consoleMock, never()).readLine(anyString())
  }

  it should "when both username and provided in URL, do nothing" in {
    val givenUrl = emptyUrl.copy(user = Some("foo"), password = Some("bar"))
    val resultUrl = new UserInteractor(consoleMock).credentializeConnectionUrl(givenUrl)

    resultUrl.user shouldEqual Some("foo")
    resultUrl.password shouldEqual Some("bar")

    verifyNoInteractions(consoleMock)
  }

  it should "trim username, but don't trim password" in {
    when(consoleMock.readLine(anyString())) thenReturn "  foo  "
    when(consoleMock.readPassword(anyString())) thenReturn "   "

    val resultUrl = new UserInteractor(consoleMock).credentializeConnectionUrl(emptyUrl)

    resultUrl.user shouldEqual Some("foo")
    resultUrl.password shouldEqual Some("   ")
  }

  it should "repeat username prompt when empty or blank input received, but don't repeat password prompt" in {
    when(consoleMock.readLine(anyString())) thenReturn "" thenReturn "  " thenReturn "foo"
    when(consoleMock.readPassword(anyString())) thenReturn ""

    val resultUrl = new UserInteractor(consoleMock).credentializeConnectionUrl(emptyUrl)

    resultUrl.user shouldEqual Some("foo")
    resultUrl.password shouldEqual Some("")

    verify(consoleMock, times(3)).readLine(anyString())
    verify(consoleMock, times(1)).readPassword(anyString())
    verifyNoMoreInteractions(consoleMock)
  }
}
