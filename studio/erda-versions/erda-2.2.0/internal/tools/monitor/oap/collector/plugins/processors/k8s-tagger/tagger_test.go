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

package tagger

import (
	"testing"

	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/plugins/processors/k8s-tagger/metadata/pod"
)

func Test_generateIndexByMatcher(t *testing.T) {
	type args struct {
		matcher string
		tags    map[string]string
	}
	tests := []struct {
		name string
		args args
		want pod.Key
	}{
		{
			args: args{
				matcher: "%{namespace}/%{pod}",
				tags: map[string]string{
					"pod":       "aaa",
					"namespace": "default",
				},
			},
			want: "default/aaa",
		},
		{
			name: "single match",
			args: args{
				matcher: "%{namespace}/%{pod}",
				tags: map[string]string{
					"pod":        "aaa",
					"namespacex": "default",
				},
			},
			want: "%{namespace}/aaa",
		},
		{
			name: "not match",
			args: args{
				matcher: "%{namespace}/%{pod}",
				tags: map[string]string{
					"podx":       "aaa",
					"namespacex": "default",
				},
			},
			want: "%{namespace}/%{pod}",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := generateIndexByMatcher(tt.args.matcher, tt.args.tags); got != tt.want {
				t.Errorf("generateIndexByMatcher() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_generateIndexByMatcher1(t *testing.T) {
	type args struct {
		matcher string
		tags    map[string]string
	}
	tests := []struct {
		name string
		args args
		want pod.Key
	}{
		{
			args: args{
				matcher: "%{pod_namespace}/%{pod_name}/%{container}",
				tags: map[string]string{
					"pod_namespace": "default",
					"pod_name":      "p1",
					"container":     "c1",
				},
			},
			want: "default/p1/c1",
		},
		{
			args: args{
				matcher: "%{namespace}/%{name}/%{container}",
				tags: map[string]string{
					"namespace": "default",
					"name":      "p1",
					"container": "c1",
				},
			},
			want: "default/p1/c1",
		},
		{
			args: args{
				matcher: "%{namespace}/%{name}/%{container}",
				tags: map[string]string{
					"name":      "p1",
					"container": "c1",
				},
			},
			want: "%{namespace}/p1/c1",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := generateIndexByMatcher(tt.args.matcher, tt.args.tags); got != tt.want {
				t.Errorf("generateIndexByMatcher() = %v, want %v", got, tt.want)
			}
		})
	}
}
