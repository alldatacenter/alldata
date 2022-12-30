#!/usr/bin/python
# -*- coding: UTF-8 -*-

import os
import os.path
import sys
import time
from subprocess import Popen, PIPE

self_path = os.path.split(os.path.realpath(__file__))[0]

def popen(command):
    child = Popen(command, stdin = PIPE, stdout = PIPE, stderr = PIPE, shell = True)
    out, err = child.communicate()
    ret = child.wait()
    return (ret, out.strip(), err.strip())

# 获取所有的storageClass
command = "kubectl get storageclass|grep -v NAME"
(ret, out, err) = popen(command)
storageClasses = []
for line in out.split("\n"):
    storageClass = line.split()[0]
    storageClasses.append(storageClass)
    h = open(self_path + '/storage-class-check.yaml', 'r')
    content = h.read()
    h.close()
    yaml = content.replace('${storageClass}', storageClass)
    h = open('/tmp/' + storageClass + ".yaml", 'w')
    h.write(yaml)
    h.close()
    (ret, out, err) = popen("kubectl apply -f " + '/tmp/' + storageClass + ".yaml") 
    print(out)

result = {}

for i in range(10):
    time.sleep(1)
    print("StorageClass Test Wait " + str(10 - i) + "s") 
    for storageClass in storageClasses:
       if result.get(storageClass) is None:
           result[storageClass] = "Failed"
       if result[storageClass] == "OK":
           continue
       
       command ="kubectl get pod test-" + storageClass + "-0 | grep -v NAME"
       (ret, out, err) = popen(command)
       
       if ret == 0 and out.split()[2] == "Completed":
           result[storageClass] = "OK"

for storageClass in storageClasses:
    (ret, out, err) = popen("kubectl delete -f " + '/tmp/' + storageClass + ".yaml") 
    print(out)
    (ret, out, err) = popen("kubectl delete pvc " + 'test-' + storageClass + "-0") 
    print(out)


print("")
print("-------------------")
for k,v in result.items():
    print(k + " result: " +  v)




