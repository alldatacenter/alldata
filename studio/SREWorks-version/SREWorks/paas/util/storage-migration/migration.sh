set -x
set -e 

ROOT=$(cd `dirname $0`; pwd)

if [ "$FORCE" = "true" ];then
   bash ${ROOT}/dataops-mysql-migration.sh -f
else
   bash ${ROOT}/dataops-mysql-migration.sh
fi

bash ${ROOT}/core-mysql-migration.sh 

if [ "$FORCE" = "true" ];then
   bash ${ROOT}/dataops-es-migration.sh -f -l ${ES_BATCH_LIMIT}
else
   bash ${ROOT}/dataops-es-migration.sh -l ${ES_BATCH_LIMIT}
fi

