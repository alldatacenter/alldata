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
	"encoding/json"
	"testing"

	"github.com/stretchr/testify/assert"
	"k8s.io/utils/pointer"

	"github.com/koordinator-sh/koordinator/apis/extension"
)

type testExtensionsStruct struct {
	TestBoolVal *bool `json:"testBoolVal,omitempty"`
}

func Test_registerDefaultColocationExtension(t *testing.T) {
	testExtensionKey := "test-ext-key"

	testExtensionVal := &testExtensionsStruct{
		TestBoolVal: pointer.Bool(true),
	}
	t.Run("test register default colocation extension", func(t *testing.T) {
		err := RegisterDefaultColocationExtension(testExtensionKey, testExtensionVal)
		assert.NoError(t, err, "RegisterDefaultColocationExtension")
		defautlColocationCfg := DefaultColocationStrategy()
		configBytes, fmtErr := json.Marshal(defautlColocationCfg)
		configStr := string(configBytes)

		expectStr := "{\"enable\":false,\"metricAggregateDurationSeconds\":300,\"metricReportIntervalSeconds\":60," +
			"\"metricAggregatePolicy\":{\"durations\":[\"5m0s\",\"10m0s\",\"30m0s\"]}," +
			"\"cpuReclaimThresholdPercent\":60,\"memoryReclaimThresholdPercent\":65,\"memoryCalculatePolicy\":\"usage\"," +
			"\"degradeTimeMinutes\":15,\"updateTimeThresholdSeconds\":300,\"resourceDiffThreshold\":0.1," +
			"\"extensions\":{\"test-ext-key\":{\"testBoolVal\":true}}}"
		assert.Equal(t, expectStr, configStr, "config json")
		assert.NoError(t, fmtErr, "default colocation config marshall")

		gotVal, exist := defautlColocationCfg.Extensions[testExtensionKey]
		assert.True(t, exist, "key %v not exist in default config extensions", testExtensionKey)
		gotStruct, ok := gotVal.(*testExtensionsStruct)
		assert.True(t, ok, "*testExtensionsStruct convert is not ok")
		assert.Equal(t, testExtensionVal, gotStruct, "testExtensionsStruct not equal")
		UnregisterDefaultColocationExtension(testExtensionKey)
	})
}

func Test_registerAlreadyExistDefaultColocationExtension(t *testing.T) {
	testExtensionKey := "test-ext-key"

	testExtensionVal := &testExtensionsStruct{
		TestBoolVal: pointer.Bool(true),
	}
	t.Run("test register default colocation extension", func(t *testing.T) {
		err := RegisterDefaultColocationExtension(testExtensionKey, testExtensionVal)
		assert.NoError(t, err, "RegisterDefaultColocationExtension")
		err2 := RegisterDefaultColocationExtension(testExtensionKey, testExtensionVal)
		assert.Error(t, err2, "Register duplicate DefaultColocationExtension")
		UnregisterDefaultColocationExtension(testExtensionKey)
	})
}

func TestExtraFields_DeepCopy(t *testing.T) {
	type testExtStruct struct {
		TestBoolVal *bool
	}
	tests := []struct {
		name string
		in   extension.ExtraFields
		want *extension.ExtraFields
	}{
		{
			name: "deep copy struct",
			in: extension.ExtraFields{
				"test-ext-key": &testExtStruct{
					TestBoolVal: pointer.BoolPtr(true),
				},
			},
			want: &extension.ExtraFields{
				"test-ext-key": &testExtStruct{
					TestBoolVal: pointer.BoolPtr(true),
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := tt.in.DeepCopy()
			assert.Equal(t, tt.want, got, "deep copy should be equal")
		})
	}
}
