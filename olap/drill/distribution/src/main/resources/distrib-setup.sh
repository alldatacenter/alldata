#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# This file is empty by default. Drill startup checks will be
# performed in drill-config.sh. Distributions can replace this file with a
# distribution-specific version that checks (and sets) environment variables
# and options specific to that distribution.
# Users should not put anything in this file. Additional checks can be defined
# and put in drill-setup.sh instead.
# To FAIL any check, return with a non-zero return code
# e.g.
# if [ $status == "FAILED" ]; return 1; fi

###==========================================================================
# FEATURES
###==========================================================================
