#!/bin/bash

set -x
set -e

PYTHON_BIN=python
RUN_DIR=/app/postrun
SPLIT_STRING='.'
PYTHON_SUFFIX='.py'

# tpl文件渲染
ENV_ARG=$(awk 'BEGIN{for(v in ENVIRON) printf "${%s} ", v;}')
for file in $(find $RUN_DIR); do
    if [ "${file: -4}" == ".tpl" ]; then
        echo "Replace template file: ${file} "
        new_basename=$(basename ${file} .tpl)
        dir=$(dirname "$file")
        envsubst "${ENV_ARG}" <${file} >${dir}/${new_basename}
    fi
done

# 等待 60s 避免发生意外
sleep 60s

# postrun 脚本执行
for script in `find $RUN_DIR -maxdepth 1 -type f -name "*.sh" -o -name "*.py" | sort`
do
    # 去尾判断，检查是不是.py脚本，如果是则使用 python 执行，否则使用 /bin/bash 执行
    SRCPWD=${script%$SPLIT_STRING*}
    if [ "${script%$PYTHON_SUFFIX}" != "$script"  ]
    then
        echo "Execute python script: ", $script, $SRCPWD
        SRCPWD=$SRCPWD $PYTHON_BIN $script
    else
        echo "Execute bash script ", $script, $SRCPWD
        SRCPWD=$SRCPWD /bin/bash $script
    fi
done
