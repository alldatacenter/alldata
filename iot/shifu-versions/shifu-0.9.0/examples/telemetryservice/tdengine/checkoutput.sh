#!bin/bash
tdengineOutput=2
for i in {1..30}
do
    output=$(docker exec tdengine taos -s "Show databases;" | grep 'failed' | wc -l)
    echo $output
    if [[ $output -eq 0 ]]
    then
        break
    elif [[ $i -eq 30 ]]
    then
        exit 1
    fi
done
# init TDengine Table
docker exec tdengine taos -f /root/init.sql

for i in {1..30}
do
    docker exec nginx curl localhost:9090/sql
    output=$(docker exec tdengine taos -s "Select rawdata from shifu.testsubtable where rawdata='testData' limit 10;" | grep 'testData' | wc -l)
    echo $output
    if [[ $output -ge $tdengineOutput ]]
    then
        exit 0
    fi
done
exit 1
