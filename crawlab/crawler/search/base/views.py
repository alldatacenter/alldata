import json
from django.shortcuts import render
from django.views.generic.base import View
from search.models import Article
from django.http import HttpResponse
from elasticsearch import Elasticsearch
from datetime import datetime
import redis

client = Elasticsearch(hosts=["172.16.252.113"])
redis_cli = redis.StrictRedis()


def unicode(s):
    if not isinstance(s, str):
        try:
            return s.decode('utf-8')
        except UnicodeDecodeError:
            pass
    return s


class IndexView(View):
    # 首页
    def get(self, request):
        topn_search_old = redis_cli.zrevrangebyscore("search_keywords_set", "+inf", "-inf", start=0, num=5)
        topn_search = []
        for top in topn_search_old:
            top = unicode(top)
            topn_search.append(top)
        return render(request, "index.html", {"topn_search": topn_search})


# Create your views here.
class SearchSuggest(View):
    def get(self, request):
        key_words = request.GET.get('s', '')
        re_datas = []
        if key_words:
            s = Article.search()
            s = s.suggest('my_suggest', key_words, completion={
                "field": "title_suggest", "fuzzy": {
                    "fuzziness": 2
                },
                "size": 10
            })
            suggestions = s.execute_suggest()
            for match in suggestions.my_suggest[0].options:
                source = match._source
                re_datas.append(source["title"])
        return HttpResponse(json.dumps(re_datas), content_type="application/json")


class SearchView(View):
    def get(self, request):
        key_words = request.GET.get("q", "")
        s_type = request.GET.get("s_type", "jobbole_article")
        redis_cli.zincrby("search_keywords_set", key_words)
        topn_search_old = redis_cli.zrevrangebyscore("search_keywords_set", "+inf", "-inf", start=0, num=5)
        topn_search = []
        for top in topn_search_old:
            top = unicode(top)
            topn_search.append(top)
        # print(topn_search)
        page = request.GET.get("p", "1")
        try:
            page = int(page)
        except:
            page = 1

        jobbole_count = int(redis_cli.get("jobbole_count"))
        lagou_count = int(redis_cli.get("lagou_count"))
        zhihu_question_count = int(redis_cli.get("zhihu_question_count"))
        start_time = datetime.now()
        response = client.search(
            index="jobbole",
            doc_type=s_type,
            body={
                "query": {
                    "multi_match": {
                        "query": key_words,
                        "fields": ["tags", "title", "content"]
                    }
                },
                "from": (page - 1) * 10,
                "size": 10,
                "highlight": {
                    "pre_tags": ['<span class="keyWord">'],
                    "post_tags": ['</span>'],
                    "fields": {
                        "title": {},
                        "content": {},
                    }
                }
            }
        )

        end_time = datetime.now()
        last_seconds = (end_time - start_time).total_seconds()
        total_nums = response["hits"]["total"]
        if (page % 10) > 0:
            page_nums = int(total_nums / 10) + 1
        else:
            page_nums = int(total_nums / 10)
        hit_list = []
        for hit in response["hits"]["hits"]:
            hit_dict = {}
            if "title" in hit["highlight"]:
                hit_dict["title"] = "".join(hit["highlight"]["title"])
            else:
                hit_dict["title"] = hit["_source"]["title"]

            if s_type == "jobbole_article":
                if "content" in hit["highlight"]:
                    hit_dict["content"] = "".join(hit["highlight"]["content"])[:500]
                else:
                    hit_dict["content"] = hit["_source"]["content"][:500]

            if s_type == "jobbole_article":
                hit_dict["create_date"] = hit["_source"]["create_date"]
            else:
                hit_dict["create_date"] = ""
            hit_dict["url"] = hit["_source"]["url"]
            hit_dict["score"] = hit["_score"]

            hit_list.append(hit_dict)

        return render(request, "result.html", {"page": page,
                                               "all_hits": hit_list,
                                               "key_words": key_words,
                                               "total_nums": total_nums,
                                               "page_nums": page_nums,
                                               "last_seconds": last_seconds,
                                               "jobbole_count": jobbole_count,
                                               "zhihu_question_count": zhihu_question_count,
                                               "lagou_count": lagou_count,
                                               "topn_search": topn_search,
                                               "s_type": s_type})
