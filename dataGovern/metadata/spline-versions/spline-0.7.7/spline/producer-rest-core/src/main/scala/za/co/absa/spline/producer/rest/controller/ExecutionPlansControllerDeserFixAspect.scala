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

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.{Around, Aspect, Pointcut}
import org.slf4s.Logging
import org.springframework.stereotype.Component
import za.co.absa.spline.producer.model.v1_1._
import za.co.absa.spline.producer.rest.controller.ExecutionPlansControllerDeserFixAspect.fixExecPlan

@Aspect
@Component
class ExecutionPlansControllerDeserFixAspect extends Logging {

  @Pointcut("execution(public * za.co.absa.spline.producer.rest.controller.*Controller.*(..))")
  def publicControllerMethods(): Unit = {}

  @Pointcut("execution(* *(.., za.co.absa.spline.producer.model.v1_1.ExecutionPlan, ..))")
  def acceptingExecutionPlan(): Unit = {}

  @Around("publicControllerMethods() && acceptingExecutionPlan()")
  def aroundAdvice(jp: ProceedingJoinPoint): AnyRef = {
    log.debug(s"Intercepting controller method `${jp.getSignature}`")
    val origArgs = jp.getArgs
    val fixedArgs = origArgs.map {
      case ep: ExecutionPlan => fixExecPlan(ep)
      case x => x
    }
    jp.proceed(fixedArgs)
  }
}

object ExecutionPlansControllerDeserFixAspect extends Logging {
  private def fixExecPlan(execPlan: ExecutionPlan): ExecutionPlan = {
    log.debug(s"Fixing model for execution plan #${execPlan.id}")
    execPlan.copy(
      operations = fixOperations(execPlan.operations),
      attributes = execPlan.attributes.map(fixAttribute),
      expressions = execPlan.expressions.map(fixExpressions),
      extraInfo = fixMap(execPlan.extraInfo)
    )
  }

  private def fixOperations(operations: Operations): Operations = operations match {
    case Operations(w, rs, os) => operations.copy(
      write = w.copy(
        params = fixMap(w.params),
        extra = fixMap(w.extra)
      ),
      reads = rs.map(r => r.copy(
        params = fixMap(r.params),
        extra = fixMap(r.extra)
      )),
      other = os.map(o => o.copy(
        params = fixMap(o.params),
        extra = fixMap(o.extra)
      )),
    )
  }

  private def fixAttribute(attr: Attribute): Attribute = {
    attr.copy(
      extra = fixMap(attr.extra)
    )
  }

  private def fixExpressions(expressions: Expressions): Expressions = expressions match {
    case Expressions(funcs, constants) => expressions.copy(
      functions = funcs.map(fe => fe.copy(
        params = fixMap(fe.params),
        extra = fixMap(fe.extra)
      )),
      constants = constants.map(ce => ce.copy(
        extra = fixMap(ce.extra)
      ))
    )
  }

  private def fixMap(obj: Map[String, Any]): Map[String, Any] = {
    obj.mapValues(fixValue)
      .view.force // see: https://github.com/scala/bug/issues/4776
  }

  private def fixValue(v: Any): Any = v match {
    case m: Map[String, _] => AttrOrExprRef.fromMap(m) getOrElse fixMap(m)
    case xs: Seq[_] => xs.map(fixValue)
    case _ => v
  }
}
