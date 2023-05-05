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

package runtime

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
)

// DeployContext 部署上下文
type DeployContext struct {
	Runtime        *dbclient.Runtime
	App            *apistructs.ApplicationDTO
	LastDeployment *dbclient.Deployment
	// ReleaseId to deploy
	ReleaseID  string
	Operator   string
	DeployType string

	// Extras:
	// used for pipeline
	BuildID uint64
	// used for ability
	AddonActions map[string]interface{}
	// used for runtime-addon
	InstanceID string
	// used for
	Scale0 bool

	// 不由 orchestrator 来推进部署
	SkipPushByOrch bool

	// deployment order
	DeploymentOrderId string
	Param             string
}
