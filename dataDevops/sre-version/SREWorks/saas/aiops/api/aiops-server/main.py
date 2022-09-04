#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'sreworks'

import os
from flask_swagger_ui import get_swaggerui_blueprint
from app.aiops_server_app import get_flask_app, DEFAULT_APP_NAME
from handlers.tsp_view import TSPView

os.system("celery multi start w1 -A tsp_train.celery worker -l INFO --pidfile=/var/run/celery/%n.pid  --logfile=/var/log/celery/%n%I.log")

app_name = DEFAULT_APP_NAME
app = get_flask_app(app_name)


SWAGGER_URL = '/aiops/v1/docs'
API_URL = '/static/swagger.json'
swaggerui_blueprint = get_swaggerui_blueprint(
    SWAGGER_URL,
    API_URL,
    config={
        'app_name': "sreworks aiops"
    }
)
app.register_blueprint(swaggerui_blueprint)

app.add_url_rule('/aiops/v1/tsp_config/getById', view_func=TSPView.as_view('get_tsp_config_by_id', exec_func='get_tsp_config_by_id'), methods=['GET'])
app.add_url_rule('/aiops/v1/tsp_config/getByMetric', view_func=TSPView.as_view('get_tsp_config_by_metric', exec_func='get_tsp_config_by_metric'), methods=['GET'])
app.add_url_rule('/aiops/v1/tsp_config/create', view_func=TSPView.as_view('create_tsp_config', exec_func='create_tsp_config'), methods=['POST'])
app.add_url_rule('/aiops/v1/tsp_task/submit', view_func=TSPView.as_view('submit_tsp_task', exec_func='submit_tsp_task'), methods=['POST'])
app.add_url_rule('/aiops/v1/tsp/getTspResult', view_func=TSPView.as_view('get_tsp_result', exec_func='get_tsp_result'), methods=['GET'])


if __name__ == "__main__":
    app.run(
        host='0.0.0.0',
        port=9000
    )
