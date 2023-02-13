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

package lib

import (
	"reflect"
	"regexp"
	"testing"
)

func TestIsJSONArray(t *testing.T) {
	type args struct {
		b []byte
	}
	tests := []struct {
		name string
		args args
		want bool
	}{
		{"", args{b: []byte(`[{"a":1}]`)}, true},
		{"", args{b: []byte(`{"a":1}`)}, false},
		{"", args{b: []byte(`[]`)}, true},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := IsJSONArray(tt.args.b); got != tt.want {
				t.Errorf("isJSONArray() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestNormalizeKey(t *testing.T) {
	type args struct {
		key string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			args: args{key: "abc.edf/hij"},
			want: "abc_edf_hij",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := NormalizeKey(tt.args.key); got != tt.want {
				t.Errorf("NormalizeKey() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestRegexGroupMap(t *testing.T) {
	type args struct {
		pattern *regexp.Regexp
		s       string
	}
	tests := []struct {
		name string
		args args
		want map[string]string
	}{
		{
			args: args{
				pattern: regexp.MustCompile("^/kubepods/\\w+/[\\w|\\-]+/(?P<container_id>\\w+)"),
				s:       "/kubepods/burstable/pod164ec226-8106-4904-9bcb-0218a9b2b793/8367a8b0993ebdf8883a0ad8be9c3978b04883e56a156a8de563afa467d49dec",
			},
			want: map[string]string{
				"container_id": "8367a8b0993ebdf8883a0ad8be9c3978b04883e56a156a8de563afa467d49dec",
			},
		},
		{
			args: args{
				pattern: regexp.MustCompile("^/kubepods/(?P<type>\\w+)/[\\w|\\-]+/(?P<container_id>\\w+)"),
				s:       "/kubepods/burstable/pod164ec226-8106-4904-9bcb-0218a9b2b793/8367a8b0993ebdf8883a0ad8be9c3978b04883e56a156a8de563afa467d49dec",
			},
			want: map[string]string{
				"container_id": "8367a8b0993ebdf8883a0ad8be9c3978b04883e56a156a8de563afa467d49dec",
				"type":         "burstable",
			},
		},
		{
			name: "not match",
			args: args{
				pattern: regexp.MustCompile("^/kubepods/(?P<type>\\w+)/[\\w|\\-]+/(?P<container_id>\\w+)"),
				s:       "/",
			},
			want: map[string]string{},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := RegexGroupMap(tt.args.pattern, tt.args.s); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("RegexGroupMap() = %v, want %v", got, tt.want)
			}
		})
	}
}
