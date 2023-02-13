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

package type_conversion

import "testing"

func TestInterfaceToUint64(t *testing.T) {
	type args struct {
		value interface{}
	}
	tests := []struct {
		name    string
		args    args
		want    uint64
		wantErr bool
	}{
		{
			name: "test_int",
			args: args{
				value: 1,
			},
			want:    uint64(1),
			wantErr: false,
		},
		{
			name: "test_string",
			args: args{
				value: "1",
			},
			want:    uint64(1),
			wantErr: false,
		},
		{
			name: "test_float64",
			args: args{
				value: float64(1),
			},
			want:    uint64(1),
			wantErr: false,
		},
		{
			name: "test_empty",
			args: args{
				value: nil,
			},
			want:    0,
			wantErr: true,
		},
		{
			name: "test_other",
			args: args{
				value: int32(1),
			},
			want:    0,
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := InterfaceToUint64(tt.args.value)
			if (err != nil) != tt.wantErr {
				t.Errorf("InterfaceToUint64() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("InterfaceToUint64() got = %v, want %v", got, tt.want)
			}
		})
	}
}
