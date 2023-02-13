set -e
set -x
mvn clean package
pod=`kubectl --kubeconfig=/Users/jinghua.yjh/.kube/config.js get pod -n sreworks | grep team-team | awk '{print $1}'`
kubectl -n sreworks --kubeconfig=/Users/jinghua.yjh/.kube/config.js exec -ti ${pod} -- rm -f /app/teammanage.jar
time kubectl -n sreworks --kubeconfig=/Users/jinghua.yjh/.kube/config.js cp teammanage-start/target/teammanage.jar ${pod}:/app/
kubectl -n sreworks --kubeconfig=/Users/jinghua.yjh/.kube/config.js exec -ti ${pod} -- java -Xmx1g -Xms1g -XX:ActiveProcessorCount=2 -jar /app/teammanage.jar
