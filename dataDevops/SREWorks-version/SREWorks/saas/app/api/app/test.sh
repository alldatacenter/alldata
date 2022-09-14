set -e
set -x
mvn clean package
pod=`kubectl get pod -n sreworks | grep app-app | grep Running | awk '{print $1}'`
kubectl -n sreworks exec -ti ${pod} -- rm -f /app/app.jar
time kubectl -n sreworks cp app-start/target/app.jar ${pod}:/app/
kubectl -n sreworks exec -ti ${pod} -- java -Xmx1g -Xms1g -XX:ActiveProcessorCount=2 -jar /app/app.jar
