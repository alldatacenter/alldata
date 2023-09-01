<!--
  ~ Licensed to the Apache Software Foundation (ASF) under one or more
  ~ contributor license agreements.  See the NOTICE file distributed with
  ~ this work for additional information regarding copyright ownership.
  ~ The ASF licenses this file to You under the Apache License, Version 2.0
  ~ (the "License"); you may not use this file except in compliance with
  ~ the License.  You may obtain a copy of the License at
  ~
  ~    http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->

# Fully Upgrade of Shuffle Servers

If we want to upgrade shuffle servers in full, we first need to update the configuration files in the configMap.

Then, we need to edit the rss object as follows:

+ update `.spec.shuffleServer.image` with new image version of shuffle server
+ set `.spec.shuffleServer.sync` field to `true`
+ update `.spec.shuffleServer.upgradeStrategy` field:
    + set `.spec.shuffleServer.upgradeStrategy.type` to be `FullUpgrade`

```yaml
spec:
  shuffleServer:
    image: "${rss-shuffle-server-image}"
    sync: true
    upgradeStrategy:
      type: "FullUpgrade"
```

We can refer to the [example](rss-full-upgrade.yaml).