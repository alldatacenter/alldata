# coding: utf-8

from . import health_event_definition_init as event
from . import health_risk_definition_init as risk
from . import health_alert_definition_init as alert
from . import health_incident_definition_init as incident
from . import health_failure_definition_init as failure


def init():
    # event.add_event_definitions()
    risk.add_risk_definitions()
    alert.add_alert_definitions()
    incident.add_incident_type()
    incident.add_incident_definitions()
    failure.add_failure_definitions()
