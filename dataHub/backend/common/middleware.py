from __future__ import unicode_literals
import logging
import json
import re


class ApiLogMiddleware(object):
    def __init__(self, get_response):
        self.get_response = get_response
        self.apiLogger = logging.getLogger('api')

    def __call__(self, request):
        try:
            body = json.loads(request.body)
        except Exception:
            body = dict()
        body.update(dict(request.POST))
        query = ""
        for key in body.keys():
            if key == "query":
                query = body[key] = body.get(key).replace("\n", "").replace("\t", "")
                query = re.sub(' +', ' ', query)
        response = self.get_response(request)
        if request.path == "/graphql/":
            self.apiLogger.info("\nrequest: {}\tcode: {}\tcontent: {}".format(
                query,
                response.status_code,
                response.content[0:200]))
        return response
