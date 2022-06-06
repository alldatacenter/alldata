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
import pprint

from ambari_server.serverConfiguration import get_value_from_properties, get_ambari_properties, update_properties_2
from ambari_server.userInput import get_prompt_default,get_validated_string_input, get_YN_input
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons.exceptions import FatalException
from ambari_commons.os_utils import is_root

REGEX_TRUE_FALSE = "^(true|false)?$"
REGEX_ANYTHING = ".*"

logger = logging.getLogger(__name__)

class KerberosPropertyTemplate:
    def __init__(self, properties, i_option, i_prop_name, i_prop_val_pattern, i_prompt_regex, i_allow_empty_prompt, i_prop_name_default=None):
        self.prop_name = i_prop_name
        self.option = i_option
        self.kerberos_prop_name = get_value_from_properties(properties, i_prop_name, i_prop_name_default)
        self.kerberos_prop_val_prompt = i_prop_val_pattern.format(get_prompt_default(self.kerberos_prop_name))
        self.prompt_regex = i_prompt_regex
        self.allow_empty_prompt = i_allow_empty_prompt

@OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
def init_kerberos_properties_list(properties, options):
    kerberos_properties = [
        KerberosPropertyTemplate(properties, options.kerberos_enabled, "authentication.kerberos.enabled", "Enable Kerberos authentication [true|false] {0}: ",
                                 REGEX_TRUE_FALSE, False, "false"),
        KerberosPropertyTemplate(properties, options.kerberos_spnego_principal, "authentication.kerberos.spnego.principal", "SPNEGO principal {0}: ",
                                 REGEX_ANYTHING, False, "HTTP/_HOST"),
        KerberosPropertyTemplate(properties, options.kerberos_spnego_keytab_file, "authentication.kerberos.spnego.keytab.file", "SPNEGO keytab file {0}: ",
                                 REGEX_ANYTHING, False, "/etc/security/keytabs/spnego.service.keytab"),
        KerberosPropertyTemplate(properties, options.kerberos_auth_to_local_rules, "authentication.kerberos.auth_to_local.rules", "Auth-to-local rules {0}: ",
                                 REGEX_ANYTHING, False, "DEFAULT")
    ]
    return kerberos_properties

def setup_kerberos(options):
    logger.info("Setting up Kerberos authentication...")
    if not is_root():
        err = "ambari-server setup-kerberos should be run with root-level privileges"
        raise FatalException(4, err)

    properties = get_ambari_properties()
    kerberos_property_list_required = init_kerberos_properties_list(properties, options)

    kerberos_property_value_map = {}
    for kerberos_property in kerberos_property_list_required:
        input = get_validated_string_input(
            kerberos_property.kerberos_prop_val_prompt,
            kerberos_property.kerberos_prop_name,
            kerberos_property.prompt_regex,
            "Invalid characters in the input!",
            False,
            kerberos_property.allow_empty_prompt
            )
        if input is not None and input != "":
            kerberos_property_value_map[kerberos_property.prop_name] = input

    print "Properties to be updated / written into ambari properties:"
    pp = pprint.PrettyPrinter()
    pp.pprint(kerberos_property_value_map)


    save = get_YN_input("Save settings [y/n] (y)? ", True)
    if save:
        update_properties_2(properties, kerberos_property_value_map)
        print "Kerberos authentication settings successfully saved. Please restart the server in order for the new settings to take effect."
    else:
        print "Kerberos authentication settings aborted."

    return 0;





