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
	"github.com/pkg/errors"
	"github.com/xormplus/xorm"

	. "github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/vars"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
)

type GatewayPluginInstanceServiceImpl struct {
	engine *orm.OrmEngine
	*SessionHelper
	executor xorm.Interface
}

func NewGatewayPluginInstanceServiceImpl() (*GatewayPluginInstanceServiceImpl, error) {
	engine, error := orm.GetSingleton()
	if error != nil {
		return nil, errors.Wrap(error, "new GatewayPluginInstanceServiceImpl failed")
	}
	return &GatewayPluginInstanceServiceImpl{
		engine:   engine,
		executor: engine,
	}, nil
}

func (impl *GatewayPluginInstanceServiceImpl) NewSession(helper ...*SessionHelper) (GatewayPluginInstanceService, error) {
	var session *SessionHelper
	var err error
	if len(helper) == 0 {
		session, err = NewSessionHelper()
		if err != nil {
			return nil, err
		}
	} else if helper[0] == nil {
		return &GatewayPluginInstanceServiceImpl{
			engine:   impl.engine,
			executor: impl.engine,
		}, nil
	} else {
		session = helper[0]
	}
	return &GatewayPluginInstanceServiceImpl{
		engine:        impl.engine,
		executor:      session.session,
		SessionHelper: session,
	}, nil
}

func (impl *GatewayPluginInstanceServiceImpl) Insert(instance *orm.GatewayPluginInstance) error {
	if instance == nil {
		return errors.New(ERR_INVALID_ARG)
	}
	_, err := orm.Insert(impl.executor, instance)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil
}

func (impl *GatewayPluginInstanceServiceImpl) DeleteByRouteId(routeId string) error {
	if len(routeId) == 0 {
		return errors.New(ERR_INVALID_ARG)
	}
	_, err := orm.Delete(impl.executor, &orm.GatewayPluginInstance{},
		"route_id = ?", routeId)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil
}

func (impl *GatewayPluginInstanceServiceImpl) DeleteByApiId(apiId string) error {
	if len(apiId) == 0 {
		return errors.New(ERR_INVALID_ARG)
	}
	_, err := orm.Delete(impl.executor, &orm.GatewayPluginInstance{},
		"api_id = ?", apiId)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil
}

func (impl *GatewayPluginInstanceServiceImpl) DeleteByServiceId(serviceId string) error {
	if len(serviceId) == 0 {
		return errors.New(ERR_INVALID_ARG)
	}
	_, err := orm.Delete(impl.executor, &orm.GatewayPluginInstance{},
		"service_id = ?", serviceId)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil
}

func (impl *GatewayPluginInstanceServiceImpl) DeleteById(id string) error {
	if len(id) == 0 {
		return errors.New(ERR_INVALID_ARG)
	}
	_, err := orm.Delete(impl.executor, &orm.GatewayPluginInstance{},
		"id = ?", id)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil
}

func (impl *GatewayPluginInstanceServiceImpl) DeleteByConsumerId(consumerId string) error {
	if len(consumerId) == 0 {
		return errors.New(ERR_INVALID_ARG)
	}
	_, err := orm.Delete(impl.executor, &orm.GatewayPluginInstance{},
		"consumer_id = ?", consumerId)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil
}

func (impl *GatewayPluginInstanceServiceImpl) Update(instance *orm.GatewayPluginInstance) error {
	if instance == nil {
		return errors.New(ERR_INVALID_ARG)
	}
	_, err := orm.Update(impl.executor, instance)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil
}

func (impl *GatewayPluginInstanceServiceImpl) GetByPluginNameAndApiId(name string, id string) (*orm.GatewayPluginInstance, error) {
	if len(name) == 0 || len(id) == 0 {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	instance := &orm.GatewayPluginInstance{}
	succ, err := orm.Get(impl.executor, instance, "plugin_name = ? and api_id = ?", name, id)
	if err != nil {
		return nil, errors.Wrap(err, ERR_SQL_FAIL)
	}
	if !succ {
		return nil, nil
	}
	return instance, nil
}

func (impl *GatewayPluginInstanceServiceImpl) GetByAny(cond *orm.GatewayPluginInstance) (*orm.GatewayPluginInstance, error) {
	if cond == nil {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	dao := &orm.GatewayPluginInstance{}
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
	return dao, nil
}

func (impl *GatewayPluginInstanceServiceImpl) SelectByOnlyApiId(id string) ([]orm.GatewayPluginInstance, error) {
	var result []orm.GatewayPluginInstance
	if len(id) == 0 {
		return result, errors.New(ERR_INVALID_ARG)
	}
	err := orm.Select(impl.executor.Desc("create_time"), &result, "api_id = ? and (consumer_id is NULL or consumer_id =\"\")", id)
	if err != nil {
		return result, errors.Wrap(err, ERR_SQL_FAIL)
	}
	return result, nil
}

func (impl *GatewayPluginInstanceServiceImpl) SelectByPolicyId(id string) ([]orm.GatewayPluginInstance, error) {
	var result []orm.GatewayPluginInstance
	if len(id) == 0 {
		return result, errors.New(ERR_INVALID_ARG)
	}
	err := orm.Select(impl.executor.Desc("create_time"), &result, "policy_id = ?", id)
	if err != nil {
		return result, errors.Wrap(err, ERR_SQL_FAIL)
	}
	return result, nil
}
