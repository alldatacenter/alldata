set -e
set -x
mvn clean package
pod=`kubectl get pod | grep dev-sreworks-plugin-p-tm-account-aliyun | awk '{print $1}'`
kubectl exec -ti ${pod} -- rm -f /app/plugin-teammanage-account-aliyun.jar
time kubectl cp plugin-teammanage-account-aliyun-start/target/plugin-teammanage-account-aliyun.jar ${pod}:/app/
kubectl exec -ti ${pod} -- java -Xmx1g -Xms1g -XX:ActiveProcessorCount=2 -jar /app/plugin-teammanage-account-aliyun.jar
