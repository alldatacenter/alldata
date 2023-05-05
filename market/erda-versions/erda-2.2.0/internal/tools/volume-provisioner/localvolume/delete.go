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

package localvolume

import (
	"context"
	"fmt"
	"strings"

	"github.com/sirupsen/logrus"
	v1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/erda-project/erda/pkg/strutil"
)

func (p *localVolumeProvisioner) Delete(ctx context.Context, pv *v1.PersistentVolume) error {
	var selectNodeName string

	logrus.Infof("Start deleting volume: namespace: %s, pvname: %v", pv.Namespace, pv.Name)

	nodeListOption, err := genListOptionFromNodeAffinity(pv.Spec.NodeAffinity)
	if err != nil {
		logrus.Error(err)
		return err
	}

	if values := strings.Split(nodeListOption.LabelSelector, "="); len(values) == 2 {
		selectNodeName = values[1]
	}

	if p.lvpConfig.ModeEdge {
		if selectNodeName != p.lvpConfig.NodeName {
			return nil
		}
		return p.cmdExecutor.OnLocal(fmt.Sprintf("rm -rf %s || true",
			strutil.JoinPath("/hostfs", pv.Spec.PersistentVolumeSource.Local.Path)))
	}

	return p.cmdExecutor.OnNodesPods(fmt.Sprintf("rm -rf %s || true",
		strutil.JoinPath("/hostfs", pv.Spec.PersistentVolumeSource.Local.Path)),
		nodeListOption, metav1.ListOptions{LabelSelector: p.lvpConfig.MatchLabel})
}

func genListOptionFromNodeAffinity(affinity *v1.VolumeNodeAffinity) (metav1.ListOptions, error) {
	for _, t := range affinity.Required.NodeSelectorTerms {
		for _, expr := range t.MatchExpressions {
			if expr.Key == "kubernetes.io/hostname" &&
				expr.Operator == v1.NodeSelectorOpIn &&
				len(expr.Values) == 1 {
				return metav1.ListOptions{
					LabelSelector: fmt.Sprintf("kubernetes.io/hostname=%s", expr.Values[0]),
				}, nil
			}
		}
	}
	return metav1.ListOptions{}, fmt.Errorf("failed to generate ListOption from VolumeNodeAffinity: %v", affinity)
}
