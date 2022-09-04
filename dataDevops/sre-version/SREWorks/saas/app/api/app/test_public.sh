set -e
set -x
mvn clean package
pod=`kubectl --kubeconfig=/Users/jinghua.yjh/.kube/config.sreworks-public get pod | grep app-app | awk '{print $1}'`
kubectl --kubeconfig=/Users/jinghua.yjh/.kube/config.sreworks-public exec -ti ${pod} -- rm -f /app/app.jar
time kubectl --kubeconfig=/Users/jinghua.yjh/.kube/config.sreworks-public cp app-start/target/app.jar ${pod}:/app/
kubectl --kubeconfig=/Users/jinghua.yjh/.kube/config.sreworks-public exec -ti ${pod} -- java -Xmx1g -Xms1g -XX:ActiveProcessorCount=2 -jar /app/app.jar
