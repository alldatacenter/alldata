#代码目录
code_path=$PWD
REGISTRY_PATH='registry.cn-hangzhou.aliyuncs.com/lilishop-ui'
BUILD_VERSION=4.2.4.1

read -p "请输入仓库地址": REGISTRY_PATH

echo $REGISTRY_PATH

docker build -t $REGISTRY_PATH/buyer-ui:$BUILD_VERSION ${code_path}/buyer
docker push $REGISTRY_PATH/buyer-ui:$BUILD_VERSION

docker build -t $REGISTRY_PATH/seller-ui:$BUILD_VERSION ${code_path}/seller
docker push $REGISTRY_PATH/seller-ui:$BUILD_VERSION

docker build -t $REGISTRY_PATH/manager-ui:$BUILD_VERSION ${code_path}/manager
docker push $REGISTRY_PATH/manager-ui:$BUILD_VERSION

