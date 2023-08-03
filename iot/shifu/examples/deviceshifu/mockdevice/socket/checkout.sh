#!bin/bash

default='{"message":"1234567890","status":200}' 

for i in {1..5} 
do
    out=$(kubectl exec -it -n deviceshifu nginx -- curl -XPOST -H "Content-Type:application/json" deviceshifu-socket.deviceshifu.svc.cluster.local/cmd -d '{"command":"123"}'  --connect-timeout 5)
    out=${out:13-1:3}
    default=${default:13-1:3}
    echo $out
    echo $default 
    if [[ $out == "" ]]
    then 
        echo "empty reply"
    elif [[ $out == $default ]]
    then
        echo "equal with default message~"
        exit 0
    else 
        echo "not match"
        exit 1
    fi
done
exit 1
