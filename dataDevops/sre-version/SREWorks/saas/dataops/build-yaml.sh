
set -e
set -x

# 获取当前路径
SELF=$(cd "$(dirname "$0")";pwd)

# 获取项目根路径
ROOT=${SELF}/../../


echo "" > ${SELF}/tmp-merge-build.yaml

envsubst < ${SELF}/api/mysql/build.yaml.tpl > ${SELF}/tmp-build.yaml
cat ${SELF}/tmp-build.yaml >> ${SELF}/tmp-merge-build.yaml
echo "---" >> ${SELF}/tmp-merge-build.yaml

envsubst < ${SELF}/api/elasticsearch/build.yaml.tpl > ${SELF}/tmp-build.yaml
cat ${SELF}/tmp-build.yaml >> ${SELF}/tmp-merge-build.yaml
echo "---" >> ${SELF}/tmp-merge-build.yaml

envsubst < ${SELF}/api/grafana/build.yaml.tpl > ${SELF}/tmp-build.yaml
cat ${SELF}/tmp-build.yaml >> ${SELF}/tmp-merge-build.yaml
echo "---" >> ${SELF}/tmp-merge-build.yaml

envsubst < ${SELF}/api/kibana/build.yaml.tpl > ${SELF}/tmp-build.yaml
cat ${SELF}/tmp-build.yaml >> ${SELF}/tmp-merge-build.yaml
echo "---" >> ${SELF}/tmp-merge-build.yaml

envsubst < ${SELF}/api/metricbeat/build.yaml.tpl > ${SELF}/tmp-build.yaml
cat ${SELF}/tmp-build.yaml >> ${SELF}/tmp-merge-build.yaml
echo "---" >> ${SELF}/tmp-merge-build.yaml

envsubst < ${SELF}/api/filebeat/build.yaml.tpl > ${SELF}/tmp-build.yaml
cat ${SELF}/tmp-build.yaml >> ${SELF}/tmp-merge-build.yaml
echo "---" >> ${SELF}/tmp-merge-build.yaml

envsubst < ${SELF}/api/skywalking/build.yaml.tpl > ${SELF}/tmp-build.yaml
cat ${SELF}/tmp-build.yaml >> ${SELF}/tmp-merge-build.yaml
echo "---" >> ${SELF}/tmp-merge-build.yaml

#envsubst < ${SELF}/api/mongodb/build.yaml.tpl > ${SELF}/tmp-build.yaml
#cat ${SELF}/tmp-build.yaml >> ${SELF}/tmp-merge-build.yaml
#echo "---" >> ${SELF}/tmp-merge-build.yaml

envsubst < ${SELF}/api/ververica-platform/build.yaml.tpl > ${SELF}/tmp-build.yaml
cat ${SELF}/tmp-build.yaml >> ${SELF}/tmp-merge-build.yaml
echo "---" >> ${SELF}/tmp-merge-build.yaml

envsubst < ${SELF}/api/logstash/build.yaml.tpl > ${SELF}/tmp-build.yaml
cat ${SELF}/tmp-build.yaml >> ${SELF}/tmp-merge-build.yaml
echo "---" >> ${SELF}/tmp-merge-build.yaml

envsubst < ${SELF}/api/build.yaml.tpl > ${SELF}/tmp-build.yaml
cat ${SELF}/tmp-build.yaml >> ${SELF}/tmp-merge-build.yaml
echo "---" >> ${SELF}/tmp-merge-build.yaml
 

cat ${SELF}/tmp-merge-build.yaml | python -c 'import sys;print("\n---\n".join([raw.strip() for raw in sys.stdin.read().strip().split("---") if raw.strip()]))' > ${SELF}/merge-build.yaml

# 合并前后端build.yaml
cat ${SELF}/ui/data/build.yaml > ${SELF}/build.yaml
echo "---" >>  ${SELF}/build.yaml
cat ${SELF}/merge-build.yaml >> ${SELF}/build.yaml

rm ${SELF}/merge-build.yaml
rm ${SELF}/tmp-build.yaml
rm ${SELF}/tmp-merge-build.yaml

