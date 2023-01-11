#!/bin/sh

usage ()
{
  echo "usage: $0 apply/delete"
  exit
}

TAG=$(cat version.txt)

if [ "$1" == "apply" ] || [ "$1" == "delete" ]; then
        if [ "$1" == "apply" ]; then
                make download-demo-files
                kind delete cluster && kind create cluster --image=kindest/node:v1.24.0
                kind load docker-image nginx:1.21
                kind load docker-image quay.io/brancz/kube-rbac-proxy:v0.13.1
                kind load docker-image edgehub/deviceshifu-http-http:$TAG
                kind load docker-image edgehub/shifu-controller:$TAG
                kind load docker-image edgehub/mockdevice-agv:$TAG
                kind load docker-image edgehub/mockdevice-plate-reader:$TAG
                kind load docker-image edgehub/mockdevice-robot-arm:$TAG
                kind load docker-image edgehub/mockdevice-thermometer:$TAG
                kubectl apply -f pkg/k8s/crd/install/shifu_install.yml
        else
                kind delete cluster
                docker rmi $(docker images | grep 'edgehub/mockdevice' | awk '{print $3}')
                docker rmi $(docker images | grep 'edgehub/deviceshifu-http-http' | awk '{print $3}')
                docker rmi $(docker images | grep 'edgehub/shifu-controller' | awk '{print $3}')
                docker rmi quay.io/brancz/kube-rbac-proxy:v0.13.1
                docker rmi $(docker images | grep 'kindest/node' | awk '{print $3}')
                docker rmi nginx:1.21
        fi
else
        echo "not a valid argument, need to be apply/delete"
        exit 0
fi
