<h1 style="text-align: center">AllData 一站式大数据平台</h1>

### 主要特性
- 使用最新技术栈，社区资源丰富。
- 高效率开发，代码生成器可一键生成前后端代码
- 支持数据字典，可方便地对一些状态进行管理
- 支持接口限流，避免恶意请求导致服务层压力过大
- 支持接口级别的功能权限与数据权限，可自定义操作
- 自定义权限注解与匿名接口注解，可快速对接口拦截与放行
- 对一些常用地前端组件封装：表格数据请求、数据字典等
- 前后端统一异常拦截处理，统一输出异常，避免繁琐的判断
- 支持在线用户管理与服务器性能监控，支持限制单用户登录
- 支持运维管理，可方便地对远程服务器的应用进行部署与管理

###  系统功能
- 用户管理：提供用户的相关配置，新增用户后，默认密码为123456
- 角色管理：对权限与菜单进行分配，可根据部门设置角色的数据权限
- 菜单管理：已实现菜单动态路由，后端可配置化，支持多级菜单
- 部门管理：可配置系统组织架构，树形表格展示
- 岗位管理：配置各个部门的职位
- 字典管理：可维护常用一些固定的数据，如：状态，性别等
- 系统日志：记录用户操作日志与异常日志，方便开发人员定位排错
- SQL监控：采用druid 监控数据库访问性能，默认用户名admin，密码123456
- 定时任务：整合Quartz做定时任务，加入任务日志，任务运行情况一目了然
- 代码生成：高灵活度生成前后端代码，减少大量重复的工作任务
- 邮件工具：配合富文本，发送html格式的邮件
- 七牛云存储：可同步七牛云存储的数据到系统，无需登录七牛云直接操作云数据
- 支付宝支付：整合了支付宝支付并且提供了测试账号，可自行测试
- 服务监控：监控服务器的负载情况
- 运维管理：一键部署你的应用

### 项目结构
项目采用按功能分模块的开发方式，结构如下

- `eladmin-common` 为系统的公共模块，各种工具类，公共配置存在该模块

- `eladmin-system` 为系统核心模块也是项目入口模块，也是最终需要打包部署的模块

- `eladmin-logging` 为系统的日志模块，其他模块如果需要记录日志需要引入该模块

- `eladmin-tools` 为第三方工具模块，包含：图床、邮件、云存储、本地存储、支付宝

- `eladmin-generator` 为系统的代码生成模块，代码生成的模板在 system 模块中

### 详细结构

```
- eladmin-common 公共模块
    - annotation 为系统自定义注解
    - aspect 自定义注解的切面
    - base 提供了Entity、DTO基类和mapstruct的通用mapper
    - config 自定义权限实现、redis配置、swagger配置、Rsa配置等
    - exception 项目统一异常的处理
    - utils 系统通用工具类
- eladmin-system 系统核心模块（系统启动入口）
	- config 配置跨域与静态资源，与数据权限
	    - thread 线程池相关
	- modules 系统相关模块(登录授权、系统监控、定时任务、运维管理等)
- eladmin-logging 系统日志模块
- eladmin-tools 系统第三方工具模块
- eladmin-generator 系统代码生成模块
```

### 主要技术栈

### 后端技术栈

- 开发框架：Spring Boot 2.3
- 微服务框架：Spring Cloud Hoxton.SR9
- 安全框架：Spring Security + Spring OAuth 2.0
- 任务调度：Quartz
- 持久层框架：MyBatis Plus
- 数据库连接池：Hikaricp
- 服务注册与发现: Spring Cloud Config
- 客户端负载均衡：Ribbon
- 熔断组件：Hystrix
- 网关组件：Spring Cloud Gateway
- 消息队列：Rabbitmq
- 缓存：Redis
- 日志管理：Logback
- 运行容器：Undertow
- 工作流: Flowable 6.5.0

### 前端技术栈

- JS框架：Vue、nodejs
- CSS框架：sass
- 组件库：ElementUI
- 打包构建工具：Webpack

### 功能一览

- 平台基础设置
    - 系统管理
        - 岗位管理：配置系统用户所属担任职务。
        - 部门管理：配置系统组织机构，树结构展现支持数据权限。
        - 菜单管理：配置系统菜单，操作权限，按钮权限标识等。
        - 角色管理：角色菜单权限分配、设置角色按机构进行数据范围权限划分。
        - 用户管理：用户是系统操作者，该功能主要完成系统用户配置。
        - 参数管理：对系统动态配置常用参数。
        - 字典管理：对系统中经常使用的一些较为固定的数据进行维护。
    - 系统监控
        - 登录日志：系统登录日志记录查询。
        - 操作日志：系统正常操作日志记录和查询；系统异常信息日志记录和查询。
    - 任务调度
        - 任务管理：在线（添加、修改、删除)任务调度。
        - 日志管理：任务调度执行结果日志。
- 元数据管理
    - 数据源：数据源连接信息管理，可生成数据库文档。
    - 元数据：数据库表的元数据信息管理。
    - 数据授权：设置元数据信息权限划分。
    - 变更记录：元数据信息变更记录信息管理。
    - 数据检索：数据源、数据表、元数据等信息查询。
    - 数据地图：元数据的隶属数据表、数据库的图形展示。
    - SQL工作台：在线执行查询sql。
- 数据标准管理
    - 标准字典：国标数据维护。
    - 对照表：本地数据中需要对照标准的数据维护。
    - 字典对照：本地数据与国标数据的对照关系。
    - 对照统计：本地数据与国标数据的对照结果统计分析。
- 数据质量管理
    - 规则配置：数据质量规则配置。
    - 问题统计：数据质量规则统计。
    - 质量报告：数据质量结果统计分析。
    - 定时任务：数据质量定时任务。
    - 任务日志：数据质量定时任务日志。
- 主数据管理
    - 数据模型：主数据数据模型维护。
    - 数据管理：主数据数据管理。
- 数据集市管理
    - 数据服务：动态开发api数据服务，可生成数据服务文档。
    - 数据脱敏：api数据服务返回结果动态脱敏。
    - 接口日志：api数据服务调用日志。
    - 服务集成：三方数据服务集成管理。
    - 服务日志：三方数据服务集成调用日志。
- 可视化管理
    - 数据集：基于sql的查询结果维护。
    - 图表配置：动态echarts图表配置，支持多维表格、折线、柱状、饼图、雷达、散点等多种图表。
    - 看板配置：拖拽式添加图表组件，调整位置、大小。
    - 酷屏配置：拖拽式添加图表组件，调整背景图、颜色、位置、大小。
- 流程管理
    - 流程定义：流程定义管理。
    - 流程实例
        - 运行中的流程：运行中的流程实例管理。
        - 我发起的流程：我发起的流程实例管理。
        - 我参与的流程：我参与的流程实例管理。
    - 流程任务
        - 待办任务：待办任务管理。
        - 已办任务：已办任务管理。
    - 业务配置：配置业务系统与流程的相关属性。


### 部署方式

> 数据库版本为 **mysql5.7** 及以上版本
> 
> 依次创建以下数据库：
> **data_cloud**
> **data_cloud_flowable**
> **data_cloud_quartz**
> **foodmart2**
> **robot**
> 字符集：**utf8mb4**
> 排序规则：**utf8mb4_general_ci**
> 
> 数据库创建完毕，导入 db 文件夹下的 **sql** 脚本 即可完成数据库初始化
> 
> 修改 **datax-config** 配置中心 **config** 文件夹下的配置文件，把 **redis**，**mysql** 和 **rabbitmq** 的配置信息改成自己的
> 
> 把系统导入 **idea** 中，等待 **maven** 依赖下载完毕，在项目根目录下执行 **mvn install**，**install**  完毕后可以获取到各个模块的 **jar** 包，上传到服务器的同一个文件夹，**依次执行**即可，部署脚本在 **sh** 目录下，内容如下（服务较多，一台服务器内存可能不够用，可考虑分开部署，自行修改脚本）：

### 安装aspose-words
> cd lib
>
> mvn install:install-file -DgroupId=com.aspose -DartifactId=aspose-words -Dversion=20.3 -Dpackaging=jar -Dfile=aspose-words-20.3.jar


```sh
#!/bin/sh
# eureka 注册中心
dataxEureka=./datax-eureka.jar
# config 配置中心
dataxConfig=./datax-config.jar
# gateway 网关
dataxGateway=./datax-gateway.jar
# oauth2 认证服务
dataxAuth=./datax-auth.jar
# 系统服务
systemService=./system-service.jar
# 任务调度服务
quartzService=./quartz-service.jar
# 工作流服务
workflowService=./workflow-service.jar
# 元数据服务
metadataService=./data-metadata-service.jar
# 元数据 sql 控制台服务
metadataConsoleService=./data-metadata-service-console.jar
# 数据标准服务
standardService=./data-standard-service.jar
# 数据质量服务
qualityService=./data-quality-service.jar
# 数据集市数据集服务
marketService=./data-market-service.jar
# 数据集市接口映射服务
marketServiceMapping=./data-market-service-mapping.jar
# 数据集市数据集成服务
marketServiceIntegration=./data-market-service-integration.jar
# 主数据服务
masterdataService=./data-masterdata-service.jar
# 可视化服务
visualService=./data-visual-service.jar
case "$1" in
start)
    echo "--------dataxEureka 开始启动--------------"
    nohup java -jar $dataxEureka >/dev/null 2>&1 &
    dataxEurekaPid=`ps -ef|grep $dataxEureka |grep -v grep|awk '{print $2}'`
    until [ -n "$dataxEurekaPid" ]
      do
       dataxEurekaPid=`ps -ef|grep $dataxEureka |grep -v grep|awk '{print $2}'`
      done
    echo "dataxEurekaPid is $dataxEurekaPid"
    echo "--------dataxEureka 启动成功--------------"
    stop 10

    echo "--------dataxConfig 开始启动--------------"
    nohup java -jar $dataxConfig >/dev/null 2>&1 &
    dataxConfigPid=`ps -ef|grep $dataxConfig |grep -v grep|awk '{print $2}'`
    until [ -n "$dataxConfigPid" ]
      do
       dataxConfigPid=`ps -ef|grep $dataxConfig |grep -v grep|awk '{print $2}'`
      done
    echo "dataxConfigPid is $dataxConfigPid"
    echo "--------dataxConfig 启动成功--------------"
    stop 10

    echo "--------dataxGateway 开始启动--------------"
    nohup java -jar $dataxGateway >/dev/null 2>&1 &
    dataxGatewayPid=`ps -ef|grep $dataxGateway |grep -v grep|awk '{print $2}'`
    until [ -n "$dataxConfigPid" ]
      do
       dataxGatewayPid=`ps -ef|grep $dataxGateway |grep -v grep|awk '{print $2}'`
      done
    echo "dataxGatewayPid is $dataxGatewayPid"
    echo "--------dataxGateway 启动成功--------------"
    stop 10

    echo "--------dataxAuth 开始启动--------------"
    nohup java -jar $dataxAuth >/dev/null 2>&1 &
    dataxAuthPid=`ps -ef|grep $dataxAuth |grep -v grep|awk '{print $2}'`
    until [ -n "$dataxAuthPid" ]
      do
       dataxAuthPid=`ps -ef|grep $dataxAuth |grep -v grep|awk '{print $2}'`
      done
    echo "dataxAuthPid is $dataxAuthPid"
    echo "--------dataxAuth 启动成功--------------"

    echo "--------systemService 开始启动--------------"
    nohup java -jar $systemService >/dev/null 2>&1 &
    systemServicePid=`ps -ef|grep $systemService |grep -v grep|awk '{print $2}'`
    until [ -n "$systemServicePid" ]
      do
       systemServicePid=`ps -ef|grep $systemService |grep -v grep|awk '{print $2}'`
      done
    echo "systemServicePid is $systemServicePid"
    echo "--------systemService 启动成功--------------"

    echo "--------quartzService 开始启动--------------"
    nohup java -jar $quartzService >/dev/null 2>&1 &
    quartzServicePid=`ps -ef|grep $quartzService |grep -v grep|awk '{print $2}'`
    until [ -n "$quartzServicePid" ]
      do
       quartzServicePid=`ps -ef|grep $quartzService |grep -v grep|awk '{print $2}'`
      done
    echo "quartzServicePid is $quartzServicePid"
    echo "--------quartzService 启动成功--------------"

    echo "--------workflowService 开始启动--------------"
    nohup java -jar $workflowService >/dev/null 2>&1 &
    workflowServicePid=`ps -ef|grep $workflowService |grep -v grep|awk '{print $2}'`
    until [ -n "$workflowServicePid" ]
      do
       workflowServicePid=`ps -ef|grep $workflowService |grep -v grep|awk '{print $2}'`
      done
    echo "workflowServicePid is $workflowServicePid"
    echo "--------workflowService 启动成功--------------"

    echo "--------metadataService 开始启动--------------"
    nohup java -jar $metadataService >/dev/null 2>&1 &
    metadataServicePid=`ps -ef|grep $metadataService |grep -v grep|awk '{print $2}'`
    until [ -n "$metadataServicePid" ]
      do
       metadataServicePid=`ps -ef|grep $metadataService |grep -v grep|awk '{print $2}'`
      done
    echo "metadataServicePid is $metadataServicePid"
    echo "--------metadataService 启动成功--------------"

    echo "--------metadataConsoleService 开始启动--------------"
    nohup java -jar $metadataConsoleService >/dev/null 2>&1 &
    metadataConsoleServicePid=`ps -ef|grep $metadataConsoleService |grep -v grep|awk '{print $2}'`
    until [ -n "$metadataConsoleServicePid" ]
      do
       metadataConsoleServicePid=`ps -ef|grep $metadataConsoleService |grep -v grep|awk '{print $2}'`
      done
    echo "metadataConsoleServicePid is $metadataConsoleServicePid"
    echo "--------metadataConsoleService 启动成功--------------"

    echo "--------standardService 开始启动--------------"
    nohup java -jar $standardService >/dev/null 2>&1 &
    standardServicePid=`ps -ef|grep $standardService |grep -v grep|awk '{print $2}'`
    until [ -n "$standardServicePid" ]
      do
       standardServicePid=`ps -ef|grep $standardService |grep -v grep|awk '{print $2}'`
      done
    echo "standardServicePid is $standardServicePid"
    echo "--------standardService 启动成功--------------"

    echo "--------qualityService 开始启动--------------"
    nohup java -jar $qualityService >/dev/null 2>&1 &
    qualityServicePid=`ps -ef|grep $qualityService |grep -v grep|awk '{print $2}'`
    until [ -n "$qualityServicePid" ]
      do
       qualityServicePid=`ps -ef|grep $qualityService |grep -v grep|awk '{print $2}'`
      done
    echo "qualityServicePid is $qualityServicePid"
    echo "--------qualityService 启动成功--------------"

    echo "--------marketService 开始启动--------------"
    nohup java -jar $marketService >/dev/null 2>&1 &
    marketServicePid=`ps -ef|grep $marketService |grep -v grep|awk '{print $2}'`
    until [ -n "$marketServicePid" ]
      do
       marketServicePid=`ps -ef|grep $marketService |grep -v grep|awk '{print $2}'`
      done
    echo "marketServicePid is $marketServicePid"
    echo "--------marketService 启动成功--------------"
	sleep 10

    echo "--------marketServiceMapping 开始启动--------------"
    nohup java -jar $marketServiceMapping >/dev/null 2>&1 &
    marketServiceMappingPid=`ps -ef|grep $marketServiceMapping |grep -v grep|awk '{print $2}'`
    until [ -n "$marketServiceMappingPid" ]
      do
       marketServiceMappingPid=`ps -ef|grep $marketServiceMapping |grep -v grep|awk '{print $2}'`
      done
    echo "marketServiceMappingPid is $marketServiceMappingPid"
    echo "--------marketServiceMapping 启动成功--------------"

    echo "--------marketServiceIntegration 开始启动--------------"
    nohup java -jar $marketServiceIntegration >/dev/null 2>&1 &
    marketServiceIntegrationPid=`ps -ef|grep $marketServiceIntegration |grep -v grep|awk '{print $2}'`
    until [ -n "$marketServiceIntegrationPid" ]
      do
       marketServiceIntegrationPid=`ps -ef|grep $marketServiceIntegration |grep -v grep|awk '{print $2}'`
      done
    echo "marketServiceIntegrationPid is $marketServiceIntegrationPid"
    echo "--------marketServiceIntegration 启动成功--------------"

    echo "--------masterdataService 开始启动--------------"
    nohup java -jar $masterdataService >/dev/null 2>&1 &
    masterdataServicePid=`ps -ef|grep $masterdataService |grep -v grep|awk '{print $2}'`
    until [ -n "$masterdataServicePid" ]
      do
       masterdataServicePid=`ps -ef|grep $masterdataService |grep -v grep|awk '{print $2}'`
      done
    echo "masterdataServicePid is $masterdataServicePid"
    echo "--------masterdataService 启动成功--------------"

    echo "--------visualService 开始启动--------------"
    nohup java -jar $visualService >/dev/null 2>&1 &
    visualServicePid=`ps -ef|grep $visualService |grep -v grep|awk '{print $2}'`
    until [ -n "$visualServicePid" ]
      do
       visualServicePid=`ps -ef|grep $visualService |grep -v grep|awk '{print $2}'`
      done
    echo "visualServicePid is $visualServicePid"
    echo "--------visualService 启动成功--------------"
;;
stop)
    P_ID=`ps -ef|grep $dataxEureka |grep -v grep|awk '{print $2}'`
    if [ "$P_ID" == "" ]; then
      echo "===dataxEureka process not exists or stop success"
    else
      kill -9 $P_ID
      echo "dataxEureka killed success"
    fi

    P_ID=`ps -ef|grep $dataxConfig |grep -v grep|awk '{print $2}'`
    if [ "$P_ID" == "" ]; then
      echo "===dataxConfig process not exists or stop success"
    else
      kill -9 $P_ID
      echo "dataxConfig killed success"
    fi

    P_ID=`ps -ef|grep $dataxGateway |grep -v grep|awk '{print $2}'`
    if [ "$P_ID" == "" ]; then
      echo "===dataxGateway process not exists or stop success"
    else
      kill -9 $P_ID
      echo "dataxGateway killed success"
    fi

    P_ID=`ps -ef|grep $dataxAuth |grep -v grep|awk '{print $2}'`
    if [ "$P_ID" == "" ]; then
      echo "===dataxAuth process not exists or stop success"
    else
      kill -9 $P_ID
      echo "dataxAuth killed success"
    fi

    P_ID=`ps -ef|grep $systemService |grep -v grep|awk '{print $2}'`
    if [ "$P_ID" == "" ]; then
      echo "===systemService process not exists or stop success"
    else
      kill -9 $P_ID
      echo "systemService killed success"
    fi

    P_ID=`ps -ef|grep $quartzService |grep -v grep|awk '{print $2}'`
    if [ "$P_ID" == "" ]; then
      echo "===quartzService process not exists or stop success"
    else
      kill -9 $P_ID
      echo "quartzService killed success"
    fi

    P_ID=`ps -ef|grep $workflowService |grep -v grep|awk '{print $2}'`
    if [ "$P_ID" == "" ]; then
      echo "===workflowService process not exists or stop success"
    else
      kill -9 $P_ID
      echo "workflowService killed success"
    fi

    P_ID=`ps -ef|grep $metadataService |grep -v grep|awk '{print $2}'`
    if [ "$P_ID" == "" ]; then
      echo "===metadataService process not exists or stop success"
    else
      kill -9 $P_ID
      echo "metadataService killed success"
    fi

    P_ID=`ps -ef|grep $metadataConsoleService |grep -v grep|awk '{print $2}'`
    if [ "$P_ID" == "" ]; then
      echo "===metadataConsoleService process not exists or stop success"
    else
      kill -9 $P_ID
      echo "metadataConsoleService killed success"
    fi

    P_ID=`ps -ef|grep $standardService |grep -v grep|awk '{print $2}'`
    if [ "$P_ID" == "" ]; then
      echo "===standardService process not exists or stop success"
    else
      kill -9 $P_ID
      echo "standardService killed success"
    fi

    P_ID=`ps -ef|grep $qualityService |grep -v grep|awk '{print $2}'`
    if [ "$P_ID" == "" ]; then
      echo "===qualityService process not exists or stop success"
    else
      kill -9 $P_ID
      echo "qualityService killed success"
    fi

    P_ID=`ps -ef|grep $marketService |grep -v grep|awk '{print $2}'`
    if [ "$P_ID" == "" ]; then
      echo "===marketService process not exists or stop success"
    else
      kill -9 $P_ID
      echo "marketService killed success"
    fi

    P_ID=`ps -ef|grep $marketServiceMapping |grep -v grep|awk '{print $2}'`
    if [ "$P_ID" == "" ]; then
      echo "===marketServiceMapping process not exists or stop success"
    else
      kill -9 $P_ID
      echo "marketServiceMapping killed success"
    fi

    P_ID=`ps -ef|grep $marketServiceIntegration |grep -v grep|awk '{print $2}'`
    if [ "$P_ID" == "" ]; then
      echo "===marketServiceIntegration process not exists or stop success"
    else
      kill -9 $P_ID
      echo "marketServiceIntegration killed success"
    fi

    P_ID=`ps -ef|grep $masterdataService |grep -v grep|awk '{print $2}'`
    if [ "$P_ID" == "" ]; then
      echo "===masterdataService process not exists or stop success"
    else
      kill -9 $P_ID
      echo "masterdataService killed success"
    fi

    P_ID=`ps -ef|grep $visualService |grep -v grep|awk '{print $2}'`
    if [ "$P_ID" == "" ]; then
      echo "===visualService process not exists or stop success"
    else
      kill -9 $P_ID
      echo "visualService killed success"
    fi
;;
restart)
    $0 stop
    sleep 10
    $0 start
    echo "===restart success==="
;;
esac
exit 0
```

启动：startDatax.sh start

关闭：startDatax.sh stop

重启：startDatax.sh restart

用户名：admin

密码：123456