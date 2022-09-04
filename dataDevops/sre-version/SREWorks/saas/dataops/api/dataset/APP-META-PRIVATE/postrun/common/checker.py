# coding: utf-8

import time
import requests
from .constant import host

max_try_times = 60
retry_time_interval_seconds = 60


def check_sreworks_data_service_ready():
    for service_name, service_host in host.items():
        try_times = 1
        while try_times < max_try_times:
            try:
                r = requests.get(service_host)
                break
            except Exception as ex:
                print(ex)
                time.sleep(retry_time_interval_seconds)
                if try_times >= max_try_times:
                    raise Exception(ex)
