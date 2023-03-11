#!/bin/sh

# note: Generate a key on the local computer ssh-keygen -t rsa
# and append the public key of the local computer to the remote machine /home/deploy/.ssh/authorized_keys

PACKAGE_NAME=flink-sql-lineage-1.0.0
REMOTE_IP=192.168.90.150

cd $(dirname "$0") || exit
cd .. || exit
PROJECT_DIR=$(pwd)

echo 'Start maven packaging'
mvn clean package -DskipTests -Pprod
echo 'Finish maven package'

echo 'Copy the deploy package to the server /opt/workspace'
scp "$PROJECT_DIR"/lineage-dist/dist/${PACKAGE_NAME}.tgz deploy@${REMOTE_IP}:/opt/workspace/

echo 'Stop the service on the server and delete old package'
ssh deploy@${REMOTE_IP} 'cd /opt/workspace/'${PACKAGE_NAME}'; sh sbin/stop.sh'
ssh deploy@${REMOTE_IP} 'rm -rf /opt/workspace/'${PACKAGE_NAME}''

echo 'Execute the decompression command on the server and restart the service'
ssh deploy@"${REMOTE_IP}" 'cd /opt/workspace; tar -xvzf '${PACKAGE_NAME}'.tgz; source ~/.bash_profile ; cd '${PACKAGE_NAME}'; sh sbin/start.sh'

printf "\n\nDeployment completed\n"
printf "It takes tens of seconds for the service to start, please wait a moment.\n\n"

printf "Swagger API: http://%s:8194/swagger-ui/index.html\n" "${REMOTE_IP}"
printf "Knife4j API: http://%s:8194/doc.html\n" "${REMOTE_IP}"
printf "Quick Catalog API: http://%s:8194/catalogs/1\n" "${REMOTE_IP}"
printf "Index Page: http://%s:8194/index.html\n" "${REMOTE_IP}"