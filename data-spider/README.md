# 1. Vue数据展示项目
  1.1 直接http构造es查询，显示查询结果，提供web端查看
  1.2 前端拼接hivesq1，查询hive表数据
  
# 2. 爬虫系统
  2.1 爬取数据后，走rabbitmq消息队列通信，据爬取后上传到sftp，然后跑mapreduce任务写入hive
  2.2 定时调度爬虫系统
data-spider基本架构图
 ![1571122621028](https://my-macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20200304/data-spider.png)

