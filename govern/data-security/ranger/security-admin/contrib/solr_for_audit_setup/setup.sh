#!/bin/bash
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

#This script downloads Solr (optional) and sets up Solr for Ranger Audit Server
curr_dir=`pwd`
PROPFILE=$PWD/install.properties

if [ ! -f ${PROPFILE} ]
then
	echo "$PROPFILE file not found....!!";
	exit 1;
fi

get_prop(){
	validateProperty=$(sed '/^\#/d' $2 | grep "^$1\s*="  | tail -n 1) # for validation
	if  test -z "$validateProperty" ; then log "[E] '$1' not found in $2 file while getting....!!"; exit 1; fi
	value=$(echo $validateProperty | cut -d "=" -f2-)
	echo $value
}

SOLR_INSTALL_FOLDER=$(get_prop 'SOLR_INSTALL_FOLDER' $PROPFILE)
SOLR_RANGER_HOME=$(get_prop 'SOLR_RANGER_HOME' $PROPFILE)
SOLR_RANGER_PORT=$(get_prop 'SOLR_RANGER_PORT' $PROPFILE)
SOLR_DEPLOYMENT=$(get_prop 'SOLR_DEPLOYMENT' $PROPFILE)
SOLR_RANGER_DATA_FOLDER=$(get_prop 'SOLR_RANGER_DATA_FOLDER' $PROPFILE)
SOLR_ZK=$(get_prop 'SOLR_ZK' $PROPFILE)
SOLR_USER=$(get_prop 'SOLR_USER' $PROPFILE)
SOLR_GROUP=$(get_prop 'SOLR_GROUP' $PROPFILE)
SOLR_RANGER_COLLECTION=$(get_prop 'SOLR_RANGER_COLLECTION' $PROPFILE)
SOLR_INSTALL=$(get_prop 'SOLR_INSTALL' $PROPFILE)
SOLR_DOWNLOAD_URL=$(get_prop 'SOLR_DOWNLOAD_URL' $PROPFILE)
SOLR_LOG_FOLDER=$(get_prop 'SOLR_LOG_FOLDER' $PROPFILE)
MAX_AUDIT_RETENTION_DAYS=$(get_prop 'MAX_AUDIT_RETENTION_DAYS' $PROPFILE)
SOLR_MAX_MEM=$(get_prop 'SOLR_MAX_MEM' $PROPFILE)
SOLR_HOST_URL=$(get_prop 'SOLR_HOST_URL' $PROPFILE)
SOLR_SHARDS=$(get_prop 'SOLR_SHARDS' $PROPFILE)
SOLR_REPLICATION=$(get_prop 'SOLR_REPLICATION' $PROPFILE)
CONFIG_JAVA_HOME=$(get_prop 'JAVA_HOME' $PROPFILE)

JAVA_HOME=${CONFIG_JAVA_HOME:-$JAVA_HOME}

#Current timestamp
ts=$(date +"%m%d%y%H%M%S")

#Validate all variables
check_java_version() {
    #Check for JAVA_HOME
    if [ "${JAVA_HOME}" == "" ]; then
	echo "Error: JAVA_HOME environment property not defined, aborting installation."
	exit 1
    fi

    export JAVA_BIN=${JAVA_HOME}/bin/java

    if [ ! -x ${JAVA_BIN} ]; then
        echo "Error: '${JAVA_BIN}' command not found"
        exit 1;
    fi

    version=$("$JAVA_BIN" -version 2>&1 | awk -F '"' '/version/ {print $2}')
    major=`echo ${version} | cut -d. -f1`
    minor=`echo ${version} | cut -d. -f2`
    if [[ "${major}" == 1 && "${minor}" < 7 ]] ; then
	echo "Error: Java 1.7 or above is required, current java version is $version"
	exit 1;
    fi
}

#Check Java version. Minimum JDK 1.7 is needed
check_java_version

if [ "$SOLR_INSTALL_FOLDER" = "" ]; then
    echo "Error: SOLR_INSTALL_FOLDER not set"
    exit 1
fi

if [ "$SOLR_RANGER_HOME" = "" ]; then
    echo "Error: SOLR_RANGER_HOME not set"
    exit 1
fi

if [ "$SOLR_RANGER_PORT" = "" ]; then
    echo "Error: SOLR_RANGER_PORT not set"
    exit 1
fi

if [ "$SOLR_DEPLOYMENT" = "standalone" ]; then
    if [ "$SOLR_RANGER_DATA_FOLDER" = "" ]; then
	echo "Error: SOLR_RANGER_DATA_FOLDER not set"
	exit 1
    fi
else
    if [ "$SOLR_ZK" = "" ]; then
	echo "Error: SOLR_ZK not set"
	exit 1
    fi
fi

if [ "$SOLR_USER" = "" ]; then
    SOLR_USER=solr
fi

if [ "$SOLR_GROUP" = "" ]; then
    SOLR_GROUP=solr
fi

if [ "$SOLR_RANGER_COLLECTION" = "" ]; then
    SOLR_RANGER_COLLECTION=ranger_audits
fi

curr_user=`whoami`
is_root=0
if [ -w /etc/passwd ]; then
    is_root=1
fi

errorList=

if [ "$SOLR_INSTALL" = "true" -a $is_root -eq 0 ]; then
    echo "Error: Solr will be installed only if run as root."
    exit 1
fi

if [ "$SOLR_INSTALL" = "true" -a "$SOLR_DOWNLOAD_URL" = "" ]; then
    echo "Error: If SOLR_INSTALL=true, then SOLR_DOWNLOAD_URL can't be empty"
    exit 1
fi

if [ "$SOLR_LOG_FOLDER" = "logs" ]; then
    NEW_SOLR_LOG_FOLDER=$SOLR_INSTALL/server/${$SOLR_LOG_FOLDER}
    echo "`date`|INFO|Changing SOLR_LOG_FOLDER from $SOLR_LOG_FOLDER to $NEW_SOLR_LOG_FOLDER"
    SOLR_LOG_FOLDER=$NEW_SOLR_LOG_FOLDER
fi


function run_root_usage {
    echo "sudo chown -R $SOLR_USER:$SOLR_GROUP $SOLR_INSTALL_FOLDER"
    echo "sudo mkdir -p $SOLR_RANGER_HOME"
    echo "sudo chown -R $SOLR_USER:$SOLR_GROUP $SOLR_RANGER_HOME"
    if [ "$SOLR_LOG_FOLDER" != "logs" ]; then
	echo "sudo mkdir -p $SOLR_LOG_FOLDER"
	echo "sudo chown -R $SOLR_USER:$SOLR_GROUP $SOLR_LOG_FOLDER"
    fi
}

function set_ownership {
    user=$1
    group=$2
    folder=$3
    chown -R $user:$group $folder 
    if [ $is_root -eq 1 ]; then
	parent_folder=`dirname $folder`
	while [ "$parent_folder" != "/" ]; do
	    su - $SOLR_USER -c "ls $parent_folder" &> /dev/null
	    if [ $? -ne 0 ]; then
		err="ERROR: User $SOLR_USER doesn't have permission to read folder $parent_folder. Please make sure to give appropriate permissions, else $SOLR_USER won't be able to access $folder"
		echo $err
		errorList="$errorList\n$err"
	    fi
	    folder=$parent_folder
	    parent_folder=`dirname $folder`
	done
    fi
}

if [ $is_root -ne 1 ]; then
    if [ "$SOLR_USER" != "$curr_user" ]; then
        echo "`date`|ERROR|You need to run this script as root or as user $SOLR_USER"
        echo "If you need to run as $SOLR_USER, then first execute the following commands as root or sudo"
        egrep "^$SOLR_GROUP" /etc/group >& /dev/null
        if [ $? -ne 0 ]
        then
            echo "sudo groupadd $SOLR_GROUP"
        fi
        id $SOLR_USER &> /dev/null
        if [ $? -ne 0 ]; then
            echo "sudo useradd -g $SOLR_GROUP $SOLR_USER"
        fi
        run_root_usage
        exit 1
    fi

    #Let's make $curr_user has permission to write to $SOLR_RANGER_HOME and also chown
    mkdir -p $SOLR_RANGER_HOME 2> /dev/null
    if [ ! -d $SOLR_RANGER_HOME ]; then
	echo "`date`|ERROR|Solr Ranger Home folder <$SOLR_RANGER_HOME> could not be created. Current user is $curr_user"
	run_root_usage
	exit 1
    fi
    
    test_file=${SOLR_RANGER_HOME}/testfile_${ts}.txt
    touch $test_file 2> /dev/null
    if [ $? -ne 0 ]; then
	echo "`date`|ERROR|User $curr_user doesn't have permission to write to $SOLR_RANGER_HOME."
	run_root_usage
	exit 1
    fi
    
    chown $SOLR_USER:$SOLR_GROUP $test_file 2> /dev/null
    if [ $? -ne 0 ]; then
	echo "`date`|ERROR|User $curr_user doesn't have permission chown to $SOLR_USER:$SOLR_GROUP in $SOLR_RANGER_HOME"
	run_root_usage
	exit 1
    fi
    rm -f $test_file
    
    #Solr on first time startup, it creates the webapp folder. So the $SOLR_USER needs permission to create webapp
    test_file=$SOLR_INSTALL_FOLDER/testfile_${ts}.txt
    touch $test_file 2> /dev/null
    if [ $? -ne 0 ]; then
	echo "`date`|ERROR|User $curr_user doesn't have write permission to $SOLR_INSTALL_FOLDER"
	run_root_usage
	exit 1
    fi
    rm -f $test_file

    #Let's make $curr_user has permission to write to logs folder
    mkdir -p $SOLR_LOG_FOLDER 2> /dev/null
    if [ ! -d $SOLR_LOG_FOLDER ]; then
	echo "`date`|ERROR|Log folder <$SOLR_LOG_FOLDER> could not be created. Current user is $curr_user"
	run_root_usage
	exit 1
    fi

    test_file=$SOLR_LOG_FOLDER/testfile_${ts}.txt
    touch $test_file 2> /dev/null
    if [ $? -ne 0 ]; then
	echo "`date`|ERROR|User $curr_user doesn't have permission to write to log folder $SOLR_LOG_FOLDER"
	run_root_usage
	exit 1
    fi
    rm -f $test_file
fi


if [ -d $SOLR_RANGER_HOME ]; then
    #echo "`date`|WARN|Solr Ranger Home <$SOLR_RANGER_HOME> exists. Moving to ${SOLR_RANGER_HOME}.bk.${ts}"
    echo "`date`|INFO|Solr Ranger Home <$SOLR_RANGER_HOME> exists. Will overwrite configurations"
    #mv $SOLR_RANGER_HOME ${SOLR_RANGER_HOME}.bk.${ts}
fi


#Download and install Solr if needed
if [ "$SOLR_INSTALL" = "true" ]; then
    if [ ! -x `which wget 2> /dev/null` ]; then
	echo "Error: wget is not found in the path. Please install wget"
	exit
    fi

    if [ -d $SOLR_INSTALL_FOLDER ]; then
	echo "`date`|WARN|$SOLR_INSTALL_FOLDER exists. This script will overwrite some files"
    fi
    
    echo "`date`|INFO|Downloading solr from $SOLR_DOWNLOAD_URL"
    #Temporary create a folder to untar the folder
    tmp_folder=/tmp/solr_untar_${ts}
    mkdir -p ${tmp_folder}
    cd ${tmp_folder}
    wget $SOLR_DOWNLOAD_URL
    #Assuming this is a new folder and there will be only one file
    tgz_file=`ls *z`
    if [ ! -f $tgz_file ]; then
	echo "`date`|ERROR|Downloaded file <`pwd`/$tgz_file> not found"
	exit 1
    fi
    
    mkdir tmp
    tar xfz $tgz_file -C tmp
    cd tmp
    
    #Assuming there will only one folder begining with "s"
    solr_folder=`ls | grep "^solr"`
    
    if [ ! -d $solr_folder ]; then
	echo "`date`|ERROR|Solr temporary folder `pwd`/<$solr_folder> not found"
	exit 1
    fi

    if [ -d $SOLR_INSTALL_FOLDER ]; then
	echo "`date`|WARN|$SOLR_INSTALL_FOLDER exists. Moving to ${SOLR_INSTALL_FOLDER}.bk.${ts}"
	mv $SOLR_INSTALL_FOLDER ${SOLR_INSTALL_FOLDER}.bk.${ts}
    fi

    mv $solr_folder $SOLR_INSTALL_FOLDER
    rm -rf $tmp_folder
    echo "`date`|INFO|Installed Solr in $SOLR_INSTALL_FOLDER"
fi

if [ ! -d $SOLR_INSTALL_FOLDER ]; then
    echo "`date`|ERROR|$SOLR_INSTALL_FOLDER not found. Check \$SOLR_INSTALL_FOLDER"
    exit 1
fi

if [ ! -x $SOLR_INSTALL_FOLDER/bin/solr ]; then
    echo "`date`|ERROR|Solr doesn't seem to be installed properly. $SOLR_INSTALL_FOLDER/bin/solr doesn't exist. Please check $SOLR_INSTALL_FOLDER"
    exit 1
fi

########## At this point, we have the Solr installed folder ####

######### Copy the Solr config file for Ranger ######
cd $curr_dir

mkdir -p $SOLR_RANGER_HOME/resources
cp -r resources/* $SOLR_RANGER_HOME/resources

if [ "$SOLR_DEPLOYMENT" = "standalone" ]; then
    echo "`date`|INFO|Configuring standalone instance"
    echo "`date`|INFO|Copying Ranger Audit Server configuration to $SOLR_RANGER_HOME"
    cp -r solr_standalone/* $SOLR_RANGER_HOME
    mkdir -p $SOLR_RANGER_HOME/${SOLR_RANGER_COLLECTION}/conf
    cp -r conf/* $SOLR_RANGER_HOME/${SOLR_RANGER_COLLECTION}/conf

    sed  -e "s#{{MAX_AUDIT_RETENTION_DAYS}}#$MAX_AUDIT_RETENTION_DAYS#g" $SOLR_RANGER_HOME/${SOLR_RANGER_COLLECTION}/conf/solrconfig.xml.j2 > $SOLR_RANGER_HOME/${SOLR_RANGER_COLLECTION}/conf/solrconfig.xml
    sed  "s#{{RANGER_AUDITS_DATA_FOLDER}}#$SOLR_RANGER_DATA_FOLDER#g" $SOLR_RANGER_HOME/${SOLR_RANGER_COLLECTION}/core.properties.j2 > $SOLR_RANGER_HOME/${SOLR_RANGER_COLLECTION}/core.properties
    sed  -e "s#{{JAVA_HOME}}#$JAVA_HOME#g" -e "s#{{SOLR_USER}}#$SOLR_USER#g"  -e "s#{{SOLR_MAX_MEM}}#$SOLR_MAX_MEM#g" -e "s#{{SOLR_INSTALL_DIR}}#$SOLR_INSTALL_FOLDER#g" -e "s#{{SOLR_RANGER_HOME}}#$SOLR_RANGER_HOME#g" -e "s#{{SOLR_PORT}}#$SOLR_RANGER_PORT#g" -e "s#{{SOLR_LOG_FOLDER}}#$SOLR_LOG_FOLDER#g" $SOLR_RANGER_HOME/scripts/solr.in.sh.j2 > $SOLR_RANGER_HOME/scripts/solr.in.sh
    
else
    echo "`date`|INFO|Configuring SolrCloud instance"
    cp -r solr_cloud/* $SOLR_RANGER_HOME
    mkdir -p $SOLR_RANGER_HOME/conf
    cp -r conf/* $SOLR_RANGER_HOME/conf

    #Get the first ZooKeeper host:port/path
    #FIRST_SOLR_ZK=$(IFS="," ; set -- $SOLR_ZK ; echo $1)
    FIRST_SOLR_ZK=$SOLR_ZK

    sed  -e "s#{{MAX_AUDIT_RETENTION_DAYS}}#$MAX_AUDIT_RETENTION_DAYS#g" $SOLR_RANGER_HOME/conf/solrconfig.xml.j2 > $SOLR_RANGER_HOME/conf/solrconfig.xml

    sed  -e "s#{{JAVA_HOME}}#$JAVA_HOME#g" -e "s#{{SOLR_USER}}#$SOLR_USER#g"  -e "s#{{SOLR_MAX_MEM}}#$SOLR_MAX_MEM#g" -e "s#{{SOLR_INSTALL_DIR}}#$SOLR_INSTALL_FOLDER#g" -e "s#{{SOLR_RANGER_HOME}}#$SOLR_RANGER_HOME#g" -e "s#{{SOLR_PORT}}#$SOLR_RANGER_PORT#g" -e "s#{{SOLR_ZK}}#$SOLR_ZK#g" -e "s#{{SOLR_LOG_FOLDER}}#$SOLR_LOG_FOLDER#g" $SOLR_RANGER_HOME/scripts/solr.in.sh.j2 > $SOLR_RANGER_HOME/scripts/solr.in.sh

    sed -e "s#{{JAVA_HOME}}#$JAVA_HOME#g" -e "s#{{SOLR_USER}}#$SOLR_USER#g" -e "s#{{SOLR_INSTALL_DIR}}#$SOLR_INSTALL_FOLDER#g" -e "s#{{SOLR_RANGER_HOME}}#$SOLR_RANGER_HOME#g" -e "s#{{SOLR_ZK}}#$FIRST_SOLR_ZK#g" $SOLR_RANGER_HOME/scripts/add_ranger_audits_conf_to_zk.sh.j2 > $SOLR_RANGER_HOME/scripts/add_ranger_audits_conf_to_zk.sh
    sed -e "s#{{JAVA_HOME}}#$JAVA_HOME#g" -e "s#{{SOLR_INSTALL_DIR}}#$SOLR_INSTALL_FOLDER#g" -e "s#{{SOLR_ZK}}#$SOLR_ZK#g" -e "s#{{SOLR_HOST_URL}}#$SOLR_HOST_URL#g"  -e "s#{{SOLR_SHARDS}}#$SOLR_SHARDS#g"  -e "s#{{SOLR_REPLICATION}}#$SOLR_REPLICATION#g"  $SOLR_RANGER_HOME/scripts/create_ranger_audits_collection.sh.j2 > $SOLR_RANGER_HOME/scripts/create_ranger_audits_collection.sh
    sed -e "s#{{SOLR_PORT}}#$SOLR_RANGER_PORT#g" $SOLR_RANGER_HOME/solr.xml.j2 > $SOLR_RANGER_HOME/solr.xml

fi

#Common overrides
sed -e "s#{{JAVA_HOME}}#$JAVA_HOME#g" -e "s#{{SOLR_INSTALL_DIR}}#$SOLR_INSTALL_FOLDER#g" -e "s#{{SOLR_PORT}}#$SOLR_RANGER_PORT#g" -e "s#{{SOLR_USER}}#$SOLR_USER#g" -e "s#{{SOLR_LOG_FOLDER}}#$SOLR_LOG_FOLDER#g" -e "s#{{SOLR_RANGER_HOME}}#$SOLR_RANGER_HOME#g" $SOLR_RANGER_HOME/scripts/stop_solr.sh.j2 > $SOLR_RANGER_HOME/scripts/stop_solr.sh
sed  -e "s#{{SOLR_LOG_FOLDER}}#$SOLR_LOG_FOLDER#g" $SOLR_RANGER_HOME/resources/log4j.properties.j2 > $SOLR_RANGER_HOME/resources/log4j.properties
sed -e "s#{{JAVA_HOME}}#$JAVA_HOME#g" -e "s#{{SOLR_USER}}#$SOLR_USER#g" -e "s#{{SOLR_ZK}}#$SOLR_ZK#g" -e "s#{{SOLR_INSTALL_DIR}}#$SOLR_INSTALL_FOLDER#g" -e "s#{{SOLR_RANGER_HOME}}#$SOLR_RANGER_HOME#g" -e "s#{{SOLR_PORT}}#$SOLR_RANGER_PORT#g" $SOLR_RANGER_HOME/scripts/solr.sh.j2 > $SOLR_RANGER_HOME/scripts/solr.sh
sed  -e "s#{{SOLR_USER}}#$SOLR_USER#g" -e "s#{{SOLR_INSTALL_DIR}}#$SOLR_INSTALL_FOLDER#g" -e "s#{{SOLR_RANGER_HOME}}#$SOLR_RANGER_HOME#g" $SOLR_RANGER_HOME/scripts/start_solr.sh.j2 > $SOLR_RANGER_HOME/scripts/start_solr.sh

#Let's make all ownership is given to $SOLR_USER:$SOLR_GROUP
if [ $is_root -eq 1 ]; then
    #Let's see if $SOLR_GROUP exists,create group if not exists.
    egrep "^$SOLR_GROUP" /etc/group >& /dev/null
    if [ $? -ne 0 ]
    then
        echo "`date`|INFO|Creating group $SOLR_GROUP"
        groupadd ${SOLR_GROUP}
    fi

    #Let's see if $SOLR_USER exists,create user if not exists
    id $SOLR_USER &> /dev/null
    if [ $? -ne 0 ]; then
		echo "`date`|INFO|Creating user $SOLR_USER"
		useradd -g $SOLR_GROUP $SOLR_USER 2>/dev/null
    fi

    set_ownership $SOLR_USER $SOLR_GROUP $SOLR_INSTALL_FOLDER
	if [ ! -d $SOLR_RANGER_HOME ]; then
		mkdir -p $SOLR_RANGER_HOME
    fi
    set_ownership $SOLR_USER $SOLR_GROUP $SOLR_RANGER_HOME

	if [ ! -d $SOLR_LOG_FOLDER ]; then
		mkdir -p $SOLR_LOG_FOLDER
    fi
    set_ownership $SOLR_USER $SOLR_GROUP $SOLR_LOG_FOLDER

    if [ "$SOLR_DEPLOYMENT" = "standalone" ]; then
		if [ ! -d $SOLR_RANGER_DATA_FOLDER ]; then
			mkdir -p $SOLR_RANGER_DATA_FOLDER
		fi
		set_ownership $SOLR_USER $SOLR_GROUP $SOLR_RANGER_DATA_FOLDER
    fi
else
    set_ownership $SOLR_USER $SOLR_GROUP $SOLR_RANGER_HOME
fi
chmod a+x $SOLR_RANGER_HOME/scripts/*.sh

SOLR_INSTALL_NOTES=$SOLR_RANGER_HOME/install_notes.txt
echo "Solr installation notes for Ranger Audits." > $SOLR_INSTALL_NOTES

cat > $SOLR_INSTALL_NOTES <<EOF
Solr installation notes for Ranger Audits.

Note: Don't edit this file. It will be over written if you run $0 again.

EOF

if [ "$SOLR_DEPLOYMENT" = "standalone" ]; then
cat >> $SOLR_INSTALL_NOTES <<EOF
You have installed Solr in standalone mode.

Note: In production deployment, it is recommended to run in SolrCloud mode with at least 2 nodes and replication factor 2

EOF
else

cat >> $SOLR_INSTALL_NOTES <<EOF
You have installed Solr in SolrCloud mode. You will have to do additional steps to create the collections for Ranger Audit. See below for instructions:
EOF

    if [ "$SOLR_REPLICATION" = "1" ]; then
cat >> $SOLR_INSTALL_NOTES <<EOF

Note: In production deployment, it is recommended to run in SolrCloud mode with at least 2 nodes with replication factor 2
EOF
    fi

cat >> $SOLR_INSTALL_NOTES <<EOF

*** IMPORTANT ***
For configuring SolrCloud, you need to do the following:
EOF

if [ "$SOLR_REPLICATION" != "1" ]; then
    cat >> $SOLR_INSTALL_NOTES <<EOF
1. Copy the same install.properties on all solr nodes and sing $0 script install and configure Solr for Ranger Audits on all other nodes also (don't start it yet)
2. Execute $SOLR_RANGER_HOME/scripts/add_ranger_audits_conf_to_zk.sh (only once from any node)
3. Start Solr on all nodes: $SOLR_RANGER_HOME/scripts/start_solr.sh
4. Create Ranger Audit collection: $SOLR_RANGER_HOME/scripts/create_ranger_audits_collection.sh (only once from any node)

EOF
else 
    cat >> $SOLR_INSTALL_NOTES <<EOF
1. Add Ranger Audit config to ZooKeeper: $SOLR_RANGER_HOME/scripts/add_ranger_audits_conf_to_zk.sh
2. Start Solr: $SOLR_RANGER_HOME/scripts/start_solr.sh
3. Create Ranger Audit collection: $SOLR_RANGER_HOME/scripts/create_ranger_audits_collection.sh

EOF
    
fi
fi

cat >> $SOLR_INSTALL_NOTES <<EOF
Start and Stoping Solr:
EOF

if [ "$SOLR_USER" != "root" ]; then
    cat >> $SOLR_INSTALL_NOTES <<EOF
Login as user $SOLR_USER or root and the run the below commands to start or stop Solr:
EOF
else
    cat >> $SOLR_INSTALL_NOTES <<EOF
Login as root and the run the below commands to start or stop Solr:
EOF

fi

cat >> $SOLR_INSTALL_NOTES <<EOF

To start Solr run: $SOLR_RANGER_HOME/scripts/start_solr.sh
To stop Solr run: $SOLR_RANGER_HOME/scripts/stop_solr.sh

After starting Solr for RangerAudit, Solr will listen at $SOLR_RANGER_PORT. E.g http://`hostname -f`:$SOLR_RANGER_PORT

Configure Ranger to use the following URL http://`hostname -f`:$SOLR_RANGER_PORT/solr/${SOLR_RANGER_COLLECTION}

Solr HOME for Ranger Audit is $SOLR_RANGER_HOME

EOF

if [ "$SOLR_DEPLOYMENT" = "standalone" ]; then
cat >> $SOLR_INSTALL_NOTES <<EOF
DATA FOLDER: $SOLR_RANGER_DATA_FOLDER

Make sure you have enough disk space for index. In production, it is recommended to have at least 1TB free.
`df -h $SOLR_RANGER_DATA_FOLDER`
EOF
else
cat >> $SOLR_INSTALL_NOTES <<EOF
SOLR_REPLICATION: $SOLR_REPLICATION
SOLR_SHARDS: $SOLR_SHARDS
DATA FOLDERS: $SOLR_RANGER_HOME/ranger_audits_shard*

Make sure you have enough disk space for index. In production, it is recommended to have at least 1TB free.
`df -h $SOLR_RANGER_HOME`
EOF
fi

echo -e $errorList >> $SOLR_INSTALL_NOTES

echo "`date`|INFO|Done configuring Solr for Apache Ranger Audit"
echo "`date`|INFO|Solr HOME for Ranger Audit is $SOLR_RANGER_HOME"
if [ "$SOLR_DEPLOYMENT" = "standalone" ]; then
    echo "`date`|INFO|Data folder for Audit logs is $SOLR_RANGER_DATA_FOLDER"
fi
echo "`date`|INFO|To start Solr run $SOLR_RANGER_HOME/scripts/start_solr.sh"
echo "`date`|INFO|To stop Solr run $SOLR_RANGER_HOME/scripts/stop_solr.sh"
echo "`date`|INFO|After starting Solr for RangerAudit, it will listen at $SOLR_RANGER_PORT. E.g http://`hostname -f`:$SOLR_RANGER_PORT"
echo "`date`|INFO|Configure Ranger to use the following URL http://`hostname -f`:$SOLR_RANGER_PORT/solr/${SOLR_RANGER_COLLECTION}"
if [ "$SOLR_DEPLOYMENT" = "solrcloud" ]; then
    echo "`date`|INFO|Please refer to $SOLR_INSTALL_NOTES for instructions for setting up collections in SolrCloud"
fi
echo "`date`|INFO| ** NOTE: If Solr is Secured then solrclient JAAS configuration has to be added to Ranger Admin and Ranger Plugin properties"
echo "`date`|INFO| ** Refer documentation on how to configure Ranger for audit to Secure Solr"
echo "########## Done ###################"
echo "Created file $SOLR_INSTALL_NOTES with instructions to start and stop"
echo "###################################"

if [ "$errorList" != "" ]; then
    echo -e "$errorList"
fi
