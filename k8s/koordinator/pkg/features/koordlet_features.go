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

package features

import (
	"fmt"

	"k8s.io/apimachinery/pkg/util/runtime"
	"k8s.io/component-base/featuregate"

	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
)

const (
	// owner: @zwzhang0107
	// alpha: v0.1
	//
	// AuditEvents is used to audit recent events.
	AuditEvents featuregate.Feature = "AuditEvents"

	// owner: @zwzhang0107
	// alpha: v0.1
	//
	// AuditEventsHTTPHandler is used to get recent events from koordlet port.
	AuditEventsHTTPHandler featuregate.Feature = "AuditEventsHTTPHandler"

	// owner: @zwzhang0107 @saintube
	// alpha: v0.1
	// beta: v1.1
	//
	// BECPUSuppress suppresses cpuset for best-effort pod according to node cpu usage.
	BECPUSuppress featuregate.Feature = "BECPUSuppress"

	// owner: @zwzhang0107 @saintube
	// alpha: v0.4
	//
	// BECPUEvict evicts best-effort pod when they lack of resource.
	BECPUEvict featuregate.Feature = "BECPUEvict"

	// owner: @zwzhang0107 @saintube
	// alpha: v0.4
	//
	// BEMemoryEvict evict best-effort pod based on node memory usage.
	BEMemoryEvict featuregate.Feature = "BEMemoryEvict"

	// owner: @saintube @zwzhang0107
	// alpha: v0.2
	// beta: v1.1
	//
	// CPUBurst set cpu.cfs_burst_us; scale up cpu.cfs_quota_us if pod cpu throttled
	CPUBurst featuregate.Feature = "CPUBurst"

	// owner: @saintube @zwzhang0107
	// alpha: v0.3
	// beta: v1.1
	//
	// RdtResctrl sets intel rdt resctrl for processes belonging to ls or be pods
	RdtResctrl featuregate.Feature = "RdtResctrl"

	// owner: @saintube @zwzhang0107
	// alpha: v0.3
	//
	// CgroupReconcile reconciles qos config for resources like cpu, memory, disk, etc.
	// This will be divided into several independent features according to
	// https://github.com/koordinator-sh/koordinator/issues/174
	CgroupReconcile featuregate.Feature = "CgroupReconcile"

	// owner: @Joseph @zwzhang0107
	// alpha: v0.5
	// beta: v1.1
	//
	// NodeTopologyReport report node topology info to api-server through crd.
	NodeTopologyReport featuregate.Feature = "NodeTopologyReport"

	// owner: @jasonliu747 @Joseph
	// alpha: v0.6
	//
	// Accelerators enables GPU related feature in koordlet. Only Nvidia GPUs supported.
	Accelerators featuregate.Feature = "Accelerators"

	// owner: @songtao98 @zwzhang0107
	// alpha: v1.0
	//
	// CPICollector enables cpi collector feature of koordlet.
	CPICollector featuregate.Feature = "CPICollector"

	// owner: @songtao98 @zwzhang0107
	// alpha: v1.0
	//
	// PSICollector enables psi collector feature of koordlet.
	PSICollector featuregate.Feature = "PSICollector"
)

func init() {
	runtime.Must(DefaultMutableKoordletFeatureGate.Add(defaultKoordletFeatureGates))
}

var (
	DefaultMutableKoordletFeatureGate featuregate.MutableFeatureGate = featuregate.NewFeatureGate()
	DefaultKoordletFeatureGate        featuregate.FeatureGate        = DefaultMutableKoordletFeatureGate

	defaultKoordletFeatureGates = map[featuregate.Feature]featuregate.FeatureSpec{
		AuditEvents:            {Default: false, PreRelease: featuregate.Alpha},
		AuditEventsHTTPHandler: {Default: false, PreRelease: featuregate.Alpha},
		BECPUSuppress:          {Default: true, PreRelease: featuregate.Beta},
		BECPUEvict:             {Default: false, PreRelease: featuregate.Alpha},
		BEMemoryEvict:          {Default: false, PreRelease: featuregate.Alpha},
		CPUBurst:               {Default: true, PreRelease: featuregate.Beta},
		RdtResctrl:             {Default: true, PreRelease: featuregate.Beta},
		CgroupReconcile:        {Default: false, PreRelease: featuregate.Alpha},
		NodeTopologyReport:     {Default: true, PreRelease: featuregate.Beta},
		Accelerators:           {Default: false, PreRelease: featuregate.Alpha},
		CPICollector:           {Default: false, PreRelease: featuregate.Alpha},
		PSICollector:           {Default: false, PreRelease: featuregate.Alpha},
	}
)

// IsFeatureDisabled returns whether the featuregate is disabled by nodeSLO config
func IsFeatureDisabled(nodeSLO *slov1alpha1.NodeSLO, feature featuregate.Feature) (bool, error) {
	if nodeSLO == nil || nodeSLO.Spec == (slov1alpha1.NodeSLOSpec{}) {
		return true, fmt.Errorf("cannot parse feature config for invalid nodeSLO %v", nodeSLO)
	}

	spec := nodeSLO.Spec
	switch feature {
	case BECPUSuppress, BEMemoryEvict, BECPUEvict:
		if spec.ResourceUsedThresholdWithBE == nil || spec.ResourceUsedThresholdWithBE.Enable == nil {
			return true, fmt.Errorf("cannot parse feature config for invalid nodeSLO %v", nodeSLO)
		}
		return !(*spec.ResourceUsedThresholdWithBE.Enable), nil
	default:
		return true, fmt.Errorf("cannot parse feature config for unsupported feature %s", feature)
	}
}
