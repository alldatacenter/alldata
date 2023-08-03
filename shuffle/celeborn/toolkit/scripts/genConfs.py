#!/usr/bin/env python3
# coding=utf-8
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import sys

thrift_server_conf = dict()
spark_default_conf = dict()
thrift_server_conf_path = "/etc/taihao-apps/spark-conf/spark-thriftserver.conf"
spark_default_conf_path = "/etc/taihao-apps/spark-conf/spark-defaults.conf"
hdfs_conf_path = "/etc/taihao-apps/hadoop-conf/core-site.xml"


def get_hdfs_root_path():
    cf = open(hdfs_conf_path)
    line = cf.readline()
    hdfs_root = ""
    while line:
        lp = line.strip()
        if "hdfs://" in lp:
            hdfs_root = lp.replace("<value>", "").replace("</value>", "").strip()
            break
        line = cf.readline()
    cf.close()
    return hdfs_root


def read_conf(conf, file):
    cf = open(file)
    line = cf.readline()
    while line:
        lps = line.strip()
        if lps.startswith("#") or not lps:
            line = cf.readline()
            continue
        splits = lps.split()
        key = splits[0]
        value = splits[1]
        conf[key] = value
        line = cf.readline()
    cf.close()


def read_ts_conf():
    read_conf(thrift_server_conf, thrift_server_conf_path)


def read_dconf():
    read_conf(spark_default_conf, spark_default_conf_path)


def save_confs(file, conf):
    for (key, value) in conf.items():
        file.writelines(key + " " + value + "\n")
    file.close()


def set_common_spark_confs(conf):
    conf["spark.eventLog.enabled"] = "true"
    conf["spark.serializer"] = "org.apache.spark.serializer.KryoSerializer"
    conf["spark.executor.memoryOverhead"] = "4g"
    conf["spark.dynamicAllocation.enabled"] = "false"
    conf["spark.executor.instances"] = "350"
    conf["spark.sql.adaptive.enabled"] = "true"
    conf["spark.sql.adaptive.skewJoin.enabled"] = "true"
    conf["spark.shuffle.service.enabled"] = "true"
    conf["spark.sql.adaptive.localShuffleReader.enabled"] = "false"
    conf["spark.sql.adaptive.coalescePartitions.initialPartitionNum"] = "4096"
    conf["spark.sql.hive.forceRpadString"] = "true"
    if "spark.eventLog.dir" in spark_default_conf:
        conf["spark.eventLog.dir"] = spark_default_conf["spark.eventLog.dir"]


def set_skew_join_confs(conf):
    conf["spark.sql.adaptive.autoBroadcastJoinThreshold"] = "-1"
    conf["spark.sql.adaptive.skewJoin.enabled"] = "true"
    conf["spark.sql.adaptive.skewJoin.skewedPartitionThresholdInBytes"] = "32m"
    conf["spark.sql.autoBroadcastJoinThreshold"] = "-1"


def set_celeborn_confs(conf, replicate=False):
    conf["spark.shuffle.manager"] = "org.apache.spark.shuffle.celeborn.SparkShuffleManager"
    conf["spark.serializer"] = "org.apache.spark.serializer.KryoSerializer"
    conf["spark.celeborn.master.endpoints"] = "master-1-1:9097"
    conf["spark.shuffle.service.enabled"] = "false"

    if replicate:
        conf["spark.celeborn.client.push.replicate.enabled"] = "true"
    else:
        conf["spark.celeborn.client.push.replicate.enabled"] = "false"


def save_ess_conf(dir):
    ess = open(dir + "/spark-ess.conf", "w")
    nconf = thrift_server_conf.copy()
    set_common_spark_confs(nconf)
    save_confs(ess, nconf)


def save_celeborn_conf(dir):
    celeborn = open(dir + "/spark-celeborn.conf", "w")
    nconf = thrift_server_conf.copy()
    set_common_spark_confs(nconf)
    set_celeborn_confs(nconf, False)
    save_confs(celeborn, nconf)


def save_celeborn_dup_conf(dir):
    celeborndup = open(dir + "/spark-celeborn-dup.conf", "w")
    nconf = thrift_server_conf.copy()
    set_common_spark_confs(nconf)
    set_celeborn_confs(nconf, True)
    save_confs(celeborndup, nconf)


def save_skewjoin_spark_confs(dir):
    celeborndup = open(dir + "/spark-skewjoin.conf", "w")
    nconf = thrift_server_conf.copy()
    set_common_spark_confs(nconf)
    set_skew_join_confs(nconf)
    set_celeborn_confs(nconf, True)
    save_confs(celeborndup, nconf)


def update_spark_confs(target_dir):
    print("Generated configuration output path:" + target_dir)
    print("generate new spark confs")
    read_ts_conf()
    read_dconf()
    save_ess_conf(target_dir)
    save_celeborn_conf(target_dir)
    save_celeborn_dup_conf(target_dir)
    save_skewjoin_spark_confs(target_dir)


def merge_two_dicts(x, y):
    z = x.copy()
    z.update(y)
    return z


def update_hibench_confs(hiben_conf_dir):
    hdfs_root_path = get_hdfs_root_path()
    hibench_hadoop_conf_file_path = hiben_conf_dir + "/hadoop.conf"
    hibench_hadoop_conf = dict()
    read_conf(hibench_hadoop_conf, hibench_hadoop_conf_file_path)
    hibench_hadoop_conf["hibench.hdfs.master"] = hdfs_root_path
    hibench_hadoop_conf_file_instance = open(hibench_hadoop_conf_file_path, "w")
    save_confs(hibench_hadoop_conf_file_instance, hibench_hadoop_conf)
    hibench_hadoop_conf_file_instance.close()
    hibench_spark_conf_file = hiben_conf_dir + "/spark.conf"
    hibench_spark_conf = dict()
    read_conf(hibench_spark_conf, hibench_spark_conf_file)
    nconf = thrift_server_conf.copy()
    set_common_spark_confs(nconf)
    set_celeborn_confs(nconf, True)
    new_hibench_spark_conf = merge_two_dicts(nconf, hibench_spark_conf)
    normal_hibench_spark_conf_file_path = hibench_spark_conf_file + ".celeborn"
    save_confs(open(normal_hibench_spark_conf_file_path, "w"), new_hibench_spark_conf)
    splits_hibench_spark_conf_file_path = hibench_spark_conf_file + ".split"
    new_hibench_spark_conf["spark.celeborn.client.shuffle.partitionSplit.threshold"] = "16m"
    save_confs(open(splits_hibench_spark_conf_file_path, "w"), new_hibench_spark_conf)


if __name__ == '__main__':
    if len(sys.argv) <= 1:
        print("Need regression root path")
        sys.exit(-1)
    rootDir = sys.argv[1]
    targetDir = rootDir + "/conf"
    hibenConfDir = rootDir + "/hibench3/conf"
    update_spark_confs(targetDir)
    update_hibench_confs(hibenConfDir)
