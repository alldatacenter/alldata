# -*- coding: utf-8 -*-
__author__ = 'bobby'
from datetime import datetime
from elasticsearch_dsl import DocType, Date, Integer, Keyword, Text
from elasticsearch_dsl.connections import connections
from elasticsearch_dsl import Completion
from elasticsearch_dsl.analysis import CustomAnalyzer as _CustomAnalyzer

# Define a default Elasticsearch client
connections.create_connection(hosts=['localhost'])


class CustomAnalyzer(_CustomAnalyzer):
    def get_analysis_definition(self):
        return {}


ik_analyzer = CustomAnalyzer('ik_max_word', filter=['lowercase'])


class Article(DocType):
    title_suggest = Completion(analyzer=ik_analyzer, search_analyzer=ik_analyzer)
    title = Text(analyzer='ik_max_word', search_analyzer="ik_max_word", fields={'title': Keyword()})
    id = Text()
    url = Text()
    front_image_url = Text()
    front_image_path = Text()
    create_date = Date()
    praise_nums = Integer()
    comment_nums = Integer()
    fav_nums = Integer()
    tags = Text(analyzer='ik_max_word', fields={'tags': Keyword()})
    content = Text(analyzer='ik_max_word')

    class Meta:
        index = 'jobbole'
        doc_type = 'jobbole_article'


class Lagou(DocType):
    # 拉勾网职位信息
    title_suggest = Completion(analyzer=ik_analyzer, search_analyzer=ik_analyzer)
    title = Text(analyzer='ik_max_word', search_analyzer="ik_max_word", fields={'title': Keyword()})
    id = Text()
    url = Text()
    salary = Text()
    job_city = Text()
    work_years = Text()
    degree_need = Text()
    job_type = Text()
    publish_time = Text()
    job_advantage = Text()
    job_desc = Text()
    job_addr = Text()
    company_name = Text()
    company_url = Text()
    tags = Text(analyzer='ik_max_word', fields={'tags': Keyword()})
    crawl_time = Date()

    class Meta:
        index = 'jobbole'
        doc_type = 'lagou_job'


class ZhihuQuestion(DocType):
    title_suggest = Completion(analyzer=ik_analyzer, search_analyzer=ik_analyzer)
    title = Text(analyzer='ik_max_word', search_analyzer="ik_max_word", fields={'title': Keyword()})
    content = Text(analyzer='ik_max_word')
    url = Text()
    question_id = Text()
    answer_num = Integer()
    comments_num = Integer()
    watch_user_num = Integer()
    click_num = Integer()
    topics = Text()
    id = Text()

    class Meta:
        index = 'jobbole'
        doc_type = 'zhihu_question'


class ZhihuAnswer(DocType):
    title_suggest = Completion(analyzer=ik_analyzer, search_analyzer=ik_analyzer)
    id = Text()
    zhihu_id = Text()
    url = Text()
    question_id = Text()
    author_id = Text()
    content = Text(analyzer='ik_max_word')

    praise_num = Integer()
    comments_num = Integer()
    create_time = Date()
    update_time = Date()
    crawl_time = Date()

    class Meta:
        index = 'jobbole'
        doc_type = 'zhihu_answer'


if __name__ == "__main__":
    Article.init()
    Lagou.init()
    ZhihuQuestion.init()
    ZhihuAnswer.init()
