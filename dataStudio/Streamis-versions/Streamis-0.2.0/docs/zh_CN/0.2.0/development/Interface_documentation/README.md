## 1. 目录

* [StreamDataSource 接口文档]()
* [StreamJobManager 接口文档]()
* [StreamWorkflow 接口文档]()

## 2. URL规范

```
/api/rest_j/v1/streamis/{moduleName}/.+
```

**约定**：

 - rest_j表示接口符合Jersey规范
 - v1为服务的版本号，**版本号会随着 Linkis 版本进行升级**
 - streamis为微服务名
 - {moduleName}为模块名，其中：
    * StreamDataSource 模块名命名为 streamDataSource；
    * StreamJobManager 模块名命名为 streamJobManager；
    * StreamWorkflow 模块名命名为 streamWorkflow；

## 3. 接口请求格式

## 3. 接口请求格式

```json
{
 	"method": "/api/rest_j/v1/streamis/.+",
 	"data": {}
}
```

**约定**：

 - method：请求的Restful API URL。
 - data：请求的具体数据。

## 4. 接口返回格式

```json
{
    "method": "/api/rest_j/v1/streamis/.+",
    "status": 0,
    "message": "创建成功！",
    "data": {}
}
```

**约定**：

 - method：返回请求的Restful API URL，主要是websocket模式需要使用。
 - status：返回状态信息，其中：-1表示没有登录，0表示成功，1表示错误，2表示验证失败，3表示没该接口的访问权限。
 - data：返回具体的数据。
 - message：返回请求的提示信息。如果status非0时，message返回的是错误信息，其中data有可能存在stack字段，返回具体的堆栈信息。 

另：根据status的不同，HTTP请求的状态码也不一样，一般情况下：

 - 当status为0时，HTTP的状态码为200
 - 当status为-1时，HTTP的状态码为401
 - 当status为1时，HTTP的状态码为400
 - 当status为2时，HTTP的状态码为412
 - 当status为3时，HTTP的状态码为403