#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License. See accompanying LICENSE file.
#

import os
import re
import sys
import errno
import shlex
import logging
import subprocess
import platform
import fileinput
import getpass
import shutil
from xml.etree import ElementTree as ET
from os.path import basename
from subprocess import Popen,PIPE
from datetime import date
from datetime import datetime
from operator import contains
try: input = raw_input
except NameError: pass

os_name = platform.system()
os_name = os_name.upper()

msgPrompt = "Enter the below options"
msgCommand = "Usage : python rolebasedusersearchutil.py -u <userName> -p <password> -r <role>"
msgRoleList = " <role> can be ROLE_USER/ROLE_SYS_ADMIN/ROLE_KEY_ADMIN/ROLE_ADMIN_AUDITOR/ROLE_KEY_ADMIN_AUDITOR"



if os_name == "LINUX":
    RANGER_ADMIN_HOME = os.getenv("RANGER_ADMIN_HOME")
    if RANGER_ADMIN_HOME is None:
        RANGER_ADMIN_HOME = os.getcwd()
elif os_name == "WINDOWS":
    RANGER_ADMIN_HOME = os.getenv("RANGER_ADMIN_HOME")

def log(msg,type):
    if type == 'info':
        logging.info(" %s",msg)
    if type == 'debug':
        logging.debug(" %s",msg)
    if type == 'warning':
        logging.warning(" %s",msg)
    if type == 'exception':
        logging.exception(" %s",msg)
    if type == 'error':
        logging.error(" %s",msg)

def main(argv):
    FORMAT = '%(asctime)-15s %(message)s'
    logging.basicConfig(format=FORMAT, level=logging.DEBUG)
    ews_lib = os.path.join(RANGER_ADMIN_HOME,"ews","lib")
    app_home = os.path.join(RANGER_ADMIN_HOME,"ews","webapp")
    ranger_log = os.path.join(RANGER_ADMIN_HOME,"ews","logs")

    if os.environ['JAVA_HOME'] == "":
        log("[E] ---------- JAVA_HOME environment property not defined, aborting installation. ----------", "error")
        sys.exit(1)
    JAVA_BIN=os.path.join(os.environ['JAVA_HOME'],'bin','java')
    if os_name == "WINDOWS" :
        JAVA_BIN = JAVA_BIN+'.exe'
    if os.path.isfile(JAVA_BIN):
        pass
    else:
        while os.path.isfile(JAVA_BIN) == False:
            log("Enter java executable path: :","info")
            JAVA_BIN=input()
    log("[I] Using Java:" + str(JAVA_BIN),"info")
    userName = ""
    password = ""
    userRole = ""
    userNameMsgFlag = False
    passwordMsgFlag = False
    userRoleMsgFlag = False
    userroleFlag = False

    if len(argv) == 1:
        log("[I] " +msgPrompt + " or \n" + msgCommand + "\n " +msgRoleList, "info")
        userName = input('Enter a user name: ')
        password = getpass.getpass('Enter a user password:')
        userRole = input('Enter a role: ')
    elif len(argv) > 1 and len(argv) < 8 :
        for i in range(1, len(sys.argv)) :
            if sys.argv[i] == "-u" :
                if len(argv)-1 > i+1 or len(argv)-1 == i+1:
                    userName = sys.argv[i+1]
                    continue
            if sys.argv[i] == "-p" :
                if len(argv)-1 > i+1 or len(argv)-1 == i+1:
                    password = sys.argv[i+1]
                    continue
            if sys.argv[i] == "-r" :
                if len(argv)-1 > i+1 or len(argv)-1 == i+1:
                    userRole = sys.argv[i+1]
                userroleFlag = True
                continue
    else:
        log("[E] Invalid argument list.", "error")
        log("[I] " + msgCommand + "\n  " + msgRoleList, "info")
        sys.exit(1)

    if userName == "" :
       userNameMsgFlag = True
    elif userName != "" :
        if userName.lower() == "-p" or userName.lower() == "-r" or userName.lower() == "-u" :
             userNameMsgFlag = True
    if password == "" :
        passwordMsgFlag = True
    elif  password.lower() == "-p" or password.lower() == "-r" or password.lower() == "-u" :
        passwordMsgFlag = True
    if userroleFlag == True :
        if userRole == "":
           userRoleMsgFlag = True
        elif userRole != "":
            if userRole.lower() == "-p" or userRole.lower() == "-r" or userRole.lower() == "-u":
                userRoleMsgFlag = True
    if userNameMsgFlag == True or  passwordMsgFlag == True or userRoleMsgFlag == True :
        log("[I] "+msgPrompt + " or \n" + msgCommand + "\n  " +msgRoleList, "info")
    if userNameMsgFlag == True :
        userName = input('Enter a user name: ')
    if passwordMsgFlag == True :
        password = getpass.getpass("Enter user password:")
    if userRoleMsgFlag == True :
        userRole = input('Enter a role: ')
    if userName != "" and password != "" :
        if os_name == "LINUX":
            path = os.path.join("%s","WEB-INF","classes","conf:%s","WEB-INF","classes","lib","*:%s","WEB-INF",":%s","META-INF",":%s","WEB-INF","lib","*:%s","WEB-INF","classes",":%s","WEB-INF","classes","META-INF:%s/*")%(app_home ,app_home ,app_home, app_home, app_home, app_home ,app_home,ews_lib)
        elif os_name == "WINDOWS":
            path = os.path.join("%s","WEB-INF","classes","conf;%s","WEB-INF","classes","lib","*;%s","WEB-INF",";%s","META-INF",";%s","WEB-INF","lib","*;%s","WEB-INF","classes",";%s","WEB-INF","classes","META-INF" )%(app_home ,app_home ,app_home, app_home, app_home, app_home ,app_home)
        if userRole != "" :
            get_java_cmd = "%s -Dlogdir=%s -Dlogback.configurationFile=db_patch.logback.xml -cp %s org.apache.ranger.patch.cliutil.%s %s %s %s"%(JAVA_BIN,ranger_log,path,'RoleBasedUserSearchUtil',userName,password,userRole)
        if userRole == "" :
            get_java_cmd = "%s -Dlogdir=%s -Dlogback.configurationFile=db_patch.logback.xml -cp %s org.apache.ranger.patch.cliutil.%s %s %s "%(JAVA_BIN,ranger_log,path,'RoleBasedUserSearchUtil',userName,password)
        if os_name == "LINUX":
            ret = subprocess.call(shlex.split(get_java_cmd))
        elif os_name == "WINDOWS":
            ret = subprocess.call(get_java_cmd)
        if ret == 0:
            log("[I] List fetched successfully","info")
        else:
            log("[E] Unable to fetch user list of given role ","error")
            sys.exit(1)
    else:
        log("[E] Input Error","error")

main(sys.argv)
