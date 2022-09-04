#!/usr/bin/python
# -*- coding: UTF-8 -*-

import MySQLdb
import json
import time
import os
import datetime

CORE_DB_HOST = os.getenv('DB_HOST') 
CORE_DB_USER = os.getenv('DB_USER')
CORE_DB_PASSWORD = os.getenv('DB_PASSWORD')
CORE_DB_PORT = os.getenv('DB_PORT')
current_time = int(time.time())

core_db = MySQLdb.connect(host=CORE_DB_HOST, port=int(CORE_DB_PORT), user=CORE_DB_USER, passwd=CORE_DB_PASSWORD, db="sreworks_appmanager", charset='utf8' )
core_cursor = core_db.cursor()
core_cursor.execute("select cluster_config from am_cluster where cluster_id = 'master'")
data = core_cursor.fetchone()
kubeconfig=json.dumps(json.loads(data[0])["kube"])

cluster_db = MySQLdb.connect(host=CORE_DB_HOST, port=int(CORE_DB_PORT), user=CORE_DB_USER, passwd=CORE_DB_PASSWORD, db="sreworks_meta", charset='utf8' )
cluster_cursor = cluster_db.cursor()
cluster_cursor.execute("select id from cluster where cluster_name = 'default' and name = 'default' and team_id = 1")
data = cluster_cursor.fetchone()
if data is None:
    sql = f"""INSERT INTO sreworks_meta.cluster (cluster_name,creator,description,gmt_create,gmt_modified,kubeconfig,last_modifier,name,team_id) 
        VALUES ('default', '999999999','','{current_time}', '{current_time}', '{kubeconfig}', '999999999', 'default', '1')"""
    print(sql)
    cluster_cursor.execute(sql)
    cluster_db.commit()
else:
    sql = f"UPDATE sreworks_meta.cluster set kubeconfig = '{kubeconfig}' WHERE cluster_name = 'default' AND name = 'default' AND team_id = 1 "
    print(sql)
    cluster_cursor.execute(sql)
    cluster_db.commit()

cluster_cursor = cluster_db.cursor()
cluster_cursor.execute("select id,name,kubeconfig from cluster where cluster_name = 'default' and name = 'default' and team_id = 1")
data = cluster_cursor.fetchone()
clusterId = str(data[0]) + "id"

core_cursor = core_db.cursor()
core_cursor.execute("select cluster_id from am_cluster where cluster_id = '{clusterId}'")
data = core_cursor.fetchone()
cluster_config = json.dumps({"kube":json.loads(kubeconfig)})
current_datetime = datetime.datetime.now()
if data is None:
    sql = f"""INSERT INTO am_cluster (gmt_create,gmt_modified,cluster_id,cluster_name,cluster_type,cluster_config,master_flag) 
        VALUES ('{current_datetime}', '{current_datetime}', '{clusterId}', 'default', 'kubernetes','{cluster_config}', '0')"""
    print(sql)
    core_cursor.execute(sql)
    core_db.commit()
else:
    sql = f"UPDATE am_cluster set cluster_config = '{cluster_config}' WHERE cluster_id = '{clusterId}' AND cluster_name = 'default'"
    print(sql)
    core_cursor.execute(sql)
    core_db.commit()

print("default cluster sync done!")

