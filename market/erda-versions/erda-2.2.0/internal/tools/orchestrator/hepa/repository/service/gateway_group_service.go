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

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common"
	. "github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/vars"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
)

type GatewayGroupServiceImpl struct {
	engine *orm.OrmEngine
}

func NewGatewayGroupServiceImpl() (*GatewayGroupServiceImpl, error) {
	engine, error := orm.GetSingleton()
	if error != nil {
		return nil, errors.Wrap(error, "new GatewayGroupServiceImpl failed")
	}
	return &GatewayGroupServiceImpl{engine}, nil
}

func (impl *GatewayGroupServiceImpl) Insert(group *orm.GatewayGroup) error {
	if group == nil {
		return errors.New(ERR_INVALID_ARG)
	}
	_, err := orm.Insert(impl.engine, group)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil
}

func (impl *GatewayGroupServiceImpl) CountByConsumerId(consumerId string) (int64, error) {
	if len(consumerId) == 0 {
		return 0, errors.New(ERR_INVALID_ARG)
	}
	count, err := orm.Count(impl.engine, &orm.GatewayGroup{}, "consumer_id = ?", consumerId)
	if err != nil {
		return 0, errors.Wrap(err, ERR_SQL_FAIL)
	}
	return count, nil
}

func (impl *GatewayGroupServiceImpl) GetPageByConsumerId(consumerId string, page *common.Page) (*common.PageQuery, error) {
	if len(consumerId) == 0 {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	total, err := impl.CountByConsumerId(consumerId)
	if err != nil {
		return nil, errors.Wrap(err, "get total consumer count failed")
	}
	page.SetTotalNum(total)
	result := []orm.GatewayGroup{}
	if total == 0 {
		p := common.GetPageQuery(page, result)
		return &p, nil
	}
	err = orm.SelectPage(impl.engine.Desc("create_time"), &result, page, "consumer_id = ?", consumerId)
	if err != nil {
		return nil, errors.Wrap(err, ERR_SQL_FAIL)
	}
	p := common.GetPageQuery(page, result)
	return &p, nil
}

func (impl *GatewayGroupServiceImpl) GetById(id string) (*orm.GatewayGroup, error) {
	if len(id) == 0 {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	group := &orm.GatewayGroup{}
	succ, err := orm.Get(impl.engine, group, "id = ?", id)
	if err != nil {
		return nil, errors.Wrap(err, ERR_SQL_FAIL)
	}
	if !succ {
		return nil, nil
	}
	return group, nil
}

func (impl *GatewayGroupServiceImpl) GetByNameAndConsumerId(name string, id string) (*orm.GatewayGroup, error) {
	if len(id) == 0 {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	group := &orm.GatewayGroup{}
	succ, err := orm.Get(impl.engine, group, "group_name = ? and consumer_id = ?", name, id)
	if err != nil {
		return nil, errors.Wrap(err, ERR_SQL_FAIL)
	}
	if !succ {
		return nil, nil
	}
	return group, nil
}

func (impl *GatewayGroupServiceImpl) DeleteById(id string) error {
	if len(id) == 0 {
		return errors.New(ERR_INVALID_ARG)
	}
	_, err := orm.Delete(impl.engine, &orm.GatewayGroup{}, "id = ?", id)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil

}

func (impl *GatewayGroupServiceImpl) Update(group *orm.GatewayGroup) error {
	if group == nil {
		return errors.New(ERR_INVALID_ARG)
	}
	_, err := orm.Update(impl.engine, group)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil
}
