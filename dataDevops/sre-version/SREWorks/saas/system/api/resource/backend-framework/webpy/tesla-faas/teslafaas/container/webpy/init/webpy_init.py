from teslafaas.common.check_helper import CheckHelper
from teslafaas.container.webpy.init.config_init import ConfdInit
import logging
import os
import sys


class WebPyStartParam(object):

    def __init__(self):
        self._product = None
        self._service_name = None
        self._env = None
        self._app_home = None
        self._is_debug = True

    def dict_2_objct(self, **kwargs):
        for key, value in kwargs.items():
            self.__setattr__(key, value)

    @property
    def product(self):
        return self._product

    @product.setter
    def product(self, product):
        self._product = CheckHelper.ensure_string('product', product)

    @property
    def service_name(self):
        return self._service_name

    @service_name.setter
    def service_name(self, service_name):
        self._service_name = CheckHelper.ensure_string('service_name', service_name)

    @property
    def env(self):
        return self._env

    @env.setter
    def env(self, env):
        self._env = CheckHelper.ensure_string('env', env)

    @property
    def app_home(self):
        return self._app_home

    @app_home.setter
    def app_home(self, app_home):
        self._app_home = CheckHelper.ensure_string('app_home', app_home)

    @property
    def is_debug(self):
        return self._is_debug

    @is_debug.setter
    def is_debug(self, is_debug):
        self._is_debug = CheckHelper.ensure_boolean('is_debug', is_debug)

    # base_dir = os.path.abspath(src_path.rstrip('/'))
    # module_name = dir_name = os.path.basename(base_dir)


class WebPyInitContext:
    """
    webpy init
    """

    def __init__(self, params):
        logging.info('start init webpy')
        self.params = params
        # self._init_confd()
        self.config = self._init_config()
        self._update_config()

    def _update_config(self):
        if not self.config.has_key('application') or self.config['application'] is None:
            self.config['application'] = {}
        application_ = self.config['application']
        application_['home'] = self.params.app_home
        application_['product'] = self.params.product
        application_['server_name'] = self.params.service_name
        application_['server_port'] = self.params.port
        self.config['env'] = self.params.env

    def get_value_with_required(self, key):
        """
        get required params
        """
        if key is None:
            raise RuntimeError("key should not None")
        ks = key.split(".")
        value = self.config
        for k in ks:
            if not value.has_key(k):
                raise RuntimeError("key not exit in config, key=%s" % key)
            value = value[k]
        if value is None:
            raise RuntimeError("value is None, key=%s" % key)
        return value

    def get_value(self, key, default_value=None):
        """
        get param value
        """
        if key is None:
            raise RuntimeError("key should not None")
        ks = key.split(".")
        value = self.config
        for k in ks:
            if not value.has_key(k):
                return default_value
            value = value[k]
        if value is None:
            return default_value
        return value


    def _init_config(self):
        """
        init config
        """
        base_dir = os.path.abspath(self.params.app_home.rstrip('/'))
        module_name = os.path.basename(base_dir)
        src_parent_path = os.path.dirname(base_dir)
        if src_parent_path not in sys.path:
            sys.path.append(src_parent_path)
        # TODO: add src to python path
        if src_parent_path not in sys.path:
            sys.path.append(src_parent_path)
        config = ConfdInit.load_conf(base_dir, module_name, self.params.env)
        logging.info("read config, config=%s", config)
        return config

