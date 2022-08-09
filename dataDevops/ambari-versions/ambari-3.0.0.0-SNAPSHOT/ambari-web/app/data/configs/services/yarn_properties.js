/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

module.exports = [
  {
    "category": "RESOURCEMANAGER",
    "filename": "yarn-site.xml",
    "name": "yarn.acl.enable",
    "serviceName": "YARN"
  },
  {
    "category": "RESOURCEMANAGER",
    "filename": "yarn-site.xml",
    "name": "yarn.admin.acl",
    "serviceName": "YARN"
  },
  {
    "category": "RESOURCEMANAGER",
    "filename": "yarn-site.xml",
    "name": "yarn.log-aggregation-enable",
    "serviceName": "YARN"
  },
  {
    "category": "CapacityScheduler",
    "filename": "yarn-site.xml",
    "name": "yarn.resourcemanager.scheduler.class",
    "serviceName": "YARN"
  },
  {
    "category": "CapacityScheduler",
    "filename": "yarn-site.xml",
    "name": "yarn.scheduler.minimum-allocation-mb",
    "serviceName": "YARN"
  },
  {
    "category": "CapacityScheduler",
    "filename": "yarn-site.xml",
    "name": "yarn.scheduler.maximum-allocation-mb",
    "serviceName": "YARN"
  },
  {
    "category": "NODEMANAGER",
    "filename": "yarn-site.xml",
    "name": "yarn.nodemanager.resource.memory-mb",
    "serviceName": "YARN"
  },
  {
    "category": "NODEMANAGER",
    "filename": "yarn-site.xml",
    "name": "yarn.nodemanager.vmem-pmem-ratio",
    "serviceName": "YARN"
  },
  {
    "category": "Isolation",
    "filename": "yarn-site.xml",
    "name": "yarn.nodemanager.linux-container-executor.cgroups.mount-path",
    "serviceName": "YARN"
  },
  {
    "category": "Isolation",
    "filename": "yarn-site.xml",
    "name": "yarn.nodemanager.linux-container-executor.group",
    "serviceName": "YARN"
  },
  {
    "category": "NODEMANAGER",
    "filename": "yarn-site.xml",
    "name": "yarn.nodemanager.log-dirs",
    "serviceName": "YARN"
  },
  {
    "category": "NODEMANAGER",
    "filename": "yarn-site.xml",
    "name": "yarn.nodemanager.local-dirs",
    "serviceName": "YARN"
  },
  {
    "category": "NODEMANAGER",
    "filename": "yarn-site.xml",
    "name": "yarn.nodemanager.remote-app-log-dir",
    "serviceName": "YARN"
  },
  {
    "category": "NODEMANAGER",
    "filename": "yarn-site.xml",
    "name": "yarn.nodemanager.remote-app-log-dir-suffix",
    "serviceName": "YARN"
  },
  {
    "category": "NODEMANAGER",
    "filename": "yarn-site.xml",
    "name": "yarn.nodemanager.aux-services",
    "serviceName": "YARN"
  },
  {
    "category": "NODEMANAGER",
    "filename": "yarn-site.xml",
    "name": "yarn.nodemanager.log.retain-second",
    "serviceName": "YARN"
  },
  {
    "category": "General",
    "filename": "yarn-env.xml",
    "index": 0,
    "name": "yarn_heapsize",
    "serviceName": "YARN"
  },
  {
    "category": "ResourceTypes",
    "filename": "resource-types.xml",
    "name": "yarn.resource-types",
    "serviceName": "YARN"
  },
  {
    "category": "ResourceTypes",
    "filename": "resource-types.xml",
    "name": "yarn.resource-types.yarn.io/gpu.maximum-allocation",
    "serviceName": "YARN"
  },
  {
    "category": "RESOURCEMANAGER",
    "filename": "yarn-env.xml",
    "index": 1,
    "name": "resourcemanager_heapsize",
    "serviceName": "YARN"
  },
  {
    "category": "NODEMANAGER",
    "filename": "yarn-env.xml",
    "index": 1,
    "name": "nodemanager_heapsize",
    "serviceName": "YARN"
  },
  {
    "category": "APP_TIMELINE_SERVER",
    "filename": "yarn-env.xml",
    "index": 1,
    "name": "apptimelineserver_heapsize",
    "serviceName": "YARN"
  },
  {
    "category": "APP_TIMELINE_SERVER",
    "filename": "yarn-site.xml",
    "name": "yarn.timeline-service.enabled",
    "serviceName": "YARN"
  },
  {
    "category": "APP_TIMELINE_SERVER",
    "filename": "yarn-site.xml",
    "name": "yarn.timeline-service.leveldb-timeline-store.path",
    "serviceName": "YARN"
  },
  {
    "category": "APP_TIMELINE_SERVER",
    "filename": "yarn-site.xml",
    "name": "yarn.timeline-service.leveldb-timeline-store.ttl-interval-ms",
    "serviceName": "YARN"
  },
  {
    "category": "APP_TIMELINE_SERVER",
    "filename": "yarn-site.xml",
    "name": "yarn.timeline-service.store-class",
    "serviceName": "YARN"
  },
  {
    "category": "APP_TIMELINE_SERVER",
    "filename": "yarn-site.xml",
    "name": "yarn.timeline-service.ttl-enable",
    "serviceName": "YARN"
  },
  {
    "category": "APP_TIMELINE_SERVER",
    "filename": "yarn-site.xml",
    "name": "yarn.timeline-service.ttl-ms",
    "serviceName": "YARN"
  },
  {
    "category": "APP_TIMELINE_SERVER",
    "filename": "yarn-site.xml",
    "name": "yarn.timeline-service.generic-application-history.store-class",
    "serviceName": "YARN"
  },
  {
    "category": "APP_TIMELINE_SERVER",
    "filename": "yarn-site.xml",
    "name": "yarn.timeline-service.webapp.address",
    "serviceName": "YARN"
  },
  {
    "category": "APP_TIMELINE_SERVER",
    "filename": "yarn-site.xml",
    "name": "yarn.timeline-service.webapp.https.address",
    "serviceName": "YARN"
  },
  {
    "category": "APP_TIMELINE_SERVER",
    "filename": "yarn-site.xml",
    "name": "yarn.timeline-service.address",
    "serviceName": "YARN"
  },
  {
    "category": "APP_TIMELINE_SERVER",
    "filename": "yarn-site.xml",
    "name": "yarn.timeline-service.leveldb-state-store.path",
    "serviceName": "YARN"
  },
  {
    "category": "APP_TIMELINE_SERVER",
    "filename": "yarn-site.xml",
    "name": "yarn.timeline-service.state-store-class",
    "serviceName": "YARN"
  },
  {
    "category": "FaultTolerance",
    "filename": "yarn-site.xml",
    "name": "yarn.nodemanager.recovery.enabled",
    "serviceName": "YARN"
  },
  {
    "category": "FaultTolerance",
    "filename": "yarn-site.xml",
    "name": "yarn.resourcemanager.recovery.enabled",
    "serviceName": "YARN"
  },
  {
    "category": "FaultTolerance",
    "filename": "yarn-site.xml",
    "name": "yarn.resourcemanager.work-preserving-recovery.enabled",
    "serviceName": "YARN"
  },
  {
    "category": "FaultTolerance",
    "filename": "yarn-site.xml",
    "name": "yarn.resourcemanager.zk-address",
    "serviceName": "YARN"
  },
  {
    "category": "FaultTolerance",
    "filename": "yarn-site.xml",
    "name": "yarn.resourcemanager.connect.retry-interval.ms",
    "serviceName": "YARN"
  },
  {
    "category": "FaultTolerance",
    "filename": "yarn-site.xml",
    "name": "yarn.resourcemanager.connect.max-wait.ms",
    "serviceName": "YARN"
  },
  {
    "category": "FaultTolerance",
    "filename": "yarn-site.xml",
    "name": "yarn.resourcemanager.ha.enabled",
    "serviceName": "YARN"
  },
  {
    "category": "Isolation",
    "filename": "yarn-site.xml",
    "name": "yarn.nodemanager.container-executor.class",
    "serviceName": "YARN"
  },
  {
    "category": "Isolation",
    "filename": "yarn-site.xml",
    "name": "yarn.nodemanager.linux-container-executor.resources-handler.class",
    "serviceName": "YARN"
  },
  {
    "category": "Isolation",
    "filename": "yarn-site.xml",
    "name": "yarn.nodemanager.linux-container-executor.cgroups.hierarchy",
    "serviceName": "YARN"
  },
  {
    "category": "Isolation",
    "filename": "yarn-site.xml",
    "name": "yarn.nodemanager.linux-container-executor.cgroups.mount",
    "serviceName": "YARN"
  },
  {
    "category": "Isolation",
    "filename": "yarn-site.xml",
    "name": "yarn.nodemanager.linux-container-executor.cgroups.strict-resource-usage",
    "serviceName": "YARN"
  },
  {
    "category": "CapacityScheduler",
    "filename": "yarn-site.xml",
    "name": "yarn.nodemanager.resource.cpu-vcores",
    "serviceName": "YARN"
  },
  {
    "category": "CapacityScheduler",
    "filename": "yarn-site.xml",
    "name": "yarn.nodemanager.resource.percentage-physical-cpu-limit",
    "serviceName": "YARN"
  },
  {
    "filename": "ranger-yarn-plugin-properties.xml",
    "index": 1,
    "name": "ranger-yarn-plugin-enabled",
    "serviceName": "YARN"
  },
  {
    "category": "ContainerExecutor",
    "filename": "container-executor.xml",
    "name": "docker_module_enabled",
    "serviceName": "YARN"
  },
  {
    "category": "ContainerExecutor",
    "filename": "container-executor.xml",
    "name": "docker_binary",
    "serviceName": "YARN"
  },
  {
    "category": "ContainerExecutor",
    "filename": "container-executor.xml",
    "name": "docker_allowed_capabilities",
    "serviceName": "YARN"
  },
  {
    "category": "ContainerExecutor",
    "filename": "container-executor.xml",
    "name": "docker_allowed_devices",
    "serviceName": "YARN"
  },
  {
    "category": "ContainerExecutor",
    "filename": "container-executor.xml",
    "name": "docker_allowed_networks",
    "serviceName": "YARN"
  },
  {
    "category": "ContainerExecutor",
    "filename": "container-executor.xml",
    "name": "docker_allowed_ro-mounts",
    "serviceName": "YARN"
  },
  {
    "category": "ContainerExecutor",
    "filename": "container-executor.xml",
    "name": "docker_allowed_rw-mounts",
    "serviceName": "YARN"
  },
  {
    "category": "ContainerExecutor",
    "filename": "container-executor.xml",
    "name": "docker_allowed_volume-drivers",
    "serviceName": "YARN"
  },
  {
    "category": "ContainerExecutor",
    "filename": "container-executor.xml",
    "name": "docker_privileged-containers_enabled",
    "serviceName": "YARN"
  },
  {
    "category": "ContainerExecutor",
    "filename": "container-executor.xml",
    "name": "min_user_id",
    "serviceName": "YARN"
  },
  {
    "category": "ContainerExecutor",
    "filename": "container-executor.xml",
    "name": "gpu_module_enabled",
    "serviceName": "YARN"
  },
  {
    "category": "ContainerExecutor",
    "filename": "container-executor.xml",
    "name": "cgroup_root",
    "serviceName": "YARN"
  },
  {
    "category": "ContainerExecutor",
    "filename": "container-executor.xml",
    "name": "yarn_hierarchy",
    "serviceName": "YARN"
  },
  {
    "category": "Registry",
    "filename": "yarn-site.xml",
    "name": "hadoop.registry.zk.quorum",
    "serviceName": "YARN"
  },
  {
    "category": "Registry",
    "filename": "yarn-env.xml",
    "name": "registry.dns.bind-port",
    "serviceName": "YARN"
  },
  {
    "category": "Registry",
    "filename": "yarn-site.xml",
    "name": "hadoop.registry.dns.zone-mask",
    "serviceName": "YARN"
  },
  {
    "category": "Registry",
    "filename": "yarn-site.xml",
    "name": "hadoop.registry.dns.zone-subnet",
    "serviceName": "YARN"
  },
  {
    "category": "Registry",
    "filename": "yarn-site.xml",
    "name": "hadoop.registry.dns.enabled",
    "serviceName": "YARN"
  },
  {
    "category": "Registry",
    "filename": "yarn-site.xml",
    "name": "hadoop.registry.dns.domain-name",
    "serviceName": "YARN"
  }
];