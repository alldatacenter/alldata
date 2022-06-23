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

const VER = "0.7.0"

const {db, aql} = require("@arangodb");

console.log(`[Spline] Start migration to ${VER}`);

console.log("Update 'dataSource'");

db._query(aql`
    WITH dataSource
    FOR ds IN dataSource
        UPDATE ds
            WITH {
                "name": REGEX_MATCHES(ds.uri, "([^/]+)/*$")[1],
            }
            IN dataSource
`);

console.log("Update 'progress.execPlanDetails'");

db._query(aql`
    WITH progress
    FOR p IN progress
        UPDATE p
            WITH {
                "execPlanDetails": {
                    "dataSourceName": REGEX_MATCHES(p.execPlanDetails.dataSourceUri, "([^/]+)/*$")[1],
                }
            }
            IN progress
`);

console.log("[Spline] Create index 'dataSource._created'");
db.dataSource.ensureIndex({type: "persistent", fields: ["_created"]});

console.log("[Spline] Create index 'dataSource.name'");
db.dataSource.ensureIndex({type: "persistent", fields: ["name"]});

console.log("[Spline] Create index 'progress.execPlanDetails.dataSourceName'");
db.progress.ensureIndex({type: "persistent", fields: ["execPlanDetails.dataSourceName"]});

console.log(`[Spline] Migration done. Version ${VER}`);
