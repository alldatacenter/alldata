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

package resourceexecutor

import (
	"math"
	"path/filepath"
	"testing"

	"github.com/stretchr/testify/assert"

	sysutil "github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

func TestCgroupFileWriteIfDifferent(t *testing.T) {
	taskDir := "/"
	type args struct {
		cgroupTaskDir string
		resource      sysutil.Resource
		value         string
		currentValue  string
	}
	tests := []struct {
		name    string
		args    args
		want    bool
		wantErr bool
	}{
		{
			name: "currentValue is the same as value",
			args: args{
				cgroupTaskDir: taskDir,
				resource:      sysutil.CPUShares,
				value:         "1024",
				currentValue:  "1024",
			},
			want:    false,
			wantErr: false,
		},
		{
			name: "currentValue is different with value",
			args: args{
				cgroupTaskDir: taskDir,
				resource:      sysutil.CPUShares,
				value:         "1024",
				currentValue:  "512",
			},
			want:    true,
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			helper := sysutil.NewFileTestUtil(t)
			helper.CreateCgroupFile(taskDir, tt.args.resource)

			err := cgroupFileWrite(taskDir, tt.args.resource, tt.args.value)
			assert.NoError(t, err)

			got, gotErr := cgroupFileWriteIfDifferent(taskDir, tt.args.resource, tt.args.currentValue)
			assert.Equal(t, tt.want, got)
			assert.Equal(t, tt.wantErr, gotErr != nil)
		})
	}
}

func TestCgroupFileReadInt(t *testing.T) {
	taskDir := "/"
	testingInt64 := int64(1024)
	testingMaxInt64 := int64(math.MaxInt64)
	type args struct {
		file      sysutil.Resource
		value     string
		supported bool
	}
	tests := []struct {
		name      string
		args      args
		expect    *int64
		expectErr bool
	}{
		{
			name: "test_read_success",
			args: args{
				file:      sysutil.CPUShares,
				value:     "1024",
				supported: true,
			},
			expect:    &testingInt64,
			expectErr: false,
		},
		{
			name: "test_read_error",
			args: args{
				file:      sysutil.CPUShares,
				value:     "unknown",
				supported: true,
			},
			expect:    nil,
			expectErr: true,
		},
		{
			name: "test_read_value_for_max_str",
			args: args{
				file:      sysutil.MemoryHigh,
				value:     CgroupMaxSymbolStr,
				supported: true,
			},
			expect:    &testingMaxInt64,
			expectErr: false,
		},
		{
			name: "test_read_value_for_resource_unsupported",
			args: args{
				file:      sysutil.MemoryWmarkRatio,
				value:     "0",
				supported: false,
			},
			expect:    nil,
			expectErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			helper := sysutil.NewFileTestUtil(t)
			helper.SetResourcesSupported(true, tt.args.file) // set supported for initialization
			helper.CreateCgroupFile(taskDir, tt.args.file)

			err := sysutil.CommonFileWrite(tt.args.file.Path(taskDir), tt.args.value)
			assert.NoError(t, err)

			helper.SetResourcesSupported(tt.args.supported, tt.args.file)
			got, gotErr := cgroupFileReadInt(taskDir, tt.args.file)

			assert.Equal(t, tt.expect, got)
			assert.Equal(t, tt.expectErr, gotErr != nil)
		})
	}
}

func TestCgroupPathExist(t *testing.T) {
	type fields struct {
		isV2         bool
		creatfile    bool
		filename     string
		subfs        string
		resourceType sysutil.ResourceType
	}
	tests := []struct {
		name      string
		fields    fields
		wantExist bool
	}{
		{
			name: "V1 Cgroup path exist",
			fields: fields{
				isV2:         false,
				creatfile:    true,
				filename:     "cpu.my_test_cgroup",
				subfs:        sysutil.CgroupCPUDir,
				resourceType: sysutil.ResourceType("cpu.my_test_cgroup"),
			},
			wantExist: true,
		},
		{
			name: "v2 Cgroup path exist",
			fields: fields{
				isV2:         true,
				creatfile:    true,
				filename:     "cpu.my_test_cgroup",
				subfs:        sysutil.CgroupV2Dir,
				resourceType: sysutil.ResourceType("cpu.my_test_cgroup"),
			},
			wantExist: true,
		},
		{
			name: "Cgroup file no exist",
			fields: fields{
				isV2:         false,
				creatfile:    false,
				filename:     "memory.my_test_cgroup",
				subfs:        sysutil.CgroupMemDir,
				resourceType: sysutil.ResourceType("memory.my_test_cgroup"),
			},
			wantExist: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			testHelper := sysutil.NewFileTestUtil(t)
			defer testHelper.Cleanup()

			var r sysutil.Resource
			if tt.fields.isV2 {
				r = sysutil.DefaultFactory.NewV2(tt.fields.resourceType, tt.fields.filename)
			} else {
				r = sysutil.DefaultFactory.New(tt.fields.filename, tt.fields.subfs)
			}
			if tt.fields.creatfile {
				testHelper.CreateCgroupFile("", r)
			} else {
				testHelper.MkDirAll(filepath.Dir(r.Path("")))
			}

			isExist, _ := IsCgroupPathExist("", r)
			assert.Equal(t, tt.wantExist, isExist)
		})
	}
}
