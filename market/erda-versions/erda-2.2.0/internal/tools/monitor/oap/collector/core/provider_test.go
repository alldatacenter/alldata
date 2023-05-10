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

package core

import (
	"reflect"
	"testing"

	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/core/model"
)

func Test_extractFilterConfig(t *testing.T) {
	type args struct {
		cfg interface{}
	}
	tests := []struct {
		name string
		args args
		want model.FilterConfig
	}{
		{
			args: args{cfg: struct {
				Keypass     map[string][]string `file:"keypass"`
				OtherConfig interface{}         `file:"other_config"`
			}{
				Keypass:     map[string][]string{"key": {"val1*", "val2*"}},
				OtherConfig: "nothing",
			}},
			want: model.FilterConfig{
				Keypass: map[string][]string{"key": {"val1*", "val2*"}},
			},
		},
		{
			name: "pointer cfg",
			args: args{cfg: &struct {
				Keypass     map[string][]string `file:"keypass"`
				OtherConfig interface{}         `file:"other_config"`
			}{
				Keypass:     map[string][]string{"key": {"val1*", "val2*"}},
				OtherConfig: "nothing",
			}},
			want: model.FilterConfig{
				Keypass: map[string][]string{"key": {"val1*", "val2*"}},
			},
		},
		{
			args: args{cfg: &struct {
				Keyinclude  []string    `file:"keyinclude"`
				OtherConfig interface{} `file:"other_config"`
			}{
				Keyinclude:  []string{"abc", "edf"},
				OtherConfig: "nothing",
			}},
			want: model.FilterConfig{
				Keyinclude: []string{"abc", "edf"},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := extractFilterConfig(tt.args.cfg); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("extractFilterConfig() = %v, want %v", got, tt.want)
			}
		})
	}
}
