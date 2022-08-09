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


COMMANDS_TOPIC = '/user/commands'
CONFIGURATIONS_TOPIC = '/user/configs'
HOST_LEVEL_PARAMS_TOPIC = '/user/host_level_params'
ALERTS_DEFINITIONS_TOPIC = '/user/alert_definitions'
METADATA_TOPIC = '/events/metadata'
TOPOLOGIES_TOPIC = '/events/topologies'
SERVER_RESPONSES_TOPIC = '/user/'
AGENT_ACTIONS_TOPIC = '/user/agent_actions'
ENCRYPTION_KEY_TOPIC = '/events/encryption_key'

PRE_REGISTRATION_TOPICS_TO_SUBSCRIBE = [SERVER_RESPONSES_TOPIC, AGENT_ACTIONS_TOPIC, ENCRYPTION_KEY_TOPIC]
POST_REGISTRATION_TOPICS_TO_SUBSCRIBE = [COMMANDS_TOPIC]

AGENT_RESPONSES_TOPIC = '/reports/responses'
TOPOLOGY_REQUEST_ENDPOINT = '/agents/topologies'
METADATA_REQUEST_ENDPOINT = '/agents/metadata'
CONFIGURATIONS_REQUEST_ENDPOINT = '/agents/configs'
HOST_LEVEL_PARAMS_TOPIC_ENPOINT = '/agents/host_level_params'
ALERTS_DEFINITIONS_REQUEST_ENDPOINT = '/agents/alert_definitions'
COMPONENT_STATUS_REPORTS_ENDPOINT = '/reports/component_status'
COMPONENT_VERSION_REPORTS_ENDPOINT = '/reports/component_version'
COMMANDS_STATUS_REPORTS_ENDPOINT = '/reports/commands_status'
HOST_STATUS_REPORTS_ENDPOINT = '/reports/host_status'
ALERTS_STATUS_REPORTS_ENDPOINT = '/reports/alerts_status'

HEARTBEAT_ENDPOINT = '/heartbeat'
REGISTRATION_ENDPOINT = '/register'

CORRELATION_ID_STRING = 'correlationId'
MESSAGE_ID = 'messageId'
