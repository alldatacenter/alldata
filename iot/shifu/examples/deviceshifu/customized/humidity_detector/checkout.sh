#!bin/bash

cleaned_raw_data="$(cat cleaned_raw_data)"
raw_data="$(cat raw_data)"

sleep 5

for i in {1..5}
do
    humidity_output=$(kubectl exec -it -n deviceshifu nginx -- curl -XPOST -H "Content-Type:application/json" -s deviceshifu-humidity-detector-service.deviceshifu.svc.cluster.local:80/humidity --connect-timeout 5)
    humidity_check="$(diff <(echo "$humidity_output") <(echo "$raw_data") -b)"
    if [[ $humidity_output == "" ]]
    then
        echo "empty humidity reply"
        exit 1
    elif [[ $humidity_check == "" ]]
    then
        echo "equal humidity reply"
    else
        echo "wrong humidity reply"
        echo "$humidity_check"
        exit 1
    fi
    # humidity_custom return the cleaned data
    humidity_custom_output=$(kubectl exec -it -n deviceshifu nginx -- curl -XPOST -H "Content-Type:application/json" -s deviceshifu-humidity-detector-service.deviceshifu.svc.cluster.local:80/humidity_custom --connect-timeout 5)
    humidity_custom_check="$(diff <(echo "$humidity_custom_output") <(echo "$cleaned_raw_data") -b)"
    if [[ $humidity_custom_output == "" ]]
    then
        echo "empty humidity_custom reply"
        exit 1
    elif [[ $humidity_custom_check == "" ]]
    then
        echo "equal humidity_custom reply"
    else
        echo "wrong humidity_custom reply"
        echo "$humidity_custom_check"
        exit 1
    fi
    # check the telemetryservice get the cleaned data
    telemetryservice_custom_output=$(kubectl exec -it -n deviceshifu nginx -- curl -XPOST -H "Content-Type:application/json" -s mockserver.devices.svc.cluster.local:11111/custom_data/read --connect-timeout 5)
    telemetryservice_custom_check="$(diff <(echo "$telemetryservice_custom_output") <(echo "$cleaned_raw_data") -b)"
    if [[ $telemetryservice_custom_output == "" ]]
    then
        echo "empty telemetryservice_custom_output reply"
        exit 1
    elif [[ $telemetryservice_custom_check == "" ]]
    then
        echo "equal telemetryservice_custom_output reply"
    else
        echo "wrong telemetryservice_custom_output reply"
        echo "$telemetryservice_custom_check"
        exit 1
    fi
    # check telemetryservice get the rawdata
    telemetryservice_output=$(kubectl exec -it -n deviceshifu nginx -- curl -XPOST -H "Content-Type:application/json" -s mockserver.devices.svc.cluster.local:11111/data/read --connect-timeout 5)
    telemetryservice_check="$(diff <(echo "$telemetryservice_output") <(echo "$raw_data") -b)"
    if [[ $telemetryservice_output == "" ]]
    then
        echo "empty telemetryservice_output reply"
        exit 1
    elif [[ $telemetryservice_check == "" ]]
    then
        echo "equal telemetryservice_output reply"
    else
        echo "wrong telemetryservice_output reply"
        echo "$telemetryservice_check"
        exit 1
    fi
done
exit 0
