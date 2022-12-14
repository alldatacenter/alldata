<h1 style="text-align: center">AllData 一站式大数据平台</h1>

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



### 部署方式

> 数据库版本为 **mysql5.7** 及以上版本
> 
#### 1、`eladmin`数据库初始化
> 
> 1.1 source install/eladmin/eladmin_alldatadc.sql
> 
> 1.2 source install/eladmin/eladmin_dts.sql
>
> 1.3 source install/datax/eladmin_data_cloud.sql
>
> 1.4 source install/datax/eladmin_cloud_quartz.sql
> 
> 1.5 source install/datax/eladmin_foodmart2.sql
> 
> 1.6 source install/datax/eladmin_robot.sql
> 
#### 2、修改 **datax-config** 配置中心

> **config** 文件夹下的配置文件，修改 **redis**，**mysql** 和 **rabbitmq** 的配置信息
> 
#### 3、安装aspose-words

> cd install/datax
>
> mvn install:install-file -DgroupId=com.aspose -DartifactId=aspose-words -Dversion=20.3 -Dpackaging=jar -Dfile=aspose-words-20.3.jar
>
#### 4、项目根目录下执行 **mvn install**
> 
> 获取安装包build/eladmin-release-2.6.tar.gz
> 
> 上传服务器解压
> 
#### 5、部署微服务：install/alldata_dev/alldata_start.md
> 
| 16gmaster                      | port | ip             |
|--------------------------------| ---- | -------------- |
| eladmin-system                 | 8613 | 16gmaster  |
| datax-config                   | 8611 | 16gmaster  |
| data-market-service      | 8822 | 16gmaster  |
| datax-service-data-integration | 8824 | 16gmaster  |
| data-metadata-service    | 8820 | 16gmaster  |

| 16gslave                      | port | ip             |
|-------------------------------| ---- | -------------- |
| datax-eureka                  | 8610 | 16gslave    |
| datax-gateway                 | 8612 | 16gslave    |
| datax-service-workflow        | 8814 | 16gslave    |
| data-metadata-service-console    | 8821 | 16gslave    |
| datax-service-data-mapping    | 8823 | 16gslave    |
| data-masterdata-service | 8828 | 16gslave    |
| data-quality-service    | 8826 | 16gslave    |

| 16gdata               | port | ip             |
|-----------------------| ---- | -------------- |
| data-standard-service | 8825 | 16gdata |
| data-visual-service   | 8827 | 16gdata |
| email-service         | 8812 | 16gdata |
| file-service          | 8811 | 16gdata |
| quartz-service        | 8813 | 16gdata |
| system-service        | 8810 | 16gdata |
| datax-tool-monitor    | 8711 | 16gdata |


#### 6、启动顺序

> 1、启动eureka
>
> 2、启动config
>
> 3、启动gateway
>
> 4、启动masterdata
>
> 5、启动metadata
>
> 6、启动其他Jar
> 
> 用户名：admin 密码：123456
> 
> 
#### 7、部署`Eladmin`:
> 
> 7.1 启动`Eladmin`后端
> 
> nohup java -jar -Xms128m -Xmx2048m -XX:PermSize=128M -XX:MaxPermSize=256M -XX:+UseG1GC -XX:MaxGCPauseMillis=20 
> 
> -XX:InitiatingHeapOccupancyPercent=35 -XX:+ExplicitGCInvokesConcurrent -XX:MaxInlineLevel=15 /mnt/poc/eladmin/deploy/eladmin-system-2.6.jar  &
> 
> 7.2 部署`Eladmin`前端
> 
> source /etc/profile
>
> cd $(dirname $0)
> 
> source /root/.bashrc && nvm use v10.15.3
> 
> nohup npm run dev &
> 
> 7.3 访问`Eladmin`页面
> 
> curl http://localhost:8013
