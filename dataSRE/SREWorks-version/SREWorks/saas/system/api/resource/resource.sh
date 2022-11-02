set -x


    while true
    do
        curl prod-upload-filemanage
        if [[ "$?" == "0" ]]; then
           echo "upload-filemanage is ok"
           break
        else
           echo "wait upload-filemanage ready"
           sleep 5
        fi
    done

/app/mc alias set sw ${APPMANAGER_PACKAGE_ENDPOINT_PROTOCOL}${APPMANAGER_PACKAGE_ENDPOINT} ${APPMANAGER_PACKAGE_ACCESS_KEY} ${APPMANAGER_PACKAGE_SECRET_KEY}

/app/mc ls sw/
/app/mc mb -p sw/resource
/app/mc anonymous set download sw/resource

ls -l /app/infos/

ls /app/infos/|while read line
do
  name=$(echo $line|awk -F '.' '{print $1}')
  cd /app/backend-framework/$name;
  zip -r ../$name.zip ./*
  mv /app/backend-framework/$name.zip /app/framework-builds/$name.zip
  curl "http://prod-upload-filemanage/sreworksFile/get?name=${name}&category=opsscaffold" -s |grep $name
  if [ $? -ne 0 ]; then
    curl http://prod-upload-filemanage/sreworksFile/create -H 'Content-Type: application/json' -d $(cat /app/infos/$line)
  fi
done

/app/mc cp --recursive /app/framework-builds sw/resource/


ls /app/infos/|while read line
do
  name=$(echo $line|awk -F '.' '{print $1}')
  curl "http://prod-upload-filemanage/sreworksFile/get?name=${name}&category=opsscaffold" -s |grep $name
  if [ $? -ne 0 ]; then
    echo "${name} not exist"
    exit 1
  fi
done


