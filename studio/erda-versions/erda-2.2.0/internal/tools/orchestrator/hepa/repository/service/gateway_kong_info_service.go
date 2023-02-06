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
	"fmt"
	"os"
	"strings"

	"github.com/pkg/errors"
	"github.com/xormplus/xorm"

	. "github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/vars"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/config"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
)

type GatewayKongInfoServiceImpl struct {
	engine *orm.OrmEngine
	*SessionHelper
	executor xorm.Interface
}

func NewGatewayKongInfoServiceImpl() (*GatewayKongInfoServiceImpl, error) {
	engine, error := orm.GetSingleton()
	if error != nil {
		return nil, errors.Wrap(error, "new GatewayKongInfoServiceImpl failed")
	}
	return &GatewayKongInfoServiceImpl{
		engine:   engine,
		executor: engine,
	}, nil
}

func (impl *GatewayKongInfoServiceImpl) NewSession(helper ...*SessionHelper) (GatewayKongInfoService, error) {
	var session *SessionHelper
	var err error
	if len(helper) == 0 {
		session, err = NewSessionHelper()
		if err != nil {
			return nil, err
		}
	} else if helper[0] == nil {
		return &GatewayKongInfoServiceImpl{
			engine:   impl.engine,
			executor: impl.engine,
		}, nil
	} else {
		session = helper[0]
	}
	return &GatewayKongInfoServiceImpl{
		engine:        impl.engine,
		executor:      session.session,
		SessionHelper: session,
	}, nil
}

func (impl *GatewayKongInfoServiceImpl) GetBelongTuples(instanceId string) ([]KongBelongTuple, error) {
	var result []orm.GatewayKongInfo
	err := orm.Select(impl.executor.Distinct("env", "project_id", "az"), &result, "addon_instance_id = ?", instanceId)
	if err != nil {
		return nil, errors.Wrap(err, ERR_SQL_FAIL)
	}
	var res []KongBelongTuple
	for _, info := range result {
		res = append(res, KongBelongTuple{
			Env:       info.Env,
			ProjectId: info.ProjectId,
			Az:        info.Az,
		})
	}
	return res, nil
}

func (impl *GatewayKongInfoServiceImpl) GenK8SInfo(kongInfo *orm.GatewayKongInfo) (string, string, error) {
	return fmt.Sprintf("addon-%s--%s", kongInfo.ServiceName, kongInfo.AddonInstanceId), kongInfo.ServiceName, nil
}

func (impl *GatewayKongInfoServiceImpl) GetK8SInfo(cond *orm.GatewayKongInfo) (string, string, error) {
	kongInfo, err := impl.GetKongInfo(cond)
	if err != nil {
		return "", "", err
	}
	return impl.GenK8SInfo(kongInfo)
}

func (impl *GatewayKongInfoServiceImpl) GetKongInfo(cond *orm.GatewayKongInfo) (*orm.GatewayKongInfo, error) {
	if cond == nil {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	kongInfo, err := impl.GetByAny(cond)
	if err != nil {
		return nil, err
	}
	if kongInfo == nil {
		kongInfo, err = impl.GetByAny(&orm.GatewayKongInfo{
			Az: cond.Az,
		})
		if err != nil {
			return nil, err
		}
	}
	if kongInfo == nil {
		return nil, errors.Errorf("get kong info faild, %+v", cond)
	}
	err = impl.adjustKongInfo(kongInfo)
	if err != nil {
		return nil, err
	}
	return kongInfo, nil
}

func (impl *GatewayKongInfoServiceImpl) GetForUpdate(projectId, env, az string) (*orm.GatewayKongInfo, error) {
	if projectId == "" || env == "" || az == "" {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	if impl.session == nil {
		return nil, errors.New("session is nil")
	}
	impl.session.Exec("set innodb_lock_wait_timeout=600")
	dao := &orm.GatewayKongInfo{}
	succ, err := orm.GetForUpdate(impl.session, impl.engine, dao, "project_id = ? and env = ? and az = ?", projectId, env, az)
	if err != nil {
		return nil, errors.WithStack(err)
	}
	if !succ {
		return nil, nil
	}
	return dao, nil
}

func (impl *GatewayKongInfoServiceImpl) GetByAny(cond *orm.GatewayKongInfo) (*orm.GatewayKongInfo, error) {
	if cond == nil {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	dao := &orm.GatewayKongInfo{}
	bCond, err := orm.BuildConds(impl.engine, cond, cond.GetMustCondCols())
	if err != nil {
		return nil, errors.Wrap(err, "buildConds failed")
	}
	succ, err := orm.GetByAnyI(impl.executor, bCond, dao)
	if err != nil {
		return nil, errors.Wrap(err, ERR_SQL_FAIL)
	}
	if !succ {
		return nil, nil
	}
	err = impl.adjustKongInfo(dao)
	if err != nil {
		return nil, err
	}
	return dao, nil
}

func (impl *GatewayKongInfoServiceImpl) GetTenantId(projectId, env, az string) (string, error) {
	kongInfo, err := impl.GetByAny(&orm.GatewayKongInfo{
		ProjectId: projectId,
		Env:       env,
		Az:        az,
	})
	if err != nil {
		return "", err
	}
	if kongInfo == nil {
		return "", errors.Errorf("get kong info failed, projectId:%s, env:%s, az:%s",
			projectId, env, az)
	}
	return kongInfo.TenantId, nil
}

func (impl *GatewayKongInfoServiceImpl) acquireKongAddr(netportalUrl, selfAz string, info *orm.GatewayKongInfo) (string, error) {
	if strings.HasPrefix(info.KongAddr, "inet://") {
		pathSlice := strings.SplitN(strings.TrimPrefix(info.KongAddr, "inet://"), "/", 2)
		if len(pathSlice) != 2 {
			return "", errors.Errorf("invalid addr:%s", info.KongAddr)
		}
		if selfAz == info.Az {
			return pathSlice[1], nil
		}
		return fmt.Sprintf("%s/%s", netportalUrl, pathSlice[1]), nil
	}
	if selfAz != info.Az {
		kongAddr := info.KongAddr
		kongAddr = strings.TrimPrefix(kongAddr, "http://")
		kongAddr = strings.TrimPrefix(kongAddr, "https://")
		return fmt.Sprintf("%s/%s", netportalUrl, kongAddr), nil
	}
	return info.KongAddr, nil
}

func (impl *GatewayKongInfoServiceImpl) adjustKongInfo(info *orm.GatewayKongInfo) error {
	selfAz := os.Getenv("DICE_CLUSTER_NAME")
	netportalUrl := "inet://" + info.Az
	kongAddr, err := impl.acquireKongAddr(netportalUrl, selfAz, info)
	if err != nil {
		return err
	}
	info.KongAddr = kongAddr
	// TODO: Compatibility code, will be removed later
	if config.ServerConf.UseAdminEndpoint && selfAz != info.Az {
		info.KongAddr = "http://" + strings.Replace(info.Endpoint, "gateway", "gateway-admin", 1)
	}
	return nil
}

func (impl *GatewayKongInfoServiceImpl) Update(info *orm.GatewayKongInfo) error {
	if info == nil {
		return errors.New(ERR_INVALID_ARG)
	}
	err := impl.adjustKongInfo(info)
	if err != nil {
		return err
	}
	_, err = orm.Update(impl.executor, info)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil
}

func (impl *GatewayKongInfoServiceImpl) Insert(info *orm.GatewayKongInfo) error {
	if info == nil {
		return errors.New(ERR_INVALID_ARG)
	}
	err := impl.adjustKongInfo(info)
	if err != nil {
		return err
	}
	_, err = orm.Insert(impl.executor, info)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil
}
