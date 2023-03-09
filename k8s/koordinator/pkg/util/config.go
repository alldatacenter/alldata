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

package util

import (
	"k8s.io/klog/v2"
	"k8s.io/utils/pointer"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
)

// DefaultNodeSLOSpecConfig defines the default config of the nodeSLOSpec, which would be used by the resmgr
func DefaultNodeSLOSpecConfig() slov1alpha1.NodeSLOSpec {
	return slov1alpha1.NodeSLOSpec{
		ResourceUsedThresholdWithBE: DefaultResourceThresholdStrategy(),
		ResourceQOSStrategy:         DefaultResourceQOSStrategy(),
		CPUBurstStrategy:            DefaultCPUBurstStrategy(),
	}
}

func DefaultResourceThresholdStrategy() *slov1alpha1.ResourceThresholdStrategy {
	return &slov1alpha1.ResourceThresholdStrategy{
		Enable:                      pointer.BoolPtr(false),
		CPUSuppressThresholdPercent: pointer.Int64Ptr(65),
		CPUSuppressPolicy:           slov1alpha1.CPUSetPolicy,
		MemoryEvictThresholdPercent: pointer.Int64Ptr(70),
	}
}

func DefaultCPUQOS(qos apiext.QoSClass) *slov1alpha1.CPUQOS {
	var cpuQOS *slov1alpha1.CPUQOS
	switch qos {
	case apiext.QoSLSR:
		cpuQOS = &slov1alpha1.CPUQOS{
			GroupIdentity: pointer.Int64Ptr(2),
		}
	case apiext.QoSLS:
		cpuQOS = &slov1alpha1.CPUQOS{
			GroupIdentity: pointer.Int64Ptr(2),
		}
	case apiext.QoSBE:
		cpuQOS = &slov1alpha1.CPUQOS{
			GroupIdentity: pointer.Int64Ptr(-1),
		}
	default:
		klog.Infof("cpu qos has no auto config for qos %s", qos)
	}
	return cpuQOS
}

// TODO https://github.com/koordinator-sh/koordinator/pull/94#discussion_r858786733
func DefaultResctrlQOS(qos apiext.QoSClass) *slov1alpha1.ResctrlQOS {
	var resctrlQOS *slov1alpha1.ResctrlQOS
	switch qos {
	case apiext.QoSLSR:
		resctrlQOS = &slov1alpha1.ResctrlQOS{
			CATRangeStartPercent: pointer.Int64Ptr(0),
			CATRangeEndPercent:   pointer.Int64Ptr(100),
			MBAPercent:           pointer.Int64Ptr(100),
		}
	case apiext.QoSLS:
		resctrlQOS = &slov1alpha1.ResctrlQOS{
			CATRangeStartPercent: pointer.Int64Ptr(0),
			CATRangeEndPercent:   pointer.Int64Ptr(100),
			MBAPercent:           pointer.Int64Ptr(100),
		}
	case apiext.QoSBE:
		resctrlQOS = &slov1alpha1.ResctrlQOS{
			CATRangeStartPercent: pointer.Int64Ptr(0),
			CATRangeEndPercent:   pointer.Int64Ptr(30),
			MBAPercent:           pointer.Int64Ptr(100),
		}
	default:
		klog.Infof("resctrl qos has no auto config for qos %s", qos)
	}
	return resctrlQOS
}

// DefaultMemoryQOS returns the recommended configuration for memory qos strategy.
// Please refer to `apis/slo/v1alpha1` for the definition of each field.
// In the recommended configuration, all abilities of memcg qos are disable, including `MinLimitPercent`,
// `LowLimitPercent`, `ThrottlingPercent` since they are not fully beneficial to all scenarios. Whereas, they are still
// useful when the use case is determined. e.g. lock some memory to improve file read performance.
// Asynchronous memory reclaim is enabled by default to alleviate the direct reclaim pressure, including `WmarkRatio`
// and `WmarkScalePermill`. The watermark of async reclaim is not recommended to set too low, since lower the watermark
// the more excess reclamations.
// Memory min watermark grading corresponding to `WmarkMinAdj` is enabled. It benefits high-priority pods by postponing
// global reclaim when machine's free memory is below than `/proc/sys/vm/min_free_kbytes`.
func DefaultMemoryQOS(qos apiext.QoSClass) *slov1alpha1.MemoryQOS {
	var memoryQOS *slov1alpha1.MemoryQOS
	switch qos {
	case apiext.QoSLSR:
		memoryQOS = &slov1alpha1.MemoryQOS{
			MinLimitPercent:   pointer.Int64Ptr(0),
			LowLimitPercent:   pointer.Int64Ptr(0),
			ThrottlingPercent: pointer.Int64Ptr(0),
			WmarkRatio:        pointer.Int64Ptr(95),
			WmarkScalePermill: pointer.Int64Ptr(20),
			WmarkMinAdj:       pointer.Int64Ptr(-25),
			PriorityEnable:    pointer.Int64Ptr(0),
			Priority:          pointer.Int64Ptr(0),
			OomKillGroup:      pointer.Int64Ptr(0),
		}
	case apiext.QoSLS:
		memoryQOS = &slov1alpha1.MemoryQOS{
			MinLimitPercent:   pointer.Int64Ptr(0),
			LowLimitPercent:   pointer.Int64Ptr(0),
			ThrottlingPercent: pointer.Int64Ptr(0),
			WmarkRatio:        pointer.Int64Ptr(95),
			WmarkScalePermill: pointer.Int64Ptr(20),
			WmarkMinAdj:       pointer.Int64Ptr(-25),
			PriorityEnable:    pointer.Int64Ptr(0),
			Priority:          pointer.Int64Ptr(0),
			OomKillGroup:      pointer.Int64Ptr(0),
		}
	case apiext.QoSBE:
		memoryQOS = &slov1alpha1.MemoryQOS{
			MinLimitPercent:   pointer.Int64Ptr(0),
			LowLimitPercent:   pointer.Int64Ptr(0),
			ThrottlingPercent: pointer.Int64Ptr(0),
			WmarkRatio:        pointer.Int64Ptr(95),
			WmarkScalePermill: pointer.Int64Ptr(20),
			WmarkMinAdj:       pointer.Int64Ptr(50),
			PriorityEnable:    pointer.Int64Ptr(0),
			Priority:          pointer.Int64Ptr(0),
			OomKillGroup:      pointer.Int64Ptr(0),
		}
	default:
		klog.V(5).Infof("memory qos has no auto config for qos %s", qos)
	}
	return memoryQOS
}

func DefaultResourceQOSStrategy() *slov1alpha1.ResourceQOSStrategy {
	return &slov1alpha1.ResourceQOSStrategy{
		LSRClass: &slov1alpha1.ResourceQOS{
			CPUQOS: &slov1alpha1.CPUQOSCfg{
				Enable: pointer.BoolPtr(false),
				CPUQOS: *DefaultCPUQOS(apiext.QoSLSR),
			},
			ResctrlQOS: &slov1alpha1.ResctrlQOSCfg{
				Enable:     pointer.BoolPtr(false),
				ResctrlQOS: *DefaultResctrlQOS(apiext.QoSLSR),
			},
			MemoryQOS: &slov1alpha1.MemoryQOSCfg{
				Enable:    pointer.BoolPtr(false),
				MemoryQOS: *DefaultMemoryQOS(apiext.QoSLSR),
			},
		},
		LSClass: &slov1alpha1.ResourceQOS{
			CPUQOS: &slov1alpha1.CPUQOSCfg{
				Enable: pointer.BoolPtr(false),
				CPUQOS: *DefaultCPUQOS(apiext.QoSLS),
			},
			ResctrlQOS: &slov1alpha1.ResctrlQOSCfg{
				Enable:     pointer.BoolPtr(false),
				ResctrlQOS: *DefaultResctrlQOS(apiext.QoSLS),
			},
			MemoryQOS: &slov1alpha1.MemoryQOSCfg{
				Enable:    pointer.BoolPtr(false),
				MemoryQOS: *DefaultMemoryQOS(apiext.QoSLS),
			},
		},
		BEClass: &slov1alpha1.ResourceQOS{
			CPUQOS: &slov1alpha1.CPUQOSCfg{
				Enable: pointer.BoolPtr(false),
				CPUQOS: *DefaultCPUQOS(apiext.QoSBE),
			},
			ResctrlQOS: &slov1alpha1.ResctrlQOSCfg{
				Enable:     pointer.BoolPtr(false),
				ResctrlQOS: *DefaultResctrlQOS(apiext.QoSBE),
			},
			MemoryQOS: &slov1alpha1.MemoryQOSCfg{
				Enable:    pointer.BoolPtr(false),
				MemoryQOS: *DefaultMemoryQOS(apiext.QoSBE),
			},
		},
	}
}

func NoneResourceQOS(qos apiext.QoSClass) *slov1alpha1.ResourceQOS {
	return &slov1alpha1.ResourceQOS{
		CPUQOS: &slov1alpha1.CPUQOSCfg{
			Enable: pointer.BoolPtr(false),
			CPUQOS: *NoneCPUQOS(),
		},
		ResctrlQOS: &slov1alpha1.ResctrlQOSCfg{
			Enable:     pointer.BoolPtr(false),
			ResctrlQOS: *NoneResctrlQOS(),
		},
		MemoryQOS: &slov1alpha1.MemoryQOSCfg{
			Enable:    pointer.BoolPtr(false),
			MemoryQOS: *NoneMemoryQOS(),
		},
	}
}

func NoneCPUQOS() *slov1alpha1.CPUQOS {
	return &slov1alpha1.CPUQOS{
		GroupIdentity: pointer.Int64(0),
	}
}

func NoneResctrlQOS() *slov1alpha1.ResctrlQOS {
	return &slov1alpha1.ResctrlQOS{
		CATRangeStartPercent: pointer.Int64Ptr(0),
		CATRangeEndPercent:   pointer.Int64Ptr(100),
		MBAPercent:           pointer.Int64Ptr(100),
	}
}

// NoneMemoryQOS returns the all-disabled configuration for memory qos strategy.
func NoneMemoryQOS() *slov1alpha1.MemoryQOS {
	return &slov1alpha1.MemoryQOS{
		MinLimitPercent:   pointer.Int64Ptr(0),
		LowLimitPercent:   pointer.Int64Ptr(0),
		ThrottlingPercent: pointer.Int64Ptr(0),
		WmarkRatio:        pointer.Int64Ptr(0),
		WmarkScalePermill: pointer.Int64Ptr(50),
		WmarkMinAdj:       pointer.Int64Ptr(0),
		PriorityEnable:    pointer.Int64Ptr(0),
		Priority:          pointer.Int64Ptr(0),
		OomKillGroup:      pointer.Int64Ptr(0),
	}
}

// NoneResourceQOSStrategy indicates the qos strategy with all qos
func NoneResourceQOSStrategy() *slov1alpha1.ResourceQOSStrategy {
	return &slov1alpha1.ResourceQOSStrategy{
		LSRClass: NoneResourceQOS(apiext.QoSLSR),
		LSClass:  NoneResourceQOS(apiext.QoSLS),
		BEClass:  NoneResourceQOS(apiext.QoSBE),
	}
}

func DefaultCPUBurstStrategy() *slov1alpha1.CPUBurstStrategy {
	return &slov1alpha1.CPUBurstStrategy{
		CPUBurstConfig:            DefaultCPUBurstConfig(),
		SharePoolThresholdPercent: pointer.Int64Ptr(50),
	}
}

func DefaultCPUBurstConfig() slov1alpha1.CPUBurstConfig {
	return slov1alpha1.CPUBurstConfig{
		Policy:                     slov1alpha1.CPUBurstNone,
		CPUBurstPercent:            pointer.Int64Ptr(1000),
		CFSQuotaBurstPercent:       pointer.Int64Ptr(300),
		CFSQuotaBurstPeriodSeconds: pointer.Int64Ptr(-1),
	}
}
