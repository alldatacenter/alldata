/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package framework

import (
	"time"

	"k8s.io/klog/v2"
)

type Context struct {
	DeviceCollectors map[string]DeviceCollector
	Collectors       map[string]Collector
}

func DeviceCollectorsStarted(devices map[string]DeviceCollector) bool {
	for name, device := range devices {
		if device.Enabled() && !device.Started() {
			klog.V(6).Infof("device collector %v is enabled but has not started yet", name)
			return false
		}
	}
	return true
}

func CollectorsHasStarted(collectors map[string]Collector) bool {
	for name, collector := range collectors {
		if collector.Enabled() && !collector.Started() {
			klog.V(6).Infof("collector %v is enabled but has not started yet", name)
			return false
		}
	}
	return true
}

type CPUStat struct {
	// TODO check CPUTick or CPUUsage can be abandoned
	CPUTick   uint64
	CPUUsage  uint64
	Timestamp time.Time
}
