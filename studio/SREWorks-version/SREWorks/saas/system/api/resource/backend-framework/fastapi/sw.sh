#!/bin/bash

SW_ROOT=$(cd `dirname $0`; pwd)

set -e


initContainer(){
   touch ${SW_ROOT}/${DOCKERFILE}
   
   case $SCHEME in
   sql)

        if [ ! -d "${SW_ROOT}/APP-META/db_${NAME}" ]; then
           mkdir ${SW_ROOT}/APP-META/db_${NAME}
           cp ${SW_ROOT}/APP-META/templates/db/* ${SW_ROOT}/APP-META/db_${NAME}
        fi

        ;;
   python)
        echo "python scheme not support"
        exit 1
        ;;
   *)
        echo "unknown scheme"
        exit 1
   esac

}

_init(){
  POSITIONAL=()
  while [[ $# -gt 0 ]]; do
    key="$1"
    case $key in
      -n|--name)
        NAME="$2"
        shift # past argument
        shift # past value
        ;;
      -t|--target)
        TARGET="$2"
        shift # past argument
        shift # past value
        ;;
      -d|--dockfile)
        DOCKERFILE="$2"
        shift # past argument
        shift # past value
        ;;
      -d|--dockfile)
        DOCKERFILE="$2"
        shift # past argument
        shift # past value
        ;;
      -s|--scheme)
        SCHEME="$2"
        shift # past argument
        shift # past value
        ;;
      *)    # unknown option
        POSITIONAL+=("$1") # save it in an array for later
        shift # past argument
        ;;
    esac
  done
  set -- "${POSITIONAL[@]}" # restore positional parameters
  if  [ ! -n "$NAME" ] ;then
    echo "" >&2
    echo "SREWorks Project Framework: FastAPI " >&2
    echo "" >&2
    echo "Usage:" >&2
    echo "   sw.sh init [flags]" >&2
    echo "" >&2
    echo "Flags:" >&2
    echo "   -n, --name          " >&2
    echo "   -t, --target        initContainer " >&2
    echo "   -d, --dockfile      Dockile_xxx.tpl" >&2
    echo "   -s, --scheme        sql | python" >&2
    echo "" >&2
  else
    echo "NAME                = ${NAME}" >&2
    echo "TARGET              = ${TARGET}" >&2
    echo "DOCKERFILE          = ${DOCKERFILE}" >&2
    echo "SCHEME              = ${SCHEME}" >&2

    initContainer

  fi

}

_push(){
  POSITIONAL=()
  while [[ $# -gt 0 ]]; do
    key="$1"
    case $key in
      -u|--url)
        URL="$2"
        shift # past argument
        shift # past value
        ;;
      -a|--ciAccount)
        CI_ACCOUNT="$2"
        shift # past argument
        shift # past value
        ;;
      -t|--ciToken)
        CI_TOKEN="$2"
        shift # past argument
        shift # past value
        ;;
      *)    # unknown option
        POSITIONAL+=("$1") # save it in an array for later
        shift # past argument
        ;;
    esac
  done
  set -- "${POSITIONAL[@]}" # restore positional parameters
  if  [ ! -n "$URL" ] ;then
    echo "" >&2
    echo "SREWorks Project Framework: FastAPI " >&2
    echo "" >&2
    echo "Usage:" >&2
    echo "   sw.sh push [flags]" >&2
    echo "" >&2
    echo "Flags:" >&2
    echo "   -u, --url           repo url like: http://github.com/sreworks/sreworks.git" >&2
    echo "   -a, --ciAccount     " >&2
    echo "   -t, --ciToken       " >&2
    echo "" >&2
  else
    echo "URL                 = ${URL}" >&2
    echo "CI_TOKEN            = ${CI_TOKEN}" >&2
    echo "CI_ACCOUNT          = ${CI_ACCOUNT}" >&2

    HTTP_PROTOCOL=$(echo $URL | awk -F '//' '{print $1}')
    URL_NO_HTTP=$(echo $URL | awk -F '//' '{print $NF}'|awk -F '@' '{print $NF}')
    if  [ ! -n "$CI_TOKEN" ] || [ ! -n "$CI_ACCOUNT" ] ;then
       REPO_URL=$URL
    else
       REPO_URL=${HTTP_PROTOCOL}"//"${CI_ACCOUNT}:${CI_TOKEN}'@'$URL_NO_HTTP
    fi

    set -x
    cd $SW_ROOT
    git init
    git remote remove sw_remote
    git remote add sw_remote $REPO_URL
    git add -A
    git config user.name "sreworks-admin"
    git config user.email "admin@sreworks.io"
    git commit -m "init"
    git push sw_remote master:master

  fi

}


help(){
    echo "" >&2
    echo "SREWorks Project Framework: FastAPI " >&2
    echo "" >&2
    echo "Usage:" >&2
    echo "   sw.sh [command]" >&2
    echo "" >&2
    echo "Available Commands:" >&2
    echo "   init   init container template" >&2
    echo "   push   push to remote repo" >&2
    echo "" >&2
}

mod=""
while [[ $1 != -* ]] && [[ $# -gt 0 ]] ; do
  mod=${mod}"_"$1
  shift
done

if  [ ! -n "$mod" ] ;then
  help
else
  eval $mod $*|| help || exit 1
fi





