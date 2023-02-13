set -e
set -x
mvn clean package
pod=`kubectl get pod | grep resource-aliyun-rds | awk '{print $1}'`
kubectl exec -ti ${pod} -- rm -f /app/plugin-clustermanage-resource-aliyun-rds.jar
time kubectl cp plugin-clustermanage-resource-aliyun-rds-start/target/plugin-clustermanage-resource-aliyun-rds.jar ${pod}:/app/
kubectl exec -ti ${pod} -- java -Xmx1g -Xms1g -XX:ActiveProcessorCount=2 -jar /app/plugin-clustermanage-resource-aliyun-rds.jar
