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

package za.co.absa.spline.persistence.migration

import scalax.collection.Graph
import scalax.collection.GraphPredef._
import scalax.collection.edge.Implicits.edge2WDiEdgeAssoc
import scalax.collection.edge.WDiEdge
import za.co.absa.commons.version.impl.SemVer20Impl.SemanticVersion

class MigrationScriptRepository(scriptLoader: MigrationScriptLoader) {

  private val scripts: Seq[MigrationScript] = scriptLoader.loadAll()

  def findMigrationChain(verFrom: SemanticVersion, verTo: SemanticVersion): Seq[MigrationScript] = {
    try {
      val scriptByVersionPair = scripts.groupBy(scr => (scr.verFrom, scr.verTo)).mapValues(_.head)
      val edges: Seq[WDiEdge[SemanticVersion]] = scripts.map(scr => scr.verFrom ~> scr.verTo % 1)
      val graph = Graph(edges: _*)
      val vFrom = graph.get(verFrom)
      val vTo = graph.get(verTo)
      val path = (vFrom shortestPathTo vTo).get
      path.edges.map(e => scriptByVersionPair((e.from, e.to))).toSeq
    } catch {
      case _: NoSuchElementException =>
        sys.error(s"Cannot find migration scripts from version ${verFrom.asString} to ${verTo.asString}")
    }
  }

  def latestToVersion: SemanticVersion = {
    scripts.map(_.verTo).max
  }
}

object MigrationScriptRepository extends MigrationScriptRepository(
  new ResourceMigrationScriptLoader("classpath:migration-scripts"))

