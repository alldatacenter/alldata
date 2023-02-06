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

	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/instanceinfo"
	"github.com/erda-project/erda/pkg/strutil"
)

func updateAddonStatefulSet(dbclient *instanceinfo.Client, stslist *appsv1.StatefulSetList, delete bool) error {
	r := dbclient.ServiceReader()
	w := dbclient.ServiceWriter()

	for _, sts := range stslist.Items {
		var (
			cluster     string
			orgName     string
			orgID       string
			projectName string
			projectID   string
			workspace   string
			addonName   string

			phase     instanceinfo.ServicePhase
			message   string
			startedAt time.Time

			// 参考 terminus.io/dice/dice/docs/dice-env-vars.md
			envsuffixmap = map[string]*string{
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
		// 1. Get all needed information from statefulset
		// -------------------------------
		for _, env := range sts.Spec.Template.Spec.Containers[0].Env {
			for k, v := range envsuffixmap {
				if strutil.HasSuffixes(env.Name, k) {
					*v = env.Value
				}
			}
		}

		// If the content in envsuffixmap is empty, don't update this sts to DB
		// Because the services initiated by the dice deployment process should have these environment variables
		skipThisSts := false
		for _, v := range envsuffixmap {
			if *v == "" {
				skipThisSts = true
				break
			}
		}
		if skipThisSts {
			continue
		}
		phase = instanceinfo.ServicePhaseUnHealthy
		if sts.Spec.Replicas == nil || *sts.Spec.Replicas == sts.Status.ReadyReplicas {
			phase = instanceinfo.ServicePhaseHealthy
		}
		startedAt = sts.ObjectMeta.CreationTimestamp.Time

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

func updateAddonStatefulSetOnWatch(db *instanceinfo.Client) (func(*appsv1.StatefulSet), func(*appsv1.StatefulSet)) {
	addOrUpdateFunc := func(sts *appsv1.StatefulSet) {
		if err := updateAddonStatefulSet(db,
			&appsv1.StatefulSetList{Items: []appsv1.StatefulSet{*sts}}, false); err != nil {
			logrus.Errorf("failed to update statefulset: %v", err)
		}
	}
	deleteFunc := func(sts *appsv1.StatefulSet) {
		if err := updateAddonStatefulSet(db,
			&appsv1.StatefulSetList{Items: []appsv1.StatefulSet{*sts}}, true); err != nil {
			logrus.Errorf("failed to update(delete) statefulset: %v", err)
		}
	}
	return addOrUpdateFunc, deleteFunc
}
