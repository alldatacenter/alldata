# 1. Vue数据展示项目

  1.1 直接http构造es查询，显示查询结果，提供web端查看
  
  1.2 前端拼接hivesql，查询hive表数据
  
# 2. 爬虫系统

  2.1 爬取数据后，走rabbitmq消息队列通信，数据文件爬取后上传到sftp，然后跑mapreduce任务创建hive表，上传到hdfs
  
  2.2 定时调度爬虫系统
  
# 3. data-spider基本架构图
 https://my-macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20200304/data-spider.png

