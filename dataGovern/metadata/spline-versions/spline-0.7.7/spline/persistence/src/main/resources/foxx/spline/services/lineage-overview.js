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

"use strict";
const {db, aql} = require('@arangodb');
const {memoize} = require('../utils/common');
const {GraphBuilder} = require('../utils/graph');
const {observedWritesByRead} = require('./observed-writes-by-read');

/**
 * Return a high-level lineage overview of the given write event.
 *
 * @param eventKey write event key
 * @param maxDepth maximum number of job nodes in any path of the resulted graph (excluding cycles).
 * It shows how far the traversal should looks for the lineage.
 * @returns za.co.absa.spline.consumer.service.model.LineageOverview
 */
function lineageOverview(eventKey, maxDepth) {

    const executionEvent = db._query(aql`
        WITH progress
        RETURN FIRST(FOR p IN progress FILTER p._key == ${eventKey} RETURN p)
    `).next();

    const targetDataSource = executionEvent && db._query(aql`
        WITH progress, progressOf, executionPlan, affects, dataSource
        RETURN FIRST(FOR ds IN 2 OUTBOUND ${executionEvent} progressOf, affects RETURN ds)
    `).next();

    const lineageGraph = eventLineageOverviewGraph(executionEvent, maxDepth);

    return lineageGraph && {
        "info": {
            "timestamp": executionEvent.timestamp,
            "applicationId": executionEvent.extra.appId,
            "targetDataSourceId": targetDataSource._key
        },
        "graph": {
            "depthRequested": maxDepth,
            "depthComputed": lineageGraph.depth || -1,
            "nodes": lineageGraph.vertices,
            "edges": lineageGraph.edges
        }
    }
}

function eventLineageOverviewGraph(startEvent, maxDepth) {
    if (!startEvent || maxDepth < 0) {
        return null;
    }

    const startSource = db._query(aql`
        WITH progress, progressOf, executionPlan, affects, dataSource
        FOR ds IN 2 OUTBOUND ${startEvent} progressOf, affects 
            LIMIT 1
            RETURN {
                "_id": ds._key,
                "_class": "za.co.absa.spline.consumer.service.model.DataSourceNode",
                "name": ds.uri
            }
        `
    ).next();

    const graphBuilder = new GraphBuilder([startSource]);

    const collectPartialGraphForEvent = event => {
        const partialGraph = db._query(aql`
            WITH progress, progressOf, executionPlan, affects, depends, dataSource

            LET exec = FIRST(FOR ex IN 1 OUTBOUND ${event} progressOf RETURN ex)
            LET affectedDsEdge = FIRST(FOR v, e IN 1 OUTBOUND exec affects RETURN e)
            LET rdsWithInEdges = (FOR ds, e IN 1 OUTBOUND exec depends RETURN [ds, e])
            LET readSources = rdsWithInEdges[*][0]
            LET readDsEdges = rdsWithInEdges[*][1]
            
            LET vertices = (
                FOR vert IN APPEND(readSources, exec)
                    LET vertType = SPLIT(vert._id, '/')[0]
                    RETURN vertType == "dataSource"
                        ? {
                            "_id": vert._key,
                            "_class": "za.co.absa.spline.consumer.service.model.DataSourceNode",
                            "name": vert.uri
                        }
                        : MERGE(KEEP(vert, ["systemInfo", "agentInfo"]), {
                            "_id": vert._key,
                            "_class": "za.co.absa.spline.consumer.service.model.ExecutionNode",
                            "name": vert.name || ""
                        })
            )
            
            LET edges = (
                FOR edge IN APPEND(readDsEdges, affectedDsEdge)
                    LET edgeType = SPLIT(edge._id, '/')[0]
                    LET exKey = SPLIT(edge._from, '/')[1]
                    LET dsKey = SPLIT(edge._to, '/')[1]
                    RETURN {
                        "source": edgeType == "depends" ? dsKey : exKey,
                        "target": edgeType == "affects" ? dsKey : exKey
                    }
            )
            
            RETURN {vertices, edges}
        `).next();

        graphBuilder.add(partialGraph);
    };

    const traverse = memoize(e => e._id, (event, depth) => {
        let remainingDepth = depth - 1;
        if (depth > 1) {
            observedWritesByRead(event)
                .forEach(writeEvent => {
                    const remainingDepth_i = traverse(writeEvent, depth - 1);
                    remainingDepth = Math.min(remainingDepth, remainingDepth_i);
                })
        }

        collectPartialGraphForEvent(event);

        return remainingDepth;
    });

    const remainingDepth = maxDepth > 0 ? traverse(startEvent, maxDepth) : 0;
    const resultedGraph = graphBuilder.graph();

    return {
        depth: maxDepth - remainingDepth,
        vertices: resultedGraph.vertices,
        edges: resultedGraph.edges
    }
}

module.exports = {
    lineageOverview
}

