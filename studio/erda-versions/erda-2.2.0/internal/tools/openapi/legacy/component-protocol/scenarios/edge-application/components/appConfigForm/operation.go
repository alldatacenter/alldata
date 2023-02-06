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

package appconfigform

import (
	"encoding/json"
	"fmt"
	"regexp"
	"strconv"
	"strings"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/edge-application/i18n"
	i18r "github.com/erda-project/erda/pkg/i18n"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	IMAGE                 = "image"
	ADDON                 = "addon"
	MysqlAddonName        = "mysql-edge"
	MysqlAddonVersion     = "5.7"
	ApplicationNameLength = 30
)

type ComponentFormModal struct {
	ctxBundle protocol.ContextBundle
	component *apistructs.Component
}

func (c *ComponentFormModal) SetBundle(ctxBundle protocol.ContextBundle) error {
	if ctxBundle.Bdl == nil {
		return fmt.Errorf("invalie bundle")
	}
	c.ctxBundle = ctxBundle
	return nil
}

func (c *ComponentFormModal) SetComponent(component *apistructs.Component) error {
	if component == nil {
		return fmt.Errorf("invalie bundle")
	}
	c.component = component
	return nil
}

func (c *ComponentFormModal) OperateSubmit(orgID int64, identity apistructs.Identity) error {

	var (
		appSubmitReq  = Application{}
		createRequest *apistructs.EdgeAppCreateRequest
		updateRequest *apistructs.EdgeAppUpdateRequest
	)
	i18nLocale := c.ctxBundle.Bdl.GetLocale(c.ctxBundle.Locale)
	if _, ok := c.component.State["formData"]; !ok {
		return fmt.Errorf("must provide formdata")
	}

	jsonData, err := json.Marshal(c.component.State["formData"])
	if err != nil {
		return fmt.Errorf("marshal application create formdata error: %v", err)
	}

	err = json.Unmarshal(jsonData, &appSubmitReq)
	if err != nil {
		return err
	}

	if err = validateSubmitData(appSubmitReq, i18nLocale); err != nil {
		return err
	}

	// Update submit
	if appSubmitReq.ID > 0 {
		edgeApp, err := c.ctxBundle.Bdl.GetEdgeApp(appSubmitReq.ID, identity)
		if err != nil {
			return fmt.Errorf("get edge app error: %v", err)
		}

		switch appSubmitReq.DeployResource {
		case MIDDLEWARE:
			updateRequest = &apistructs.EdgeAppUpdateRequest{
				OrgID:        orgID,
				Name:         edgeApp.Name,
				ClusterID:    edgeApp.ClusterID,
				Type:         edgeApp.Type,
				AddonName:    edgeApp.AddonName,
				AddonVersion: edgeApp.AddonVersion,
				EdgeSites:    appSubmitReq.Sites,
			}
			break
		case MIRROR:
			updateRequest = &apistructs.EdgeAppUpdateRequest{
				OrgID:               orgID,
				Name:                edgeApp.Name,
				ClusterID:           edgeApp.ClusterID,
				Type:                edgeApp.Type,
				Image:               appSubmitReq.Mirror.MirrorAddress,
				RegistryAddr:        reverseRegistryAddr(appSubmitReq.Mirror.MirrorAddress),
				RegistryUser:        appSubmitReq.Mirror.RegistryUser,
				RegistryPassword:    appSubmitReq.Mirror.RegistryPassword,
				ConfigSetName:       appSubmitReq.ConfigSet,
				Replicas:            appSubmitReq.Replicaset,
				DependApp:           appSubmitReq.Depends,
				HealthCheckType:     appSubmitReq.HealthCheckConfig.HealthCheckType,
				HealthCheckHttpPort: appSubmitReq.HealthCheckConfig.Port,
				HealthCheckHttpPath: appSubmitReq.HealthCheckConfig.Path,
				HealthCheckExec:     appSubmitReq.HealthCheckConfig.HealthCheckExec,
				EdgeSites:           appSubmitReq.Sites,
				LimitCpu:            appSubmitReq.Storage.CpuLimit,
				RequestCpu:          appSubmitReq.Storage.CpuRequest,
				LimitMem:            appSubmitReq.Storage.MemoryLimit,
				RequestMem:          appSubmitReq.Storage.MemoryRequest,
				PortMaps:            convert2PortMap(appSubmitReq),
			}
			break
		default:
			return fmt.Errorf("no this deploy type %s", appSubmitReq.DeployResource)
		}

		if err = c.ctxBundle.Bdl.UpdateEdgeApp(updateRequest, appSubmitReq.ID, identity); err != nil {
			return fmt.Errorf("update edge app error: %v, appid: %d", err, appSubmitReq.ID)
		}

	} else if appSubmitReq.ID == 0 {
		// Create submit
		clusterInfo, err := c.ctxBundle.Bdl.GetCluster(appSubmitReq.Cluster)
		if err != nil {
			return fmt.Errorf("get cluster info error: %v", err)
		}

		switch appSubmitReq.DeployResource {
		case MIDDLEWARE:
			res := strings.Split(appSubmitReq.MiddlewareType, ":")
			if len(res) != 2 {
				return fmt.Errorf("illegal addon type and version: %s", appSubmitReq.MiddlewareType)
			}
			createRequest = &apistructs.EdgeAppCreateRequest{
				OrgID:        orgID,
				Name:         appSubmitReq.AppName,
				Type:         deConvertDeployResource(appSubmitReq.DeployResource),
				ClusterID:    int64(clusterInfo.ID),
				AddonName:    res[0],
				AddonVersion: res[1],
				EdgeSites:    appSubmitReq.Sites,
			}
			break
		case MIRROR:
			createRequest = &apistructs.EdgeAppCreateRequest{
				OrgID:               orgID,
				Name:                appSubmitReq.AppName,
				ClusterID:           int64(clusterInfo.ID),
				Type:                deConvertDeployResource(appSubmitReq.DeployResource),
				Image:               appSubmitReq.Mirror.MirrorAddress,
				RegistryAddr:        reverseRegistryAddr(appSubmitReq.Mirror.MirrorAddress),
				RegistryUser:        appSubmitReq.Mirror.RegistryUser,
				RegistryPassword:    appSubmitReq.Mirror.RegistryPassword,
				ConfigSetName:       appSubmitReq.ConfigSet,
				DependApp:           appSubmitReq.Depends,
				Replicas:            appSubmitReq.Replicaset,
				HealthCheckType:     appSubmitReq.HealthCheckConfig.HealthCheckType,
				HealthCheckHttpPort: appSubmitReq.HealthCheckConfig.Port,
				HealthCheckHttpPath: appSubmitReq.HealthCheckConfig.Path,
				HealthCheckExec:     appSubmitReq.HealthCheckConfig.HealthCheckExec,
				EdgeSites:           appSubmitReq.Sites,
				LimitCpu:            appSubmitReq.Storage.CpuLimit,
				RequestCpu:          appSubmitReq.Storage.CpuRequest,
				LimitMem:            appSubmitReq.Storage.MemoryLimit,
				RequestMem:          appSubmitReq.Storage.MemoryRequest,
				PortMaps:            convert2PortMap(appSubmitReq),
			}
			break
		default:
			return fmt.Errorf("no this deploy type %s", appSubmitReq.DeployResource)
		}

		if err = c.ctxBundle.Bdl.CreateEdgeApp(createRequest, identity); err != nil {
			return fmt.Errorf("create edge app error: %v", err)
		}
	}

	c.component.State["addAppDrawerVisible"] = false

	return nil
}

func (c *ComponentFormModal) OperateChangeCluster(orgID int64, identity apistructs.Identity) error {
	var (
		changeReq ChangeClusterReqForm
		formModal apistructs.EdgeFormModalPointProps
	)

	if _, ok := c.component.State["formData"]; !ok {
		return fmt.Errorf("must provide formdata")
	}

	jsonData, err := json.Marshal(c.component.State["formData"])
	if err != nil {
		return fmt.Errorf("marshal application create formdata error: %v", err)
	}

	if err = json.Unmarshal(jsonData, &changeReq); err != nil {
		return err
	}

	clusterInfo, err := c.ctxBundle.Bdl.GetCluster(changeReq.ClusterName)
	if err != nil {
		return fmt.Errorf("get cluster info error: %v", err)
	}

	sites, err := c.ctxBundle.Bdl.ListEdgeSelectSite(orgID, int64(clusterInfo.ID), apistructs.EdgeListValueTypeName, identity)
	if err != nil {
		return err
	}

	configSets, err := c.ctxBundle.Bdl.ListEdgeSelectConfigSet(orgID, int64(clusterInfo.ID), apistructs.EdgeListValueTypeName, identity)
	if err != nil {
		return err
	}

	edgeApps, err := c.ctxBundle.Bdl.ListEdgeSelectApps(orgID, int64(clusterInfo.ID), "", apistructs.EdgeListValueTypeName, identity)
	if err != nil {
		return err
	}

	jsonData, err = json.Marshal(c.component.Props)
	if err != nil {
		return err
	}

	if err = json.Unmarshal(jsonData, &formModal); err != nil {
		return err
	}

	for _, field := range formModal.Fields {
		if field.Key == "sites" {
			field.ComponentProps["options"] = sites
		}
		if field.Key == "configSets" {
			field.ComponentProps["options"] = configSets
		}
		if field.Key == "depends" {
			field.ComponentProps["options"] = edgeApps
		}
	}

	c.component.Props = formModal

	c.component.State["addAppDrawerVisible"] = true

	if formData, ok := c.component.State["formData"].(map[string]interface{}); ok {
		delete(formData, "sites")
		delete(formData, "configSets")
		delete(formData, "depends")
	}

	return nil
}

func (c *ComponentFormModal) OperateRendering(orgID int64, identity apistructs.Identity) error {
	var (
		appState  = apistructs.EdgeAppState{}
		formModal apistructs.EdgeFormModalPointProps
	)
	i18nLocale := c.ctxBundle.Bdl.GetLocale(c.ctxBundle.Locale)
	jsonData, err := json.Marshal(c.component.State)
	if err != nil {
		return fmt.Errorf("marshal component state error: %v", err)
	}

	err = json.Unmarshal(jsonData, &appState)
	if err != nil {
		return fmt.Errorf("unmarshal state json data error: %v", err)
	}

	if appState.FormClear {
		c.component.State["formData"] = make(map[string]interface{})

		edgeClusters, err := c.ctxBundle.Bdl.ListEdgeCluster(uint64(orgID), apistructs.EdgeListValueTypeName, identity)
		if err != nil {
			return fmt.Errorf("get avaliable edge clusters error: %v", err)
		}

		c.component.Props = getProps(edgeClusters, nil, nil, nil, appState.OperationType, "", i18nLocale)
		return nil
	}

	if appState.AppID != 0 {
		appObj, err := c.ctxBundle.Bdl.GetEdgeApp(appState.AppID, identity)
		if err != nil {
			return fmt.Errorf("get application error: %v", err)
		}
		clusterInfo, err := c.ctxBundle.Bdl.GetCluster(strconv.Itoa(int(appObj.ClusterID)))
		if err != nil {
			return fmt.Errorf("get cluster %d error: %v", appObj.ClusterID, err)
		}

		sites, err := c.ctxBundle.Bdl.ListEdgeSelectSite(orgID, appObj.ClusterID, apistructs.EdgeListValueTypeName, identity)
		if err != nil {
			return fmt.Errorf("list available sties error: %v", err)
		}

		jsonData, err = json.Marshal(c.component.Props)
		if err != nil {
			return err
		}

		if err = json.Unmarshal(jsonData, &formModal); err != nil {
			return err
		}

		if appObj.Type == IMAGE {
			formData := Application{
				ID:             appObj.ID,
				AppName:        appObj.Name,
				DeployResource: ConvertDeployResource(appObj.Type),
				Cluster:        clusterInfo.Name,
				Sites:          appObj.EdgeSites,
				ConfigSet:      appObj.ConfigSetName,
				Replicaset:     appObj.Replicas,
				Depends:        appObj.DependApp,
				Storage: ApplicationStorage{
					CpuRequest:    appObj.RequestCpu,
					CpuLimit:      appObj.LimitCpu,
					MemoryRequest: appObj.RequestMem,
					MemoryLimit:   appObj.LimitMem,
				},
				Mirror: ApplicationMirror{
					MirrorAddress:    appObj.Image,
					RegistryUser:     appObj.RegistryUser,
					RegistryPassword: appObj.RegistryPassword,
				},
				HealthCheckConfig: ApplicationHealthCheckConfig{
					HealthCheckType: appObj.HealthCheckType,
					Path:            appObj.HealthCheckHttpPath,
					Port:            appObj.HealthCheckHttpPort,
					HealthCheckExec: appObj.HealthCheckExec,
				},
			}

			formPortMaps := make([]ApplicationPortMapData, 0)
			for _, portMap := range appObj.PortMaps {
				formPortMaps = append(formPortMaps, ApplicationPortMapData{
					Protocol:  deConvert2Protocol(portMap.Protocol),
					Container: portMap.ContainerPort,
					Service:   portMap.ServicePort,
				})
			}

			formData.PortMap.Data = formPortMaps

			c.component.State["formData"] = formData

			configSets, err := c.ctxBundle.Bdl.ListEdgeSelectConfigSet(orgID, appObj.ClusterID, apistructs.EdgeListValueTypeName, identity)
			if err != nil {
				return fmt.Errorf("list avaliable configset error: %v", err)
			}

			apps, err := c.ctxBundle.Bdl.ListEdgeSelectApps(orgID, appObj.ClusterID, appObj.Name, apistructs.EdgeListValueTypeName, identity)
			if err != nil {
				return fmt.Errorf("list avaliable apps error: %v", err)
			}

			c.component.Props = getProps(nil, sites, configSets, apps, appState.OperationType, "", i18nLocale)

		} else if appObj.Type == ADDON {
			formData := Application{
				ID:             appObj.ID,
				AppName:        appObj.Name,
				DeployResource: ConvertDeployResource(appObj.Type),
				MiddlewareType: appObj.AddonName,
				Cluster:        clusterInfo.Name,
				Sites:          appObj.EdgeSites,
			}

			c.component.State["formData"] = formData
			c.component.Props = getProps(nil, sites, nil, nil, appState.OperationType, MIDDLEWARE, i18nLocale)
		}
	}

	c.component.State["addAppDrawerVisible"] = true

	return nil
}

func ConvertDeployResource(typeInDB string) string {
	if typeInDB == IMAGE {
		return MIRROR
	} else {
		return MIDDLEWARE
	}
}

func deConvertDeployResource(typeRequest string) string {
	if typeRequest == MIRROR {
		return IMAGE
	} else {
		return ADDON
	}
}

func convert2Protocol(request string) string {
	if request == "k1" {
		return "TCP"
	} else {
		return ""
	}
}

func deConvert2Protocol(request string) string {
	// Tmp support tcp.
	return "k1"
}

func reverseRegistryAddr(image string) string {
	res := strings.Split(image, "/")
	if len(res) < 2 {
		return ""
	}
	if strings.Contains(res[0], ".") {
		return res[0]
	}
	return ""
}

func convert2PortMap(request Application) []apistructs.PortMap {
	portMaps := make([]apistructs.PortMap, 0)
	for _, item := range request.PortMap.Data {
		portMaps = append(portMaps, apistructs.PortMap{
			Protocol:      convert2Protocol(item.Protocol),
			ContainerPort: item.Container,
			ServicePort:   item.Service,
		})
	}
	return portMaps
}

func validateSubmitData(reqData Application, lr *i18r.LocaleResource) error {
	if err := strutil.Validate(reqData.AppName, strutil.MaxRuneCountValidator(ApplicationNameLength)); err != nil {
		return err
	}

	isRight, err := regexp.MatchString(AppNameMatchPattern, reqData.AppName)
	if err != nil {
		return err
	}

	if !isRight {
		return fmt.Errorf(lr.Get(i18n.I18nKeyInputApplicationNameTip))
	}

	if reqData.DeployResource == MIRROR {
		if err = strutil.Validate(reqData.Mirror.MirrorAddress, strutil.MaxRuneCountValidator(apistructs.EdgeDefaultValueMaxLength)); err != nil {
			return err
		}
		if err = strutil.Validate(reqData.Mirror.RegistryUser, strutil.MaxRuneCountValidator(apistructs.EdgeDefaultNameMaxLength)); err != nil {
			return err
		}
		if err = strutil.Validate(reqData.Mirror.RegistryPassword, strutil.MaxRuneCountValidator(apistructs.EdgeDefaultNameMaxLength)); err != nil {
			return err
		}
		if err = strutil.Validate(reqData.HealthCheckConfig.Path, strutil.MaxRuneCountValidator(apistructs.EdgeDefaultValueMaxLength)); err != nil {
			return err
		}

		if reqData.Storage.MemoryRequest > reqData.Storage.MemoryLimit {
			return fmt.Errorf("memory resource request can't larger than limit")
		}

		if reqData.Storage.CpuRequest > reqData.Storage.CpuLimit {
			return fmt.Errorf("cpu resource request can't larger than limit")
		}
	}

	return nil
}

func RenderCreator() protocol.CompRender {
	return &ComponentFormModal{}
}
