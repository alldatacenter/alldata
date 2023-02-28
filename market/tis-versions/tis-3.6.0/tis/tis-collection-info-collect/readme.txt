改工程的作用是，每隔一定时间从centernode中将终搜所有应用节点中的一些重要的
状态信息搜集到线上的数据库中，提供给终搜系统中生成应用运行的趋势报表

入口类：
com.taobao.terminator.collect.servlet.FullReportCreateServlet


容器启动：
com.taobao.terminator.ClusterStateCollectManager

生成全量报告邮件
ClusterStateCollectManager.exportReport()

3分鐘一次收集集群状态：
TSearcherClusterInfoCollect.start
具体从solr cloud中拉取数据的代码在：CoreStatisticsReport

+createNewSnapshot()--->vaildateUpdateCount() 方法中会校验cluster中更新状态是否正常



CoreStatisticsReport: getCoreStatus() 方法有向solrcollection发送状态请求