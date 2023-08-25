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
	"fmt"
	"reflect"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/client-go/tools/record"
	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/apis/extension"
	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
)

var (
	globalNodeSLOMergedExtender = map[string]NodeSLOMergedPlugin{}
)

func RegisterNodeSLOMergedExtender(name string, extender NodeSLOMergedPlugin) error {
	if _, exist := globalNodeSLOMergedExtender[name]; exist {
		return fmt.Errorf("node merged plugin %s already exist", name)
	}
	globalNodeSLOMergedExtender[name] = extender
	return nil
}

func calculateExtensionsCfgMerged(oldCfgMap extension.ExtensionCfgMap, configMap *corev1.ConfigMap, recorder record.EventRecorder) extension.ExtensionCfgMap {
	oldCfgMapCopy := *oldCfgMap.DeepCopy()
	if oldCfgMapCopy.Object == nil {
		oldCfgMapCopy.Object = make(map[string]extension.ExtensionCfg)
	}
	mergedCfgMap := oldCfgMapCopy
	for name, extender := range globalNodeSLOMergedExtender {
		newCfgMap, err := extender.MergeNodeSLOExtension(mergedCfgMap, configMap, recorder)
		if err != nil {
			klog.Warningf("run merge nodeSLO extender %v failed, error %v", name, err)
			continue
		}
		mergedCfgMap = newCfgMap
		klog.V(5).Infof("run merge nodeSLO extender %v success, extensionCfg detail update %v", name, mergedCfgMap)
	}
	if reflect.DeepEqual(oldCfgMapCopy, mergedCfgMap) {
		return oldCfgMap
	}
	return mergedCfgMap
}

func getExtensionsConfigSpec(node *corev1.Node, cfgMap *extension.ExtensionCfgMap) *slov1alpha1.ExtensionsMap {
	extMap := slov1alpha1.ExtensionsMap{}
	if cfgMap == nil || cfgMap.Object == nil {
		return &extMap
	}
	for name, extender := range globalNodeSLOMergedExtender {
		extKey, extStrategy, err := extender.GetNodeSLOExtension(node, cfgMap)
		if err != nil {
			klog.Warningf("run get nodeSLO extender %v failed, error %v", name, err)
			continue
		}
		if extStrategy == nil {
			continue
		}
		if extMap.Object == nil {
			extMap.Object = make(map[string]interface{})
		}
		extMap.Object[extKey] = extStrategy
		klog.V(5).Infof("run get nodeSLO extender %v success, extMap %v", name, extMap)
	}
	return &extMap
}

func UnregisterNodeSLOMergedExtender(name string) {
	delete(globalNodeSLOMergedExtender, name)
}

type NodeSLOMergedPlugin interface {
	// calculate each extension cfg merged
	MergeNodeSLOExtension(oldCfgMap extension.ExtensionCfgMap, configMap *corev1.ConfigMap, recorder record.EventRecorder) (extension.ExtensionCfgMap, error)
	// get each extension config spec
	GetNodeSLOExtension(node *corev1.Node, cfg *extension.ExtensionCfgMap) (string, interface{}, error)
}
