#!bin/bash

default='{"mqtt_message":"","mqtt_receive_timestamp":"0001-01-01 00:00:00 +0000 UTC"}'

for i in {1..5} 
do
    kubectl exec -it mosquitto -n devices -- mosquitto_pub -h mosquitto-service -d -p 18830 -t /test/test -m "test2333" 
    out=$(kubectl exec -it -n deviceshifu nginx -- curl deviceshifu-mqtt/mqtt_data --connect-timeout 5)

    echo $out
    echo $default
    if [[ $out == "" ]]
    then 
        echo "empty reply"
    elif [[ $out == $default ]]
    then
        echo "euqal default message"
    else 
        exit 0
    fi
done
exit 1
