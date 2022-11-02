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

# action commands
SETUP_ACTION = "setup"
START_ACTION = "start"
PSTART_ACTION = "pstart"
STOP_ACTION = "stop"
RESTART_ACTION = "restart"
RESET_ACTION = "reset"
UPGRADE_ACTION = "upgrade"
REFRESH_STACK_HASH_ACTION = "refresh-stack-hash"
STATUS_ACTION = "status"
SETUP_HTTPS_ACTION = "setup-https"
SETUP_JDBC_ACTION = "setup-jdbc"
LDAP_SETUP_ACTION = "setup-ldap"
SETUP_SSO_ACTION = "setup-sso"
LDAP_SYNC_ACTION = "sync-ldap"
SET_CURRENT_ACTION = "set-current"
SETUP_GANGLIA_HTTPS_ACTION = "setup-ganglia-https"
ENCRYPT_PASSWORDS_ACTION = "encrypt-passwords"
SETUP_SECURITY_ACTION = "setup-security"
UPDATE_HOST_NAMES_ACTION = "update-host-names"
CHECK_DATABASE_ACTION = "check-database"
BACKUP_ACTION = "backup"
RESTORE_ACTION = "restore"
SETUP_JCE_ACTION = "setup-jce"
ENABLE_STACK_ACTION = "enable-stack"
DB_PURGE_ACTION = "db-purge-history"
INSTALL_MPACK_ACTION = "install-mpack"
UNINSTALL_MPACK_ACTION = "uninstall-mpack"
UPGRADE_MPACK_ACTION = "upgrade-mpack"
PAM_SETUP_ACTION = "setup-pam"
MIGRATE_LDAP_PAM_ACTION = "migrate-ldap-pam"
KERBEROS_SETUP_ACTION = "setup-kerberos"
SETUP_TPROXY_ACTION = "setup-trusted-proxy"
