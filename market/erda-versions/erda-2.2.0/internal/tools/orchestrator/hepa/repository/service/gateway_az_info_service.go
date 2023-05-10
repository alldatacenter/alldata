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

package service

import (
	"encoding/json"
	"strings"
	"time"

	"github.com/pkg/errors"
	log "github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/bundle"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/util"
	. "github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/vars"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
	"github.com/erda-project/erda/pkg/discover"
)

type AdminProjectDto struct {
	Name          string            `json:"name"`
	ClusterConfig map[string]string `json:"clusterConfig"`
}

type AdminRespDto struct {
	Success bool            `json:"success"`
	Data    AdminProjectDto `json:"data"`
}

const (
	CT_K8S  = "kubernetes"
	CT_DCOS = "dcos"
	CT_EDAS = "edas"
)

type ClusterRespDto struct {
	Success bool           `json:"success"`
	Data    ClusterInfoDto `json:"data"`
}

type ClusterInfoDto struct {
	DiceClusterType string `json:"DICE_CLUSTER_TYPE"`
	DiceRootDomain  string `json:"DICE_ROOT_DOMAIN"`
	MasterAddr      string `json:"MASTER_VIP_ADDR"`
	NetportalUrl    string `json:"NETPORTAL_URL"`
}

type GatewayAzInfoServiceImpl struct {
	engine *orm.OrmEngine
}

func NewGatewayAzInfoServiceImpl() (*GatewayAzInfoServiceImpl, error) {
	engine, error := orm.GetSingleton()
	if error != nil {
		return nil, errors.Wrap(error, "new GatewayAzInfoServiceImpl failed")
	}
	return &GatewayAzInfoServiceImpl{engine}, nil
}

func (impl *GatewayAzInfoServiceImpl) GetAz(cond *orm.GatewayAzInfo) (string, error) {
	azInfo, err := impl.GetAzInfo(cond)
	if err != nil {
		return "", err
	}
	if azInfo == nil {
		return "", errors.Errorf("cluster not found in :%+v", cond)
	}
	return azInfo.Az, nil
}

func (impl *GatewayAzInfoServiceImpl) SelectByAny(cond *orm.GatewayAzInfo) ([]orm.GatewayAzInfo, error) {
	var result []orm.GatewayAzInfo
	if cond == nil {
		return result, errors.New(ERR_INVALID_ARG)
	}
	err := orm.SelectByAny(impl.engine, &result, cond)
	if err != nil {
		return result, errors.Wrap(err, ERR_SQL_FAIL)
	}
	return result, nil
}

func (impl *GatewayAzInfoServiceImpl) SelectValidAz() ([]orm.GatewayAzInfo, error) {
	var result []orm.GatewayAzInfo
	err := orm.Select(impl.engine.Distinct("az"), &result, `master_addr != ""`)
	if err != nil {
		return result, errors.Wrap(err, ERR_SQL_FAIL)
	}
	return result, nil
}

func fillInfo(info *orm.GatewayAzInfo, clusterInfo *ClusterInfoDto) {
	clusterType := clusterInfo.DiceClusterType
	switch clusterType {
	case CT_K8S:
		info.Type = orm.AT_K8S
	case CT_DCOS:
		info.Type = orm.AT_DCOS
	case CT_EDAS:
		info.Type = orm.AT_EDAS
	default:
		info.Type = orm.AT_UNKNOWN
	}
	info.WildcardDomain = clusterInfo.DiceRootDomain
	if clusterInfo.MasterAddr == "" {
		info.MasterAddr = ""
	}
	if clusterInfo.NetportalUrl == "" {
		info.MasterAddr = clusterInfo.MasterAddr
	} else {
		info.MasterAddr = clusterInfo.NetportalUrl + "/" + clusterInfo.MasterAddr
	}
}

func (impl *GatewayAzInfoServiceImpl) GetAzInfoByClusterName(name string) (*orm.GatewayAzInfo, error) {
	info := &orm.GatewayAzInfo{
		Az: name,
	}
	cluster, err := bundle.Bundle.GetCluster(name)
	if err != nil {
		return nil, err
	}
	clusterResp := &ClusterInfoDto{}
	cm, err := json.Marshal(cluster.CM)
	if err != nil {
		return nil, err
	}
	err = json.Unmarshal(cm, clusterResp)
	if err != nil {
		return nil, err
	}
	fillInfo(info, clusterResp)
	return info, nil
}

func (impl *GatewayAzInfoServiceImpl) GetAzInfo(cond *orm.GatewayAzInfo) (*orm.GatewayAzInfo, error) {
	if cond == nil || cond.ProjectId == "" || cond.Env == "" {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	info := &orm.GatewayAzInfo{}
	exist, err := orm.GetByAny(impl.engine, info, cond)
	if err != nil {
		return nil, errors.WithStack(err)
	}
	now := time.Now()
	if exist && info.Type != "" && (info.NeedUpdate == 0 || (now.Sub(info.UpdateTime).Seconds() < 60 &&
		now.Sub(info.UpdateTime).Seconds() > 0)) {
		return info, nil
	}
	code, body, err := util.CommonRequest("GET", discover.CoreServices()+"/api/projects/"+cond.ProjectId, nil,
		map[string]string{"Internal-Client": "hepa-gateway"})
	if err != nil {
		err = errors.WithMessage(err, "request dice admin failed")
		goto failback
	}
	if code < 300 {
		data := &AdminRespDto{}
		err = json.Unmarshal(body, data)
		if err != nil {
			err = errors.Wrapf(err, "unmarshal failed:%s", body)
			goto failback
		}
		if !data.Success {
			err = errors.Errorf("request dice admin failed: resp[%s]", body)
			goto failback
		}
		az, ok := data.Data.ClusterConfig[strings.ToUpper(cond.Env)]
		if !ok {
			err = errors.Errorf("can't find az of info[%+v] in admin resp[%s]", cond, body)
			goto failback
		}
		cluster, err := bundle.Bundle.GetCluster(az)
		if err != nil {
			return nil, err
		}
		clusterResp := &ClusterInfoDto{}
		cm, err := json.Marshal(cluster.CM)
		if err != nil {
			return nil, err
		}
		err = json.Unmarshal(cm, clusterResp)
		if err != nil {
			return nil, err
		}
		fillInfo(info, clusterResp)
		info.Az = az
		if exist {
			_, _ = orm.Update(impl.engine, info, "az", "wildcard_domain", "type", "master_addr")
		} else {
			cond.NeedUpdate = 1
			cond.Az = az
			_, _ = orm.Insert(impl.engine, cond)
		}
		return info, nil
	}

failback:
	if exist {
		log.Error(err)
		return info, nil
	}
	return nil, err
}
