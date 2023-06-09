/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// this file copy form apache spark
package com.netease.arctic.spark.distibutions

import com.netease.arctic.spark.distributions.{Expression, Literal, NamedReference, Transform}
import org.apache.spark.sql.catalyst
import org.apache.spark.sql.types.{DataType, IntegerType, StringType}

/**
 * Helper methods for working with the logical expressions API.
 *
 * Factory methods can be used when referencing the logical expression nodes is ambiguous because
 * logical and internal expressions are used.
 */
object LogicalExpressions {
  def literal[T](value: T): LiteralValue[T] = {
    val internalLit = catalyst.expressions.Literal(value)
    literal(value, internalLit.dataType)
  }

  def literal[T](value: T, dataType: DataType): LiteralValue[T] = LiteralValue(value, dataType)

  def parseReference(name: String): NamedReference =
    FieldReference(Seq(name))

  def reference(nameParts: Seq[String]): NamedReference = FieldReference(nameParts)

  def apply(name: String, arguments: Expression*): Transform = ApplyTransform(name, arguments)

  def bucket(numBuckets: Int, references: Array[NamedReference]): BucketTransform =
    BucketTransform(literal(numBuckets, IntegerType), references)

  def identity(reference: NamedReference): IdentityTransform = IdentityTransform(reference)

}

/**
 * Allows Spark to rewrite the given references of the transform during analysis.
 */
sealed trait RewritableTransform extends Transform {
  /** Creates a copy of this transform with the new analyzed references. */
  def withReferences(newReferences: Seq[NamedReference]): Transform
}

/**
 * Base class for simple transforms of a single column.
 */
abstract class SingleColumnTransform(ref: NamedReference) extends RewritableTransform {

  def reference: NamedReference = ref

  override def references: Array[NamedReference] = Array(ref)

  override def arguments: Array[Expression] = Array(ref)

  override def describe: String = name + "(" + reference.describe + ")"

  override def toString: String = describe

  protected def withNewRef(ref: NamedReference): Transform

  override def withReferences(newReferences: Seq[NamedReference]): Transform = {
    assert(newReferences.length == 1,
      s"Tried rewriting a single column transform (${this}) with multiple references.")
    withNewRef(newReferences.head)
  }
}

final case class BucketTransform(
                                               numBuckets: Literal[Int],
                                               columns: Seq[NamedReference]) extends RewritableTransform {

  override val name: String = "bucket"

  override def references: Array[NamedReference] = {
    arguments.collect { case named: NamedReference => named }
  }

  override def arguments: Array[Expression] = numBuckets +: columns.toArray

  override def describe: String = s"bucket(${arguments.map(_.describe).mkString(", ")})"

  override def toString: String = describe

  override def withReferences(newReferences: Seq[NamedReference]): Transform = {
    this.copy(columns = newReferences)
  }
}

object BucketTransform {
  def unapply(transform: Transform): Option[(Int, NamedReference)] = transform match {
    case NamedTransform("bucket", Seq(
    Lit(value: Int, IntegerType),
    Ref(seq: Seq[String]))) =>
      Some((value, FieldReference(seq)))
    case _ =>
      None
  }
}

final case class ApplyTransform(
                                              name: String,
                                              args: Seq[Expression]) extends Transform {

  override def arguments: Array[Expression] = args.toArray

  override def references: Array[NamedReference] = {
    arguments.collect { case named: NamedReference => named }
  }

  override def describe: String = s"$name(${arguments.map(_.describe).mkString(", ")})"

  override def toString: String = describe
}

/**
 * Convenience extractor for any Literal.
 */
private object Lit {
  def unapply[T](literal: Literal[T]): Some[(T, DataType)] = {
    Some((literal.value, literal.dataType))
  }
}

/**
 * Convenience extractor for any NamedReference.
 */
private object Ref {
  def unapply(named: NamedReference): Some[Seq[String]] = {
    Some(named.fieldNames)
  }
}

/**
 * Convenience extractor for any Transform.
 */
object NamedTransform {
  def unapply(transform: Transform): Some[(String, Seq[Expression])] = {
    Some((transform.name, transform.arguments))
  }
}

final case class IdentityTransform(ref: NamedReference) extends SingleColumnTransform(ref) {
  override val name: String = "identity"

  override def describe: String = ref.describe

  override protected def withNewRef(ref: NamedReference): Transform = this.copy(ref)
}

object IdentityTransform {
  def unapply(transform: Transform): Option[FieldReference] = transform match {
    case NamedTransform("identity", Seq(Ref(parts))) =>
      Some(FieldReference(parts))
    case _ =>
      None
  }
}

case class LiteralValue[T](value: T, dataType: DataType) extends Literal[T] {
  override def describe: String = {
    if (dataType.isInstanceOf[StringType]) {
      s"'$value'"
    } else {
      s"$value"
    }
  }

  override def toString: String = describe
}

case class FieldReference(parts: Seq[String]) extends NamedReference {
  override def fieldNames: Array[String] = parts.toArray

  override def describe: String = parts.map(quoteIfNeeded).mkString(".")

  override def toString: String = describe

  def quoteIfNeeded(part: String): String = {
    if (part.contains(".") || part.contains("`")) {
      s"`${part.replace("`", "``")}`"
    } else {
      part
    }
  }
}

object FieldReference {
  def apply(column: String): NamedReference = {
    LogicalExpressions.parseReference(column)
  }
}



