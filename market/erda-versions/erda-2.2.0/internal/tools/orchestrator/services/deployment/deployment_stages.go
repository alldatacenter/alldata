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

package deployment

import (
	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
)

func (d *Deployment) DeployStageAddons(deploymentID uint64) (*apistructs.DeploymentCreateResponseDTO, error) {
	fsm := NewFSMContext(deploymentID, d.db, d.evMgr, d.bdl, d.addon, d.migration, d.encrypt, d.resource, d.releaseSvc, d.serviceGroupImpl, d.scheduler, d.envConfig, d.clusterSvc)
	if err := fsm.Load(); err != nil {
		return nil, errors.Wrapf(err, "failed to load fsm, deployment: %d, (%v)", deploymentID, err)
	}
	var err error
	switch fsm.Deployment.Status {
	case apistructs.DeploymentStatusWaitApprove:

	case apistructs.DeploymentStatusWaiting:
		err = fsm.continueWaiting()
		if err != nil {
			break
		}
		fallthrough
	case apistructs.DeploymentStatusDeploying:
		switch fsm.Deployment.Phase {
		case apistructs.DeploymentPhaseInit:
			err = fsm.continuePhasePreAddon()
			if err != nil {
				break
			}
			fallthrough
		case apistructs.DeploymentPhaseAddon:
			err = fsm.continuePhaseAddon()
			if err != nil {
				break
			}
		}
	default:
		return nil, errors.Errorf("DeployStageAddons: deployment status != WAITING or DEPLOYING or WAITAPPROVE")
	}
	return &apistructs.DeploymentCreateResponseDTO{
		DeploymentID:  fsm.deploymentID,
		ApplicationID: fsm.Runtime.ApplicationID,
		RuntimeID:     fsm.Runtime.ID,
	}, err
}

func (d *Deployment) DeployStageServices(deploymentID uint64) (*apistructs.DeploymentCreateResponseDTO, error) {
	fsm := NewFSMContext(deploymentID, d.db, d.evMgr, d.bdl, d.addon, d.migration, d.encrypt, d.resource, d.releaseSvc, d.serviceGroupImpl, d.scheduler, d.envConfig, d.clusterSvc)
	if err := fsm.Load(); err != nil {
		return nil, errors.Wrapf(err, "failed to load fsm, deployment: %d, (%v)", deploymentID, err)
	}
	var err error
	switch fsm.Deployment.Status {
	case apistructs.DeploymentStatusDeploying:
		switch fsm.Deployment.Phase {
		case apistructs.DeploymentPhaseScript:
			err = fsm.continuePhasePreService()
			if err != nil {
				break
			}
			fallthrough
		case apistructs.DeploymentPhaseService:
			err = fsm.continuePhaseService()
			if err != nil {
				break
			}
		}
	default:
		return nil, errors.Errorf("DeployStageServices: deployment status != DEPLOYING")
	}
	return &apistructs.DeploymentCreateResponseDTO{
		DeploymentID:  deploymentID,
		ApplicationID: fsm.Runtime.ApplicationID,
		RuntimeID:     fsm.Runtime.ID,
	}, err
}

func (d *Deployment) DeployStageDomains(deploymentID uint64) (*apistructs.DeploymentCreateResponseDTO, error) {
	fsm := NewFSMContext(deploymentID, d.db, d.evMgr, d.bdl, d.addon, d.migration, d.encrypt, d.resource, d.releaseSvc, d.serviceGroupImpl, d.scheduler, d.envConfig, d.clusterSvc)
	if err := fsm.Load(); err != nil {
		return nil, errors.Wrapf(err, "failed to load fsm, deployment: %d, (%v)", deploymentID, err)
	}
	var err error
	switch fsm.Deployment.Status {
	case apistructs.DeploymentStatusDeploying:
		switch fsm.Deployment.Phase {
		case apistructs.DeploymentPhaseRegister:
			err = fsm.continuePhaseRegister()
			if err != nil {
				break
			}
			fallthrough
		case apistructs.DeploymentPhaseCompleted:
			err = fsm.continuePhaseCompleted()
			if err != nil {
				break
			}
		}
	default:
		return nil, errors.Errorf("DeployStageDomains: deployment status != DEPLOYING")
	}
	return &apistructs.DeploymentCreateResponseDTO{
		DeploymentID:  fsm.deploymentID,
		ApplicationID: fsm.Runtime.ApplicationID,
		RuntimeID:     fsm.Runtime.ID,
	}, err
}
