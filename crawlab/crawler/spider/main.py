# -*- coding: utf-8 -*-
__author__ = 'bobby'

from scrapy.cmdline import execute

import sys
import os

#pip install -i https://pypi.douban.com/simple/ mysqlclient
sys.path.append(os.path.dirname(os.path.abspath(__file__)))
# execute(["scrapy", "crawl", "jobbole"])
execute(["scrapy", "crawl", "zhihu"])
# execute(["scrapy", "crawl", "lagou"])
# execute(["scrapy", "crawl", "zhihu2"])

# import scrapy
# from scrapy.crawler import CrawlerProcess
# from spiders.jobbole import JobboleSpider
# from spiders.lagou import LagouSpider
# from twisted.internet import reactor, defer
# from scrapy.crawler import CrawlerRunner
# from scrapy.utils.log import configure_logging
#
# configure_logging()
# runner = CrawlerRunner()
#
# @defer.inlineCallbacks
# def crawl():
#     yield runner.crawl(JobboleSpider)
#     yield runner.crawl(LagouSpider)
#     reactor.stop()
#
# crawl()
# reactor.run() # the script will block here until the last crawl call is finished