#!bin/bash

default='FreeOpcUa Python Server'

for i in {1..5} 
do
    out=$(kubectl exec -it -n deviceshifu nginx -- curl deviceshifu-opcua/get_server --connect-timeout 5)

    echo $out
    echo $default
    if [[ $out == "" ]]
    then 
        echo "empty reply"
    elif [[ $out == $default ]]
    then
        echo "equal"
        exit 0
    else 
        exit 1
    fi
done
exit 1
