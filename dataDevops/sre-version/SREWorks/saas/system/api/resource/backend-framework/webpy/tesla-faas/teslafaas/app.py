#!/home/tops/bin/python
# coding: utf-8
"""
tesla_faas ~/handlers1/ ~/handlers2/

1. add python path of handlers
2. create handlers webpy app
3. create faas server ProxyAppModule webpy app
4. Invoke `TeslaFaasServerApplication ProxyAppModule
--containers handler1Module --containers handler2Module ` in tesla-gunicorn
"""
import logging
import os
import sys
import traceback
from teslafaas.container.webpy.init.webpy_init import WebPyInitContext, WebPyStartParam
from teslafaas.common.service_register import ServiceRegister
from container.loader import ContainerLoader
from common import env_init


if os.getenv('DISABLED_GEVENT') is None or os.getenv('DISABLED_GEVENT') == "false":
    logging.info("enable gevent =====")
    import gevent
    from gevent import monkey
    monkey.noisy = False
    # if the new version is used make sure to patch subprocess
    if gevent.version_info[0] == 0:
        monkey.patch_all()
    else:
        monkey.patch_all(subprocess=True)

# Only for dev
here = os.path.abspath(os.path.dirname(__file__))
root_path = os.path.join(here, "../")
sys.path.insert(0, os.path.abspath(root_path))


def app_start(app_home, product='tesla', service_name='tesla-faas', env="DAILY", port=8000, is_debug=True):
    # init global logger config at start time
    try:
        param = WebPyStartParam()
        param.app_home = app_home
        param.product = product
        param.service_name = service_name
        param.env = env
        param.port = port
        param.is_debug = is_debug
        mod_name = os.path.basename(os.path.abspath(app_home.rstrip('/')))
        env_init.init_logger(mod_name)
        context = WebPyInitContext(param)
        loader = ContainerLoader(context)
        app_wsgi_mod, container = loader.load_module(app_home, env=env)
        #register to nacos
        register = ServiceRegister(context)
        register.registerd()
        return app_wsgi_mod.wsgifunc
    except:
        print traceback.format_exc()
