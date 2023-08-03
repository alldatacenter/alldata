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

# Fully Restart of Shuffle Servers

If we want to restart shuffle server pods in full, we need to set `.spec.shuffleServer.sync` field to `true`, and
update `.spec.shuffleServer.upgradeStrategy.type` field to be `FullRestart`.

```yaml
spec:
shuffleServer:
  sync: true
  upgradeStrategy:
    type: "FullRestart"
```

Unlike full upgrade, full restart does not require configuration and image modification.

We can refer to the [example](rss-full-restart.yaml).