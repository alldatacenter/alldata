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

class DistinctCollector {
    constructor(keyFn, initialValues) {
        this.keyFn = keyFn;
        this.keys = new Set((initialValues || []).map(v => keyFn(v)));
        this.vals = initialValues || [];
    }

    add(o) {
        const k = this.keyFn(o);
        if (!this.keys.has(k)) {
            this.keys.add(k);
            this.vals.push(o);
        }
    }
}

class GraphBuilder {
    constructor(vertices, edges) {
        this.vertexCollector = new DistinctCollector(v => v._id, vertices);
        this.edgeCollector = new DistinctCollector(e => `${e.source}:${e.target}`, edges);
    }

    add(pGraph) {
        pGraph.vertices.forEach(v => this.vertexCollector.add(v));
        pGraph.edges.forEach(e => this.edgeCollector.add(e));
    }

    graph() {
        return {
            vertices: this.vertexCollector.vals,
            edges: this.edgeCollector.vals
        }
    }
}

module.exports = {
    GraphBuilder
}
