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

package executors

import (
	"context"

	"istio.io/client-go/pkg/clientset/versioned"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/istioctl"
)

type BaseExecutor struct {
	client versioned.Interface
}

// SetIstioClient
func (exe *BaseExecutor) SetIstioClient(client versioned.Interface) {
	exe.client = client
}

// OnServiceCreate
func (exe BaseExecutor) OnServiceCreate(context.Context, *apistructs.Service) (istioctl.ExecResult, error) {
	return istioctl.ExecSuccess, nil
}

// OnServiceUpdate
func (exe BaseExecutor) OnServiceUpdate(context.Context, *apistructs.Service) (istioctl.ExecResult, error) {
	return istioctl.ExecSuccess, nil
}

// OnServiceDelete
func (exe BaseExecutor) OnServiceDelete(context.Context, *apistructs.Service) (istioctl.ExecResult, error) {
	return istioctl.ExecSuccess, nil
}
