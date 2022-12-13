#!/bin/sh
# eureka 注册中心
dataxEureka=./datax-eureka.jar
# config 配置中心
dataxConfig=./datax-config.jar
# gateway 网关
dataxGateway=./datax-gateway.jar
# oauth2 认证服务
dataxAuth=./eladmin-system.jar
# 系统服务
systemService=./system-service.jar
# 任务调度服务
quartzService=./quartz-service.jar
# 工作流服务
workflowService=./workflow-service.jar
# 元数据服务
metadataService=./data-metadata-service.jar
# 元数据 sql 控制台服务
metadataConsoleService=./data-metadata-service-console.jar
# 数据标准服务
standardService=./data-standard-service.jar
# 数据质量服务
qualityService=./data-quality-service.jar
# 数据集市数据集服务
marketService=./data-market-service.jar
# 数据集市接口映射服务
marketServiceMapping=./data-market-service-mapping.jar
# 数据集市数据集成服务
marketServiceIntegration=./data-market-service-integration.jar
# 主数据服务
masterdataService=./data-masterdata-service.jar
# 可视化服务
visualService=./data-visual-service.jar
case "$1" in
start)
    echo "--------dataxEureka 开始启动--------------"
    nohup java -jar -Xmx 500m $dataxEureka >/dev/null 2>&1 &
    dataxEurekaPid=`ps -ef|grep $dataxEureka |grep -v grep|awk '{print $2}'`
    until [ -n "$dataxEurekaPid" ]
      do
       dataxEurekaPid=`ps -ef|grep $dataxEureka |grep -v grep|awk '{print $2}'`
      done
    echo "dataxEurekaPid is $dataxEurekaPid"
    echo "--------dataxEureka 启动成功--------------"
    stop 10

    echo "--------dataxConfig 开始启动--------------"
    nohup java -jar -Xmx 500m $dataxConfig >/dev/null 2>&1 &
    dataxConfigPid=`ps -ef|grep $dataxConfig |grep -v grep|awk '{print $2}'`
    until [ -n "$dataxConfigPid" ]
      do
       dataxConfigPid=`ps -ef|grep $dataxConfig |grep -v grep|awk '{print $2}'`
      done
    echo "dataxConfigPid is $dataxConfigPid"
    echo "--------dataxConfig 启动成功--------------"
    stop 10

    echo "--------dataxGateway 开始启动--------------"
    nohup java -jar -Xmx 500m $dataxGateway >/dev/null 2>&1 &
    dataxGatewayPid=`ps -ef|grep $dataxGateway |grep -v grep|awk '{print $2}'`
    until [ -n "$dataxConfigPid" ]
      do
       dataxGatewayPid=`ps -ef|grep $dataxGateway |grep -v grep|awk '{print $2}'`
      done
    echo "dataxGatewayPid is $dataxGatewayPid"
    echo "--------dataxGateway 启动成功--------------"
    stop 10

    echo "--------dataxAuth 开始启动--------------"
    nohup java -jar -Xmx 500m $dataxAuth >/dev/null 2>&1 &
    dataxAuthPid=`ps -ef|grep $dataxAuth |grep -v grep|awk '{print $2}'`
    until [ -n "$dataxAuthPid" ]
      do
       dataxAuthPid=`ps -ef|grep $dataxAuth |grep -v grep|awk '{print $2}'`
      done
    echo "dataxAuthPid is $dataxAuthPid"
    echo "--------dataxAuth 启动成功--------------"

    echo "--------systemService 开始启动--------------"
    nohup java -jar -Xmx 500m $systemService >/dev/null 2>&1 &
    systemServicePid=`ps -ef|grep $systemService |grep -v grep|awk '{print $2}'`
    until [ -n "$systemServicePid" ]
      do
       systemServicePid=`ps -ef|grep $systemService |grep -v grep|awk '{print $2}'`
      done
    echo "systemServicePid is $systemServicePid"
    echo "--------systemService 启动成功--------------"

    echo "--------quartzService 开始启动--------------"
    nohup java -jar -Xmx 500m $quartzService >/dev/null 2>&1 &
    quartzServicePid=`ps -ef|grep $quartzService |grep -v grep|awk '{print $2}'`
    until [ -n "$quartzServicePid" ]
      do
       quartzServicePid=`ps -ef|grep $quartzService |grep -v grep|awk '{print $2}'`
      done
    echo "quartzServicePid is $quartzServicePid"
    echo "--------quartzService 启动成功--------------"

    echo "--------workflowService 开始启动--------------"
    nohup java -jar -Xmx 500m $workflowService >/dev/null 2>&1 &
    workflowServicePid=`ps -ef|grep $workflowService |grep -v grep|awk '{print $2}'`
    until [ -n "$workflowServicePid" ]
      do
       workflowServicePid=`ps -ef|grep $workflowService |grep -v grep|awk '{print $2}'`
      done
    echo "workflowServicePid is $workflowServicePid"
    echo "--------workflowService 启动成功--------------"

    echo "--------metadataService 开始启动--------------"
    nohup java -jar -Xmx 500m $metadataService >/dev/null 2>&1 &
    metadataServicePid=`ps -ef|grep $metadataService |grep -v grep|awk '{print $2}'`
    until [ -n "$metadataServicePid" ]
      do
       metadataServicePid=`ps -ef|grep $metadataService |grep -v grep|awk '{print $2}'`
      done
    echo "metadataServicePid is $metadataServicePid"
    echo "--------metadataService 启动成功--------------"

    echo "--------metadataConsoleService 开始启动--------------"
    nohup java -jar -Xmx 500m $metadataConsoleService >/dev/null 2>&1 &
    metadataConsoleServicePid=`ps -ef|grep $metadataConsoleService |grep -v grep|awk '{print $2}'`
    until [ -n "$metadataConsoleServicePid" ]
      do
       metadataConsoleServicePid=`ps -ef|grep $metadataConsoleService |grep -v grep|awk '{print $2}'`
      done
    echo "metadataConsoleServicePid is $metadataConsoleServicePid"
    echo "--------metadataConsoleService 启动成功--------------"

    echo "--------standardService 开始启动--------------"
    nohup java -jar -Xmx 500m $standardService >/dev/null 2>&1 &
    standardServicePid=`ps -ef|grep $standardService |grep -v grep|awk '{print $2}'`
    until [ -n "$standardServicePid" ]
      do
       standardServicePid=`ps -ef|grep $standardService |grep -v grep|awk '{print $2}'`
      done
    echo "standardServicePid is $standardServicePid"
    echo "--------standardService 启动成功--------------"

    echo "--------qualityService 开始启动--------------"
    nohup java -jar -Xmx 500m $qualityService >/dev/null 2>&1 &
    qualityServicePid=`ps -ef|grep $qualityService |grep -v grep|awk '{print $2}'`
    until [ -n "$qualityServicePid" ]
      do
       qualityServicePid=`ps -ef|grep $qualityService |grep -v grep|awk '{print $2}'`
      done
    echo "qualityServicePid is $qualityServicePid"
    echo "--------qualityService 启动成功--------------"

    echo "--------marketService 开始启动--------------"
    nohup java -jar -Xmx 500m $marketService >/dev/null 2>&1 &
    marketServicePid=`ps -ef|grep $marketService |grep -v grep|awk '{print $2}'`
    until [ -n "$marketServicePid" ]
      do
       marketServicePid=`ps -ef|grep $marketService |grep -v grep|awk '{print $2}'`
      done
    echo "marketServicePid is $marketServicePid"
    echo "--------marketService 启动成功--------------"
	sleep 10

    echo "--------marketServiceMapping 开始启动--------------"
    nohup java -jar -Xmx 500m $marketServiceMapping >/dev/null 2>&1 &
    marketServiceMappingPid=`ps -ef|grep $marketServiceMapping |grep -v grep|awk '{print $2}'`
    until [ -n "$marketServiceMappingPid" ]
      do
       marketServiceMappingPid=`ps -ef|grep $marketServiceMapping |grep -v grep|awk '{print $2}'`
      done
    echo "marketServiceMappingPid is $marketServiceMappingPid"
    echo "--------marketServiceMapping 启动成功--------------"

    echo "--------marketServiceIntegration 开始启动--------------"
    nohup java -jar -Xmx 500m $marketServiceIntegration >/dev/null 2>&1 &
    marketServiceIntegrationPid=`ps -ef|grep $marketServiceIntegration |grep -v grep|awk '{print $2}'`
    until [ -n "$marketServiceIntegrationPid" ]
      do
       marketServiceIntegrationPid=`ps -ef|grep $marketServiceIntegration |grep -v grep|awk '{print $2}'`
      done
    echo "marketServiceIntegrationPid is $marketServiceIntegrationPid"
    echo "--------marketServiceIntegration 启动成功--------------"

    echo "--------masterdataService 开始启动--------------"
    nohup java -jar -Xmx 500m $masterdataService >/dev/null 2>&1 &
    masterdataServicePid=`ps -ef|grep $masterdataService |grep -v grep|awk '{print $2}'`
    until [ -n "$masterdataServicePid" ]
      do
       masterdataServicePid=`ps -ef|grep $masterdataService |grep -v grep|awk '{print $2}'`
      done
    echo "masterdataServicePid is $masterdataServicePid"
    echo "--------masterdataService 启动成功--------------"

    echo "--------visualService 开始启动--------------"
    nohup java -jar -Xmx 500m $visualService >/dev/null 2>&1 &
    visualServicePid=`ps -ef|grep $visualService |grep -v grep|awk '{print $2}'`
    until [ -n "$visualServicePid" ]
      do
       visualServicePid=`ps -ef|grep $visualService |grep -v grep|awk '{print $2}'`
      done
    echo "visualServicePid is $visualServicePid"
    echo "--------visualService 启动成功--------------"
;;
stop)
    P_ID=`ps -ef|grep $dataxEureka |grep -v grep|awk '{print $2}'`
    if [ "$P_ID" == "" ]; then
      echo "===dataxEureka process not exists or stop success"
    else
      kill -9 $P_ID
      echo "dataxEureka killed success"
    fi

    P_ID=`ps -ef|grep $dataxConfig |grep -v grep|awk '{print $2}'`
    if [ "$P_ID" == "" ]; then
      echo "===dataxConfig process not exists or stop success"
    else
      kill -9 $P_ID
      echo "dataxConfig killed success"
    fi

    P_ID=`ps -ef|grep $dataxGateway |grep -v grep|awk '{print $2}'`
    if [ "$P_ID" == "" ]; then
      echo "===dataxGateway process not exists or stop success"
    else
      kill -9 $P_ID
      echo "dataxGateway killed success"
    fi

    P_ID=`ps -ef|grep $dataxAuth |grep -v grep|awk '{print $2}'`
    if [ "$P_ID" == "" ]; then
      echo "===dataxAuth process not exists or stop success"
    else
      kill -9 $P_ID
      echo "dataxAuth killed success"
    fi

    P_ID=`ps -ef|grep $systemService |grep -v grep|awk '{print $2}'`
    if [ "$P_ID" == "" ]; then
      echo "===systemService process not exists or stop success"
    else
      kill -9 $P_ID
      echo "systemService killed success"
    fi

    P_ID=`ps -ef|grep $quartzService |grep -v grep|awk '{print $2}'`
    if [ "$P_ID" == "" ]; then
      echo "===quartzService process not exists or stop success"
    else
      kill -9 $P_ID
      echo "quartzService killed success"
    fi

    P_ID=`ps -ef|grep $workflowService |grep -v grep|awk '{print $2}'`
    if [ "$P_ID" == "" ]; then
      echo "===workflowService process not exists or stop success"
    else
      kill -9 $P_ID
      echo "workflowService killed success"
    fi

    P_ID=`ps -ef|grep $metadataService |grep -v grep|awk '{print $2}'`
    if [ "$P_ID" == "" ]; then
      echo "===metadataService process not exists or stop success"
    else
      kill -9 $P_ID
      echo "metadataService killed success"
    fi

    P_ID=`ps -ef|grep $metadataConsoleService |grep -v grep|awk '{print $2}'`
    if [ "$P_ID" == "" ]; then
      echo "===metadataConsoleService process not exists or stop success"
    else
      kill -9 $P_ID
      echo "metadataConsoleService killed success"
    fi

    P_ID=`ps -ef|grep $standardService |grep -v grep|awk '{print $2}'`
    if [ "$P_ID" == "" ]; then
      echo "===standardService process not exists or stop success"
    else
      kill -9 $P_ID
      echo "standardService killed success"
    fi

    P_ID=`ps -ef|grep $qualityService |grep -v grep|awk '{print $2}'`
    if [ "$P_ID" == "" ]; then
      echo "===qualityService process not exists or stop success"
    else
      kill -9 $P_ID
      echo "qualityService killed success"
    fi

    P_ID=`ps -ef|grep $marketService |grep -v grep|awk '{print $2}'`
    if [ "$P_ID" == "" ]; then
      echo "===marketService process not exists or stop success"
    else
      kill -9 $P_ID
      echo "marketService killed success"
    fi

    P_ID=`ps -ef|grep $marketServiceMapping |grep -v grep|awk '{print $2}'`
    if [ "$P_ID" == "" ]; then
      echo "===marketServiceMapping process not exists or stop success"
    else
      kill -9 $P_ID
      echo "marketServiceMapping killed success"
    fi

    P_ID=`ps -ef|grep $marketServiceIntegration |grep -v grep|awk '{print $2}'`
    if [ "$P_ID" == "" ]; then
      echo "===marketServiceIntegration process not exists or stop success"
    else
      kill -9 $P_ID
      echo "marketServiceIntegration killed success"
    fi

    P_ID=`ps -ef|grep $masterdataService |grep -v grep|awk '{print $2}'`
    if [ "$P_ID" == "" ]; then
      echo "===masterdataService process not exists or stop success"
    else
      kill -9 $P_ID
      echo "masterdataService killed success"
    fi

    P_ID=`ps -ef|grep $visualService |grep -v grep|awk '{print $2}'`
    if [ "$P_ID" == "" ]; then
      echo "===visualService process not exists or stop success"
    else
      kill -9 $P_ID
      echo "visualService killed success"
    fi
;;
restart)
    $0 stop
    sleep 10
    $0 start
    echo "===restart success==="
;;
esac
exit 0
