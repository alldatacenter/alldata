// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package actionagent

import (
	"os"
	"strconv"
	"strings"

	"github.com/pkg/errors"
	"github.com/shirou/gopsutil/v3/host"
	"github.com/shirou/gopsutil/v3/load"
	"github.com/shirou/gopsutil/v3/mem"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/pipeline/pkg/taskinspect"
)

// validate
func (agent *Agent) validate() {

	// easy use

	contextDir := os.Getenv(CONTEXTDIR)
	if contextDir == "" {
		agent.AppendError(errors.Errorf("missing %s", CONTEXTDIR))
	}
	agent.EasyUse.ContainerContext = contextDir

	workDir := os.Getenv(WORKDIR)
	if workDir == "" {
		agent.AppendError(errors.Errorf("missing %s", WORKDIR))
	}
	agent.EasyUse.ContainerWd = workDir

	metaFile := os.Getenv(METAFILE)
	if metaFile == "" {
		agent.AppendError(errors.Errorf("missing %s", METAFILE))
	}
	agent.EasyUse.ContainerMetaFile = metaFile

	uploadDir := os.Getenv(UPLOADDIR)
	agent.EasyUse.ContainerUploadDir = uploadDir
	agent.EasyUse.ContainerTempTarUploadDir = "/tmp/tar-upload"

	agent.EasyUse.RunScript = "/opt/action/run"

	agent.EasyUse.RunMultiStdoutFilePath = "/tmp/stdout"
	agent.EasyUse.RunMultiStderrFilePath = "/tmp/stderr"

	// validate envs

	// convert(XXX_PUBLIC_URL, XXX_ADDR) -> XXX_ADDR
	agent.convertEnvsByClusterLocation()

	// config collector
	agent.configCollector()

	// report machine stat
	agent.reportMachineStat()
}

const (
	EnvSuffixPublicURL = "_PUBLIC_URL"
	EnvSuffixAddr      = "_ADDR"
)

// 根据是否是边缘集群对环境变量进行转换
// convert(XXX_PUBLIC_URL, XXX_ADDR) -> XXX_ADDR
// example: convert(OPENAPI_PUBLIC_URL, OPENAPI_ADDR) -> OPENAPI_ADDR
func (agent *Agent) convertEnvsByClusterLocation() {

	addrEnvs := make(map[string]string)
	publicURLEnvs := make(map[string]string)

	for _, kv := range os.Environ() {
		s := strings.SplitN(kv, "=", 2)
		if len(kv) < 2 {
			logrus.Printf("Please Ignore. Invalid Env: %s\n", kv)
			continue
		}
		k := s[0]
		v := s[1]

		if strings.HasSuffix(k, EnvSuffixAddr) {
			addrEnvs[k] = v
		}
		if strings.HasSuffix(k, EnvSuffixPublicURL) {
			publicURLEnvs[k] = v
		}
	}

	// 遍历 XXX_PUBLIC_URL，根据 cluster location (central or edge):
	// 1. 如果是 edge cluster，则 XXX_ADDR = XXX_PUBLIC_URL;
	// 2. 如果是 central cluster:
	//    1) 如果 XXX_ADDR 存在，则不做处理;
	//    2) 如果 XXX_ADDR 不存在，则 XXX_ADDR = XXX_PUBLIC_URL
	for pk, pv := range publicURLEnvs {
		xxx := strings.TrimSuffix(pk, EnvSuffixPublicURL)
		ak := xxx + EnvSuffixAddr
		// edge cluster
		if agent.EasyUse.IsEdgeCluster {
			// XXX_ADDR = XXX_PUBLIC_URL
			if err := os.Unsetenv(ak); err != nil {
				agent.AppendError(err)
				continue
			}
			if err := os.Setenv(ak, pv); err != nil {
				agent.AppendError(err)
				continue
			}
			continue
		}
		// central cluster
		_, ok := addrEnvs[ak]
		if ok {
			continue
		}
		if err := os.Setenv(ak, pv); err != nil {
			agent.AppendError(err)
			continue
		}
	}
}

func (agent *Agent) reportMachineStat() {
	defer func() {
		if r := recover(); r != nil {
			logrus.Warnf("[Ignore] failed to collect machine stat, err: %v", r)
		}
	}()

	var stat taskinspect.PipelineTaskMachineStat

	// host
	hostIP := os.Getenv("HOST_IP")
	hostStat, err := host.Info()
	if err != nil {
		logrus.Warnf("[Ignore] failed to collect host stat, err: %v", err)
	} else {
		stat.Host = taskinspect.PipelineTaskMachineHostStat{
			HostIP:          hostIP,
			Hostname:        hostStat.Hostname,
			UptimeSec:       hostStat.Uptime,
			BootTimeSec:     hostStat.BootTime,
			OS:              hostStat.OS,
			Platform:        hostStat.Platform,
			PlatformVersion: hostStat.PlatformVersion,
			KernelVersion:   hostStat.KernelVersion,
			KernelArch:      hostStat.KernelArch,
		}
	}

	// pod
	stat.Pod = taskinspect.PipelineTaskMachinePodStat{
		PodIP: os.Getenv("POD_IP"),
	}

	// load
	loadAvg, err := load.Avg()
	if err != nil {
		logrus.Warnf("[Ignore] failed to collect load avg, err: %v", err)
	} else {
		stat.Load = taskinspect.PipelineTaskMachineLoadStat{
			Load1:  loadAvg.Load1,
			Load5:  loadAvg.Load5,
			Load15: loadAvg.Load15,
		}
	}

	// mem
	vm, err := mem.VirtualMemory()
	if err != nil {
		logrus.Warnf("[Ignore] failed to collect mem stat, err: %v", err)
	} else {
		stat.Mem = taskinspect.PipelineTaskMachineMemStat{
			Total:       vm.Total,
			Available:   vm.Available,
			Used:        vm.Used,
			Free:        vm.Free,
			UsedPercent: vm.UsedPercent,
			Buffers:     vm.Buffers,
			Cached:      vm.Cached,
		}
	}

	// swap
	swap, err := mem.SwapMemory()
	if err != nil {
		logrus.Warnf("[Ignore] failed to collect swap stat, err: %v", err)
	} else {
		stat.Swap = taskinspect.PipelineTaskMachineSwapStat{
			Total:       swap.Total,
			Used:        swap.Used,
			Free:        swap.Free,
			UsedPercent: swap.UsedPercent,
		}
	}

	// callback
	data := Callback{MachineStat: &stat}
	if err := agent.callbackToPipelinePlatform(&data); err != nil {
		logrus.Warnf("[Ignore] failed to report machine stat, err: %v", err)
	}
}

const (
	EnvEnablePushLog2Collector = "ACTIONAGENT_ENABLE_PUSH_LOG_TO_COLLECTOR"
	EnvCollectorAddr           = "COLLECTOR_ADDR"
	EnvTaskLogID               = "TERMINUS_DEFINE_TAG"

	TagDiceOrgName = "dice_org_name"
	TagDiceOrgID   = "dice_org_id"
)

// configCollector config collector about
func (agent *Agent) configCollector() {
	// enable push log 2 collector
	enablePushLog2CollectorStr := os.Getenv(EnvEnablePushLog2Collector)
	enablePushLog2Collector, _ := strconv.ParseBool(enablePushLog2CollectorStr)
	logrus.Debugf("env %s: %s", EnvEnablePushLog2Collector, enablePushLog2CollectorStr)
	if !enablePushLog2Collector {
		return
	}
	agent.EasyUse.EnablePushLog2Collector = true

	// collector addr
	collectorAddr := os.Getenv(EnvCollectorAddr)
	logrus.Debugf("env %s: %s", EnvCollectorAddr, collectorAddr)
	if collectorAddr == "" {
		logrus.Warnf("missing env %s while enable push log to collector, skip push", EnvEnablePushLog2Collector)
		return
	}
	agent.EasyUse.CollectorAddr = collectorAddr
	agent.CallbackReporter.SetCollectorAddress(collectorAddr)

	// task log id
	taskLogID := os.Getenv(EnvTaskLogID)
	logrus.Debugf("env %s: %s", EnvTaskLogID, taskLogID)
	if taskLogID == "" {
		logrus.Warnf("missing taskLodID while enable push log to collector, skip push")
	}
	agent.EasyUse.TaskLogID = taskLogID
}
