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

package deployment_order

import (
	"fmt"
	"strings"

	"github.com/erda-project/erda-proto-go/core/dicehub/release/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
	"github.com/erda-project/erda/internal/tools/orchestrator/queue"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/deployment"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/environment"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/runtime"
)

const (
	release        = "RELEASE"
	gitBranchLabel = "gitBranch"
)

type DeploymentOrder struct {
	db         *dbclient.DBClient
	bdl        *bundle.Bundle
	rt         *runtime.Runtime
	deploy     *deployment.Deployment
	queue      *queue.PusherQueue
	releaseSvc pb.ReleaseServiceServer
	envConfig  *environment.EnvConfig
}

type Option func(*DeploymentOrder)

func New(options ...Option) *DeploymentOrder {
	r := &DeploymentOrder{}
	for _, op := range options {
		op(r)
	}
	return r
}

// WithDBClient with database client
func WithDBClient(db *dbclient.DBClient) Option {
	return func(r *DeploymentOrder) {
		r.db = db
	}
}

// WithBundle with bundle
func WithBundle(bdl *bundle.Bundle) Option {
	return func(d *DeploymentOrder) {
		d.bdl = bdl
	}
}

// WithRuntime with runtime service
func WithRuntime(rt *runtime.Runtime) Option {
	return func(d *DeploymentOrder) {
		d.rt = rt
	}
}

// WithDeployment with deployment service
func WithDeployment(deploy *deployment.Deployment) Option {
	return func(d *DeploymentOrder) {
		d.deploy = deploy
	}
}

// WithQueue with queue service
func WithQueue(queue *queue.PusherQueue) Option {
	return func(d *DeploymentOrder) {
		d.queue = queue
	}
}

// WithReleaseSvc with dicehub release service
func WithReleaseSvc(svc pb.ReleaseServiceServer) Option {
	return func(d *DeploymentOrder) {
		d.releaseSvc = svc
	}
}

// With with dicehub release service
func WithEnvConfig(envConfig *environment.EnvConfig) Option {
	return func(d *DeploymentOrder) {
		d.envConfig = envConfig
	}
}

// batchCheckExecutePermission
func (d *DeploymentOrder) batchCheckExecutePermission(userId, workspace string, applicationsInfo map[int64]string) error {
	deniedApps := make([]string, 0)
	// TODO: core-services provide batch auth interface
	for appId, appName := range applicationsInfo {
		isOk, err := d.checkExecutePermission(userId, workspace, uint64(appId))
		if err != nil {
			return err
		}
		if !isOk {
			deniedApps = append(deniedApps, appName)
		}
	}

	if len(deniedApps) != 0 {
		return fmt.Errorf("can't execute at application %s, permission denied", strings.Join(deniedApps, ","))
	}

	return nil
}

func (d *DeploymentOrder) checkExecutePermission(userId, workspace string, appId uint64) (bool, error) {
	access, err := d.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   userId,
		Scope:    apistructs.AppScope,
		ScopeID:  appId,
		Resource: fmt.Sprintf("runtime-%s", strings.ToLower(workspace)),
		Action:   apistructs.CreateAction,
	})
	if err != nil {
		return false, fmt.Errorf("failed to check permission, err: %v", err)
	}
	return access.Access, nil
}
