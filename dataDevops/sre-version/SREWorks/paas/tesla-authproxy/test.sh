set -e
set -x
mvn clean package -DskipTests
pod=`kubectl --kubeconfig=/Users/jinghua.yjh/.kube/config.sreworks get pod | grep paas-authproxy | grep Running | awk '{print $1}'`
kubectl --kubeconfig=/Users/jinghua.yjh/.kube/config.sreworks exec -ti ${pod} -- rm -f /app/tesla-authproxy.jar
time kubectl --kubeconfig=/Users/jinghua.yjh/.kube/config.sreworks cp tesla-authproxy-start/target/tesla-authproxy.jar ${pod}:/app/
kubectl --kubeconfig=/Users/jinghua.yjh/.kube/config.sreworks exec -ti ${pod} -- /app/sbin/run.sh
