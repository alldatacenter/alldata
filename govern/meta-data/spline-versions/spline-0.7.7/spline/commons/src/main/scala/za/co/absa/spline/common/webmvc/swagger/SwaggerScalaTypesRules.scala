/*
 * Copyright 2019 ABSA Group Limited
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

package za.co.absa.spline.common.webmvc.swagger

import java.util

import com.fasterxml.classmate.TypeResolver
import org.springframework.core.Ordered.HIGHEST_PRECEDENCE
import springfox.documentation.schema.AlternateTypeRules.{GENERIC_SUBSTITUTION_RULE_ORDER, newRule}
import springfox.documentation.schema.{AlternateTypeRule, AlternateTypeRuleConvention, WildcardType}
import za.co.absa.commons.lang.TypeConstraints.not

import scala.concurrent.Future

trait SwaggerScalaTypesRules extends AlternateTypeRuleConvention {
  val typeResolver: TypeResolver

  import typeResolver._

  override def getOrder: Int = HIGHEST_PRECEDENCE

  private def newListCoercionRule[C <: Iterable[_] : not[Map[_, _]]#λ : Manifest] =
    newRule(
      resolve(manifest[C].runtimeClass, classOf[WildcardType]),
      resolve(classOf[util.List[_]], classOf[WildcardType]))

  private def newMapCoercionRule[C <: Map[_, _] : Manifest] =
    newRule(
      resolve(manifest[C].runtimeClass, classOf[WildcardType], classOf[WildcardType]),
      resolve(classOf[util.Map[_, _]], classOf[WildcardType], classOf[WildcardType]))

  private def newUnboxingRule[T: Manifest] =
    newRule(
      resolve(manifest[T].runtimeClass, classOf[WildcardType]),
      resolve(classOf[WildcardType]),
      GENERIC_SUBSTITUTION_RULE_ORDER)

  override def rules(): util.List[AlternateTypeRule] = util.Arrays.asList(
    newRule(classOf[java.util.Date], classOf[Long]),
    newRule(classOf[java.sql.Date], classOf[Long]),
    newRule(classOf[java.sql.Timestamp], classOf[Long]),

    newUnboxingRule[Option[_]],
    newUnboxingRule[Future[_]],

    newListCoercionRule[Set[_]],
    newListCoercionRule[Seq[_]],
    newListCoercionRule[List[_]],
    newListCoercionRule[Vector[_]],

    newMapCoercionRule[Map[_, _]]
  )
}
