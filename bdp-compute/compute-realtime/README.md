# 1、数据实时计算介绍 
~~~markdown
数据实时计算： 
	使用storm、flink、spark streaming开发实时计算系统；
	使用kylin, spark开发多维度分析系统；
compute-realtime目前包含2个模块，compute-realtime-spark, compute-realtime-view

1、compute-realtime-spark: 
    1.1 主要做的事情：
        基于Javee平台展示的Spark实时数据分析平台
        hadoop HA集群搭建部署；
        基于zookeeper的kafka HA集群搭建部署；
        HA: 本地搭建时共5个节点,2个namenode,3个datanode；
        spark core, spark sql, spark streaming基础掌握；
        kafka实时模拟生成数据并使用spark streaming实时处理来自kafka的数据；
        实时处理分析结果保存到mysql, 由highcharts动态刷新；
        highcharts实时展示统计分析结果，以及spark sql算子执行结果；
    1.2 主要模块
        广告点击流量分析
        广告点击趋势分析
        各省份top3热门广告分析
        各区域top3热门商品统计
        页面单跳转化率
        用户访问session分析
        Top10热门品类分析
        Top10用户session分析

2、compute-realtime-view
    2.1 数据可视化显示，定时模拟kafka消息队列的数据
    2.2 主要使用highcharts，html，css, js显示compute-realtime-spark数据处理的统计数据，存在mysql
~~~
