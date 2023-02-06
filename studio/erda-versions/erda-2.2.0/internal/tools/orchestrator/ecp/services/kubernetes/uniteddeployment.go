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

package kubernetes

import (
	"context"
	"fmt"
	"sort"

	appsv1 "k8s.io/api/apps/v1"
	v1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/intstr"
	crClient "sigs.k8s.io/controller-runtime/pkg/client"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/clientgo/apis/openyurt/v1alpha1"
)

const (
	UnitedDeploymentAPIVersion = "apps.openyurt.io/v1alpha1"
	UnitedDeploymentKind       = "UnitedDeployment"
	StatefulSetType            = "StatefulSet"
	DeploymentType             = "Deployment"
	ServiceAPIVersion          = "v1"
	ServiceKind                = "Service"
	ServiceTopologyKeys        = "topology.kubernetes.io/zone"
	HealthCheckCommandType     = "COMMAND"
	HealthCheckHttpType        = "HTTP"
)

// ListUnitedDeployment List unitedDeployment under the specified cluster and namespace.
func (k *Kubernetes) ListUnitedDeployment(clusterName, namespace string) (*v1alpha1.UnitedDeploymentList, error) {
	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return nil, fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	var udList v1alpha1.UnitedDeploymentList
	if err = client.CRClient.List(context.Background(), &udList, &crClient.ListOptions{
		Namespace: namespace,
	}); err != nil {
		return nil, fmt.Errorf("cluster %s list united deployment error, %v", clusterName, err)
	}

	return &udList, nil
}

// GetUnitedDeployment Get unitedDeployment under the specified cluster and namespace.
func (k *Kubernetes) GetUnitedDeployment(clusterName, namespace, unitedDeploymentName string) (*v1alpha1.UnitedDeployment, error) {
	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return nil, fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	var ud v1alpha1.UnitedDeployment
	if err = client.CRClient.Get(context.Background(), crClient.ObjectKey{
		Namespace: namespace,
		Name:      unitedDeploymentName,
	}, &ud); err != nil {
		return nil, fmt.Errorf("cluster %s get united deployment %s error, %v", clusterName, unitedDeploymentName, err)
	}

	return &ud, nil
}

// CreateUnitedDeployment Crate unitedDeployment on specified cluster and namespace.
func (k *Kubernetes) CreateUnitedDeployment(clusterName string, unitedDeployment *v1alpha1.UnitedDeployment) error {
	if unitedDeployment == nil {
		return fmt.Errorf("create action must give a non-nil UnitedDeployment entity")
	}

	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	if err = client.CRClient.Create(context.Background(), unitedDeployment); err != nil {
		return fmt.Errorf("cluster %s create united deployment %s error, %v", clusterName, unitedDeployment.Name, err)
	}

	return nil
}

// DeleteUnitedDeployment Delete unitedDeployment on specified cluster and namespace.
func (k *Kubernetes) DeleteUnitedDeployment(clusterName, namespace string, unitedDeploymentName string) error {
	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	ud, err := k.GetUnitedDeployment(clusterName, namespace, unitedDeploymentName)
	if err != nil {
		return err
	}

	if err = client.CRClient.Delete(context.Background(), ud); err != nil {
		return fmt.Errorf("cluster %s delete united deployment error, %v", clusterName, err)
	}

	return nil
}

// UpdateUnitedDeployment Update unitedDeployment on specified cluster and namespace.
func (k *Kubernetes) UpdateUnitedDeployment(clusterName, namespace string, unitedDeployment *v1alpha1.UnitedDeployment) error {
	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}
	unitedDeployment.Namespace = namespace
	if err = client.CRClient.Update(context.Background(), unitedDeployment); err != nil {
		return fmt.Errorf("cluster %s update united deployment error, %v", clusterName, err)
	}

	return nil
}

// GenerateUnitedDeploymentSpec Generate Uniteddeployment spec
func (k *Kubernetes) GenerateUnitedDeploymentSpec(req *apistructs.GenerateUnitedDeploymentRequest, env []v1.EnvVar, volumeMount []v1.VolumeMount, affinity *v1.Affinity, probe *v1.Probe) (*v1alpha1.UnitedDeployment, error) {
	var nodePools []v1alpha1.Pool
	var ud *v1alpha1.UnitedDeployment
	var workloadTemplate v1alpha1.WorkloadTemplate
	sort.Strings(req.EdgeSites)
	for i := range req.EdgeSites {
		pool := v1alpha1.Pool{
			Name: req.EdgeSites[i],
			NodeSelectorTerm: v1.NodeSelectorTerm{
				MatchExpressions: []v1.NodeSelectorRequirement{
					{
						Key:      "apps.openyurt.io/nodepool",
						Operator: "In",
						Values:   []string{req.EdgeSites[i]},
					},
				},
			},
			Replicas: &req.Replicas,
		}
		nodePools = append(nodePools, pool)

	}

	container := v1.Container{
		Name:            req.Name,
		Image:           req.Image,
		ImagePullPolicy: "IfNotPresent",
		Resources: v1.ResourceRequirements{
			Requests: v1.ResourceList{
				v1.ResourceCPU:    resource.MustParse(req.RequestCPU),
				v1.ResourceMemory: resource.MustParse(req.RequestMem),
			},
			Limits: v1.ResourceList{
				v1.ResourceCPU:    resource.MustParse(req.LimitCPU),
				v1.ResourceMemory: resource.MustParse(req.LimitMem),
			},
		},
		Env:            env,
		VolumeMounts:   volumeMount,
		LivenessProbe:  probe,
		ReadinessProbe: probe,
	}

	labels := map[string]string{"app": req.Name}
	switch req.Type {
	case DeploymentType:
		workloadTemplate = v1alpha1.WorkloadTemplate{
			DeploymentTemplate: &v1alpha1.DeploymentTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Labels: labels,
				},
				Spec: appsv1.DeploymentSpec{
					Replicas: nil,
					Strategy: appsv1.DeploymentStrategy{
						Type: "Recreate",
					},
					Template: v1.PodTemplateSpec{
						ObjectMeta: metav1.ObjectMeta{
							Labels: labels,
						},
						Spec: v1.PodSpec{
							Containers: []v1.Container{container},
							//deployment in edge use hostnetwork
							HostNetwork: true,
							ImagePullSecrets: []v1.LocalObjectReference{
								{
									Name: req.Name,
								},
							},
							DNSPolicy: "ClusterFirstWithHostNet",
							Affinity:  affinity,
						},
					},
				},
			},
		}
	case StatefulSetType:
		workloadTemplate = v1alpha1.WorkloadTemplate{
			StatefulSetTemplate: &v1alpha1.StatefulSetTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Labels: labels,
				},
				Spec: appsv1.StatefulSetSpec{
					Replicas: nil,
					Template: v1.PodTemplateSpec{
						ObjectMeta: metav1.ObjectMeta{
							Labels: labels,
						},
						Spec: v1.PodSpec{
							Containers: []v1.Container{container},
							Affinity:   affinity,
						},
					},
				},
			},
		}
	}
	ud = &v1alpha1.UnitedDeployment{
		TypeMeta: metav1.TypeMeta{
			Kind:       UnitedDeploymentKind,
			APIVersion: UnitedDeploymentAPIVersion,
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:      req.Name,
			Namespace: req.Namespace,
		},
		Spec: v1alpha1.UnitedDeploymentSpec{
			Selector: &metav1.LabelSelector{
				MatchLabels: labels,
			},
			ConfigSet:        req.ConfigSet,
			WorkloadTemplate: workloadTemplate,
			Topology:         v1alpha1.Topology{Pools: nodePools},
		},
	}

	return ud, nil
}

// GenerateEdgeServiceSpec Generate edge service spec
func (k *Kubernetes) GenerateEdgeServiceSpec(req *apistructs.GenerateEdgeServiceRequest) (*v1.Service, error) {
	var svcPortList []v1.ServicePort
	var svc *v1.Service
	for i := range req.PortMaps {
		iPort := v1.ServicePort{
			Name:       fmt.Sprintf("%s-%v", req.Name, req.PortMaps[i].ServicePort),
			Protocol:   v1.Protocol(req.PortMaps[i].Protocol),
			Port:       req.PortMaps[i].ServicePort,
			TargetPort: intstr.FromInt(req.PortMaps[i].ContainerPort),
		}
		svcPortList = append(svcPortList, iPort)
	}
	labels := map[string]string{"app": req.Name}
	svc = &v1.Service{
		TypeMeta: metav1.TypeMeta{
			Kind:       ServiceKind,
			APIVersion: ServiceAPIVersion,
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:      req.Name,
			Namespace: req.Namespace,
		},
		Spec: v1.ServiceSpec{
			Ports:        svcPortList,
			Selector:     labels,
			TopologyKeys: []string{ServiceTopologyKeys},
		},
	}
	return svc, nil
}

// GenerateHealthCheckSpec Generate Heathcheck spec
func (k *Kubernetes) GenerateHealthCheckSpec(req *apistructs.GenerateHeathCheckRequest) *v1.Probe {
	var probe *v1.Probe
	probe = NewCheckProbe()
	if req.HealthCheckType == HealthCheckHttpType {
		probe.HTTPGet = &v1.HTTPGetAction{
			Path:   req.HealthCheckHttpPath,
			Port:   intstr.FromInt(req.HealthCheckHttpPort),
			Scheme: "HTTP",
		}
	} else if req.HealthCheckType == HealthCheckCommandType {
		probe.Exec = &v1.ExecAction{
			Command: []string{"sh", "-c", req.HealthCheckExec},
		}
	}
	return probe
}

func NewCheckProbe() *v1.Probe {
	return &v1.Probe{
		InitialDelaySeconds: 0,
		// Healthy check
		TimeoutSeconds: 10,
		// Healthy check interval
		PeriodSeconds:    15,
		FailureThreshold: int32(apistructs.HealthCheckDuration) / 15,
	}
}
