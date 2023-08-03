#!bin/bash

default='FreeOpcUa Python Server'
writeData=54188

for i in {1..5}; do
    out=$(kubectl exec -it -n deviceshifu nginx -- curl deviceshifu-opcua/get_server --connect-timeout 5)

    echo $out
    echo $default
    if [[ $out == "" ]]; then
        echo "empty reply"
    elif [[ $out == $default ]]; then
        echo "equal"
        break
    else
        exit 1
    fi

    if [[ $i == 5 ]]; then
        echo "timeout"
        exit 1
    fi
done

kubectl exec -it -n deviceshifu nginx -- curl -X POST -d "{\"value\":${writeData}}" deviceshifu-opcua/writable_value

out=$(kubectl exec -it -n deviceshifu nginx -- curl deviceshifu-opcua/writable_value)
if [[ $out -ne $writeData ]]; then
    echo "write failed"
    exit 1
fi

echo "write success"
