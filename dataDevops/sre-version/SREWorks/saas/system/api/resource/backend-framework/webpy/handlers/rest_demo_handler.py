
from teslafaas.container.webpy.common.BaseHandler import RestHandler
from teslafaas.container.webpy.common.decorators import app_route,exception_wrapper, rest_log
import time
                
"""
  [WARNING]
  Replace the class name, 
  and it shoud be same with the function name you created!!!
"""
@app_route(r'/restdemo/(.*)')
class BangTest(RestHandler):

    @rest_log
    # @exception_wrapper
    def GET(self, *args):
        print args
        return "success"

    @exception_wrapper
    def POST(self):
        pass
                  
    @exception_wrapper
    def PUT(self):
        pass
                  
    @exception_wrapper
    def DELETE(self):
        pass
