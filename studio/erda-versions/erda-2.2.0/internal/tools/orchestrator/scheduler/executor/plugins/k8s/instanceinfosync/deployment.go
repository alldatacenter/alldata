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

package instanceinfosync

import (
	"time"

	"github.com/sirupsen/logrus"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"

	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/instanceinfo"
)

// updateStatelessServiceDeploymentService Update stateless-service type deployment to db
// TODO: The 3 fields of'cluster','namespace', and'name' have not been processed
func updateStatelessServiceDeployment(dbclient *instanceinfo.Client, deploylist *appsv1.DeploymentList, delete bool) error {
	r := dbclient.ServiceReader()
	w := dbclient.ServiceWriter()

	for _, deploy := range deploylist.Items {
		var (
			cluster         string
			orgName         string
			orgID           string
			projectName     string
			projectID       string
			applicationName string
			applicationID   string
			runtimeName     string
			runtimeID       string
			serviceName     string
			workspace       string
			phase           instanceinfo.ServicePhase
			message         string
			startedAt       time.Time
			namespace       string
			name            string

			// 参考 terminus.io/dice/dice/docs/dice-env-vars.md
			envmap = map[string]*string{
				"DICE_CLUSTER_NAME":     &cluster,
				"DICE_ORG_NAME":         &orgName,
				"DICE_ORG_ID":           &orgID,
				"DICE_PROJECT_NAME":     &projectName,
				"DICE_PROJECT_ID":       &projectID,
				"DICE_APPLICATION_NAME": &applicationName,
				"DICE_APPLICATION_ID":   &applicationID,
				"DICE_RUNTIME_NAME":     &runtimeName,
				"DICE_RUNTIME_ID":       &runtimeID,
				"DICE_SERVICE_NAME":     &serviceName,
				"DICE_WORKSPACE":        &workspace,
			}
		)
		// -------------------------------------
		// 1. Obtain all required information from deployment
		// -------------------------------------
		for _, env := range deploy.Spec.Template.Spec.Containers[0].Env {
			if variable, ok := envmap[env.Name]; ok {
				*variable = env.Value
			}
		}
		// If the content in envmap is empty, do not update the deployment to DB
		// Because the services initiated by the dice deployment process should have these environment variables
		skipThisDeploy := false
		for _, v := range envmap {
			if *v == "" {
				skipThisDeploy = true
				break
			}
		}
		if skipThisDeploy {
			continue
		}
		phase = instanceinfo.ServicePhaseUnHealthy
		for _, cond := range deploy.Status.Conditions {
			if cond.Type == appsv1.DeploymentAvailable && cond.Status == corev1.ConditionFalse {
				phase = instanceinfo.ServicePhaseHealthy
				break
			}
		}

		startedAt = deploy.ObjectMeta.CreationTimestamp.Time

		if runtimeID != "" && workspace != "" {
			namespace = "services"
			name = workspace + "-" + runtimeID
		}

		// -------------------------------
		// 2. Update or create ServiceInfo record
		// -------------------------------

		svcs, err := r.ByOrgName(orgName).
			ByProjectName(projectName).
			ByApplicationName(applicationName).
			ByRuntimeName(runtimeName).
			ByService(serviceName).
			ByWorkspace(workspace).
			ByServiceType("stateless-service").
			Do()
		if err != nil {
			return err
		}
		serviceinfo := instanceinfo.ServiceInfo{
			Cluster:         cluster,
			Namespace:       namespace,
			Name:            name,
			OrgName:         orgName,
			OrgID:           orgID,
			ProjectName:     projectName,
			ProjectID:       projectID,
			ApplicationName: applicationName,
			ApplicationID:   applicationID,
			RuntimeName:     runtimeName,
			RuntimeID:       runtimeID,
			ServiceName:     serviceName,
			Workspace:       workspace,
			ServiceType:     "stateless-service",
			Phase:           phase,
			Message:         message,
			StartedAt:       startedAt,
		}
		switch len(svcs) {
		case 0: // There is no such service in DB
			if delete {
				break
			}
			if err := w.Create(&serviceinfo); err != nil {
				return err
			}
		default: // Will there be a situation where len(svcs)> 1?
			for _, svc := range svcs {
				serviceinfo.ID = svc.ID
				if delete {
					if err := w.Delete(svc.ID); err != nil {
						return err
					}
				} else {
					if err := w.Update(serviceinfo); err != nil {
						return err
					}
				}
			}
		}
	}
	return nil
}

// updateAddonDeploymentService Update addon type deployment to db
func updateAddonDeployment(dbclient *instanceinfo.Client, deploylist *appsv1.DeploymentList, delete bool) error {
	r := dbclient.ServiceReader()
	w := dbclient.ServiceWriter()

	for _, deploy := range deploylist.Items {
		var (
			cluster     string
			orgName     string
			orgID       string
			projectName string
			projectID   string
			workspace   string
			addonName   string
			phase       instanceinfo.ServicePhase
			message     string
			startedAt   time.Time

			envmap = map[string]*string{
				"DICE_CLUSTER_NAME": &cluster,
				"DICE_ORG_NAME":     &orgName,
				"DICE_ORG_ID":       &orgID,
				"DICE_PROJECT_NAME": &projectName,
				"DICE_PROJECT_ID":   &projectID,
				"DICE_WORKSPACE":    &workspace,
				"DICE_ADDON_NAME":   &addonName,
			}
		)
		// -------------------------------
		// 1. Get all the required information from the deployment
		// -------------------------------
		for _, env := range deploy.Spec.Template.Spec.Containers[0].Env {
			if variable, ok := envmap[env.Name]; ok {
				*variable = env.Value
			}
		}

		skipThisDeploy := false
		for _, v := range envmap {
			if *v == "" {
				skipThisDeploy = true
				break
			}
		}
		if skipThisDeploy {
			continue
		}
		phase = instanceinfo.ServicePhaseUnHealthy
		for _, cond := range deploy.Status.Conditions {
			if cond.Type == appsv1.DeploymentAvailable && cond.Status == corev1.ConditionFalse {
				phase = instanceinfo.ServicePhaseHealthy
				break
			}
		}
		startedAt = deploy.ObjectMeta.CreationTimestamp.Time

		// -------------------------------
		// 2. Update or create ServiceInfo record
		// -------------------------------
		svcs, err := r.ByOrgName(orgName).
			ByProjectName(projectName).
			ByWorkspace(workspace).
			ByServiceType("addon").
			Do()
		if err != nil {
			return err
		}
		serviceinfo := instanceinfo.ServiceInfo{
			Cluster:     cluster,
			OrgName:     orgName,
			OrgID:       orgID,
			ProjectName: projectName,
			ProjectID:   projectID,
			Workspace:   workspace,
			ServiceType: "addon",
			Phase:       phase,
			Message:     message,
			StartedAt:   startedAt,
		}
		switch len(svcs) {
		case 0:
			if delete {
				break
			}
			if err := w.Create(&serviceinfo); err != nil {
				return err
			}
		default:
			for _, svc := range svcs {
				serviceinfo.ID = svc.ID
				if delete {
					if err := w.Delete(svc.ID); err != nil {
						return err
					}
				} else {
					if err := w.Update(serviceinfo); err != nil {
						return err
					}
				}
			}
		}
	}
	return nil
}

func updateDeploymentOnWatch(db *instanceinfo.Client) (func(*appsv1.Deployment), func(*appsv1.Deployment)) {
	addOrUpdateFunc := func(deploy *appsv1.Deployment) {
		if err := updateStatelessServiceDeployment(db,
			&appsv1.DeploymentList{Items: []appsv1.Deployment{*deploy}}, false); err != nil {
			logrus.Errorf("failed to update deployment: %v", err)
		}
		if err := updateAddonDeployment(db,
			&appsv1.DeploymentList{Items: []appsv1.Deployment{*deploy}}, false); err != nil {
			logrus.Errorf("failed to update deployment: %v", err)
		}
	}
	deleteFunc := func(deploy *appsv1.Deployment) {
		if err := updateStatelessServiceDeployment(db,
			&appsv1.DeploymentList{Items: []appsv1.Deployment{*deploy}}, true); err != nil {
			logrus.Errorf("failed to update(delete) deployment: %v", err)
		}
		if err := updateAddonDeployment(db,
			&appsv1.DeploymentList{Items: []appsv1.Deployment{*deploy}}, true); err != nil {
			logrus.Errorf("failed to update(delete) deployment: %v", err)
		}
	}

	return addOrUpdateFunc, deleteFunc
}
