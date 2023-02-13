set -e
set -x
mvn clean package
pod=`kubectl get pod | grep dev-sreworks-plugin-p-cm-resource-aliyun-redis | awk '{print $1}'`
kubectl exec -ti ${pod} -- rm -f /app/plugin-clustermanage-resource-aliyun-redis.jar
time kubectl cp plugin-clustermanage-resource-aliyun-redis-start/target/plugin-clustermanage-resource-aliyun-redis.jar ${pod}:/app/
kubectl exec -ti ${pod} -- java -Xmx1g -Xms1g -XX:ActiveProcessorCount=2 -jar /app/plugin-clustermanage-resource-aliyun-redis.jar
