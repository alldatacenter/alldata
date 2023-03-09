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

package nodeslo

import (
	"encoding/json"

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/labels"
	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/apis/extension"
	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

func getResourceThresholdSpec(node *corev1.Node, cfg *extension.ResourceThresholdCfg) (*slov1alpha1.ResourceThresholdStrategy, error) {
	nodeLabels := labels.Set(node.Labels)
	for _, nodeStrategy := range cfg.NodeStrategies {
		selector, err := metav1.LabelSelectorAsSelector(nodeStrategy.NodeSelector)
		if err != nil {
			klog.Errorf("failed to parse node selector %v, err: %v", nodeStrategy.NodeSelector, err)
			continue
		}
		if selector.Matches(nodeLabels) {
			return nodeStrategy.ResourceThresholdStrategy.DeepCopy(), nil
		}
	}

	return cfg.ClusterStrategy.DeepCopy(), nil
}

func getResourceQOSSpec(node *corev1.Node, cfg *extension.ResourceQOSCfg) (*slov1alpha1.ResourceQOSStrategy, error) {
	nodeLabels := labels.Set(node.Labels)
	for _, nodeStrategy := range cfg.NodeStrategies {
		selector, err := metav1.LabelSelectorAsSelector(nodeStrategy.NodeSelector)
		if err != nil {
			klog.Errorf("failed to parse node selector %v, err: %v", nodeStrategy.NodeSelector, err)
			continue
		}
		if selector.Matches(nodeLabels) {
			return nodeStrategy.ResourceQOSStrategy.DeepCopy(), nil
		}
	}

	return cfg.ClusterStrategy.DeepCopy(), nil
}

func getCPUBurstConfigSpec(node *corev1.Node, cfg *extension.CPUBurstCfg) (*slov1alpha1.CPUBurstStrategy, error) {

	nodeLabels := labels.Set(node.Labels)
	for _, nodeStrategy := range cfg.NodeStrategies {
		selector, err := metav1.LabelSelectorAsSelector(nodeStrategy.NodeSelector)
		if err != nil {
			klog.Errorf("failed to parse node selector %v, err: %v", nodeStrategy.NodeSelector, err)
			continue
		}
		if selector.Matches(nodeLabels) {
			return nodeStrategy.CPUBurstStrategy.DeepCopy(), nil
		}

	}
	return cfg.ClusterStrategy.DeepCopy(), nil
}

func calculateResourceThresholdCfgMerged(oldCfg extension.ResourceThresholdCfg, configMap *corev1.ConfigMap) (extension.ResourceThresholdCfg, error) {
	cfgStr, ok := configMap.Data[extension.ResourceThresholdConfigKey]
	if !ok {
		return DefaultSLOCfg().ThresholdCfgMerged, nil
	}

	mergedCfg := extension.ResourceThresholdCfg{}
	if err := json.Unmarshal([]byte(cfgStr), &mergedCfg); err != nil {
		klog.Errorf("failed to unmarshal config %s, err: %s", extension.ResourceThresholdConfigKey, err)
		return oldCfg, err
	}

	// merge ClusterStrategy
	clusterMerged := DefaultSLOCfg().ThresholdCfgMerged.ClusterStrategy.DeepCopy()
	if mergedCfg.ClusterStrategy != nil {
		mergedStrategyInterface, _ := util.MergeCfg(clusterMerged, mergedCfg.ClusterStrategy)
		clusterMerged = mergedStrategyInterface.(*slov1alpha1.ResourceThresholdStrategy)
	}
	mergedCfg.ClusterStrategy = clusterMerged

	for index, nodeStrategy := range mergedCfg.NodeStrategies {
		// merge with clusterStrategy
		clusterCfgCopy := mergedCfg.ClusterStrategy.DeepCopy()
		if nodeStrategy.ResourceThresholdStrategy != nil {
			mergedNodeStrategyInterface, _ := util.MergeCfg(clusterCfgCopy, nodeStrategy.ResourceThresholdStrategy)
			mergedCfg.NodeStrategies[index].ResourceThresholdStrategy = mergedNodeStrategyInterface.(*slov1alpha1.ResourceThresholdStrategy)
		} else {
			mergedCfg.NodeStrategies[index].ResourceThresholdStrategy = clusterCfgCopy
		}

	}

	return mergedCfg, nil
}

func calculateResourceQOSCfgMerged(oldCfg extension.ResourceQOSCfg, configMap *corev1.ConfigMap) (extension.ResourceQOSCfg, error) {
	cfgStr, ok := configMap.Data[extension.ResourceQOSConfigKey]
	if !ok {
		return DefaultSLOCfg().ResourceQOSCfgMerged, nil
	}

	mergedCfg := DefaultSLOCfg().ResourceQOSCfgMerged
	if err := json.Unmarshal([]byte(cfgStr), &mergedCfg); err != nil {
		klog.Errorf("failed to unmarshal config %s, err: %s", extension.ResourceQOSConfigKey, err)
		return oldCfg, err
	}

	// merge ClusterStrategy
	clusterMerged := DefaultSLOCfg().ResourceQOSCfgMerged.ClusterStrategy.DeepCopy()
	if mergedCfg.ClusterStrategy != nil {
		mergedStrategyInterface, _ := util.MergeCfg(clusterMerged, mergedCfg.ClusterStrategy)
		clusterMerged = mergedStrategyInterface.(*slov1alpha1.ResourceQOSStrategy)
	}
	mergedCfg.ClusterStrategy = clusterMerged

	for index, nodeStrategy := range mergedCfg.NodeStrategies {
		// merge with clusterStrategy
		var mergedNodeStrategy *slov1alpha1.ResourceQOSStrategy
		clusterCfgCopy := mergedCfg.ClusterStrategy.DeepCopy()
		if nodeStrategy.ResourceQOSStrategy != nil {
			mergedStrategyInterface, _ := util.MergeCfg(clusterCfgCopy, nodeStrategy.ResourceQOSStrategy)
			mergedNodeStrategy = mergedStrategyInterface.(*slov1alpha1.ResourceQOSStrategy)
		} else {
			mergedNodeStrategy = clusterCfgCopy
		}
		mergedCfg.NodeStrategies[index].ResourceQOSStrategy = mergedNodeStrategy

	}

	return mergedCfg, nil
}

func calculateCPUBurstCfgMerged(oldCfg extension.CPUBurstCfg, configMap *corev1.ConfigMap) (extension.CPUBurstCfg, error) {
	cfgStr, ok := configMap.Data[extension.CPUBurstConfigKey]
	if !ok {
		return DefaultSLOCfg().CPUBurstCfgMerged, nil
	}

	mergedCfg := extension.CPUBurstCfg{}
	if err := json.Unmarshal([]byte(cfgStr), &mergedCfg); err != nil {
		klog.Errorf("failed to unmarshal config %s, err: %s", extension.CPUBurstConfigKey, err)
		return oldCfg, err
	}

	// merge ClusterStrategy
	clusterMerged := DefaultSLOCfg().CPUBurstCfgMerged.ClusterStrategy.DeepCopy()
	if mergedCfg.ClusterStrategy != nil {
		mergedStrategyInterface, _ := util.MergeCfg(clusterMerged, mergedCfg.ClusterStrategy)
		clusterMerged = mergedStrategyInterface.(*slov1alpha1.CPUBurstStrategy)
	}
	mergedCfg.ClusterStrategy = clusterMerged

	for index, nodeStrategy := range mergedCfg.NodeStrategies {
		// merge with clusterStrategy
		clusterCfgCopy := mergedCfg.ClusterStrategy.DeepCopy()
		if nodeStrategy.CPUBurstStrategy != nil {
			mergedStrategyInterface, _ := util.MergeCfg(clusterCfgCopy, nodeStrategy.CPUBurstStrategy)
			mergedCfg.NodeStrategies[index].CPUBurstStrategy = mergedStrategyInterface.(*slov1alpha1.CPUBurstStrategy)
		} else {
			mergedCfg.NodeStrategies[index].CPUBurstStrategy = clusterCfgCopy
		}

	}

	return mergedCfg, nil
}
