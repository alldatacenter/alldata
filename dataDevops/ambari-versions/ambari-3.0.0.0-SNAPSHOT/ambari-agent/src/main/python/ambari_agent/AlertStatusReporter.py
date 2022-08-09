#!/usr/bin/env python

'''
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
'''

import logging
import threading
from collections import defaultdict
from ambari_stomp.adapter.websocket import ConnectionIsAlreadyClosed
from ambari_agent import Constants

logger = logging.getLogger(__name__)

class AlertStatusReporter(threading.Thread):
  """
  Thread sends alert reports to server. The report is only sent if its 'text' or 'state' has changed.
  This is done to reduce bandwidth usage on large clusters and number of operations (DB and others) done by server.
  """
  FIELDS_CHANGED_RESEND_ALERT = ['text', 'state']

  def __init__(self, initializer_module):
    self.initializer_module = initializer_module
    self.collector = initializer_module.alert_scheduler_handler.collector()
    self.stop_event = initializer_module.stop_event
    self.alert_reports_interval = initializer_module.config.alert_reports_interval
    self.alert_definitions_cache = initializer_module.alert_definitions_cache
    self.stale_alerts_monitor = initializer_module.stale_alerts_monitor
    self.server_responses_listener = initializer_module.server_responses_listener
    self.reported_alerts = defaultdict(lambda:defaultdict(lambda:[]))
    self.send_alert_changes_only = initializer_module.config.send_alert_changes_only
    threading.Thread.__init__(self)

  def run(self):
    """
    Run an endless loop which reports all the alert statuses got from collector
    """
    if self.alert_reports_interval == 0:
      logger.warn("AlertStatusReporter is turned off. Some functionality might not work correctly.")
      return

    while not self.stop_event.is_set():
      try:
        if self.initializer_module.is_registered:
          self.clean_not_existing_clusters_info()
          alerts = self.collector.alerts()
          self.stale_alerts_monitor.save_executed_alerts(alerts)
          alerts_to_send = self.get_changed_alerts(alerts) if self.send_alert_changes_only else alerts

          if alerts_to_send and self.initializer_module.is_registered:
            correlation_id = self.initializer_module.connection.send(message=alerts_to_send, destination=Constants.ALERTS_STATUS_REPORTS_ENDPOINT, log_message_function=AlertStatusReporter.log_sending)
            self.server_responses_listener.listener_functions_on_success[correlation_id] = lambda headers, message: self.save_results(alerts_to_send)

      except ConnectionIsAlreadyClosed: # server and agent disconnected during sending data. Not an issue
        pass
      except:
        logger.exception("Exception in AlertStatusReporter. Re-running it")

      self.stop_event.wait(self.alert_reports_interval)

    logger.info("AlertStatusReporter has successfully finished")

  def save_results(self, alerts):
    """
    Save alert reports which were synced to server
    """
    for alert in alerts:
      cluster_id = alert['clusterId']
      alert_name = alert['name']

      self.reported_alerts[cluster_id][alert_name] = [alert[field] for field in self.FIELDS_CHANGED_RESEND_ALERT]

  def get_changed_alerts(self, alerts):
    """
    Get alert reports, which changed since last successful report to server
    """
    changed_alerts = []
    for alert in alerts:
      cluster_id = alert['clusterId']
      alert_name = alert['name']

      if [alert[field] for field in self.FIELDS_CHANGED_RESEND_ALERT] != self.reported_alerts[cluster_id][alert_name]:
        changed_alerts.append(alert)

    return changed_alerts
    

  def clean_not_existing_clusters_info(self):
    """
    This needs to be done to remove information about clusters which where deleted (e.g. ambari-server reset)
    """
    for cluster_id in self.reported_alerts.keys():
      if not cluster_id in self.alert_definitions_cache.get_cluster_ids():
        del self.reported_alerts[cluster_id]


  @staticmethod
  def log_sending(message_dict):
    """
    Returned dictionary will be used while logging sent alert status.
    Used because full dict is too big for logs and should be shortened
    """
    try:
      for alert_status in message_dict:
        if 'text' in alert_status:
          alert_status['text'] = '...'
    except KeyError:
      pass
      
    return message_dict
