# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information rega4rding copyright ownership.
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

##################################################################
#                      SERVER INSTALL HELPER                     #
##################################################################

# WARNING. Please keep the script POSIX compliant and don't use bash extensions

ROOT_DIR_PATH="${RPM_INSTALL_PREFIX}"
ROOT=`echo "${RPM_INSTALL_PREFIX}" | sed 's|/$||g'` # Customized folder, which ambari-server files are installed into ('/' or '' are default).
AMBARI_UNIT="ambari-server"
ACTION=$1


OLD_PYLIB_PATH="${ROOT}/usr/lib/python2.6/site-packages"
OLD_PY_MODULES="ambari_commons;resource_management;ambari_jinja2;ambari_simplejson;ambari_server"

AMBARI_SERVER_ROOT_DIR="${ROOT}/usr/lib/${AMBARI_UNIT}"
AMBARI_AGENT_ROOT_DIR=="${ROOT}/usr/lib/ambari-agent"
AMBARI_SERVER="${AMBARI_SERVER_ROOT_DIR}/lib/ambari_server"

CA_CONFIG="${ROOT}/var/lib/${AMBARI_UNIT}/keys/ca.config"
COMMON_DIR_SERVER="${ROOT}/usr/lib/${AMBARI_UNIT}/lib/ambari_commons"
RESOURCE_MANAGEMENT_DIR_SERVER="${ROOT}/usr/lib/${AMBARI_UNIT}/lib/resource_management"
JINJA_SERVER_DIR="${ROOT}/usr/lib/${AMBARI_UNIT}/lib/ambari_jinja2"
SIMPLEJSON_SERVER_DIR="${ROOT}/usr/lib/${AMBARI_UNIT}/lib/ambari_simplejson"
AMBARI_PROPERTIES="${ROOT}/etc/${AMBARI_UNIT}/conf/ambari.properties"
AMBARI_ENV_RPMSAVE="${ROOT}/var/lib/${AMBARI_UNIT}/ambari-env.sh.rpmsave" # this turns into ambari-env.sh during ambari-server start
AMBARI_SERVER_KEYS_FOLDER="${ROOT}/var/lib/${AMBARI_UNIT}/keys"
AMBARI_SERVER_KEYS_DB_FOLDER="${ROOT}/var/lib/${AMBARI_UNIT}/keys/db"
AMBARI_SERVER_NEWCERTS_FOLDER="${ROOT}/var/lib/${AMBARI_UNIT}/keys/db/newcerts"
CLEANUP_MODULES="resource_management;ambari_commons;ambari_server;ambari_ws4py;ambari_stomp;ambari_jinja2;ambari_simplejson"
AMBARI_SERVER_VAR="${ROOT}/var/lib/${AMBARI_UNIT}"
AMBARI_HELPER="${ROOT}/var/lib/ambari-server/install-helper.sh.orig"

PYTHON_WRAPER_DIR="${ROOT}/usr/bin"
PYTHON_WRAPER_TARGET="${PYTHON_WRAPER_DIR}/ambari-python-wrap"

LOG_FILE=/dev/null

AMBARI_SERVER_EXECUTABLE_LINK="${ROOT}/usr/sbin/ambari-server"
AMBARI_SERVER_EXECUTABLE="${ROOT}/etc/init.d/ambari-server"

AMBARI_CONFIGS_DIR="${ROOT}/etc/ambari-server/conf"
AMBARI_CONFIGS_DIR_SAVE="${ROOT}/etc/ambari-server/conf.save"
AMBARI_CONFIGS_DIR_SAVE_BACKUP="${ROOT}/etc/ambari-server/conf_$(date '+%d_%m_%y_%H_%M').save"
AMBARI_LOG4J="${AMBARI_CONFIGS_DIR}/log4j.properties"


resolve_log_file(){
 local log_dir=/var/log/${AMBARI_UNIT}
 local log_file="${log_dir}/${AMBARI_UNIT}-pkgmgr.log"

 if [ ! -d "${log_dir}" ]; then
   mkdir "${log_dir}" 1>/dev/null 2>&1
 fi

 if [ -d "${log_dir}" ]; then
   touch ${log_file} 1>/dev/null 2>&1
   if [ -f "${log_file}" ]; then
    LOG_FILE="${log_file}"
   fi
 fi

 echo "--> Install-helper custom action log started at $(date '+%d/%m/%y %H:%M') for '${ACTION}'" 1>>${LOG_FILE} 2>&1
}

clean_pyc_files(){
  # cleaning old *.pyc files
  local lib_dir="${AMBARI_SERVER_ROOT_DIR}/lib"

  echo ${CLEANUP_MODULES} | tr ';' '\n' | while read item; do
    local item="${lib_dir}/${item}"
    echo "Cleaning pyc files from ${item}..."
    if [ -d "${item}" ]; then
      find ${item:?} -name *.pyc -exec rm {} \; 1>>${LOG_FILE} 2>&1
    else
      echo "Skipping ${item} pyc cleaning, as package not existing"
    fi
  done
}

remove_ambari_unit_dir(){
  # removing empty dirs, which left after cleaning pyc files

  find "${AMBARI_SERVER_ROOT_DIR}" -type d | tac | while read item; do
    echo "Removing empty dir ${item}..."
    rmdir --ignore-fail-on-non-empty ${item} 1>/dev/null 2>&1
  done

  rm -rf ${AMBARI_HELPER}
  find "${AMBARI_SERVER_VAR}" -type d | tac | while read item; do
    echo "Removing empty dir ${item}..."
    rmdir --ignore-fail-on-non-empty ${item} 1>/dev/null 2>&1
  done
}

remove_autostart(){
   which chkconfig > /dev/null 2>&1
  if [ "$?" -eq 0 ] ; then
    chkconfig --list | grep ambari-server && chkconfig --del ambari-server
  fi
  which update-rc.d > /dev/null 2>&1
  if [ "$?" -eq 0 ] ; then
    update-rc.d -f ambari-server remove
  fi
}

install_autostart(){
  local autostart_server_cmd=""
  which chkconfig > /dev/null 2>&1
  if [ "$?" -eq 0 ] ; then
    autostart_server_cmd="chkconfig --add ambari-server"
  fi
  which update-rc.d > /dev/null 2>&1
  if [ "$?" -eq 0 ] ; then
    autostart_server_cmd="update-rc.d ambari-server defaults"
  fi

  # if installed to customized root folder, skip ambari-server service actions,
  # as no file in /etc/init.d/ambari-server is present
  if [ ! "${ROOT}/" -ef "/" ]; then
    echo "Not adding ambari-server service to startup, as installed to customized root."
    echo "If you need this functionality run the commands below, which create ambari-server service and configure it to run at startup: "
    echo "sudo ln -s ${AMBARI_SERVER_EXECUTABLE} /etc/init.d/ambari-server"
    echo "sudo ${autostart_server_cmd}"
  else
    ${autostart_server_cmd}
  fi
}

locate_python(){
  local python_binaries="/usr/bin/python;/usr/bin/python2;/usr/bin/python2.7"

  echo ${python_binaries}| tr ';' '\n' | while read python_binary; do
    ${python_binary} -c "import sys ; ver = sys.version_info ; sys.exit(not (ver >= (2,7) and ver<(3,0)))" 1>>${LOG_FILE} 2>/dev/null

    if [ $? -eq 0 ]; then
      echo "${python_binary}"
      break
    fi
  done
}

do_install(){

  rm -f "${AMBARI_SERVER_EXECUTABLE_LINK}"
  ln -s "${AMBARI_SERVER_EXECUTABLE}" "${AMBARI_SERVER_EXECUTABLE_LINK}"

  echo ${OLD_PY_MODULES} | tr ';' '\n' | while read item; do
   local old_path="${OLD_PYLIB_PATH}/${item}"
   if [ -d "${old_path}" ]; then
     echo "Removing old python module ${old_path}..."  1>>${LOG_FILE} 2>&1
     rm -rf ${old_path} 1>/dev/null 2>&1
   fi
  done

  # remove old python wrapper
  rm -f "${PYTHON_WRAPER_TARGET}"

  local ambari_python=$(locate_python)

  if [ -z "${ambari_python}" ]; then
    >&2 echo "Cannot detect Python for Ambari to use. Please manually set ${PYTHON_WRAPER_TARGET} link to point to correct Python binary"
  else
    mkdir -p "${PYTHON_WRAPER_DIR}"
    ln -s "${ambari_python}" "${PYTHON_WRAPER_TARGET}"
  fi

  sed -i "s|ambari.root.dir\s*=\s*/|ambari.root.dir=${ROOT_DIR_PATH}|g" "${AMBARI_LOG4J}"
  sed -i "s|root_dir\s*=\s*/|root_dir = ${ROOT_DIR_PATH}|g" "${CA_CONFIG}"
  sed -i "s|^ROOT=\"/\"$|ROOT=\"${ROOT_DIR_PATH}\"|g" "${AMBARI_SERVER_EXECUTABLE}"

  install_autostart |tee -a ${LOG_FILE}

  if [ -d "${AMBARI_SERVER_KEYS_FOLDER}" ]; then
      chmod 700 "${AMBARI_SERVER_KEYS_FOLDER}"
      if [ -d "${AMBARI_SERVER_KEYS_DB_FOLDER}" ]; then
          chmod 700 "${AMBARI_SERVER_KEYS_DB_FOLDER}"
          if [ -d "${AMBARI_SERVER_NEWCERTS_FOLDER}" ]; then
              chmod 700 "${AMBARI_SERVER_NEWCERTS_FOLDER}"
          fi
      fi
  fi

  if [ -f "${AMBARI_ENV_RPMSAVE}" ]; then
    local python_path_line="export PYTHONPATH=${AMBARI_SERVER_ROOT_DIR}/lib:\$\{PYTHONPATH\}"
    grep "^${python_path_line}\$" "${AMBARI_ENV_RPMSAVE}" > /dev/null
    if [ $? -ne 0 ]; then
      echo -e "\n${python_path_line}" >> ${AMBARI_ENV_RPMSAVE}
    fi
  fi
}

copy_helper(){
  local install_helper="${RPM_INSTALL_PREFIX}/var/lib/ambari-server/install-helper.sh"
  cp -f ${install_helper} ${AMBARI_HELPER} 1>/dev/null 2>&1
}

do_remove(){
  ${AMBARI_SERVER_EXECUTABLE} stop > /dev/null 2>&1

  if [ -d "${AMBARI_CONFIGS_DIR_SAVE}" ]; then
    mv "${AMBARI_CONFIGS_DIR_SAVE}" "${AMBARI_CONFIGS_DIR_SAVE_BACKUP}"
  fi
  # part.1 Remove link created during install AMBARI_ENV_RPMSAVE
  rm -f "${AMBARI_SERVER_EXECUTABLE_LINK}"
  cp -rf "${AMBARI_CONFIGS_DIR}" "${AMBARI_CONFIGS_DIR_SAVE}"

  remove_autostart 1>>${LOG_FILE} 2>&1
  copy_helper
}


do_cleanup(){
  # do_cleanup is a function, which called after do_remove stage and is supposed to be save place to
  # remove obsolete files generated by application activity

  clean_pyc_files 1>>${LOG_FILE} 2>&1
  remove_ambari_unit_dir 1>>${LOG_FILE} 2>&1

  if [ ! -d "${AMBARI_AGENT_ROOT_DIR}" ]; then
    echo "Removing ${PYTHON_WRAPER_TARGET} ..." 1>>${LOG_FILE} 2>&1
    rm -f ${PYTHON_WRAPER_TARGET} 1>>${LOG_FILE} 2>&1
  fi

  # part.2 Remove link created during install AMBARI_ENV_RPMSAVE
  rm -rf "${AMBARI_CONFIGS_DIR}" 1>>${LOG_FILE} 2>&1
}

do_backup(){
  # ToDo: find a way to move backup logic here from preinstall.sh and preinst scripts
  # ToDo: general problem is that still no files are installed on step, when backup is supposed to be done
  echo ""
}

do_upgrade(){
  # this function only gets called for rpm. Deb packages always call do_install directly.
  do_install
}

resolve_log_file

case "${ACTION}" in
    install)
      do_install
      ;;
    remove)
      do_remove
      ;;
    upgrade)
      do_upgrade
      ;;
    cleanup)
      do_cleanup
      ;;
    *)
      echo "Wrong command given"
      ;;
esac
