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

package index

import (
	"reflect"
	"strings"
	"testing"
)

func TestPattern_Match(t *testing.T) {
	tests := []struct {
		name           string
		pattern        string
		text           string
		invalidChars   string
		wantBuildError bool
		wantMatch      bool
		wantKeys       []string
		wantVars       []string
	}{
		{
			pattern:   "prefix-<key1>-<key2>",
			text:      "prefix-k1-k2",
			wantMatch: true,
			wantKeys:  []string{"k1", "k2"},
			wantVars:  []string{},
		},
		{
			pattern:      "spot-<metric>-<namespace>-r-{number}",
			text:         "spot-application_rpc_slow-full_cluster-r-000001",
			invalidChars: "-.",
			wantMatch:    true,
			wantKeys:     []string{"application_rpc_slow", "full_cluster"},
			wantVars:     []string{"000001"},
		},
		{
			pattern:   "prefix-<key1>-<key2>-{var1}",
			text:      "prefix-k1-k2-v1",
			wantMatch: true,
			wantKeys:  []string{"k1", "k2"},
			wantVars:  []string{"v1"},
		},
		{
			pattern:   "prefix-<key1>-<key2>-{var1}",
			text:      "prefix-k1-k2-v1",
			wantMatch: true,
			wantKeys:  []string{"k1", "k2"},
			wantVars:  []string{"v1"},
		},
		{
			pattern:   "<key1>-<key2>-{var1}",
			text:      "k1-k2-v1",
			wantMatch: true,
			wantKeys:  []string{"k1", "k2"},
			wantVars:  []string{"v1"},
		},
		{
			pattern:   "<key1>-<key2>-{var1}",
			text:      "k1+k2-v1",
			wantMatch: false,
		},
		{
			pattern:   "<key1>-<key2>-{var1}",
			text:      "prefix-k1-k2-v1",
			wantMatch: true,
			wantKeys:  []string{"prefix", "k1"},
			wantVars:  []string{"k2-v1"},
		},
		{
			pattern:   "fixed-index",
			text:      "fixed-index",
			wantMatch: true,
			wantKeys:  []string{},
			wantVars:  []string{},
		},
		{
			pattern:   "fixed-index",
			text:      "fixed-index-1",
			wantMatch: false,
		},
		{
			pattern:   "<key1>-<key2>-{var1}",
			text:      "k1-k2+v1",
			wantMatch: false,
		},
		{
			pattern:        "prefix-<key1><key2>",
			wantBuildError: true,
		},
		{
			pattern:        "prefix-<key1><key2>-{var1}",
			wantBuildError: true,
		},
		{
			pattern:        "prefix-<key1>-<key2>{var1}",
			wantBuildError: true,
		},
		{
			pattern:        "<key1>-<key2>{var1}",
			wantBuildError: true,
		},
		{
			pattern:      "<key1>-<key2>-{var1}",
			text:         "k1-k2-v1-v2",
			invalidChars: "-.",
			wantMatch:    false,
		},
		{
			pattern:      "<key1>_<key2>-{var1}",
			text:         "k1_k2-v1.v2",
			invalidChars: "-.",
			wantMatch:    false,
		},
		{
			pattern:      "<key1>_<key2>-{var1}",
			text:         "k1_k2-v1",
			invalidChars: "-.",
			wantMatch:    true,
			wantKeys:     []string{"k1", "k2"},
			wantVars:     []string{"v1"},
		},
		{
			pattern:      "prefix-<key1>_<key2>-{var1}",
			text:         "prefix-k1_k2-v1",
			invalidChars: "-.",
			wantMatch:    true,
			wantKeys:     []string{"k1", "k2"},
			wantVars:     []string{"v1"},
		},
		{
			pattern:      "prefix-<key1>_<key2>-{var1}",
			text:         "prefix-k1_k2-v1.suffix",
			invalidChars: "-.",
			wantMatch:    false,
		},
		{
			pattern:      "<metric>-<namespace>-r-{number}",
			text:         "docker_container_summary-full_cluster-r-000013",
			invalidChars: "-.",
			wantMatch:    true,
			wantKeys:     []string{"docker_container_summary", "full_cluster"},
			wantVars:     []string{"000013"},
		},
		{
			pattern:   "prefix-<key1>-<key2>-{}",
			text:      "prefix-k1-k2-",
			wantMatch: true,
			wantKeys:  []string{"k1", "k2"},
			wantVars:  []string{""},
		},
		{
			pattern:   "prefix-<key1>-<key2>-{}.{}",
			text:      "prefix-k1-k2-.",
			wantMatch: true,
			wantKeys:  []string{"k1", "k2"},
			wantVars:  []string{"", ""},
		},
		{
			pattern:   "prefix-<key1>-<key2>-{}.{}",
			text:      "prefix-k1--.",
			wantMatch: true,
			wantKeys:  []string{"k1", ""},
			wantVars:  []string{"", ""},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			pattern, err := BuildPattern(tt.pattern)
			if tt.wantBuildError {
				if err == nil {
					t.Errorf("buildPattern(%q) successfully, but want error", tt.pattern)
				}
				return
			}
			if err != nil {
				t.Errorf("buildPattern(%q) got error: %s", tt.pattern, err)
				return
			}

			result, ok := pattern.Match(tt.text, tt.invalidChars)
			if ok != tt.wantMatch {
				t.Errorf("pattern %q Match(%q) got: %v, want: %v, segments: %v", tt.pattern, tt.text, ok, tt.wantMatch, pattern.Segments)
				return
			}
			if result != nil {
				if !reflect.DeepEqual(result.Keys, tt.wantKeys) {
					t.Errorf("pattern %q Match(%q) got keys %v, want %v", tt.pattern, tt.text, result.Keys, tt.wantKeys)
				}
				if !reflect.DeepEqual(result.Vars, tt.wantVars) {
					t.Errorf("pattern %q Match(%q) got vars %v, want %v", tt.pattern, tt.text, result.Vars, tt.wantVars)
				}
			}
		})
	}
}

func TestPattern_Fill(t *testing.T) {
	tests := []struct {
		name           string
		pattern        string
		keys           []string
		wantBuildError bool
		wantFillError  bool
		want           string
	}{
		{
			pattern: "prefix-<key1>-<key2>",
			keys:    []string{"k1", "k2"},
			want:    "prefix-k1-k2",
		},
		{
			pattern: "prefix-<key1>-<key2>-suffix",
			keys:    []string{"k1", "k2"},
			want:    "prefix-k1-k2-suffix",
		},
		{
			pattern: "<key1>-<key2>",
			keys:    []string{"k1", "k2"},
			want:    "k1-k2",
		},
		{
			pattern:       "<key1>-<key2>",
			keys:          []string{"k1"},
			wantFillError: true,
		},
		{
			pattern:        "<key1>-<key2",
			wantBuildError: true,
		},
		{
			pattern: "prefix-suffix",
			keys:    []string{},
			want:    "prefix-suffix",
		},
		{
			pattern: "",
			keys:    []string{},
			want:    "",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			pattern, err := BuildPattern(tt.pattern)
			if tt.wantBuildError {
				if err == nil {
					t.Errorf("buildPattern(%q) successfully, but want error", tt.pattern)
				}
				return
			}
			if err != nil {
				t.Errorf("buildPattern(%q) got error: %s", tt.pattern, err)
				return
			}

			result, err := pattern.Fill(tt.keys...)
			if tt.wantFillError {
				if err == nil {
					t.Errorf("pattern %q Fill(%s) successfully, but want error", tt.pattern, strings.Join(tt.keys, ","))
				}
				return
			}
			if err != nil {
				t.Errorf("pattern %q Fill(%s) got error: %s", tt.pattern, strings.Join(tt.keys, ","), err)
				return
			}
			if result != tt.want {
				t.Errorf("pattern %q Fill(%q) got result %v, want %v", tt.pattern, strings.Join(tt.keys, ","), result, tt.want)
			}
		})
	}
}
