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
	"testing"

	"github.com/stretchr/testify/assert"
	"k8s.io/utils/pointer"

	"github.com/koordinator-sh/koordinator/apis/extension"
	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
)

func getDefaultExtensionStrategy() *slov1alpha1.ExtensionsMap {
	defaultCfg := getDefaultExtensionCfg()
	defaultStrategy := slov1alpha1.ExtensionsMap{}
	if defaultCfg == nil || defaultCfg.Object == nil {
		return &defaultStrategy
	}
	defaultStrategy.Object = make(map[string]interface{}, len(defaultCfg.Object))
	for key, cfg := range defaultCfg.Object {
		defaultStrategy.Object[key] = cfg.ClusterStrategy
	}
	return &defaultStrategy
}

type testExtensionsStrategyStruct struct {
	TestBoolVal *bool `json:"testBoolVal,omitempty"`
}

func Test_registerDefaultExtension(t *testing.T) {
	testExtensionKey := "test-ext-key"

	testExtensionVal := &testExtensionsStrategyStruct{
		TestBoolVal: pointer.Bool(true),
	}
	t.Run("test register default colocation extension", func(t *testing.T) {
		err := RegisterDefaultExtension(testExtensionKey, testExtensionVal)
		assert.NoError(t, err, "RegisterDefaultExtension")
		defaultStrategy := getDefaultExtensionStrategy()
		strategtBytes, fmtErr := json.Marshal(defaultStrategy)
		strategyStr := string(strategtBytes)
		expectStr := "\"test-ext-key\":{\"testBoolVal\":true}"
		assert.Contains(t, strategyStr, expectStr, "marshall testing extension strategy")
		assert.NoError(t, fmtErr, "default straregy marshall")

		defaultCfg := getDefaultExtensionCfg()
		configBytes, fmtErr := json.Marshal(defaultCfg)
		configStr := string(configBytes)
		expectStr = "\"test-ext-key\":{\"clusterStrategy\":{\"testBoolVal\":true}}"
		assert.Contains(t, configStr, expectStr, "marshall testing extension config")
		assert.NoError(t, fmtErr, "default config marshall")

		gotStrategyVal, exist := defaultStrategy.Object[testExtensionKey]
		assert.True(t, exist, "key %v not exist in default strategy extensions", testExtensionKey)
		gotStrategyStruct, ok := gotStrategyVal.(*testExtensionsStrategyStruct)
		assert.True(t, ok, "*testExtensionsStrategyStruct convert is not ok")
		assert.Equal(t, testExtensionVal, gotStrategyStruct, "testExtensionsStrategyStruct not equal")

		gotConfigVal, exist := defaultCfg.Object[testExtensionKey]
		assert.True(t, exist, "key %v not exist in default config extensions", testExtensionKey)
		gotConfigStruct, ok := gotConfigVal.ClusterStrategy.(*testExtensionsStrategyStruct)
		assert.True(t, ok, "*testExtensionsConfigStruct convert is not ok")
		assert.Equal(t, testExtensionVal, gotConfigStruct, "testExtensionsConfigStruct not equal")

		UnregisterDefaultExtension(testExtensionKey)
	})
}

func Test_registerAlreadyExistDefaultExtension(t *testing.T) {
	testExtensionKey := "test-ext-key"

	testExtensionVal := &testExtensionsStrategyStruct{
		TestBoolVal: pointer.Bool(true),
	}
	t.Run("test register default extension", func(t *testing.T) {
		err := RegisterDefaultExtension(testExtensionKey, testExtensionVal)
		assert.NoError(t, err, "RegisterDefaultExtension")
		err2 := RegisterDefaultExtension(testExtensionKey, testExtensionVal)
		assert.Error(t, err2, "Register duplicate DefaultExtension")
		UnregisterDefaultExtension(testExtensionKey)
	})
}

func TestExtensionsCfgMap_DeepCopy(t *testing.T) {
	type testExtStruct struct {
		TestBoolVal *bool
	}
	testExtensionCfg := extension.ExtensionCfg{ClusterStrategy: testExtStruct{
		TestBoolVal: pointer.BoolPtr(true),
	}}
	testingExtensionCfgMap := extension.ExtensionCfgMap{}
	testingExtensionCfgMap.Object = map[string]extension.ExtensionCfg{
		"test-ext-key": testExtensionCfg,
	}
	tests := []struct {
		name string
		in   extension.ExtensionCfgMap
		want *extension.ExtensionCfgMap
	}{
		{
			name: "deep copy struct",
			in:   *testingExtensionCfgMap.DeepCopy(),
			want: testingExtensionCfgMap.DeepCopy(),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := tt.in.DeepCopy()
			assert.Equal(t, tt.want, got, "deep copy should be equal")
		})
	}
}
