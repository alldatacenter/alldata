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
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/tools/record"

	"github.com/koordinator-sh/koordinator/apis/extension"
)

const (
	testExtKey = "test-ext-key"
	testExtIF  = "test-ext-str"
)

type ManageNodeSLO struct{}

func (m *ManageNodeSLO) MergeNodeSLOExtension(oldCfg extension.ExtensionCfgMap, configMap *corev1.ConfigMap, recorder record.EventRecorder) (extension.ExtensionCfgMap, error) {
	newCfg := oldCfg.DeepCopy()
	if cfgIf, ok := configMap.Data[testExtKey]; ok {
		extensionCfg := extension.ExtensionCfg{ClusterStrategy: cfgIf}
		newCfg.Object[testExtKey] = extensionCfg
	}
	return *newCfg, nil
}

func (m *ManageNodeSLO) GetNodeSLOExtension(node *corev1.Node, cfgMap *extension.ExtensionCfgMap) (string, interface{}, error) {
	if cfg, ok := cfgMap.Object[testExtKey]; ok {
		return testExtKey, cfg.ClusterStrategy, nil
	}
	return testExtKey, nil, nil
}

func Test_NodeMergedExtender(t *testing.T) {
	t.Run("register extender", func(t *testing.T) {
		pluginName := "test-plugin-name"
		extender := &ManageNodeSLO{}
		if err := RegisterNodeSLOMergedExtender(pluginName, extender); err != nil {
			t.Errorf("RegisterNodeMergedExtender() error = %v", err)
		}
		configMap := &corev1.ConfigMap{
			Data: map[string]string{
				testExtKey: testExtIF,
			},
		}
		node := &corev1.Node{
			ObjectMeta: metav1.ObjectMeta{
				Labels: map[string]string{},
			},
		}
		cfgMap := extension.ExtensionCfgMap{}
		newCfg := calculateExtensionsCfgMerged(cfgMap, configMap, &record.FakeRecorder{})
		extMap := getExtensionsConfigSpec(node, &newCfg)
		gotIf := extMap.Object[testExtKey].(string)
		if gotIf != testExtIF {
			t.Errorf("run NodeMergedExtender got ext key %s, want %s", gotIf, testExtIF)
		}
		UnregisterNodeSLOMergedExtender(pluginName)
	})
}

func Test_RegistAlreadyExistNodeMergedExtender(t *testing.T) {
	t.Run("register extender", func(t *testing.T) {
		pluginName := "test-plugin-name"
		extender := &ManageNodeSLO{}
		err := RegisterNodeSLOMergedExtender(pluginName, extender)
		assert.NoError(t, err, "register first time")
		err1 := RegisterNodeSLOMergedExtender(pluginName, extender)
		assert.Error(t, err1, "register duplicate")
		UnregisterNodeSLOMergedExtender(pluginName)
	})
}
