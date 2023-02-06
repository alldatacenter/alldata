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
	"fmt"
	"strconv"
	"strings"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/k8serror"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/util"
	"github.com/erda-project/erda/pkg/istioctl"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/strutil"
)

// ValidateRuntime check runtime validity
func ValidateRuntime(specObj interface{}, action string) (*apistructs.ServiceGroup, error) {
	sg, ok := specObj.(apistructs.ServiceGroup)
	if !ok || len(sg.Services) == 0 {
		return nil, errors.Errorf("invalid service spec, action: %s", action)
	}

	if strutil.Contains(sg.Type, "--") {
		return nil, errors.Errorf("failed to validate runtime, action: %s, name: %s, namespace: %s, (namespace cannot contain consecutive dash)",
			action, sg.ID, sg.Type)
	}
	var ns = MakeNamespace(&sg)
	if !IsGroupStateful(&sg) && sg.ProjectNamespace != "" {
		ns = sg.ProjectNamespace
	}
	// init runtime.Services' namespace for later usage
	for i := range sg.Services {
		sg.Services[i].Namespace = ns
	}
	return &sg, nil
}

func (k *Kubernetes) createRuntime(ctx context.Context, sg *apistructs.ServiceGroup) error {
	var ns = MakeNamespace(sg)
	if sg.ProjectNamespace != "" && !IsGroupStateful(sg) {
		ns = sg.ProjectNamespace
	}
	// return error if this namespace exists
	if err := k.CreateNamespace(ns, sg); err != nil {
		return err
	}

	layers, err := util.ParseServiceDependency(sg)
	if err != nil {
		return err
	}

	// statefulset application
	if IsGroupStateful(sg) {
		return k.CreateStatefulGroup(ctx, sg, layers)
	}

	// stateless application
	return k.createStatelessGroup(ctx, sg, layers)
}

func (k *Kubernetes) destroyRuntime(ns string) error {
	// Deleting a namespace will cascade delete the resources under that namespace
	logrus.Debugf("delete the kubernetes namespace %s", ns)
	return k.DeleteNamespace(ns)
}

func (k *Kubernetes) destroyRuntimeByProjectNamespace(ns string, sg *apistructs.ServiceGroup) error {
	for _, service := range sg.Services {
		err := k.deleteProjectService(&service)
		if err != nil {
			return fmt.Errorf("delete service %s error: %v", service.ProjectServiceName, err)
		}

		switch service.WorkLoad {
		case ServicePerNode:
			err = k.deleteDaemonSet(ns, service.ProjectServiceName)
		case ServiceJob:
			err = k.deleteJob(ns, service.Name)
		default:
			err = k.deleteDeployment(ns, service.ProjectServiceName)
		}
		if err != nil && !util.IsNotFound(err) {
			return fmt.Errorf("delete resource %s, %s error: %v", service.WorkLoad, service.ProjectServiceName, err)
		}

		labelSelector := map[string]string{
			"app": service.Name,
		}

		deploys, err := k.deploy.List(ns, labelSelector)
		if err != nil {
			return fmt.Errorf("list pod resource error: %v in the namespace %s", err, ns)
		}

		remainCount := 0
		for _, deploy := range deploys.Items {
			if deploy.DeletionTimestamp == nil {
				remainCount++
			}
		}

		if remainCount < 1 {
			logrus.Debugf("delete the kubernetes service %s on namespace %s", service.Name, service.Namespace)
			err = k.service.Delete(ns, service.Name)
			if err != nil {
				return fmt.Errorf("delete service %s error: %v", service.Name, err)
			}
			if k.istioEngine != istioctl.EmptyEngine {
				err = k.istioEngine.OnServiceOperator(istioctl.ServiceDelete, &service)
				if err != nil {
					return fmt.Errorf("delete istio resource error: %v", err)
				}
			}
			logrus.Debugf("delete the kubernetes service %s on namespace %s finished", service.Name, service.Namespace)
		}
	}
	return nil
}

func (k *Kubernetes) updateRuntime(ctx context.Context, sg *apistructs.ServiceGroup) error {
	var ns = MakeNamespace(sg)
	if sg.ProjectNamespace != "" && !IsGroupStateful(sg) {
		ns = sg.ProjectNamespace
	}
	if err := k.UpdateNamespace(ns, sg); err != nil {
		errMsg := fmt.Sprintf("update namespace err: %v", err)
		logrus.Error(errMsg)
		return fmt.Errorf(errMsg)
	}
	// Stateful apps don’t support updates yet
	if IsGroupStateful(sg) {
		return errors.Errorf("Not supported for updating stateful applications")
	}
	return k.updateOneByOne(ctx, sg)
}

func (k *Kubernetes) createStatelessGroup(ctx context.Context, sg *apistructs.ServiceGroup, layers [][]*apistructs.Service) error {
	var ns = MakeNamespace(sg)
	if sg.ProjectNamespace != "" {
		ns = sg.ProjectNamespace
	}

	var errOccurred error
	var err error
	for _, layer := range layers {
		// services in one layer could be create in parallel, BUT NO NEED
		for _, service := range layer {
			service.Namespace = ns
			// logrus.Debugf("in Create, going to create service(%s/%s)", service.Namespace, service.Name)
			// As long as one of the services fails to create, then the successfully created services are cleared
			// In this case, create a new state and return to the upper level
			if err = k.createOne(ctx, service, sg); err == nil {
				continue
			}
			errOccurred = err
			logrus.Errorf("failed to create serivce and going to destroy servicegroup, name: %s, ns: %s, (%v)",
				service.Name, service.Namespace, err)
			if IsQuotaError(err) && sg.ProjectNamespace != "" {
				continue
			}
			defer func() {
				logrus.Debugf("revert resource when create runtime %s failed", sg.ID)
				var delErr error
				if sg.ProjectNamespace == "" {
					delErr = k.destroyRuntime(ns)
				} else {
					delErr = k.destroyRuntimeByProjectNamespace(ns, sg)
				}
				if delErr == nil {
					logrus.Infof("succeed to delete namespace, ns: %s", ns)
					return
				}

				if k8serror.NotFound(delErr) {
					logrus.Infof("failed to destroy namespace, ns: %s, (namespace not found)", ns)
					return
				}
				// There will be residual resources, requiring manual operation and maintenance
				logrus.Errorf("failed to destroy resource, ns: %s, (%v)", ns, delErr)
				return
			}()
			return err
		}
	}
	return errOccurred
}

// CreateStatefulGroup create statefull group
func (k *Kubernetes) CreateStatefulGroup(ctx context.Context, sg *apistructs.ServiceGroup, layers [][]*apistructs.Service) error {
	if sg == nil || len(layers) == 0 {
		return k8serror.ErrInvalidParams
	}
	// Judge the group from the label, each group is a statefulset
	groups, err := groupStatefulset(sg)
	if err != nil {
		logrus.Infof(err.Error())
		return err
	}
	ns := MakeNamespace(sg)
	// Decompose into a statefulset
	if len(groups) == 1 {
		logrus.Infof("create one statefulset, name: %s", sg.ID)
		if err := k.createStatefulService(sg); err != nil {
			return err
		}
		// Preprocess to obtain the environment variables of all services, since there is only one statefulset, globalSeq is set to 0
		annotations := initAnnotations(layers, 0)

		annotations["RUNTIME_NAMESPACE"] = sg.Type
		annotations["RUNTIME_NAME"] = sg.ID
		//annotations["GROUP_ID"] = sg.Services[0].Labels[groupID]
		annotations["GROUP_ID"], _ = getGroupID(&sg.Services[0])
		annotations["K8S_NAMESPACE"] = ns

		allEnv := k.initGroupEnv(layers, annotations)

		// The upper layer guarantees that the same group must be the same image
		info := StatefulsetInfo{
			sg:          sg,
			namespace:   ns,
			envs:        allEnv,
			annotations: annotations,
		}
		return k.createStatefulSet(ctx, info)
	}

	logrus.Infof("statefulset groups: %+v", groups)

	globalAnno := map[string]string{}
	var groupLayersArray [][][]*apistructs.Service

	for i := range groups {
		groupLayers, err := util.ParseServiceDependency(groups[i])
		if err != nil {
			logrus.Errorf("failed to parse stateful groups, name: %v, (%v)", groups[i].ID, err)
			return err
		}
		annotations := initAnnotations(groupLayers, i)
		logrus.Infof("get group annotations, groupid: %s, anno: %+v", groups[i].ID, annotations)
		for k, v := range annotations {
			globalAnno[k] = v
		}
		groupLayersArray = append(groupLayersArray, groupLayers)
	}

	logrus.Infof("debug createStatefulGroup, globalAnno: %+v", globalAnno)

	globalAnno["RUNTIME_NAMESPACE"] = sg.Type
	globalAnno["RUNTIME_NAME"] = sg.ID
	globalAnno["K8S_NAMESPACE"] = ns

	for i := range groups {
		groupLayers := groupLayersArray[i]
		groupEnv := k.initGroupEnv(groupLayers, globalAnno)

		// todo: how to name it?
		groupName := groups[i].Services[0].Name
		if idx := strings.LastIndex(groupName, "-"); idx > 0 {
			groupName = groupName[:idx]
		}

		// GO-mysql, G1-mysql
		groups[i].ID = strutil.Concat("G", strconv.Itoa(i), "-", groupName)

		if err := k.createStatefulService(groups[i]); err != nil {
			return err
		}
		info := StatefulsetInfo{
			sg:          groups[i],
			namespace:   ns,
			envs:        groupEnv,
			annotations: globalAnno,
		}
		if err := k.createStatefulSet(ctx, info); err != nil {
			logrus.Errorf("failed to create one stateful group, name: %v, (%v)", groups[i].ID, err)
		}
	}

	return nil
}

// IsGroupStateful Determine whether it is a stateful service
// the caller have to make sure sg is not nil
// Generally speaking, those with "SERVICE_TYPE" set to "ADDONS" are stateful applications,
// However, some of the ADDONS types still want to be deployed in a stateless manner, adding STATELESS_SERVICE
// To distinguish whether the ADDONS type is stateful or stateless, the default is to execute according to state
func IsGroupStateful(sg *apistructs.ServiceGroup) bool {
	if sg.Labels[ServiceType] == ServiceAddon {
		if sg.Labels[StatelessService] != IsStatelessService {
			return true
		}
	}
	return false
}

func (k *Kubernetes) getGroupStatus(ctx context.Context, sg *apistructs.ServiceGroup) (apistructs.StatusDesc, error) {
	if IsGroupStateful(sg) {
		return k.GetStatefulStatus(sg)
	}
	return k.getStatelessStatus(ctx, sg)
}

func (k *Kubernetes) inspectStateless(sg *apistructs.ServiceGroup) (*apistructs.ServiceGroup, error) {
	var ns = MakeNamespace(sg)
	if sg.ProjectNamespace != "" {
		ns = sg.ProjectNamespace
		k.setProjectServiceName(sg)
	}

	for i, svc := range sg.Services {
		serviceName := getServiceName(&svc)
		if len(svc.Ports) == 0 {
			continue
		}
		serviceHost := strutil.Join([]string{serviceName, ns, "svc.cluster.local"}, ".")
		sg.Services[i].ProxyIp = serviceHost
		sg.Services[i].Vip = serviceHost
		sg.Services[i].ShortVIP = serviceHost
		sg.Services[i].ProxyPorts = diceyml.ComposeIntPortsFromServicePorts(svc.Ports)
	}
	return sg, nil
}

func (k *Kubernetes) InspectStateful(sg *apistructs.ServiceGroup) (*apistructs.ServiceGroup, error) {
	namespace := MakeNamespace(sg)
	name := statefulsetName(sg)
	// have only one statefulset
	if !strings.HasPrefix(namespace, "group-") {
		info, err := k.inspectOne(sg, namespace, name, 0)
		if err != nil {
			return nil, err
		}
		return info.sg, nil
	}
	return k.inspectGroup(sg, namespace, name)
}
