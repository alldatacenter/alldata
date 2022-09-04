#!/usr/bin/python
# -*- coding: UTF-8 -*-

import os
import os.path
import sys
import json
import urllib
import zipfile
from tempfile import NamedTemporaryFile
import shutil
try:
    import urllib.request              
except ImportError:
    pass

self_path = os.path.split(os.path.realpath(__file__))[0]

def download(url):
    if hasattr(urllib, "urlretrieve"):
        f = NamedTemporaryFile(delete=False)
        return urllib.urlretrieve(url, f.name)[0]
    else:
        return urllib.request.urlretrieve(url)[0]

f = open(self_path + '/../built-in.json', 'r')
builtInList = json.loads(f.read())
f.close()

# 获取云端市场的索引文件sw-index.json
if len(sys.argv) > 1:
   endpoint = sys.argv[1]
else:
   endpoint = "http://sreworks.oss-cn-beijing.aliyuncs.com/markets"

f = open(download(endpoint + "/sw-index.json"), 'r')
markets = json.loads(f.read())
f.close()

marketPacks = {}
for marketPack in markets["packages"]:
    marketPacks[marketPack["appId"]] = marketPack

for buildIn in builtInList:
    print(buildIn)
    marketPack = marketPacks[buildIn["appId"]]
    lastVersion = marketPack["packageVersions"][-1]
    if marketPack["urls"][lastVersion].startswith("http"):
        lastPackageUrl = marketPack["urls"][lastVersion]
    else:
        lastPackageUrl = endpoint + "/" + urllib.parse.quote(marketPack["urls"][lastVersion])
 

    loalPath = self_path + "/../" + buildIn["packagePath"]
   
    if os.path.exists(loalPath):
        shutil.rmtree(loalPath)
    else:
        os.makedirs(loalPath)

    print(buildIn["appId"] + " -> " + loalPath)
    with zipfile.ZipFile(download(lastPackageUrl),"r") as zip_ref:
        zip_ref.extractall(loalPath)

    for name in os.listdir(loalPath):
        filename = loalPath + "/" + name
        if zipfile.is_zipfile(filename):
            dirName = filename + ".dir"
            os.makedirs(dirName)
            with zipfile.ZipFile(filename,"r") as zip_ref:
                zip_ref.extractall(dirName)
            os.remove(filename)


