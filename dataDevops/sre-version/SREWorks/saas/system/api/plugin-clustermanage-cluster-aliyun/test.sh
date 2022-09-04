set -e
set -x
mvn clean package
pod=`kubectl get pod | grep dev-sreworks-plugin-p-cm-cluster-aliyun | awk '{print $1}'`
kubectl exec -ti ${pod} -- rm -f /app/plugin-clustermanage-cluster-aliyun.jar
time kubectl cp plugin-clustermanage-cluster-aliyun-start/target/plugin-clustermanage-cluster-aliyun.jar ${pod}:/app/
kubectl exec -ti ${pod} -- java -Xmx1g -Xms1g -XX:ActiveProcessorCount=2 -jar /app/plugin-clustermanage-cluster-aliyun.jar
