
set -e
set -x

# 获取当前路径
SELF=$(cd "$(dirname "$0")";pwd)

# 获取项目根路径
ROOT=${SELF}/../../


# 合并前后端build.yaml
envsubst < ${SELF}/api/build.yaml.tpl > ${SELF}/api/build.yaml
cat ${SELF}/ui/build.yaml > ${SELF}/build.yaml
echo "---" >>  ${SELF}/build.yaml
cat ${SELF}/api/build.yaml >> ${SELF}/build.yaml

rm ${SELF}/api/build.yaml

