set -e
set -x
mvn clean package
pod=`kubectl -n sreworks get pod | grep cluster-clustermanage | awk '{print $1}'`
kubectl -n sreworks exec -ti ${pod} -- rm -f /app/clustermanage.jar
time kubectl -n sreworks cp clustermanage-start/target/clustermanage.jar ${pod}:/app/
kubectl -n sreworks exec -ti ${pod} -- java -Xmx1g -Xms1g -XX:ActiveProcessorCount=2 -jar /app/clustermanage.jar
