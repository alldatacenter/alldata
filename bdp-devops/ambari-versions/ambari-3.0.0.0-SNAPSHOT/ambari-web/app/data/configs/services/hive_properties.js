/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

module.exports = [
  {
    "category": "HIVE_METASTORE",
    "filename": "hive-site.xml",
    "index": 7,
    "name": "javax.jdo.option.ConnectionDriverName",
    "serviceName": "HIVE"
  },
  {
    "category": "HIVE_METASTORE",
    "filename": "hive-site.xml",
    "index": 5,
    "name": "javax.jdo.option.ConnectionUserName",
    "serviceName": "HIVE"
  },
  {
    "category": "HIVE_METASTORE",
    "filename": "hive-site.xml",
    "index": 6,
    "name": "javax.jdo.option.ConnectionPassword",
    "serviceName": "HIVE"
  },
  {
    "category": "HIVE_METASTORE",
    "filename": "hive-site.xml",
    "index": 8,
    "name": "javax.jdo.option.ConnectionURL",
    "serviceName": "HIVE"
  },
  {
    "category": "HIVE_METASTORE",
    "filename": "hive-site.xml",
    "index": 4,
    "name": "ambari.hive.db.schema.name",
    "serviceName": "HIVE"
  },
  {
    "category": "HIVE_METASTORE",
    "displayType": "radio button",
    "filename": "hive-env.xml",
    "index": 2,
    "name": "hive_database",
    "options": [
      {
        "displayName": "New MySQL Database",
        "hidden": false
      },
      {
        "displayName": "Existing MySQL / MariaDB Database",
        "hidden": false
      },
      {
        "displayName": "Existing PostgreSQL Database",
        "hidden": false
      },
      {
        "displayName": "Existing Oracle Database",
        "hidden": false
      }
    ],
    "radioName": "hive-database",
    "serviceName": "HIVE"
  },
  {
    "category": "Advanced webhcat-env",
    "filename": "hive-env.xml",
    "name": "hcat_log_dir",
    "serviceName": "HIVE"
  },
  {
    "category": "Advanced webhcat-env",
    "filename": "hive-env.xml",
    "name": "hcat_pid_dir",
    "serviceName": "HIVE"
  },
  {
    "category": "HIVE_METASTORE",
    "filename": "hive-env.xml",
    "name": "hive_database_type",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.cbo.enable",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.optimize.reducededuplication.min.reducer",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.optimize.reducededuplication",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.orc.splits.include.file.footer",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.merge.mapfiles",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.merge.mapredfiles",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.merge.tezfiles",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.merge.smallfiles.avgsize",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.merge.size.per.task",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.merge.orcfile.stripe.level",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.auto.convert.join",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.auto.convert.join.noconditionaltask",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.auto.convert.join.noconditionaltask.size",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.optimize.bucketmapjoin.sortedmerge",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.tez.smb.number.waves",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.map.aggr.hash.percentmemory",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.map.aggr",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.optimize.sort.dynamic.partition",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.stats.autogather",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.stats.fetch.column.stats",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.vectorized.execution.enabled",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.vectorized.execution.reduce.enabled",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.vectorized.groupby.checkinterval",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.vectorized.groupby.flush.percent",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.limit.pushdown.memory.usage",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.optimize.index.filter",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.exec.reducers.bytes.per.reducer",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.smbjoin.cache.rows",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.exec.orc.default.stripe.size",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.fetch.task.conversion",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.fetch.task.conversion.threshold",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.fetch.task.aggr",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.compute.query.using.stats",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.tez.auto.reducer.parallelism",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.tez.max.partition.factor",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.tez.min.partition.factor",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.tez.dynamic.partition.pruning",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.tez.dynamic.partition.pruning.max.event.size",
    "serviceName": "HIVE"
  },
  {
    "category": "Performance",
    "filename": "hive-site.xml",
    "name": "hive.tez.dynamic.partition.pruning.max.data.size",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.exec.pre.hooks",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.exec.post.hooks",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.exec.failure.hooks",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.execution.engine",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.exec.dynamic.partition",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.exec.dynamic.partition.mode",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.exec.max.dynamic.partitions",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.exec.max.dynamic.partitions.pernode",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.exec.max.created.files",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.enforce.bucketing",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "datanucleus.cache.level2.type",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.metastore.uris",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.metastore.warehouse.dir",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.exec.parallel.thread.number",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.security.authorization.enabled",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.security.authorization.manager",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.security.metastore.authenticator.manager",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.security.metastore.authorization.manager",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.server2.authentication",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.server2.enable.doAs",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.server2.tez.default.queues",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.server2.tez.initialize.default.sessions",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.server2.tez.sessions.per.default.queue",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.server2.thrift.http.path",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.server2.thrift.http.port",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.server2.thrift.max.worker.threads",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.server2.thrift.port",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.server2.thrift.sasl.qop",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.server2.transport.mode",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.server2.use.SSL",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.tez.container.size",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.tez.java.opts",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.tez.log.level",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.txn.manager",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.txn.timeout",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.txn.max.open.batch",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.compactor.initiator.on",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.compactor.worker.threads",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.compactor.worker.timeout",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.compactor.check.interval",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.compactor.delta.num.threshold",
    "serviceName": "HIVE"
  },
  {
    "category": "General",
    "filename": "hive-site.xml",
    "name": "hive.compactor.delta.pct.threshold",
    "serviceName": "HIVE"
  }
];
