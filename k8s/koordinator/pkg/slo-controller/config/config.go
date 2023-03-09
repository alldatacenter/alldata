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

package config

import (
	"flag"
	"reflect"
	"time"

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/labels"
	"k8s.io/utils/pointer"

	"github.com/koordinator-sh/koordinator/apis/extension"
	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

var (
	// SLO configmap name
	ConfigNameSpace  = "koordinator-system"
	SLOCtrlConfigMap = "slo-controller-config"
)

func InitFlags(fs *flag.FlagSet) {
	fs.StringVar(&SLOCtrlConfigMap, "slo-config-name", SLOCtrlConfigMap, "determines the name the slo-controller configmap uses.")
	fs.StringVar(&ConfigNameSpace, "config-namespace", ConfigNameSpace, "determines the namespace of configmap uses.")
}

func NewDefaultColocationCfg() *extension.ColocationCfg {
	defaultCfg := DefaultColocationCfg()
	return &defaultCfg
}

func DefaultColocationCfg() extension.ColocationCfg {
	return extension.ColocationCfg{
		ColocationStrategy: DefaultColocationStrategy(),
	}
}

func DefaultColocationStrategy() extension.ColocationStrategy {
	calculatePolicy := extension.CalculateByPodUsage
	cfg := extension.ColocationStrategy{
		Enable:                         pointer.Bool(false),
		MetricAggregateDurationSeconds: pointer.Int64(300),
		MetricReportIntervalSeconds:    pointer.Int64(60),
		MetricAggregatePolicy: &slov1alpha1.AggregatePolicy{
			Durations: []metav1.Duration{
				{Duration: 5 * time.Minute},
				{Duration: 10 * time.Minute},
				{Duration: 30 * time.Minute},
			},
		},
		CPUReclaimThresholdPercent:    pointer.Int64(60),
		MemoryReclaimThresholdPercent: pointer.Int64(65),
		MemoryCalculatePolicy:         &calculatePolicy,
		DegradeTimeMinutes:            pointer.Int64(15),
		UpdateTimeThresholdSeconds:    pointer.Int64(300),
		ResourceDiffThreshold:         pointer.Float64(0.1),
	}
	cfg.ColocationStrategyExtender = defaultColocationStrategyExtender
	return cfg
}

func IsColocationStrategyValid(strategy *extension.ColocationStrategy) bool {
	return strategy != nil &&
		(strategy.MetricAggregateDurationSeconds == nil || *strategy.MetricAggregateDurationSeconds > 0) &&
		(strategy.MetricReportIntervalSeconds == nil || *strategy.MetricReportIntervalSeconds > 0) &&
		(strategy.CPUReclaimThresholdPercent == nil || *strategy.CPUReclaimThresholdPercent > 0) &&
		(strategy.MemoryReclaimThresholdPercent == nil || *strategy.MemoryReclaimThresholdPercent > 0) &&
		(strategy.DegradeTimeMinutes == nil || *strategy.DegradeTimeMinutes > 0) &&
		(strategy.UpdateTimeThresholdSeconds == nil || *strategy.UpdateTimeThresholdSeconds > 0) &&
		(strategy.ResourceDiffThreshold == nil || *strategy.ResourceDiffThreshold > 0)
}

func IsNodeColocationCfgValid(nodeCfg *extension.NodeColocationCfg) bool {
	if nodeCfg == nil {
		return false
	}
	if nodeCfg.NodeSelector.MatchLabels == nil {
		return false
	}
	if _, err := metav1.LabelSelectorAsSelector(nodeCfg.NodeSelector); err != nil {
		return false
	}
	// node colocation should not be empty
	return !reflect.DeepEqual(&nodeCfg.ColocationStrategy, &extension.ColocationStrategy{})
}

func GetNodeColocationStrategy(cfg *extension.ColocationCfg, node *corev1.Node) *extension.ColocationStrategy {
	if cfg == nil || node == nil {
		return nil
	}

	strategy := cfg.ColocationStrategy.DeepCopy()

	nodeLabels := labels.Set(node.Labels)
	for _, nodeCfg := range cfg.NodeConfigs {
		selector, err := metav1.LabelSelectorAsSelector(nodeCfg.NodeSelector)
		if err != nil || !selector.Matches(nodeLabels) {
			continue
		}

		merged, err := util.MergeCfg(strategy, &nodeCfg.ColocationStrategy)
		if err != nil {
			continue
		}

		strategy, _ = merged.(*extension.ColocationStrategy)
		break
	}

	return strategy
}
