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

package system

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestCgroupResourceRegistry(t *testing.T) {
	t.Run("", func(t *testing.T) {
		r, ok := DefaultRegistry.Get(CgroupVersionV1, CPUCFSQuotaName)
		assert.True(t, ok)
		cgroup, ok := r.(*CgroupResource)
		assert.True(t, ok)
		assert.Equal(t, cgroup.FileName, CPUCFSQuotaName)

		r, ok = DefaultRegistry.Get(CgroupVersionV2, CPUCFSQuotaName)
		assert.True(t, ok)
		cgroup, ok = r.(*CgroupResource)
		assert.True(t, ok)
		assert.Equal(t, cgroup.FileName, CPUMaxName)
	})
}

func TestCgroupResource(t *testing.T) {
	type fields struct {
		isV2             bool
		checkSupportedFn func(r Resource, parentDir string) (bool, string)
		filename         string
		subfs            string
		resourceType     ResourceType
	}
	tests := []struct {
		name          string
		fields        fields
		wantSupported bool
	}{
		{
			name: "resource must be supported",
			fields: fields{
				isV2:         false,
				filename:     "cpu.my_test_cgroup",
				subfs:        CgroupCPUDir,
				resourceType: ResourceType("cpu.my_test_cgroup"),
			},
			wantSupported: true,
		},
		{
			name: "v2 resource must be supported",
			fields: fields{
				isV2:         true,
				filename:     "cpu.my_test_cgroup",
				subfs:        CgroupV2Dir,
				resourceType: ResourceType("cpu.my_test_cgroup"),
			},
			wantSupported: true,
		},
		{
			name: "resource not supported",
			fields: fields{
				isV2:             false,
				checkSupportedFn: SupportedIfFileExists,
				filename:         "memory.my_test_cgroup",
				subfs:            CgroupMemDir,
				resourceType:     ResourceType("memory.my_test_cgroup"),
			},
			wantSupported: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			testHelper := NewFileTestUtil(t)
			defer testHelper.Cleanup()

			var r Resource
			if tt.fields.isV2 {
				r = DefaultFactory.NewV2(tt.fields.resourceType, tt.fields.filename)
			} else {
				r = DefaultFactory.New(tt.fields.filename, tt.fields.subfs)
			}
			if tt.fields.checkSupportedFn != nil {
				r.WithCheckSupported(tt.fields.checkSupportedFn)
			}
			assert.Equal(t, tt.fields.resourceType, r.ResourceType())
			isValid, validMsg := r.IsValid("")
			assert.True(t, isValid)
			assert.Equal(t, "", validMsg)
			isSupported, _ := r.IsSupported("")
			assert.Equal(t, tt.wantSupported, isSupported)
		})
	}
}
