# coding: utf-8

import json
import web
import socket
from .BaseHandler import BaseHandler
from . import urls as global_urls

LOCAL_HOSTNAME = socket.gethostname()

class IndexHandler(BaseHandler):

    def GET(self, *args):
        ret = {}
        ret['user'] = self.user_name
        ret['empid'] = self.user_empid
        ret['locale'] = self.locale
        ret['server'] = LOCAL_HOSTNAME
        web.header('Content-Type', 'application/json')
        ret['handlers'] = [global_urls[i] for i in xrange(0,len(global_urls),2)]
        return json.dumps(ret)
