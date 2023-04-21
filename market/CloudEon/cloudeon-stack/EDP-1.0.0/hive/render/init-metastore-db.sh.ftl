#!/bin/bash


export HIVE_CONF_DIR=/opt/edp/${service.serviceName}/conf
/bin/bash  -c "schematool -dbType mysql -initSchema -verbose"