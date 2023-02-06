// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package storage

import (
	"testing"

	"github.com/erda-project/erda/apistructs"
)

func TestVolumeTypeToSCName(t *testing.T) {
	type args struct {
		diskType string
		vendor   string
	}
	tests := []struct {
		name    string
		args    args
		want    string
		wantErr bool
	}{
		// TODO: Add test cases.
		{
			name: "test01",
			args: args{
				vendor: apistructs.CSIVendorAlibaba,
			},
			want:    apistructs.DiceLocalVolumeSC,
			wantErr: false,
		},
		{
			name: "test02",
			args: args{
				diskType: "DICE-LOCAL",
				vendor:   apistructs.CSIVendorAlibaba,
			},
			want:    apistructs.DiceLocalVolumeSC,
			wantErr: false,
		},
		{
			name: "test03",
			args: args{
				diskType: "DICE-NAS",
				vendor:   apistructs.CSIVendorAlibaba,
			},
			want:    apistructs.DiceNFSVolumeSC,
			wantErr: false,
		},
		{
			name: "test04",
			args: args{
				diskType: apistructs.VolumeTypeSSD,
				vendor:   apistructs.CSIVendorAlibaba,
			},
			want:    apistructs.AlibabaSSDSC,
			wantErr: false,
		},
		{
			name: "test05",
			args: args{
				diskType: apistructs.VolumeTypeNAS,
				vendor:   apistructs.CSIVendorAlibaba,
			},
			want:    apistructs.AlibabaNASSC,
			wantErr: false,
		},
		{
			name: "test06",
			args: args{
				diskType: apistructs.VolumeTypeSSD,
				vendor:   apistructs.CSIVendorTencent,
			},
			want:    apistructs.TencentSSDSC,
			wantErr: false,
		},
		{
			name: "test07",
			args: args{
				diskType: apistructs.VolumeTypeNAS,
				vendor:   apistructs.CSIVendorTencent,
			},
			want:    apistructs.TencentNASSC,
			wantErr: false,
		},
		{
			name: "test08",
			args: args{
				diskType: apistructs.VolumeTypeSSD,
				vendor:   apistructs.CSIVendorHuawei,
			},
			want:    apistructs.HuaweiSSDSC,
			wantErr: false,
		},
		{
			name: "test09",
			args: args{
				diskType: apistructs.VolumeTypeNAS,
				vendor:   apistructs.CSIVendorHuawei,
			},
			want:    apistructs.HuaweiNASSC,
			wantErr: false,
		},
		{
			name: "test10",
			args: args{
				diskType: "XXXX",
				vendor:   apistructs.CSIVendorAlibaba,
			},
			want:    "",
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := VolumeTypeToSCName(tt.args.diskType, tt.args.vendor)
			if (err != nil) != tt.wantErr {
				t.Errorf("VolumeTypeToSCName() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("VolumeTypeToSCName() got = %v, want %v", got, tt.want)
			}
		})
	}
}
