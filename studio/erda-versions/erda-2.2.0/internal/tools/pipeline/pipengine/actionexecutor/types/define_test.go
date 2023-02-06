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

package types

import (
	"testing"

	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

func TestIsK8SKind(t *testing.T) {
	testCases := []struct {
		name     string
		kind     Kind
		expected bool
	}{
		{
			name:     "k8s job kind",
			kind:     "K8SJOB",
			expected: true,
		},
		{
			name:     "k8s flink kind",
			kind:     "K8SFLINK",
			expected: true,
		},
		{
			name:     "k8s spark kind",
			kind:     "K8SSPARK",
			expected: true,
		},
		{
			name:     "apitest kind",
			kind:     Kind(spec.PipelineTaskExecutorKindAPITest),
			expected: false,
		},
		{
			name:     "unknown kind",
			kind:     "unknown",
			expected: false,
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			actual := tc.kind.IsK8sKind()
			if actual != tc.expected {
				t.Errorf("expected %t, got %t", tc.expected, actual)
			}
		})
	}
}
