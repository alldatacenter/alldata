#!/usr/bin/python
# coding: utf-8
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.

from bin import ambaribuild

# TODO move this to a proper location
def unittest():
	# parse
	result = ambaribuild.parse(["test"])
	assert result.is_deep_clean == False
	assert result.is_test == True
	assert result.is_rebuild == False
	assert result.stack_distribution == None
	assert result.is_install_server == False
	assert result.is_install_agent == False
	assert result.is_deploy == False
	assert result.is_server_debug == False

	result = ambaribuild.parse(["server"])
	assert result.is_deep_clean == False
	assert result.is_test == False
	assert result.is_rebuild == False
	assert result.stack_distribution == None
	assert result.is_install_server == True
	assert result.is_install_agent == False
	assert result.is_deploy == False
	assert result.is_server_debug == False

	result = ambaribuild.parse(["agent"])
	assert result.is_deep_clean == False
	assert result.is_test == False
	assert result.is_rebuild == False
	assert result.stack_distribution == None
	assert result.is_install_server == True
	assert result.is_install_agent == True
	assert result.is_deploy == False
	assert result.is_server_debug == False

	result = ambaribuild.parse(["agent", "-b"])
	assert result.is_deep_clean == False
	assert result.is_test == False
	assert result.is_rebuild == True
	assert result.stack_distribution == None
	assert result.is_install_server == True
	assert result.is_install_agent == True
	assert result.is_deploy == False
	assert result.is_server_debug == False

	result = ambaribuild.parse(["deploy", "-d"])
	assert result.is_deep_clean == False
	assert result.is_test == False
	assert result.is_rebuild == False
	assert result.stack_distribution == None
	assert result.is_install_server == True
	assert result.is_install_agent == True
	assert result.is_deploy == True
	assert result.is_server_debug == True

	result = ambaribuild.parse(["deploy", "-b", "-s", "BIGTOP", "-d", "-c"])
	assert result.is_deep_clean == True
	assert result.is_test == False
	assert result.is_rebuild == True
	assert result.stack_distribution == "BIGTOP"
	assert result.is_install_server == True
	assert result.is_install_agent == True
	assert result.is_deploy == True
	assert result.is_server_debug == True

if __name__ == "__main__":
	unittest()

