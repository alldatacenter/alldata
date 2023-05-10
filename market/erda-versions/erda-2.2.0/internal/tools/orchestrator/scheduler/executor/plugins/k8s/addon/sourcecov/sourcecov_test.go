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

package sourcecov

import (
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/require"

	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/addon/sourcecov/mock"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/k8serror"
)

func Test_CreateNSIfNotExists(t *testing.T) {
	assert := require.New(t)
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	ns := mock.NewMockNamespaceUtil(ctrl)
	ns.EXPECT().Create("ns", nil).Return(nil).Times(1)
	ns.EXPECT().Exists("ns").Return(k8serror.ErrNotFound).Times(1)
	operator := &SourcecovOperator{}
	operator.ns = ns
	assert.Nil(operator.CreateNsIfNotExists("ns"))
}
