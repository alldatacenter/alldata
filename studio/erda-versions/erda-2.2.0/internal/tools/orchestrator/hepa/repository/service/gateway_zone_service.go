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

type GatewayZoneServiceImpl struct {
	engine *orm.OrmEngine
	*SessionHelper
	executor xorm.Interface
}

func NewGatewayZoneServiceImpl() (*GatewayZoneServiceImpl, error) {
	engine, error := orm.GetSingleton()
	if error != nil {
		return nil, errors.Wrap(error, "new GatewayZoneServiceImpl failed")
	}
	return &GatewayZoneServiceImpl{
		engine:   engine,
		executor: engine,
	}, nil
}

func (impl *GatewayZoneServiceImpl) NewSession(helper ...*SessionHelper) (GatewayZoneService, error) {
	var session *SessionHelper
	var err error
	if len(helper) == 0 {
		session, err = NewSessionHelper()
		if err != nil {
			return nil, err
		}
	} else if helper[0] == nil {
		return &GatewayZoneServiceImpl{
			engine:   impl.engine,
			executor: impl.engine,
		}, nil

	} else {
		session = helper[0]
	}
	return &GatewayZoneServiceImpl{
		engine:        impl.engine,
		executor:      session.session,
		SessionHelper: session,
	}, nil
}

func (impl *GatewayZoneServiceImpl) Update(zone *orm.GatewayZone) error {
	if zone == nil {
		return errors.New(ERR_INVALID_ARG)
	}
	_, err := orm.Update(impl.executor, zone)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil
}

func (impl *GatewayZoneServiceImpl) Insert(zone *orm.GatewayZone) error {
	if zone == nil {
		return errors.New(ERR_INVALID_ARG)
	}
	_, err := orm.Insert(impl.executor, zone)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil
}

func (impl *GatewayZoneServiceImpl) GetByAny(cond *orm.GatewayZone) (*orm.GatewayZone, error) {
	if cond == nil {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	zone := &orm.GatewayZone{}
	bCond, err := orm.BuildConds(impl.engine, cond, cond.GetMustCondCols())
	if err != nil {
		return nil, errors.Wrap(err, "buildConds failed")
	}
	succ, err := orm.GetByAnyI(impl.executor, bCond, zone)
	if err != nil {
		return nil, errors.Wrap(err, ERR_SQL_FAIL)
	}
	if !succ {
		return nil, nil
	}
	return zone, nil
}

func (impl *GatewayZoneServiceImpl) SelectPolicyZones(clusterName string) ([]orm.GatewayZone, error) {
	var result []orm.GatewayZone
	if clusterName == "" {
		return result, errors.New(ERR_INVALID_ARG)
	}
	err := orm.Select(impl.executor, &result, "dice_cluster_name = ? and kong_policies is not null and kong_policies != ''", clusterName)
	if err != nil {
		return result, errors.Wrap(err, ERR_SQL_FAIL)
	}
	return result, nil
}

func (impl *GatewayZoneServiceImpl) SelectByAny(cond *orm.GatewayZone) ([]orm.GatewayZone, error) {
	var result []orm.GatewayZone
	if cond == nil {
		return result, errors.New(ERR_INVALID_ARG)
	}
	bCond, err := orm.BuildConds(impl.engine, cond, cond.GetMustCondCols())
	if err != nil {
		return result, errors.Wrap(err, ERR_SQL_FAIL)
	}
	err = orm.SelectByAnyI(impl.executor, bCond, &result)
	if err != nil {
		return result, errors.Wrap(err, ERR_SQL_FAIL)
	}
	return result, nil
}

func (impl *GatewayZoneServiceImpl) GetById(id string) (*orm.GatewayZone, error) {
	if len(id) == 0 {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	zone := &orm.GatewayZone{}
	succ, err := orm.Get(impl.executor, zone, "id = ?", id)
	if err != nil {
		return nil, errors.Wrap(err, ERR_SQL_FAIL)
	}
	if !succ {
		return nil, nil
	}
	return zone, nil
}

func (impl *GatewayZoneServiceImpl) DeleteById(id string) error {
	if len(id) == 0 {
		return errors.New(ERR_INVALID_ARG)
	}
	_, err := orm.Delete(impl.executor, &orm.GatewayZone{}, "id = ?", id)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil
}
