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

type GatewayRouteServiceImpl struct {
	engine *orm.OrmEngine
	*SessionHelper
	executor xorm.Interface
}

func NewGatewayRouteServiceImpl() (*GatewayRouteServiceImpl, error) {
	engine, error := orm.GetSingleton()
	if error != nil {
		return nil, errors.Wrap(error, "new GatewayRouteServiceImpl failed")
	}
	return &GatewayRouteServiceImpl{
		engine:   engine,
		executor: engine,
	}, nil
}

func (impl *GatewayRouteServiceImpl) NewSession(helper ...*SessionHelper) (GatewayRouteService, error) {
	var session *SessionHelper
	var err error
	if len(helper) == 0 {
		session, err = NewSessionHelper()
		if err != nil {
			return nil, err
		}
	} else if helper[0] == nil {
		return &GatewayRouteServiceImpl{
			engine:   impl.engine,
			executor: impl.engine,
		}, nil
	} else {
		session = helper[0]
	}
	return &GatewayRouteServiceImpl{
		engine:        impl.engine,
		executor:      session.session,
		SessionHelper: session,
	}, nil
}

func (impl *GatewayRouteServiceImpl) Insert(route *orm.GatewayRoute) error {
	if route == nil {
		return errors.New(ERR_INVALID_ARG)
	}
	_, err := orm.Insert(impl.executor, route)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil
}

func (impl *GatewayRouteServiceImpl) Update(route *orm.GatewayRoute) error {
	if route == nil {
		return errors.New(ERR_INVALID_ARG)
	}
	_, err := orm.Update(impl.executor, route)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil
}

func (impl *GatewayRouteServiceImpl) DeleteById(id string) error {
	if len(id) == 0 {
		return errors.New(ERR_INVALID_ARG)
	}
	_, err := orm.Delete(impl.executor, &orm.GatewayRoute{}, "id = ?", id)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil

}

func (impl *GatewayRouteServiceImpl) GetByApiId(id string) (*orm.GatewayRoute, error) {
	if len(id) == 0 {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	route := &orm.GatewayRoute{}
	succ, err := orm.Get(impl.executor, route, "api_id = ?", id)
	if err != nil {
		return nil, errors.Wrap(err, ERR_SQL_FAIL)
	}
	if !succ {
		return nil, nil
	}
	return route, nil
}

func (impl *GatewayRouteServiceImpl) GetById(id string) (*orm.GatewayRoute, error) {
	if len(id) == 0 {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	route := &orm.GatewayRoute{}
	succ, err := orm.Get(impl.executor, route, "id = ?", id)
	if err != nil {
		return nil, errors.Wrap(err, ERR_SQL_FAIL)
	}
	if !succ {
		return nil, nil
	}
	return route, nil
}
