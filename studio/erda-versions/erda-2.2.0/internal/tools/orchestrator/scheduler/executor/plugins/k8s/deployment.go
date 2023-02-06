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
	"context"
	"encoding/json"
	"fmt"
	"os"
	"strconv"
	"strings"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	appsv1 "k8s.io/api/apps/v1"
	apiv1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/validation"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/k8sapi"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/toleration"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/constraintbuilders"
	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/constraintbuilders/constraints"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	// DefaultServiceDNSSuffix k8s service dns fixed suffix
	DefaultServiceDNSSuffix = "svc.cluster.local"
	shardDirSuffix          = "-shard-dir"
	sidecarNamePrefix       = "sidecar-"
	EnableServiceLinks      = "ENABLE_SERVICE_LINKS"
	RegistrySecretName      = "REGISTRY_SECRET_NAME"

	LabelKeyPrefix          = "annotations/"
	ECIPodSidecarConfigPath = "/etc/sidecarconf"

	// ECI Pod fluent-bit sidecar contianer configuration
	// Env Name
	ECIPodFluentbitSidecarImageEnvName      = "COLLECTOR_SIDECAR_IMAGE"
	ECIPodFluentbitSidecarConfigFileEnvName = "CONFIG_FILE"

	// Env Default Value
	ECIPodFluentbitSidecarImage              = "registry.erda.cloud/erda/erda-fluent-bit:2.1-alpha-20220329155354-3fcba88"
	ECIPodFluentbitSidecarConfigFileEnvValue = "/fluent-bit/etc/eci/fluent-bit.conf"
)

var ECIPodSidecarENVFromScheduler = []string{"ECI_POD_FLUENTBIT_COLLECTOR_ADDR", "COLLECTOR_SIDECAR_IMAGE", "ECI_POD_FLUENTBIT_CONFIG_FILE"}

func (k *Kubernetes) createDeployment(ctx context.Context, service *apistructs.Service, sg *apistructs.ServiceGroup) error {
	deployment, err := k.newDeployment(service, sg)
	if err != nil {
		return errors.Errorf("failed to generate deployment struct, name: %s, (%v)", service.Name, err)
	}

	err = k.deploy.Create(deployment)
	if err != nil {
		return errors.Errorf("failed to create deployment, name: %s, (%v)", service.Name, err)
	}
	if service.K8SSnippet == nil || service.K8SSnippet.Container == nil {
		return nil
	}
	err = k.deploy.Patch(deployment.Namespace, deployment.Name, service.Name, (apiv1.Container)(*service.K8SSnippet.Container))
	if err != nil {
		return errors.Errorf("failed to patch deployment, name: %s, snippet: %+v, (%v)", service.Name, *service.K8SSnippet.Container, err)
	}
	return nil
}

func (k *Kubernetes) getDeploymentStatusFromMap(service *apistructs.Service, deployments map[string]appsv1.Deployment) (apistructs.StatusDesc, error) {
	var (
		statusDesc apistructs.StatusDesc
	)
	// in version 1.10.3, the following two apis are equal
	// http://localhost:8080/apis/extensions/v1beta1/namespaces/default/deployments/myk8stest6
	// http://localhost:8080/apis/extensions/v1beta1/namespaces/default/deployments/myk8stest6/status
	deploymentName := getDeployName(service)

	if deployment, ok := deployments[deploymentName]; ok {

		status := deployment.Status
		//You may get this status when you just start creating
		if len(status.Conditions) == 0 {
			statusDesc.Status = apistructs.StatusUnknown
			statusDesc.LastMessage = "cannot get statusDesc condition"
			return statusDesc, nil
		}

		for _, c := range status.Conditions {
			if c.Type == k8sapi.DeploymentReplicaFailure && c.Status == "True" {
				statusDesc.Status = apistructs.StatusFailing
				return statusDesc, nil
			}
			if c.Type == k8sapi.DeploymentAvailable && c.Status == "False" {
				statusDesc.Status = apistructs.StatusFailing
				return statusDesc, nil
			}
		}

		statusDesc.Status = apistructs.StatusUnknown

		if status.Replicas == status.ReadyReplicas &&
			status.Replicas == status.AvailableReplicas &&
			status.Replicas == status.UpdatedReplicas {
			if status.Replicas > 0 {
				statusDesc.Status = apistructs.StatusReady
				statusDesc.LastMessage = fmt.Sprintf("deployment(%s) is running", deployment.Name)
			} else if deployment.Spec.Replicas != nil && *deployment.Spec.Replicas == 0 {
				statusDesc.Status = apistructs.StatusReady
			} else {
				statusDesc.LastMessage = fmt.Sprintf("deployment(%s) replica is 0, been deleting", deployment.Name)
			}
		}
	}

	return statusDesc, nil
}

func (k *Kubernetes) getDeployment(namespace, name string) (*appsv1.Deployment, error) {
	return k.deploy.Get(namespace, name)
}

func (k *Kubernetes) putDeployment(ctx context.Context, deployment *appsv1.Deployment, service *apistructs.Service) error {
	_, projectID, workspace, runtimeID := extractContainerEnvs(deployment.Spec.Template.Spec.Containers)
	deltaCPU, deltaMem, err := k.getDeploymentDeltaResource(ctx, deployment)
	if err != nil {
		logrus.Errorf("faield to get delta resource for deployment %s, %v", deployment.Name, err)
	} else {
		ok, reason, err := k.CheckQuota(ctx, projectID, workspace, runtimeID, deltaCPU, deltaMem, "update", service.Name)
		if err != nil {
			return err
		}
		if !ok {
			return errors.New(reason)
		}
	}

	err = k.deploy.Put(deployment)
	if err != nil {
		return errors.Errorf("failed to update deployment, name: %s, (%v)", service.Name, err)
	}
	if service.K8SSnippet == nil || service.K8SSnippet.Container == nil {
		return nil
	}
	err = k.deploy.Patch(deployment.Namespace, deployment.Name, service.Name, (apiv1.Container)(*service.K8SSnippet.Container))
	if err != nil {
		return errors.Errorf("failed to patch deployment, name: %s, snippet: %+v, (%v)", service.Name, *service.K8SSnippet.Container, err)
	}
	return nil
}

func (k *Kubernetes) getDeploymentDeltaResource(ctx context.Context, deploy *appsv1.Deployment) (deltaCPU, deltaMemory int64, err error) {
	oldDeploy, err := k.k8sClient.ClientSet.AppsV1().Deployments(deploy.Namespace).Get(ctx, deploy.Name, metav1.GetOptions{})
	if err != nil {
		return 0, 0, err
	}
	oldCPU, oldMem := getRequestsResources(oldDeploy.Spec.Template.Spec.Containers)
	if oldDeploy.Spec.Replicas != nil {
		oldCPU *= int64(*oldDeploy.Spec.Replicas)
		oldMem *= int64(*oldDeploy.Spec.Replicas)
	}
	newCPU, newMem := getRequestsResources(deploy.Spec.Template.Spec.Containers)
	if deploy.Spec.Replicas != nil {
		newCPU *= int64(*deploy.Spec.Replicas)
		newMem *= int64(*deploy.Spec.Replicas)
	}

	deltaCPU = newCPU - oldCPU
	deltaMemory = newMem - oldMem
	return
}

func (k *Kubernetes) deleteDeployment(namespace, name string) error {
	logrus.Debugf("delete deployment %s on namespace %s", name, namespace)
	return k.deploy.Delete(namespace, name)
}

// SetPodAnnotationsBaseContainerEnvs Add container environment variables
func SetPodAnnotationsBaseContainerEnvs(container apiv1.Container, podAnnotations map[string]string) {
	if podAnnotations == nil {
		podAnnotations = make(map[string]string)
	}

	for _, env := range container.Env {
		if env.Name == "DICE_ORG_NAME" || strings.HasSuffix(env.Name, "_DICE_ORG_NAME") {
			podAnnotations["msp.erda.cloud/org_name"] = env.Value
			continue
		}

		if env.Name == "DICE_CLUSTER_NAME" || strings.HasSuffix(env.Name, "_DICE_CLUSTER_NAME") {
			podAnnotations["msp.erda.cloud/cluster_name"] = env.Value
			continue
		}

		if env.Name == "DICE_PROJECT_NAME" || strings.HasSuffix(env.Name, "_DICE_PROJECT_NAME") {
			podAnnotations["msp.erda.cloud/project_name"] = env.Value
			continue
		}

		if env.Name == "DICE_SERVICE_NAME" || strings.HasSuffix(env.Name, "_DICE_SERVICE_NAME") {
			podAnnotations["msp.erda.cloud/service_name"] = env.Value
			continue
		}

		if env.Name == "DICE_APPLICATION_NAME" || strings.HasSuffix(env.Name, "_DICE_APPLICATION_NAME") {
			podAnnotations["msp.erda.cloud/application_name"] = env.Value
			continue
		}

		if env.Name == "DICE_WORKSPACE" || strings.HasSuffix(env.Name, "_DICE_WORKSPACE") {
			podAnnotations["msp.erda.cloud/workspace"] = env.Value
			continue
		}

		if env.Name == "DICE_RUNTIME_NAME" || strings.HasSuffix(env.Name, "_DICE_RUNTIME_NAME") {
			podAnnotations["msp.erda.cloud/runtime_name"] = env.Value
			continue
		}

		if env.Name == "DICE_RUNTIME_ID" || strings.HasSuffix(env.Name, "_DICE_RUNTIME_ID") {
			podAnnotations["msp.erda.cloud/runtime_id"] = env.Value
			continue
		}

		if env.Name == "DICE_APPLICATION_ID" || strings.HasSuffix(env.Name, "_DICE_APPLICATION_ID") {
			podAnnotations["msp.erda.cloud/application_id"] = env.Value
			continue
		}

		if env.Name == "DICE_ORG_ID" || strings.HasSuffix(env.Name, "_DICE_ORG_ID") {
			podAnnotations["msp.erda.cloud/org_id"] = env.Value
			continue
		}

		if env.Name == "DICE_PROJECT_ID" || strings.HasSuffix(env.Name, "_DICE_PROJECT_ID") {
			podAnnotations["msp.erda.cloud/project_id"] = env.Value
			continue
		}

		if env.Name == "DICE_DEPLOYMENT_ID" || strings.HasSuffix(env.Name, "_DICE_DEPLOYMENT_ID") {
			podAnnotations["msp.erda.cloud/deployment_id"] = env.Value
			continue
		}

		if env.Name == "TERMINUS_LOG_KEY" || strings.HasSuffix(env.Name, "_TERMINUS_LOG_KEY") {
			podAnnotations["msp.erda.cloud/terminus_log_key"] = env.Value
			continue
		}

		if env.Name == "MONITOR_LOG_KEY" || strings.HasSuffix(env.Name, "_MONITOR_LOG_KEY") {
			podAnnotations["msp.erda.cloud/monitor_log_key"] = env.Value
			continue
		}

		if env.Name == "MSP_ENV_ID" || strings.HasSuffix(env.Name, "_MSP_ENV_ID") {
			podAnnotations["msp.erda.cloud/msp_env_id"] = env.Value
			continue
		}

		if env.Name == "MSP_LOG_ATTACH" || strings.HasSuffix(env.Name, "_MSP_LOG_ATTACH") {
			podAnnotations["msp.erda.cloud/msp_log_attach"] = env.Value
			continue
		}

		if env.Name == "MONITOR_LOG_COLLECTOR" || strings.HasSuffix(env.Name, "_MONITOR_LOG_COLLECTOR") {
			podAnnotations["msp.erda.cloud/monitor_log_collector"] = env.Value
			continue
		}

		if env.Name == "TERMINUS_KEY" || strings.HasSuffix(env.Name, "_TERMINUS_KEY") {
			podAnnotations["msp.erda.cloud/terminus_key"] = env.Value
			continue
		}
	}
}

// AddContainersEnv Add container environment variables
func (k *Kubernetes) AddContainersEnv(containers []apiv1.Container, service *apistructs.Service, sg *apistructs.ServiceGroup) error {
	var envs []apiv1.EnvVar
	ns := MakeNamespace(sg)
	serviceName := service.Name
	if sg.ProjectNamespace != "" {
		ns = sg.ProjectNamespace
		serviceName = service.ProjectServiceName
	}

	// User-injected environment variables
	if len(service.Env) > 0 {
		for k, v := range service.Env {
			envs = append(envs, apiv1.EnvVar{
				Name:  k,
				Value: v,
			})
		}
	}

	addEnv := func(svc *apistructs.Service, envs *[]apiv1.EnvVar, useClusterIP bool) error {
		var err error
		// use SHORT dns, service's name is equal to SHORT dns
		svcName := svc.Name
		if svc.ProjectServiceName != "" {
			svcName = svc.ProjectServiceName
		}
		host := strings.Join([]string{svcName, svc.Namespace, DefaultServiceDNSSuffix}, ".")
		if useClusterIP {
			host, err = k.getClusterIP(svc.Namespace, svc.Name)
			if err != nil {
				return err
			}
		}
		// add {serviceName}_HOST
		*envs = append(*envs, apiv1.EnvVar{
			Name:  makeEnvVariableName(svc.Name) + "_HOST",
			Value: host,
		})

		// {serviceName}_PORT Refers to the first port
		if len(svc.Ports) > 0 {
			*envs = append(*envs, apiv1.EnvVar{
				Name:  makeEnvVariableName(svc.Name) + "_PORT",
				Value: strconv.Itoa(svc.Ports[0].Port),
			})
		}

		//If there are multiple ports, use them in sequence: {serviceName}_PORT0,{serviceName}_PORT1,...
		for i, port := range svc.Ports {
			*envs = append(*envs, apiv1.EnvVar{
				Name:  makeEnvVariableName(svc.Name) + "_PORT" + strconv.Itoa(i),
				Value: strconv.Itoa(port.Port),
			})
		}
		return nil
	}

	//All containers have environment variables for all services
	if sg.ServiceDiscoveryMode == "GLOBAL" {
		// Since some services may not be created yet, so inject dns
		// TODO: You can also create the k8s service of all services in the runtime first
		for _, svc := range sg.Services {
			// Services without port exposure have no environment variables such as {serviceName}_HOST, {serviceName}_PORT, etc.
			if len(svc.Ports) == 0 {
				continue
			}
			svc.Namespace = ns
			// dns environment variables
			if err := addEnv(&svc, &envs, false); err != nil {
				return err
			}
		}
	} else {
		//Inject the environment variables of dependent services according to the convention, for example, service A depends on service B,
		//Then inject {B}_HOST, {B}_PORT into the container environment variable visible to A
		var backendURLEnv apiv1.EnvVar
		for _, name := range service.Depends {
			var dependSvc *apistructs.Service
			for _, svc := range sg.Services {
				if svc.Name != name {
					continue
				}
				dependSvc = &svc
				break
			}
			// this would not happen as if this situations exist,
			// we would find out it in the stage of util.ParseServiceDependency
			if dependSvc == nil {
				return errors.Errorf("failed to find service in runtime, servicename: %s", name)
			}

			if len(dependSvc.Ports) == 0 {
				continue
			}

			// Inject {B}_HOST, {B}_PORT, etc.
			if err := addEnv(dependSvc, &envs, false); err != nil {
				return err
			}

			//Adapt the upper-level logic, if IS_ENDPOINT is assigned to true, add the BACKEND_URL environment variable
			if service.Labels["IS_ENDPOINT"] == "true" && len(dependSvc.Ports) > 0 {
				backendIP := strings.Join([]string{getServiceName(dependSvc), dependSvc.Namespace, DefaultServiceDNSSuffix}, ".")
				backendPort := dependSvc.Ports[0].Port
				backendURLEnv = apiv1.EnvVar{
					Name:  "BACKEND_URL",
					Value: backendIP + ":" + strconv.Itoa(backendPort),
				}
			}
		}

		if len(backendURLEnv.Name) > 0 {
			envs = append(envs, backendURLEnv)
			logrus.Debugf("got service BACKEND_URL, service: %s, namespace: %s, url: %s",
				serviceName, service.Namespace, backendURLEnv.Value)
		}

		// Inject the environment variables of the service itself, namely {A}_HOST, {A}_PORT, etc.
		if len(service.Ports) > 0 {
			if err := addEnv(service, &envs, false); err != nil {
				return err
			}
		}
	}

	clusterInfo, err := k.ClusterInfo.Get()
	if err != nil {
		return errors.Errorf("failed to get cluster info, clusterName: %s, (%v)", k.clusterName, err)
	}

	// add K8S label
	envs = append(envs, apiv1.EnvVar{
		Name:  "IS_K8S",
		Value: "true",
	})

	// add root domain
	envs = append(envs, apiv1.EnvVar{
		Name:  "DICE_ROOT_DOMAIN",
		Value: clusterInfo["DICE_ROOT_DOMAIN"],
	})

	// add namespace label
	envs = append(envs, apiv1.EnvVar{
		Name:  "DICE_NAMESPACE",
		Value: ns,
	})

	if service.NewHealthCheck != nil && service.NewHealthCheck.HttpHealthCheck != nil {
		envs = append(envs, apiv1.EnvVar{
			Name:  "DICE_HTTP_HEALTHCHECK_PATH",
			Value: service.NewHealthCheck.HttpHealthCheck.Path,
		})
	}
	// add POD_IP, HOST_IP
	envs = append(envs, apiv1.EnvVar{
		Name: "POD_IP",
		ValueFrom: &apiv1.EnvVarSource{
			FieldRef: &apiv1.ObjectFieldSelector{
				APIVersion: "v1",
				FieldPath:  "status.podIP",
			},
		},
	}, apiv1.EnvVar{
		Name: "HOST_IP",
		ValueFrom: &apiv1.EnvVarSource{
			FieldRef: &apiv1.ObjectFieldSelector{
				APIVersion: "v1",
				FieldPath:  "status.hostIP",
			},
		},
	})

	// Add POD_NAME
	envs = append(envs, apiv1.EnvVar{
		Name: "POD_NAME",
		ValueFrom: &apiv1.EnvVarSource{
			FieldRef: &apiv1.ObjectFieldSelector{
				APIVersion: "v1",
				FieldPath:  "metadata.name",
			},
		},
	})

	// Add POD_UUID
	envs = append(envs, apiv1.EnvVar{
		Name: "POD_UUID",
		ValueFrom: &apiv1.EnvVarSource{
			FieldRef: &apiv1.ObjectFieldSelector{
				APIVersion: "v1",
				FieldPath:  "metadata.uid",
			},
		},
	})

	// add SELF_URL、SELF_HOST、SELF_PORT
	selfHost := strings.Join([]string{serviceName, service.Namespace, DefaultServiceDNSSuffix}, ".")
	envs = append(envs, apiv1.EnvVar{
		Name:  "SELF_HOST",
		Value: selfHost,
	})
	for i, port := range service.Ports {
		portStr := strconv.Itoa(port.Port)
		if i == 0 {
			// TODO: we should deprecate this SELF_URL
			// TODO: we don't care what http is
			// special port env, SELF_PORT == SELF_PORT0
			envs = append(envs, apiv1.EnvVar{
				Name:  "SELF_PORT",
				Value: portStr,
			}, apiv1.EnvVar{
				Name:  "SELF_URL",
				Value: "http://" + selfHost + ":" + portStr,
			})

		}
		envs = append(envs, apiv1.EnvVar{
			Name:  "SELF_PORT" + strconv.Itoa(i),
			Value: portStr,
		})
	}

	for i, container := range containers {
		requestmem, _ := container.Resources.Requests.Memory().AsInt64()
		limitmem, _ := container.Resources.Limits.Memory().AsInt64()
		envs = append(envs,
			apiv1.EnvVar{
				Name:  "DICE_CPU_ORIGIN",
				Value: fmt.Sprintf("%f", service.Resources.Cpu)},
			apiv1.EnvVar{
				Name:  "DICE_MEM_ORIGIN",
				Value: fmt.Sprintf("%f", service.Resources.Mem)},
			apiv1.EnvVar{
				Name:  "DICE_CPU_REQUEST",
				Value: container.Resources.Requests.Cpu().AsDec().String()},
			apiv1.EnvVar{
				Name:  "DICE_MEM_REQUEST",
				Value: fmt.Sprintf("%d", requestmem/1024/1024)},
			apiv1.EnvVar{
				Name:  "DICE_CPU_LIMIT",
				Value: container.Resources.Limits.Cpu().AsDec().String()},
			apiv1.EnvVar{
				Name:  "DICE_MEM_LIMIT",
				Value: fmt.Sprintf("%d", limitmem/1024/1024)},
		)

		if len(containers[i].Env) > 0 {
			containers[i].Env = append(containers[i].Env, envs...)
		} else {
			containers[i].Env = envs
		}
	}
	return nil
}

func podAnnotations(service *apistructs.Service, podannotations map[string]string) {
	//Inject is turned off by default, and will not be affected by turning on inject in ns
	podannotations["sidecar.istio.io/inject"] = "false"
	// When mesh is enabled and security traffic is enabled, HTTP health checks need to be hijacked
	if service.MeshEnable != nil {
		if *service.MeshEnable {
			podannotations["sidecar.istio.io/inject"] = "true"
			if service.TrafficSecurity.Mode == "https" && service.NewHealthCheck != nil &&
				service.NewHealthCheck.HttpHealthCheck != nil {
				podannotations["sidecar.istio.io/rewriteAppHTTPProbers"] = "true"
			}
		}
	}
}

func (k *Kubernetes) setDeploymentZeroReplica(deploy *appsv1.Deployment) {
	var zero int32
	deploy.Spec.Replicas = &zero
}

func (k *Kubernetes) newDeployment(service *apistructs.Service, serviceGroup *apistructs.ServiceGroup) (*appsv1.Deployment, error) {
	deploymentName := getDeployName(service)
	enableServiceLinks := false
	if _, ok := serviceGroup.Labels[EnableServiceLinks]; ok {
		enableServiceLinks = true
	}
	deployment := &appsv1.Deployment{
		TypeMeta: metav1.TypeMeta{
			Kind:       "Deployment",
			APIVersion: "apps/v1",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:        deploymentName,
			Namespace:   service.Namespace,
			Labels:      make(map[string]string),
			Annotations: make(map[string]string),
		},
		Spec: appsv1.DeploymentSpec{
			RevisionHistoryLimit: func(i int32) *int32 { return &i }(int32(3)),
			Replicas:             func(i int32) *int32 { return &i }(int32(service.Scale)),
			Template: apiv1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Name:   deploymentName,
					Labels: make(map[string]string),
				},
				Spec: apiv1.PodSpec{
					EnableServiceLinks:    func(enable bool) *bool { return &enable }(enableServiceLinks),
					ShareProcessNamespace: func(b bool) *bool { return &b }(false),
					Tolerations:           toleration.GenTolerations(),
				},
			},
			Selector: &metav1.LabelSelector{
				MatchLabels: map[string]string{"app": service.Name},
			},
		},
	}
	imagePullSecrets, err := k.setImagePullSecrets(service.Namespace)
	if err != nil {
		return nil, err
	}
	deployment.Spec.Template.Spec.ImagePullSecrets = imagePullSecrets

	if v := k.options["FORCE_BLUE_GREEN_DEPLOY"]; v == "false" &&
		(strutil.ToUpper(service.Env[DiceWorkSpace]) == apistructs.DevWorkspace.String() ||
			strutil.ToUpper(service.Env[DiceWorkSpace]) == apistructs.TestWorkspace.String()) {
		deployment.Spec.Strategy = appsv1.DeploymentStrategy{Type: "Recreate"}
	}

	affinity := constraintbuilders.K8S(&serviceGroup.ScheduleInfo2, service, []constraints.PodLabelsForAffinity{
		{PodLabels: map[string]string{"app": service.Name}}}, k).Affinity

	if v, ok := service.Env[DiceWorkSpace]; ok {
		affinity.NodeAffinity.PreferredDuringSchedulingIgnoredDuringExecution = append(
			affinity.NodeAffinity.PreferredDuringSchedulingIgnoredDuringExecution,
			k.composeDeploymentNodeAntiAffinityPreferred(v)...)
	}

	deployment.Spec.Template.Spec.Affinity = &affinity
	// inject hosts
	deployment.Spec.Template.Spec.HostAliases = ConvertToHostAlias(service.Hosts)

	container := apiv1.Container{
		// TODO, container name e.g. redis-1528180634
		Name:  service.Name,
		Image: service.Image,
	}

	err = k.setContainerResources(*service, &container)
	if err != nil {
		errMsg := fmt.Sprintf("set container resource err: %v", err)
		logrus.Errorf(errMsg)
		return nil, fmt.Errorf(errMsg)
	}

	logrus.Debugf("container name: %s, container resource spec: %+v", container.Name, container.Resources)

	// Generate sidecars container configuration
	sidecars := k.generateSidecarContainers(service.SideCars)

	// Generate initcontainer configuration
	initcontainers := k.generateInitContainer(service.InitContainer)

	containers := []apiv1.Container{container}
	containers = append(containers, sidecars...)
	deployment.Spec.Template.Spec.Containers = containers
	if len(initcontainers) > 0 {
		deployment.Spec.Template.Spec.InitContainers = initcontainers
	}
	//for k, v := range service.Labels {
	// TODO: temporary modifications
	//if k != "HAPROXY_0_VHOST" {
	//	deployment.Metadata.Labels[k] = v
	//	deployment.Spec.Template.Metadata.Labels[k] = v
	//}
	//}

	// k8s deployment Must have app label to manage pod
	deployment.Labels["app"] = service.Name
	deployment.Spec.Template.Labels["app"] = service.Name

	setDeploymentLabels(service, deployment, serviceGroup.ID)

	if deployment.Spec.Template.Annotations == nil {
		deployment.Spec.Template.Annotations = make(map[string]string)
	}
	podAnnotations(service, deployment.Spec.Template.Annotations)

	// inherit Labels from service.Labels and service.DeploymentLabels
	err = inheritDeploymentLabels(service, deployment)
	if err != nil {
		logrus.Errorf("failed to set labels for service %s for Pod with error: %v\n", service.Name, err)
		return nil, err
	}

	// set pod Annotations from service.Labels and service.DeploymentLabels
	setPodAnnotationsFromLabels(service, deployment.Spec.Template.Annotations)

	// According to the current setting, there is only one user container in a pod
	if service.Cmd != "" {
		for i := range containers {
			//TODO:
			//cmds := strings.Split(service.Cmd, " ")
			cmds := []string{"sh", "-c", service.Cmd}
			containers[i].Command = cmds
		}
	}

	// ECI Pod inject fluent-bit sidecar container
	useECI := UseECI(deployment.Labels, deployment.Spec.Template.Labels)
	if useECI {
		sidecar, err := GenerateECIPodSidecarContainers(k.DeployInEdgeCluster())
		if err != nil {
			logrus.Errorf("%v", err)
			return nil, err
		}
		SetPodContainerLifecycleAndSharedVolumes(&deployment.Spec.Template.Spec)
		deployment.Spec.Template.Spec.Containers = append(deployment.Spec.Template.Spec.Containers, sidecar)
	}

	// Configure health check
	SetHealthCheck(&deployment.Spec.Template.Spec.Containers[0], service)

	if err := k.AddContainersEnv(deployment.Spec.Template.Spec.Containers /*containers*/, service, serviceGroup); err != nil {
		return nil, err
	}

	SetPodAnnotationsBaseContainerEnvs(deployment.Spec.Template.Spec.Containers[0], deployment.Spec.Template.Annotations)

	// TODO: Delete this logic
	//Mobil temporary demand:
	// Inject the secret under the "secret" namespace into the business container

	secrets, err := k.CopyErdaSecrets("secret", service.Namespace)
	if err != nil {
		logrus.Errorf("failed to copy secret: %v", err)
		return nil, err
	}
	secretvolumes := []apiv1.Volume{}
	secretvolmounts := []apiv1.VolumeMount{}
	for _, secret := range secrets {
		secretvol, volmount := k.SecretVolume(&secret)
		secretvolumes = append(secretvolumes, secretvol)
		secretvolmounts = append(secretvolmounts, volmount)
	}

	err = k.AddPodMountVolume(service, &deployment.Spec.Template.Spec, secretvolmounts, secretvolumes)
	if err != nil {
		logrus.Errorf("failed to AddPodMountVolume for deployment %s/%s: %v", deployment.Namespace, deployment.Name, err)
		return nil, err
	}
	k.AddSpotEmptyDir(&deployment.Spec.Template.Spec, service.Resources.EmptyDirCapacity)

	if err = DereferenceEnvs(&deployment.Spec.Template); err != nil {
		return nil, err
	}
	logrus.Debugf("show k8s deployment, name: %s, deployment: %+v", deploymentName, deployment)
	return deployment, nil
}

func (k *Kubernetes) generateInitContainer(initcontainers map[string]diceyml.InitContainer) []apiv1.Container {
	containers := []apiv1.Container{}
	if initcontainers == nil {
		return containers
	}
	for name, initcontainer := range initcontainers {
		reqCPU := fmt.Sprintf("%.fm", k.CPUOvercommit(initcontainer.Resources.CPU)*1000)
		limitCPU := fmt.Sprintf("%.fm", initcontainer.Resources.CPU*1000)
		memory := fmt.Sprintf("%.dMi", initcontainer.Resources.Mem)

		sc := apiv1.Container{
			Name:  name,
			Image: initcontainer.Image,
			Resources: apiv1.ResourceRequirements{
				Requests: apiv1.ResourceList{
					apiv1.ResourceCPU:              resource.MustParse(reqCPU),
					apiv1.ResourceMemory:           resource.MustParse(memory),
					apiv1.ResourceEphemeralStorage: resource.MustParse(k8sapi.EphemeralStorageSizeRequest),
				},
				Limits: apiv1.ResourceList{
					apiv1.ResourceCPU:              resource.MustParse(limitCPU),
					apiv1.ResourceMemory:           resource.MustParse(memory),
					apiv1.ResourceEphemeralStorage: resource.MustParse(k8sapi.EphemeralStorageSizeLimit),
				},
			},
			Command: []string{"sh", "-c", initcontainer.Cmd},
		}
		if initcontainer.Resources.EphemeralStorageCapacity > 1 {
			maxEphemeral := fmt.Sprintf("%dGi", initcontainer.Resources.EphemeralStorageCapacity)
			sc.Resources.Limits[apiv1.ResourceEphemeralStorage] = resource.MustParse(maxEphemeral)
		}
		for i, dir := range initcontainer.SharedDirs {
			emptyDirVolumeName := fmt.Sprintf("%s-%d", name, i)
			dstMount := apiv1.VolumeMount{
				Name:      emptyDirVolumeName,
				MountPath: dir.SideCar,
				ReadOnly:  false, // rw
			}
			sc.VolumeMounts = append(sc.VolumeMounts, dstMount)
		}
		containers = append(containers, sc)
	}

	return containers
}

func (k *Kubernetes) generateSidecarContainers(sidecars map[string]*diceyml.SideCar) []apiv1.Container {
	var containers []apiv1.Container

	if len(sidecars) == 0 {
		return nil
	}

	for name, sidecar := range sidecars {
		reqCPU := fmt.Sprintf("%.fm", k.CPUOvercommit(sidecar.Resources.CPU)*1000)
		limitCPU := fmt.Sprintf("%.fm", sidecar.Resources.CPU*1000)
		memory := fmt.Sprintf("%.dMi", sidecar.Resources.Mem)

		sc := apiv1.Container{
			Name:  strutil.Concat(sidecarNamePrefix, name),
			Image: sidecar.Image,
			Resources: apiv1.ResourceRequirements{
				Requests: apiv1.ResourceList{
					apiv1.ResourceCPU:              resource.MustParse(reqCPU),
					apiv1.ResourceMemory:           resource.MustParse(memory),
					apiv1.ResourceEphemeralStorage: resource.MustParse(k8sapi.EphemeralStorageSizeRequest),
				},
				Limits: apiv1.ResourceList{
					apiv1.ResourceCPU:              resource.MustParse(limitCPU),
					apiv1.ResourceMemory:           resource.MustParse(memory),
					apiv1.ResourceEphemeralStorage: resource.MustParse(k8sapi.EphemeralStorageSizeLimit),
				},
			},
		}
		if sidecar.Resources.EphemeralStorageCapacity > 1 {
			maxEphemeral := fmt.Sprintf("%dGi", sidecar.Resources.EphemeralStorageCapacity)
			sc.Resources.Limits[apiv1.ResourceEphemeralStorage] = resource.MustParse(maxEphemeral)
		}

		//Do not insert platform environment variables (DICE_*) to prevent the instance list from being collected
		for k, v := range sidecar.Envs {
			sc.Env = append(sc.Env, apiv1.EnvVar{
				Name:  k,
				Value: v,
			})
		}

		// Sidecar and business container share directory
		for _, dir := range sidecar.SharedDirs {
			emptyDirVolumeName := strutil.Concat(name, shardDirSuffix)
			dstMount := apiv1.VolumeMount{
				Name:      emptyDirVolumeName,
				MountPath: dir.SideCar,
				ReadOnly:  false, // rw
			}
			sc.VolumeMounts = append(sc.VolumeMounts, dstMount)
		}
		containers = append(containers, sc)
	}

	return containers
}

func makeEnvVariableName(str string) string {
	return strings.ToUpper(strings.Replace(str, "-", "_", -1))
}

// AddPodMountVolume Add pod volume configuration
func (k *Kubernetes) AddPodMountVolume(service *apistructs.Service, podSpec *apiv1.PodSpec,
	secretvolmounts []apiv1.VolumeMount, secretvolumes []apiv1.Volume) error {

	if len(podSpec.Volumes) == 0 {
		podSpec.Volumes = make([]apiv1.Volume, 0)
	}

	//Pay attention to the settings mentioned above, there is only one container in a pod
	if len(podSpec.Containers[0].VolumeMounts) == 0 {
		podSpec.Containers[0].VolumeMounts = make([]apiv1.VolumeMount, 0)
	}

	// get cluster info
	clusterInfo, err := k.ClusterInfo.Get()
	if err != nil {
		return errors.Errorf("failed to get cluster info, clusterName: %s, (%v)", k.clusterName, err)
	}

	// hostPath type
	for i, bind := range service.Binds {
		if bind.HostPath == "" || bind.ContainerPath == "" {
			return errors.New("bind HostPath or ContainerPath is empty")
		}
		//Name formation '[a-z0-9]([-a-z0-9]*[a-z0-9])?'
		name := "volume" + "-bind-" + strconv.Itoa(i)

		hostPath, err := ParseJobHostBindTemplate(bind.HostPath, clusterInfo)
		if err != nil {
			return err
		}

		// The hostPath that does not start with an absolute path is used to apply for local disk resources in the old volume interface
		if !strings.HasPrefix(hostPath, "/") {
			//hostPath = strutil.Concat("/mnt/k8s/", hostPath)
			pvcName := strings.Replace(hostPath, "_", "-", -1)
			sc := "dice-local-volume"
			if err := k.pvc.CreateIfNotExists(&apiv1.PersistentVolumeClaim{
				ObjectMeta: metav1.ObjectMeta{
					Name:      fmt.Sprintf("%s-%s", service.Name, pvcName),
					Namespace: service.Namespace,
				},
				Spec: apiv1.PersistentVolumeClaimSpec{
					AccessModes: []apiv1.PersistentVolumeAccessMode{apiv1.ReadWriteOnce},
					Resources: apiv1.ResourceRequirements{
						Requests: apiv1.ResourceList{
							apiv1.ResourceStorage: resource.MustParse("10Gi"),
						},
					},
					StorageClassName: &sc,
				},
			}); err != nil {
				return err
			}

			podSpec.Volumes = append(podSpec.Volumes,
				apiv1.Volume{
					Name: name,
					VolumeSource: apiv1.VolumeSource{
						PersistentVolumeClaim: &apiv1.PersistentVolumeClaimVolumeSource{
							ClaimName: fmt.Sprintf("%s-%s", service.Name, pvcName),
						},
					},
				})

			podSpec.Containers[0].VolumeMounts = append(podSpec.Containers[0].VolumeMounts,
				apiv1.VolumeMount{
					Name:      name,
					MountPath: bind.ContainerPath,
					ReadOnly:  bind.ReadOnly,
				})
			continue
		}

		podSpec.Volumes = append(podSpec.Volumes,
			apiv1.Volume{
				Name: name,
				VolumeSource: apiv1.VolumeSource{
					HostPath: &apiv1.HostPathVolumeSource{
						Path: hostPath,
					},
				},
			})

		podSpec.Containers[0].VolumeMounts = append(podSpec.Containers[0].VolumeMounts,
			apiv1.VolumeMount{
				Name:      name,
				MountPath: bind.ContainerPath,
				ReadOnly:  bind.ReadOnly,
			})
	}

	// pvc volume type
	if len(service.Volumes) > 0 {
		if err := k.setStatelessServiceVolumes(service, podSpec); err != nil {
			return err
		}
	}

	// Configure the business container sidecar shared directory
	for name, sidecar := range service.SideCars {
		for _, dir := range sidecar.SharedDirs {
			emptyDirVolumeName := strutil.Concat(name, shardDirSuffix)

			quantitySize := resource.MustParse(k8sapi.PodEmptyDirSizeLimit10Gi)
			if sidecar.Resources.EmptyDirCapacity > 0 {
				maxEmptyDir := fmt.Sprintf("%dGi", sidecar.Resources.EmptyDirCapacity)
				quantitySize = resource.MustParse(maxEmptyDir)
			}

			srcMount := apiv1.VolumeMount{
				Name:      emptyDirVolumeName,
				MountPath: dir.Main,
				ReadOnly:  false, // rw
			}
			// Business master container
			podSpec.Containers[0].VolumeMounts = append(podSpec.Containers[0].VolumeMounts, srcMount)

			podSpec.Volumes = append(podSpec.Volumes, apiv1.Volume{
				Name: emptyDirVolumeName,
				VolumeSource: apiv1.VolumeSource{
					EmptyDir: &apiv1.EmptyDirVolumeSource{
						SizeLimit: &quantitySize,
					},
				},
			})
		}
	}
	if service.InitContainer != nil {
		for name, initc := range service.InitContainer {
			for i, dir := range initc.SharedDirs {
				name := fmt.Sprintf("%s-%d", name, i)
				srcMount := apiv1.VolumeMount{
					Name:      name,
					MountPath: dir.Main,
					ReadOnly:  false,
				}
				quantitySize := resource.MustParse(k8sapi.PodEmptyDirSizeLimit10Gi)
				if initc.Resources.EmptyDirCapacity > 0 {
					maxEmptyDir := fmt.Sprintf("%dGi", initc.Resources.EmptyDirCapacity)
					quantitySize = resource.MustParse(maxEmptyDir)
				}
				podSpec.Containers[0].VolumeMounts = append(podSpec.Containers[0].VolumeMounts, srcMount)
				podSpec.Volumes = append(podSpec.Volumes, apiv1.Volume{
					Name: name,
					VolumeSource: apiv1.VolumeSource{
						EmptyDir: &apiv1.EmptyDirVolumeSource{
							SizeLimit: &quantitySize,
						},
					},
				})
			}
		}
	}

	podSpec.Volumes = append(podSpec.Volumes, secretvolumes...)
	podSpec.Containers[0].VolumeMounts = append(podSpec.Containers[0].VolumeMounts, secretvolmounts...)

	return nil
}

func (k *Kubernetes) setStatelessServiceVolumes(service *apistructs.Service, podSpec *apiv1.PodSpec) error {

	runtimeName := ""
	workspace := ""
	pvcUUID := ""
	for _, v := range podSpec.Containers[0].Env {
		if v.Name == "DICE_RUNTIME_NAME" || strings.HasSuffix(v.Name, "_DICE_RUNTIME_NAME") {
			runtimeName = v.Value
			continue
		}

		if v.Name == "DICE_WORKSPACE" || strings.HasSuffix(v.Name, "_DICE_WORKSPACE") {
			workspace = v.Value
			continue
		}

		if v.Name == "DICE_APPLICATION_ID" || strings.HasSuffix(v.Name, "_DICE_APPLICATION_ID") {
			pvcUUID = v.Value
			continue
		}
	}

	if runtimeName != "" {
		if pvcUUID != "" {
			pvcUUID = fmt.Sprintf("%s-%s", pvcUUID, strings.Replace(runtimeName, "/", "-", -1))
		} else {
			pvcUUID = fmt.Sprintf("%s", strings.Replace(runtimeName, "/", "-", -1))
		}
	}

	if workspace != "" {
		pvcUUID = fmt.Sprintf("%s-%s", pvcUUID, workspace)
	}

	for i, vol := range service.Volumes {
		//Name formation '[a-z0-9]([-a-z0-9]*[a-z0-9])?'
		name := "volume" + "-pvc-" + strconv.Itoa(i)
		pvcName := ""
		if pvcUUID != "" {
			pvcName = fmt.Sprintf("%s-%s-%s", service.Name, pvcUUID, vol.ID)
		} else {
			pvcName = fmt.Sprintf("%s-%s", podSpec.Containers[0].Name, vol.ID)
		}
		sc := "dice-local-volume"
		capacity := "20Gi"
		if vol.StorageClassName != "" {
			sc = vol.StorageClassName
		}

		// 校验 sc 是否存在（volume 的 type + vendor 的组合可能会产生当前不支持的 sc）
		_, err := k.storageClass.Get(sc)
		if err != nil {
			if err.Error() == "not found" {
				logrus.Errorf("failed to set volume for sevice %s: storageclass %s not found.", service.Name, sc)
				return errors.Errorf("failed to set volume for sevice %s: storageclass %s not found.", service.Name, sc)
			} else {
				logrus.Errorf("failed to set volume for sevice %s: get storageclass %s from cluster failed: %#v", service.Name, sc, err)
				return errors.Errorf("failed to set volume for sevice %s: get storageclass %s from cluster failed: %#v", service.Name, sc, err)
			}
		}

		// stateless service with replicas > 1, only use sc support mounted by by multi-pods
		if service.WorkLoad == ServicePerNode && !strings.Contains(sc, "-nfs-") && !strings.Contains(sc, "-nas-") {
			// for daemonset
			return errors.Errorf("failed to set volume for sevice %s: can not use storageclass %s create pvc for per_node service, please set volume type to 'NAS' or 'DICE-NAS'", service.Name, sc)
		} else {
			// for deployment
			if service.Scale > 1 && !strings.Contains(sc, "-nfs-") && !strings.Contains(sc, "-nas-") {
				return errors.Errorf("failed to set volume for sevice %s: can not use storageclass %s create pvc for service with more than one replicas for stateless service, please set volume type to 'NAS'", service.Name, sc)
			}
		}

		if vol.Capacity > 20 {
			capacity = fmt.Sprintf("%dGi", vol.Capacity)
		}
		pvc := apiv1.PersistentVolumeClaim{
			ObjectMeta: metav1.ObjectMeta{
				Name:      pvcName,
				Namespace: service.Namespace,
			},
			Spec: apiv1.PersistentVolumeClaimSpec{
				AccessModes: []apiv1.PersistentVolumeAccessMode{apiv1.ReadWriteOnce},
				Resources: apiv1.ResourceRequirements{
					Requests: apiv1.ResourceList{
						apiv1.ResourceStorage: resource.MustParse(capacity),
					},
				},
				StorageClassName: &sc,
			},
		}
		if vol.SCVolume.Snapshot != nil && vol.SCVolume.Snapshot.MaxHistory > 0 {
			if sc == apistructs.AlibabaSSDSC {
				pvc.Annotations = make(map[string]string)
				vs := diceyml.VolumeSnapshot{
					MaxHistory: vol.SCVolume.Snapshot.MaxHistory,
				}
				vsMap := map[string]diceyml.VolumeSnapshot{}
				vsMap[sc] = vs
				data, _ := json.Marshal(vsMap)
				pvc.Annotations[apistructs.CSISnapshotMaxHistory] = string(data)
			} else {
				logrus.Warnf("Service %s pvc volume use storageclass %v, it do not support snapshot. Only volume.type SSD for Alibaba disk SSD support snapshot\n", service.Name, sc)
			}
		}

		if err := k.pvc.CreateIfNotExists(&pvc); err != nil {
			return err
		}

		podSpec.Volumes = append(podSpec.Volumes,
			apiv1.Volume{
				Name: name,
				VolumeSource: apiv1.VolumeSource{
					PersistentVolumeClaim: &apiv1.PersistentVolumeClaimVolumeSource{
						ClaimName: pvcName,
					},
				},
			})

		podSpec.Containers[0].VolumeMounts = append(podSpec.Containers[0].VolumeMounts,
			apiv1.VolumeMount{
				Name:      name,
				MountPath: vol.TargetPath,
				ReadOnly:  vol.ReadOnly,
			})
	}
	return nil
}

func (k *Kubernetes) AddSpotEmptyDir(podSpec *apiv1.PodSpec, emptySize int) {
	quantitySize := resource.MustParse(k8sapi.PodEmptyDirSizeLimit10Gi)
	if emptySize > 0 {
		quantitySize = resource.MustParse(fmt.Sprintf("%dGi", emptySize))
	}
	podSpec.Volumes = append(podSpec.Volumes, apiv1.Volume{
		Name: "spot-emptydir",
		VolumeSource: apiv1.VolumeSource{EmptyDir: &apiv1.EmptyDirVolumeSource{
			SizeLimit: &quantitySize,
		}},
	})

	podSpec.Containers[0].VolumeMounts = append(podSpec.Containers[0].VolumeMounts, apiv1.VolumeMount{
		Name:      "spot-emptydir",
		MountPath: "/tmp",
	})
}

func getDeployName(service *apistructs.Service) string {
	if service.ProjectServiceName != "" {
		return service.ProjectServiceName
	}
	return service.Name
}

func setDeploymentLabels(service *apistructs.Service, deployment *appsv1.Deployment, sgID string) {
	if service.ProjectServiceName != "" {
		deployment.Spec.Selector.MatchLabels[LabelServiceGroupID] = sgID
		deployment.Spec.Template.Labels[LabelServiceGroupID] = sgID
		deployment.Labels[LabelServiceGroupID] = sgID
	}
}

func ConvertToHostAlias(hosts []string) []apiv1.HostAlias {
	var r []apiv1.HostAlias
	for _, host := range hosts {
		splitRes := strings.Fields(host)
		if len(splitRes) < 2 {
			continue
		}
		r = append(r, apiv1.HostAlias{
			IP:        splitRes[0],
			Hostnames: splitRes[1:],
		})
	}
	return r
}

func (k *Kubernetes) scaleDeployment(ctx context.Context, sg *apistructs.ServiceGroup, serviceIndex int) error {
	// only support scale the first one service
	ns := sg.ProjectNamespace
	if ns == "" {
		ns = MakeNamespace(sg)
	}

	scalingService := sg.Services[serviceIndex]
	deploymentName := getDeployName(&scalingService)
	deploy, err := k.getDeployment(ns, deploymentName)
	if err != nil {
		getErr := fmt.Errorf("failed to get the deployment %s in namespace %s, err is: %s", deploymentName, ns, err.Error())
		return getErr
	}

	// 如果扩展为多个(>=2)副本，需要判断 deployment 的 Pod 是否挂载支持共享挂载的 PVC，如果不支持，则不能扩展
	if scalingService.Scale > 1 {
		for _, vol := range deploy.Spec.Template.Spec.Volumes {
			if vol.PersistentVolumeClaim != nil && vol.PersistentVolumeClaim.ClaimName != "" {
				pvc, err := k.pvc.Get(deploy.Namespace, vol.PersistentVolumeClaim.ClaimName)
				if err != nil {
					return fmt.Errorf("failed to update the deployment, err is: %v", err)
				}
				if *pvc.Spec.StorageClassName == apistructs.AlibabaSSDSC ||
					*pvc.Spec.StorageClassName == apistructs.VolumeTypeDiceLOCAL ||
					*pvc.Spec.StorageClassName == apistructs.TencentSSDSC ||
					*pvc.Spec.StorageClassName == apistructs.HuaweiSSDSC {
					return fmt.Errorf("failed to update the deployment %s/%s, it has pvc with name %s which can not mounted by multi-pods", deploy.Namespace, deploy.Name, pvc.Name)
				}
			}
		}
	}

	oldCPU, oldMem := getRequestsResources(deploy.Spec.Template.Spec.Containers)
	if deploy.Spec.Replicas != nil {
		oldCPU *= int64(*deploy.Spec.Replicas)
		oldMem *= int64(*deploy.Spec.Replicas)
	}

	deploy.Spec.Replicas = func(i int32) *int32 { return &i }(int32(scalingService.Scale))

	for index := range deploy.Spec.Template.Spec.Containers {
		// only support one container on Erda currently
		container := deploy.Spec.Template.Spec.Containers[index]

		err = k.setContainerResources(scalingService, &container)
		if err != nil {
			setContainerErr := fmt.Errorf("failed to set container resource, err is: %s", err.Error())
			return setContainerErr
		}

		k.UpdateContainerResourceEnv(scalingService.Resources, &container)

		deploy.Spec.Template.Spec.Containers[index] = container
	}

	newCPU, newMem := getRequestsResources(deploy.Spec.Template.Spec.Containers)
	newCPU *= int64(*deploy.Spec.Replicas)
	newMem *= int64(*deploy.Spec.Replicas)

	_, projectID, workspace, runtimeID := extractContainerEnvs(deploy.Spec.Template.Spec.Containers)
	ok, reason, err := k.CheckQuota(ctx, projectID, workspace, runtimeID, newCPU-oldCPU, newMem-oldMem, "scale", scalingService.Name)
	if err != nil {
		return err
	}
	if !ok {
		return errors.New(reason)
	}

	err = k.deploy.Put(deploy)
	if err != nil {
		updateErr := fmt.Errorf("failed to update the deployment, err is: %s", err.Error())
		return updateErr
	}
	return nil
}

func (k *Kubernetes) setContainerResources(service apistructs.Service, container *apiv1.Container) error {
	if service.Resources.Cpu < MIN_CPU_SIZE {
		return errors.Errorf("invalid cpu, value: %v, (which is lower than min cpu(%v))",
			service.Resources.Cpu, MIN_CPU_SIZE)
	}

	//Set the over-score ratio according to the environment
	cpuSubscribeRatio := k.cpuSubscribeRatio
	memSubscribeRatio := k.memSubscribeRatio
	switch strutil.ToUpper(service.Env[DiceWorkSpace]) {
	case "DEV":
		cpuSubscribeRatio = k.devCpuSubscribeRatio
		memSubscribeRatio = k.devMemSubscribeRatio
	case "TEST":
		cpuSubscribeRatio = k.testCpuSubscribeRatio
		memSubscribeRatio = k.testMemSubscribeRatio
	case "STAGING":
		cpuSubscribeRatio = k.stagingCpuSubscribeRatio
		memSubscribeRatio = k.stagingMemSubscribeRatio
	}
	if cpuSubscribeRatio < 1.0 {
		cpuSubscribeRatio = 1.0
	}
	if memSubscribeRatio < 1.0 {
		memSubscribeRatio = 1.0
	}

	cpu := fmt.Sprintf("%dm", int(service.Resources.Cpu*1000))
	memory := fmt.Sprintf("%dMi", int(service.Resources.Mem))

	maxCpu := fmt.Sprintf("%dm", int(service.Resources.MaxCPU*1000))
	maxMem := fmt.Sprintf("%dMi", int(service.Resources.MaxMem))

	container.Resources = apiv1.ResourceRequirements{
		Requests: apiv1.ResourceList{
			apiv1.ResourceCPU:              resource.MustParse(cpu),
			apiv1.ResourceMemory:           resource.MustParse(memory),
			apiv1.ResourceEphemeralStorage: resource.MustParse(k8sapi.EphemeralStorageSizeRequest),
		},
		Limits: apiv1.ResourceList{
			apiv1.ResourceCPU:              resource.MustParse(maxCpu),
			apiv1.ResourceMemory:           resource.MustParse(maxMem),
			apiv1.ResourceEphemeralStorage: resource.MustParse(k8sapi.EphemeralStorageSizeLimit),
		},
	}

	if service.Resources.EphemeralStorageCapacity > 1 {
		maxEphemeral := fmt.Sprintf("%dGi", service.Resources.EphemeralStorageCapacity)
		container.Resources.Limits[apiv1.ResourceEphemeralStorage] = resource.MustParse(maxEphemeral)
	}

	if err := k.SetFineGrainedCPU(container, map[string]string{}, cpuSubscribeRatio); err != nil {
		logrus.Errorf("set cpu resource failed, container name: %s, error: %v", container.Name, err)
		return err
	}
	if err := k.SetOverCommitMem(container, memSubscribeRatio); err != nil {
		logrus.Errorf("set mem resource failed, container name: %s, error: %v", container.Name, err)
		return err
	}

	logrus.Debugf("container name: %s, resource: %+v", container.Name, container.Resources)

	return nil
}

func (k *Kubernetes) UpdateContainerResourceEnv(originResource apistructs.Resources, container *apiv1.Container) {
	for index, env := range container.Env {
		var needToUpdate = false
		switch env.Name {
		case "DICE_CPU_ORIGIN":
			needToUpdate = true
			env.Value = fmt.Sprintf("%f", originResource.Cpu)
		case "DICE_CPU_REQUEST":
			needToUpdate = true
			env.Value = container.Resources.Requests.Cpu().AsDec().String()
		case "DICE_CPU_LIMIT":
			needToUpdate = true
			env.Value = container.Resources.Limits.Cpu().AsDec().String()
		case "DICE_MEM_ORIGIN":
			needToUpdate = true
			env.Value = fmt.Sprintf("%f", originResource.Mem)
		case "DICE_MEM_REQUEST":
			needToUpdate = true
			env.Value = fmt.Sprintf("%d", container.Resources.Requests.Memory().Value()/1024/1024)
		case "DICE_MEM_LIMIT":
			needToUpdate = true
			env.Value = fmt.Sprintf("%d", container.Resources.Limits.Memory().Value()/1024/1024)
		}
		if needToUpdate {
			container.Env[index] = env
		}
	}
	return
}

// DereferenceEnvs dereferences envs if the placeholder ${env.PLACEHOLDER} in the env.
func DereferenceEnvs(podTempate *apiv1.PodTemplateSpec) error {
	var (
		left, right = "${", "}"
		find        = func(p string) bool { return strings.HasPrefix(p, "env.") }
	)
	for i, container := range podTempate.Spec.Containers {
		var envMap = make(map[string]string)
		for _, env := range container.Env {
			if env.ValueFrom == nil {
				envMap[env.Name] = env.Value
			}
		}
		for j := range container.Env {
			name, value := container.Env[j].Name, container.Env[j].Value
			for {
				placeholder, indexStart, indexEnd, err := strutil.FirstCustomExpression(value, left, right, find)
				if err != nil {
					return err
				}
				if indexStart == indexEnd {
					break
				}
				placeholder = strings.TrimPrefix(placeholder, "env.")
				kv := strings.Split(placeholder, ":")
				placeholder = kv[0]
				if placeholder == name {
					return errors.Errorf("loop reference in env name %s", name)
				}
				v, ok := envMap[placeholder]
				if !ok {
					if len(kv) > 1 {
						v = strings.TrimPrefix(kv[1], " ")
					} else {
						return errors.Errorf("env reference not found and default value not be set: %s", placeholder)
					}
				}
				value = strutil.Replace(value, v, indexStart, indexEnd)
			}
			podTempate.Spec.Containers[i].Env[j].Value = value
		}
	}
	return nil
}

// inherit Labels from service.Labels and service.DeploymentLabels
// Now, some of these labels used for AliCloud ECI Pod Annotations (https://www.alibabacloud.com/help/zh/doc-detail/144561.htm).
// But, this can be used for setting any pod labels as wished.
func inheritDeploymentLabels(service *apistructs.Service, deployment *appsv1.Deployment) error {
	if deployment == nil {
		return nil
	}

	if deployment.Labels == nil {
		deployment.Labels = make(map[string]string)
	}

	if deployment.Spec.Template.Labels == nil {
		deployment.Spec.Template.Labels = make(map[string]string)
	}

	hasHostPath := serviceHasHostpath(service)

	err := setPodLabelsFromService(hasHostPath, service.Labels, deployment.Labels)
	if err != nil {
		return errors.Errorf("error in service.Labels: for deployment %v in namesapce %v with error: %v\n", deployment.Name, deployment.Namespace, err)
	}

	err = setPodLabelsFromService(hasHostPath, service.Labels, deployment.Spec.Template.Labels)
	if err != nil {
		return errors.Errorf("error in service.Labels: for deployment %v in namesapce %v with error: %v\n", deployment.Name, deployment.Namespace, err)
	}

	err = setPodLabelsFromService(hasHostPath, service.DeploymentLabels, deployment.Spec.Template.Labels)
	if err != nil {
		return errors.Errorf("error in service.DeploymentLabels: for deployment %v in namesapce %v with error: %v\n", deployment.Name, deployment.Namespace, err)
	}

	return nil
}

// inherit Labels from service.Labels and service.DeploymentLabels
// Now, some of these labels used for AliCloud ECI Pod Annotations (https://www.alibabacloud.com/help/zh/doc-detail/144561.htm).
// But, this can be used for setting any pod labels as wished.
func inheritDaemonsetLabels(service *apistructs.Service, daemonset *appsv1.DaemonSet) error {
	if daemonset == nil {
		return nil
	}

	if daemonset.Labels == nil {
		daemonset.Labels = make(map[string]string)
	}

	if daemonset.Spec.Template.Labels == nil {
		daemonset.Spec.Template.Labels = make(map[string]string)
	}

	hasHostPath := serviceHasHostpath(service)

	err := setPodLabelsFromService(hasHostPath, service.Labels, daemonset.Labels)
	if err != nil {
		return errors.Errorf("error in service.Labels: for daemonset %v in namesapce %v with error: %v\n", daemonset.Name, daemonset.Namespace, err)
	}

	for lk, lv := range daemonset.Labels {
		if lk == apistructs.AlibabaECILabel && lv == "true" {
			return errors.Errorf("error in service.Labels: for daemonset %v in namesapce %v with error: ECI not support daemonset, do not set lables %s='true' for daemonset", daemonset.Name, daemonset.Namespace, lk)
		}
	}

	err = setPodLabelsFromService(hasHostPath, service.Labels, daemonset.Spec.Template.Labels)
	if err != nil {
		return errors.Errorf("error in service.Labels: for daemonset %v in namesapce %v with error: %v\n", daemonset.Name, daemonset.Namespace, err)
	}

	err = setPodLabelsFromService(hasHostPath, service.DeploymentLabels, daemonset.Spec.Template.Labels)
	if err != nil {
		return errors.Errorf("error in service.DeploymentLabels: for daemonset %v in namesapce %v with error: %v\n", daemonset.Name, daemonset.Namespace, err)
	}

	for lk, lv := range daemonset.Spec.Template.Labels {
		if lk == apistructs.AlibabaECILabel && lv == "true" {
			return errors.Errorf("error in service.DeploymentLabels: for daemonset %v in namesapce %v with error: ECI not support daemonset, do not set lables %s='true' for daemonset.\n", daemonset.Name, daemonset.Namespace, lk)
		}
	}

	return nil
}

// setPodLabelsFromService
func setPodLabelsFromService(hasHostPath bool, labels map[string]string, podLabels map[string]string) error {
	for key, value := range labels {
		if len(value) > 63 {
			logrus.Warnf("Label key: %s with Invalid value: %s: must be no more than 63 characters", key, value)
			logrus.Warnf("Label key: %s with value: %s will not convert to j8s label.", key, value)
			continue
		}

		if errs := validation.IsValidLabelValue(value); len(errs) > 0 {
			logrus.Warnf("Label key: %s with invalid value: %s will not convert to j8s label.", key, value)
			continue
		}

		// labels begin with `annotations/` from service.Labels and service.DeploymentLabels used for
		// generating Pod Annotations, not for Labels
		if strings.HasPrefix(key, LabelKeyPrefix) {
			continue
		}
		// HostPath not supported in AliCloud ECI
		if hasHostPath && key == apistructs.AlibabaECILabel && value == "true" {
			return errors.Errorf("can not create ECI Pod with hostPath")
		}

		if _, ok := podLabels[key]; !ok {
			podLabels[key] = value
		}
	}
	return nil
}

// Set pod Annotations from service.Labels and service.DeploymentLabels
// Now, these annotations used for AliCloud ECI Pod Annotations (https://www.alibabacloud.com/help/zh/doc-detail/144561.htm).
// But, this can be used for setting any pod annotations as wished.
func setPodAnnotationsFromLabels(service *apistructs.Service, podannotations map[string]string) {
	if podannotations == nil {
		return
	}

	for key, value := range service.Labels {
		// labels begin with `annotations/` from service.Labels and service.DeploymentLabels used for
		// generating Pod Annotations, not for Labels
		if strings.HasPrefix(key, LabelKeyPrefix) {
			podannotations[strings.TrimPrefix(key, LabelKeyPrefix)] = value
			continue
		}

		if key == apistructs.AlibabaECILabel && value == "true" {
			images := strings.Split(service.Image, "/")
			if len(images) >= 2 {
				podannotations[diceyml.AddonImageRegistry] = images[0]
			}
		}
	}
	for key, value := range service.DeploymentLabels {
		// labels begin with `annotations/` from service.Labels and service.DeploymentLabels used for
		// generating Pod Annotations, not for Labels
		if strings.HasPrefix(key, LabelKeyPrefix) {
			podannotations[strings.TrimPrefix(key, LabelKeyPrefix)] = value
			continue
		}

		if key == apistructs.AlibabaECILabel && value == "true" {
			images := strings.Split(service.Image, "/")
			if len(images) >= 2 {
				podannotations[diceyml.AddonImageRegistry] = images[0]
			}
		}
	}
}

func serviceHasHostpath(service *apistructs.Service) bool {
	for _, bind := range service.Binds {
		if strutil.HasPrefixes(bind.HostPath, "/") {
			return true
		}
	}
	return false
}

func GenerateECIPodSidecarContainers(inEdge bool) (apiv1.Container, error) {
	sc := apiv1.Container{
		Name: "fluent-bit",
		//Image: sidecar.Image,
		Resources: apiv1.ResourceRequirements{
			Requests: apiv1.ResourceList{
				apiv1.ResourceCPU:    resource.MustParse("0.1"),
				apiv1.ResourceMemory: resource.MustParse("256Mi"),
			},
			Limits: apiv1.ResourceList{
				apiv1.ResourceCPU:    resource.MustParse("1"),
				apiv1.ResourceMemory: resource.MustParse("1Gi"),
			},
		},
		VolumeMounts: []apiv1.VolumeMount{},
	}

	if err := getSideCarConfigFromConfigMapVolumeFiles(&sc, inEdge); err != nil {
		logrus.Errorf("Can not create log collector sidecar container for ECI Pod, error：%v", err)
		return sc, errors.Errorf("Can not create log collector sidecar container for ECI Pod, error：%v", err)
	}

	sc.VolumeMounts = append(sc.VolumeMounts, apiv1.VolumeMount{
		Name:      "erda-volume",
		MountPath: "/erda",
		ReadOnly:  false, // rw
	})

	sc.VolumeMounts = append(sc.VolumeMounts, apiv1.VolumeMount{
		Name:      "stdlog",
		MountPath: "/stdlog",
		ReadOnly:  false, // rw
	})

	return sc, nil
}

func SetPodContainerLifecycleAndSharedVolumes(podSpec *apiv1.PodSpec) {
	for i := range podSpec.Containers {
		if len(podSpec.Containers[i].VolumeMounts) == 0 {
			podSpec.Containers[i].VolumeMounts = make([]apiv1.VolumeMount, 0)
		}
		podSpec.Containers[i].VolumeMounts = append(podSpec.Containers[i].VolumeMounts, apiv1.VolumeMount{
			Name:      "erda-volume",
			MountPath: "/erda",
			ReadOnly:  false, // rw
		})

		podSpec.Containers[i].VolumeMounts = append(podSpec.Containers[i].VolumeMounts, apiv1.VolumeMount{
			Name:      "stdlog",
			MountPath: "/stdlog",
			ReadOnly:  false, // rw
		})
		lifecyclePostStartExecCmd := fmt.Sprintf("mkdir -p /erda/containers/%s && cp /proc/self/cpuset /erda/containers/%s/cpuset", podSpec.Containers[i].Name, podSpec.Containers[i].Name)
		podSpec.Containers[i].Lifecycle = &apiv1.Lifecycle{
			PostStart: &apiv1.Handler{
				Exec: &apiv1.ExecAction{
					Command: []string{"sh", "-c", lifecyclePostStartExecCmd},
				},
			},
		}
	}

	if len(podSpec.Volumes) == 0 {
		podSpec.Volumes = make([]apiv1.Volume, 0)
	}
	podSpec.Volumes = append(podSpec.Volumes, apiv1.Volume{
		Name: "erda-volume",
		VolumeSource: apiv1.VolumeSource{
			EmptyDir: &apiv1.EmptyDirVolumeSource{},
		},
	})
	podSpec.Volumes = append(podSpec.Volumes, apiv1.Volume{
		Name: "stdlog",
		VolumeSource: apiv1.VolumeSource{
			FlexVolume: &apiv1.FlexVolumeSource{
				Driver: "alicloud/pod-stdlog",
			},
		},
	})
}

func getSideCarConfigFromConfigMapVolumeFiles(sc *apiv1.Container, inEdge bool) error {
	if sc.Env == nil {
		sc.Env = make([]apiv1.EnvVar, 0)
	}

	for _, envKey := range ECIPodSidecarENVFromScheduler {
		envValue := os.Getenv(envKey)
		if envKey == ECIPodFluentbitSidecarImageEnvName {
			if envValue != "" {
				sc.Image = envValue
			} else {
				sc.Image = ECIPodFluentbitSidecarImage
			}
			continue
		} else {
			switch envKey {
			case "ECI_POD_FLUENTBIT_CONFIG_FILE":
				envValue = getEnvFromName(ECIPodFluentbitSidecarConfigFileEnvName)
				if envValue == "" {
					envValue = ECIPodFluentbitSidecarConfigFileEnvValue
				}
			}
		}

		sc.Env = append(sc.Env, apiv1.EnvVar{
			Name:  strutil.TrimPrefixes(envKey, "ECI_POD_FLUENTBIT_"),
			Value: envValue,
		})
	}

	// COLLECTOR_ADDR
	ns := os.Getenv("DICE_NAMESPACE")
	if ns == "" {
		ns = "default"
	}
	sc.Env = append(sc.Env, apiv1.EnvVar{
		Name:  "COLLECTOR_ADDR",
		Value: fmt.Sprintf("collector.%s.svc.cluster.local:7076", ns),
	})

	// COLLECTOR_PUBLIC_URL
	collcror_pubilic_url := os.Getenv("COLLECTOR_PUBLIC_URL")
	if collcror_pubilic_url != "" {
		sc.Env = append(sc.Env, apiv1.EnvVar{
			Name:  "COLLECTOR_PUBLIC_URL",
			Value: collcror_pubilic_url,
		})
	}

	if inEdge {
		sc.Env = append(sc.Env, apiv1.EnvVar{
			Name:  "DICE_IS_EDGE",
			Value: "true",
		})
	} else {
		sc.Env = append(sc.Env, apiv1.EnvVar{
			Name:  "DICE_IS_EDGE",
			Value: "false",
		})
	}

	return nil
}

func UseECI(controllerLabels, PodLabels map[string]string) bool {
	if len(controllerLabels) == 0 && len(PodLabels) == 0 {
		return false
	}

	if value, ok := controllerLabels[apistructs.AlibabaECILabel]; ok {
		if value == "true" {
			return true
		}
	}

	if value, ok := PodLabels[apistructs.AlibabaECILabel]; ok {
		if value == "true" {
			return true
		}
	}
	return false
}

func getEnvFromName(envName string) string {
	value := os.Getenv(envName)
	if value != "" {
		return value
	}
	return ""
}
