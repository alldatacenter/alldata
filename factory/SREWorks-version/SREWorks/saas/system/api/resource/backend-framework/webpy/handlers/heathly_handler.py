
from teslafaas.container.webpy.common.BaseHandler import BaseHandler
from teslafaas.container.webpy.common.decorators import app_route, exception_wrapper, rest_log

"""
  [WARNING]
  Replace the class name, 
  and it shoud be same with the function name you created!!!
"""
@app_route(r'/status.taobao')
class BangTest(BaseHandler):

    @rest_log
    @exception_wrapper
    def GET(self, *args):
        return "success"