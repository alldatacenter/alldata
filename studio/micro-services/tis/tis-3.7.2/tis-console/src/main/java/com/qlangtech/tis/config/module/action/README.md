## 一键自动化构建TiDB 单表索引REST接口描述

### Url

http://${host}:8080/solr/config/config.ajax?action=collection_action&emethod=do_create

### Request Body

``` javascript
{
 datasource: {
    plugin: "TiKV",
    pdAddrs: "192.168.28.201:2379",
    dbName: "employees"
 },
 table: "employess",
 indexName: "employess",
 columns: [
  {name:"id",token:"ik",search:true}
 ]
 ,
 incr: {
   plugin: "TiCDC-Kafka"，
   mqAddress: "192.168.28.201:9092" ,
   topic: "test_topic"  ,
   groupId: "test_group" ,
   offsetResetStrategy: "earliest" #earliest or latest or none
 }
}
```

### Response 

``` javascript
{
 success: true,
 errormsg:["err1"],
 bizresult: {
  taskid: 123  # 后续可以根据此id 轮询任务执行状态
 }
}
```
## 取得索引状态
### Url
http://${host}:8080/solr/config/config.ajax?action=collection_action&emethod=do_get_index_status
### Request Body
``` javascript
{
 indexName: ""
 log:  true
}
```
### Response 

``` javascript
{
  doc_num: 99999999,
  latest_fullbuild_time: "2020-11-11 12:00:00",
  incr_ready: false # 增量是否开通 
}
```
## 轮询索引构建结果

### Url

http://${host}:8080/solr/config/config.ajax?action=collection_action&emethod=do_get_task_status

### Request Body

``` javascript
{
 taskid: 123
 log: true # 是否显示日志
}
```

### Response 

``` javascript
{
 success: true,
 errormsg:["err1"], # 系统级异常信息
 bizresult: {
  taskid: 123
  complete: false,
  faild: false,
  stage: "dump", # dump,join,indexBuild,indexBackflow
  errs: "" ,
  logs: "" # 提交参数中 log 为true
 }
}
```

## 查询增量通道状态

### Url 
http://${host}:8080/solr/config/config.ajax?action=collection_action&emethod=do_get_incr_status

### Request Body
``` javascript
{
 indexName: "employess",
 log: true
}
```
### Response 

``` javascript
{
 success: true,
 errormsg:["err1"], # 系统级异常信息
 bizresult: {
  current_cursor:[
     {mqName:"testKafka",cursor:123}
  ]  # mq消费的游标
  last_60_sec: 20, #最近 60秒内的流量
  logs: ""         #提交参数中 log 为true
 }
}
```

## 创建增量通道

如果之前创建的索引实例没有创建增量通道，就可以创建，不然，报错

### Url 
http://${host}:8080/solr/config/config.ajax?action=collection_action&emethod=do_create_incr

### Request Body

``` javascript
{
 indexName: "employess" ,
 incr: {
   plugin: "TiCDC-Kafka"，
   mqAddress: "192.168.28.201:9092" ,
   topic: "test_topic"  ,
   groupId: "test_group" ,
   offsetResetStrategy: "earliest" #earliest or latest or none,可选
 }
}
```

### Response 

``` javascript
{
 success: true,
 errormsg:["err1"], # 系统级异常信息
 bizresult: {
  logs: "" # 增量实行日志
 }
}
```

## 执行全量构建

如果业务方数据有变更需要重新构建全量数据

### Url
http://${host}:8080/solr/config/config.ajax?action=collection_action&emethod=do_fullbuild

### Request Body
``` javascript
{
 indexName: "employess"
}
```

### Response 
``` javascript
{
 success: true,
 errormsg:["err1"],
 bizresult: {
  taskid: 123  # 后续可以根据此id 轮询任务执行状态
 }
}
```
后续可以根据`do_get_status`来轮询执行状态

## 删除已创建的索引
将索引删除，如果已经开通增量通道，一并将增量实例也删除

### Url
http://${host}:8080/solr/config/config.ajax?action=collection_action&emethod=do_delete_index

### Request Body
``` javascript
{
 indexName: "employess"
}
```

### Response
``` javascript
{
 success: false, # 删除操作是否成功
 errormsg:["err1"],
 bizresult: {
 }
}
```



