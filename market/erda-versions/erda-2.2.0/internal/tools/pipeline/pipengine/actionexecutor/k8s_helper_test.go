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

package actionexecutor

import (
	"testing"

	"github.com/erda-project/erda/internal/tools/pipeline/pipengine/actionexecutor/plugins/apitest"
	"github.com/erda-project/erda/internal/tools/pipeline/pipengine/actionexecutor/plugins/k8sflink"
	"github.com/erda-project/erda/internal/tools/pipeline/pipengine/actionexecutor/plugins/k8sjob"
	"github.com/erda-project/erda/internal/tools/pipeline/pipengine/actionexecutor/plugins/k8sspark"
	"github.com/erda-project/erda/internal/tools/pipeline/pipengine/actionexecutor/plugins/wait"
	"github.com/erda-project/erda/internal/tools/pipeline/pipengine/actionexecutor/types"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

func TestGetKindByExecutorName(t *testing.T) {
	testCases := []struct {
		name          types.Name
		expectedKind  types.Kind
		expectedFound bool
	}{
		{
			name:          types.Name("api-test"),
			expectedKind:  apitest.Kind,
			expectedFound: true,
		},
		{
			name:          types.Name("wait"),
			expectedKind:  wait.Kind,
			expectedFound: true,
		},
		{
			name:          types.Name("k8s-job-terminus-dev"),
			expectedKind:  k8sjob.Kind,
			expectedFound: true,
		},
		{
			name:          types.Name("k8s-flink-terminus-dev"),
			expectedKind:  k8sflink.Kind,
			expectedFound: true,
		},
		{
			name:          types.Name("k8s-spark-terminus-dev"),
			expectedKind:  k8sspark.Kind,
			expectedFound: true,
		},
		{
			name:          types.Name("unknown"),
			expectedKind:  "",
			expectedFound: false,
		},
	}
	mgr.factory = types.Factory
	mgr.kindsByName = map[types.Name]types.Kind{
		types.Name(spec.PipelineTaskExecutorNameAPITestDefault): types.Kind(spec.PipelineTaskExecutorKindAPITest),
		types.Name(spec.PipelineTaskExecutorNameWaitDefault):    types.Kind(spec.PipelineTaskExecutorKindWait),
	}
	for _, tt := range testCases {
		t.Run(tt.name.String(), func(t *testing.T) {
			kind, found := mgr.GetKindByExecutorName(tt.name)
			if kind != tt.expectedKind {
				t.Errorf("getK8sKindByExecutorName() gotKind = %v, want %v", kind, tt.expectedKind)
			}
			if found != tt.expectedFound {
				t.Errorf("getK8sKindByExecutorName() gotFound = %v, want %v", found, tt.expectedFound)
			}
		})
	}
}
