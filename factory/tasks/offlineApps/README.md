# 1、数据离线计算介绍 
~~~markdown
数据离线计算：
	使用Hive、MR、HiveSQL、ETL开发离线计算系统；
compute-mr目前包含3个模块，mr-website-analyse, mr-website-sdk，mr-website-view

1、mr-website-analyse: 
    1.1 主要做的事情：
        hadoop HA集群搭建部署；
        mapreduce基础掌握；
        使用 oozie进行任务调度；
        使用 hive保存数据到hdfs，以及从hdfs导出到 mysql；
        使用hbase结合mapreduce处理业务，如用户行为分析；
        使用flume,nginx模拟收集日志，从java sdk端和js 网站端收集数据等; 
    1.2 主要模块
        用户基本信息分析
        浏览器分析
        地域分析
        浏览深度分析
        搜索引擎分析
        事件分析
        订单分析

2、mr-website-sdk
    2.1 java服务端sdk采集
    2.2 JS前端页面的数据模拟采集  

3、mr-website-view
    3.1 数据可视化显示
    3.2 主要使用highcharts，html，css, js显示mr-website-analyse数据处理的统计数据，存在mysql
~~~
