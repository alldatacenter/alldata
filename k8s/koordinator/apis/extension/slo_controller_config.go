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

package extension

import (
	"time"

	"github.com/mohae/deepcopy"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
)

const (
	// keys in the configmap
	ColocationConfigKey        = "colocation-config"
	ResourceThresholdConfigKey = "resource-threshold-config"
	ResourceQOSConfigKey       = "resource-qos-config"
	CPUBurstConfigKey          = "cpu-burst-config"
)

// +k8s:deepcopy-gen=true
type ColocationCfg struct {
	ColocationStrategy `json:",inline"`
	NodeConfigs        []NodeColocationCfg `json:"nodeConfigs,omitempty"`
}

// +k8s:deepcopy-gen=true
type NodeColocationCfg struct {
	NodeSelector *metav1.LabelSelector
	ColocationStrategy
}

// +k8s:deepcopy-gen=true
type ResourceThresholdCfg struct {
	ClusterStrategy *slov1alpha1.ResourceThresholdStrategy `json:"clusterStrategy,omitempty"`
	NodeStrategies  []NodeResourceThresholdStrategy        `json:"nodeStrategies,omitempty"`
}

// +k8s:deepcopy-gen=true
type NodeResourceThresholdStrategy struct {
	// an empty label selector matches all objects while a nil label selector matches no objects
	NodeSelector *metav1.LabelSelector `json:"nodeSelector,omitempty"`
	*slov1alpha1.ResourceThresholdStrategy
}

// +k8s:deepcopy-gen=true
type NodeCPUBurstCfg struct {
	// an empty label selector matches all objects while a nil label selector matches no objects
	NodeSelector *metav1.LabelSelector `json:"nodeSelector,omitempty"`
	*slov1alpha1.CPUBurstStrategy
}

// +k8s:deepcopy-gen=true
type CPUBurstCfg struct {
	ClusterStrategy *slov1alpha1.CPUBurstStrategy `json:"clusterStrategy,omitempty"`
	NodeStrategies  []NodeCPUBurstCfg             `json:"nodeStrategies,omitempty"`
}

// +k8s:deepcopy-gen=true
type ResourceQOSCfg struct {
	ClusterStrategy *slov1alpha1.ResourceQOSStrategy `json:"clusterStrategy,omitempty"`
	NodeStrategies  []NodeResourceQOSStrategy        `json:"nodeStrategies,omitempty"`
}

// +k8s:deepcopy-gen=true
type NodeResourceQOSStrategy struct {
	// an empty label selector matches all objects while a nil label selector matches no objects
	NodeSelector *metav1.LabelSelector `json:"nodeSelector,omitempty"`
	*slov1alpha1.ResourceQOSStrategy
}

type CalculatePolicy string

const (
	CalculateByPodUsage   CalculatePolicy = "usage"
	CalculateByPodRequest CalculatePolicy = "request"
)

// +k8s:deepcopy-gen=true
type ColocationStrategyExtender struct {
	Extensions ExtraFields `json:"extensions,omitempty"`
}

// +k8s:deepcopy-gen=false
type ExtraFields map[string]interface{}

func (in *ExtraFields) DeepCopyInto(out *ExtraFields) {
	if in == nil {
		return
	} else {
		outIf := deepcopy.Copy(*in)
		*out = outIf.(ExtraFields)
	}
}

func (in *ExtraFields) DeepCopy() *ExtraFields {
	if in == nil {
		return nil
	}
	out := new(ExtraFields)
	in.DeepCopyInto(out)
	return out
}

// +k8s:deepcopy-gen=true
type AggregatePolicy struct {
	Durations      []time.Duration               `json:"durations,omitempty"`
	StatisticTypes []slov1alpha1.AggregationType `json:"statisticTypes,omitempty"`
}

// +k8s:deepcopy-gen=true
type ColocationStrategy struct {
	Enable                         *bool                        `json:"enable,omitempty"`
	MetricAggregateDurationSeconds *int64                       `json:"metricAggregateDurationSeconds,omitempty"`
	MetricReportIntervalSeconds    *int64                       `json:"metricReportIntervalSeconds,omitempty"`
	MetricAggregatePolicy          *slov1alpha1.AggregatePolicy `json:"metricAggregatePolicy,omitempty"`
	CPUReclaimThresholdPercent     *int64                       `json:"cpuReclaimThresholdPercent,omitempty"`
	MemoryReclaimThresholdPercent  *int64                       `json:"memoryReclaimThresholdPercent,omitempty"`
	MemoryCalculatePolicy          *CalculatePolicy             `json:"memoryCalculatePolicy,omitempty"`
	DegradeTimeMinutes             *int64                       `json:"degradeTimeMinutes,omitempty"`
	UpdateTimeThresholdSeconds     *int64                       `json:"updateTimeThresholdSeconds,omitempty"`
	ResourceDiffThreshold          *float64                     `json:"resourceDiffThreshold,omitempty"`
	ColocationStrategyExtender     `json:",inline"`             // for third-party extension
}

/*
Koordinator uses configmap to manage the configuration of SLO, the configmap is stored in
 <ConfigNameSpace>/<SLOCtrlConfigMap>, with the following keys respectively:
   - <extension.ColocationConfigKey>
   - <ResourceThresholdConfigKey>
   - <ResourceQOSConfigKey>
   - <CPUBurstConfigKey>

et.

For example, the configmap is as follows:

```
apiVersion: v1
data:
  colocation-config: |
    {
      "enable": false,
      "metricAggregateDurationSeconds": 300,
      "metricReportIntervalSeconds": 60,
      "metricAggregatePolicy": {
        "durations": [
          {
            "duration": "5m"
          },
          {
            "duration": "10m"
          },
          {
            "duration": "15m"
          }
        ]
      },
      "cpuReclaimThresholdPercent": 60,
      "memoryReclaimThresholdPercent": 65,
      "memoryCalculatePolicy": "usage",
      "degradeTimeMinutes": 15,
      "updateTimeThresholdSeconds": 300,
      "resourceDiffThreshold": 0.1,
      "nodeConfigs": [
        {
          "nodeSelector": {
            "matchLabels": {
              "kubernetes.io/kernel": "alios"
            }
          },
          "updateTimeThresholdSeconds": 360,
          "resourceDiffThreshold": 0.2
        }
      ]
    }
  cpu-burst-config: |
    {
      "clusterStrategy": {
        "policy": "none",
        "cpuBurstPercent": 1000,
        "cfsQuotaBurstPercent": 300,
        "cfsQuotaBurstPeriodSeconds": -1,
        "sharePoolThresholdPercent": 50
      },
      "nodeStrategies": [
        {
          "nodeSelector": {
            "matchLabels": {
              "kubernetes.io/kernel": "alios"
            }
          },
          "policy": "cfsQuotaBurstOnly",
          "cfsQuotaBurstPercent": 400
        }
      ]
    }
  resource-qos-config: |
    {
      "clusterStrategy": {
        "lsrClass": {
          "cpuQOS": {
            "enable": false,
            "groupIdentity": 2
          },
          "memoryQOS": {
            "enable": false,
            "minLimitPercent": 0,
            "lowLimitPercent": 0,
            "throttlingPercent": 0,
            "wmarkRatio": 95,
            "wmarkScalePermill": 20,
            "wmarkMinAdj": -25,
            "priorityEnable": 0,
            "priority": 0,
            "oomKillGroup": 0
          },
          "resctrlQOS": {
            "enable": false,
            "catRangeStartPercent": 0,
            "catRangeEndPercent": 100,
            "mbaPercent": 100
          }
        },
        "lsClass": {
          "cpuQOS": {
            "enable": false,
            "groupIdentity": 2
          },
          "memoryQOS": {
            "enable": false,
            "minLimitPercent": 0,
            "lowLimitPercent": 0,
            "throttlingPercent": 0,
            "wmarkRatio": 95,
            "wmarkScalePermill": 20,
            "wmarkMinAdj": -25,
            "priorityEnable": 0,
            "priority": 0,
            "oomKillGroup": 0
          },
          "resctrlQOS": {
            "enable": false,
            "catRangeStartPercent": 0,
            "catRangeEndPercent": 100,
            "mbaPercent": 100
          }
        },
        "beClass": {
          "cpuQOS": {
            "enable": false,
            "groupIdentity": -1
          },
          "memoryQOS": {
            "enable": false,
            "minLimitPercent": 0,
            "lowLimitPercent": 0,
            "throttlingPercent": 0,
            "wmarkRatio": 95,
            "wmarkScalePermill": 20,
            "wmarkMinAdj": 50,
            "priorityEnable": 0,
            "priority": 0,
            "oomKillGroup": 0
          },
          "resctrlQOS": {
            "enable": false,
            "catRangeStartPercent": 0,
            "catRangeEndPercent": 30,
            "mbaPercent": 100
          }
        }
      },
      "nodeStrategies": [
        {
          "nodeSelector": {
            "matchLabels": {
              "kubernetes.io/kernel": "alios"
            }
          },
          "beClass": {
            "memoryQOS": {
              "wmarkRatio": 90
            }
          }
        }
      ]
    }
  resource-threshold-config: |
    {
      "clusterStrategy": {
        "enable": false,
        "cpuSuppressThresholdPercent": 65,
        "cpuSuppressPolicy": "cpuset",
        "memoryEvictThresholdPercent": 70
      },
      "nodeStrategies": [
        {
          "nodeSelector": {
            "matchLabels": {
              "kubernetes.io/kernel": "alios"
            }
          },
          "cpuEvictBEUsageThresholdPercent": 80
        }
      ]
    }
kind: ConfigMap
metadata:
  annotations:
    meta.helm.sh/release-name: koordinator
    meta.helm.sh/release-namespace: default
  labels:
    app.kubernetes.io/managed-by: Helm
  name: slo-controller-config
  namespace: koordinator-system
```
*/
