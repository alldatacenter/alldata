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

package k8s

import (
	"strings"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/strutil"
)

func (k *Kubernetes) DeletePV(sg *apistructs.ServiceGroup) error {
	if !IsGroupStateful(sg) {
		return nil
	}
	// todo:
	for _, service := range sg.Services {
		for _, bind := range service.Binds {
			hostPath := bind.HostPath
			// Find local disk
			if strings.HasPrefix(hostPath, "/") || len(hostPath) == 0 {
				continue
			}
			// todo: The pv name rule is uniformly produced by a certain function
			pvName := strutil.Concat("lp-", sg.ID, "-")
			if len(hostPath) > 8 {
				pvName = strutil.Concat(pvName, hostPath[:8])
			} else {
				pvName = strutil.Concat(pvName, hostPath)
			}

			// todo: Confirm that the PV is bound to the corresponding PVC of the service under the runtime
			list, err := k.pv.List(pvName)
			if err != nil {
				logrus.Errorf("failed to list pv, runtime: %s, pv: %s, (%v)", sg.ID, pvName, err)
				continue
			}
			for i := range list.Items {
				if !strings.HasPrefix(list.Items[i].Name, pvName) {
					continue
				}
				logrus.Infof("succeed to got pvName: %s, phase: %v", list.Items[i].Name, list.Items[i].Status.Phase)
				if err := k.pv.Delete(list.Items[i].Name); err != nil {
					logrus.Errorf("failed to delete pv name: %s, (%v)", list.Items[i].Name, err)
				}
			}
		}
	}
	return nil
}
