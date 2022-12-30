
from teslafaas.app import app_start
import os


service_name = os.getenv('APP_NAME', 'faas-tesla-qiuqiang-test')
env = os.getenv("ENV", 'DAILY')
server_port = os.getenv('HTTP_SERVER_PORT', '8001')
product = os.getenv('PRODUCT', 'tesla')

app = app_start(os.path.dirname(__file__), product=product, service_name=service_name, env=env, port=server_port)