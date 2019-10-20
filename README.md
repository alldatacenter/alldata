# 1、数据平台介绍 
~~~markdown
数据平台:
	主要包含多个子系统的开发，项目采用Dubbo微服务架构，使用Altas作为服务治理，包括spark，storm，flink，scala，python等技术栈。
数据来源：
	商城：使用SpringBoot，Java，Vue，React，Android开发多端商城，包括网站、App、微信小程序；
	日志：使用Java开发服务端日志、客户端日志收集系统，使用DataX实现数据的导入导出系统；
	爬虫：爬虫平台支持可配置的爬取公网数据的任务开发；
数据存储：
	分布式文件系统使用HDFS，分布式数据库使用HBase，Mongodb、Elasticsearch，内存数据库使用redis；
数据计算：
	使用Hive、MR、HiveSQL、ETL开发离线计算系统；
	使用storm、flink、spark streaming开发实时计算系统；
	使用kylin, spark开发多维度分析系统；
数据开发：
	任务管理系统：负责调度、分配、提交任务到数据平台；
	任务运维系统：查看Task运行情况；
数据应用：
	使用python，ml，spark mllib实现个性化推荐系统；
	使用python，scrapy，django，elasticsearch实现搜索引擎；
	使用scala，flink开发反作弊系统；
	使用FineReport，scala，playframework开发报表分析系统；
	使用ELK技术栈搭建日志搜索平台；
	使用skywalking，Phoenix实现监控平台；
	使用scala、playframework，docker，k8s，shell实现快速打包平台；
~~~

# 2、数据平台展示
2.1 商城图片展示：

商城App:

![1571122567309](http://pz8zk5xal.bkt.clouddn.com/01.png)

![1571122543642](http://pz8zk5xal.bkt.clouddn.com/02.png)

![1571122608000](http://pz8zk5xal.bkt.clouddn.com/03.png)

![1571122626783](http://pz8zk5xal.bkt.clouddn.com/04.png)

商城小程序：

![1571124130480](http://pz8zk5xal.bkt.clouddn.com/21.png)

![1571124166039](http://pz8zk5xal.bkt.clouddn.com/22.png)

![1571124189674](http://pz8zk5xal.bkt.clouddn.com/23.png)

![1571124400916](http://pz8zk5xal.bkt.clouddn.com/24.png)

![1571124444669](http://pz8zk5xal.bkt.clouddn.com/25.png)

商城移动端：

![1571124611795](http://pz8zk5xal.bkt.clouddn.com/31.png)

![1571124636465](http://pz8zk5xal.bkt.clouddn.com/32.png)

商城PC端：

![1571124631001](http://pz8zk5xal.bkt.clouddn.com/51.png)

![1571124631002](http://pz8zk5xal.bkt.clouddn.com/52.png)

![1571124631003](http://pz8zk5xal.bkt.clouddn.com/53.png)

![1571124631004](http://pz8zk5xal.bkt.clouddn.com/54.png)

![1571124631005](http://pz8zk5xal.bkt.clouddn.com/55.png)

![1571124631006](http://pz8zk5xal.bkt.clouddn.com/56.png)

![1571124631007](http://pz8zk5xal.bkt.clouddn.com/57.png)

![1571124631008](http://pz8zk5xal.bkt.clouddn.com/58.png)

商城后台管理：

![1571122855084](http://pz8zk5xal.bkt.clouddn.com/06.png)

![1571122899353](http://pz8zk5xal.bkt.clouddn.com/07.png)

![1571122933051](http://pz8zk5xal.bkt.clouddn.com/08.png)

![1571122950489](http://pz8zk5xal.bkt.clouddn.com/09.png)

![1571122972036](http://pz8zk5xal.bkt.clouddn.com/11.png)

![1571122972036](http://pz8zk5xal.bkt.clouddn.com/12.png)

2.2 其他模块页面展示...xxx

# 3、数据来源

```markdown
商城前台：
	mall-shopping-app: 商城App
	mall-shopping-app-service: 商城App服务
	mall-shopping-wc: 商城小程序
	mall-shopping-mobile: 商城前台
	mall-shopping-pc: 商城pc端
	mall-shopping-pc-service: 商城pc端服务
	mall-shopping-service: 商城前台服务（小程序和前台接入此接口）
商城后台：
	mall-admin-web: 商城后台
	mall-admin-service: 商城后台服务
```
# 4、数据收集
```markdown
log-collect-server: 
	服务端日志收集系统
log-collect-client: 
	支持各app集成的客户端SDK，负责收集app客户端数据；
data-import-export: 
	基于DataX实现数据集成(导入导出)
data-spider:
	爬虫平台支持可配置的爬取公网数据的任务开发；
```
# 5、数据存储
```markdown
分布式文件系统：hdfs
分布式数据库：hbase、mongodb、elasticsearch
分布式内存存储系统：redis
```
# 6、数据计算
```markdown
compute-mr（离线计算）: Hive、MR
compute-realtime（流计算）: storm、flink
multi-dimension-analysis（多维度分析）: kylin, spark
```
# 7、数据开发
```markdown
task-schedular: 任务调度
task-ops: 任务运维
```
# 8、数据产品
```markdown
data-face: 数据可视化
data-insight: 用户画像分析
```
# 9、数据应用
```markdown
system-recommender: 推荐
system-ad: 广告
system-search: 搜索
system-anti-cheating: 反作弊
system-report-analysis: 报表分析
system-elk: ELK日志系统，实现日志搜索平台
system-apm: skywalking监控平台
system-deploy: k8s，scala，playframework，docker打包平台。
system-tasksubmit: 任务提交平台
```
# 10、启动配置教程

 10.1 启动前，打包dubbo-servie项目，进入dubbo-service目录，执行mvn clean package -DskipTests=TRUE打包，然后执行mvn install.

 10.2 启动dubbo-service项目，配置tomcat端口为8091

 10.3 启动商城项目的多个子系统

后台：
     
 10.3.1、前端：启动mall-admin-web项目，进入项目目录，执行npm install，然后执行npm run dev；

![1571122561021](https://my-macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/61.png)

 10.3.2、后端：启动mall-admin-service/mall-admin-search项目，配置tomcat端口为8092，接着启动mall-manage-service项目，tomcat端口配置为8093；
 ![1571122621024](https://my-macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/62.png)
 ![1571122621025](https://my-macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/63.png)
  前台：
     
 10.3.3、小程序和移动端
  	 
 10.3.3.1、前端：商城小程序，启动mall-shopping-wc项目，安装微信开发者工具，配置开发者key和secret，使用微信开发者工具导入即可，然后点击编译，可以手机预览使用。
 ![1571122621026](https://my-macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/66.png)
 10.3.3.2、前端：商城移动端，启动mall-shopping-mobile，进入项目目录，执行mpm install和mpm run dev；

10.3.3.3、后端：小程序和移动端用的是同一个后台服务，启动mall-shopping-service项目，进入项目目录，配置tomcat端口8094
  	 ![1571122621027](https://my-macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/64.png)
 10.3.4、商城PC端

10.3.4.1、前端：启动mall-shopping-pc项目，进入项目目录，执行mpm install和mpm run dev；

10.3.4.2、后端：启动mall-shopping-pc-service项目，配置tomcat端口为8095；![1571122621028](https://my-macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/65.png)

10.3.5 其他xxx待配置开发。