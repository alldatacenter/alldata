#!/bin/bash
packagePath=$1
md5=`md5sum $packagePath | awk '{print $1}'`
echo $md5
