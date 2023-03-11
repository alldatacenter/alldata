# -*- coding: utf-8 -*-

# Define here the models for your scraped items
#
# See documentation in:
# http://doc.scrapy.org/en/latest/topics/items.html

import datetime
import re

import scrapy
from scrapy.loader import ItemLoader
from scrapy.loader.processors import MapCompose, TakeFirst, Join

from ArticleSpider.utils.common import extract_num
from ArticleSpider.settings import SQL_DATETIME_FORMAT, SQL_DATE_FORMAT
from w3lib.html import remove_tags
from ArticleSpider.models.models import Article
from ArticleSpider.models.models import Lagou
from ArticleSpider.models.models import ZhihuQuestion
from ArticleSpider.models.models import ZhihuAnswer
import redis

from elasticsearch_dsl.connections import connections

es = connections.create_connection(Article._doc_type.using)
redis_cli = redis.StrictRedis()


class ArticlespiderItem(scrapy.Item):
    # define the fields for your item here like:
    # name = scrapy.Field()
    pass


def add_jobbole(value):
    return value + "-bobby"


def date_convert(value):
    try:
        create_date = datetime.datetime.strptime(value, "%Y/%m/%d").date()
    except Exception as e:
        create_date = datetime.datetime.now().date()

    return create_date


def get_nums(value):
    match_re = re.match(".*?(\d+).*", value)
    if match_re:
        nums = int(match_re.group(1))
    else:
        nums = 0

    return nums


def return_value(value):
    return value


def remove_comment_tags(value):
    # 去掉tag中提取的评论
    if "评论" in value:
        return ""
    else:
        return value


def gen_suggests(index, info_tuple):
    # 根据字符串生成搜索建议数组
    used_words = set()
    suggests = []
    for text, weight in info_tuple:
        if text:
            # 调用es的analyze接口分析字符串
            words = es.indices.analyze(index=index, analyzer="ik_max_word", params={'filter': ["lowercase"]}, body=text)
            anylyzed_words = set([r["token"] for r in words["tokens"] if len(r["token"]) > 1])
            new_words = anylyzed_words - used_words
        else:
            new_words = set()

        if new_words:
            suggests.append({"input": list(new_words), "weight": weight})

    return suggests


class ArticleItemLoader(ItemLoader):
    # 自定义itemloader
    default_output_processor = TakeFirst()


class JobBoleArticleItem(scrapy.Item):
    title = scrapy.Field()
    create_date = scrapy.Field(
        input_processor=MapCompose(date_convert),
    )
    url = scrapy.Field()
    url_object_id = scrapy.Field()
    front_image_url = scrapy.Field(
        output_processor=MapCompose(return_value)
    )
    front_image_path = scrapy.Field()
    praise_nums = scrapy.Field(
        input_processor=MapCompose(get_nums)
    )
    comment_nums = scrapy.Field(
        input_processor=MapCompose(get_nums)
    )
    fav_nums = scrapy.Field(
        input_processor=MapCompose(get_nums)
    )
    tags = scrapy.Field(
        input_processor=MapCompose(remove_comment_tags),
        output_processor=Join(",")
    )
    content = scrapy.Field()

    def get_insert_sql(self):
        insert_sql = """
            insert into jobbole_article(title, url, create_date, fav_nums, front_image_url, front_image_path,
            praise_nums, comment_nums, tags, content)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s) ON DUPLICATE KEY UPDATE content=VALUES(fav_nums)
        """

        fron_image_url = ""
        # content = remove_tags(self["content"])

        if self["front_image_url"]:
            fron_image_url = self["front_image_url"][0]
        front_image_path = ""
        params = (self["title"], self["url"], self["create_date"], self["fav_nums"],
                  fron_image_url, front_image_path, self["praise_nums"], self["comment_nums"],
                  self["tags"], self["content"])
        return insert_sql, params

    def save_to_es(self):
        article = Article()
        article.title = self['title']
        article.create_date = self["create_date"]
        article.content = remove_tags(self["content"])
        article.front_image_url = self["front_image_url"]
        if "front_image_path" in self:
            article.front_image_path = self["front_image_path"]
        article.praise_nums = self["praise_nums"]
        article.fav_nums = self["fav_nums"]
        article.comment_nums = self["comment_nums"]
        article.url = self["url"]
        article.tags = self["tags"]
        article.meta.id = self["url_object_id"]

        article.title_suggest = gen_suggests(Article._doc_type.index, ((article.title, 7), (article.tags, 8)))

        article.save()

        redis_cli.incr("jobbole_count")

        return


class ZhihuQuestionItem(scrapy.Item):
    # 知乎的问题 item
    zhihu_id = scrapy.Field()
    topics = scrapy.Field()
    url = scrapy.Field()
    title = scrapy.Field()
    content = scrapy.Field()
    answer_num = scrapy.Field()
    comments_num = scrapy.Field()
    watch_user_num = scrapy.Field()
    click_num = scrapy.Field()
    crawl_time = scrapy.Field()

    def get_insert_sql(self):
        # 插入知乎question表的sql语句
        insert_sql = """
            insert into zhihu_question(zhihu_id, topics, url, title, content, answer_num, comments_num,
              watch_user_num, click_num, crawl_time
              )
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            ON DUPLICATE KEY UPDATE content=VALUES(content), answer_num=VALUES(answer_num), comments_num=VALUES(comments_num),
              watch_user_num=VALUES(watch_user_num), click_num=VALUES(click_num)
        """
        zhihu_id = self["zhihu_id"][0]
        topics = ",".join(self["topics"])
        url = self["url"][0]
        title = "".join(self["title"])
        content = "".join(self["content"])
        answer_num = extract_num("".join(self["answer_num"]))
        comments_num = extract_num("".join(self["comments_num"]))

        if len(self["watch_user_num"]) == 2:
            self["watch_user_num"][0] = str.replace(self["watch_user_num"][0], ",", "")
            watch_user_num = int(self["watch_user_num"][0])
            self["watch_user_num"][1] = str.replace(self["watch_user_num"][1], ",", "")
            click_num = int(self["watch_user_num"][1])
        else:
            watch_user_num = int(self["watch_user_num"][0])
            click_num = 0

        crawl_time = datetime.datetime.now().strftime(SQL_DATETIME_FORMAT)

        params = (zhihu_id, topics, url, title, content, answer_num, comments_num,
                  watch_user_num, click_num, crawl_time)

        return insert_sql, params

    def save_to_es(self):
        zhihu_question = ZhihuQuestion()
        zhihu_question.title_suggest = gen_suggests(ZhihuQuestion._doc_type.index,
                                                    ((zhihu_question.title, 10), (zhihu_question.topics, 7)))
        zhihu_question.title = self['title']
        zhihu_question.content = self["content"]
        zhihu_question.url = self["url"]
        zhihu_question.question_id = self["zhihu_id"][0]
        zhihu_question.answer_num = extract_num("".join(self["answer_num"]))
        zhihu_question.comments_num = extract_num("".join(self["comments_num"]))

        if len(self["watch_user_num"]) == 2:
            self["watch_user_num"][0] = str.replace(self["watch_user_num"][0], ",", "")
            zhihu_question.watch_user_num = int(self["watch_user_num"][0])
            self["watch_user_num"][1] = str.replace(self["watch_user_num"][1], ",", "")
            zhihu_question.click_num = int(self["watch_user_num"][1])
        else:
            zhihu_question.watch_user_num = int(self["watch_user_num"][0])
            zhihu_question.click_num = 0

        zhihu_question.topics = self["topics"]
        zhihu_question.meta.id = self["zhihu_id"][0]

        zhihu_question.save()
        redis_cli.incr("zhihu_question_count")
        return


class ZhihuAnswerItem(scrapy.Item):
    # 知乎的问题回答item
    zhihu_id = scrapy.Field()
    url = scrapy.Field()
    question_id = scrapy.Field()
    author_id = scrapy.Field()
    content = scrapy.Field()
    praise_num = scrapy.Field()
    comments_num = scrapy.Field()
    create_time = scrapy.Field()
    update_time = scrapy.Field()
    crawl_time = scrapy.Field()

    def get_insert_sql(self):
        # 插入知乎question表的sql语句
        insert_sql = """
            insert into zhihu_answer(zhihu_id, url, question_id, author_id, content, praise_num, comments_num,
              create_time, update_time, crawl_time
              ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
              ON DUPLICATE KEY UPDATE content=VALUES(content), comments_num=VALUES(comments_num), praise_num=VALUES(praise_num),
              update_time=VALUES(update_time)
        """

        create_time = datetime.datetime.fromtimestamp(self["create_time"]).strftime(SQL_DATETIME_FORMAT)
        update_time = datetime.datetime.fromtimestamp(self["update_time"]).strftime(SQL_DATETIME_FORMAT)
        params = (
            self["zhihu_id"], self["url"], self["question_id"],
            self["author_id"], self["content"], self["praise_num"],
            self["comments_num"], create_time, update_time,
            self["crawl_time"].strftime(SQL_DATETIME_FORMAT),
        )


        return insert_sql, params

    def save_to_es(self):
        zhihu_answer = ZhihuAnswer()
        zhihu_answer.title_suggest = gen_suggests(ZhihuAnswer._doc_type.index,
                                                  ((zhihu_answer.create_time, 10), (zhihu_answer.content, 7)))
        zhihu_answer.meta.id = self["zhihu_id"]

        zhihu_answer.zhihu_id = self['zhihu_id']
        zhihu_answer.url = remove_tags(self["url"])
        zhihu_answer.question_id = self["question_id"]
        zhihu_answer.author_id = self["author_id"]
        zhihu_answer.content = self["content"]
        zhihu_answer.praise_num = self["praise_num"]
        zhihu_answer.comments_num = self["comments_num"]
        zhihu_answer.create_time = datetime.datetime.fromtimestamp(self["create_time"])
        zhihu_answer.update_time = datetime.datetime.fromtimestamp(self["update_time"])
        zhihu_answer.crawl_time = self["crawl_time"]

        zhihu_answer.save()
        redis_cli.incr("zhihu_answer_count")
        return


def replace_splash(value):
    return value.replace("/", "")


def handle_strip(value):
    return value.strip()


def handle_jobaddr(value):
    addr_list = value.split("\n")
    addr_list = [item.strip() for item in addr_list if item.strip() != "查看地图"]
    return "".join(addr_list)


def remove_splash(value):
    # 去掉工作城市的斜线
    return value.replace("/", "")


def handle_jobaddr(value):
    addr_list = value.split("\n")
    addr_list = [item.strip() for item in addr_list if item.strip() != "查看地图"]
    return "".join(addr_list)


class LagouJobItemLoader(ItemLoader):
    # 自定义itemloader
    default_output_processor = TakeFirst()


class LagouJobItem(scrapy.Item):
    # 拉勾网职位信息
    title = scrapy.Field()
    url = scrapy.Field()
    url_object_id = scrapy.Field()
    salary = scrapy.Field()
    job_city = scrapy.Field(
        input_processor=MapCompose(remove_splash),
    )
    work_years = scrapy.Field(
        input_processor=MapCompose(remove_splash),
    )
    degree_need = scrapy.Field(
        input_processor=MapCompose(remove_splash),
    )
    job_type = scrapy.Field()
    publish_time = scrapy.Field()
    job_advantage = scrapy.Field()
    job_desc = scrapy.Field()
    job_addr = scrapy.Field(
        input_processor=MapCompose(remove_tags, handle_jobaddr),
    )
    company_name = scrapy.Field()
    company_url = scrapy.Field()
    tags = scrapy.Field(
        input_processor=Join(",")
    )
    crawl_time = scrapy.Field()

    def get_insert_sql(self):
        insert_sql = """
            insert into lagou_job(title, url, url_object_id, salary, job_city, work_years, degree_need,
            job_type, publish_time, job_advantage, job_desc, job_addr, company_name, company_url,
            tags, crawl_time) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            ON DUPLICATE KEY UPDATE salary=VALUES(salary), job_desc=VALUES(job_desc)
        """
        params = (
            self["title"], self["url"], self["url_object_id"], self["salary"], self["job_city"],
            self["work_years"], self["degree_need"], self["job_type"],
            self["publish_time"], self["job_advantage"], self["job_desc"],
            self["job_addr"], self["company_name"], self["company_url"],
            self["job_addr"], self["crawl_time"].strftime(SQL_DATETIME_FORMAT),
        )

        return insert_sql, params

    def save_to_es(self):
        # 拉勾网职位信息

        laGou = Lagou()

        laGou.title_suggest = gen_suggests(Lagou._doc_type.index, ((laGou.title, 10), (laGou.tags, 7)))
        laGou.title = self["title"]
        laGou.meta.id = self["url_object_id"]
        laGou.url = self["url"]
        laGou.salary = self["salary"]
        laGou.job_city = self["job_city"]
        laGou.work_years = self["work_years"]
        laGou.degree_need = self["degree_need"]
        laGou.job_type = self["job_type"]
        laGou.publish_time = self["publish_time"]
        laGou.job_advantage = self["job_advantage"]
        laGou.job_desc = self["job_desc"]
        laGou.job_addr = self["job_addr"]
        laGou.company_name = self["company_name"]
        laGou.company_url = self["company_url"]
        laGou.tags = ",".join(self["tags"])
        laGou.crawl_time = self["crawl_time"]

        laGou.save()

        redis_cli.incr("lagou_count")

        return
