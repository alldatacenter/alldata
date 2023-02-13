from celery import Celery
from app.aiops_server_app import get_flask_app, DEFAULT_APP_NAME
from common.config import get_config

app_name = DEFAULT_APP_NAME
app = get_flask_app(app_name)

config = get_config()
celery_result_backend_config = config.get('redis').get('celery_result_backend')
celery_broker_config = config.get('redis').get('celery_broker')


def _build_redis_url(host, port=6379, db=0, password=None):
    if password is None:
        return 'redis://:' + celery_result_backend_config.get('password') + '@' + host + ':' + str(port) + '/' + str(db)
    else:
        return 'redis://:' + password + '@' + host + ':' + str(port) + '/' + str(db)


# 'redis://localhost:6379'
app.config['CELERY_RESULT_BACKEND'] = _build_redis_url(celery_result_backend_config.get('host'),
                                                       celery_result_backend_config.get('port'),
                                                       celery_result_backend_config.get('db'),
                                                       celery_result_backend_config.get('password'))
app.config['CELERY_BROKER_URL'] = _build_redis_url(celery_broker_config.get('host'), celery_broker_config.get('port'),
                                                   celery_broker_config.get('db'), celery_broker_config.get('password'))

# celery = Celery(
#     app.import_name,
#     backend=app.config['CELERY_RESULT_BACKEND'],
#     broker=app.config['CELERY_BROKER_URL']
# )
# celery.conf.update(app.config)


def make_celery():
    celery = Celery(
        app.import_name,
        backend=app.config['CELERY_RESULT_BACKEND'],
        broker=app.config['CELERY_BROKER_URL']
    )
    celery.conf.update(app.config)

    # class ContextTask(celery.Task):
    #     def __call__(self, *args, **kwargs):
    #         with app.app_context():
    #             return self.run(*args, **kwargs)
    #
    # celery.Task = ContextTask
    return celery
