#!/bin/bash
input=$1
SERVER_LIST_CONFIG=./rack.properties
function read_config() {
	cat $SERVER_LIST_CONFIG | while read LINE
	do
		ip=${LINE%=*}
		rack=${LINE#*=} 
		if [ "$ip" = "$input" ]
		then
			echo $rack
			break
		fi
	done
}
read_config
