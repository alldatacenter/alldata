#!/bin/bash

SW_ROOT=$(cd `dirname $0`; pwd)


action_save(){
    mkdir -p $DIR
    cat $IMAGES|while read line
    do
       file_name=$(echo $line |tr '/' '_'|tr ':' '_')
       docker pull $line
       echo "docker save $line -o $DIR/$file_name"
       docker save $line -o $DIR/$file_name
    done   
}


action_push(){
    cat $IMAGES|while read line
    do
       file_name=$(echo $line |tr '/' '_'|tr ':' '_')
       docker load -i $DIR/$file_name
       docker push $line
    done
}


POSITIONAL=()
while [[ $# -gt 0 ]]; do
  key="$1"

  case $key in
    -a|--action)
      ACTION="$2"
      shift # past argument
      shift # past value
      ;;
    -i|--images)
      IMAGES="$2"
      shift # past argument
      #shift # past value
      ;;
    -d|--dir)
      DIR="$2"
      shift # past argument
      shift # past value
      ;;
    --tag)
      TAG="$2"
      shift # past argument
      shift # past value
      ;; 
    --default)
      DEFAULT=YES
      shift # past argument
      ;;
    *)    # unknown option
      POSITIONAL+=("$1") # save it in an array for later
      shift # past argument
      ;;
  esac
done

set -- "${POSITIONAL[@]}" # restore positional parameters

if  [ ! -n "$ACTION" ] ;then
    echo "" >&2
    echo "      -a, --action        save, push " >&2
    echo "" >&2
    echo "      [save]" >&2
    echo "        -d, --dir         Save images in directory" >&2
    echo "        -i, --images      images.txt " >&2
    echo "" >&2
    echo "      [push]" >&2
    echo "        -d, --dir         Push images from directory" >&2
    echo "        -i, --images      images.txt " >&2 
    echo "" >&2
else
    [ -n "$IMAGES" ] && IMAGES=$IMAGES || IMAGES=$SW_ROOT/images.txt
    
    echo "ACTION              = ${ACTION}" >&2
    echo "DIR                 = ${DIR}" >&2
    echo "IMAGES              = ${IMAGES}" >&2

    case $ACTION in
    save)
        action_save
        ;;
    push)
        action_push
        ;;
    *)
        echo "No match action"
    esac
fi

