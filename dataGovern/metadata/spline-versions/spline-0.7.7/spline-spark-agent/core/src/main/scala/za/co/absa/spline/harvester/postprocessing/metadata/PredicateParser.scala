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

package za.co.absa.spline.harvester.postprocessing.metadata

import fastparse.Parsed.{Failure, Success}
import fastparse.SingleLineWhitespace._
import fastparse._
import za.co.absa.spline.harvester.postprocessing.metadata.BaseNodeName._
import za.co.absa.spline.harvester.postprocessing.metadata.ComparisonSymbol._

object PredicateParser {

  def parse(input: String): (String, Predicate) =
    fastparse.parse(input, parser(_)) match {
      case Success(value, index) => toPredicateResult(value)
      case Failure(str, i, extra) => throw new RuntimeException(extra.trace().longMsg)
    }

  private def toPredicateResult(selector: (String, Option[Expr])) = selector match {
    case (name, Some(expr)) => (name, Predicate(expr))
    case (name, None) => (name, Predicate(Bool(true)))
  }

  // -------- Parser --------

  private def parser[_: P] = P(extraElementName ~ selector.? ~ End)

  private def extraElementName[_: P] = P(ExecutionPlan | ExecutionEvent | Operation | Read | Write).!

  private def selector[_: P] = P("[" ~/ expr ~ "]")


  // -------- Expression hierarchy --------

  private def expr[_: P]: P[Expr] = P(or)

  private def or[_: P]: P[Expr] = P(and ~ ("||" ~/ and).rep).map {
    case (e, seq) if seq.isEmpty => e
    case (e, seq) => Or(e +: seq: _*)
  }

  private def and[_: P]: P[Expr] = P(equalityOp ~ ("&&" ~/ equalityOp).rep).map {
    case (e, seq) if seq.isEmpty => e
    case (e, seq) => And(e +: seq: _*)
  }

  private def equalityOp[_: P]: P[Expr] = P(comparisonOp ~ (("==" | "!=").! ~/ comparisonOp).?).map {
    case (e, None) => e
    case (l, Some((operand, r))) => operand match {
      case "==" => Eq(l, r)
      case "!=" => Neq(l, r)
    }
  }

  private def comparisonOp[_: P]: P[Expr] = P(factor ~ ((LTE | GTE | LT | GT).! ~/ factor).?).map {
    case (e, None) => e
    case (l, Some((operand, r))) => Comparison(l, r, operand)
  }

  private def factor[_: P]: P[Expr] = P(not | literal | path | parens)

  private def not[_: P]: P[Expr] = P("!" ~/ factor).map(Not)


  private def parens[_: P] = P("(" ~/ expr ~ ")")

  // -------- Literals --------

  private def literal[_: P]: P[Expr] = P(boolean | long | integer | text)

  private def boolean[_: P] = P("true" | "false").!
    .map {
      case "true" => Bool(true)
      case "false" => Bool(false)
    }

  private def long[_: P] = P(CharIn("0-9").rep(1).! ~ "L").map(l => LongNum(l.toLong))

  private def integer[_: P] = P(CharIn("0-9").rep(1)).!.map(i => IntNum(i.toInt))

  private def text[_: P] = P("'" ~/ CharsWhile(_ != '\'', 0).! ~ "'").map(Text)

  // -------- Path --------

  private def path[_: P] = P(pathSegment).rep(min = 1, sep = ".").map(s => Path(s.flatten: _*))

  private def pathSegment[_: P] = P(pathProperty ~ (pathArrayAccess | pathMapAccess).rep).map {
    case (segment, segmentSeq) => segment +: segmentSeq
  }

  private def pathProperty[_: P]: P[PathSegment] = P(CharIn("a-zA-Z@") ~ CharIn("a-zA-Z0-9").rep).!.map(PropertyPS)

  private def pathArrayAccess[_: P]: P[PathSegment] = P("[" ~ integer ~ "]").map(i => ArrayPS(i.value))

  private def pathMapAccess[_: P]: P[PathSegment] = P("[" ~ text ~ "]").map(t => MapPS(t.value))

}
