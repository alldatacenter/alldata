/*
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

module.exports = {
  'GET /mock/ams/v1/login/current': (req, res) => {
    res.json({
      code: 403,
      msg: 'fail',
      result: null
    })
  },
  'GET /api/catalog': (req, res) => {
    res.json({
      code: 200,
      msg: 'success',
      result: [
        {
          name: 'catalog1'
        },
        {
          name: 'catalog2'
        }
      ]
    })
  },
  'GET /api/database': (req, res) => {
    res.json({
      code: 200,
      msg: 'success',
      result: ['database1', 'database2']
    })
  },
  'GET mock/ams/v1/catalogs/opensource_arctic/databases': (req, res) => {
    res.json({
      code: 200,
      msg: 'success',
      result: ['database1', 'database2',
      "arctic_spark_test267",
      "arctic_spark_test3",
      "arctic_test",
      "arctic_test_2",
      "chbenchmark1",
      "hellowrld",
      "hwtest1",
      "arctic_spark_test36",
      "uu",
      "arctic_test_2sw",
      "chbenchmark2",
      "tt",
      "yuyu",
      "arctic_spark_test32",
      "arctic_testty",
      "arctic_test_2ty",
      "chbenchmark",
      "hellowrltd",
      "rt",
      "arctic_spark_test3",
      "arctic_test_2yy",
      "chbenchmark5",
      "hellowrld7",
      "hwtesrtrt1"
    ]
    })
  },
  'GET mock/ams/v1/catalogs/opensource_arctic/databases/arctic_test/tables': (req, res) => {
    res.json({
      code: 200,
      msg: 'success',
      result: ['table11', 'table22',"arctic_spark_test267",
      "arctic_spark_test3",
      "arctic_test",
      "arctic_test_2",
      "chbenchmark1",
      "hellowrld",
      "hwtest1",
      "arctic_spark_test36",
      "uu",
      "arctic_test_2sw",
      "chbenchmark2",
      "tt",
      "yuyu",
      "arctic_spark_test32",
      "arctic_testty",
      "arctic_test_2ty",
      "chbenchmark",
      "hellowrltd",
      "rt",
      "arctic_spark_test3",
      "arctic_test_2yy",
      "chbenchmark5",
      "hellowrld7",
      "hwtesrtrt1"]
    })
  },
  'GET /ams/v1/catalogs/trino_online_env/databases/arctic100wdynamic/tables': (req, res) => {
    res.json({
      code: 200,
      msg: 'success',
      result: [
        {
          name: 'arctic_spark_test3'
        },
        {
          name: 'arctic_test'
        }
      ]
    })
  },
  'GET /ams/v1/catalogs/trino_online_env/databases/arctic100wfileSize/tables': (req, res) => {
    res.json({
      code: 200,
      msg: 'success',
      result: [
        {
          name: 'arctic_5555'
        },
        {
          name: 'arctic_666'
        }
      ]
    })
  },
  'GET /ams/v1/catalogs/local_catalog/databases/db/tables': (req, res) => {
    res.json({
      code: 200,
      msg: 'success',
      result: [
        {
          name: 'arctic_7777'
        },
        {
          name: 'arctic_8888'
        }
      ]
    })
  },
  'GET /api/v1/as/db/t1/detail': (req, res) => {
    res.json({
      code: 200,
      msg: 'success',
      result: {
        tableType: 'NEW_TABLE',
        tableIdentifier: {
          catalog: 'bdmstest_arctic',
          database: 'default',
          tableName: 'zyxtest',
          id: 580
        },
        schema: [
          {
            field: 'id',
            type: 'int',
            comment: '9NY3o9NY3oKew3C9NY3oKew3CKew3C'
          },
          {
            field: 'id2',
            type: 'int',
            comment: 'j3LlkVneOj'
          }
        ],
        pkList: [
          {
            field: 'id1id1id1id1id1id1id1',
            type: 'string',
            comment: 'comment1comment1comment1comment1'
          },
          {
            field: 'id2',
            type: 'string',
            comment: 'comment2'
          }
        ],
        partitionColumnList: [
          {
            field: 'oR08d7C7uS',
            sourceField: 'dN0ELHP0UM',
            transform: 'MhKJ0LJ8Iu'
          },
          {
            field: 'PGU1tfKH5b',
            sourceField: 'u2eqHWhRkk',
            transform: 'eSJU2b9zib'
          },
          {
            field: 'r54KVdiiCX',
            sourceField: '0xwvC4ujZ8',
            transform: 'EFWKEFWKsKPSJUEFWKEFWKsKPSJUsKPSJUEFWKEFWKsKPSJUsKPSJUsKPSJU'
          }
        ],
        properties: {
          EFWKEFWKsKPSJUEFWKEFWKsKPSJUsKPSJUEFWKEFWKsKPSJUsKPSJUsKPSJU: '148'
        },
        metrics: {
          lastCommitTime: 1651301798030,
          size: '12KB',
          file: 'file1',
          averageFileSize: '10KB'
        }
      }
    })
  },
  'GET /mock/ams/v1/tables/bdmstest_arctic/ndc_test_db/realtime_dw_inventory_poc_wt_3141/partitions': (req, res) => {
    res.json({
      code: 200,
      msg: 'success',
      result: {
        list: [
          {
            partition: '2022-03-02',
            fileCount: 16,
            size: '123KB',
            lastCommitTime: 1651301798030
          },
          {
            partition: '2022-03-03',
            fileCount: 20,
            size: '1234KB',
            lastCommitTime: 1651301798030
          }
        ],
        total: 2
      }
    })
  },
  'GET /api/v1/as/db/t1/2022-03-02/detail': (req, res) => {
    res.json({
      code: 200,
      msg: 'success',
      result: [
        {
          file: 'file',
          fsn: 'fsn1',
          type: 'type1',
          size: '123KB',
          commitTime: 1651301798030,
          commitId: 'commitId',
          path: 'path'
        },
        {
          file: 'file2',
          fsn: 'fsn2',
          type: 'type2',
          size: '1234KB',
          commitTime: 1651301798030,
          commitId: 'commitId2',
          path: 'path2'
        }
      ]
    })
  },
  'GET /api/v1/as/db/t1/transactions': (req, res) => {
    res.json({
      code: 200,
      msg: 'success',
      result: {
        list: [
          {
            id: 'id1',
            fileCount: 200,
            fileSize: 'Bic05xfOhE',
            commitTime: 1651301798030,
            snapshotId: '81213'
          },
          {
            id: 'id2',
            fileCount: 200,
            fileSize: 'OiuK7iAcU1',
            commitTime: 1651301798030,
            snapshotId: '82194'
          }
        ],
        total: 100
      }
    })
  },
  'GET /api/v1/tables/as/db/t1/optimize': (req, res) => {
    res.json({
      code: 200,
      msg: 'success',
      result: {
        list: [
          {
            tableIdentifier: {
              catalog: 'arctic_online_new',
              database: 'tmp_music',
              tableName: 'dim_moyi_itm_gift_base_dd',
              id: 205
            },
            compactRange: 'Partition',
            recordId: 103354,
            visibleTime: 1651301798030,
            commitTime: 1651301798030,
            planTime: 1651301798030,
            duration: 18173,
            parallelism: 10,
            totalFilesStatBeforeCompact: {
              fileCnt: 5,
              totalSize: 78539,
              averageSize: 15707
            },
            insertFilesStatBeforeCompact: {
              fileCnt: 4,
              totalSize: 66763,
              averageSize: 16690
            },
            deleteFilesStatBeforeCompact: {
              fileCnt: 0,
              totalSize: 0,
              averageSize: 0
            },
            baseFilesStatBeforeCompact: {
              fileCnt: 1,
              totalSize: 11776,
              averageSize: 11776
            },
            totalFilesStatAfterCompact: {
              fileCnt: 4,
              totalSize: 67985,
              averageSize: 16996
            },
            snapshotInfo: {
              snapshotId: 9197286040231952000,
              operation: null,
              totalSize: 7369863,
              totalFiles: 506,
              totalRecords: 653,
              addedFiles: null,
              addedFilesSize: 67985,
              addedRecords: 142,
              removedFilesSize: 11776,
              removedFiles: 1,
              removedRecords: 1
            },
            partitionCnt: 1,
            partitions: 'dt=2022-04-29',
            baseTableMaxFileSequence: '{dt=2022-04-29=12, dt=2022-04-28=7}'
          },
          {
            tableIdentifier: {
              catalog: 'arctic_online_new',
              database: 'tmp_music',
              tableName: 'dim_moyi_itm_gift_base_dd',
              id: 205
            },
            compactRange: 'Partition',
            recordId: 103354,
            visibleTime: 1651204980005,
            commitTime: 1651204980005,
            planTime: 1651204961832,
            duration: 18173,
            totalFilesStatBeforeCompact: {
              fileCnt: 5,
              totalSize: 78539,
              averageSize: 15707
            },
            insertFilesStatBeforeCompact: {
              fileCnt: 4,
              totalSize: 66763,
              averageSize: 16690
            },
            deleteFilesStatBeforeCompact: {
              fileCnt: 0,
              totalSize: 0,
              averageSize: 0
            },
            baseFilesStatBeforeCompact: {
              fileCnt: 1,
              totalSize: 11776,
              averageSize: 11776
            },
            totalFilesStatAfterCompact: {
              fileCnt: 4,
              totalSize: 67985,
              averageSize: 16996
            },
            snapshotInfo: {
              snapshotId: 9197286040231952000,
              operation: null,
              totalSize: 7369863,
              totalFiles: 506,
              totalRecords: 653,
              addedFiles: null,
              addedFilesSize: 67985,
              addedRecords: 142,
              removedFilesSize: 11776,
              removedFiles: 1,
              removedRecords: 1
            },
            partitionCnt: 1,
            partitions: 'dt=2022-04-29',
            baseTableMaxFileSequence: '{dt=2022-04-29=12, dt=2022-04-28=7}'
          },
          {
            tableIdentifier: {
              catalog: 'arctic_online_new',
              database: 'tmp_music',
              tableName: 'dim_moyi_itm_gift_base_dd',
              id: 205
            },
            compactRange: 'Partition',
            recordId: 103354,
            visibleTime: 1651204980005,
            commitTime: 1651204980005,
            planTime: 1651204961832,
            duration: 18173,
            totalFilesStatBeforeCompact: {
              fileCnt: 5,
              totalSize: 78539,
              averageSize: 15707
            },
            insertFilesStatBeforeCompact: {
              fileCnt: 4,
              totalSize: 66763,
              averageSize: 16690
            },
            deleteFilesStatBeforeCompact: {
              fileCnt: 0,
              totalSize: 0,
              averageSize: 0
            },
            baseFilesStatBeforeCompact: {
              fileCnt: 1,
              totalSize: 11776,
              averageSize: 11776
            },
            totalFilesStatAfterCompact: {
              fileCnt: 4,
              totalSize: 67985,
              averageSize: 16996
            },
            snapshotInfo: {
              snapshotId: 9197286040231952000,
              operation: null,
              totalSize: 7369863,
              totalFiles: 506,
              totalRecords: 653,
              addedFiles: null,
              addedFilesSize: 67985,
              addedRecords: 142,
              removedFilesSize: 11776,
              removedFiles: 1,
              removedRecords: 1
            },
            partitionCnt: 1,
            partitions: 'dt=2022-04-29',
            baseTableMaxFileSequence: '{dt=2022-04-29=12, dt=2022-04-28=7}'
          }
        ],
        total: 7
      }
    })
  },
  'GET /mock/ams/v1/terminal/examples': (req, res) => {
    res.json({
      code: 200,
      msg: 'success',
      result: [
        'CreateTable',
        'EditTable',
        'DeleteTable'
      ]
    })
  },
  'GET /mock/ams/v1/terminal/examples/CreateTable': (req, res) => {
    res.json({
      code: 200,
      msg: 'success',
      result: 'INSERT INTO sloth_test_qa_mysql_167.qa_slothdata.alltype1 select * FROM sloth_test_qa_mysql_167.qa_slothdata.alltype;INSERT INTO sloth_test_qa_mysql_167.qa_slothdata.alltype1 select * FROM sloth_test_qa_mysql_167.qa_slothdata.alltype;'
    })
  },
  'GET /mock/ams/v1/terminal/3/result': (req, res) => {
    res.json({
      code: 200,
      msg: 'success',
      result: [
        {
          id: 'Result1',
          status: 'Failed',
          columns: [
            'namespace'
          ],
          rowData: [
            [
              'arctic_test'
            ],
            [
              'arctic_test_2'
            ],
            [
              'hellowrld'
            ]
          ]
        },
        {
          id: 'Result2',
          status: 'Failed',
          columns: [
            'namespace'
          ],
          rowData: [
            [
              'arctic_test222222222222222222'
            ],
            [
              'arctic_test_24444444444444'
            ],
            [
              'hellowrld666666666666666666666'
            ]
          ]
        }
      ]
    })
  },
  'GET /mock/ams/v1/tables/catalogs/arctic_catalog_dev/dbs/ndc_test_db/tables/user_order_unpk/upgradeStatus': (req, res) => {
    res.json({
      code: 200,
      msg: 'success',
      result: {
        status: "upgrade",
        errorMessage: "errorMessage"
      }
    })
  },
  'GET /mock/ams/v1/tables/catalogs/arctic_catalog_dev/dbs/ndc_test_db/tables/user_order_unpk/hive/details': (req, res) => {
    res.json({
      code: 200,
      msg: 'success',
      result: {
        "tableType": "HIVE",
        "tableIdentifier": {
          "catalog": "bdmstest_arctic",
          "database": "default",
          "tableName": "zyxtest"
        },
        "schema": [
          {
            "field": "id1",
            "type": "int",
            "description": "rZiGhjpbqj"
          },
          {
            "field": "id2",
            "type": "int",
            "description": "x6T9Y8D7wi"
          },
          {
            "field": "id3",
            "type": "int",
            "description": "AWpoSVLR6f"
          },
          {
            "field": "id4",
            "type": "int",
            "description": "rZiGhjpbqj"
          },
          {
            "field": "id5",
            "type": "int",
            "description": "x6T9Y8D7wi"
          },
          {
            "field": "id6",
            "type": "int",
            "description": "AWpoSVLR6f"
          }
        ],
        "partitionColumnList": [
          {
            "field": "TqgUCqOfr0",
            "type": "bZpDUpDo2l",
            "description": "D3SVsvwmuD"
          },
          {
            "field": "g1tpuaWFg6",
            "type": "tJr2zYltbL",
            "description": "F5z48Arinv"
          },
          {
            "field": "I61mT0lDBP",
            "type": "dSDu69M3Ph",
            "description": "X6Nx4K7S8t"
          },
          {
            "field": "I61mT0welDBP",
            "type": "dSDu69M3Ph",
            "description": "X6Nwex4K7S8t"
          }
        ],
        "properties": {
          "xxxx": "148"
        }
      }
    })
  },
  'GET /mock/ams/v1/upgrade/properties': (req, res) => {
    res.json({
      code: 200,
      msg: 'success',
      result: {
        "key1": "koYg4SDRzM",
        "key2": "T3ScQHN0hE"
      }
    })
  },
  'POST /mock/ams/v1/tables/catalogs/arctic_catalog_dev/dbs/ndc_test_db/tables/user_order_unpk/upgrade': (req, res) => {
    res.json({
      code: 200,
      msg: 'success',
      result: {}
    })
  },
  'GET /mock/ams/v1/tables/catalogs/trino_online_env/dbs/arctic10wforOptimizeContinue/tables/nation/operations': (req, res) => {
    res.json({
      code: 200,
      msg: 'success',
      result: {
        list: [
          {
            ts: 11234567890123,
            operation: "sdsd"
          }
        ]
      }
    })
  },
  'GET /mock/ams/v1/overview/summary': (req, res) => {
    res.json({
      code: 200,
      msg: 'success',
      result: {
        "catalogCnt": 2,
        "tableCnt": 37944,
        "tableTotalSize": 10585900,
        "totalCpu": "6",
        "totalMemory": 62464
      }
    })
  },
  'GET /mock/ams/v1/overview/top/tables': (req, res) => {
    res.json({
      code: 200,
      msg: 'success',
      result: [
        {
          "tableName": "trino_online_env_hive.spark_test.ctpri",
          "size": 12938982,
          "fileCnt": 57889
        },
        {
          "tableName": "trino_online_env_hive.spark_test.ctpp_col",
          "size": 329043290,
          "fileCnt": 79910
        }
      ]
    })
  },
  'GET /mock/ams/v1/overview/metric/optimize/resource': (req, res) => {
    res.json({
      code: 200,
      msg: 'success',
      result: {
        "timeLine": [
          "10-09 14:48"
        ],
        "usedCpu": [
          "83.24"
        ],
        "usedCpuDivision": [
          "1828核/2196核"
        ],
        "usedCpuPercent": [
          "83.24%"
        ],
        "usedMem": [
          "83.24"
        ],
        "usedMemDivision": [
          "1828核/2196核10364G"
        ],
        "usedMemPercent": [
          "83.24%"
        ]
      }
    })
  },
  'GET /mock/ams/v1/catalogs/types': (req, res) => {
    res.json({
      code: 200,
      msg: 'success',
      result: [
        'hadoop',
        'hive'
      ]
    })
  },
  'GET /mock/ams/v1/catalogs/bdms_test_catalog_hive': (req, res) => {
    res.json({
      code: 200,
      msg: 'success',
      result: {
        name: 'bdms_test_catalog_hive',
        type: 'hive',
        storageConfig: {
          "storage_config.storage.type": "hdfs",
          "storage_config.core-site": {
            "fileName": "fileName1",
		        "filrUrl": "http://afk.qqzdvuzccctprn.bkfj"
          },
          "storage_config.hdfs-site": {
            "fileName": "fileName2",
		        "filrUrl": "http://afk.qqzdvuzccctprn.bkfj"
          },
          "storage_config.hive-site": {
            "fileName": "fileName3",
		        "filrUrl": "http://afk.qqzdvuzccctprn.bkfj"
          }
        },
        authConfig: {
          "auth_config.type": "simpel",
          "auth_config.hadoop_username": "omPRZh6bc8",
          "auth_config.principal": "L2TeTS0OzC",
          "auth_config.keytab":  {
            "fileName": "fileNamekeytab",
            "fileUrl": "http://bfu.qynoircxcut.civkj"
          },
          "auth_config.krb5": {
            "fileName": "fileNamekrb5",
            "fileUrl": "http://bfu.qynoircxcut.civkj"
          }
        },
        properties: {
          'key1': 'value1'
        }
      }
    })
  },
  'DELETE /mock/ams/v1/catalogs/bdms_test_catalog_hive': (req, res) => {
    res.json({
      code: 200,
      msg: 'success',
      result: {}
    })
  },
  'PUT /mock/ams/v1/catalogs/bdms_test_catalog_hive': (req, res) => {
    res.json({
      code: 200,
      msg: 'success',
      result: {}
    })
  },
  'GET /mock/ams/v1/check/catalogs/bdms_test_catalog_hive': (req, res) => {
    res.json({
      code: 200,
      msg: 'success',
      result: {}
    })
  },
  'GET /mock/ams/v1/settings/system': (req, res) => {
    res.json({
      code: 200,
      msg: 'success',
      result: {
        'arctic.ams.server-host.prefix': '127.0.0.1',
        'arctic.ams.thrift.port': '18112'
      }
    })
  },
  'GET /mock/ams/v1/settings/containers': (req, res) => {
    res.json({
      code: 200,
      msg: 'success',
      result: [
        {
          name: 'flinkcontainer',
          type: 'flink',
          properties: {
            'properties.FLINK_HOME': '/home/arctic/flink-1.12.7/'
          },
          optimizeGroup: [{
            name: 'flinkOp',
            tmMemory: '1024',
            jmMemory: 'sdsa'
          }]
        },
        {
          name: 'flinkcontainer2',
          type: 'flink',
          properties: {
            'properties.FLINK_HOME': '/home/arctic/flink-1.12.7/'
          },
          optimizeGroup: [{
            name: 'flinkOp',
            tmMemory: '1024',
            jmMemory: 'sdsa2'
          }]
        },
      ]
    })
  },
  'GET /mock/ams/v1/catalogs/bdms_test_catalog_hive/delete/check': (req, res) => {
    res.json({
      code: 200,
      msg: 'success',
      result: true
    })
  }
}
