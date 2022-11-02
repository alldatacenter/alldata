# coding: utf-8

from .risk import risk_job_config_init
from .alert import alert_job_config_init
from .incident import order_incident_job_config_init
from .incident import pay_incident_job_config_init
from .incident import oa_incident_job_config_init
from .collect import collect_job_config_init
from .normal import normal_job_config_init


def init():
    risk_job_config_init.add_jobs()
    alert_job_config_init.add_jobs()
    order_incident_job_config_init.add_jobs()
    pay_incident_job_config_init.add_jobs()
    oa_incident_job_config_init.add_jobs()
    collect_job_config_init.add_jobs()
    normal_job_config_init.add_jobs()
