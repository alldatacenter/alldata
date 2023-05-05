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

package vk

import (
	"testing"

	"github.com/erda-project/erda/apistructs"
)

func TestGetLabelsWithVendor(t *testing.T) {
	type args struct {
		vendor string
	}
	tests := []struct {
		name    string
		args    args
		want    map[string]string
		wantErr bool
	}{
		{
			name: "alibaba",
			args: args{
				vendor: apistructs.ECIVendorAlibaba,
			},
			want: map[string]string{
				apistructs.AlibabaECILabel: "true",
			},
			wantErr: false,
		},
		{
			name: "fake-vendor",
			args: args{
				vendor: "fake",
			},
			wantErr: true,
		}}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := GetLabelsWithVendor(tt.args.vendor)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetLabelsWithVendor error = %v, wantErr %v", err, tt.wantErr)
				return
			}

			for k, v := range tt.want {
				if wv, ok := got[k]; !ok {
					t.Errorf("GetLabelsWithVendor got = %v, want %v", got, tt.want)
				} else if v != wv {
					t.Errorf("GetLabelsWithVendor got = %v, want %v", got, tt.want)
				}
			}
		})
	}
}
