## examples
Tesla FaaS 简单示例代码

开发和部署此项目请[参见文档](https://yuque.antfin-inc.com/bdsre/faas/rpct16)


### 最简单的代码包括下列文件:
* `__init__.py`   [必须]
* conf.ini  [非必须] ini格式的配置文件，支持mysql数据库/redis/zk/tesla sdk的配置
* conf.json  [非必须] json格式的配置文件，此文件的配置会覆盖ini中的配置
* conf.py  [非必须] python格式的配置文件，此文件的配置会覆盖json中的配置
* urls.py  [非必须] 路由配置，参考示例
* dependency_repos.ini [非必须] 依赖的git工程库，如bigdatak，对内部署时每次都会fetch指定分支最新的commit
```
[bigdatak]
repo_name=bigdata/bigdatak
branch = blink_master
import_depth = 2     # 为1时import的路径是工程根目录如bigdatak.bigdatak.lib，为2时import的路径是二级目录如bigdatak.lib
```
* requirements.txt  [非必须] 依赖信息

    Tesla FaaS 框架已经内置了下列模块，可以直接使用，无需在本项目依赖中重复定义
    ```
    setuptools>=38.5.1, <=39.1.0
    six>=1.11.0

    gevent >= 1.3.7
    web.py == 0.37
    setproctitle == 1.1.10
    # use built-in gunicorn base on 19.9.0 instead
    # gunicorn >= 19.9.0
    enum34 >= 1.1.6
    requests >= 2.9.1
    pbr >= 5.1.1
    requests-unixsocket == 0.1.5
    futures >= 3.2.0
    PyMySQL == 0.9.2
    DBUtils == 1.3
    redis == 3.0.1
    kazoo == 2.6.0

    tesla_sdk >= 5.7.4
    ```
    
* 其他文件  包括用户自己的handler、models等包

用户的项目根目录的父目录会被自动加到PYTHONPATH中，所以:
* 用户代码中可以import从根目录(包括根目录)开始的本项目包, 或使用相对导入
* 用户代码根目录中必须是有效的python包名，包含中划线，斜线等字符


### 示例包括下列功能:
* handlers 模块，处理HTTP接口层
    * hello_test_handler.py  hello word 保活接口
    * mysql_test_handler.py  说明 mysql 数据库的使用方法
    * redis_test_handler.py  说明 redis 的使用方法
    * sdk_test_handler.py  说明 tesla sdk 和 通道服务 的使用方法
    * context_test_handler.py  说明 HTTP 请求上下文的使用方法
    * log_test_handler.py  说明 log 的使用方法
    * base_handler_test.py  说明 BaseHandler / RestHandler 基类的使用方法
    * factory_test_handler.py  说明 handler.factory使用方法
    
* models 模块，数据库处理层
    * mysql_test_model.py 说明了如何使用mysql数据库以及多数据源的使用
    
* services 模块，封装models层，为handlers层提供服务实现
* common 模块，封装通用的类，方法，CONST常量定义
* factory 模块，框架自动处理factory中继承自 `teslafaas.container.webpy.bcc_factory.BCCFactoryBase`的工厂类，提供单例类实例

