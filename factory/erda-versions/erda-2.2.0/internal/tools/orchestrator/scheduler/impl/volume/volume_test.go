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

package volume

import (
	"reflect"
	"testing"

	"bou.ke/monkey"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/crypto/uuid"
)

func TestNewVolumeID(t *testing.T) {
	type args struct {
		config VolumeCreateConfig
	}
	tests := []struct {
		name    string
		args    args
		want    VolumeIdentity
		wantErr bool
	}{
		{
			name: "Test_01",
			args: args{
				config: VolumeCreateConfig{
					Size: 10,
					Type: "local",
				},
			},
			want:    "6c6f63616c7cdf0078cfe74afe95a962572da952",
			wantErr: false,
		},
		{
			name: "Test_02",
			args: args{
				config: VolumeCreateConfig{
					Size: 10,
					Type: "nas",
				},
			},
			want:    "6e61737cdf0078cfe74afe95a962572da952",
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {

			generatePatch := monkey.Patch(uuid.UUID, func() string {
				return "7cdf0078cfe74afe95a962572da952"
			})

			defer generatePatch.Unpatch()
			got, err := NewVolumeID(tt.args.config)
			if (err != nil) != tt.wantErr {
				t.Errorf("NewVolumeID() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("NewVolumeID() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestDecodeVolumeType(t *testing.T) {
	type args struct {
		s string
	}
	tests := []struct {
		name    string
		args    args
		want    apistructs.VolumeType
		wantErr bool
	}{
		{
			name: "Test_01",
			args: args{
				s: "6c6f63616c7cdf0078cfe74afe95a962572da952",
			},
			want:    "local",
			wantErr: false,
		},
		{
			name: "Test_02",
			args: args{
				s: "6e61737cdf0078cfe74afe95a962572da952",
			},
			want:    "nas",
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := DecodeVolumeType(tt.args.s)
			if (err != nil) != tt.wantErr {
				t.Errorf("DecodeVolumeType() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("DecodeVolumeType() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestEncodeVolumeType(t *testing.T) {
	type args struct {
		t VolumeType
	}
	tests := []struct {
		name    string
		args    args
		want    string
		wantErr bool
	}{
		{
			name: "Test_01",
			args: args{
				t: "local",
			},
			want:    "6c6f63616c",
			wantErr: false,
		},
		{
			name: "Test_02",
			args: args{
				t: "nas",
			},
			want:    "6e6173",
			wantErr: false,
		},
		{
			name: "Test_03",
			args: args{
				t: "xxx",
			},
			want:    "",
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := EncodeVolumeType(tt.args.t)
			if (err != nil) != tt.wantErr {
				t.Errorf("EncodeVolumeType() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("EncodeVolumeType() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestAttachDest_Validate(t *testing.T) {
	tests := []struct {
		name    string
		d       AttachDest
		wantErr bool
	}{
		{
			name: "Test_01",
			d: AttachDest{
				Namespace: "testn",
				Service:   "tests",
				Path:      "/opt/test",
			},
			wantErr: false,
		},
		{
			name: "Test_02",
			d: AttachDest{
				Namespace: "",
				Service:   "tests",
				Path:      "/opt/test",
			},
			wantErr: true,
		},
		{
			name: "Test_03",
			d: AttachDest{
				Namespace: "testn",
				Service:   "",
				Path:      "/opt/test",
			},
			wantErr: true,
		},
		{
			name: "Test_04",
			d: AttachDest{
				Namespace: "testn",
				Service:   "tests",
				Path:      "",
			},
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if err := tt.d.Validate(); (err != nil) != tt.wantErr {
				t.Errorf("Validate() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func TestAttachDest_Equal(t *testing.T) {
	type args struct {
		d2 AttachDest
	}
	tests := []struct {
		name string
		d    AttachDest
		args args
		want bool
	}{
		{
			name: "Test_01",
			d: AttachDest{
				Namespace: "testn",
				Service:   "tests",
				Path:      "/opt/test",
			},
			args: args{
				d2: AttachDest{
					Namespace: "testn",
					Service:   "tests",
					Path:      "/opt/test",
				},
			},
			want: true,
		},
		{
			name: "Test_02",
			d: AttachDest{
				Namespace: "testn",
				Service:   "tests",
				Path:      "/opt/test",
			},
			args: args{
				d2: AttachDest{
					Namespace: "testn",
					Service:   "tests",
					Path:      "/opt/test01",
				},
			},
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := tt.d.Equal(tt.args.d2); got != tt.want {
				t.Errorf("Equal() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestAttachDest_String(t *testing.T) {
	tests := []struct {
		name string
		d    AttachDest
		want string
	}{
		{
			name: "Test_01",
			d: AttachDest{
				Namespace: "testn",
				Service:   "tests",
				Path:      "/opt/test",
			},
			want: "<testn>:<tests>:</opt/test>",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := tt.d.String(); got != tt.want {
				t.Errorf("String() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestVolumeCreateConfigFrom(t *testing.T) {
	type args struct {
		r apistructs.VolumeCreateRequest
	}
	tests := []struct {
		name    string
		args    args
		want    VolumeCreateConfig
		wantErr bool
	}{
		{
			name: "Test_01",
			args: args{
				r: apistructs.VolumeCreateRequest{
					Size: 10,
					Type: "local",
				},
			},
			want: VolumeCreateConfig{
				Size: 10,
				Type: "local",
			},
			wantErr: false,
		},
		{
			name: "Test_02",
			args: args{
				r: apistructs.VolumeCreateRequest{
					Size: 10,
					Type: "nas",
				},
			},
			want: VolumeCreateConfig{
				Size: 10,
				Type: "nas",
			},
			wantErr: false,
		},
		{
			name: "Test_03",
			args: args{
				r: apistructs.VolumeCreateRequest{
					Size: 10,
					Type: "local",
				},
			},
			want: VolumeCreateConfig{
				Size: 10,
				Type: "local",
			},
			wantErr: false,
		},
		{
			name: "Test_04",
			args: args{
				r: apistructs.VolumeCreateRequest{
					Size: 10,
					Type: "nasvolume",
				},
			},
			want: VolumeCreateConfig{
				Size: 10,
				Type: "nas",
			},
			wantErr: false,
		},
		{
			name: "Test_05",
			args: args{
				r: apistructs.VolumeCreateRequest{
					Size: 10,
					Type: "xxx",
				},
			},
			want: VolumeCreateConfig{
				Size: 0,
				Type: "",
			},
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := VolumeCreateConfigFrom(tt.args.r)
			if (err != nil) != tt.wantErr {
				t.Errorf("VolumeCreateConfigFrom() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("VolumeCreateConfigFrom() got = %v, want %v", got, tt.want)
			}
		})
	}
}
