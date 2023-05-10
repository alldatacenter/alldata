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

package i18n

import "testing"

func TestSetGoroutineBindLang(t *testing.T) {
	type args struct {
		localeName string
	}
	tests := []struct {
		name string
		args args
	}{
		{
			name: "test",
			args: args{
				localeName: "test",
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
		})
	}
}

func TestGetGoroutineBindLang(t *testing.T) {
	tests := []struct {
		name string
		want string
	}{
		{
			name: "test",
			want: "test",
		},
		{
			name: "test",
			want: "",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if tt.want != "" {
				SetGoroutineBindLang(tt.want)
			}

			if got := GetGoroutineBindLang(); got != tt.want {
				t.Errorf("GetGoroutineBindLang() = %v, want %v", got, tt.want)
			}
		})
	}
}
