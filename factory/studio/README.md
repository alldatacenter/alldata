<h1 style="text-align: center">AllData 一站式细分领域数字化解决方案</h1>

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

- `common` 为系统的公共模块，各种工具类，公共配置存在该模块

- `system` 为系统核心模块也是项目入口模块，也是最终需要打包部署的模块

- `logging` 为系统的日志模块，其他模块如果需要记录日志需要引入该模块

- `tools` 为第三方工具模块，包含：图床、邮件、云存储、本地存储、支付宝

- `generator` 为系统的代码生成模块，代码生成的模板在 system 模块中

### 详细结构

```
- common 公共模块
    - annotation 为系统自定义注解
    - aspect 自定义注解的切面
    - base 提供了Entity、DTO基类和mapstruct的通用mapper
    - config 自定义权限实现、redis配置、swagger配置、Rsa配置等
    - exception 项目统一异常的处理
    - utils 系统通用工具类
- system 系统核心模块（系统启动入口）
	- config 配置跨域与静态资源，与数据权限
	    - thread 线程池相关
	- modules 系统相关模块(登录授权、系统监控、定时任务、运维管理等)
- logging 系统日志模块
- tools 系统第三方工具模块
- generator 系统代码生成模块
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
#### 1、`studio`数据库初始化
> 
> 1.1 source install/16gmaster/studio/studio_alldatadc.sql
> 
> 1.2 source install/16gmaster/studio/studio_dts.sql
>
> 1.3 source install/16gmaster/studio/studio_data_cloud.sql
>
> 1.4 source install/16gmaster/studio/studio_cloud_quartz.sql
> 
> 1.5 source install/16gmaster/studio/studio_foodmart2.sql
> 
> 1.6 source install/16gmaster/studio/studio_robot.sql
> 
#### 2、修改 **config** 配置中心

> **config** 文件夹下的配置文件，修改 **redis**，**mysql** 和 **rabbitmq** 的配置信息
> 
#### 3、项目根目录下执行 **mvn install**
> 
> 获取安装包build/studio-release-0.3.2.tar.gz
> 
> 上传服务器解压
> 
#### 4、部署微服务: 进入不同的目录启动相关服务
> 
> 4.1 必须启动、并且顺序启动
> 
> eureka->config->gateway
> 
> 4.2 按需启动`cd install/16gmaster`
> 
> 譬如启动元数据管理
> 
> sh `install/16gmaster/data-metadata-service.sh`
> 
> tail -100f `install/16gmaster/data-metadata-service.log`
> 
> 4.2 按需启动`cd install/16gdata`
> 
> 按需启动相关服务
> 
> 4.3 按需启动`cd install/16gslave`
>
> 按需启动相关服务
> 
> 

#### 5、部署`studio`:
>
> 5.1 启动`sh install/16gmaster/system.sh`
> 
> 5.2 部署`studio`前端
> 
> source /etc/profile
>
> cd $(dirname $0)
> 
> source /root/.bashrc && nvm use v10.15.3
> 
> nohup npm run dev &
> 
> 5.3 访问`studio`页面
> 
> curl http://localhost:8013
>
> 用户名：admin 密码：123456
