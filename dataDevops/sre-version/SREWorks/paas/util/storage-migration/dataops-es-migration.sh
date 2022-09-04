#!/bin/sh

set -x
set -e

force="false"
limit=100
while getopts 'fl:' OPT; do
    case $OPT in
        f) force="true";;
        l) limit="$OPTARG";;
        ?) ;;
    esac
done

echo "==========migrate policy&template, starting...=========="
elasticdump --input=http://${DATA_ES_USER}:${DATA_ES_PASSWORD}@${DATA_ES_HOST}:${DATA_ES_PORT} --output=http://${DATA_DEST_ES_USER}:${DATA_DEST_ES_PASSWORD}@${DATA_DEST_ES_HOST}:${DATA_DEST_ES_PORT} --type=policy
elasticdump --input=http://${DATA_ES_USER}:${DATA_ES_PASSWORD}@${DATA_ES_HOST}:${DATA_ES_PORT} --output=http://${DATA_DEST_ES_USER}:${DATA_DEST_ES_PASSWORD}@${DATA_DEST_ES_HOST}:${DATA_DEST_ES_PORT} --type=template
echo "==========migrate policy&template, end=========="

indices=$(curl -XGET -u ${DATA_ES_USER}:${DATA_ES_PASSWORD} http://${DATA_ES_HOST}:${DATA_ES_PORT}/_cat/indices?format=JSON)
echo $indices | jq -c .[].index | while read str_index; do
  index=${str_index: 1: ${#str_index} - 2}
  if [[ $index == .* ]]; then
    echo "==========build-in index:"$index", skip...=========="
  else
    if [[ $force == "true" ]]; then
      index_exist_code=$(curl -IL -u ${DATA_DEST_ES_USER}:${DATA_DEST_ES_PASSWORD} http://${DATA_DEST_ES_HOST}:${DATA_DEST_ES_PORT}/$index?pretty| head -n 1 | cut -d$' ' -f2)
      if [[ $index_exist_code == 200 ]]; then
        echo "==========flush index:"$index", starting...=========="
        curl -XDELETE -u ${DATA_DEST_ES_USER}:${DATA_DEST_ES_PASSWORD} http://${DATA_DEST_ES_HOST}:${DATA_DEST_ES_PORT}/$index
        echo "==========flush index:"$index", end=========="
      fi
    fi
    echo "==========migrate index:"$index", starting...=========="

#    multielasticdump --direction=dump --match="'${index}'" --input=http://${DATA_ES_USER}:${DATA_ES_PASSWORD}@${DATA_ES_HOST}:${DATA_ES_PORT}  --ignoreType='mapping,data' --includeType='analyzer,alias,settings,template' --output=/tmp/es_backup
#    multielasticdump --direction=load --match="'${index}'" --input=/tmp/es_backup  --ignoreType='mapping,data' --includeType='analyzer,alias,settings,template' --output=http://${DATA_DEST_ES_USER}:${DATA_DEST_ES_PASSWORD}@${DATA_DEST_ES_HOST}:${DATA_DEST_ES_PORT}
#
    elasticdump --input=http://${DATA_ES_USER}:${DATA_ES_PASSWORD}@${DATA_ES_HOST}:${DATA_ES_PORT}/$index --output=http://${DATA_DEST_ES_USER}:${DATA_DEST_ES_PASSWORD}@${DATA_DEST_ES_HOST}:${DATA_DEST_ES_PORT}/$index --type=settings
    elasticdump --input=http://${DATA_ES_USER}:${DATA_ES_PASSWORD}@${DATA_ES_HOST}:${DATA_ES_PORT}/$index --output=http://${DATA_DEST_ES_USER}:${DATA_DEST_ES_PASSWORD}@${DATA_DEST_ES_HOST}:${DATA_DEST_ES_PORT}/$index --type=alias --ignore-errors=true

    elasticdump --input=http://${DATA_ES_USER}:${DATA_ES_PASSWORD}@${DATA_ES_HOST}:${DATA_ES_PORT}/$index --output=${index}.json --type=mapping
    index_mapping=$(jq '.[].mappings' ${index}.json)
    echo $index_mapping | curl -XPUT -H 'Content-Type: application/json' -u ${DATA_DEST_ES_USER}:${DATA_DEST_ES_PASSWORD} http://${DATA_DEST_ES_HOST}:${DATA_DEST_ES_PORT}/$index/_mapping -d @-
    rm -f ${index}.json

    elasticdump --input=http://${DATA_ES_USER}:${DATA_ES_PASSWORD}@${DATA_ES_HOST}:${DATA_ES_PORT}/$index --output=http://${DATA_DEST_ES_USER}:${DATA_DEST_ES_PASSWORD}@${DATA_DEST_ES_HOST}:${DATA_DEST_ES_PORT}/$index --limit=${limit} --type=data
    echo "==========migrate index:"$index", end=========="
  fi
done
