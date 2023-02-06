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

type GatewayPackageApiInConsumerServiceImpl struct {
	engine *orm.OrmEngine
	*SessionHelper
	executor xorm.Interface
}

func NewGatewayPackageApiInConsumerServiceImpl() (*GatewayPackageApiInConsumerServiceImpl, error) {
	engine, error := orm.GetSingleton()
	if error != nil {
		return nil, errors.Wrap(error, "new GatewayPackageApiInConsumerServiceImpl failed")
	}
	return &GatewayPackageApiInConsumerServiceImpl{
		engine:   engine,
		executor: engine,
	}, nil
}

func (impl *GatewayPackageApiInConsumerServiceImpl) NewSession(helper ...*SessionHelper) (GatewayPackageApiInConsumerService, error) {
	var session *SessionHelper
	var err error
	if len(helper) == 0 {
		session, err = NewSessionHelper()
		if err != nil {
			return nil, err
		}
	} else if helper[0] == nil {
		return &GatewayPackageApiInConsumerServiceImpl{
			engine:   impl.engine,
			executor: impl.engine,
		}, nil
	} else {
		session = helper[0]
	}
	return &GatewayPackageApiInConsumerServiceImpl{
		engine:        impl.engine,
		executor:      session.session,
		SessionHelper: session,
	}, nil
}

func (impl *GatewayPackageApiInConsumerServiceImpl) Update(dao *orm.GatewayPackageApiInConsumer) error {
	if dao == nil {
		return errors.New(ERR_INVALID_ARG)
	}
	_, err := orm.Update(impl.executor, dao)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil
}

func (impl *GatewayPackageApiInConsumerServiceImpl) Insert(dao *orm.GatewayPackageApiInConsumer) error {
	if dao == nil {
		return errors.New(ERR_INVALID_ARG)
	}
	_, err := orm.Insert(impl.executor, dao)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil
}

func (impl *GatewayPackageApiInConsumerServiceImpl) GetByAny(cond *orm.GatewayPackageApiInConsumer) (*orm.GatewayPackageApiInConsumer, error) {
	if cond == nil {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	dao := &orm.GatewayPackageApiInConsumer{}
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

func (impl *GatewayPackageApiInConsumerServiceImpl) SelectByAny(cond *orm.GatewayPackageApiInConsumer) ([]orm.GatewayPackageApiInConsumer, error) {
	var result []orm.GatewayPackageApiInConsumer
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

func (impl *GatewayPackageApiInConsumerServiceImpl) Get(id string) (*orm.GatewayPackageApiInConsumer, error) {
	if len(id) == 0 {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	dao := &orm.GatewayPackageApiInConsumer{}
	succ, err := orm.Get(impl.executor, dao, "id = ?", id)
	if err != nil {
		return nil, errors.Wrap(err, ERR_SQL_FAIL)
	}
	if !succ {
		return nil, nil
	}
	return dao, nil
}

func (impl *GatewayPackageApiInConsumerServiceImpl) Delete(packageId, packageApiId, consumerId string) error {
	if packageId == "" || consumerId == "" {
		return errors.New(ERR_INVALID_ARG)
	}
	_, err := orm.Delete(impl.executor, &orm.GatewayPackageApiInConsumer{}, "package_id = ? and package_api_id = ? and consumer_id = ?", packageId, packageApiId, consumerId)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil
}

func (impl *GatewayPackageApiInConsumerServiceImpl) DeleteByConsumerId(id string) error {
	if len(id) == 0 {
		return errors.New(ERR_INVALID_ARG)
	}
	_, err := orm.Delete(impl.executor, &orm.GatewayPackageApiInConsumer{}, "consumer_id = ?", id)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil
}

func (impl *GatewayPackageApiInConsumerServiceImpl) SelectByConsumer(id string) ([]orm.GatewayPackageApiInConsumer, error) {
	var result []orm.GatewayPackageApiInConsumer
	if len(id) == 0 {
		return result, errors.New(ERR_INVALID_ARG)
	}
	err := orm.Select(impl.executor, &result, "consumer_id = ?", id)
	if err != nil {
		return result, errors.Wrap(err, ERR_SQL_FAIL)
	}
	return result, nil
}

func (impl *GatewayPackageApiInConsumerServiceImpl) SelectByPackageApi(packageId, packageApiId string) ([]orm.GatewayPackageApiInConsumer, error) {
	var result []orm.GatewayPackageApiInConsumer
	if len(packageId) == 0 {
		return result, errors.New(ERR_INVALID_ARG)
	}
	err := orm.Select(impl.executor, &result, "package_id = ? and package_api_id = ?",
		packageId, packageApiId)
	if err != nil {
		return result, errors.Wrap(err, ERR_SQL_FAIL)
	}
	return result, nil
}
