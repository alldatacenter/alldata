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

package za.co.absa.spline.producer.service.model

import scalax.collection.Graph
import scalax.collection.GraphEdge.DiEdge
import scalax.collection.GraphPredef._
import za.co.absa.commons.graph.GraphImplicits._
import za.co.absa.commons.lang.CollectionImplicits._
import za.co.absa.commons.lang.OptionImplicits._
import za.co.absa.spline.persistence.DefaultJsonSerDe._
import za.co.absa.spline.persistence.model.{Edge, EdgeDef}
import za.co.absa.spline.persistence.{model => pm}
import za.co.absa.spline.producer.model.v1_1.AttrOrExprRef
import za.co.absa.spline.producer.model.v1_1.OperationLike.Id
import za.co.absa.spline.producer.model.{v1_1 => am}
import za.co.absa.spline.producer.service.model.ExecutionPlanPersistentModelBuilder._
import za.co.absa.spline.producer.service.{InconsistentEntityException, model}

import java.util.UUID.randomUUID

class ExecutionPlanPersistentModelBuilder private(
  ep: am.ExecutionPlan,
  persistedDSKeyByURI: Map[pm.DataSource.Uri, pm.DataSource.Key]
) {
  private val keyCreator = new ExecutionPlanKeyCreator(ep)
  private val epPKey: pm.ArangoDocument.Id = ep.id.toString
  private val epPID: pm.ArangoDocument.Id = pm.NodeDef.ExecutionPlan.id(epPKey)

  // operation
  private var _pmOperations: Seq[pm.Operation] = Vector.empty
  private var _pmFollows: Seq[pm.Edge] = Vector.empty
  private var _pmEmits: Seq[pm.Edge] = Vector.empty
  private var _pmUses: Seq[pm.Edge] = Vector.empty
  private var _pmProduces: Seq[pm.Edge] = Vector.empty

  // schema
  private var _pmSchemas: Seq[pm.Schema] = Vector.empty
  private var _pmConsistsOf: Seq[pm.Edge] = Vector.empty

  // attribute
  private var _pmAttributes: Seq[pm.Attribute] = Vector.empty
  private var _pmComputedBy: Seq[pm.Edge] = Vector.empty

  // expression
  private var _pmExpressions: Seq[pm.Expression] = Vector.empty
  private var _pmTakes: Seq[pm.Edge] = Vector.empty

  // attr dependency graph
  private var _attrDepGraph = Graph.empty[AttrOrExprRef, DiEdge]

  private val pmDataSourceByURI: Map[String, pm.DataSource] = {
    val referencedURIs = ep.dataSources
    val persistedURIs = persistedDSKeyByURI.keys
    val transientDSKeyByURI = (referencedURIs -- persistedURIs).map(_ -> randomUUID.toString).toMap
    val dsKeyByUri = transientDSKeyByURI ++ persistedDSKeyByURI
    dsKeyByUri.map {
      case (uri, key) =>
        val dsName = pm.DataSource.getName(uri)
        uri -> pm.DataSource(uri, dsName, key)
    }
  }

  def build(): ExecutionPlanPersistentModel = {
    val pmExecutionPlan = pm.ExecutionPlan(
      name = ep.name,
      discriminator = ep.discriminator,
      _key = ep.id.toString,
      systemInfo = ep.systemInfo.toJsonAs[Map[String, Any]],
      agentInfo = ep.agentInfo.map(_.toJsonAs[Map[String, Any]]).orNull,
      extra = ep.extraInfo)

    val pmExecutes = EdgeDef.Executes.edge(ep.id, keyCreator.asOperationKey(ep.operations.write.id), epPKey)

    val pmDerivesFrom =
      for {
        attrFrom <- ep.attributes if attrFrom.childRefs.nonEmpty
        refFrom = AttrOrExprRef.attrRef(attrFrom.id)
        refTo <- findAncestorAttributeRefs(refFrom)
      } yield {
        EdgeDef.DerivesFrom.edge(
          keyCreator.asAttributeKey(refFrom.refId),
          keyCreator.asAttributeKey(refTo.refId),
          epPKey
        )
      }

    val pmTransientDataSources = {
      val persistentDSUris = persistedDSKeyByURI.keys
      (pmDataSourceByURI -- persistentDSUris).values.toSeq
    }

    model.ExecutionPlanPersistentModel(
      // plan
      executionPlan = pmExecutionPlan,
      executes = pmExecutes,
      depends = pmDepends,
      affects = pmAffects,

      // operation
      operations = _pmOperations,
      follows = _pmFollows,
      readsFrom = pmReadsFrom,
      writesTo = pmWritesTo,
      emits = _pmEmits,
      uses = _pmUses,
      produces = _pmProduces,

      // data source
      dataSources = pmTransientDataSources,

      // schema
      schemas = _pmSchemas,
      consistsOf = _pmConsistsOf,

      // attribute
      attributes = _pmAttributes,
      computedBy = _pmComputedBy,
      derivesFrom = pmDerivesFrom,

      // expression
      expressions = _pmExpressions,
      takes = _pmTakes,
    )
  }

  def addOperations(operations: Seq[am.OperationLike]): this.type = {
    val schemaInfoByOpId: Map[Id, SchemaInfo] = getSchemaInfos(operations)
    val schemaInfos = schemaInfoByOpId.values.distinctBy(_.oid)

    for (SchemaInfo(oid, attrs, diff) <- schemaInfos) {
      val opKey = keyCreator.asOperationKey(oid)
      val schemaKey = keyCreator.asSchemaKey(oid)
      this._pmSchemas +:= pm.Schema(schemaKey, Some(epPID))
      this._pmConsistsOf ++= attrs.zipWithIndex map {
        case (attrId, i) =>
          val attrKey = keyCreator.asAttributeKey(attrId)
          EdgeDef.ConsistsOf.edge(schemaKey, attrKey, epPKey, i)
      }
      for (attrId <- diff) {
        val attrKey = keyCreator.asAttributeKey(attrId)
        this._pmProduces :+= EdgeDef.Produces.edge(opKey, attrKey, epPKey)
      }
    }

    operations.foreach(op => {
      val opKey = keyCreator.asOperationKey(op.id)
      this._pmOperations :+= (op match {
        case r: am.ReadOperation => toReadOperation(r)
        case w: am.WriteOperation => toWriteOperation(w)
        case t: am.DataOperation => toTransformOperation(t)
      })

      for ((ref: am.AttrOrExprRef, path: Edge.FromPath) <- collectRefsWithPaths(op.params, "$['params']")) {
        this._pmUses :+= {
          if (ref.isAttribute)
            EdgeDef.Uses.edgeToAttr(opKey, keyCreator.asAttributeKey(ref.refId), epPKey, path)
          else
            EdgeDef.Uses.edgeToExpr(opKey, keyCreator.asExpressionKey(ref.refId), epPKey, path)
        }
      }

      for (schemaInfo <- schemaInfoByOpId.get(op.id)) {
        this._pmEmits :+= EdgeDef.Emits.edge(
          opKey,
          keyCreator.asSchemaKey(schemaInfo.oid),
          epPKey
        )
      }

      this._pmFollows ++= op.childIds.zipWithIndex map {
        case (childId, i) =>
          EdgeDef.Follows.edge(opKey, keyCreator.asOperationKey(childId), epPKey, i)
      }
    })

    this
  }

  def addAttributes(attributes: Seq[am.Attribute]): this.type = {
    for (attr <- attributes) {
      val attrKey = keyCreator.asAttributeKey(attr.id)
      this._pmAttributes :+= pm.Attribute(
        _key = attrKey,
        _belongsTo = Some(epPID),
        dataType = attr.dataType,
        extra = attr.extra,
        name = attr.name
      )
      attr.childRefs.zipWithIndex.foreach {
        case (ref, i) =>
          this._attrDepGraph += AttrOrExprRef.attrRef(attr.id) ~> ref
          if (ref.isExpression)
            this._pmComputedBy :+= EdgeDef.ComputedBy.edge(attrKey, keyCreator.asExpressionKey(ref.refId), epPKey, i)
      }
    }
    this
  }

  def addExpressions(expressions: Seq[am.ExpressionLike]): this.type = {
    expressions.foreach {
      case expr: am.Literal =>
        this._pmExpressions :+= pm.LiteralExpression(
          _key = keyCreator.asExpressionKey(expr.id),
          _belongsTo = Some(epPID),
          dataType = expr.dataType,
          extra = expr.extra,
          value = expr.value
        )
      case expr: am.FunctionalExpression =>
        val exprKey = keyCreator.asExpressionKey(expr.id)
        this._pmExpressions :+= pm.FunctionalExpression(
          _key = exprKey,
          _belongsTo = Some(epPID),
          dataType = expr.dataType,
          extra = expr.extra,
          name = expr.name,
          arity = expr.childRefs.length,
          params = expr.params,
        )
        expr.childRefs.zipWithIndex.foreach({
          case (ref, i) =>
            this._attrDepGraph += AttrOrExprRef.exprRef(expr.id) ~> ref
            this._pmTakes :+= (ref match {
              case AttrOrExprRef(Some(attrId), _) =>
                EdgeDef.Takes.edgeToAttr(exprKey, keyCreator.asAttributeKey(attrId), epPKey, i)
              case AttrOrExprRef(_, Some(exprId)) =>
                EdgeDef.Takes.edgeToExpr(exprKey, keyCreator.asExpressionKey(exprId), epPKey, i)
            })
        })
    }

    this
  }

  private def pmReadsFrom: Seq[pm.Edge] =
    for {
      ro <- ep.operations.reads
      ds <- ro.inputSources
    } yield {
      EdgeDef.ReadsFrom.edge(
        keyCreator.asOperationKey(ro.id),
        pmDataSourceByURI(ds)._key,
        epPKey
      )
    }

  private def pmWritesTo: pm.Edge = {
    EdgeDef.WritesTo.edge(
      keyCreator.asOperationKey(ep.operations.write.id),
      pmDataSourceByURI(ep.operations.write.outputSource)._key,
      epPKey
    )
  }

  private def pmDepends: Seq[pm.Edge] =
    for {
      ro <- ep.operations.reads
      ds <- ro.inputSources
    } yield EdgeDef.Depends.edge(
      ep.id,
      pmDataSourceByURI(ds)._key,
      epPKey
    )

  private def pmAffects: pm.Edge =
    EdgeDef.Affects.edge(
      ep.id,
      pmDataSourceByURI(ep.operations.write.outputSource)._key,
      epPKey
    )

  private def collectRefsWithPaths(obj: Map[String, Any], pathPrefix: Edge.FromPath): Iterable[(am.AttrOrExprRef, Edge.FromPath)] = {
    def fromVal(v: Any, p: Edge.FromPath): Iterable[(am.AttrOrExprRef, Edge.FromPath)] = v match {
      case ref: am.AttrOrExprRef => Seq(ref -> p)
      case m: Map[String, _] => collectRefsWithPaths(m, p)
      case xs: Seq[_] => xs.zipWithIndex.flatMap { case (x, i) => fromVal(x, s"$p[$i]") }
      case _ => Nil
    }

    obj.toSeq.flatMap { case (k, v) => fromVal(v, s"$pathPrefix['$k']") }
  }

  private def findAncestorAttributeRefs(attrRef: AttrOrExprRef) = {
    this._attrDepGraph // given the attribute dependency DAG (consisting of attribute and expression references)
      .get(attrRef.ensuring(_.isAttribute)) // starting on the given attribute reference
      .outerNodeTraverser // traverse the nodes (expression & attribute refs)
      .withMaxWeight(1, e => if (e.toOuter.from.isAttribute) 1 else 0) // to the nearest reachable attribute refs
      .filter(_.isAttribute) // collecting visited attribute refs
      .tail // except for the starting one
  }

  private def toTransformOperation(t: am.DataOperation) = {
    pm.Transformation(
      name = t.name,
      params = t.params,
      extra = t.extra,
      _key = keyCreator.asOperationKey(t.id),
      _belongsTo = Some(epPID)
    )
  }

  private def toWriteOperation(w: am.WriteOperation) = {
    pm.Write(
      outputSource = w.outputSource,
      append = w.append,
      name = w.name,
      params = w.params,
      extra = w.extra,
      _key = keyCreator.asOperationKey(w.id),
      _belongsTo = Some(epPID)
    )
  }

  private def toReadOperation(r: am.ReadOperation) = {
    pm.Read(
      inputSources = r.inputSources,
      name = r.name,
      params = r.params,
      extra = r.extra,
      _key = keyCreator.asOperationKey(r.id),
      _belongsTo = Some(epPID)
    )
  }
}


object ExecutionPlanPersistentModelBuilder {

  def toPersistentModel(
    ep: am.ExecutionPlan,
    persistedDSKeyByURI: Map[pm.DataSource.Uri, pm.DataSource.Key]
  ): ExecutionPlanPersistentModel = {
    val maybeExpressions = ep.expressions.map(_.all)
    new ExecutionPlanPersistentModelBuilder(ep, persistedDSKeyByURI)
      .addOperations(ep.operations.all)
      .addAttributes(ep.attributes)
      .having(maybeExpressions)(_ addExpressions _)
      .build()
  }

  private[model] case class SchemaInfo(oid: am.OperationLike.Id, schema: am.OperationLike.Schema, diff: Set[am.Attribute.Id])

  private[model] def getSchemaInfos(operations: Seq[am.OperationLike]): Map[Id, SchemaInfo] = {
    require(operations.nonEmpty)

    val opsSortedTopologically =
      try {
        operations.sortedTopologically(reverse = true)
      } catch {
        case e: RuntimeException => throw new InconsistentEntityException(e.getMessage)
      }

    opsSortedTopologically
      .foldLeft(Map.empty[Id, SchemaInfo]) {
        (schemaByOpId, op) =>
          val maybeInSchemaInfos = op.childIds.map(schemaByOpId.get)
          val maybeOutSchema = op.output

          (maybeInSchemaInfos, maybeOutSchema) match {

            // Schema agnostic operation
            case (maybeInSchemaInfos, None)
              if maybeInSchemaInfos.forall(_.isEmpty)=>
              schemaByOpId

            // output is defined
            case (maybeInSchemaInfo, Some(outSchema)) =>
              val inSchemaInfos = maybeInSchemaInfo.flatten
              val outSchemaInfo = inSchemaInfos
                .find(PartialFunction.cond(_) {
                  case si: SchemaInfo => si.schema == outSchema
                })
                .getOrElse({
                  val diff = inSchemaInfos.foldLeft(outSchema.toSet)(_ -- _.schema)
                  SchemaInfo(op.id, outSchema, diff)
                })
              schemaByOpId.updated(op.id, outSchemaInfo)

            // output in undefined and all inputs are equals, so the output can be inferred
            case (Some(inSchemaInfo) +: maybeInSchemaInfos, None)
              if maybeInSchemaInfos.forall(_.exists(_.oid == inSchemaInfo.oid)) =>
              schemaByOpId.updated(op.id, inSchemaInfo)

            // output is undefined and is not inferrable
            case (_, None) =>
              throw new InconsistentEntityException(s"Cannot infer schema for operation #${op.id}: the input schema is ambiguous")
          }
      }
  }

}
