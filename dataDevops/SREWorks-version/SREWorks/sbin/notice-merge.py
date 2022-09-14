#!/usr/bin/python
# -*- coding: UTF-8 -*-

import os
import os.path
import sys

self_path = os.path.split(os.path.realpath(__file__))[0]

noticeFiles = [
   "/paas/appmanager/NOTICE",
   "/paas/tesla-gateway/NOTICE",
   "/paas/tesla-authproxy/NOTICE",
   "/paas/NOTICE",
   "/paas/sw-frontend/NOTICE" 
]

raws = []

for nf in noticeFiles:
    f = open(self_path + '/../' + nf, 'r')
    content = f.read()
    for block in content.split("\n\n"):
        licence = block.strip().split("--", 1)[0].strip("-").strip()
        if licence == "" or "=====" in licence: continue
        component = block.replace(licence, "").strip().strip("-").strip().split()
        
        raws.append({
            "licence": licence,
            "component": component,
        })


data = {
   "The MIT License (MIT)": set(),
   "Apache Software Foundation License 2.0": set(),
}

for r in raws:
    if "MIT" in r["licence"]:
        key = "The MIT License (MIT)"
    elif "Apache" in r["licence"]:
        key = "Apache Software Foundation License 2.0"
    elif r["licence"].lower() == "unknown":
        key = "Unknown"
    elif "BSD" in r["licence"]:
        key = "BSD licensed"
    elif "Eclipse" in r["licence"]:
        key = "Eclipse Public License"
    else:
        key = r["licence"]
    if key not in data:
        data[key] = set()
    for component in r["component"]:
        if "springframework" in component:
            value = "org.springframework"
        else:
            value = component

        data[key].add(value)

content = """
========================================================
Copyright (c) 2019-2022, Alibaba Group Holding Limited.
Licensed under the Apache License, Version 2.0

===================================================================
This product contains various third-party components under other open source licenses.
This section summarizes those components and their licenses.
"""
for k,v in data.items():
    if "JSON" in k:
        continue
    elif "DOM4J" in k:
        continue
    content += "\n"
    content += k + "\n"
    content += "-"*len(k) + "\n"
    for component in v:
        content += component + "\n"
    content += "\n"

for k,v in data.items():
    if "JSON" in k:
        h = open(self_path + "/JSON.LICENSE", 'r')
        license = h.read()
        h.close()
    elif "DOM4J" in k:
        h = open(self_path + "/DOM4J.LICENSE", 'r')
        license = h.read()
        h.close()
    else:
        continue

    content += "\n"
    content += k + "\n"
    content += "-"*len(k) + "\n"
    content += license
    content += "\n"

f = open(self_path + "/../NOTICE", 'w')
f.write(content)
f.close()
