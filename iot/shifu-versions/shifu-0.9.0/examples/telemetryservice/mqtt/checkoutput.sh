#!bin/bash

default='testData'
# testMQTT
for i in {1..30} 
do
    docker exec nginx curl localhost:9090/mqtt
    output=$(docker exec nginx curl localhost:17773/data)
    echo $output
    echo $default
    if [[ $output == $default ]]
    then
        exit 0
    fi
done
exit 1