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

package file_manager

import (
	"reflect"
	"testing"
)

func Test_parseInstanceMetadata(t *testing.T) {
	tests := []struct {
		name string
		text string
		want map[string]string
	}{
		{
			text: "",
			want: map[string]string{},
		},
		{
			text: "k8spodname=name1, k8snamespace=ns1,k8scontainername=c1",
			want: map[string]string{
				"k8spodname":       "name1",
				"k8snamespace":     "ns1",
				"k8scontainername": "c1",
			},
		},
		{
			text: " =,k8spodname=name1, k8snamespace=ns1,k8scontainername=c1= ,=,",
			want: map[string]string{
				"k8spodname":       "name1",
				"k8snamespace":     "ns1",
				"k8scontainername": "c1=",
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := parseInstanceMetadata(tt.text); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("parseInstanceMetadata() = %v, want %v", got, tt.want)
			}
		})
	}
}
