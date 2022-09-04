#!/usr/bin/python
# -*- coding: UTF-8 -*-

import os
import os.path
import sys

self_path = os.path.split(os.path.realpath(__file__))[0]

f = open(self_path + "/SUMMARY.md")
summary = f.read()
f.close()
files = []
for a in summary.split("]("):
    fileName = a.split(")")[0]
    if not fileName.startswith("documents/"): continue
    files.append(fileName.split("/")[1])

os.popen("rm %s/pictures/*" % self_path)

for docName in os.listdir(self_path + "/documents"):
    if docName not in files: continue
    f = open(self_path + "/documents/" + docName, 'r')
    content = f.read()
    f.close()
    raw = content.replace('![](', '![image.png](').split(".png](")

    for r in raw:
        if not r.startswith("http"): continue
        pic = r.split(")")[0]
        pic_name = pic.split(".png#")[0].split("/")[-1] + ".png"
        command = "wget '%s' -O %s/pictures/%s" % (pic, self_path, pic_name)
        os.popen(command)
        content = content.replace(pic, "/pictures/" + pic_name)

    
    f = open(self_path + "/documents/" + docName, 'w')
    f.write(content)
    f.close()
        
    
