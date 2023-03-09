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

package groupidentity

import (
	"path/filepath"
	"strconv"
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/utils/pointer"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/util"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

func initCPUBvt(dirWithKube string, value int64, helper *system.FileTestUtil) {
	helper.WriteCgroupFileContents(util.GetKubeQosRelativePath(corev1.PodQOSGuaranteed), system.CPUBVTWarpNs,
		strconv.FormatInt(value, 10))
	helper.WriteCgroupFileContents(dirWithKube, system.CPUBVTWarpNs, strconv.FormatInt(value, 10))
}

func initKernelGroupIdentity(value int64, helper *system.FileTestUtil) {
	helper.WriteProcSubFileContents(filepath.Join(system.SysctlSubDir, system.KernelSchedGroupIdentityEnable), strconv.FormatInt(value, 10))
}

func getPodCPUBvt(podDirWithKube string, helper *system.FileTestUtil) int64 {
	valueStr := helper.ReadCgroupFileContents(podDirWithKube, system.CPUBVTWarpNs)
	value, _ := strconv.ParseInt(valueStr, 10, 64)
	return value
}

func Test_bvtPlugin_systemSupported(t *testing.T) {
	kubeRootDir := util.GetKubeQosRelativePath(corev1.PodQOSGuaranteed)
	type fields struct {
		UseCgroupsV2            bool
		initPath                *string
		initKernelGroupIdentity bool
	}
	tests := []struct {
		name   string
		fields fields
		want   bool
	}{
		{
			name: "system support since bvt file exist",
			fields: fields{
				initPath: &kubeRootDir,
			},
			want: true,
		},
		{
			name:   "system not support since bvt not file exist",
			fields: fields{},
			want:   false,
		},
		{
			name: "system support since bvt kernel file exist",
			fields: fields{
				initKernelGroupIdentity: true,
			},
			want: true,
		},
		{
			name: "system not support since bvt not file exist (cgroups-v2)",
			fields: fields{
				UseCgroupsV2: true,
			},
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			testHelper := system.NewFileTestUtil(t)
			defer testHelper.Cleanup()
			testHelper.SetCgroupsV2(tt.fields.UseCgroupsV2)
			if tt.fields.initPath != nil {
				initCPUBvt(*tt.fields.initPath, 0, testHelper)
			}
			if tt.fields.initKernelGroupIdentity {
				initKernelGroupIdentity(0, testHelper)
			}
			b := &bvtPlugin{}
			if got := b.SystemSupported(); got != tt.want {
				t.Errorf("SystemSupported() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_bvtPlugin_Register(t *testing.T) {
	t.Run("register bvt plugin", func(t *testing.T) {
		b := &bvtPlugin{}
		b.Register()
	})
}

func Test_bvtPlugin_initialized(t *testing.T) {
	kubeRootDir := util.GetKubeQosRelativePath(corev1.PodQOSGuaranteed)
	type fields struct {
		initPath                     *string
		initKernelGroupIdentity      bool
		initKernelGroupIdentityValue int
		rule                         *bvtRule
		hasKernelEnable              *bool
		kernelEnabled                *bool
	}
	tests := []struct {
		name    string
		fields  fields
		want    bool
		wantErr bool
	}{
		{
			name:   "cannot initialize since system not support",
			fields: fields{},
			want:   false,
		},
		{
			name: "no need to initialize",
			fields: fields{
				hasKernelEnable: pointer.Bool(false),
			},
			want: false,
		},
		{
			name: "failed to initialize",
			fields: fields{
				hasKernelEnable: pointer.Bool(true),
			},
			want:    false,
			wantErr: true,
		},
		{
			name: "initialized since only bvt file exist",
			fields: fields{
				initPath: &kubeRootDir,
			},
			want: false,
		},
		{
			name: "initialized since bvt kernel file exist",
			fields: fields{
				initKernelGroupIdentity:      true,
				initKernelGroupIdentityValue: 0,
				rule: &bvtRule{
					enable: true,
				},
			},
			want: true,
		},
		{
			name: "initialized since bvt kernel file exist 1",
			fields: fields{
				initKernelGroupIdentity:      true,
				initKernelGroupIdentityValue: 1,
				rule: &bvtRule{
					enable: true,
				},
			},
			want: true,
		},
		{
			name: "initialized since bvt kernel file exist 2",
			fields: fields{
				initPath:                     &kubeRootDir,
				initKernelGroupIdentity:      true,
				initKernelGroupIdentityValue: 0,
				rule: &bvtRule{
					enable: true,
				},
			},
			want: true,
		},
		{
			name: "not initialize since bvt file not exist and cpu qos disabled",
			fields: fields{
				initKernelGroupIdentity:      true,
				initKernelGroupIdentityValue: 1,
				rule: &bvtRule{
					enable: false,
				},
			},
			want: false,
		},
		{
			name: "skip sysctl set since bvt kernel enable not changed",
			fields: fields{
				initPath:                     &kubeRootDir,
				initKernelGroupIdentity:      true,
				initKernelGroupIdentityValue: 0,
				rule: &bvtRule{
					enable: true,
				},
				hasKernelEnable: pointer.Bool(true),
				kernelEnabled:   pointer.Bool(true),
			},
			want: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			testHelper := system.NewFileTestUtil(t)
			if tt.fields.initPath != nil {
				initCPUBvt(*tt.fields.initPath, 0, testHelper)
			}
			if tt.fields.initKernelGroupIdentity {
				initKernelGroupIdentity(int64(tt.fields.initKernelGroupIdentityValue), testHelper)
			}

			b := &bvtPlugin{
				rule:             tt.fields.rule,
				hasKernelEnabled: tt.fields.hasKernelEnable,
				kernelEnabled:    tt.fields.kernelEnabled,
			}
			got, gotErr := b.initialize()
			assert.Equal(t, tt.wantErr, gotErr != nil)
			assert.Equal(t, tt.want, got)
		})
	}
}
