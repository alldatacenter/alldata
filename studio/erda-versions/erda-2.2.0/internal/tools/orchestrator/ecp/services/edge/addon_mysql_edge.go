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
	"encoding/json"
	"errors"
	"fmt"
	"sort"
	"strconv"
	"strings"

	"github.com/sirupsen/logrus"
	"gopkg.in/yaml.v3"
	v1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/ecp/dbclient"
	"github.com/erda-project/erda/pkg/clientgo/apis/openyurt/v1alpha1"
	"github.com/erda-project/erda/pkg/crypto/uuid"
	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/parser/diceyml"
)

const (
	MasterTag = "mysql-master"
	SlaveTag  = "mysql-slave"
)

func (e *Edge) CreateEdgeMysql(ctx context.Context, req *apistructs.EdgeAppCreateRequest) error {
	var err error
	var extensionResult *apistructs.ExtensionVersion
	var masterUD *v1alpha1.UnitedDeployment
	var slaveUD *v1alpha1.UnitedDeployment
	var masterSvc *v1.Service
	var slaveSvc *v1.Service
	var clusterInfo *clusterpb.ClusterInfo
	var envs []v1.EnvVar
	var mysqlExtraData map[string]string
	var mysqlPortMap []apistructs.PortMap
	var volumeClaimTemplates []v1.PersistentVolumeClaim
	var volumeMount []v1.VolumeMount
	var masterAffinity v1.Affinity
	var slaveAffinity v1.Affinity

	extensionRequest := apistructs.ExtensionVersionGetRequest{
		Name:       req.AddonName,
		Version:    req.AddonVersion,
		YamlFormat: true,
	}
	//get addon result
	extensionResult, err = e.bdl.GetExtensionVersion(extensionRequest)
	if err != nil {
		logrus.Errorf("failed to get extension result: %v", err)
		return err
	}

	//parse edge mysql dice.yaml
	var extDice diceyml.Object
	extDiceStr := extensionResult.Dice.(string)
	err = yaml.Unmarshal([]byte(extDiceStr), &extDice)

	namespace := fmt.Sprintf("%s-%s", EdgeAppPrefix, req.Name)

	mysqlPassword := uuid.UUID()[:8] + "0@x" + uuid.UUID()[:8]

	masterImage := extDice.Services[MasterTag].Image
	slaveImage := extDice.Services[SlaveTag].Image

	masterRequestCpu := fmt.Sprintf("%.fm", extDice.Services[MasterTag].Resources.CPU*1000)
	masterRequestMemory := fmt.Sprintf("%.dMi", extDice.Services[MasterTag].Resources.Mem)
	masterLimitCpu := fmt.Sprintf("%.fm", extDice.Services[MasterTag].Resources.MaxCPU*1000)
	masterLimitMemory := fmt.Sprintf("%.dMi", extDice.Services[MasterTag].Resources.Mem)

	slaveRequestCpu := fmt.Sprintf("%.fm", extDice.Services[SlaveTag].Resources.CPU*1000)
	slaveRequestMemory := fmt.Sprintf("%.dMi", extDice.Services[SlaveTag].Resources.Mem)
	slaveLimitCpu := fmt.Sprintf("%.fm", extDice.Services[SlaveTag].Resources.MaxCPU*1000)
	slaveLimitMemory := fmt.Sprintf("%.dMi", extDice.Services[SlaveTag].Resources.Mem)

	if clusterInfo, err = e.getClusterInfo(ctx, req.ClusterID); err != nil {
		return err
	}
	envs = append(envs, v1.EnvVar{
		Name:  "MASTER_SVC_NAME",
		Value: fmt.Sprintf("%s-%s.%s", req.Name, MasterTag, namespace),
	})

	envs = append(envs, v1.EnvVar{
		Name:  "MYSQL_USER_PASSWORD",
		Value: mysqlPassword,
	})

	envs = append(envs, v1.EnvVar{
		Name: "POD_NAME",
		ValueFrom: &v1.EnvVarSource{
			FieldRef: &v1.ObjectFieldSelector{
				FieldPath: "metadata.name",
			},
		},
	})
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
	var scName = "dice-local-volume"
	volumeClaimTemplates = append(volumeClaimTemplates, v1.PersistentVolumeClaim{
		ObjectMeta: metav1.ObjectMeta{
			Name: "mysqldata",
		},
		Spec: v1.PersistentVolumeClaimSpec{
			AccessModes:      []v1.PersistentVolumeAccessMode{"ReadWriteOnce"},
			StorageClassName: &scName,
			Resources: v1.ResourceRequirements{
				Requests: v1.ResourceList{
					v1.ResourceStorage: resource.MustParse("10G"),
				},
			},
		},
	})

	volumeMount = append(volumeMount, v1.VolumeMount{
		Name:      "mysqldata",
		MountPath: "/data",
	})
	masterUnitedDeploymentRequest := &apistructs.GenerateUnitedDeploymentRequest{
		Name:       fmt.Sprintf("%s-%s", req.Name, MasterTag),
		Namespace:  namespace,
		RequestCPU: masterRequestCpu,
		LimitCPU:   masterLimitCpu,
		RequestMem: masterRequestMemory,
		LimitMem:   masterLimitMemory,
		Image:      masterImage,
		Type:       StatefulSetType,
		ConfigSet:  req.ConfigSetName,
		EdgeSites:  req.EdgeSites,
		Replicas:   1,
	}
	masterAffinity = v1.Affinity{
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
									Values:   []string{fmt.Sprintf("%s-%s", req.Name, SlaveTag)},
								},
							},
						},
						TopologyKey: "kubernetes.io/hostname",
					},
				},
			},
		},
	}
	//generage mysql master uniteddeploymentspec
	if masterUD, err = e.k8s.GenerateUnitedDeploymentSpec(masterUnitedDeploymentRequest, envs, volumeMount, &masterAffinity, nil); err != nil {
		return err
	}

	masterUD.Spec.WorkloadTemplate.StatefulSetTemplate.Spec.VolumeClaimTemplates = volumeClaimTemplates
	slaveUnitedDeploymentRequest := &apistructs.GenerateUnitedDeploymentRequest{
		Name:       fmt.Sprintf("%s-%s", req.Name, SlaveTag),
		Namespace:  namespace,
		RequestCPU: slaveRequestCpu,
		LimitCPU:   slaveLimitCpu,
		RequestMem: slaveRequestMemory,
		LimitMem:   slaveLimitMemory,
		Image:      slaveImage,
		Type:       StatefulSetType,
		ConfigSet:  req.ConfigSetName,
		EdgeSites:  req.EdgeSites,
		Replicas:   1,
	}
	slaveAffinity = v1.Affinity{
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
									Values:   []string{fmt.Sprintf("%s-%s", req.Name, MasterTag)},
								},
							},
						},
						TopologyKey: "kubernetes.io/hostname",
					},
				},
			},
		},
	}
	//generage mysql master uniteddeploymentspec
	if slaveUD, err = e.k8s.GenerateUnitedDeploymentSpec(slaveUnitedDeploymentRequest, envs, volumeMount, &slaveAffinity, nil); err != nil {
		return err
	}
	slaveUD.Spec.WorkloadTemplate.StatefulSetTemplate.Spec.VolumeClaimTemplates = volumeClaimTemplates
	mysqlPortMap = append(mysqlPortMap, apistructs.PortMap{
		Protocol:      "TCP",
		ContainerPort: 3306,
		ServicePort:   3306,
	})

	masterServiceRequest := &apistructs.GenerateEdgeServiceRequest{
		Name:      fmt.Sprintf("%s-%s", req.Name, MasterTag),
		Namespace: namespace,
		PortMaps:  mysqlPortMap,
	}
	if masterSvc, err = e.k8s.GenerateEdgeServiceSpec(masterServiceRequest); err != nil {
		return err
	}

	slaveServiceRequest := &apistructs.GenerateEdgeServiceRequest{
		Name:      fmt.Sprintf("%s-%s", req.Name, SlaveTag),
		Namespace: namespace,
		PortMaps:  mysqlPortMap,
	}
	if slaveSvc, err = e.k8s.GenerateEdgeServiceSpec(slaveServiceRequest); err != nil {
		return err
	}

	//Create namespcae
	if err = e.k8s.CreateNamespace(clusterInfo.Name, &v1.Namespace{
		TypeMeta: metav1.TypeMeta{},
		ObjectMeta: metav1.ObjectMeta{
			Name: namespace,
		},
	}); err != nil {
		return err
	}
	//create master uniteddeployment
	if err = e.k8s.CreateUnitedDeployment(clusterInfo.Name, masterUD); err != nil {
		return err
	}
	//create master service
	if err = e.k8s.CreateService(clusterInfo.Name, namespace, masterSvc); err != nil {
		return err
	}

	//create slave uniteddeployment
	if err = e.k8s.CreateUnitedDeployment(clusterInfo.Name, slaveUD); err != nil {
		return err
	}
	//create slave service
	if err = e.k8s.CreateService(clusterInfo.Name, namespace, slaveSvc); err != nil {
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

	mysqlExtraData = make(map[string]string)
	mysqlExtraData["MYSQL_HOST"] = fmt.Sprintf("%s-%s.%s", req.Name, MasterTag, namespace)
	mysqlExtraData["MYSQL_USER"] = "mysql"
	mysqlExtraData["MYSQL_PASSWORD"] = mysqlPassword
	extraData, err := json.MarshalIndent(mysqlExtraData, "", "\t")
	if err != nil {
		return err
	}

	// create edge_app record
	edgeApp := &dbclient.EdgeApp{
		BaseModel:     dbengine.BaseModel{},
		OrgID:         req.OrgID,
		Name:          req.Name,
		ClusterID:     req.ClusterID,
		Type:          req.Type,
		Image:         req.Image,
		ProductID:     req.ProductID,
		AddonName:     req.AddonName,
		AddonVersion:  req.AddonVersion,
		ConfigSetName: req.ConfigSetName,
		Replicas:      req.Replicas,
		Description:   req.Description,
		EdgeSites:     string(EdgeSites),
		LimitCpu:      req.LimitCpu,
		RequestCpu:    req.RequestCpu,
		LimitMem:      req.LimitMem,
		RequestMem:    req.RequestMem,
		PortMaps:      string(PortMaps),
		ExtraData:     string(extraData),
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

func (e *Edge) UpdateEdgeMysql(ctx context.Context, edgeAppID int64, req *apistructs.EdgeAppUpdateRequest) error {
	var masterUD *v1alpha1.UnitedDeployment
	var slaveUD *v1alpha1.UnitedDeployment
	var err error
	var app *dbclient.EdgeApp
	var clusterInfo *clusterpb.ClusterInfo
	var nodePools []v1alpha1.Pool
	var replicas int32

	namespace := fmt.Sprintf("%s-%s", EdgeAppPrefix, req.Name)
	replicas = 1
	if clusterInfo, err = e.getClusterInfo(ctx, req.ClusterID); err != nil {
		return err
	}

	app, err = e.db.GetEdgeApp(edgeAppID)
	if err != nil {
		return err
	}
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
			Replicas: &replicas,
		}
		nodePools = append(nodePools, pool)

	}

	if masterUD, err = e.k8s.GetUnitedDeployment(clusterInfo.Name, namespace, fmt.Sprintf("%s-%s", req.Name, MasterTag)); err != nil {
		return err
	}
	if slaveUD, err = e.k8s.GetUnitedDeployment(clusterInfo.Name, namespace, fmt.Sprintf("%s-%s", req.Name, SlaveTag)); err != nil {
		return err
	}

	deleteSites := make([]string, 0)

	for _, pool := range masterUD.Spec.Topology.Pools {
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

	dependsApp, err := e.db.ListDependsEdgeApps(req.OrgID, req.ClusterID, app.Name)
	if err != nil {
		return fmt.Errorf("get depends app eror: %v", err)
	}

	for _, edgeApp := range *dependsApp {
		for _, delSite := range deleteSites {
			if strings.Contains(edgeApp.EdgeSites, fmt.Sprintf("\"%s\"", delSite)) {
				return fmt.Errorf("%s had been releated %s in site %s, please offline it first", edgeApp.Name, req.Name, delSite)
			}
		}
	}

	masterUD.Kind = UnitedDeploymentKind
	masterUD.APIVersion = UnitedDeploymentAPIVersion
	masterUD.Spec.Topology.Pools = nodePools
	slaveUD.Kind = UnitedDeploymentKind
	slaveUD.APIVersion = UnitedDeploymentAPIVersion
	slaveUD.Spec.Topology.Pools = nodePools

	if err = e.k8s.UpdateUnitedDeployment(clusterInfo.Name, namespace, masterUD); err != nil {
		return err
	}

	if err = e.k8s.UpdateUnitedDeployment(clusterInfo.Name, namespace, slaveUD); err != nil {
		return err
	}

	EdgeSites, err := json.MarshalIndent(req.EdgeSites, "", "\t")
	if err != nil {
		return err
	}

	//update edgesites
	app.EdgeSites = string(EdgeSites)

	if err = e.db.UpdateEdgeApp(app); err != nil {
		return err
	}
	return nil
}

func (e *Edge) GetEdgeMysqlStatus(ctx context.Context, appID int64) (*apistructs.EdgeAppStatusResponse, error) {
	var err error
	var app *dbclient.EdgeApp
	var udMaster *v1alpha1.UnitedDeployment
	var udSlave *v1alpha1.UnitedDeployment
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
	if udMaster, err = e.k8s.GetUnitedDeployment(clusterInfo.Name, namespace, fmt.Sprintf("%s-%s", appInfo.Name, MasterTag)); err != nil {
		return nil, err
	}
	if udSlave, err = e.k8s.GetUnitedDeployment(clusterInfo.Name, namespace, fmt.Sprintf("%s-%s", appInfo.Name, SlaveTag)); err != nil {
		return nil, err
	}
	appStatus.Name = appInfo.Name
	appStatus.OrgID = appInfo.OrgID
	appStatus.Type = appInfo.Type
	appStatus.ClusterID = appInfo.ClusterID
	appStatusFromUdMaster := udMaster.Status
	appStatusFromUdSlave := udSlave.Status
	for i := range appInfo.EdgeSites {
		if (appStatusFromUdMaster.PoolReadyReplicas[appInfo.EdgeSites[i]] == appStatusFromUdMaster.PoolReplicas[appInfo.EdgeSites[i]]) && (appStatusFromUdSlave.PoolReadyReplicas[appInfo.EdgeSites[i]] == appStatusFromUdSlave.PoolReplicas[appInfo.EdgeSites[i]]) {
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

func (e *Edge) DeleteEdgeMysql(ctx context.Context, appID int64) error {
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

func (e *Edge) RestartEdgeMysql(ctx context.Context, edgeApp *dbclient.EdgeApp, siteName string) error {
	var (
		masterName   string
		slaveName    string
		namespace    = fmt.Sprintf("%s-%s", EdgeAppPrefix, edgeApp.Name)
		masterPrefix = fmt.Sprintf("%s-%s-%s", edgeApp.Name, MasterTag, siteName)
		slavePrefix  = fmt.Sprintf("%s-%s-%s", edgeApp.Name, SlaveTag, siteName)
	)

	clusterInfo, err := e.getClusterInfo(ctx, edgeApp.ClusterID)
	if err != nil {
		return fmt.Errorf("get cluster info error: %v", err)
	}

	stsList, err := e.k8s.ListStatefulSet(clusterInfo.Name, namespace)
	if err != nil {
		return fmt.Errorf("list statefulSet error: %v", err)
	}

	for _, item := range stsList.Items {
		if item.Name[:strings.LastIndex(item.Name, "-")] == masterPrefix {
			masterName = item.Name
		}
		if item.Name[:strings.LastIndex(item.Name, "-")] == slavePrefix {
			slaveName = item.Name
		}
	}

	if err = e.k8s.DeleteStatefulSet(clusterInfo.Name, namespace, masterName); err != nil {
		return fmt.Errorf("delete statefulSet(mysql master) %s in namespaces %s error: %v", masterName, namespace, err)
	}

	if err = e.k8s.DeleteStatefulSet(clusterInfo.Name, namespace, slaveName); err != nil {
		return fmt.Errorf("delete statefulSet(mysql slave) %s in namespaces %s error: %v", masterName, namespace, err)
	}

	return nil
}

func (e *Edge) OfflineEdgeMysql(ctx context.Context, edgeApp *dbclient.EdgeApp, siteName string) error {
	var (
		isResourceFound bool
		edgeSites       []string
		newPools        []v1alpha1.Pool
		masterUDName    = fmt.Sprintf("%s-%s", edgeApp.Name, MasterTag)
		slaveUDName     = fmt.Sprintf("%s-%s", edgeApp.Name, SlaveTag)
		namespace       = fmt.Sprintf("%s-%s", EdgeAppPrefix, edgeApp.Name)
		updateMasterUD  = &v1alpha1.UnitedDeployment{
			TypeMeta: metav1.TypeMeta{
				Kind:       UnitedDeploymentKind,
				APIVersion: UnitedDeploymentAPIVersion,
			},
		}
		updateSlaveUD = &v1alpha1.UnitedDeployment{
			TypeMeta: metav1.TypeMeta{
				Kind:       UnitedDeploymentKind,
				APIVersion: UnitedDeploymentAPIVersion,
			},
		}
		relatedApps = make([]string, 0)
	)

	// If this addon is depend by other application (effective dependence: deployed in the same site)
	// Offline other depend application in this site first.
	dependApps, err := e.db.ListDependsEdgeApps(edgeApp.OrgID, edgeApp.ClusterID, edgeApp.Name)
	if err != nil {
		return fmt.Errorf("get depend apps eror: %v", err)
	}

	for _, app := range *dependApps {
		if strings.Contains(app.EdgeSites, fmt.Sprintf("\"%s\"", siteName)) {
			relatedApps = append(relatedApps, app.Name)
		}
	}

	if len(relatedApps) != 0 {
		return fmt.Errorf("application %s releated this application in site: %s", fmt.Sprint(relatedApps), siteName)
	}

	clusterInfo, err := e.getClusterInfo(ctx, edgeApp.ClusterID)
	if err != nil {
		return fmt.Errorf("get cluster info error: %v", err)
	}

	masterUD, err := e.k8s.GetUnitedDeployment(clusterInfo.Name, namespace, masterUDName)
	if err != nil {
		return fmt.Errorf("get uniteddeployment error: %v", err)
	}

	slaveUD, err := e.k8s.GetUnitedDeployment(clusterInfo.Name, namespace, slaveUDName)
	if err != nil {
		return fmt.Errorf("get uniteddeployment error: %v", err)
	}

	pools := masterUD.Spec.Topology.Pools

	for index, pool := range pools {
		if pool.Name == siteName {
			isResourceFound = true
			newPools = append(pools[:index], pools[index+1:]...)
		}
	}

	if !isResourceFound {
		return fmt.Errorf("%s not found in node pool resource", siteName)
	}

	masterUD.Spec.Topology.Pools = newPools
	slaveUD.Spec.Topology.Pools = newPools

	updateMasterUD.Name = masterUD.Name
	updateMasterUD.ResourceVersion = masterUD.ResourceVersion
	updateMasterUD.Spec = masterUD.Spec

	updateSlaveUD.Name = slaveUD.Name
	updateSlaveUD.ResourceVersion = slaveUD.ResourceVersion
	updateSlaveUD.Spec = slaveUD.Spec

	if err = e.k8s.UpdateUnitedDeployment(clusterInfo.Name, namespace, updateMasterUD); err != nil {
		return fmt.Errorf("update united deployment mysql master error: %v", err)
	}
	if err = e.k8s.UpdateUnitedDeployment(clusterInfo.Name, namespace, updateSlaveUD); err != nil {
		updateMasterUD.Spec.Topology.Pools = pools
		if err = e.k8s.UpdateUnitedDeployment(clusterInfo.Name, namespace, updateMasterUD); err != nil {
			return fmt.Errorf("back update united deployment error")
		}
		return fmt.Errorf("update united deployment mysql slave error: %v", err)
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
		updateMasterUD.Spec.Topology.Pools = pools
		if err = e.k8s.UpdateUnitedDeployment(clusterInfo.Name, namespace, updateMasterUD); err != nil {
			return fmt.Errorf("back update united deployment error")
		}
		updateSlaveUD.Spec.Topology.Pools = pools
		if err = e.k8s.UpdateUnitedDeployment(clusterInfo.Name, namespace, updateSlaveUD); err != nil {
			return fmt.Errorf("back update united deployment error")
		}
		return fmt.Errorf("update edge app error: %v", err)

	}

	return nil
}
