#!/bin/sh

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


USAGE="Usage: setup_authentication.sh [UNIX|LDAP|AD|NONE] <path>"

if [ $# -ne 2 ]
  then
    echo $USAGE;
fi

curDt=`date '+%Y%m%d%H%M%S'`
authentication_method=$1
path=$2
CONFIG_FILE=$path/WEB-INF/classes/conf/security-applicationContext.xml

if [ $authentication_method = "UNIX" ] ; then
    	echo $path;
	awk 'FNR==NR{ _[++d]=$0;next}
	/UNIX_BEAN_SETTINGS_START/{
	  print
	  for(i=1;i<=d;i++){ print _[i] }
	  f=1;next
	}
	/UNIX_BEAN_SETTINGS_END/{f=0}!f' $path/META-INF/contextXML/unix_bean_settings.xml $CONFIG_FILE  > tmp
	mv tmp $CONFIG_FILE
	
	awk 'FNR==NR{ _[++d]=$0;next}
	/UNIX_SEC_SETTINGS_START/{
	  print
	  for(i=1;i<=d;i++){ print _[i] }
	  f=1;next
	}
	/UNIX_SEC_SETTINGS_END/{f=0}!f' $path/META-INF/contextXML/unix_security_settings.xml $CONFIG_FILE  > tmp
	mv tmp $CONFIG_FILE

    exit 0;

elif [ $authentication_method = "LDAP" ]; then
	echo $path;
	awk 'FNR==NR{ _[++d]=$0;next}
	/LDAP_BEAN_SETTINGS_START/{
	  print
	  for(i=1;i<=d;i++){ print _[i] }
	  f=1;next
	}
	/LDAP_BEAN_SETTINGS_END/{f=0}!f' $path/META-INF/contextXML/ldap_bean_settings.xml $CONFIG_FILE  > tmp
	mv tmp $CONFIG_FILE
		
	awk 'FNR==NR{ _[++d]=$0;next}
	/LDAP_SEC_SETTINGS_START/{
	  print
	  for(i=1;i<=d;i++){ print _[i] }
	  f=1;next
	}
	/LDAP_SEC_SETTINGS_END/{f=0}!f' $path/META-INF/contextXML/ldap_security_settings.xml $CONFIG_FILE  > tmp
	mv tmp $CONFIG_FILE

    exit 0;
			
elif [ $authentication_method = "ACTIVE_DIRECTORY" ]; then
	 echo $path;
	    awk 'FNR==NR{ _[++d]=$0;next}
	/AD_BEAN_SETTINGS_START/{
	  print
	  for(i=1;i<=d;i++){ print _[i] }
	  f=1;next
	}
	/AD_BEAN_SETTINGS_END/{f=0}!f' $path/META-INF/contextXML/ad_bean_settings.xml $CONFIG_FILE  > tmp
	mv tmp $CONFIG_FILE
		
	awk 'FNR==NR{ _[++d]=$0;next}
	/AD_SEC_SETTINGS_START/{
	  print
	  for(i=1;i<=d;i++){ print _[i] }
	  f=1;next
	}
	/AD_SEC_SETTINGS_END/{f=0}!f' $path/META-INF/contextXML/ad_security_settings.xml $CONFIG_FILE  > tmp
	mv tmp $CONFIG_FILE

    exit 0;
elif [ $authentication_method = "NONE" ]; then
echo $path;
    exit 0;
else
    echo $USAGE;
    exit 1;
fi




