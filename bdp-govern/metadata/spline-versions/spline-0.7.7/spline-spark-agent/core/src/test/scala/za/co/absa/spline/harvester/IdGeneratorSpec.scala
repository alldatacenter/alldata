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

package za.co.absa.spline.harvester

import com.fasterxml.uuid.Generators
import org.mockito.Mockito._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

import java.security.MessageDigest
import java.util.UUID

class IdGeneratorSpec
  extends AnyFlatSpec
    with Matchers
    with MockitoSugar {

  behavior of "ComposableIdGenerator"

  it should "compose ID generators" in {
    val dummyArg = new Object
    val gen1Mock = mock[IdGenerator[Any, Int]]
    val gen2Mock = mock[IdGenerator[Int, String]]

    when(gen1Mock.nextId(dummyArg)) thenReturn 42
    when(gen2Mock.nextId(42)) thenReturn "foo"

    val compositeIdGenerator = new ComposableIdGenerator(gen1Mock, gen2Mock)

    compositeIdGenerator.nextId(dummyArg) shouldEqual "foo"
  }

  behavior of "SequentialIdGenerator"

  it should "generate 0-based number sequence" in {
    val gen = new SequentialIdGenerator("a-{0}")

    gen.nextId() shouldEqual "a-0"
    gen.nextId() shouldEqual "a-1"
    gen.nextId() shouldEqual "a-2"
  }

  behavior of "UUID3IdGenerator"

  it should "generate correct UUID version 3" in {
    val name = new Object
    val namespace = UUID.randomUUID()
    val uuid3Actual = new UUID3IdGenerator(namespace).nextId(name)
    val uuid3Expected = Generators.nameBasedGenerator(namespace, MessageDigest.getInstance("MD5")).generate("{}")
    uuid3Actual shouldEqual uuid3Expected
  }

  behavior of "UUID5IdGenerator"

  it should "generate correct UUID version 5" in {
    val name = new Object
    val namespace = UUID.randomUUID()
    val uuid5Actual = new UUID5IdGenerator(namespace).nextId(name)
    val uuid5Expected = Generators.nameBasedGenerator(namespace, MessageDigest.getInstance("SHA-1")).generate("{}")
    uuid5Actual shouldEqual uuid5Expected
  }

  behavior of "UUID4IdGenerator"

  it should "generate unique UUID version 4" in {
    val uuids = 1 to 5 map (_ => new UUID4IdGenerator().nextId())
    uuids.distinct.length shouldEqual 5
  }

}
