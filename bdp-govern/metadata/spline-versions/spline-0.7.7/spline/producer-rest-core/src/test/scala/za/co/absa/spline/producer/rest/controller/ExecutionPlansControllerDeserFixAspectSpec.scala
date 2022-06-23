/*
 * Copyright 2020 ABSA Group Limited
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

package za.co.absa.spline.producer.rest.controller

import org.aspectj.lang.reflect.SourceLocation
import org.aspectj.lang.{JoinPoint, ProceedingJoinPoint, Signature}
import org.aspectj.runtime.internal.AroundClosure
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import za.co.absa.spline.producer.model.v1_1._
import za.co.absa.spline.producer.rest.controller.ExecutionPlansControllerDeserFixAspectSpec.{ProceedingJoinPointSpy, sampleExecutionPlanWithRefsAs}

import java.util.UUID

class ExecutionPlansControllerDeserFixAspectSpec
  extends AnyFlatSpec
    with MockitoSugar
    with Matchers {

  it should "fix ExecutionPlan model in every mathed argument" in {
    // prepare test input data
    val uuid1 = UUID.randomUUID
    val uuid2 = UUID.randomUUID

    val ep1 = sampleExecutionPlanWithRefsAs(
      (attrId: Attribute.Id) => Map("__attrId" -> attrId),
      (exprId: ExpressionLike.Id) => Map("__exprId" -> exprId)
    )(uuid1)

    val ep2 = sampleExecutionPlanWithRefsAs(
      (attrId: Attribute.Id) => Map("__attrId" -> attrId),
      (exprId: ExpressionLike.Id) => Map("__exprId" -> exprId)
    )(uuid2)

    // prepare expected data
    val ep1Fixed = sampleExecutionPlanWithRefsAs(
      AttrOrExprRef.attrRef,
      AttrOrExprRef.exprRef
    )(uuid1)

    val ep2Fixed = sampleExecutionPlanWithRefsAs(
      AttrOrExprRef.attrRef,
      AttrOrExprRef.exprRef
    )(uuid2)

    // create mocks
    val jpSpy = new ProceedingJoinPointSpy("foo", ep1, Int.box(42), ep2)

    // execute test
    (new ExecutionPlansControllerDeserFixAspect).aroundAdvice(jpSpy)

    // verify the result
    jpSpy.capturedProceedingArgs shouldEqual Seq("foo", ep1Fixed, Int.box(42), ep2Fixed)
  }

}

object ExecutionPlansControllerDeserFixAspectSpec {

  import za.co.absa.commons.lang.OptionImplicits._

  class ProceedingJoinPointSpy(args: AnyRef*) extends ProceedingJoinPoint {
    var capturedProceedingArgs: Seq[AnyRef] = _

    override def getArgs: Array[AnyRef] = args.toArray

    override def proceed(args: Array[AnyRef]): AnyRef = {
      require(this.capturedProceedingArgs == null, "method `proceed()` is called twice")
      this.capturedProceedingArgs = args
      "dummy return value"
    }

    override def proceed(): AnyRef = null

    override def set$AroundClosure(arc: AroundClosure): Unit = {}

    override def toShortString: String = null

    override def toLongString: String = null

    override def getThis: AnyRef = null

    override def getTarget: AnyRef = null

    override def getSignature: Signature = null

    override def getSourceLocation: SourceLocation = null

    override def getKind: String = null

    override def getStaticPart: JoinPoint.StaticPart = null
  }

  private def sampleExecutionPlanWithRefsAs(
    attrRef: Attribute.Id => _,
    exprRef: ExpressionLike.Id => _
  )(planId: UUID) = {
    val sampleMapWithRefs = Map(
      "aaa" -> "111",
      "refToAttr" -> attrRef("attr1"),
      "bbb" -> Map(
        "arrayWithRefs" -> Seq(42, attrRef("attr2"), exprRef("expr2"))
      ),
    )

    ExecutionPlan(
      id = planId,
      name = None,
      operations = Operations(
        write = WriteOperation(
          id = "1",
          outputSource = "foo://bar",
          append = true,
          childIds = Seq("4", "5"),
          params = sampleMapWithRefs,
          extra = sampleMapWithRefs
        ),
        reads = Seq(
          ReadOperation(
            id = "2",
            inputSources = Seq("foo", "bar"),
            output = Seq("attr1").asOption,
            params = sampleMapWithRefs,
            extra = sampleMapWithRefs),
          ReadOperation(
            id = "3",
            inputSources = Seq("baz", "qux"),
            output = Seq("attr2").asOption,
            params = sampleMapWithRefs,
            extra = sampleMapWithRefs)
        ),
        other = Seq(
          DataOperation(
            id = "4",
            childIds = Seq("2"),
            output = Seq("attr1", "attr3").asOption,
            params = sampleMapWithRefs,
            extra = sampleMapWithRefs),
          DataOperation(
            id = "5",
            childIds = Seq("3"),
            output = Seq("attr2", "attr4").asOption,
            params = sampleMapWithRefs,
            extra = sampleMapWithRefs)
        )
      ),
      attributes = Seq(
        Attribute(
          id = "attr1",
          name = "attr1",
          childRefs = Nil,
          extra = sampleMapWithRefs),
        Attribute(
          id = "attr2",
          name = "attr2",
          childRefs = Nil,
          extra = sampleMapWithRefs),
        Attribute(
          id = "attr3",
          name = "attr3",
          childRefs = Seq(AttrOrExprRef.attrRef("attr1")),
          extra = sampleMapWithRefs),
        Attribute(
          id = "attr4",
          name = "attr4",
          childRefs = Seq(AttrOrExprRef.exprRef("expr1")),
          extra = sampleMapWithRefs)
      ),
      expressions = Some(
        Expressions(
          functions = Seq(
            FunctionalExpression(
              id = "expr1",
              name = "expr1",
              childRefs = Seq(AttrOrExprRef.attrRef("attr2"), AttrOrExprRef.exprRef("expr2")),
              params = sampleMapWithRefs,
              extra = sampleMapWithRefs
            )),
          constants = Seq(
            Literal(
              id = "expr2",
              value = 42,
              extra = sampleMapWithRefs)
          )
        )
      ),
      extraInfo = sampleMapWithRefs,
      systemInfo = NameAndVersion("test", "1.0.0"),
    )
  }
}
