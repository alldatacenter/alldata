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
	"bytes"
	"encoding/json"
	"fmt"

	"github.com/sirupsen/logrus"
	v1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	scv1 "github.com/erda-project/erda-sourcecov/api/v1alpha1"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/addon"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/k8sapi"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/k8serror"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/constraintbuilders"
	"github.com/erda-project/erda/pkg/strutil"
)

type SourcecovOperator struct {
	k8s    addon.K8SUtil
	client *httpclient.HTTPClient
	oc     addon.OvercommitUtil
	ns     addon.NamespaceUtil
}

var APIPrefix = "/apis/" + scv1.GroupVersion.String()

func (s *SourcecovOperator) IsSupported() bool {
	resp, err := s.client.Get(s.k8s.GetK8SAddr()).
		Path(APIPrefix).
		Do().
		DiscardBody()
	if err != nil {
		logrus.Errorf("failed to query %s, host: %v, err: %v",
			APIPrefix,
			s.k8s.GetK8SAddr(), err)
		return false
	}
	if !resp.IsOK() {
		return false
	}
	return true
}

func (s *SourcecovOperator) getNamespace(sg *apistructs.ServiceGroup) string {
	return sg.Services[0].Env["PROJECT_NS"]
}

func (s *SourcecovOperator) getAgentName(sg *apistructs.ServiceGroup) string {
	return "sourcecov-agent-" + sg.ID[:10]
}

func (s *SourcecovOperator) Validate(sg *apistructs.ServiceGroup) error {
	operator, ok := sg.Labels["USE_OPERATOR"]
	if !ok {
		return fmt.Errorf("[BUG] sg need USE_OPERATOR label")
	}
	if strutil.ToLower(operator) != apistructs.AddonSourcecov {
		return fmt.Errorf("[BUG] value of label USE_OPERATOR should be 'sourcecov'")
	}
	if len(sg.Services) != 1 {
		return fmt.Errorf("illegal services num: %d", len(sg.Services))
	}
	if sg.Services[0].Env["PROJECT_NS"] == "" {
		return fmt.Errorf("illegal service: %s, need env 'PROJECT_NS'", sg.Services[0].Name)
	}
	return nil
}

func (s *SourcecovOperator) Convert(sg *apistructs.ServiceGroup) interface{} {
	svc := sg.Services[0]
	var envs []v1.EnvVar

	for key, value := range svc.Env {
		envs = append(envs, v1.EnvVar{Name: key, Value: value})
	}

	scheinfo := sg.ScheduleInfo2
	scheinfo.Stateful = true
	affinity := constraintbuilders.K8S(&scheinfo, nil, nil, nil).Affinity

	scname := "dice-nfs-volume"
	capacity := "20Gi"

	if len(svc.Volumes) > 0 {
		if svc.Volumes[0].SCVolume.Capacity >= diceyml.AddonVolumeSizeMin && svc.Volumes[0].SCVolume.Capacity <= diceyml.AddonVolumeSizeMax {
			capacity = fmt.Sprintf("%dGi", svc.Volumes[0].SCVolume.Capacity)
		}

		if svc.Volumes[0].SCVolume.Capacity > diceyml.AddonVolumeSizeMax {
			capacity = fmt.Sprintf("%dGi", diceyml.AddonVolumeSizeMax)
		}

		if svc.Volumes[0].SCVolume.StorageClassName != "" {
			scname = svc.Volumes[0].SCVolume.StorageClassName
		}
	}
	spec := scv1.Agent{
		TypeMeta: metav1.TypeMeta{
			Kind:       "Agent",
			APIVersion: scv1.GroupVersion.String(),
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:      s.getAgentName(sg),
			Namespace: s.getNamespace(sg),
		},
		Spec: scv1.AgentSpec{
			Image:            svc.Image,
			Env:              envs,
			StorageClassName: scname,
			StorageSize:      resource.MustParse(capacity),
			Affinity:         &affinity,
			Resources: &v1.ResourceRequirements{
				Requests: v1.ResourceList{
					"cpu": resource.MustParse(
						fmt.Sprintf("%dm", int(1000*s.oc.CPUOvercommit(svc.Resources.Cpu)))),
					"memory": resource.MustParse(
						fmt.Sprintf("%dMi", int(svc.Resources.Mem))),
				},
				Limits: v1.ResourceList{
					"cpu": resource.MustParse(
						fmt.Sprintf("%dm", int(1000*svc.Resources.Cpu))),
					"memory": resource.MustParse(
						fmt.Sprintf("%dMi", int(svc.Resources.Mem))),
				},
			},
		},
	}

	// set Labels and annotations
	spec.Labels = make(map[string]string)
	spec.Annotations = make(map[string]string)
	spec.Spec.Labels = make(map[string]string)
	spec.Spec.Annotations = make(map[string]string)
	addon.SetAddonLabelsAndAnnotations(svc, spec.Labels, spec.Annotations)
	addon.SetAddonLabelsAndAnnotations(svc, spec.Spec.Labels, spec.Spec.Annotations)

	// set pvc annotations for snapshot
	if len(svc.Volumes) > 0 {
		if svc.Volumes[0].SCVolume.Snapshot.MaxHistory > 0 {
			if scname == apistructs.AlibabaSSDSC {
				vs := diceyml.VolumeSnapshot{
					MaxHistory: svc.Volumes[0].SCVolume.Snapshot.MaxHistory,
				}
				vsMap := map[string]diceyml.VolumeSnapshot{}
				vsMap[scname] = vs
				data, _ := json.Marshal(vsMap)
				spec.Annotations[apistructs.CSISnapshotMaxHistory] = string(data)
				spec.Spec.Annotations[apistructs.CSISnapshotMaxHistory] = string(data)
			} else {
				logrus.Warnf("Service %s pvc volume use storageclass %s, it do not support snapshot. Only volume.type SSD for Alibaba disk SSD support snapshot\n", svc.Name, scname)
			}
		}
	}

	return &spec
}

func (s *SourcecovOperator) CreateNsIfNotExists(ns string) error {
	if err := s.ns.Exists(ns); err != nil {
		if err != k8serror.ErrNotFound {
			return err
		}

		if err = s.ns.Create(ns, nil); err != nil {
			return err
		}
	}

	return nil
}

func (s *SourcecovOperator) Create(i interface{}) error {
	spec := i.(*scv1.Agent)

	if err := s.CreateNsIfNotExists(spec.Namespace); err != nil {
		logrus.Errorf("failed to create ns %s when creating sourcecov addon %s", spec.Namespace, spec.Name)
		return err
	}

	var b bytes.Buffer
	resp, err := s.client.Post(s.k8s.GetK8SAddr()).
		Path(fmt.Sprintf("%s/namespaces/%s/agents", APIPrefix, spec.Namespace)).
		JSONBody(spec).
		Do().
		Body(&b)
	if err != nil {
		return fmt.Errorf("failed to create sourcecov agent, %s/%s, err: %v", spec.Namespace, spec.Name, err)
	}
	if !resp.IsOK() {
		return fmt.Errorf("failed to create sourcecov agent, %s/%s, statuscode: %v, body: %v",
			spec.Namespace, spec.Name, resp.StatusCode(), b.String())
	}
	return nil
}

func (s *SourcecovOperator) Inspect(sg *apistructs.ServiceGroup) (*apistructs.ServiceGroup, error) {
	var b bytes.Buffer
	var (
		ns   = s.getNamespace(sg)
		name = s.getAgentName(sg)
	)
	resp, err := s.client.Get(s.k8s.GetK8SAddr()).
		Path(fmt.Sprintf("%s/namespaces/%s/agents/%s", APIPrefix, ns, name)).
		JSONBody(k8sapi.DeleteOptions).
		Do().
		Body(&b)

	if err != nil {
		return nil, fmt.Errorf("failed to get sourcecov agent: %s/%s, err: %v", ns, name, err)
	}

	if !resp.IsOK() {
		return nil, fmt.Errorf("failed to get sourcecov agent: %s/%s, statuscode: %v, body: %v",
			sg.Type, sg.ID, resp.StatusCode(), b.String())
	}

	agent := &scv1.Agent{}
	if err = json.NewDecoder(&b).Decode(agent); err != nil {
		return nil, fmt.Errorf("failed to decode sourcecov info: %s/%s, err: %v", ns, name, err)
	}

	sg.Status = apistructs.StatusUnHealthy
	if agent.Status.ReadyReplicas >= 1 {
		sg.Status = apistructs.StatusHealthy
	}

	return sg, nil
}

func (s *SourcecovOperator) Remove(sg *apistructs.ServiceGroup) error {
	var b bytes.Buffer
	resp, err := s.client.Delete(s.k8s.GetK8SAddr()).
		Path(fmt.Sprintf("%s/namespaces/%s/agents/%s", APIPrefix, s.getNamespace(sg), s.getAgentName(sg))).
		JSONBody(k8sapi.DeleteOptions).
		Do().
		Body(&b)

	if err != nil {
		return fmt.Errorf("failed to delele sourcecov agent: %s/%s, err: %v", sg.Type, sg.ID, err)
	}

	if !resp.IsOK() {
		if resp.IsNotfound() {
			return nil
		}
		return fmt.Errorf("failed to delete sourcecov agent: %s/%s, statuscode: %v, body: %v",
			sg.Type, sg.ID, resp.StatusCode(), b.String())
	}

	return nil
}

func (s *SourcecovOperator) Update(i interface{}) error {
	spec := i.(*scv1.Agent)
	var b bytes.Buffer
	resp, err := s.client.Put(s.k8s.GetK8SAddr()).
		Path(fmt.Sprintf("%s/namespaces/%s/agents", APIPrefix, spec.Namespace)).
		JSONBody(spec).
		Do().
		Body(&b)
	if err != nil {
		return fmt.Errorf("failed to update sourcecov agent, %s/%s, err: %v", spec.Namespace, spec.Name, err)
	}
	if !resp.IsOK() {
		return fmt.Errorf("failed to update sourcecov agent, %s/%s, statuscode: %v, body: %v",
			spec.Namespace, spec.Name, resp.StatusCode(), b.String())
	}
	return nil
}

func New(k8s addon.K8SUtil, client *httpclient.HTTPClient, oc addon.OvercommitUtil, ns addon.NamespaceUtil) *SourcecovOperator {
	return &SourcecovOperator{
		k8s:    k8s,
		client: client,
		oc:     oc,
		ns:     ns,
	}
}
