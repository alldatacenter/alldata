set -e
set -x
mvn clean package
pod=`kubectl --kubeconfig=/Users/jinghua.yjh/.kube/config.sreworks-public get pod | grep filemanage-filemanage | awk '{print $1}'`
kubectl --kubeconfig=/Users/jinghua.yjh/.kube/config.sreworks-public exec -ti ${pod} -- rm -f /app/filemanage.jar
time kubectl --kubeconfig=/Users/jinghua.yjh/.kube/config.sreworks-public cp filemanage-start/target/filemanage.jar ${pod}:/app/
kubectl --kubeconfig=/Users/jinghua.yjh/.kube/config.sreworks-public exec -ti ${pod} -- java -Xmx1g -Xms1g -XX:ActiveProcessorCount=2 -jar /app/filemanage.jar
