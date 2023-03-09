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
	"path/filepath"
	"testing"

	"github.com/stretchr/testify/assert"

	sysutil "github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

func TestNewCommonCgroupUpdater(t *testing.T) {
	type fields struct {
		UseCgroupsV2 bool
	}
	type args struct {
		resourceType sysutil.ResourceType
		parentDir    string
		value        string
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    ResourceUpdater
		wantErr bool
	}{
		{
			name: "updater not found",
			args: args{
				resourceType: sysutil.ResourceType("UnknownResource"),
			},
			want:    nil,
			wantErr: true,
		},
		{
			name: "get cfs quota updater",
			args: args{
				resourceType: sysutil.CPUCFSQuotaName,
				parentDir:    "/kubepods.slice/kubepods.slice-podxxx",
				value:        "-1",
			},
			want: &CgroupResourceUpdater{
				file:       sysutil.CPUCFSQuota,
				parentDir:  "/kubepods.slice/kubepods.slice-podxxx",
				value:      "-1",
				updateFunc: CommonCgroupUpdateFunc,
			},
			wantErr: false,
		},
		{
			name: "get cpu max updater",
			fields: fields{
				UseCgroupsV2: true,
			},
			args: args{
				resourceType: sysutil.CPUCFSQuotaName,
				parentDir:    "/kubepods.slice/kubepods.slice-podxxx",
				value:        "-1",
			},
			want: &CgroupResourceUpdater{
				file:       sysutil.CPUCFSQuotaV2,
				parentDir:  "/kubepods.slice/kubepods.slice-podxxx",
				value:      "-1",
				updateFunc: CommonCgroupUpdateFunc,
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			helper := sysutil.NewFileTestUtil(t)
			defer helper.Cleanup()
			helper.SetCgroupsV2(tt.fields.UseCgroupsV2)

			got, gotErr := NewCommonCgroupUpdater(tt.args.resourceType, tt.args.parentDir, tt.args.value)
			if !tt.wantErr {
				assert.NotNil(t, got)
				assert.Equal(t, tt.want.ResourceType(), got.ResourceType())
				assert.Equal(t, tt.want.Path(), got.Path())
				assert.Equal(t, tt.want.Value(), got.Value())
			}
			assert.Equal(t, tt.wantErr, gotErr != nil, gotErr)
		})
	}
}

func TestCgroupResourceUpdater_Update(t *testing.T) {
	type fields struct {
		UseCgroupsV2 bool
		initialValue string
	}
	type args struct {
		resourceType sysutil.ResourceType
		parentDir    string
		value        string
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    string
		wantErr bool
	}{
		{
			name: "update cfs quota",
			fields: fields{
				initialValue: "10000",
			},
			args: args{
				resourceType: sysutil.CPUCFSQuotaName,
				parentDir:    "/kubepods.slice/kubepods.slice-podxxx",
				value:        "-1",
			},
			want:    "-1",
			wantErr: false,
		},
		{
			name: "update memory.min",
			fields: fields{
				UseCgroupsV2: true,
				initialValue: "0",
			},
			args: args{
				resourceType: sysutil.MemoryMinName,
				parentDir:    "/kubepods.slice/kubepods.slice-podxxx",
				value:        "1048576",
			},
			want:    "1048576",
			wantErr: false,
		},
		{
			name: "update failed since file not exist",
			args: args{
				resourceType: sysutil.MemoryMinName,
				parentDir:    "/kubepods.slice/kubepods.slice-podxxx",
				value:        "1048576",
			},
			want:    "",
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			helper := sysutil.NewFileTestUtil(t)
			defer helper.Cleanup()
			helper.SetCgroupsV2(tt.fields.UseCgroupsV2)

			u, gotErr := NewCommonCgroupUpdater(tt.args.resourceType, tt.args.parentDir, tt.args.value)
			assert.NoError(t, gotErr)
			c, ok := u.(*CgroupResourceUpdater)
			assert.True(t, ok)
			if tt.fields.initialValue != "" {
				helper.WriteCgroupFileContents(tt.args.parentDir, c.file, tt.fields.initialValue)
			}

			gotErr = u.Update()
			assert.Equal(t, tt.wantErr, gotErr != nil)
			if !tt.wantErr {
				assert.Equal(t, tt.want, helper.ReadCgroupFileContents(c.parentDir, c.file))
			}
		})
	}
}

func TestDefaultResourceUpdater_Update(t *testing.T) {
	type fields struct {
		initialValue string
	}
	type args struct {
		file   string
		subDir string
		value  string
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    string
		wantErr bool
	}{
		{
			name: "update test file successfully",
			fields: fields{
				initialValue: "1234",
			},
			args: args{
				file:   "test_file",
				subDir: "test_dir",
				value:  "5678",
			},
			want:    "5678",
			wantErr: false,
		},
		{
			name: "update test file successfully 1",
			fields: fields{
				initialValue: "5678",
			},
			args: args{
				file:   "test_file",
				subDir: "test_dir",
				value:  "5678",
			},
			want:    "5678",
			wantErr: false,
		},
		{
			name: "update failed since file not exist",
			args: args{
				file:   "test_file1",
				subDir: "test_dir1",
				value:  "5678",
			},
			want:    "",
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			helper := sysutil.NewFileTestUtil(t)
			defer helper.Cleanup()

			file := filepath.Join(helper.TempDir, tt.args.subDir, tt.args.file)
			u, gotErr := NewCommonDefaultUpdater(file, file, tt.args.value)
			assert.NoError(t, gotErr)
			_, ok := u.(*DefaultResourceUpdater)
			assert.True(t, ok)
			if tt.fields.initialValue != "" {
				helper.WriteFileContents(filepath.Join(tt.args.subDir, tt.args.file), tt.fields.initialValue)
			}

			gotErr = u.Update()
			assert.Equal(t, tt.wantErr, gotErr != nil, gotErr)
			if !tt.wantErr {
				assert.Equal(t, tt.want, helper.ReadFileContents(filepath.Join(tt.args.subDir, tt.args.file)))
			}
		})
	}
}
