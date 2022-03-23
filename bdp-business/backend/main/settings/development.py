from .default import *


DEBUG = True

ALLOWED_HOSTS = ['*']

# Redis
REDIS_HOST = '127.0.0.1'
REDIS_PORT = 6379
REDIS_DB = 0

INTERNAL_IPS = ('127.0.0.1',)

DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.mysql',
        'NAME': 'wlhbdp_mysql',
        'USER': 'root',
        'PASSWORD': 'YL1cvhweVPspMQ.',
        'HOST': 'localhost',
        'PORT': 3306,
        'OPTIONS': {
            'autocommit': True
        }
    }
}
# Json file path
JSON_PATH = '/Users/wlhbdp/Downloads/wlhbdp/bdp/bdp-brandmining/backend/data/'