set -e
set -x
pod=`kubectl get pod -n sreworks | grep app-app | grep Running | awk '{print $1}'`
kubectl -n sreworks exec -ti ${pod} -- java -Xmx1g -Xms1g -XX:ActiveProcessorCount=2 -jar /app/app.jar
