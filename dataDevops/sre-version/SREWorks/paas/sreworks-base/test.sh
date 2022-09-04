set -e
set -x
mvn clean package
pod=`kubectl --kubeconfig=/Users/jinghua.yjh/.kube/config.sreworks get pod | grep system-sreworks | awk '{print $1}'`
kubectl --kubeconfig=/Users/jinghua.yjh/.kube/config.sreworks exec -ti ${pod} -- rm -f /app/sreworks.jar
time kubectl --kubeconfig=/Users/jinghua.yjh/.kube/config.sreworks cp sreworks-start/target/sreworks.jar ${pod}:/app/
kubectl --kubeconfig=/Users/jinghua.yjh/.kube/config.sreworks exec -ti ${pod} -- java -Xmx1g -Xms1g -XX:ActiveProcessorCount=2 -jar /app/sreworks.jar
