import ConfigParser
import time
import os
import json
import sys

from web import Storage

import logging


class ConfdInit(object):


    @classmethod
    def _parse_ini_conf(cls, conf_path):
        """
        parse .ini conf
        param: conf_path  conf file path
        """
        result = {}
        config = ConfigParser.ConfigParser()
        config.optionxform = str
        config.read(conf_path)
        for section in config.sections():
            result[section] = {}
            for option in config.options(section):
                result[section][option] = config.get(section, option)
        return Storage(result)

    @classmethod
    def _parse_json_conf(cls, conf_path):
        result = {}
        try:
            with open(conf_path) as f:
                result = json.loads(f.read(), encoding='utf-8')
        except IOError:
            pass
        if not isinstance(result, dict):
            raise Exception("invalid json config, not a HashMap type")
        return Storage(result)

    @classmethod
    def _parse_py_conf(cls, conf_mod_name):
        result = {}
        try:
            __import__(conf_mod_name)
            result = vars(sys.modules[conf_mod_name])
            # execfile_(filename, cfg, cfg)
        except ImportError, IOError:
            pass
        result = {k: v for k, v in result.iteritems() if not k.startswith('__')}
        return Storage(result)

    @classmethod
    def load_conf(cls, base_dir, mod_name, env=None):
        logging.info("start load conf to cache")
        config = Storage({})
        # thread = threading.Thread(target=cls._parse_config_files, name='parse_conf_d_tread',
        #                           args=(base_dir, config, mod_name))
        # thread.start()
        cls._parse_config_files(base_dir, config, mod_name, env)
        return config

    @classmethod
    def _parse_config_files(cls, base_path, config, mod_name, env=None):
        logging.info('base_path=%s, mode_name=%s, env=%s', base_path, mod_name, env)
        new_config = {}
        new_config.update(cls._parse_ini_conf(os.path.join(base_path, 'conf.ini')))
        new_config.update(cls._parse_json_conf(os.path.join(base_path, 'conf.json')))
        new_config.update(cls._parse_py_conf("%s.%s" % (mod_name, 'conf')))
        if env is not None:
            # ini file
            file_path = os.path.join(base_path, 'conf_%s.ini' % env.lower())
            if os.path.exists(file_path):
                new_config.update(cls._parse_ini_conf(file_path))

            # json file
            file_path = os.path.join(base_path, 'conf_%s.json' % env.lower())
            if os.path.exists(file_path):
                new_config.update(cls._parse_json_conf(file_path))

            # python file
            file_path = os.path.join(base_path, 'conf_%s.py' % env.lower())
            if os.path.exists(file_path):
                new_config.update(cls._parse_py_conf('%s.conf_%s' % (mod_name, env.lower())))

        for root, _, files in os.walk('%s/conf.d/' % base_path):
            for file_name in files:
                if file_name.endswith('.ini'):
                    new_config.update(cls._parse_ini_conf(os.path.join(root, file_name)))
                elif file_name.endswith('.json'):
                    new_config.update(cls._parse_json_conf(os.path.join(root, file_name)))
                else:
                    logging.error("not support this type file, file=%s", file_name)
        logging.debug('parse conf result: %s', new_config)
        config.clear()
        config.update(new_config)

    @classmethod
    def _parse_config_filesd(cls, base_path, config, mod_name):
        while True:
            new_config = {}
            new_config.update(cls._parse_ini_conf(os.path.join(base_path, 'conf.ini')))
            new_config.update(cls._parse_json_conf(os.path.join(base_path, 'conf.json')))
            new_config.update(cls._parse_py_conf("%s.%s" % (mod_name, 'conf')))
            for root, _, files in os.walk('%s/conf.d/' % base_path):
                for file_name in files:
                    if file_name.endswith('.ini'):
                        new_config.update(cls._parse_ini_conf(os.path.join(root, file_name)))
                    elif file_name.endswith('.json'):
                        new_config.update(cls._parse_json_conf(os.path.join(root, file_name)))
                    else:
                        logging.error("not support this type file, file=%s", file_name)
            logging.debug('parse confd result: %s', new_config)
            config.clear()
            config.update(new_config)
            time.sleep(60)
