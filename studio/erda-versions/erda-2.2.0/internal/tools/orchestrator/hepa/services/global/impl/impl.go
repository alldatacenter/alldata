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

package impl

import (
	"context"
	"crypto/md5" // #nosec G501
	"encoding/hex"
	"fmt"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/pkg/errors"
	log "github.com/sirupsen/logrus"
	"google.golang.org/grpc/metadata"

	"github.com/erda-project/erda-infra/pkg/transport"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda-proto-go/msp/tenant/pb"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/bundle"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/util"
	. "github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/vars"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/config"
	gw "github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/kong"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
	db "github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/service"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/endpoint_api"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/global"
	"github.com/erda-project/erda/pkg/http/httputil"
)

type GatewayGlobalServiceImpl struct {
	azDb       db.GatewayAzInfoService
	kongDb     db.GatewayKongInfoService
	packageBiz *endpoint_api.GatewayOpenapiService
	reqCtx     context.Context
	clusterSvc clusterpb.ClusterServiceServer
	tenantSvc  pb.TenantServiceServer
}

var diceHealth *gw.DiceHealthDto = &gw.DiceHealthDto{Status: gw.DiceHealthOK}

var once sync.Once

func NewGatewayGlobalServiceImpl(clusterSvc clusterpb.ClusterServiceServer, tenantSvc pb.TenantServiceServer) (e error) {
	once.Do(
		func() {
			azDb, err := db.NewGatewayAzInfoServiceImpl()
			if err != nil {
				e = err
				return
			}
			kongDb, err := db.NewGatewayKongInfoServiceImpl()
			if err != nil {
				e = err
				return
			}
			impl := GatewayGlobalServiceImpl{
				azDb:       azDb,
				packageBiz: &endpoint_api.Service,
				kongDb:     kongDb,
				clusterSvc: clusterSvc,
				tenantSvc:  tenantSvc,
			}
			global.Service = &impl
			go func() {
				defer func() {
					util.DoRecover()
					diceHealth.Status = gw.DiceHealthFail
					diceHealth.Modules = []gw.DiceHealthModule{
						{
							Name:    "panic-happened",
							Status:  gw.DiceHealthFail,
							Message: "panic happened",
						},
					}
				}()
				for range time.Tick(time.Second * 60) {
					newHealthInfo := impl.checkKongHealth()
					diceHealth = &newHealthInfo
				}
			}()
		})
	return
}

func (impl *GatewayGlobalServiceImpl) checkKongHealth() (dto gw.DiceHealthDto) {
	var err error
	dto.Status = gw.DiceHealthOK
	defer func() {
		if err != nil {
			dto.Status = gw.DiceHealthFail
			dto.Modules = append(dto.Modules, gw.DiceHealthModule{
				Name:    "error-happened",
				Status:  gw.DiceHealthFail,
				Message: errors.Cause(err).Error(),
			})
			log.Errorf("error happened:%+v", err)
		}
	}()
	azs, err := impl.azDb.SelectValidAz()
	if err != nil {
		return
	}
	for _, az := range azs {
		data, err := bundle.Bundle.QueryClusterInfo(az.Az)
		if err != nil || data.IsDCOS() {
			continue
		}
		kongInfo, err := impl.kongDb.GetKongInfo(&orm.GatewayKongInfo{
			Az: az.Az,
		})
		if err != nil || kongInfo.KongAddr == "" || kongInfo.InnerAddr == "" {
			continue
		}
		adapter := kong.NewKongAdapter(kongInfo.KongAddr)
		_, err = adapter.GetRoutes()
		if err != nil {
			dto.Status = gw.DiceHealthFail
			dto.Modules = append(dto.Modules, gw.DiceHealthModule{
				Name:    fmt.Sprintf("cluster-gateway-control-%s", az.Az),
				Status:  gw.DiceHealthFail,
				Message: errors.Cause(err).Error(),
			})
			log.Errorf("error happened:%+v", err)
		}
		// proxyAddr := kongInfo.InnerAddr
		// if !strings.HasPrefix(proxyAddr, "inet") && strings.HasPrefix(kongInfo.KongAddr, "inet") {
		// 	addrsplit := strings.Split(kongInfo.KongAddr, "/")
		// 	inetPrefix := strings.Join(addrsplit[:len(addrsplit)-1], "/")
		// 	proxyAddr = strings.Replace(proxyAddr, "http:/", inetPrefix, 1)
		// 	if !strings.HasPrefix(proxyAddr, "inet") {
		// 		proxyAddr = fmt.Sprintf("%s/%s", inetPrefix, proxyAddr)
		// 	}
		// }
		// err = checkKongProxyHealth(proxyAddr, &dto, routes)
		// if err != nil {
		// 	dto.Status = gw.DiceHealthFail
		// 	dto.Modules = append(dto.Modules, gw.DiceHealthModule{
		// 		Name:    fmt.Sprintf("cluster-gateway-proxy-%s", az.Az),
		// 		Status:  gw.DiceHealthFail,
		// 		Message: errors.Cause(err).Error(),
		// 	})
		// 	log.Errorf("error happened:%+v", err)
		// }
	}
	return
}

func (impl GatewayGlobalServiceImpl) Clone(ctx context.Context) global.GatewayGlobalService {
	newService := impl
	newService.reqCtx = ctx
	return &newService
}

func (impl *GatewayGlobalServiceImpl) GetDiceHealth() gw.DiceHealthDto {
	return *diceHealth
}

func (impl *GatewayGlobalServiceImpl) GetOrgId(projectId string) (string, error) {
	projectIdi, err := strconv.ParseUint(projectId, 10, 64)
	if err != nil {
		return "", err
	}
	projectDto, err := bundle.Bundle.GetProject(projectIdi)
	if err != nil {
		return "", err
	}
	orgId := projectDto.OrgID
	if orgId == 0 {
		return "", errors.Errorf("invalid project: %+v", projectDto)
	}
	return strconv.FormatUint(orgId, 10), nil
}

func (impl *GatewayGlobalServiceImpl) GetClusterUIType(orgId, projectId, env string) *common.StandardResult {
	res := &common.StandardResult{Success: false}
	if projectId == "" || env == "" {
		log.Errorf("clusterName is empty")
		return res.SetReturnCode(PARAMS_IS_NULL)
	}
	az, err := impl.azDb.GetAzInfo(&orm.GatewayAzInfo{
		Env:       env,
		ProjectId: projectId,
	})
	if err != nil {
		log.Errorf("get az failed, err:%+v", err)
		return res
	}
	uiType := gw.UI_NORMAL
	if az.Type == orm.AT_K8S || az.Type == orm.AT_EDAS {
		uiType = gw.UI_K8S
	}
	if config.ServerConf.ClusterUIType != "" {
		uiType = config.ServerConf.ClusterUIType
	}
	return res.SetSuccessAndData(uiType)
}

func (impl *GatewayGlobalServiceImpl) GenerateEndpoint(info gw.DiceInfo, session ...*db.SessionHelper) (string, string, error) {
	var kongService db.GatewayKongInfoService
	var err error
	if len(session) > 0 {
		kongService, err = impl.kongDb.NewSession(session[0])
		if err != nil {
			return "", "", err
		}
	} else {
		kongService = impl.kongDb
	}
	if info.Az == "" || info.ProjectId == "" || info.Env == "" {
		return "", "", errors.Errorf("invalid diceinfo:%+v", info)
	}
	kongInfo, err := kongService.GetKongInfo(&orm.GatewayKongInfo{
		Az:        info.Az,
		ProjectId: info.ProjectId,
		Env:       info.Env,
	})
	if err != nil {
		return "", "", err
	}
	endpoint := kongInfo.Endpoint
	inner := kong.InnerHost
	if !strings.EqualFold(info.Env, ENV_TYPE_PROD) {
		endpoint = strings.ToLower(info.Env + config.ServerConf.SubDomainSplit + endpoint)
		inner = strings.ToLower(info.Env + "." + inner)
	}
	return endpoint, inner, nil
}

func (impl *GatewayGlobalServiceImpl) GetServiceAddr(env string) string {
	addr := "api-gateway.kube-system.svc.cluster.local"
	if !strings.EqualFold(env, ENV_TYPE_PROD) {
		addr = strings.ToLower(env + "-" + addr)
	}
	return addr
}

func (impl *GatewayGlobalServiceImpl) GenerateDefaultPath(projectId string, session ...*db.SessionHelper) (string, error) {
	if projectId == "" {
		return "", errors.New("empty projectId")
	}
	projectName, err := impl.GetProjectName(gw.DiceInfo{ProjectId: projectId}, session...)
	if err != nil {
		return "", err
	}
	return strings.ToLower(fmt.Sprintf("/%s", projectName)), nil
}

func (impl *GatewayGlobalServiceImpl) GetProjectNameFromCmdb(projectId string) (string, error) {
	idNum, err := strconv.ParseUint(projectId, 10, 64)
	if err != nil {
		return "", nil
	}
	projectInfo, err := bundle.Bundle.GetProject(idNum)
	if err != nil {
		return "", errors.WithStack(err)
	}
	return projectInfo.Name, nil
}

func (impl *GatewayGlobalServiceImpl) GetClustersByOrg(orgId string) ([]string, error) {
	idNum, err := strconv.ParseUint(orgId, 10, 64)
	if err != nil {
		return nil, nil
	}
	ctx := transport.WithHeader(context.Background(), metadata.New(map[string]string{httputil.InternalHeader: "hepa"}))
	resp, err := impl.clusterSvc.ListCluster(ctx, &clusterpb.ListClusterRequest{OrgID: uint32(idNum)})
	if err != nil {
		return nil, errors.WithStack(err)
	}
	clusters := resp.Data
	var names []string
	for _, cluster := range clusters {
		names = append(names, cluster.Name)
	}
	return names, nil
}

func (impl *GatewayGlobalServiceImpl) GetProjectName(info gw.DiceInfo, session ...*db.SessionHelper) (string, error) {
	var kongService db.GatewayKongInfoService
	var err error
	if len(session) > 0 {
		kongService, err = impl.kongDb.NewSession(session[0])
		if err != nil {
			return "", err
		}
	} else {
		kongService = impl.kongDb
	}
	if info.ProjectId == "" {
		return "", errors.Errorf("invalid diceinfo:%+v", info)
	}
	kongInfo, err := kongService.GetKongInfo(&orm.GatewayKongInfo{
		ProjectId: info.ProjectId,
	})
	if err != nil {
		return "", err
	}
	return kongInfo.ProjectName, nil
}

// md5V md5加密
func md5V(str string) string {
	h := md5.New() // #nosec G401
	_, _ = h.Write([]byte(str))
	return hex.EncodeToString(h.Sum(nil))
}

func encodeTenantGroup(projectId, env, clusterName, tenantGroupKey string) string {
	return md5V(projectId + "_" + strings.ToUpper(env) + "_" + clusterName + tenantGroupKey)
}

func (impl *GatewayGlobalServiceImpl) GenTenantGroup(projectId, env, clusterName string) (string, error) {
	tenantGroup := encodeTenantGroup(projectId, env, clusterName, config.ServerConf.TenantGroupKey)
	resp, err := impl.tenantSvc.CreateTenant(context.Background(), &pb.CreateTenantRequest{
		ProjectID:  projectId,
		TenantType: pb.Type_DOP.String(),
		Workspaces: []string{env},
	})
	if err != nil {
		log.Errorf("error happened: %+v", err)
		return "", err
	}
	if len(resp.Data) <= 0 {
		return tenantGroup, nil
	}
	return resp.Data[0].Id, nil
}

func (impl *GatewayGlobalServiceImpl) GetTenantGroup(projectId, env string) (res string, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened: %+v", err)
		}
	}()
	info, err := impl.kongDb.GetByAny(&orm.GatewayKongInfo{
		ProjectId: projectId,
		Env:       env,
	})
	if err != nil {
		return
	}
	if info == nil {
		err = errors.New("tenant not found")
		return
	}
	tenantGroup := info.TenantGroup
	if tenantGroup == "" {
		tenantGroup, err = impl.GenTenantGroup(projectId, env, info.Az)
		if err != nil {
			return
		}
	}
	res = tenantGroup
	return
}

func (impl *GatewayGlobalServiceImpl) CreateTenant(tenant *gw.TenantDto) (result bool, err error) {
	var session *db.SessionHelper
	var kongSession db.GatewayKongInfoService
	var az *orm.GatewayAzInfo
	defer func() {
		if err != nil {
			log.Errorf("error happened, err:%+v", err)
			if session != nil {
				_ = session.Rollback()
				session.Close()
			}
		}
	}()
	exist, err := impl.kongDb.GetByAny(&orm.GatewayKongInfo{
		TenantId: tenant.Id,
	})
	if err != nil {
		return
	}
	if exist != nil {
		log.Infof("tenant already exists, tenant:%+v", tenant)
		result = true
		return
	}
	session, err = db.NewSessionHelper()
	if err != nil {
		return
	}
	kongSession, err = impl.kongDb.NewSession(session)
	if err != nil {
		return
	}
	exist, err = kongSession.GetForUpdate(tenant.ProjectId, tenant.Env, tenant.Az)
	if err != nil {
		return
	}
	if exist != nil {
		err = kongSession.Update(&orm.GatewayKongInfo{
			BaseRow: orm.BaseRow{
				Id: exist.Id,
			},
			TenantId:        tenant.Id,
			TenantGroup:     tenant.TenantGroup,
			Az:              tenant.Az,
			Env:             tenant.Env,
			ProjectId:       tenant.ProjectId,
			ProjectName:     tenant.ProjectName,
			KongAddr:        tenant.AdminAddr,
			Endpoint:        tenant.GatewayEndpoint,
			InnerAddr:       tenant.InnerAddr,
			ServiceName:     tenant.ServiceName,
			AddonInstanceId: tenant.InstanceId,
		})
		if err != nil {
			return
		}
	} else {
		err = kongSession.Insert(&orm.GatewayKongInfo{
			TenantId:        tenant.Id,
			TenantGroup:     tenant.TenantGroup,
			Az:              tenant.Az,
			Env:             tenant.Env,
			ProjectId:       tenant.ProjectId,
			ProjectName:     tenant.ProjectName,
			KongAddr:        tenant.AdminAddr,
			Endpoint:        tenant.GatewayEndpoint,
			InnerAddr:       tenant.InnerAddr,
			ServiceName:     tenant.ServiceName,
			AddonInstanceId: tenant.InstanceId,
		})
		if err != nil {
			return
		}
		az, err = impl.azDb.GetAzInfoByClusterName(tenant.Az)
		if err != nil {
			return
		}
		if az == nil {
			err = errors.Errorf("get az failed, tenant:%+v", tenant)
			return
		}
		if (az.Type == orm.AT_K8S || az.Type == orm.AT_EDAS) && !config.ServerConf.UseAdminEndpoint {
			err = (*impl.packageBiz).CreateTenantPackage(tenant.Id, session)
			if err != nil {
				return
			}
		}
	}
	err = session.Commit()
	if err != nil {
		return
	}
	session.Close()
	result = true
	return
}

func (impl GatewayGlobalServiceImpl) GetRuntimeServicePrefix(dao *orm.GatewayRuntimeService) (string, error) {
	return "/" + dao.Id, nil
}

func (impl GatewayGlobalServiceImpl) GetGatewayFeatures(clusterName string) map[string]string {
	return map[string]string{
		"runtime-register": "on",
	}
}
