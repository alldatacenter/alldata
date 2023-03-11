# 1. Vue数据展示项目

  1.1 直接http构造es查询，显示查询结果，提供web端查看
  
  1.2 前端拼接hivesql，查询hive表数据
  
# 2. 爬虫系统

  2.1 爬取数据后，走rabbitmq消息队列通信，数据文件爬取后上传到sftp，然后跑mapreduce任务创建hive表，上传到hdfs
  
  2.2 定时调度爬虫系统
  
# 3. data-spider基本架构图
 https://my-macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20200304/data-spider.png

# 4. 启动脚本

    django搜索服务
    source /usr/local/python-3.6.2/envs/scrapytest/bin/activate
    cd /usr/local/scrapy/search
    python3 manage.py runserver 0.0.0.0:8000
    
    #启动scrapy后台服务
    cd /usr/local/scrapy/spider
    /usr/local/python-3.6.2/envs/scrapytest/bin/scrapyd &
    
    #查看scrapyd
    netstat -tlnp | grep 6800
    
    #部署spider到scrapy
    /usr/local/python-3.6.2/envs/scrapytest/bin/scrapyd-deploy Myploy -p ArticleSpider
    
    #启动爬虫
    curl http://120.79.159.59:6800/schedule.json -d project=ArticleSpider -d spider=zhihu
    curl http://120.79.159.59:6800/schedule.json -d project=ArticleSpider -d spider=lagou
    curl http://120.79.159.59:6800/schedule.json -d project=ArticleSpider -d spider=jobbole



