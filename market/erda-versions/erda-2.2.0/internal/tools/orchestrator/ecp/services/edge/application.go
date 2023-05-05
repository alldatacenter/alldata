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

package edge

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"sort"
	"strconv"
	"strings"

	"github.com/sirupsen/logrus"
	v1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/ecp/dbclient"
	"github.com/erda-project/erda/pkg/clientgo/apis/openyurt/v1alpha1"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

// ListApp Get all edge application list.
func (e *Edge) ListApp(param *apistructs.EdgeAppListPageRequest) (int, *[]apistructs.EdgeAppInfo, error) {
	var (
		total int
		apps  *[]dbclient.EdgeApp
		err   error
	)

	// Paging by clusterID
	if param.ClusterID > 0 {
		apps, err = e.db.ListAllEdgeAppByClusterID(param.OrgID, param.ClusterID)
		if err != nil {
			return 0, nil, err
		}
		total = len(*apps)
	} else {
		total, apps, err = e.db.ListEdgeApp(param)
		if err != nil {
			return 0, nil, err
		}

	}

	appInfos := make([]apistructs.EdgeAppInfo, 0, len(*apps))

	for i := range *apps {
		item, err := e.ConvertToEdgeAppInfo(&(*apps)[i])
		if err != nil {
			return 0, nil, err
		}
		appInfos = append(appInfos, *item)
	}

	return total, &appInfos, nil
}

// GetApp get edge application information
func (e *Edge) GetApp(edgeAppID int64) (*apistructs.EdgeAppInfo, error) {
	var appInfo *apistructs.EdgeAppInfo
	app, err := e.db.GetEdgeApp(edgeAppID)
	if err != nil {
		return nil, err
	}

	if appInfo, err = e.ConvertToEdgeAppInfo(app); err != nil {
		return nil, err
	}

	return appInfo, nil
}

// CreateApp Create edge application
func (e *Edge) CreateApp(ctx context.Context, req *apistructs.EdgeAppCreateRequest) error {
	var ud *v1alpha1.UnitedDeployment
	var err error
	var clusterInfo *clusterpb.ClusterInfo
	var svc *v1.Service
	var appExtraData map[string]string
	var envs []v1.EnvVar
	var probe *v1.Probe

	if req.HealthCheckType != "" {
		probe = e.k8s.GenerateHealthCheckSpec(&apistructs.GenerateHeathCheckRequest{
			HealthCheckType:     req.HealthCheckType,
			HealthCheckHttpPort: req.HealthCheckHttpPort,
			HealthCheckHttpPath: req.HealthCheckHttpPath,
			HealthCheckExec:     req.HealthCheckExec,
		})
	}

	namespace := fmt.Sprintf("%s-%s", EdgeAppPrefix, req.Name)

	// 1core=1000m
	requestCpu := fmt.Sprintf("%.fm", req.RequestCpu*1000)
	// 1Mi=1024K=1024x1024B
	requestMemory := fmt.Sprintf("%.fMi", req.RequestMem)
	limitCpu := fmt.Sprintf("%.fm", req.LimitCpu*1000)
	limitMemory := fmt.Sprintf("%.fMi", req.LimitMem)

	configSetName, err := e.getConfigSetName(req.OrgID, req.ConfigSetName)
	if err != nil {
		return fmt.Errorf("get configset %s namespaces error: %v", req.ConfigSetName, err)
	}

	unitedDeploymentRequest := &apistructs.GenerateUnitedDeploymentRequest{
		Name:       req.Name,
		Namespace:  namespace,
		RequestCPU: requestCpu,
		LimitCPU:   limitCpu,
		RequestMem: requestMemory,
		LimitMem:   limitMemory,
		Image:      req.Image,
		Type:       DeploymentType,
		ConfigSet:  configSetName,
		EdgeSites:  req.EdgeSites,
		Replicas:   req.Replicas,
	}
	if clusterInfo, err = e.getClusterInfo(ctx, req.ClusterID); err != nil {
		return err
	}
	//Handling environment variable injection for dependent applications
	var tmpExtraData map[string]string
	var app *dbclient.EdgeApp
	sort.Strings(req.DependApp)
	for i := range req.DependApp {
		var keys []string
		if app, err = e.db.GetEdgeAppByName(req.DependApp[i], req.OrgID); err != nil {
			return err
		}
		if err := json.Unmarshal([]byte(app.ExtraData), &tmpExtraData); err != nil {
			return err
		}
		for k := range tmpExtraData {
			keys = append(keys, k)
		}
		sort.Strings(keys)
		for _, k := range keys {
			envs = append(envs, v1.EnvVar{
				Name:  k,
				Value: tmpExtraData[k],
			})
		}
	}

	envs = append(envs, v1.EnvVar{
		Name:  "DICE_ORG_ID",
		Value: strconv.FormatInt(req.OrgID, 10),
	})
	envs = append(envs, v1.EnvVar{
		Name:  "DICE_EDGE_APPLICATION_NAME",
		Value: req.Name,
	})
	envs = append(envs, v1.EnvVar{
		Name:  "DICE_CLUSTER_NAME",
		Value: clusterInfo.Name,
	})
	appAffinity := v1.Affinity{
		PodAntiAffinity: &v1.PodAntiAffinity{
			PreferredDuringSchedulingIgnoredDuringExecution: []v1.WeightedPodAffinityTerm{
				{
					Weight: 100,
					PodAffinityTerm: v1.PodAffinityTerm{
						LabelSelector: &metav1.LabelSelector{
							MatchExpressions: []metav1.LabelSelectorRequirement{
								{
									Key:      "app",
									Operator: "In",
									Values:   []string{fmt.Sprintf("%s", req.Name)},
								},
							},
						},
						TopologyKey: "kubernetes.io/hostname",
					},
				},
			},
		},
	}

	if ud, err = e.k8s.GenerateUnitedDeploymentSpec(unitedDeploymentRequest, envs, nil, &appAffinity, probe); err != nil {
		return err
	}

	edgeServiceRequest := &apistructs.GenerateEdgeServiceRequest{
		Name:      req.Name,
		Namespace: namespace,
		PortMaps:  req.PortMaps,
	}
	if svc, err = e.k8s.GenerateEdgeServiceSpec(edgeServiceRequest); err != nil {
		return err
	}

	//orgInfo, err := e.getOrgInfo(req.OrgID)
	//if err != nil {
	//	return 0, err
	//}
	//Create namespcae
	if err = e.k8s.CreateNamespace(clusterInfo.Name, &v1.Namespace{
		TypeMeta: metav1.TypeMeta{},
		ObjectMeta: metav1.ObjectMeta{
			Name: namespace,
		},
	}); err != nil {
		return err
	}
	//create image pull secret
	if req.RegistryAddr != "" {
		dockerConfigJson := apistructs.RegistryAuthJson{}
		dockerConfigJson.Auths = make(map[string]apistructs.RegistryUserInfo)
		imageAddr := strings.Split(req.RegistryAddr, "/")[0]
		authString := base64.StdEncoding.EncodeToString([]byte(req.RegistryUser + ":" + req.RegistryPassword))
		dockerConfigJson.Auths[imageAddr] = apistructs.RegistryUserInfo{Auth: authString}

		var sData []byte
		if sData, err = json.Marshal(dockerConfigJson); err != nil {
			return err
		}

		if err := e.k8s.CreateSecret(clusterInfo.Name, namespace, &v1.Secret{
			TypeMeta: metav1.TypeMeta{
				Kind:       SecretKind,
				APIVersion: SecretApiVersion,
			},
			ObjectMeta: metav1.ObjectMeta{
				Name:      req.Name,
				Namespace: namespace,
			},
			Data: map[string][]byte{".dockerconfigjson": sData},
			Type: "kubernetes.io/dockerconfigjson",
		}); err != nil {

		}
	}
	//create uniteddeployment
	logrus.Errorf("[edge] uniteddeployment spec is %+v", ud)
	if err = e.k8s.CreateUnitedDeployment(clusterInfo.Name, ud); err != nil {
		return e.k8s.DeleteNamespace(clusterInfo.Name, namespace)
	}
	//create service
	if err = e.k8s.CreateService(clusterInfo.Name, namespace, svc); err != nil {
		return e.k8s.DeleteNamespace(clusterInfo.Name, namespace)
	}

	EdgeSites, err := json.MarshalIndent(req.EdgeSites, "", "\t")
	if err != nil {
		return err
	}
	PortMaps, err := json.MarshalIndent(req.PortMaps, "", "\t")
	if err != nil {
		return err
	}

	DependApp, err := json.MarshalIndent(req.DependApp, "", "\t")
	if err != nil {
		return err
	}

	appExtraData = make(map[string]string)
	appHostKey := strings.ToUpper(fmt.Sprintf("%s_HOST", req.Name))
	appExtraData[appHostKey] = fmt.Sprintf("%s.%s:%d", req.Name, namespace, req.PortMaps[0].ServicePort)
	extraData, err := json.MarshalIndent(appExtraData, "", "\t")
	if err != nil {
		return err
	}

	edgeApp := &dbclient.EdgeApp{
		BaseModel:           dbengine.BaseModel{},
		OrgID:               req.OrgID,
		Name:                req.Name,
		ClusterID:           req.ClusterID,
		Type:                req.Type,
		Image:               req.Image,
		RegistryAddr:        req.RegistryAddr,
		RegistryUser:        req.RegistryUser,
		RegistryPassword:    req.RegistryPassword,
		HealthCheckType:     req.HealthCheckType,
		HealthCheckHttpPort: req.HealthCheckHttpPort,
		HealthCheckHttpPath: req.HealthCheckHttpPath,
		HealthCheckExec:     req.HealthCheckExec,
		ProductID:           req.ProductID,
		AddonName:           req.AddonName,
		AddonVersion:        req.AddonVersion,
		ConfigSetName:       req.ConfigSetName,
		Replicas:            req.Replicas,
		Description:         req.Description,
		EdgeSites:           string(EdgeSites),
		LimitCpu:            req.LimitCpu,
		DependApp:           string(DependApp),
		RequestCpu:          req.RequestCpu,
		LimitMem:            req.LimitMem,
		RequestMem:          req.RequestMem,
		PortMaps:            string(PortMaps),
		ExtraData:           string(extraData),
	}
	if err = e.db.CreateEdgeApp(edgeApp); err != nil {
		deleteErr := e.k8s.DeleteUnitedDeployment(clusterInfo.Name, namespace, req.Name)
		if deleteErr != nil {
			return deleteErr
		}
		return err
	}
	return nil
}

func (e *Edge) UpdateApp(ctx context.Context, edgeAppID int64, req *apistructs.EdgeAppUpdateRequest) error {

	var ud *v1alpha1.UnitedDeployment
	var oldUd *v1alpha1.UnitedDeployment
	var err error
	var clusterInfo *clusterpb.ClusterInfo
	var svc *v1.Service
	var oldSvc *v1.Service
	var appExtraData map[string]string
	var envs []v1.EnvVar
	var probe *v1.Probe

	namespace := fmt.Sprintf("%s-%s", EdgeAppPrefix, req.Name)

	// 1core=1000m
	requestCpu := fmt.Sprintf("%.fm", req.RequestCpu*1000)
	// 1Mi=1024K=1024x1024B
	requestMemory := fmt.Sprintf("%.fMi", req.RequestMem)
	limitCpu := fmt.Sprintf("%.fm", req.LimitCpu*1000)
	limitMemory := fmt.Sprintf("%.fMi", req.LimitMem)

	if req.HealthCheckType != "" {
		probe = e.k8s.GenerateHealthCheckSpec(&apistructs.GenerateHeathCheckRequest{
			HealthCheckType:     req.HealthCheckType,
			HealthCheckHttpPort: req.HealthCheckHttpPort,
			HealthCheckHttpPath: req.HealthCheckHttpPath,
			HealthCheckExec:     req.HealthCheckExec,
		})
	}

	configSetName, err := e.getConfigSetName(req.OrgID, req.ConfigSetName)
	if err != nil {
		return fmt.Errorf("get configset %s namespaces error: %v", req.ConfigSetName, err)
	}

	unitedDeploymentRequest := &apistructs.GenerateUnitedDeploymentRequest{
		Name:       req.Name,
		Namespace:  namespace,
		RequestCPU: requestCpu,
		LimitCPU:   limitCpu,
		RequestMem: requestMemory,
		LimitMem:   limitMemory,
		Image:      req.Image,
		Type:       DeploymentType,
		ConfigSet:  configSetName,
		EdgeSites:  req.EdgeSites,
		Replicas:   req.Replicas,
	}

	if clusterInfo, err = e.getClusterInfo(ctx, req.ClusterID); err != nil {
		return err
	}
	//Handling environment variable injection for dependent applications
	var tmpExtraData map[string]string
	var app *dbclient.EdgeApp
	sort.Strings(req.DependApp)

	deleteSites := make([]string, 0)

	if oldUd, err = e.k8s.GetUnitedDeployment(clusterInfo.Name, namespace, req.Name); err != nil {
		return err
	}

	for _, pool := range oldUd.Spec.Topology.Pools {
		isRemove := true
		for _, site := range req.EdgeSites {
			if pool.Name == site {
				isRemove = false
				continue
			}
		}
		if isRemove {
			deleteSites = append(deleteSites, pool.Name)
		}
	}

	dependApps, err := e.db.ListDependsEdgeApps(req.OrgID, req.ClusterID, req.Name)
	if err != nil {
		return fmt.Errorf("get depends app eror: %v", err)
	}

	for _, edgeApp := range *dependApps {
		for _, delSite := range deleteSites {
			if strings.Contains(edgeApp.EdgeSites, fmt.Sprintf("\"%s\"", delSite)) {
				return fmt.Errorf("%s had been releated %s in site %s, please offline it first", edgeApp.Name, req.Name, delSite)
			}
		}
	}

	for i := range req.DependApp {
		var keys []string
		if app, err = e.db.GetEdgeAppByName(req.DependApp[i], req.OrgID); err != nil {
			return err
		}
		if err := json.Unmarshal([]byte(app.ExtraData), &tmpExtraData); err != nil {
			return err
		}
		for k := range tmpExtraData {
			keys = append(keys, k)
		}
		sort.Strings(keys)
		for _, k := range keys {
			envs = append(envs, v1.EnvVar{
				Name:  k,
				Value: tmpExtraData[k],
			})
		}
		// avoid circular dependency
		if strings.Contains(app.DependApp, fmt.Sprintf("\"%s\"", req.Name)) {
			for _, site := range req.EdgeSites {
				if strings.Contains(app.EdgeSites, fmt.Sprintf("\"%s\"", site)) {
					return fmt.Errorf("%s had been releated %s in site %s", app.Name, req.Name, site)
				}
			}
		}
	}
	envs = append(envs, v1.EnvVar{
		Name:  "DICE_ORG_ID",
		Value: strconv.FormatInt(req.OrgID, 10),
	})
	envs = append(envs, v1.EnvVar{
		Name:  "DICE_EDGE_APPLICATION_NAME",
		Value: req.Name,
	})
	envs = append(envs, v1.EnvVar{
		Name:  "DICE_CLUSTER_NAME",
		Value: clusterInfo.Name,
	})
	envs = append(envs, v1.EnvVar{
		Name:  "CONFIGSET_NAMESPACE",
		Value: configSetName,
	})

	//update image pull secret
	if req.RegistryAddr != "" {
		dockerConfigJson := apistructs.RegistryAuthJson{}
		dockerConfigJson.Auths = make(map[string]apistructs.RegistryUserInfo)
		imageAddr := strings.Split(req.RegistryAddr, "/")[0]
		authString := base64.StdEncoding.EncodeToString([]byte(req.RegistryUser + ":" + req.RegistryPassword))
		dockerConfigJson.Auths[imageAddr] = apistructs.RegistryUserInfo{Auth: authString}

		var sData []byte
		if sData, err = json.Marshal(dockerConfigJson); err != nil {
			return err
		}

		if err := e.k8s.UpdateSecret(clusterInfo.Name, namespace, &v1.Secret{
			TypeMeta: metav1.TypeMeta{
				Kind:       SecretKind,
				APIVersion: SecretApiVersion,
			},
			ObjectMeta: metav1.ObjectMeta{
				Name:      req.Name,
				Namespace: namespace,
			},
			Data: map[string][]byte{".dockerconfigjson": sData},
			Type: "kubernetes.io/dockerconfigjson",
		}); err != nil {
			return err
		}
	}

	if ud, err = e.k8s.GenerateUnitedDeploymentSpec(unitedDeploymentRequest, envs, nil, nil, probe); err != nil {
		return err
	}
	ud.ResourceVersion = oldUd.ResourceVersion
	edgeServiceRequest := &apistructs.GenerateEdgeServiceRequest{
		Name:      req.Name,
		Namespace: namespace,
		PortMaps:  req.PortMaps,
	}
	if oldSvc, err = e.k8s.GetService(clusterInfo.Name, namespace, req.Name); err != nil {
		return err
	}
	if svc, err = e.k8s.GenerateEdgeServiceSpec(edgeServiceRequest); err != nil {
		return err
	}
	svc.ResourceVersion = oldSvc.ResourceVersion
	svc.Spec.ClusterIP = oldSvc.Spec.ClusterIP
	//orgInfo, err := e.getOrgInfo(req.OrgID)
	//if err != nil {
	//	return 0, err
	//}
	//update uniteddeployment
	logrus.Errorf("[edge] uniteddeployment spec is %+v", ud)
	if err = e.k8s.UpdateUnitedDeployment(clusterInfo.Name, namespace, ud); err != nil {
		return err
	}
	//create service
	if err = e.k8s.UpdateService(clusterInfo.Name, namespace, svc); err != nil {
		return err
	}

	EdgeSites, err := json.MarshalIndent(req.EdgeSites, "", "\t")
	if err != nil {
		return err
	}
	PortMaps, err := json.MarshalIndent(req.PortMaps, "", "\t")
	if err != nil {
		return err
	}

	DependApp, err := json.MarshalIndent(req.DependApp, "", "\t")
	if err != nil {
		return err
	}

	appExtraData = make(map[string]string)
	appHostKey := strings.ToUpper(fmt.Sprintf("%s_HOST", req.Name))
	appExtraData[appHostKey] = fmt.Sprintf("%s.%s:%d", req.Name, namespace, req.PortMaps[0].ServicePort)
	extraData, err := json.MarshalIndent(appExtraData, "", "\t")
	if err != nil {
		return err
	}

	app, err = e.db.GetEdgeApp(edgeAppID)
	if err != nil {
		return err
	}
	app.Image = req.Image
	app.EdgeSites = string(EdgeSites)
	app.ConfigSetName = req.ConfigSetName
	app.Replicas = req.Replicas
	app.LimitMem = req.LimitMem
	app.RequestMem = req.RequestMem
	app.LimitCpu = req.LimitCpu
	app.RequestCpu = req.RequestCpu
	app.RegistryAddr = req.RegistryAddr
	app.RegistryUser = req.RegistryUser
	app.RegistryPassword = req.RegistryPassword
	app.HealthCheckType = req.HealthCheckType
	app.HealthCheckHttpPort = req.HealthCheckHttpPort
	app.HealthCheckHttpPath = req.HealthCheckHttpPath
	app.HealthCheckExec = req.HealthCheckExec
	app.DependApp = string(DependApp)
	app.PortMaps = string(PortMaps)
	app.ExtraData = string(extraData)

	if err = e.db.UpdateEdgeApp(app); err != nil {
		return err
	}
	return nil
}

func (e *Edge) GetAppStatus(ctx context.Context, appID int64) (*apistructs.EdgeAppStatusResponse, error) {
	var err error
	var app *dbclient.EdgeApp
	var ud *v1alpha1.UnitedDeployment
	var clusterInfo *clusterpb.ClusterInfo
	var appStatus apistructs.EdgeAppStatusResponse
	var appInfo *apistructs.EdgeAppInfo
	var tmpStatus string

	if app, err = e.db.GetEdgeApp(appID); err != nil {
		return nil, err
	}
	if appInfo, err = e.ConvertToEdgeAppInfo(app); err != nil {
		return nil, err
	}
	namespace := fmt.Sprintf("%s-%s", EdgeAppPrefix, app.Name)
	if clusterInfo, err = e.getClusterInfo(ctx, appInfo.ClusterID); err != nil {
		return nil, err
	}
	if ud, err = e.k8s.GetUnitedDeployment(clusterInfo.Name, namespace, appInfo.Name); err != nil {
		return nil, err
	}
	appStatus.Name = appInfo.Name
	appStatus.OrgID = appInfo.OrgID
	appStatus.Type = appInfo.Type
	appStatus.ClusterID = appInfo.ClusterID
	appStatusFromUd := ud.Status
	for i := range appInfo.EdgeSites {
		if appStatusFromUd.PoolReadyReplicas[appInfo.EdgeSites[i]] == appStatusFromUd.PoolReplicas[appInfo.EdgeSites[i]] {
			tmpStatus = EdgeAppSucceedStatus
		} else {
			tmpStatus = EdgeAppDeployingStatus
		}
		appStatus.SiteList = append(appStatus.SiteList, apistructs.EdgeAppSiteStatus{
			SITE:   appInfo.EdgeSites[i],
			STATUS: tmpStatus,
		})
	}
	return &appStatus, nil
}

func (e *Edge) DeleteApp(ctx context.Context, appID int64) error {
	var err error
	var apps *[]dbclient.EdgeApp
	var app *dbclient.EdgeApp
	var dependApp []string
	var clusterInfo *clusterpb.ClusterInfo

	if app, err = e.db.GetEdgeApp(appID); err != nil {
		return err
	}

	if apps, err = e.db.ListAllEdgeApp(app.OrgID); err != nil {
		return err
	}

	for i := range *apps {
		item, err := e.ConvertToEdgeAppInfo(&(*apps)[i])
		if err != nil {
			return err
		}
		if e.IsContain(item.DependApp, app.Name) {
			dependApp = append(dependApp, item.Name)
		}
	}
	//determiate depend app
	if len(dependApp) > 0 {
		errstr := fmt.Sprintf("There are apps depend on it: %v", strings.Join(dependApp, ","))
		return errors.New(errstr)
	}

	namespace := fmt.Sprintf("%s-%s", EdgeAppPrefix, app.Name)

	if clusterInfo, err = e.getClusterInfo(ctx, app.ClusterID); err != nil {
		return err
	}

	if err = e.k8s.DeleteNamespace(clusterInfo.Name, namespace); err != nil {
		return err
	}
	if err = e.db.DeleteEdgeApp(appID); err != nil {
		return err
	}
	return nil
}

func (e *Edge) RestartAppSite(ctx context.Context, edgeApp *dbclient.EdgeApp, siteName string) error {
	var (
		deploymentName string
	)

	namespace := fmt.Sprintf("%s-%s", EdgeAppPrefix, edgeApp.Name)
	deploymentPrefix := fmt.Sprintf("%s-%s", edgeApp.Name, siteName)

	clusterInfo, err := e.getClusterInfo(ctx, edgeApp.ClusterID)
	if err != nil {
		return fmt.Errorf("get cluster info error: %v", err)
	}

	deployList, err := e.k8s.ListDeployment(clusterInfo.Name, namespace)

	if err != nil {
		return fmt.Errorf("list deployment error: %v", err)
	}

	for _, item := range deployList.Items {
		if item.Name[:strings.LastIndex(item.Name, "-")] == deploymentPrefix {
			deploymentName = item.Name
			break
		}
	}

	if err = e.k8s.DeleteDeployment(clusterInfo.Name, namespace, deploymentName); err != nil {
		return fmt.Errorf("delete deplyoment %s in namespaces %s error: %v", deploymentName, namespace, err)
	}

	return nil
}

func (e *Edge) OfflineAppSite(ctx context.Context, edgeApp *dbclient.EdgeApp, siteName string) error {
	var (
		isResourceFound bool
		edgeSites       []string
		newPools        []v1alpha1.Pool
		updateUd        = &v1alpha1.UnitedDeployment{
			TypeMeta: metav1.TypeMeta{
				Kind:       UnitedDeploymentKind,
				APIVersion: UnitedDeploymentAPIVersion,
			},
		}
	)

	// If this application is depend by other application (effective dependence: deployed in the same site)
	// Offline other depend application in this site first.
	dependApps, err := e.db.ListDependsEdgeApps(edgeApp.OrgID, edgeApp.ClusterID, edgeApp.Name)
	if err != nil {
		return fmt.Errorf("get depend apps eror: %v", err)
	}

	relatedApps := make([]string, 0)

	for _, app := range *dependApps {
		if strings.Contains(app.EdgeSites, fmt.Sprintf("\"%s\"", siteName)) {
			relatedApps = append(relatedApps, app.Name)
		}
	}
	if len(relatedApps) != 0 {
		return fmt.Errorf("application %s related this application in site: %s", fmt.Sprint(relatedApps), siteName)
	}

	namespace := fmt.Sprintf("%s-%s", EdgeAppPrefix, edgeApp.Name)

	clusterInfo, err := e.getClusterInfo(ctx, edgeApp.ClusterID)
	if err != nil {
		return fmt.Errorf("get cluster info error: %v", err)
	}

	ud, err := e.k8s.GetUnitedDeployment(clusterInfo.Name, namespace, edgeApp.Name)
	if err != nil {
		return fmt.Errorf("get uniteddeployment error: %v", err)
	}

	pools := ud.Spec.Topology.Pools

	for index, pool := range pools {
		if pool.Name == siteName {
			isResourceFound = true
			newPools = append(pools[:index], pools[index+1:]...)
		}
	}

	if !isResourceFound {
		return fmt.Errorf("%s not found in node pool resource", siteName)
	}

	ud.Spec.Topology.Pools = newPools

	updateUd.Name = ud.Name
	updateUd.ResourceVersion = ud.ResourceVersion
	updateUd.Spec = ud.Spec

	if err = e.k8s.UpdateUnitedDeployment(clusterInfo.Name, namespace, updateUd); err != nil {
		return fmt.Errorf("update united deployment error: %v", err)
	}

	if len(edgeApp.EdgeSites) != 0 {
		if err := json.Unmarshal([]byte(edgeApp.EdgeSites), &edgeSites); err != nil {
			return err
		}
	}

	for index, site := range edgeSites {
		if site == siteName {
			edgeSites = append(edgeSites[:index], edgeSites[index+1:]...)
		}
	}

	sitesJson, err := json.MarshalIndent(edgeSites, "", "\t")
	if err != nil {
		return err
	}

	edgeApp.EdgeSites = string(sitesJson)

	err = e.db.UpdateEdgeApp(edgeApp)
	if err != nil {
		updateUd.Spec.Topology.Pools = pools
		if err = e.k8s.UpdateUnitedDeployment(clusterInfo.Name, namespace, updateUd); err != nil {
			return fmt.Errorf("back update united deployment error")
		}
		return fmt.Errorf("update edge app error: %v", err)
	}

	return nil
}
