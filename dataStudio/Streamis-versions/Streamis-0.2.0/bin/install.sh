#!/bin/sh
#Actively load user env
if [ -f "~/.bashrc" ];then
  echo "Warning! user bashrc file does not exist."
else
  source ~/.bashrc
fi

shellDir=`dirname $0`
workDir=`cd ${shellDir}/..;pwd`

SERVER_IP=""
SERVER_HOME=""

local_host="`hostname --fqdn`"
LOCAL_IP="`ifconfig | grep 'inet' | grep -v '127.0.0.1' | cut -d: -f2 | awk '{ print $2}'`"

#To be compatible with MacOS and Linux
txt=""
if [[ "$OSTYPE" == "darwin"* ]]; then
    txt="''"
elif [[ "$OSTYPE" == "linux-gnu" ]]; then
    txt=""
elif [[ "$OSTYPE" == "cygwin" ]]; then
    echo "streamis not support Windows operating system"
    exit 1
elif [[ "$OSTYPE" == "msys" ]]; then
    echo "streamis not support Windows operating system"
    exit 1
elif [[ "$OSTYPE" == "win32" ]]; then
    echo "streamis not support Windows operating system"
    exit 1
elif [[ "$OSTYPE" == "freebsd"* ]]; then
    txt=""
else
    echo "Operating system unknown, please tell us(submit issue) for better service"
    exit 1
fi

function isSuccess(){
if [ $? -ne 0 ]; then
    echo "Failed to " + $1
    exit 1
else
    echo "Succeed to" + $1
fi
}

function checkJava(){
	java -version
	isSuccess "execute java --version"
}


##install env:expect,
sudo yum install -y expect
isSuccess "install expect"

##install env:telnet,
sudo yum install -y telnet
isSuccess "install telnet"

##load config
echo "step1:load config"
source ${workDir}/conf/config.sh
source ${workDir}/conf/db.sh
isSuccess "load config"

local_host="`hostname --fqdn`"


##env check
echo "Do you want to clear Streamis table information in the database?"
echo " 1: Do not execute table-building statements"
echo " 2: Dangerous! Clear all data and rebuild the tables."
echo ""

MYSQL_INSTALL_MODE=1

read -p "Please input the choice:"  idx
if [[ '2' = "$idx" ]];then
  MYSQL_INSTALL_MODE=2
  echo "You chose Rebuild the table"
elif [[ '1' = "$idx" ]];then
  MYSQL_INSTALL_MODE=1
  echo "You chose not execute table-building statements"
else
  echo "no choice,exit!"
  exit 1
fi

##init db
if [[ '2' = "$MYSQL_INSTALL_MODE" ]];then
	mysql -h$MYSQL_HOST -P$MYSQL_PORT -u$MYSQL_USER -p$MYSQL_PASSWORD -D$MYSQL_DB --default-character-set=utf8 -e "source ${workDir}/db/streamis_ddl.sql"
	isSuccess "source streamis_ddl.sql"
	mysql -h$MYSQL_HOST -P$MYSQL_PORT -u$MYSQL_USER -p$MYSQL_PASSWORD -D$MYSQL_DB --default-character-set=utf8 -e "source ${workDir}/db/streamis_dml.sql"
	isSuccess "source streamis_dml.sql"
	echo "Rebuild the table"
fi


EUREKA_URL=http://$EUREKA_INSTALL_IP:$EUREKA_PORT/eureka/

##function start
function installPackage(){
echo "start to install $SERVERNAME"
echo "$SERVERNAME-step1: create dir"

if ! test -e $SERVER_HOME; then
  sudo mkdir -p $SERVER_HOME;sudo chown -R $deployUser:$deployUser $SERVER_HOME
  isSuccess "create the dir of  $SERVERNAME"
fi

echo "$SERVERNAME-step2:copy install package"
cp ${workDir}/share/$PACKAGE_DIR/$SERVERNAME.zip $SERVER_HOME
isSuccess "copy  ${SERVERNAME}.zip"
cd $SERVER_HOME/;rm -rf $SERVERNAME-bak; mv -f $SERVERNAME $SERVERNAME-bak
cd $SERVER_HOME/;unzip $SERVERNAME.zip > /dev/null
isSuccess "unzip  ${SERVERNAME}.zip"

echo "$SERVERNAME-step3:subsitution conf"
SERVER_CONF_PATH=$SERVER_HOME/$SERVERNAME/conf/application.yml
sed -i  "s#port:.*#port: $SERVER_PORT#g" $SERVER_CONF_PATH
sed -i  "s#defaultZone:.*#defaultZone: $EUREKA_URL#g" $SERVER_CONF_PATH
sed -i  "s#hostname:.*#hostname: $SERVER_IP#g" $SERVER_CONF_PATH
isSuccess "subsitution conf of $SERVERNAME"
}

function setDatasourcePassword(){
  PASSWORD=$MYSQL_PASSWORD
  temp=${PASSWORD//#/%tream%}
  sed -i  "s#wds.linkis.server.mybatis.datasource.password.*#wds.linkis.server.mybatis.datasource.password=$temp#g" $SERVER_CONF_PATH
  sed -i  "s/%tream%/#/g" $SERVER_CONF_PATH
}
##function end


##Streamis-Server Install
PACKAGE_DIR=streamis-server
SERVERNAME=streamis-server
SERVER_IP=$STREAMIS_SERVER_INSTALL_IP
SERVER_PORT=$STREAMIS_SERVER_INSTALL_PORT
SERVER_HOME=$STREAMIS_INSTALL_HOME
###install Streamis-Server
installPackage
###update Streamis-Server linkis.properties
echo "$SERVERNAME-step4:update linkis.properties"
SERVER_CONF_PATH=$SERVER_HOME/$SERVERNAME/conf/linkis.properties
sed -i  "s#wds.linkis.server.mybatis.datasource.url.*#wds.linkis.server.mybatis.datasource.url=jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DB}?characterEncoding=UTF-8#g" $SERVER_CONF_PATH
sed -i  "s#wds.linkis.server.mybatis.datasource.username.*#wds.linkis.server.mybatis.datasource.username=$MYSQL_USER#g" $SERVER_CONF_PATH
setDatasourcePassword
sed -i  "s#wds.linkis.gateway.ip.*#wds.linkis.gateway.ip=$GATEWAY_INSTALL_IP#g" $SERVER_CONF_PATH
sed -i  "s#wds.linkis.gateway.port.*#wds.linkis.gateway.port=$GATEWAY_PORT#g" $SERVER_CONF_PATH
sed -i  "s#wds.linkis.gateway.url.*#wds.linkis.gateway.url=http://${GATEWAY_INSTALL_IP}:${GATEWAY_PORT}#g" $SERVER_CONF_PATH
isSuccess "subsitution linkis.properties of $SERVERNAME"
echo "<----------------$SERVERNAME:end------------------->"
echo ""


