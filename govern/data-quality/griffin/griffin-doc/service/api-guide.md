<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Apache Griffin API Guide

This page lists the major RESTful APIs provided by Apache Griffin.

Apache Griffin default `BASE_PATH` is `http://<your ip>:8080`. 

- [HTTP Response Design](#0)

- [Griffin Basic](#1)
    - [Get Version](#11)

- [Griffin Measures](#2)
    - [Add Measure](#21)
    - [Get Measure](#22)
    - [Remove Measure](#23)
    - [Update Measure](#24)

- [Griffin Jobs](#3)
    - [Add Job](#31)
    - [Trigger job by id](#37)
    - [Get Job](#32)
    - [Remove Job](#33)
    - [Get Job Instances](#34)
    - [Get Job Instance by triggerKey](#38)
    - [Get Job Healthy Statistics](#35)
    - [Download Sample Records](#36)
    - [Get Job Instance by Id](#38)

- [Metrics](#4)
    - [Get Metrics](#41)
    - [Add Metric Value](#42)
    - [Get Metric Value](#43)
    - [Remove Metric Value](#44)
    - [Get Metric Value by Job Instance Id](#45)

- [Hive MetaStore](#5)
    - [Get Table Metadata](#51)
    - [Get Table Name](#52)
    - [Get All Databases Metadata](#53)
    - [Get Database Names](#54)
    - [Get All Tables Metadata](#55)

- [Auth](#6)


<h2 id = "0"></h2>

## HTTP Response Design
We follow general rules to design Apache Griffin's REST APIs. In the HTTP response that is sent to a client, 
the status code, which is a three-digit number, is accompanied by a reason phrase (also known as status text) that simply describes the meaning of the code. 
The status codes are classified by number range, with each class of codes having the same basic meaning.
* The range 100-199 is classed as Informational.
* 200-299 is Successful.
* 300-399 is Redirection.
* 400-499 is Client error.
* 500-599 is Server error.

### Valid Apache Griffin Response
The valid HTTP response is designed as follows:

| Action | HTTP Status | Response Body |
| ---- | ------------------ | ------ |
| POST | 201, "Created" | created item |
| GET | 200, "OK" | requested items |
| PUT | 204, "No Content" | no content |
| DELETE | 204, "No Content" | no content |

***Note that:*** The metric module is implemented with elasticsearch bulk api, so the responses do not follow rules above.

### Invalid Apache Griffin Response
The response for exception is designed as follows:

| Action | HTTP Status | Response Body |
| ---- | ------------------ | ------ |
| ANY | 400, "Bad Request" | error detail |
| ANY | 500, "Internal Server Error" | error detail |
```
{
    "timestamp": 1517208444322,
    "status": 400,
    "error": "Bad Request",
    "code": 40009,
    "message": "Property 'measure.id' is invalid",
    "path": "/api/v1/jobs"
}
```
```
{
    "timestamp": 1517209428969,
    "status": 500,
    "error": "Internal Server Error",
    "message": "Failed to add metric values",
    "exception": "java.net.ConnectException",
    "path": "/api/v1/metrics/values"
}
```
Description:

- timestamp: the timestamp of response created
- status : the HTTP status code
- error : reason phrase of the HTTP status
- code: customized error code
- message : customized error message
- exception: fully qualified name of cause exception
- path: the requested api

***Note that:*** 'exception' field may not exist if it is caused by client error, and 'code' field may not exist for server error.

<h2 id = "1"></h2>

## Apache Griffin Basic

<div id = "11"></div>

### Get Apache Griffin version
`GET /api/v1/version`

#### API Example
```bash
curl -k -H "Accept: application/json" -X GET http://127.0.0.1:8080/api/v1/version
0.3.0
```

<h2 id = "2"></h2>

## Griffin Measures

<div id = "21"></div>

### Add measure
`POST /api/v1/measures`

#### Request Header
| key          | value            |
| ------------ | ---------------- |
| Content-Type | application/json |

#### API Example
There are two kinds of measures, Apache Griffin measure and external measure.
<br>The measure's 'dq.type' can either be 'ACCURACY' or 'PROFILING'.

Here is an example to define measure of profiling:
```
curl -k -H "Content-Type: application/json" -H "Accept: application/json" \
-X POST http://127.0.0.1:8080/api/v1/measures \
-d '{
      "name": "profiling_measure",
      "measure.type": "griffin",
      "dq.type": "PROFILING",
      "rule.description": {
        "details": [
          {
            "name": "age",
            "infos": "Total Count,Average"
          }
        ]
      },
      "process.type": "BATCH",
      "owner": "test",
      "description": "measure description",
      "data.sources": [
        {
          "name": "source",
          "connector": {
            "name": "connector_name",
            "type": "HIVE",
            "version": "1.2",
            "data.unit": "1hour",
            "data.time.zone": "UTC(WET,GMT)",
            "config": {
              "database": "default",
              "table.name": "demo_src",
              "where": "dt=#YYYYMMdd# AND hour=#HH#"
            },
            "predicates": [
              {
                "type": "file.exist",
                "config": {
                  "root.path": "hdfs:///griffin/demo_src",
                  "path": "/dt=#YYYYMMdd#/hour=#HH#/_DONE"
                }
              }
            ]
          }
        }
      ],
      "evaluate.rule": {
        "rules": [
          {
            "dsl.type": "griffin-dsl",
            "dq.type": "PROFILING",
            "rule": "count(source.`age`) AS `age-count`,avg(source.`age`) AS `age-average`",
            "name": "profiling",
            "details": {}
          }
        ]
      }
    }'
```
Here is an example to define measure of accuracy:
```
curl -k -H "Content-Type: application/json" -H "Accept: application/json" \
-X POST http://127.0.0.1:8080/api/v1/measures \
-d '{
      "name": "accuracy_measure",
      "measure.type": "griffin",
      "dq.type": "ACCURACY",
      "process.type": "BATCH",
      "owner": "test",
      "description": "measure description",
      "data.sources": [
        {
          "name": "source",
          "connector": {
            "name": "connector_name_source",
            "type": "HIVE",
            "version": "1.2",
            "data.unit": "1hour",
            "data.time.zone": "UTC(WET,GMT)",
            "config": {
              "database": "default",
              "table.name": "demo_src",
              "where": "dt=#YYYYMMdd# AND hour=#HH#"
            },
            "predicates": [
              {
                "type": "file.exist",
                "config": {
                  "root.path": "hdfs:///griffin/demo_src",
                  "path": "/dt=#YYYYMMdd#/hour=#HH#/_DONE"
                }
              }
            ]
          }
        },
        {
          "name": "target",
          "connector": {
            "name": "connector_name_target",
            "type": "HIVE",
            "version": "1.2",
            "data.unit": "1hour",
            "data.time.zone": "UTC(WET,GMT)",
            "config": {
              "database": "default",
              "table.name": "demo_tgt",
              "where": "dt=#YYYYMMdd# AND hour=#HH#"
            },
            "predicates": [
              {
                "type": "file.exist",
                "config": {
                  "root.path": "hdfs:///griffin/demo_src",
                  "path": "/dt=#YYYYMMdd#/hour=#HH#/_DONE"
                }
              }
            ]
          }
        }
      ],
      "evaluate.rule": {
        "rules": [
          {
            "dsl.type": "griffin-dsl",
            "dq.type": "ACCURACY",
            "name": "accuracy",
            "rule": "source.desc=target.desc"
          }
        ]
      }
    }'
```
Here is an example to define external measure:
```
curl -k -H "Content-Type: application/json" -H "Accept: application/json" \
-X POST http://127.0.0.1:8080/api/v1/measures \
-d '{
    "name": "external_name",
    "measure.type": "external",
    "dq.type": "ACCURACY",
    "description": "measure description",
    "organization": "orgName",
    "owner": "test",
    "metric.name": "metricName"
}'
```

<div id = "22"></div>

### Get measure
`GET /api/v1/measures`<br>
`GET /api/v1/measures/{measure_id}`

#### API Example
```bash
curl -k -H "Accept: application/json" -X GET http://127.0.0.1:8080/api/v1/measures
[
  {
    "measure.type": "griffin",
    "id": 1,
    "name": "accuracy_measure",
    "owner": "test",
    "description": "measure description",
    "deleted": false,
    "dq.type": "ACCURACY",
    "sinks": [
      "ELASTICSEARCH",
      "HDFS"
    ],
    "process.type": "BATCH",
    "data.sources": [
      {
        "id": 4,
        "name": "source",
        "connector": {
          "id": 5,
          "name": "connector_name_source",
          "type": "HIVE",
          "version": "1.2",
          "predicates": [
            {
              "id": 6,
              "type": "file.exist",
              "config": {
                "root.path": "hdfs:///127.0.0.1/demo_src",
                "path": "/dt=#YYYYMMdd#/hour=#HH#/_DONE"
              }
            }
          ],
          "data.unit": "1hour",
          "data.time.zone": "UTC(WET,GMT)",
          "config": {
            "database": "default",
            "table.name": "demo_src",
            "where": "dt=#YYYYMMdd# AND hour=#HH#"
          }
        },
        "baseline": false
      },
      {
        "id": 7,
        "name": "target",
        "connector": {
          "id": 8,
          "name": "connector_name_target",
          "type": "HIVE",
          "version": "1.2",
          "predicates": [
            {
              "id": 9,
              "type": "file.exist",
              "config": {
                "root.path": "hdfs:///127.0.0.1/demo_src",
                "path": "/dt=#YYYYMMdd#/hour=#HH#/_DONE"
              }
            }
          ],
          "data.unit": "1hour",
          "data.time.zone": "UTC(WET,GMT)",
          "config": {
            "database": "default",
            "table.name": "demo_tgt",
            "where": "dt=#YYYYMMdd# AND hour=#HH#"
          }
        },
        "baseline": false
      }
    ],
    "evaluate.rule": {
      "id": 2,
      "rules": [
        {
          "id": 3,
          "rule": "source.desc=target.desc",
          "dsl.type": "griffin-dsl",
          "dq.type": "ACCURACY"
        }
      ]
    }
  }
]
```

<div id = "23"></div>

### Remove measure
`DELETE /api/v1/measures/{measure_id}`
When deleting a measure,api will also delete related jobs.
#### API example
```bash
curl -k -H "Accept: application/json" -X DELETE http://127.0.0.1:8080/api/v1/measures/1
```
The response body should be empty if no error happens, and the HTTP status is (204, "No Content").

<div id = "24"></div>

### Update measure
`PUT /api/v1/measures`

#### API example
Here is an example to update measure:
```
curl -k -H "Content-Type: application/json" -H "Accept: application/json" \
-X PUT http://127.0.0.1:8080/api/v1/measures \
-d '{
      "measure.type": "griffin",
      "id": 19,
      "name": "profiling_measure_edited",
      "owner": "test",
      "description": "measure description",
      "deleted": false,
      "dq.type": "PROFILING",
      "sinks": [
        "ELASTICSEARCH",
        "HDFS"
      ],
      "process.type": "BATCH",
      "rule.description": {
        "details": [
          {
            "name": "age",
            "infos": "Total Count,Average"
          }
        ]
      },
      "data.sources": [
        {
          "id": 22,
          "name": "source",
          "connector": {
            "id": 23,
            "name": "connector_name",
            "type": "HIVE",
            "version": "1.2",
            "predicates": [
              {
                "id": 24,
                "type": "file.exist",
                "config": {
                  "root.path": "hdfs:///griffin/demo_src",
                  "path": "/dt=#YYYYMMdd#/hour=#HH#/_DONE"
                }
              }
            ],
            "data.unit": "1hour",
            "data.time.zone": "UTC(WET,GMT)",
            "config": {
              "database": "default",
              "table.name": "demo_src",
              "where": "dt=#YYYYMMdd# AND hour=#HH#"
            }
          },
          "baseline": false
        }
      ],
      "evaluate.rule": {
        "id": 20,
        "rules": [
          {
            "id": 21,
            "rule": "count(source.`age`) AS `age-count`,avg(source.`age`) AS `age-average`",
            "dsl.type": "griffin-dsl",
            "dq.type": "PROFILING",
            "details": {}
          }
        ]
      }
    }'
```
Here is an example to update external measure:
```
curl -k -H "Content-Type: application/json" -H "Accept: application/json" \
-X PUT http://127.0.0.1:8080/api/v1/measures \
-d '{
    "measure.type": "external",
    "id": 25,
    "name": "external_name",
    "owner": "test",
    "description": "measure description edited",
    "organization": "orgName",
    "deleted": false,
    "dq.type": "ACCURACY",
    "sinks": ["ELASTICSEARCH", "HDFS"],
    "metric.name": "metricName",
    "measure.type": "external"
}'
```

<h2 id = "3"></h2>

## Griffin Jobs

<div id = "31"></div>

### Add job

`POST /api/v1/jobs`

#### API Example
```
curl -k -H "Content-Type: application/json" -H "Accept: application/json" \
-X POST http://127.0.0.1:8080/api/v1/jobs \
-d '{
    "measure.id": 10,
    "job.name":"job_name_10",
    "job.type":"batch",
    "cron.expression": "0 0/4 * * * ?",
    "cron.time.zone": "GMT+8:00",
    "predicate.config": {
        "checkdonefile.schedule":{
            "interval": "1m",
            "repeat": 2
        }
    },
    "data.segments": [
        {
            "data.connector.name": "connector_name_source",
            "as.baseline":true,
            "segment.range": {
                "begin": "-1h",
                "length": "1h"
            }
        },
        {
            "data.connector.name": "connector_name_target",
            "segment.range": {
                "begin": "-1h",
                "length": "1h"
            }
        }
    ]
}'
```

<div id = "37"></div>

### Trigger job by id
`POST /api/v1/jobs/trigger/{job_id}`

In the current version triggering the job in this way leads to scheduling of a single job instance. The method returns
immediately even if starting it may take time. The response contains `triggerKey` by which the instance could be found
when it is started (see [find instance by trigger key](#38)).

#### API Example
```
curl -k -X POST http://127.0.0.1:8080/api/v1/jobs/trigger/101
{
    "triggerKey": "DEFAULT.6da64b5bd2ee-34e2cb23-11a2-4f92-9cbd-6cb3402cdb48",
}
```

<div id = "32"></div>

### Get all jobs
`GET /api/v1/jobs`

#### API Example
```
curl -k -H "Content-Type: application/json" -H "Accept: application/json" \
-X GET http://127.0.0.1:8080/api/v1/jobs
[{
        "job.type": "batch",
        "id": 51,
        "measure.id": 10,
        "job.name": "job_name_10",
        "metric.name": "job_name_10",
        "quartz.name": "job_name_10_1547192473206",
        "quartz.group": "BA",
        "cron.expression": "0 0/4 * * * ?",
        "job.state": {
            "state": "NORMAL",
            "toStart": false,
            "toStop": true,
            "nextFireTime": 1547693040000,
            "previousFireTime": 1547692800000
        },
        "cron.time.zone": "GMT+8:00",
        "predicate.config": {
            "checkdonefile.schedule": {
                "interval": "1m",
                "repeat": 2
            }
        },
        "data.segments": [{
                "id": 52,
                "data.connector.name": "connector_name_source",
                "as.baseline": true,
                "segment.range": {
                    "id": 53,
                    "begin": "-1h",
                    "length": "1h"
                }
            }, {
                "id": 54,
                "data.connector.name": "connector_name_target",
                "as.baseline": false,
                "segment.range": {
                    "id": 55,
                    "begin": "-1h",
                    "length": "1h"
                }
            }
        ],
        "job.type": "batch"
    }
]
```

### Get a job by id
`GET /api/v1/jobs/config?jobId={job_id}`

#### API Example
```
curl -k -H "Content-Type: application/json" -H "Accept: application/json" \
-X GET http://127.0.0.1:8080/api/v1/jobs/config?jobId=827
{
    "job.type": "batch",
    "id": 827,
    "measure.id": 10,
    "job.name": "job_name_10",
    "metric.name": "job_name_10",
    "quartz.name": "job_name_10_1547694147531",
    "quartz.group": "BA",
    "cron.expression": "0 0/4 * * * ?",
    "cron.time.zone": "GMT+8:00",
    "predicate.config": {
        "checkdonefile.schedule": {
            "interval": "1m",
            "repeat": 2
        }
    },
    "data.segments": [{
            "id": 828,
            "data.connector.name": "connector_name_source",
            "as.baseline": true,
            "segment.range": {
                "id": 829,
                "begin": "-1h",
                "length": "1h"
            }
        }, {
            "id": 830,
            "data.connector.name": "connector_name_target",
            "as.baseline": false,
            "segment.range": {
                "id": 831,
                "begin": "-1h",
                "length": "1h"
            }
        }
    ],
    "job.type": "batch"
}
```

<div id = "33"></div>

### Delete job by id
`DELETE /api/v1/jobs/{job_id}`
#### API Example
```
curl -k -H "Content-Type: application/json" -H "Accept: application/json" \
-X DELETE http://127.0.0.1:8080/api/v1/jobs/51
```

### Delete job by name
`DELETE /api/v1/jobs?jobName={name}`
#### API Example
```
curl -k -H "Content-Type: application/json" -H "Accept: application/json" \
-X DELETE http://127.0.0.1:8080/api/v1/jobs?jobName=job_name_10
```

The response body should be empty if no error happens, and the HTTP status is (204, "No Content").

<div id = "34"></div>

### Get job instances
`GET /api/v1/jobs/instances?jobId={id}&page={pageNum}&size={pageSize}`

#### Request Parameter

| name  | description                         | type | example value |
| ----- | ----------------------------------- | ---- | ------------- |
| jobId | job id                              | Long | 1             |
| page  | page you want starting from index 0 | int  | 0             |
| size  | instance number per page            | int  | 10            |

#### API Example
```
curl -k -G -X GET http://127.0.0.1:8080/api/v1/jobs/instances -d jobId=827 -d page=1 -d size=5
[{
        "id": 1176,
        "sessionId": null,
        "state": "NOT_FOUND",
        "type": "BATCH",
        "predicateGroup": "PG",
        "predicateName": "job_name_10_predicate_1547776800012",
        "timestamp": 1547776800012,
        "expireTimestamp": 1548381600012
    }, {
        "id": 1175,
        "sessionId": null,
        "state": "NOT_FOUND",
        "type": "BATCH",
        "predicateGroup": "PG",
        "predicateName": "job_name_10_predicate_1547776560018",
        "timestamp": 1547776560019,
        "expireTimestamp": 1548381360019
    }
]
```

<div id = "38"></div>

### Find job instance by triggerKey
`GET /api/v1/jobs/triggerKeys/{triggerKey}`

This could be used after [triggering the job by job id](#37) to find the job instance when it is scheduled.
In the current version no more than one instance is triggered and thus the response is a list with single
element (or empty list if not found).

```
curl http://127.0.0.1:8080/api/v1/jobs/triggerKeys/DEFAULT.6da64b5bd2ee-34e2cb23-11a2-4f92-9cbd-6cb3402cdb48
[
    {
        "id":201,
        "sessionId":1,
        "state":"SUCCESS",
        "type":"BATCH",
        "appId":"application_1554199833471_0002",
        "appUri":"http://localhost:38088/cluster/app/application_1554199833471_0002",
        "predicateGroup":"PG",
        "predicateName":"acc1a_name_predicate_1554202748883",
        "triggerKey":"DEFAULT.6da64b5bd2ee-34e2cb23-11a2-4f92-9cbd-6cb3402cdb49",
        "timestamp":1554202748884,
        "expireTimestamp":1554807548884
    }
]
```


<div id = "35"></div>

### Get job healthy statistics
`GET /api/v1/jobs/health`

#### API Example
```
curl -k -X GET http://127.0.0.1:8080/api/v1/jobs/health
{
	"healthyJobCount": 0,
	"jobCount": 1
}
```

<div id = "36"></div>

### Download sample records
`GET /api/v1/jobs/download?jobName={name}&ts={timestamp}`

#### Request Parameter

| name  | description                         | type | example value |
| ----- | ----------------------------------- | ---- | ------------- |
| jobName    | job name                       | String | 1             |
| timestamp  | timestamp                      | Long   | 0             |

#### API Example
```
curl -k -G -X GET http://127.0.0.1:8080/api/v1/jobs/download \
-d jobName=job_name_10 -d timestamp=1547778857807
```
If successful, this method returns missing records in the response body, maximum record count is 100.

<div id = "38"></div>

### Get Job Instance by Id
`GET /api/v1/jobs/instances/{jobInstanceId}`

#### API Example
```
curl -k -G -X GET http://127.0.0.1:8080/api/v1/jobs/instances/1
```
If successful, this method returns job instance description for the given job instance id. If there is no instance with given id found, returns Griffin Exception.

<h2 id = "4"></h2>

## Metrics

<div id = "41"></div>

### Get metrics

`GET /api/v1/metrics`

#### API Example
The response is a map of metrics group by measure name. For example:
```
curl -k -X GET http://127.0.0.1:8080/api/v1/metrics
{
    "measure_no_predicate_day": [
        {
            "name": "job_no_predicate_day",
            "type": "accuracy",
            "owner": "test",
            "metricValues": [
                {
                    "name": "job_no_predicate_day",
                    "tmst": 1517994480000,
                    "value": {
                        "total": 125000,
                        "miss": 0,
                        "matched": 125000
                    }
                },
                {
                    "name": "job_no_predicate_day",
                    "tmst": 1517994240000,
                    "value": {
                        "total": 125000,
                        "miss": 0,
                        "matched": 125000
                    }
                }
            ]
        }
    ],
    "measure_predicate_hour": [
        {
            "name": "job_predicate_hour",
            "type": "accuracy",
            "owner": "test",
            "metricValues": []
        }
    ]
}
```

<div id = "42"></div>

### Add metric values
`POST /api/v1/metrics/values`
#### Request Body
| name          | description             | type        |
| ------------- | ----------------------- | ----------- |
| Metric Values | A list of metric values | MetricValue |
#### API Example
```
curl -k -H "Content-Type: application/json" -H "Accept: application/json" \
-X POST http://127.0.0.1:8080/api/v1/metrics/values \
-d '[
    {
        "name" : "metricName",
        "tmst" : 1509599811123,
        "value" : {
            "__tmst" : 1509599811123,
            "miss" : 11,
            "total" : 125000,
            "matched" : 124989
        }
   }
]'
```
The response body should have 'errors' field as 'false' if success, for example
```
{
    "took": 32,
    "errors": false,
    "items": [
        {
            "index": {
                "_index": "griffin",
                "_type": "accuracy",
                "_id": "AWFAs5pOJwYEbKWP7mhq",
                "_version": 1,
                "result": "created",
                "_shards": {
                    "total": 2,
                    "successful": 1,
                    "failed": 0
                },
                "created": true,
                "status": 201
            }
        }
    ]
}
```

<div id = "43"></div>

### Get metric values by name 
`GET /api/v1/metrics/values?metricName={name}&size={size}&offset={offset}&tmst={timestamp}`

#### Request Parameter
name | description | type | example value
--- | --- | --- | ---
metricName | name of the metric values | String | job_no_predicate_day
size | max amount of return records | int | 5
offset | the amount of records to skip by timestamp in descending order | int | 0
tmst | the start timestamp of records you want to get | long | 0

Parameter offset and tmst are optional.
#### API Example
```
curl -k -G -X GET http://127.0.0.1:8080/api/v1/metrics/values -d metricName=job_no_predicate_day -d size=10
[
    {
        "name": "job_no_predicate_day",
        "tmst": 1517994720000,
        "value": {
            "total": 125000,
            "miss": 0,
            "matched": 125000
        }
    },
    {
        "name": "job_no_predicate_day",
        "tmst": 1517994480000,
        "value": {
            "total": 125000,
            "miss": 0,
            "matched": 125000
        }
    },
    {
        "name": "job_no_predicate_day",
        "tmst": 1517994240000,
        "value": {
            "total": 125000,
            "miss": 0,
            "matched": 125000
        }
    }
]
```

<div id = "44"></div>

### Delete metric values by name
`DELETE /api/v1/metrics/values?metricName={name}`
#### API Example
The response body should have 'failures' field as empty if success, for example
```
curl -k -H "Accept: application/json" \
-X DELETE http://127.0.0.1:8080/api/v1/metrics/values?metricName=job_no_predicate_day
{
    "took": 363,
    "timed_out": false,
    "total": 5,
    "deleted": 5,
    "batches": 1,
    "version_conflicts": 0,
    "noops": 0,
    "retries": {
        "bulk": 0,
        "search": 0
    },
    "throttled_millis": 0,
    "requests_per_second": -1,
    "throttled_until_millis": 0,
    "failures": []
}
```

<div id = "45"></div>

### Get Metric Value by Job Instance Id
`GET http://127.0.0.1:8080/api/v1/metrics/values/:jobInstanceId`
#### API Example
```
curl -k -G -X GET http://127.0.0.1:8080/api/v1/metrics/values/{304}
{
    "name": "some_job",
    "tmst": 1553526960000,
    "value": {
        "total": 74,
        "miss": 31,
        "matched": 43,
        "matchedFraction": 0.581081081081081
    },
    "metadata": {
        "applicationId": "\"application_1549876136110_0237\"",
    }
}
```





<h2 id = "5"></h2>

### Hive MetaStore

<div id = "51"></div>

### Get table metadata

`GET /api/v1/metadata/hive/table?db={}&table={}`

#### Request Parameters
| name  | description        | type   | example value |
| ----- | ------------------ | ------ | ------------- |
| db    | hive database name | String | default       |
| table | hive table name    | String | demo_src      |

#### API Example
```
curl -k -H "Accept: application/json" \
-G -X GET http://127.0.0.1:8080/api/v1/metadata/hive/table \
-d db=default \
-d table=demo_src
{
    "tableName": "demo_src",
    "dbName": "default",
    "owner": "root",
    "createTime": 1505986176,
    "lastAccessTime": 0,
    "retention": 0,
    "sd": {
        "cols": [
            {
                "name": "id",
                "type": "bigint",
                "comment": null,
                "setName": true,
                "setType": true,
                "setComment": false
            },
            {
                "name": "age",
                "type": "int",
                "comment": null,
                "setName": true,
                "setType": true,
                "setComment": false
            },
            {
                "name": "desc",
                "type": "string",
                "comment": null,
                "setName": true,
                "setType": true,
                "setComment": false
            }
        ],
        "location": "hdfs://sandbox:9000/griffin/data/batch/demo_src"
    },
    "partitionKeys": [
        {
            "name": "dt",
            "type": "string",
            "comment": null,
            "setName": true,
            "setType": true,
            "setComment": false
        },
        {
            "name": "hour",
            "type": "string",
            "comment": null,
            "setName": true,
            "setType": true,
            "setComment": false
        }
    ]
}
```

<div id = "52"></div>

### Get table names
`GET /api/v1/metadata/hive/tables/names?db={}`
#### Request Parameter
| name | description        | typ    | example value |
| ---- | ------------------ | ------ | ------------- |
| db   | hive database name | String | default       |

#### API Example
```
curl -k -H "Accept: application/json" \
-X GET http://127.0.0.1:8080/api/v1/metadata/hive/table?db=default
[
  "demo_src",
  "demo_tgt"
]
```

<div id = "53"></div>

### Get all database tables metadata
`GET /api/v1/metadata/hive/dbs/tables`
#### API Example
```
curl -k -H "Accept: application/json" \
-X GET http://127.0.0.1:8080/api/v1/metadata/hive/dbs/tables
{
   "default": [
    {
      "tableName": "demo_src",
      "dbName": "default",
      "owner": "root",
      "createTime": 1505986176,
      "lastAccessTime": 0,
      "sd": {
        "cols": [
          {
            "name": "id",
            "type": "bigint",
            "comment": null,
            "setComment": false,
            "setType": true,
            "setName": true
          },
          {
            "name": "age",
            "type": "int",
            "comment": null,
            "setComment": false,
            "setType": true,
            "setName": true
          },
          {
            "name": "desc",
            "type": "string",
            "comment": null,
            "setComment": false,
            "setType": true,
            "setName": true
          }
        ],
        "location": "hdfs://sandbox:9000/griffin/data/batch/demo_src"
      },
      "partitionKeys": [
        {
          "name": "dt",
          "type": "string",
          "comment": null,
          "setComment": false,
          "setType": true,
          "setName": true
        },
        {
          "name": "hour",
          "type": "string",
          "comment": null,
          "setComment": false,
          "setType": true,
          "setName": true
        }
      ]
    },
    {
      "tableName": "demo_tgt",
      "dbName": "default",
      "owner": "root",
      "createTime": 1505986176,
      "lastAccessTime": 0,
      "sd": {
        "cols": [
          {
            "name": "id",
            "type": "bigint",
            "comment": null,
            "setComment": false,
            "setType": true,
            "setName": true
          },
          {
            "name": "age",
            "type": "int",
            "comment": null,
            "setComment": false,
            "setType": true,
            "setName": true
          },
          {
            "name": "desc",
            "type": "string",
            "comment": null,
            "setComment": false,
            "setType": true,
            "setName": true
          }
        ],
        "location": "hdfs://sandbox:9000/griffin/data/batch/demo_tgt"
      },
      "partitionKeys": [
        {
          "name": "dt",
          "type": "string",
          "comment": null,
          "setComment": false,
          "setType": true,
          "setName": true
        },
        {
          "name": "hour",
          "type": "string",
          "comment": null,
          "setComment": false,
          "setType": true,
          "setName": true
        }
      ]
    }
  ]
}

```

<div id = "54"></div>

### Get database names
`GET /api/v1/metadata/hive/dbs`
#### API Example
```
curl -k -H "Accept: application/json" \
-X GET http://127.0.0.1:8080/api/v1/metadata/hive/dbs
[
    "default"
]
```

<div id = "55"></div>

### Get tables metadata
`GET /api/v1/metadata/hive/tables?db={name}`
#### Request Parameter
| name | description        | typ    | example value |
| ---- | ------------------ | ------ | ------------- |
| db   | hive database name | String | default       |
#### API Example
```
curl -k -H "Accept: application/json" \
-X GET http://127.0.0.1:8080/api/v1/metadata/hive/tables?db=default
[
  {
    "tableName": "demo_src",
    "dbName": "default",
    "owner": "root",
    "createTime": 1508216660,
    "lastAccessTime": 0,
    "retention": 0,
    "sd": {
      "cols": [
        {
          "name": "id",
          "type": "bigint",
          "comment": null,
          "setName": true,
          "setType": true,
          "setComment": false
        },
        {
          "name": "age",
          "type": "int",
          "comment": null,
          "setName": true,
          "setType": true,
          "setComment": false
        },
        {
          "name": "desc",
          "type": "string",
          "comment": null,
          "setName": true,
          "setType": true,
          "setComment": false
        }
      ],
      "location": "hdfs://sandbox:9000/griffin/data/batch/demo_src"
    },
    "partitionKeys": [
      {
        "name": "dt",
        "type": "string",
        "comment": null,
        "setName": true,
        "setType": true,
        "setComment": false
      },
      {
        "name": "hour",
        "type": "string",
        "comment": null,
        "setName": true,
        "setType": true,
        "setComment": false
      }
    ]
  },
  {
    "tableName": "demo_tgt",
    "dbName": "default",
    "owner": "root",
    "createTime": 1508216660,
    "lastAccessTime": 0,
    "retention": 0,
    "sd": {
      "cols": [
        {
          "name": "id",
          "type": "bigint",
          "comment": null,
          "setName": true,
          "setType": true,
          "setComment": false
        },
        {
          "name": "age",
          "type": "int",
          "comment": null,
          "setName": true,
          "setType": true,
          "setComment": false
        },
        {
          "name": "desc",
          "type": "string",
          "comment": null,
          "setName": true,
          "setType": true,
          "setComment": false
        }
      ],
      "location": "hdfs://sandbox:9000/griffin/data/batch/demo_tgt"
 },
    "partitionKeys": [
      {
        "name": "dt",
        "type": "string",
        "comment": null,
        "setName": true,
        "setType": true,
        "setComment": false
      },
      {
        "name": "hour",
        "type": "string",
        "comment": null,
        "setName": true,
        "setType": true,
        "setComment": false
      }
    ]
  }
]
```


<h2 id = "6"></h2>

## Auth

### User authentication
`POST /api/v1/login/authenticate`

#### Request Parameter
| name | description                           | type | example value                           |
| ---- | ------------------------------------- | ---- | --------------------------------------- |
| map  | a map contains user name and password | Map  | `{"username":"user","password":"test"}` |

#### API Example
```
curl -k -H "Content-Type: application/json" -H "Accept: application/json" \
-X POST http://127.0.0.1:8080/api/v1/login/authenticate \
-d '{"username":"user","password":"test"}'
```
if authentication passes, response below will be returned.
```
{
  "fullName": "Default",
  "ntAccount": "user",
  "status": 0
}
```
