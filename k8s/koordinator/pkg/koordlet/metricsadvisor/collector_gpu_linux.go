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

package metricsadvisor

import (
	"errors"
	"fmt"
	"sort"
	"sync"
	"time"

	"github.com/NVIDIA/go-nvml/pkg/nvml"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/pkg/features"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/util"
)

type gpuDeviceManager struct {
	sync.RWMutex
	deviceCount      int
	devices          []*device
	collectTime      time.Time
	processesMetrics map[uint32][]*rawGPUMetric
}

type rawGPUMetric struct {
	SMUtil     uint32 // current utilization rate for the device
	MemoryUsed uint64
}

type device struct {
	Minor       int32 // index starting from 0
	DeviceUUID  string
	MemoryTotal uint64
	Device      nvml.Device
}

// initGPUDeviceManager will not retry if init fails,
func initGPUDeviceManager() GPUDeviceManager {
	if !features.DefaultKoordletFeatureGate.Enabled(features.Accelerators) {
		return &dummyDeviceManager{}
	}
	if ret := nvml.Init(); ret != nvml.SUCCESS {
		if ret == nvml.ERROR_LIBRARY_NOT_FOUND {
			klog.Warning("nvml init failed, library not found")
			return &dummyDeviceManager{}
		}
		klog.Warningf("nvml init failed, return %s", nvml.ErrorString(ret))
		return &dummyDeviceManager{}
	}
	manager := &gpuDeviceManager{}
	if err := manager.initGPUData(); err != nil {
		klog.Warningf("nvml init gpu data, error %s", err)
		manager.shutdown()
		return &dummyDeviceManager{}
	}

	return manager
}

func (g *gpuDeviceManager) shutdown() error {
	rt := nvml.Shutdown()
	if rt != nvml.SUCCESS {
		return fmt.Errorf("nvml shutdown error, code: %s", nvml.ErrorString(rt))
	}
	return nil
}

func (g *gpuDeviceManager) initGPUData() error {
	count, ret := nvml.DeviceGetCount()
	if ret != nvml.SUCCESS {
		return fmt.Errorf("unable to get device count: %v", nvml.ErrorString(ret))
	}
	if count == 0 {
		return errors.New("no gpu device found")
	}
	devices := make([]*device, count)
	for deviceIndex := 0; deviceIndex < count; deviceIndex++ {
		gpudevice, ret := nvml.DeviceGetHandleByIndex(deviceIndex)
		if ret != nvml.SUCCESS {
			return fmt.Errorf("unable to get device at index %d: %v", deviceIndex, nvml.ErrorString(ret))
		}

		uuid, ret := gpudevice.GetUUID()
		if ret != nvml.SUCCESS {
			return fmt.Errorf("unable to get device uuid: %v", nvml.ErrorString(ret))
		}

		minor, ret := gpudevice.GetMinorNumber()
		if ret != nvml.SUCCESS {
			return fmt.Errorf("unable to get device minor number: %v", nvml.ErrorString(ret))
		}

		memory, ret := gpudevice.GetMemoryInfo()
		if ret != nvml.SUCCESS {
			return fmt.Errorf("unable to get device memory info: %v", nvml.ErrorString(ret))
		}
		devices[deviceIndex] = &device{
			DeviceUUID:  uuid,
			Minor:       int32(minor),
			MemoryTotal: memory.Total,
			Device:      gpudevice,
		}
	}

	g.Lock()
	defer g.Unlock()
	g.deviceCount = count
	g.devices = devices
	return nil
}

func (g *gpuDeviceManager) getNodeGPUUsage() []metriccache.GPUMetric {
	g.RLock()
	defer g.RUnlock()
	tmp := make([]rawGPUMetric, g.deviceCount)
	for i := 0; i < g.deviceCount; i++ {
		tmp[i] = rawGPUMetric{}
	}
	for _, p := range g.processesMetrics {
		for idx := 0; idx < g.deviceCount; idx++ {
			if m := p[uint32(idx)]; m != nil {
				tmp[idx].SMUtil += p[uint32(idx)].SMUtil
				tmp[idx].MemoryUsed += p[uint32(idx)].MemoryUsed
			}
		}
	}
	rtn := make([]metriccache.GPUMetric, g.deviceCount)
	for i := 0; i < g.deviceCount; i++ {
		rtn[i] = metriccache.GPUMetric{
			DeviceUUID:  g.devices[i].DeviceUUID,
			Minor:       g.devices[i].Minor,
			SMUtil:      tmp[i].SMUtil,
			MemoryUsed:  *resource.NewQuantity(int64(tmp[i].MemoryUsed), resource.BinarySI),
			MemoryTotal: *resource.NewQuantity(int64(g.devices[i].MemoryTotal), resource.BinarySI),
		}
	}
	return rtn
}

func (g *gpuDeviceManager) getTotalGPUUsageOfPIDs(pids []uint32) []metriccache.GPUMetric {
	g.RLock()
	defer g.RUnlock()
	tmp := make(map[int]*rawGPUMetric)
	for _, pid := range pids {
		if metrics, exist := g.processesMetrics[pid]; exist {
			for idx, metric := range metrics {
				if metric == nil {
					continue
				}
				if _, found := tmp[idx]; !found {
					tmp[idx] = &rawGPUMetric{}
				}
				tmp[idx].MemoryUsed += metric.MemoryUsed
				tmp[idx].SMUtil += metric.SMUtil
			}
		}
	}
	if len(tmp) == 0 {
		return nil
	}
	rtn := make([]metriccache.GPUMetric, 0)
	for i := 0; i < g.deviceCount; i++ {
		if value, ok := tmp[i]; ok {
			rtn = append(rtn, metriccache.GPUMetric{
				DeviceUUID:  g.devices[i].DeviceUUID,
				Minor:       g.devices[i].Minor,
				SMUtil:      value.SMUtil,
				MemoryUsed:  *resource.NewQuantity(int64(value.MemoryUsed), resource.BinarySI),
				MemoryTotal: *resource.NewQuantity(int64(g.devices[i].MemoryTotal), resource.BinarySI),
			})
		}
	}
	return rtn
}

func (g *gpuDeviceManager) getPodGPUUsage(podParentDir string, cs []corev1.ContainerStatus) ([]metriccache.GPUMetric, error) {
	runningContainer := make([]corev1.ContainerStatus, 0)
	for _, c := range cs {
		if c.State.Running == nil {
			klog.V(5).Infof("non-running container %s", c.ContainerID)
			continue
		}
		runningContainer = append(runningContainer, c)
	}
	if len(runningContainer) == 0 {
		return nil, nil
	}
	pids, err := util.GetPIDsInPod(podParentDir, cs)
	if err != nil {
		return nil, fmt.Errorf("failed to get pid, error: %v", err)
	}
	return g.getTotalGPUUsageOfPIDs(pids), nil
}

func (g *gpuDeviceManager) getContainerGPUUsage(podParentDir string, c *corev1.ContainerStatus) ([]metriccache.GPUMetric, error) {
	if c.State.Running == nil {
		klog.V(5).Infof("non-running container %s", c.ContainerID)
		return nil, nil
	}
	currentPIDs, err := util.GetPIDsInContainer(podParentDir, c)
	if err != nil {
		return nil, fmt.Errorf("failed to get pid, error: %v", err)
	}
	return g.getTotalGPUUsageOfPIDs(currentPIDs), nil
}

func (g *gpuDeviceManager) collectGPUUsage() {
	processesGPUUsages := make(map[uint32][]*rawGPUMetric)
	for deviceIndex, gpuDevice := range g.devices {
		processesInfos, ret := gpuDevice.Device.GetComputeRunningProcesses()
		if ret != nvml.SUCCESS {
			klog.Warningf("Unable to get process info for device at index %d: %v", deviceIndex, nvml.ErrorString(ret))
			continue
		}
		processUtilizations, ret := gpuDevice.Device.GetProcessUtilization(1024)
		if ret != nvml.SUCCESS {
			klog.Warningf("Unable to get process utilization for device at index %d: %v", deviceIndex, nvml.ErrorString(ret))
			continue
		}

		// Sort by pid.
		sort.Slice(processesInfos, func(i, j int) bool {
			return processesInfos[i].Pid < processesInfos[j].Pid
		})
		sort.Slice(processUtilizations, func(i, j int) bool {
			return processUtilizations[i].Pid < processUtilizations[j].Pid
		})

		klog.V(3).Infof("Found %d processes on device %d\n", len(processesInfos), deviceIndex)
		for idx, info := range processesInfos {
			if _, ok := processesGPUUsages[info.Pid]; !ok {
				// pid not exist.
				// init processes gpu metric array.
				processesGPUUsages[info.Pid] = make([]*rawGPUMetric, g.deviceCount)
			}
			processesGPUUsages[info.Pid][deviceIndex] = &rawGPUMetric{
				SMUtil:     processUtilizations[idx].SmUtil,
				MemoryUsed: info.UsedGpuMemory,
			}
		}
	}
	g.Lock()
	g.processesMetrics = processesGPUUsages
	g.collectTime = time.Now()
	g.Unlock()
}
