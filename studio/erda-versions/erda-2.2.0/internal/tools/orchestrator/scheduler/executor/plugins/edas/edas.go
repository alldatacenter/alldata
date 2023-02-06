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

package edas

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"math"
	"net/http"
	"os"
	"regexp"
	"sort"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/aliyun/alibaba-cloud-sdk-go/sdk/requests"
	api "github.com/aliyun/alibaba-cloud-sdk-go/services/edas"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	apiv1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/intstr"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/events"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/events/eventtypes"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/executortypes"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/deployment"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/k8sapi"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/k8serror"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/k8sservice"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/resourceinfo"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/util"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	kind = "EDAS"
	// The number of cycles to query the deployment results
	loopCount = 2 * 60
	// edas k8s service namespace
	defaultNamespace = "default"
	// json prefix key
	prefixKey = "/dice/plugins/edas/"
	// k8s min ready seconds
	minReadySeconds    = 30
	notFound           = "not found"
	k8sServiceNotFound = "not found k8s service"
	appNameLengthLimit = 36
	// service name env of edas service
	diceServiceName = "DICE_SERVICE_NAME"
)

var deleteOptions = &k8sapi.CascadingDeleteOptions{
	Kind:       "DeleteOptions",
	APIVersion: "v1",
	// 'Foreground' - a cascading policy that deletes all dependents in the foreground
	// e.g. if you delete a deployment, this option would delete related replicaSets and pods
	// See more: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.10/#delete-24
	PropagationPolicy: string(metav1.DeletePropagationBackground),
}

// EDAS plugin's configure
func init() {
	executortypes.Register(kind, func(name executortypes.Name, clustername string, options map[string]string, optionsPlus interface{}) (
		executortypes.Executor, error) {
		addr, ok := options["ADDR"]
		if !ok {
			return nil, errors.Errorf("not found edas address in env variables")
		}
		accessKey, ok := options["ACCESSKEY"]
		if !ok {
			return nil, errors.Errorf("not found edas accessKey in env variables")
		}
		accessSecret, ok := options["ACCESSSECRET"]
		if !ok {
			return nil, errors.Errorf("not found edas accessKey in env variables")
		}
		clusterID, ok := options["CLUSTERID"]
		if !ok {
			return nil, errors.Errorf("not found edas clusterId in env variables")
		}
		regionID, ok := options["REGIONID"]
		if !ok {
			regionID = "cn-hangzhou"
		}

		unlimitCPU, ok := options["UNLIMITCPU"]
		if !ok {
			unlimitCPU = "false"
		}

		logicalRegionID, ok := options["LOGICALREGIONID"]
		if !ok {
			logicalRegionID = ""
		}

		client, err := api.NewClientWithAccessKey(regionID, accessKey, accessSecret)
		if err != nil {
			return nil, errors.Wrap(err, "failed to new edas client with accessKey")
		}
		client.GetConfig().Transport = &http.Transport{
			DisableCompression: true,
		}

		bdl := bundle.New(bundle.WithClusterManager())
		clusterInfo, err := bdl.GetCluster(clustername)
		if err != nil {
			return nil, errors.Errorf("get clustername %s cluster info err %v", clustername, err)
		}

		kubeAddr, kubeClient, err := util.GetClient(clustername, clusterInfo.ManageConfig)
		if err != nil {
			return nil, errors.Errorf("get http client err %v", err)
		}

		regAddr, ok := options["REGADDR"]
		if !ok {
			return nil, errors.Errorf("not found dice registry addr in env variables")
		}

		k8sDeployClient := deployment.New(deployment.WithCompleteParams(kubeAddr, kubeClient))
		k8sSvcClient := k8sservice.New(k8sservice.WithCompleteParams(kubeAddr, kubeClient))
		notifier, err := events.New(string(name), nil)
		if err != nil {
			logrus.Errorf("executor(%s) call eventbox new api error: %v", name, err)
			return nil, err
		}
		resourceInfo := resourceinfo.New(kubeAddr, kubeClient)

		edas := &EDAS{
			name:            name,
			options:         options,
			addr:            addr,
			kubeAddr:        kubeAddr,
			regAddr:         regAddr,
			regionID:        regionID,
			logicalRegionID: logicalRegionID,
			accessKey:       accessKey,
			accessSecret:    accessSecret,
			clusterID:       clusterID,
			client:          client,
			kubeClient:      kubeClient,
			notifier:        notifier,
			k8sDeployClient: k8sDeployClient,
			k8sSvcClient:    k8sSvcClient,
			unlimitCPU:      unlimitCPU,
			resourceInfo:    resourceInfo,
		}

		if disableEvent := os.Getenv("DISABLE_EVENT"); disableEvent == "true" {
			return edas, nil
		}
		evCh := make(chan *eventtypes.StatusEvent, 10)
		// key is {runtimeNamespace}/{runtimeName}, value is spec.ServiceGroup
		lstore := &sync.Map{}
		stopCh := make(chan struct{}, 1)
		edas.registerEventChanAndLocalStore(evCh, stopCh, lstore)
		return edas, nil
	})
}

// EDAS edas server structure
type EDAS struct {
	name     executortypes.Name
	options  map[string]string
	addr     string
	kubeAddr string
	regAddr  string
	// default: "cn-hangzhou"
	regionID string
	// EDAS namespace, the default is the same as regionID
	logicalRegionID string
	// key & secret of edas openAPI
	accessKey    string
	accessSecret string
	// EDAS Cluster ID
	clusterID string
	// edas pop client
	client          *api.Client
	kubeClient      *httpclient.HTTPClient
	notifier        events.Notifier
	k8sDeployClient *deployment.Deployment
	k8sSvcClient    *k8sservice.Service
	// Whether to limit the application of CPU resources less than 1c
	unlimitCPU   string
	resourceInfo *resourceinfo.ResourceInfo
}

// Kind executor kind
func (e *EDAS) Kind() executortypes.Kind {
	return kind
}

// Name executor name
func (e *EDAS) Name() executortypes.Name {
	return e.name
}

func makeEdasKey(namespace, name string) string {
	return "/dice/plugins/edas/" + namespace + "/" + name
}

func checkRuntime(r *apistructs.ServiceGroup) error {
	group := r.Type + "-" + r.ID
	length := appNameLengthLimit - len(group)

	var regexString = "^[A-Za-z_][A-Za-z0-9_]*$"

	for _, s := range r.Services {
		if len(s.Name) > length {
			return errors.Errorf("edas app name is longer than %d characters, name: %s",
				appNameLengthLimit, group+s.Name)
		}

		for k := range s.Env {
			match, err := regexp.MatchString(regexString, k)
			if err != nil {
				errMsg := fmt.Sprintf("regexp env key err %v", err)
				logrus.Errorf(errMsg)
				return errors.New(errMsg)
			}
			if !match {
				errMsg := fmt.Sprintf("key %s not match the regex express %s", k, regexString)
				logrus.Errorf(errMsg)
				return errors.New(errMsg)
			}
		}
	}
	return nil
}

// Create edas create runtime
func (e *EDAS) Create(ctx context.Context, specObj interface{}) (interface{}, error) {
	var err error

	errChan := make(chan error, 1)

	runtime, ok := specObj.(apistructs.ServiceGroup)
	if !ok {
		return nil, errors.New("edas k8s create: invalid runtime spec")
	}

	if err := checkRuntime(&runtime); err != nil {
		return nil, err
	}

	logrus.Debugf("[EDAS] Create runtime, object: %+v", runtime)

	group := runtime.Type + "-" + runtime.ID

	flows, err := util.ParseServiceDependency(&runtime)
	if err != nil {
		return nil, errors.Wrapf(err, "parse service flow, runtime: %s", group)
	}

	go func() {
		if err = e.runAppFlow(context.Background(), flows, &runtime); err != nil {
			logrus.Errorf("[EDAS] failed to run runtime service flow: %v", err)

			// remove runtime & ignore error
			e.Remove(ctx, specObj)
			errChan <- err

			return
		}
	}()

	select {
	case err := <-errChan:
		close(errChan)
		return nil, err
	case <-ctx.Done():
	case <-time.After(5 * time.Second):
	}

	return nil, nil
}

// Destroy edas destory runtime
func (e *EDAS) Destroy(ctx context.Context, specObj interface{}) error {
	return e.Remove(ctx, specObj)
}

// Status edas status of runtime
func (e *EDAS) Status(ctx context.Context, specObj interface{}) (apistructs.StatusDesc, error) {
	var status apistructs.StatusDesc

	// Initialize it to prevent the upper console from being unrecognized
	status.Status = apistructs.StatusUnknown

	runtime, ok := specObj.(apistructs.ServiceGroup)
	if !ok {
		return status, errors.New("edas k8s status: invalid runtime spec")
	}
	group := runtime.Type + "-" + runtime.ID
	for i, svc := range runtime.Services {
		svc.Namespace = runtime.Type
		// init status
		runtime.Services[i].Status = apistructs.StatusUnknown

		// check k8s deployment status
		status, _, err := e.getDeploymentStatus(group, &svc)
		if err != nil {
			logrus.Errorf("[edas] get deployment from k8s error: %+v", err)
			continue
		}
		if status.Status != apistructs.StatusReady {
			status.LastMessage = fmt.Sprintf("deployment(%s) status is %v", runtime.ID, status)
			status.Status = apistructs.StatusError
			logrus.Errorf("[edas] k8s deployment(%s) status is not ready: %+v", group, status.LastMessage)
			continue
		}

		appName := group + "-" + svc.Name

		if len(svc.Ports) != 0 {
			if _, err := e.getK8sService(appName); err != nil {
				status.LastMessage = fmt.Sprintf("deployment(%s): service(%s) is not found or error", runtime.ID, appName)
				logrus.Errorf("[edas] k8s service status is not ready: %+v", status.LastMessage)
				status.Status = apistructs.StatusError
				continue
			}
		}

		runtime.Services[i].Status = apistructs.StatusReady
	}

	isReady := true
	for _, s := range runtime.Services {
		if s.Status != apistructs.StatusReady {
			isReady = false
			break
		}
	}

	//All services are ready, the runtime is set to ready, otherwise the state remains as the value during the calculation process
	if isReady {
		status.Status = apistructs.StatusReady
	}

	return status, nil
}

//Because it takes a long time for edas to delete the interface, which exceeds the timeout set by the console, the app is deleted in parallel
// Remove edas remove runtime
func (e *EDAS) Remove(ctx context.Context, specObj interface{}) error {
	var err error

	errChan := make(chan error, 1)

	runtime, ok := specObj.(apistructs.ServiceGroup)
	if !ok {
		return errors.New("edas k8s remove: invalid runtime spec")
	}

	group := runtime.Type + "-" + runtime.ID

	go func(ss []apistructs.Service) {
		for _, srv := range ss {
			// TODO: how to handle the error
			if err = e.removeService(ctx, group, &srv); err != nil {
				errChan <- err
			}
		}
		close(errChan)
	}(runtime.Services)

	// HACK: edas api Inevitable timeout (> 10s), here you need to wait for 5s
	select {
	case err := <-errChan:
		return err
	case <-ctx.Done():
	case <-time.After(5 * time.Second):
	}

	return nil
}

// Update edas update runtime
func (e *EDAS) Update(ctx context.Context, specObj interface{}) (interface{}, error) {
	var err error
	var oldRun apistructs.ServiceGroup

	newRun, ok := specObj.(apistructs.ServiceGroup)
	if !ok {
		return nil, errors.New("edas k8s update: invalid runtime spec")
	}

	if err := checkRuntime(&newRun); err != nil {
		return nil, err
	}

	// Get the deploy list from k8s API
	logrus.Debugf("[EDAS] Update runtime, new runtime: %+v", newRun)
	if _, err = e.getK8sDeployList(newRun.Type, newRun.ID, &oldRun.Services); err != nil {
		logrus.Debugf("[EDAS] Get deploy from k8s error: %+v", err)
		return nil, err
	}
	logrus.Debugf("[EDAS] Old runtime service list is : %+v", oldRun.Services)
	if err = e.cyclicUpdateService(ctx, &newRun, &oldRun); err != nil {
		logrus.Debugf("[EDAS] Cyclic update service error: %+v", err)
		return nil, err
	}
	return nil, nil
}

//Get the deploy list of corresponding runtime from k8s api
func (e *EDAS) getK8sDeployList(namespace string, name string, services *[]apistructs.Service) (interface{}, error) {
	var err error
	var edasAhasName string
	var kubeSvc *k8sapi.Service
	var port int32
	var cpu int64
	var mem int64
	var replicas int32
	var image string
	group := namespace + "-" + name
	logrus.Debugf("[EDAS] get deploylist from group: %+v", group)

	deployList, err := e.k8sDeployClient.List(defaultNamespace, nil)
	if err != nil {
		return nil, err
	}
	logrus.Debugf("[EDAS] get deploylist of old runtime from k8s : %+v", deployList.Items)
	for _, i := range deployList.Items {
		// Get the deployed deployment of the runtime from the deploylist
		logrus.Debugf("[EDAS] deploy name: %+v", i.ObjectMeta.Name)
		if strings.Contains(i.ObjectMeta.Name, group) && *i.Spec.Replicas != 0 {
			var iService apistructs.Service
			for _, j := range i.Spec.Template.Spec.Containers[0].Env {
				if j.Name == diceServiceName {
					edasAhasName = j.Value
					cpu = i.Spec.Template.Spec.Containers[0].Resources.Limits.Cpu().MilliValue()
					mem, _ = i.Spec.Template.Spec.Containers[0].Resources.Limits.Memory().AsInt64()
					replicas = *i.Spec.Replicas
					image = i.Spec.Template.Spec.Containers[0].Image
				}
			}
			// Query the service interface to get the port list
			appName := group + "-" + edasAhasName
			if kubeSvc, err = e.getK8sService(appName); err != nil {
				if err.Error() == k8sServiceNotFound {
					port = 0
				} else {
					return nil, errors.Errorf("get k8s service err: %+v", err)
				}
			} else {
				port = kubeSvc.Spec.Ports[0].Port
			}
			iService.Name = edasAhasName
			iService.Ports = append(iService.Ports, diceyml.ServicePort{Port: int(port), Protocol: "TCP", L4Protocol: apiv1.ProtocolTCP})
			iService.Scale = int(replicas)
			iService.Resources.Cpu = float64(cpu)
			iService.Resources.Mem = float64(mem)
			iService.Image = image
			*services = append(*services, iService)
		}
	}

	logrus.Debugf("[EDAS] old service list : %+v", services)
	return services, nil
}

// Inspect Query runtime information
func (e *EDAS) Inspect(ctx context.Context, specObj interface{}) (interface{}, error) {

	runtime, ok := specObj.(apistructs.ServiceGroup)
	if !ok {
		return nil, errors.New("edas k8s inspect: invalid runtime spec")
	}

	// Metadata information is passed in from the upper layer, here you only need to get the state of the runtime and assemble it into the runtime to return
	status, err := e.Status(ctx, specObj)
	if err != nil {
		return nil, errors.Errorf("edas k8s inspect: %v", err)
	}

	logrus.Infof("[EDAS] Inspect runtime(%s) status: %+v", runtime.ID, status)

	group := runtime.Type + "-" + runtime.ID
	runtime.Status = status.Status
	runtime.LastMessage = status.LastMessage

	for i, svc := range runtime.Services {
		appName := group + "-" + svc.Name

		if len(svc.Ports) > 0 {
			kubeSvc, err := e.getK8sService(appName)
			if err != nil {
				logrus.Warnf("[EDAS] Failed to inspect runtime(%s), service name: %s, error: %v",
					group, appName, err)
			} else {
				svcRecord := kubeSvc.Metadata.Name + ".default.svc.cluster.local"
				runtime.Services[i].ProxyIp = svcRecord
				runtime.Services[i].ProxyPorts = diceyml.ComposeIntPortsFromServicePorts(svc.Ports)
				runtime.Services[i].Vip = svcRecord
			}
		}
	}

	return &runtime, nil
}

func (e *EDAS) runAppFlow(ctx context.Context, flows [][]*apistructs.Service, runtime *apistructs.ServiceGroup) error {

	group := runtime.Type + "-" + runtime.ID

	for i, batch := range flows {
		logrus.Infof("[EDAS] create runtime: %s run batch %d %+v", group, i+1, batch)

		for _, s := range batch {
			go func() {
				var err error
				var service *apistructs.Service

				service = s
				logrus.Infof("[EDAS] run app flow to create service %s", s.Name)
				if err = e.createService(ctx, runtime, service); err != nil {
					logrus.Errorf("[EDAS] failed to create service: %s, error: %v", group+"-"+s.Name, err)
				}
			}()
			time.Sleep(1 * time.Second)
		}

		if err := e.waitRuntimeRunningOnBatch(ctx, batch, group); err != nil {
			return errors.Wrap(err, "wait service flow on batch")
		}
	}
	logrus.Infof("[EDAS] run app flow %s finished", group)
	return nil
}

func (e *EDAS) createService(ctx context.Context, runtime *apistructs.ServiceGroup, s *apistructs.Service) error {
	var err error
	var appID string
	serviceSpec, err := e.fillServiceSpec(s, runtime, false)
	if err != nil {
		return errors.Wrap(err, "fill service spec")
	}

	// Create application
	if appID, err = e.insertApp(serviceSpec); err != nil {
		return errors.Wrap(err, "edas create app")
	}

	group := runtime.Type + "-" + runtime.ID
	appName := group + "-" + s.Name

	//create k8s service
	if err := e.createK8sService(appName, appID, diceyml.ComposeIntPortsFromServicePorts(s.Ports)); err != nil {
		logrus.Errorf("[EDAS] Failed to create k8s service, appName: %s, error: %v", appName, err)
		return errors.Wrap(err, "edas create k8s service")
	}

	return nil
}

func (e *EDAS) createK8sService(appName string, appID string, ports []int) error {
	k8sService := &apiv1.Service{
		TypeMeta: metav1.TypeMeta{
			APIVersion: "v1",
			Kind:       "Service",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:      appName,
			Namespace: defaultNamespace,
			Labels:    make(map[string]string),
		},
		Spec: apiv1.ServiceSpec{
			// TODO: type?
			//Type: ServiceTypeLoadBalancer,
			Selector: make(map[string]string),
		},
	}
	k8sService.Spec.Selector["edas.appid"] = appID
	servicenamePrefix := "http-"
	for i, port := range ports {
		k8sService.Spec.Ports = append(k8sService.Spec.Ports, apiv1.ServicePort{
			// TODO: name?
			Name:       strutil.Concat(servicenamePrefix, strconv.Itoa(i)),
			Port:       int32(port),
			TargetPort: intstr.FromInt(port),
		})
	}
	logrus.Errorf("[EDAS] Start to create k8s svc, appName: %s", appName)
	err := e.k8sSvcClient.Create(k8sService)
	return err
}

func (e *EDAS) updateService(ctx context.Context, runtime *apistructs.ServiceGroup, s *apistructs.Service) error {
	var appName, appID string
	var err error

	appName = runtime.Type + "-" + runtime.ID + "-" + s.Name

	// Check whether the service exists, if it does not exist, create a new one; otherwise, update it
	if appID, err = e.getAppID(appName); err != nil {
		if err.Error() == notFound {
			logrus.Warningf("[EDAS] app(%s) is not found via edas api, will create it ", appName)
			if err = e.createService(ctx, runtime, s); err != nil {
				logrus.Errorf("[EDAS] failed to create service(%s): %v", appName, err)
				return err
			}
		} else {
			logrus.Errorf("[EDAS] failed to query app(%s) by update service: %s", appName, err)
			return err
		}
	} else {
		//查询最新一次的发布单，如果存在运行中则终止
		orderList, _ := e.listRecentChangeOrderInfo(appID)
		if len(orderList.ChangeOrder) > 0 && orderList.ChangeOrder[0].Status == 1 {
			e.abortChangeOrder(orderList.ChangeOrder[0].ChangeOrderId)
		}

		svcSpec, err := e.fillServiceSpec(s, runtime, true)
		if err != nil {
			return errors.Wrap(err, "fill service spec")
		}

		_, err = e.k8sSvcClient.Get(defaultNamespace, appName)

		if err == k8serror.ErrNotFound {
			if err := e.createK8sService(appName, appID, diceyml.ComposeIntPortsFromServicePorts(s.Ports)); err != nil {
				logrus.Errorf("[EDAS] Failed to create k8s service, appName: %s, error: %v", appName, err)
				return errors.Wrap(err, "edas create k8s service")
			}
		} else if err != nil {
			logrus.Errorf("[EDAS] Failed to get k8s service, appName: %s, error: %v", appName, err)
			return errors.Wrap(err, "edas get k8s service")
		}

		if err = e.deployApp(appID, svcSpec); err != nil {
			logrus.Errorf("[EDAS] Failed to deploy app: %s, error: %v", appName, err)
			return err
		}
	}
	return nil
}

// TODO: how to handle the error
func (e *EDAS) removeService(ctx context.Context, group string, s *apistructs.Service) error {
	var err error

	appName := group + "-" + s.Name
	err = e.deleteAppByName(appName)
	if err != nil {
		logrus.Errorf("[EDAS] Failed to delete app(%s): %v", appName, err)
		return err
	}

	err = e.k8sSvcClient.Delete(defaultNamespace, appName)
	if err != nil {
		logrus.Errorf("[EDAS] Failed to delete k8s svc of app(%s): %v", appName, err)
		return err
	}
	// HACK: Regardless of whether calling edas api to delete the service is successful, try to delete the related service directly through k8s
	// if err = e.deleteDeploymentAndService(group, s); err != nil {
	// 	logrus.Warnf("[EDAS] Failed to delete k8s deployments and service, appName: %s, error: %v", appName, err)
	// }

	return nil
}

func (e *EDAS) cyclicUpdateService(ctx context.Context, newRuntime, oldRuntime *apistructs.ServiceGroup) error {
	var err error

	errChan := make(chan error, 1)
	group := newRuntime.Type + "-" + newRuntime.ID

	// Resolve dependencies
	flows, err := util.ParseServiceDependency(newRuntime)
	if err != nil {
		return errors.Wrapf(err, "parse service flow, runtime: %s", group)
	}
	go func() {
		logrus.Debugf("[EDAS] Start to cyclicUpdateService, group: %s", group)
		defer logrus.Debugf("[EDAS] End cyclicUpdateService, group: %s", group)

		// Detect services that need to be deleted in advance
		// 1. The service whose name has been deleted or updated
		// 2. The service whose port has been modified
		svcs := checkoutServicesToDelete(newRuntime, oldRuntime)
		for _, svc := range *svcs {
			appName := group + "-" + svc.Name
			logrus.Warningf("[EDAS] need to delete service(%s) because the user modified name or ports !!!", appName)

			err := e.removeService(ctx, group, &svc)
			if err != nil {
				logrus.Errorf("[EDAS] failed to remove service by cyclic update: %s, error: %v", appName, err)
			}
		}

		for _, batch := range flows {
			for _, newSvc := range batch {
				var ok bool
				var oldSvc *apistructs.Service

				svcName := newSvc.Name
				appName := group + "-" + svcName
				// add service
				if ok, oldSvc = isServiceInRuntime(svcName, oldRuntime); !ok || oldSvc == nil {
					logrus.Infof("[EDAS] cyclicupdate to create service %s", svcName)
					if err = e.createService(ctx, newRuntime, newSvc); err != nil {
						logrus.Errorf("[EDAS] Failed to create service: %s, error: %v", appName, err)
						errChan <- err
						return
					}
					continue
				}

				// update service
				// Does not include domain name updates
				if err = e.updateService(ctx, newRuntime, newSvc); err != nil {
					logrus.Errorf("[EDAS] Failed to update service: %s, error: %v", appName, err)
					errChan <- err
					return
				}
			}
		}
	}()

	// HACK: The edas api must time out (> 10s). The reason for waiting for 5s here is to facilitate the status update of the runtime.
	// Prevent the upper layer from querying the status when the asynchronous has not been executed.
	select {
	case err := <-errChan:
		close(errChan)
		return err
	case <-ctx.Done():
	case <-time.After(5 * time.Second):
	}

	return nil
}

func findService(svcName string, runtime *apistructs.ServiceGroup) (*apistructs.Service, error) {
	if len(svcName) == 0 || runtime == nil {
		return nil, errors.Errorf("find service: invalid params")
	}

	for _, svc := range runtime.Services {
		if svcName == svc.Name {
			return &svc, nil
		}
	}

	return nil, errors.Errorf(notFound)
}

func intSliceEqual(a, b []int) bool {
	if len(a) != len(b) {
		return false
	}

	if (a == nil) != (b == nil) {
		return false
	}

	for i, v := range a {
		if v != b[i] {
			return false
		}
	}

	return true
}

func stringMapEqual(a, b map[string]string) bool {
	if len(a) != len(b) {
		return false
	}

	for k, av := range a {
		if bv, ok := b[k]; !ok || bv != av {
			return false
		}
	}
	return true
}

func (e *EDAS) removeAndCreateRuntime(ctx context.Context, runtime *apistructs.ServiceGroup) (interface{}, error) {
	var err error
	var copyRun apistructs.ServiceGroup

	errChan := make(chan error, 1)
	group := runtime.Type + "-" + runtime.ID

	copyRun.Type = runtime.Type
	copyRun.ID = runtime.ID

	go func() {
		if err = e.Remove(ctx, copyRun); err != nil {
			logrus.Errorf("[EDAS] Failed to update runtime: %s, error: %v", group, err)
			errChan <- err
		}

		if _, err = e.Create(ctx, *runtime); err != nil {
			logrus.Errorf("[EDAS] Failed to update runtime: %s, error: %v", group, err)
			errChan <- err
			return
		}
	}()

	// HACK: The edas api must time out (> 10s). The reason for waiting for 5s here is to facilitate the status update of the runtime.
	// Prevent the upper layer from querying the status when the asynchronous has not been executed.
	select {
	case err := <-errChan:
		close(errChan)
		return nil, err
	case <-ctx.Done():
	case <-time.After(5 * time.Second):
	}

	return nil, nil
}

func (e *EDAS) deleteDeploymentAndService(group string, service *apistructs.Service) error {

	appName := group + "-" + service.Name

	// Only services with externally exposed ports have corresponding k8s service
	if len(service.Ports) > 0 {
		if err := e.deleteK8sService(group, service); err != nil {
			logrus.Warnf("[EDAS] Failed to delete k8s service, appName: %s, error: %v", appName, err)
		}
	}

	return e.deleteK8sDeployment(group, service)
}

func (e *EDAS) deleteK8sService(group string, s *apistructs.Service) error {
	var b bytes.Buffer
	var kubeSvc *k8sapi.Service
	var err error

	appName := group + "-" + s.Name

	if kubeSvc, err = e.getK8sService(appName); err != nil {
		return errors.Wrapf(err, "get k8s service: %s", appName)
	}

	svcName := kubeSvc.Metadata.Name

	resp, err := e.kubeClient.Delete(e.kubeAddr).
		Path("/api/v1/namespaces/" + defaultNamespace + "/services/" + svcName).
		JSONBody(deleteOptions).
		Do().
		Body(&b)

	if err != nil {
		return errors.Wrapf(err, "k8s delete service(%s) failed", svcName)
	}

	if resp == nil {
		return errors.Errorf("response is null")
	}

	if !resp.IsOK() {
		return errors.Errorf("k8s delete service(%s) status code: %v, resp body: %v", svcName, resp.StatusCode(), b.String())
	}

	return nil
}

func (e *EDAS) deleteK8sDeployment(group string, s *apistructs.Service) error {
	var b bytes.Buffer

	dep, err := e.getDeploymentInfo(group, s)
	if err != nil {
		return err
	}

	depName := dep.Metadata.Name

	resp, err := e.kubeClient.Delete(e.kubeAddr).
		Path("/apis/apps/v1beta1/namespaces/" + defaultNamespace + "/deployments/" + depName).
		JSONBody(deleteOptions).
		Do().
		Body(&b)

	if err != nil {
		return errors.Wrapf(err, "k8s delete deployment(%s) failed", depName)
	}

	if resp == nil {
		return errors.Errorf("response is null")
	}

	if !resp.IsOK() {
		return errors.Errorf("k8s delete deployment: %s, status code: %v, resp body: %v",
			depName, resp.StatusCode(), b.String())
	}

	return nil
}

func buildTLS(publicHosts []string) []k8sapi.IngressTLS {
	tls := make([]k8sapi.IngressTLS, 1)
	tls[0].Hosts = make([]string, len(publicHosts))
	for i, host := range publicHosts {
		tls[0].Hosts[i] = host
	}
	return tls
}

// Create Application
// InsertK8sApplication
// Question 1: The service name does not support "_"
func (e *EDAS) insertApp(spec *ServiceSpec) (string, error) {
	var req *api.InsertK8sApplicationRequest
	var resp *api.InsertK8sApplicationResponse

	logrus.Infof("[EDAS] Start to insert app: %s", spec.Name)

	req = api.CreateInsertK8sApplicationRequest()
	req.Headers["Pragma"] = "no-cache"
	req.Headers["Cache-Control"] = "no-cache"
	req.Headers["Connection"] = "keep-alive"
	req.SetDomain(e.addr)
	req.ClusterId = e.clusterID
	if len(e.logicalRegionID) != 0 {
		req.LogicalRegionId = e.logicalRegionID
	}

	req.AppName = spec.Name
	req.ImageUrl = spec.Image
	req.Command = spec.Cmd
	req.CommandArgs = spec.Args
	req.Envs = spec.Envs
	req.LocalVolume = spec.LocalVolume
	req.Liveness = spec.Liveness
	req.Readiness = spec.Readiness
	req.Replicas = requests.NewInteger(spec.Instances)
	if e.unlimitCPU == "true" {
		req.RequestsCpu = requests.NewInteger(spec.CPU)
		req.LimitCpu = requests.NewInteger(spec.CPU)
	} else {
		req.RequestsmCpu = requests.NewInteger(spec.Mcpu)
		req.LimitmCpu = requests.NewInteger(spec.Mcpu)
	}
	req.RequestsMem = requests.NewInteger(spec.Mem)
	req.LimitMem = requests.NewInteger(spec.Mem)

	logrus.Debugf("[EDAS] insert k8s application, request body: %+v", req)

	// InsertK8sApplication
	resp, err := e.client.InsertK8sApplication(req)
	if err != nil {
		return "", errors.Errorf("edas insert app, response http context: %s, error: %v", resp.GetHttpContentString(), err)
	}

	if resp == nil {
		return "", errors.Errorf("response is null")
	}

	logrus.Debugf("[EDAS] insertApp response, code: %d, message: %s, applicationInfo: %+v", resp.Code, resp.Message, resp.ApplicationInfo)

	if resp.Code != 200 {
		return "", errors.Errorf("failed to insert app, edasCode: %d, message: %s", resp.Code, resp.Message)
	}

	logrus.Debugf("[EDAS] start loop termination status: appName: %s", req.AppName)

	// check edas app status
	if len(resp.ApplicationInfo.ChangeOrderId) != 0 {
		status, err := e.loopTerminationStatus(resp.ApplicationInfo.ChangeOrderId)
		if err != nil {
			return "", errors.Wrapf(err, "get insert status by loop")
		}

		if status != CHANGE_ORDER_STATUS_SUCC {
			return "", errors.Errorf("failed to get the change order of inserting app, status: %s", ChangeOrderStatusString[status])
		}
	}

	logrus.Debugf("[EDAS] start loop check k8s service status: appName: %s", req.AppName)

	appID := resp.ApplicationInfo.AppId
	logrus.Infof("[EDAS] Successfully to insert app name: %s, appID: %s", spec.Name, appID)
	return appID, nil
}

// Terminate and roll back the change order
func (e *EDAS) abortAndRollbackChangeOrder(changeOrderID string) error {
	var req *api.AbortAndRollbackChangeOrderRequest
	//var resp *api.AbortAndRollbackChangeOrderResponse
	var err error
	req = api.CreateAbortAndRollbackChangeOrderRequest()
	req.Headers["Pragma"] = "no-cache"
	req.Headers["Cache-Control"] = "no-cache"
	req.Headers["Connection"] = "keep-alive"
	req.SetDomain(e.addr)
	req.ChangeOrderId = changeOrderID
	_, err = e.client.AbortAndRollbackChangeOrder(req)
	if err != nil {
		logrus.Errorf("[EDAS] failed to abort change order(%s), err: %v", changeOrderID, err)
	}
	return nil
}

//Termination of change order
func (e *EDAS) abortChangeOrder(changeOrderID string) error {
	var req *api.AbortChangeOrderRequest
	var err error
	req = api.CreateAbortChangeOrderRequest()
	req.Headers["Pragma"] = "no-cache"
	req.Headers["Cache-Control"] = "no-cache"
	req.Headers["Connection"] = "keep-alive"
	req.SetDomain(e.addr)
	req.ChangeOrderId = changeOrderID
	_, err = e.client.AbortChangeOrder(req)
	if err != nil {
		logrus.Errorf("[EDAS] failed to abort change order(%s), err: %v", changeOrderID, err)
	}
	return nil
}

// Delete Application
func (e *EDAS) deleteAppByName(appName string) error {

	logrus.Infof("[EDAS] Start to delete app: %s", appName)
	// get appId
	appID, err := e.getAppID(appName)
	if err != nil {
		if err.Error() == notFound {
			return nil
		}
		return err
	}

	orderList, err := e.listRecentChangeOrderInfo(appID)
	if err != nil {
		logrus.Errorf("failed to list recent change order info, app id: %s, err: %v", appID, err)
		return err
	}

	if len(orderList.ChangeOrder) > 0 && orderList.ChangeOrder[0].Status == 1 {
		e.abortChangeOrder(orderList.ChangeOrder[0].ChangeOrderId)
	}

	return e.deleteAppByID(appID)
}

// delete application by app id
func (e *EDAS) deleteAppByID(id string) error {
	var req *api.DeleteK8sApplicationRequest
	var resp *api.DeleteK8sApplicationResponse
	var err error

	logrus.Infof("[EDAS] Start to delete app by id: %s", id)

	req = api.CreateDeleteK8sApplicationRequest()
	req.Headers["Pragma"] = "no-cache"
	req.Headers["Cache-Control"] = "no-cache"
	req.Headers["Connection"] = "keep-alive"
	req.SetDomain(e.addr)
	req.AppId = id

	// DeleteApplicationRequest
	resp, err = e.client.DeleteK8sApplication(req)
	if err != nil {
		return errors.Errorf("response http context: %s, error: %v", resp.GetHttpContentString(), err)
	}

	if resp == nil {
		return errors.Errorf("response is null")
	}

	logrus.Debugf("[EDAS] delete app(%s) response, requestID: %s, code: %d, message: %s, changeOrderID: %s",
		id, resp.RequestId, resp.Code, resp.Message, resp.ChangeOrderId)

	if resp.Code != 200 {
		return errors.Errorf("failed to delete app(%s), edasCode: %d, message: %s", id, resp.Code, resp.Message)
	}

	if len(resp.ChangeOrderId) != 0 {
		status, err := e.loopTerminationStatus(resp.ChangeOrderId)
		if err != nil {
			return errors.Wrapf(err, "get delete status by loop")
		}

		if status != CHANGE_ORDER_STATUS_SUCC {
			return errors.Errorf("failed to get the status of deleting app(%s), status = %s", id, ChangeOrderStatusString[status])
		}
	}

	logrus.Infof("[EDAS] Successfully to delete app by id: %s", id)
	return nil
}

// get application ID
func (e *EDAS) getAppID(name string) (string, error) {
	var req *api.ListApplicationRequest
	var resp *api.ListApplicationResponse
	var err error

	req = api.CreateListApplicationRequest()
	req.SetDomain(e.addr)

	// get application list
	resp, err = e.client.ListApplication(req)
	if err != nil {
		return "", errors.Wrap(err, "edas list app")
	}

	if resp == nil {
		return "", errors.Errorf("response is null")
	}

	if resp.Code != 200 {
		return "", errors.Errorf("failed to list app, edasCode: %d, message: %s", resp.Code, resp.Message)
	}

	if len(resp.ApplicationList.Application) == 0 {
		errMsg := fmt.Sprintf("[EDAS] application list count is 0")
		logrus.Errorf(errMsg)
		return "", fmt.Errorf(errMsg)
	}
	for _, app := range resp.ApplicationList.Application {
		if name == app.Name {
			logrus.Infof("[EDAS] Successfully to get app id: %s, name: %s", app.AppId, name)
			return app.AppId, nil
		}
	}

	return "", errors.Errorf(notFound)
}

// Get the results of the task release list in a loop
func (e *EDAS) loopTerminationStatus(orderID string) (ChangeOrderStatus, error) {
	var status ChangeOrderStatus
	var err error

	retry := 2
	for i := 0; i < loopCount; i++ {
		time.Sleep(10 * time.Second)

		status, err = e.getChangeOrderInfo(orderID)
		if err != nil {
			return status, err
		}

		if status == CHANGE_ORDER_STATUS_PENDING || status == CHANGE_ORDER_STATUS_EXECUTING {
			continue
		}

		if status == CHANGE_ORDER_STATUS_SUCC || retry <= 0 {
			return status, nil
		}
		retry--
	}

	return status, errors.Errorf("get change order info timeout.")
}

// Get to check whether the k8s service is created successfully
func (e *EDAS) loopCheckK8sServiceIsCreated(spec *ServiceSpec) bool {
	var err error

	//If no port is configured, skip the k8s service check
	if len(spec.Ports) <= 0 {
		return true
	}

	// Cycle check k8s service
	for i := 0; i < 10; i++ {
		if _, err = e.getK8sService(spec.Name); err == nil {
			return true
		}
		logrus.Warningf("[EDAS] failed to get k8s service, name: %s, error: %v", spec.Name, err)
		time.Sleep(10 * time.Second)
	}

	return false
}

// Check details of changes
func (e *EDAS) getChangeOrderInfo(orderID string) (ChangeOrderStatus, error) {
	var req *api.GetChangeOrderInfoRequest
	var resp *api.GetChangeOrderInfoResponse
	var err error

	logrus.Debugf("[EDAS] Start to get change order info, orderID: %s", orderID)

	req = api.CreateGetChangeOrderInfoRequest()
	req.SetDomain(e.addr)
	req.ChangeOrderId = orderID

	if resp, err = e.client.GetChangeOrderInfo(req); err != nil {
		return CHANGE_ORDER_STATUS_ERROR, errors.Wrap(err, "edas get change order info")
	}

	if resp == nil {
		return CHANGE_ORDER_STATUS_ERROR, errors.Errorf("response is null")
	}

	if resp.Code != 200 {
		return CHANGE_ORDER_STATUS_ERROR, errors.Errorf("failed to get change order info, edasCode: %d, message: %s", resp.Code, resp.Message)
	}

	status := ChangeOrderStatus(resp.ChangeOrderInfo.Status)

	logrus.Infof("[EDAS] get change order info, orderID: %s, status: %v", orderID, status)

	return status, nil
}

// Query the list of release history
func (e *EDAS) listRecentChangeOrderInfo(appID string) (*api.ChangeOrderList, error) {
	var req *api.ListRecentChangeOrderRequest
	var resp *api.ListRecentChangeOrderResponse
	var err error

	req = api.CreateListRecentChangeOrderRequest()
	req.SetDomain(e.addr)
	req.AppId = appID

	if resp, err = e.client.ListRecentChangeOrder(req); err != nil {
		return nil, errors.Wrap(err, "edas list recent change order info")
	}

	if resp == nil {
		return nil, errors.Errorf("response is null")
	}

	if resp.Code != 200 {
		return nil, errors.Errorf("failed to list recent change order info, edasCode: %d, message: %s", resp.Code, resp.Message)
	}

	return &resp.ChangeOrderList, nil
}

func (e *EDAS) queryAppStatus(appName string) (AppStatus, error) {
	var req *api.QueryApplicationStatusRequest
	var resp *api.QueryApplicationStatusResponse
	var state = AppStatusRunning
	var err error

	appID, err := e.getAppID(appName)
	if err != nil {
		return state, err
	}

	req = api.CreateQueryApplicationStatusRequest()
	req.SetDomain(e.addr)
	req.AppId = appID

	resp, err = e.client.QueryApplicationStatus(req)
	if err != nil {
		return state, err
	}

	if resp == nil {
		return state, errors.Errorf("response is null")
	}

	logrus.Debugf("[EDAS] queryAppStatus response, appName: %s, code: %d, message: %s, app: %+v",
		appName, resp.Code, resp.Message, resp.AppInfo)

	if resp.Code != 200 {
		return state, errors.Errorf("failed to query edas app(%s) status, edasCode: %d, message: %s", appName, resp.Code, resp.Message)
	}

	var orderList *api.ChangeOrderList
	if orderList, err = e.listRecentChangeOrderInfo(appID); err != nil {
		return state, errors.Wrap(err, "list recent change order info")
	}

	lastOrderType := CHANGE_TYPE_CREATE
	if len(orderList.ChangeOrder) > 0 {
		sort.Sort(ByCreateTime(orderList.ChangeOrder))
		lastOrderType = ChangeType(orderList.ChangeOrder[len(orderList.ChangeOrder)-1].CoType)
	}

	if len(resp.AppInfo.EccList.Ecc) == 0 {
		state = AppStatusStopped
	} else {
		//There may be multiple instances, as long as one is not running, return
		for _, ecc := range resp.AppInfo.EccList.Ecc {
			appState := AppState(ecc.AppState)
			taskState := TaskState(ecc.TaskState)
			if appState == APP_STATE_AGENT_OFF || appState == APP_STATE_RUNNING_BUT_URL_FAILED {
				if taskState == TASK_STATE_PROCESSING {
					state = AppStatusDeploying
				} else {
					state = AppStatusFailed
				}
				break
			}
			if appState == APP_STATE_STOPPED {
				if taskState == TASK_STATE_PROCESSING {
					state = AppStatusDeploying
				} else if taskState == TASK_STATE_FAILED {
					state = AppStatusFailed
					break
				} else if taskState == TASK_STATE_UNKNOWN {
					state = AppStatusUnknown
					break
				} else if taskState == TASK_STATE_SUCCESS {
					if lastOrderType != CHANGE_TYPE_CREATE {
						state = AppStatusStopped
						break
					} else {
						state = AppStatusDeploying
					}
				}
			}
		}
	}

	logrus.Infof("[EDAS] Successfully to query app status: %v, app name: %s", state, appName)
	return state, nil
}

// container service, k8s service dns record: svc.Name + ".default.svc.cluster.local"
func (e *EDAS) getK8sService(name string) (*k8sapi.Service, error) {
	var err error

	slbPrefix := "intranet-" + name
	prefix := name
	if len(name) == 0 {
		return nil, errors.Errorf("get k8s service: invalid params")
	}

	svcList := &k8sapi.ServiceList{}
	// TODO: use selector
	resp, err := e.kubeClient.Get(e.kubeAddr).
		Path("/api/v1/namespaces/"+defaultNamespace+"/services").
		Header("Content-Type", "application/json").
		Do().
		JSON(svcList)
	if err != nil {
		return nil, errors.Wrapf(err, "get k8s service, prefix: %s", prefix)
	}

	if resp == nil {
		return nil, errors.Errorf("response is null")
	}

	if !resp.IsOK() {
		return nil, errors.Errorf("failed to get k8s service, prefix: %s, statusCode: %d",
			prefix, resp.StatusCode())
	}

	for _, svc := range svcList.Items {
		if svc.Spec.Type == k8sapi.ServiceTypeClusterIP &&
			// strings.Compare(svc.Spec.Selector["edas-domain"], "edas-admin") == 0 &&
			strings.HasPrefix(svc.Metadata.Name, prefix) {
			return &svc, nil
		}
	}

	for _, svc := range svcList.Items {
		if svc.Spec.Type == k8sapi.ServiceTypeLoadBalancer &&
			strings.Compare(svc.Spec.Selector["edas-domain"], "edas-admin") == 0 &&
			strings.HasPrefix(svc.Metadata.Name, slbPrefix) {
			return &svc, nil
		}
	}

	return nil, errors.Errorf("not found k8s service")
}

func (e *EDAS) getDeploymentStatus(group string, srv *apistructs.Service) (apistructs.StatusDesc, string, error) {
	var status apistructs.StatusDesc

	status.Status = apistructs.StatusUnknown

	dep, err := e.getDeploymentInfo(group, srv)
	if err != nil {
		return status, "", err
	}

	dps := dep.Status
	// this would not happen in theory
	if len(dps.Conditions) == 0 {
		status.Status = apistructs.StatusUnknown
		status.LastMessage = "could not get status condition"
		return status, "", nil
	}

	for _, c := range dps.Conditions {
		if c.Type == k8sapi.DeploymentReplicaFailure && c.Status == "True" {
			status.Status = apistructs.StatusFailing
			return status, "", nil
		}
		if c.Type == k8sapi.DeploymentAvailable && c.Status == "False" {
			status.Status = apistructs.StatusFailing
			return status, "", nil
		}
	}

	if dps.Replicas == dps.ReadyReplicas &&
		dps.Replicas == dps.AvailableReplicas &&
		dps.Replicas == dps.UpdatedReplicas {
		if dps.Replicas > 0 {
			status.Status = apistructs.StatusReady
			status.LastMessage = fmt.Sprintf("deployment(%s) is running", dep.Metadata.Name)
		} else {
			// This state is only present at the moment of deletion
			status.LastMessage = fmt.Sprintf("deployment(%s) replica is 0", dep.Metadata.Name)
		}
	}

	labels := dep.Metadata.Labels["edas.appid"] + "," + dep.Metadata.Labels["edas.groupid"]
	return status, labels, nil
}

func (e *EDAS) getDeploymentInfo(group string, srv *apistructs.Service) (*k8sapi.Deployment, error) {
	deps := &k8sapi.DeploymentList{}
	iDep := &k8sapi.Deployment{}

	prefix := group + "-" + srv.Name

	// TODO: use label selector
	resp, err := e.kubeClient.Get(e.kubeAddr).
		Path("/apis/apps/v1/namespaces/"+defaultNamespace+"/deployments").
		Header("Content-Type", "application/json").
		Do().
		JSON(&deps)
	if err != nil {
		return nil, err
	}

	if resp == nil {
		return nil, errors.Errorf("response is null")
	}

	if !resp.IsOK() {
		return nil, errors.Errorf("failed to get deployment info, namespace: %s, service name: %s, statusCode: %d",
			srv.Namespace, srv.Name, resp.StatusCode())
	}

	for _, dep := range deps.Items {
		i := dep
		if strings.Contains(dep.Metadata.Name, prefix) &&
			strings.Compare(dep.Metadata.Labels["edas-domain"], "edas-admin") == 0 {
			iDep = &i
			// return &dep, nil
		}
	}
	return iDep, nil
	// return nil, errors.Errorf("not found k8s deployment")
}

func (e *EDAS) generateServiceEnvs(s *apistructs.Service, runtime *apistructs.ServiceGroup) (map[string]string, error) {
	var envs map[string]string

	group := runtime.Type + "-" + runtime.ID

	appName := group + "-" + s.Name

	envs = s.Env
	if envs == nil {
		envs = make(map[string]string, 10)
	}

	addEnv := func(s *apistructs.Service, envs *map[string]string) error {
		appName := group + "-" + s.Name
		kubeSvc, err := e.getK8sService(appName)
		if err != nil {
			return err
		}

		svcRecord := kubeSvc.Metadata.Name + ".default.svc.cluster.local"
		// add {serviceName}_HOST
		key := makeEnvVariableName(s.Name) + "_HOST"
		(*envs)[key] = svcRecord

		// {serviceName}_PORT Point to the first port
		if len(s.Ports) > 0 {
			key := makeEnvVariableName(s.Name) + "_PORT"
			(*envs)[key] = strconv.Itoa(s.Ports[0].Port)
		}

		//If there are multiple ports, use them in sequence, like {serviceName}_PORT0, {serviceName}_PORT1,...
		for i, port := range s.Ports {
			key := makeEnvVariableName(s.Name) + "_PORT" + strconv.Itoa(i)
			(*envs)[key] = strconv.Itoa(port.Port)
		}

		return nil
	}

	// TODO: All containers have all service environment variables for addon deployment
	// Since edas needs to create the app before it can determine the service name, it is impossible to pre-fill the full amount of environment variables
	// edas does not deploy addons and does not support GLOBAL; however, it is necessary to deploy low-profile addons for testing, so no error is returned.
	if runtime.ServiceDiscoveryMode == "GLOBAL" {
		//return nil, errors.Errorf("not support ServiceDiscoveryMode: %v", runtime.ServiceDiscoveryMode)
	} else {
		for _, name := range s.Depends {
			var depSvc *apistructs.Service
			for _, svc := range runtime.Services {
				if svc.Name == name {
					depSvc = &svc
					break
				}
			}
			// not found
			if depSvc == nil {
				return nil, errors.Errorf("not find service: %s", name)
			}

			if len(depSvc.Ports) == 0 {
				continue
			}

			//Inject {depSvc}_HOST, {depSvc}_PORT, etc.
			if err := addEnv(depSvc, &envs); err != nil {
				return nil, err
			}

			depAppName := group + "-" + depSvc.Name
			if s.Labels["IS_ENDPOINT"] == "true" && len(depSvc.Ports) > 0 {
				kubeSvc, err := e.getK8sService(depAppName)
				if err != nil {
					return nil, err
				}
				svcRecord := kubeSvc.Metadata.Name + ".default.svc.cluster.local"
				port := depSvc.Ports[0]
				envs["BACKEND_URL"] = svcRecord + ":" + strconv.Itoa(port.Port)
			}
		}
	}

	svcAddr := appName + ".default.svc.cluster.local"
	// add K8S label
	envs["IS_K8S"] = "true"
	// add svc label
	envs["SELF_HOST"] = svcAddr
	if len(s.Ports) != 0 {
		envs["SELF_PORT"] = strconv.Itoa(s.Ports[0].Port)
		envs["SELF_URL"] = "http://" + svcAddr + ":" + strconv.Itoa(s.Ports[0].Port)
		envs["SELF_PORT0"] = strconv.Itoa(s.Ports[0].Port)
	}

	// TODO: add self env
	//Problem: After the service is created, there will be k8s service, which makes it impossible to insert SELF_HOST env in advance

	return envs, nil
}

func (e *EDAS) fillServiceSpec(s *apistructs.Service, runtime *apistructs.ServiceGroup, isUpdate bool) (*ServiceSpec, error) {
	var envs map[string]string
	var err error

	group := runtime.Type + "-" + runtime.ID
	appName := group + "-" + s.Name

	logrus.Debugf("[EDAS] Start to fill service spec: %s", appName)

	svcSpec := &ServiceSpec{
		Name:      appName,
		Instances: s.Scale,
		Mem:       int(s.Resources.Mem),
		Ports:     diceyml.ComposeIntPortsFromServicePorts(s.Ports),
	}

	// FIXME: hacking for registry
	// e.g. registry.marathon.l4lb.thisdcos.directory:5000  docker-registry.registry.marathon.mesos:5000
	body := strings.Split(s.Image, ":5000")
	if len(body) > 1 && e.regAddr != "" {
		svcSpec.Image = e.regAddr + body[1]
	} else {
		svcSpec.Image = s.Image
	}

	//For clusters with unlimitCPU turned on, the processing of less than 1 core is unlimited, and the default limit is 1C
	if e.unlimitCPU == "true" {
		svcSpec.CPU = 0
	} else {
		svcSpec.CPU = 1
	}

	//edas mount nfs volume
	if len(s.Binds) != 0 {
		type localVolumes struct {
			Type      string `json:"type"`
			NodePath  string `json:"nodePath"`
			MountPath string `json:"mountPath"`
		}
		var lv []localVolumes
		var lvBody []byte
		for _, bind := range s.Binds {
			if bind.HostPath == "" || bind.ContainerPath == "" || !strutil.HasPrefixes(bind.HostPath, "/") {
				continue
			}
			lv = append(lv, localVolumes{
				Type:      "DirectoryOrCreate",
				NodePath:  bind.HostPath,
				MountPath: bind.ContainerPath,
			})
		}
		lvBody, err = json.Marshal(lv)
		if err != nil {
			return nil, errors.Wrapf(err, "json marshal service localvolume")
		}
		logrus.Debugf("[EDAS] fill service spec localvolume args: %s", string(lvBody))
		svcSpec.LocalVolume = string(lvBody)
	}
	if s.Resources.Cpu >= 1.0 {
		cpu := math.Floor(s.Resources.Cpu + 0.5)
		svcSpec.CPU = int(cpu)
	}
	svcSpec.Mcpu = int(s.Resources.Cpu * 1000)
	// command: sh
	// args: [{"argument":"-c"},{"argument":"test"}]
	if len(s.Cmd) != 0 {
		svcSpec.Cmd = "sh"
		// inputArgs := strings.Split("-c "+s.Cmd, " ")
		inputArgs := []string{"-c", s.Cmd}

		type cArg struct {
			Argument string `json:"argument"`
		}
		var cArgs []cArg
		var argBody []byte
		for _, inputArg := range inputArgs {
			cArgs = append(cArgs, cArg{Argument: inputArg})
		}
		if isUpdate {
			argBody, err = json.Marshal(inputArgs)
		} else {
			argBody, err = json.Marshal(cArgs)
		}
		if err != nil {
			return nil, errors.Wrapf(err, "json marshal service cmd")
		}

		logrus.Debugf("[EDAS] fill service spec command args: %s", string(argBody))
		svcSpec.Args = string(argBody)
	}

	if envs, err = e.generateServiceEnvs(s, runtime); err != nil {
		logrus.Errorf("[EDAS] Failed to generate service envs: %s, error: %s", appName, err)
		return nil, err
	}

	// envs: [{"name":"testkey","value":"testValue"}]
	if len(envs) != 0 {
		envString, err := envToString(envs)
		if err != nil {
			return nil, err
		}
		svcSpec.Envs = envString
	}

	if err = setHealthCheck(s, svcSpec); err != nil {
		return nil, errors.Wrapf(err, "failed to set health check, service name: %s", appName)
	}

	// TODO: support postStart, preStop, nasId, mountDescs, storageType, localvolume

	logrus.Infof("[EDAS] fill service spec: %+v", svcSpec)

	return svcSpec, nil
}

func setHealthCheck(service *apistructs.Service, svcSpec *ServiceSpec) error {
	var (
		b   []byte
		err error
	)

	probe := k8s.FillHealthCheckProbe(service)
	if probe != nil {
		if b, err = json.Marshal(probe); err != nil {
			return err
		}

		svcSpec.Liveness = string(b)
		svcSpec.Readiness = string(b)
	}

	return nil
}

// FIXME: Is 20 times reasonable?
func (e *EDAS) waitRuntimeRunningOnBatch(ctx context.Context, batch []*apistructs.Service, group string) error {
	var err error
	var status AppStatus

	for i := 0; i < 60; i++ {
		done := map[string]struct{}{}

		time.Sleep(10 * time.Second)
		for _, srv := range batch {
			if _, ok := done[srv.Name]; ok {
				continue
			}
			appName := group + "-" + srv.Name
			// 1. Confirm app status from edas
			if status, err = e.queryAppStatus(appName); err != nil {
				logrus.Errorf("[EDAS] failed to query app(name: %s) status: %v", appName, err)
				continue
			}
			if status != AppStatusFailed {
				// 2. After app status is equal to running, confirm whether the k8s service is ready
				if len(srv.Ports) == 0 {
					done[appName] = struct{}{}
					continue
				}

				if _, err = e.getK8sService(appName); err != nil {
					logrus.Errorf("failed to get k8s service, appName: %s, error: %v", appName, err)
					continue
				}

				done[appName] = struct{}{}
			}
		}

		if len(done) == len(batch) {
			logrus.Infof("[EDAS] Successfully to wait runtime running on batch")
			return nil
		}
	}

	return errors.Errorf("failed to wait runtime(%s) status to running on batch.", group)
}

func makeEnvVariableName(str string) string {
	return strings.ToUpper(strings.Replace(str, "-", "_", -1))
}

func envToString(envs map[string]string) (string, error) {
	type appEnv struct {
		Name  string `json:"name"`
		Value string `json:"value"`
	}
	var appEnvs []appEnv
	var keys []string

	for env := range envs {
		keys = append(keys, env)
	}
	sort.Strings(keys)

	for _, k := range keys {
		appEnvs = append(appEnvs, appEnv{
			Name:  k,
			Value: envs[k],
		})
	}

	res, err := json.Marshal(appEnvs)
	if err != nil {
		return "", errors.Wrapf(err, "failed to json marshal map env")
	}

	return string(res), nil
}

// Deploy the application
// Role: The role of this interface is replace, temporarily only supports image tag update
func (e *EDAS) deployApp(appID string, spec *ServiceSpec) error {
	var req *api.DeployK8sApplicationRequest
	var resp *api.DeployK8sApplicationResponse
	var err error

	if spec == nil {
		return errors.Errorf("invalid params: service spec is null")
	}

	logrus.Infof("[EDAS] Start to deploy app, id: %s", appID)

	req = api.CreateDeployK8sApplicationRequest()
	req.Headers["Pragma"] = "no-cache"
	req.Headers["Cache-Control"] = "no-cache"
	req.Headers["Connection"] = "keep-alive"
	req.SetDomain(e.addr)

	// Compatible with edas proprietary cloud version 3.7.1
	res := strings.SplitAfter(spec.Image, ":")
	splitLen := len(res)
	if splitLen > 0 {
		req.ImageTag = res[splitLen-1]
	}

	req.AppId = appID
	req.Image = spec.Image
	req.Replicas = requests.NewInteger(spec.Instances)
	req.Command = spec.Cmd
	req.Args = spec.Args
	req.Envs = spec.Envs
	req.LocalVolume = spec.LocalVolume
	req.Liveness = spec.Liveness
	req.Readiness = spec.Readiness
	req.Replicas = requests.NewInteger(spec.Instances)
	if e.unlimitCPU == "true" {
		req.CpuRequest = requests.NewInteger(spec.CPU)
		req.CpuLimit = requests.NewInteger(spec.CPU)
	} else {
		req.McpuRequest = requests.NewInteger(spec.Mcpu)
		req.McpuLimit = requests.NewInteger(spec.Mcpu)
	}
	req.MemoryRequest = requests.NewInteger(spec.Mem)
	req.MemoryLimit = requests.NewInteger(spec.Mem)

	// HACK: edas don't support k8s container probe
	// This value is equivalent to k8s min-ready-seconds, for coarse-grained control
	// https://kubernetes.io/docs/concepts/workloads/controllers/deployment/?spm=a2c4g.11186623.2.3.7N5Zxk#min-ready-seconds
	req.BatchWaitTime = requests.NewInteger(minReadySeconds)

	logrus.Infof("[EDAS] deploy k8s application, request body: %+v", req)

	resp, err = e.client.DeployK8sApplication(req)
	if err != nil {
		return errors.Errorf("response http context: %s, error: %v", resp.GetHttpContentString(), err)
	}

	if resp == nil {
		return errors.Errorf("response is null")
	}

	logrus.Debugf("[EDAS] deployApp response, requestID: %s, code: %d, message: %s, ChangeOrderId: %+v",
		resp.RequestId, resp.Code, resp.Message, resp.ChangeOrderId)

	if resp.Code != 200 {
		return errors.Errorf("failed to deploy app, edasCode: %d, message: %s", resp.Code, resp.Message)
	}

	if len(resp.ChangeOrderId) != 0 {
		status, err := e.loopTerminationStatus(resp.ChangeOrderId)
		if err != nil {
			return errors.Wrapf(err, "failed to get the status of deploying app")
		}

		if status != CHANGE_ORDER_STATUS_SUCC {
			return errors.Errorf("failed to get the status of deploying app, status: %s", ChangeOrderStatusString[status])
		}
	}

	logrus.Debugf("[EDAS] Successfully to deploy app, id: %s", appID)

	return nil
}

// Instance scaling
// ScaleK8sApplicationRequest
func (e *EDAS) scaleApp(name string, scale int) error {
	var req *api.ScaleK8sApplicationRequest
	var resp *api.ScaleK8sApplicationResponse

	if len(name) == 0 {
		return errors.Errorf("edas scale app: name is null")
	}

	if scale < 0 {
		return errors.Errorf("edas scale app: size < 0 ")
	}

	appID, err := e.getAppID(name)
	if err != nil {
		return err
	}

	req = api.CreateScaleK8sApplicationRequest()
	req.Headers["Pragma"] = "no-cache"
	req.Headers["Cache-Control"] = "no-cache"
	req.Headers["Connection"] = "keep-alive"
	req.SetDomain(e.addr)

	req.AppId = appID
	req.Replicas = requests.NewInteger(scale)

	resp, err = e.client.ScaleK8sApplication(req)
	if err != nil {
		return errors.Errorf("response http context: %s, error: %v", resp.GetHttpContentString(), err)
	}

	if resp == nil {
		return errors.Errorf("response is null")
	}

	if resp.Code != 200 {
		return errors.Errorf("failed to scale app, edasCode: %d, message: %s", resp.Code, resp.Message)
	}

	// TODO: Wait for edas to fix the problem that ChangeOrderId returns struct{}
	//if len(resp.ChangeOrderId) != 0 {
	//	status, err := e.loopTerminationStatus(resp.ChangeOrderId)
	//	if err != nil {
	//		return errors.Wrapf(err, "failed to get the status of scaling app")
	//	}
	//
	//	if status != CHANGE_ORDER_STATUS_SUCC {
	//		return errors.Errorf("failed to get the status of scaling app, status: %s", ChangeOrderStatusString[status])
	//	}
	//}

	return nil
}

// Update service resoures, only support mem configuration for the time being
// cpu uses shared mode
// FIXME: This interface only supports the configuration of cpu & mem limit value, and the request value will be set to 0 after the change
func (e *EDAS) updateAppResources(name string, mem int) error {
	var req *api.UpdateK8sApplicationConfigRequest
	var resp *api.UpdateK8sApplicationConfigResponse
	var err error

	if len(name) == 0 || mem <= 0 {
		return errors.Errorf("edas update resources: invalid param")
	}

	appID, err := e.getAppID(name)
	if err != nil {
		return err
	}

	req = api.CreateUpdateK8sApplicationConfigRequest()
	req.Headers["Pragma"] = "no-cache"
	req.Headers["Cache-Control"] = "no-cache"
	req.Headers["Connection"] = "keep-alive"
	req.SetDomain(e.addr)

	req.AppId = appID
	req.ClusterId = e.clusterID
	// cpu share
	req.CpuLimit = strconv.Itoa(0)
	req.MemoryLimit = strconv.Itoa(mem)

	resp, err = e.client.UpdateK8sApplicationConfig(req)
	if err != nil {
		return err
	}

	if resp == nil {
		return errors.Errorf("response is null")
	}

	logrus.Debugf("[EDAS] updateAppResources response, code: %d, message: %s, ChangeOrderId: %+v", resp.Code, resp.Message, resp.ChangeOrderId)

	if resp.Code != 200 {
		return errors.Errorf("failed to scale app, edasCode: %d, message: %s", resp.Code, resp.Message)
	}

	if len(resp.ChangeOrderId) != 0 {
		status, err := e.loopTerminationStatus(resp.ChangeOrderId)
		if err != nil {
			return errors.Wrapf(err, "failed to get the status of scaling app")
		}

		if status != CHANGE_ORDER_STATUS_SUCC {
			return errors.Errorf("failed to get the status of scaling app, status: %s", ChangeOrderStatusString[status])
		}
	}

	return nil
}

// Apply to edas label, edas.appid and edas.groupid
func (e *EDAS) getPodsStatus(namespace string, label string) ([]k8sapi.PodItem, error) {
	var pi []k8sapi.PodItem
	var b bytes.Buffer

	resp, err := e.kubeClient.Get(e.kubeAddr).
		Path("/api/v1/namespaces/"+namespace+"/pods").
		Param("labelSelector", "app="+label).
		JSONBody(&deleteOptions).
		Do().
		Body(&b)

	if err != nil {
		return pi, errors.Wrapf(err, "k8s get podStatuses by label(%s) err: %v", err, label)
	}

	if !resp.IsOK() {
		return pi, errors.Errorf("k8s get podStatuses by label(%s) status code: %d, resp body: %v", label, resp.StatusCode(), b.String())
	}

	var pl k8sapi.PodList
	if err := json.Unmarshal(b.Bytes(), &pl); err != nil {
		return pi, err
	}
	for _, item := range pl.Items {
		pi = append(pi, k8sapi.PodItem{
			Metadata: item.Metadata,
			Status:   item.Status,
		})
	}
	return pi, nil
}

// Confirm whether to delete the service list
// The conditions are as follows:
// 1.Deleted service
// 2.The service whose name has been changed
// 3.The service of the modified port
func checkoutServicesToDelete(newRuntime, oldRuntime *apistructs.ServiceGroup) *[]apistructs.Service {
	var svcs []apistructs.Service

	if newRuntime == nil || oldRuntime == nil {
		return nil
	}

	for _, oldSvc := range oldRuntime.Services {
		ok, newSvc := isServiceInRuntime(oldSvc.Name, newRuntime)
		if !ok || (oldSvc.Ports[0].Port != newSvc.Ports[0].Port) {
			svcs = append(svcs, oldSvc)
		}
	}

	return &svcs
}

func isServiceInRuntime(name string, run *apistructs.ServiceGroup) (bool, *apistructs.Service) {
	if name == "" || run == nil {
		logrus.Warningf("[EDAS] hasServiceInRuntime invalid params, name or runtime is null")
		return false, nil
	}

	for _, svc := range run.Services {
		if svc.Name == name {
			return true, &svc
		}
	}

	return false, nil
}

func (e *EDAS) Cancel(ctx context.Context, specObj interface{}) (interface{}, error) {
	return nil, nil
}
func (e *EDAS) Precheck(ctx context.Context, specObj interface{}) (apistructs.ServiceGroupPrecheckData, error) {
	return apistructs.ServiceGroupPrecheckData{Status: "ok"}, nil
}
func (e *EDAS) SetNodeLabels(setting executortypes.NodeLabelSetting, hosts []string, labels map[string]string) error {
	return errors.New("SetNodeLabels not implemented in EDAS")
}

func (e *EDAS) CapacityInfo() apistructs.CapacityInfoData {
	return apistructs.CapacityInfoData{}
}

func (e *EDAS) ResourceInfo(brief bool) (apistructs.ClusterResourceInfoData, error) {
	return apistructs.ClusterResourceInfoData{
		CPUOverCommit:        1.0,
		ProdCPUOverCommit:    1.0,
		DevCPUOverCommit:     1.0,
		TestCPUOverCommit:    1.0,
		StagingCPUOverCommit: 1.0,
		ProdMEMOverCommit:    1.0,
		DevMEMOverCommit:     1.0,
		TestMEMOverCommit:    1.0,
		StagingMEMOverCommit: 1.0,
		Nodes: map[string]*apistructs.NodeResourceInfo{
			"1.1.1.1": {
				Labels:         []string{},
				IgnoreLabels:   true,
				Ready:          true,
				CPUAllocatable: 999999999999999.0,
				MemAllocatable: 999999999999999,
			},
		},
	}, nil
}

func (*EDAS) CleanUpBeforeDelete() {}
func (*EDAS) JobVolumeCreate(ctx context.Context, spec interface{}) (string, error) {
	return "", fmt.Errorf("not support for edas")
}
func (*EDAS) KillPod(podname string) error {
	return fmt.Errorf("not support for edas")
}

func (e *EDAS) Scale(ctx context.Context, specObj interface{}) (interface{}, error) {
	sg, ok := specObj.(apistructs.ServiceGroup)
	if !ok {
		errMsg := fmt.Sprintf("edas k8s scale: invalid service group spec")
		logrus.Errorf(errMsg)
		return nil, errors.New(errMsg)
	}

	// only support scale one service resources
	if len(sg.Services) != 1 {
		errMsg := fmt.Sprintf("the scaling service count is not equal 1")
		logrus.Errorf(errMsg)
		return nil, fmt.Errorf(errMsg)
	}

	if err := checkRuntime(&sg); err != nil {
		errMsg := fmt.Sprintf("check the runtime struct err: %v", err)
		logrus.Errorf(errMsg)
		return nil, fmt.Errorf(errMsg)
	}

	services := []apistructs.Service{}
	destService := sg.Services[0]
	originService := &apistructs.Service{}

	appName := sg.Type + "-" + sg.ID + "-" + destService.Name
	var (
		appID string
		err   error
	)
	if appID, err = e.getAppID(appName); err != nil {
		errMsg := fmt.Sprintf("get appID err in scale: %v", err)
		logrus.Errorf(errMsg)
		return nil, fmt.Errorf(errMsg)
	}

	if e.unlimitCPU == "true" {
		if destService.Resources.Cpu < 1 {
			destService.Resources.Cpu = 0
		} else {
			destService.Resources.Cpu = math.Floor(destService.Resources.Cpu + 0.5)
		}
	}

	logrus.Infof("[EDAS] start to get k8s deployment %s", strutil.Concat(sg.Type, "-", sg.ID))
	if _, err = e.getK8sDeployList(sg.Type, sg.ID, &services); err != nil {
		logrus.Debugf("[EDAS] Get deploy from k8s error: %+v", err)
		return nil, err
	}

	for _, service := range services {
		if service.Name == destService.Name {
			service.Resources.Cpu = service.Resources.Cpu / 1000
			service.Resources.Mem = service.Resources.Mem / 1024 / 1024
			originService = &service
			break
		}
	}

	var errString = make(chan string, 0)
	go func() {
		if originService != nil {
			// Query the latest release order, and terminate if it is running
			orderList, _ := e.listRecentChangeOrderInfo(appID)
			if len(orderList.ChangeOrder) > 0 && orderList.ChangeOrder[0].Status == 1 {
				err = e.abortChangeOrder(orderList.ChangeOrder[0].ChangeOrderId)
				if err != nil {
					errMsg := fmt.Sprintf("scale k8s application err: %v", err)
					logrus.Errorf(errMsg)
					errString <- errMsg
				}
			}

			logrus.Infof("[EDAS] diff cpu %v, origin is %v, dest is %v", originService.Resources.Cpu == destService.Resources.Cpu, originService.Resources.Cpu, destService.Resources.Cpu)
			logrus.Infof("[EDAS] diff memory %v, origin is %v, dest is %v", originService.Resources.Mem == destService.Resources.Mem, originService.Resources.Mem, destService.Resources.Mem)
			logrus.Infof("[EDAS] diff scale %v, origin is %v, dest is %v", originService.Scale == destService.Scale, originService.Scale, destService.Scale)
			if originService.Resources.Cpu == destService.Resources.Cpu &&
				originService.Resources.Mem == destService.Resources.Mem &&
				originService.Scale != destService.Scale {
				request := e.composeApplicationScale(destService.Scale)
				request.AppId = appID
				resp, err := e.client.ScaleK8sApplication(request)
				if err != nil {
					errMsg := fmt.Sprintf("scale k8s application err: %v", err)
					logrus.Errorf(errMsg)
					errString <- errMsg
				}
				if resp.Code != 200 {
					errMsg := fmt.Sprintf("scale k8s application resp err: %v", resp.Message)
					logrus.Errorf(errMsg)
					errString <- errMsg
				}
			} else {
				spec, err := e.fillServiceSpec(&destService, &sg, false)
				if err != nil {
					errMsg := fmt.Sprintf("compose service Spec application err: %v", err)
					logrus.Errorf(errMsg)
					errString <- errMsg
				}
				err = e.deployApp(appID, spec)
				if err != nil {
					errMsg := fmt.Sprintf("compose service Spec application err: %v", err)
					logrus.Errorf(errMsg)
					errString <- errMsg
				}
			}
			errString <- ""
		}
		errString <- fmt.Sprintf("not found service %s", destService.Name)
	}()
	select {
	case str := <-errString:
		if str != "" {
			return nil, fmt.Errorf(str)
		}
	case <-time.After(5 * time.Second):
	}
	return sg, nil
}

func (e *EDAS) composeApplicationScale(replica int) *api.ScaleK8sApplicationRequest {
	scaleRequest := api.CreateScaleK8sApplicationRequest()
	scaleRequest.Replicas = requests.NewInteger(replica)
	scaleRequest.SetDomain(e.addr)
	scaleRequest.Headers["Content-Type"] = "application/json"
	scaleRequest.RegionId = e.regionID
	return scaleRequest
}

func (e *EDAS) composeGetApplicationRequest(appID string) *api.GetK8sApplicationRequest {
	request := api.CreateGetK8sApplicationRequest()
	request.AppId = appID
	request.SetDomain(e.addr)
	request.Headers["Content-Type"] = "application/json"
	request.RegionId = e.regionID
	return request
}

func (e *EDAS) composeServiceSpecFromApplication(application api.Applcation) (*ServiceSpec, error) {
	edasEnvs := make([]EdasEnv, 0, len(application.App.EnvList.Env))
	for _, env := range application.App.EnvList.Env {
		edasEnvs = append(edasEnvs, EdasEnv{
			Name:  env.Name,
			Value: env.Value,
		})
	}
	envs, err := json.Marshal(edasEnvs)
	if err != nil {
		return nil, err
	}
	spec := ServiceSpec{}
	spec.Name = application.Name
	spec.Args = application.Conf.K8sCmdArgs
	spec.Cmd = application.Conf.K8sCmd
	spec.LocalVolume = application.Conf.K8sLocalvolumeInfo
	spec.Liveness = application.Conf.Liveness
	spec.Readiness = application.Conf.Readiness
	spec.Envs = string(envs)
	spec.Image = application.ImageInfo.ImageUrl

	return &spec, nil
}
