# -*- coding: utf-8 -*-
__author__ = 'bobby'
from datetime import datetime
from elasticsearch_dsl import DocType, Date, Integer, Keyword, Text
from elasticsearch_dsl.connections import connections

# Define a default Elasticsearch client
connections.create_connection(hosts=['localhost'])

from ArticleSpider.models.models import Article

s = Article.search()
s = s.suggest('title_suggestion', 'python', completion={'field': 'title_suggest', 'fuzzy': {'fuzziness': 2}, 'size': 10})
suggestions = s.execute_suggest()
for match in suggestions.title_suggestion[0].options:
    source = match._source
    print (source['title'], match._score)
# Display cluster health
# print(connections.get_connection().cluster.health())


# from elasticsearch_dsl import Keyword, Mapping, Nested, Text
#
# m = Mapping('article')
#
# # add fields
# m.field('title', 'text')
#
# # you can use multi-fields easily
# m.field('category', 'text', fields={'raw': Keyword()})
# # you can also create a field manually
# comment = Nested()
# comment.field('author', Text())
# comment.field('created_at', Date())
#
# # and attach it to the mapping
# m.field('comments', comment)
#
# # you can also define mappings for the meta fields
# m.meta('_all', enabled=False)

# save the mapping into index 'my-index'
# m.save('my-index')