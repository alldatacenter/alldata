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

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common"
	. "github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/vars"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
)

type GatewayDomainServiceImpl struct {
	engine *orm.OrmEngine
	*SessionHelper
	executor xorm.Interface
}

func NewGatewayDomainServiceImpl() (*GatewayDomainServiceImpl, error) {
	engine, error := orm.GetSingleton()
	if error != nil {
		return nil, errors.Wrap(error, "new GatewayDomainServiceImpl failed")
	}
	return &GatewayDomainServiceImpl{
		engine:   engine,
		executor: engine,
	}, nil
}

func (impl *GatewayDomainServiceImpl) NewSession(helper ...*SessionHelper) (GatewayDomainService, error) {
	var session *SessionHelper
	var err error
	if len(helper) == 0 {
		session, err = NewSessionHelper()
		if err != nil {
			return nil, err
		}
	} else if helper[0] == nil {
		return &GatewayDomainServiceImpl{
			engine:   impl.engine,
			executor: impl.engine,
		}, nil
	} else {
		session = helper[0]
	}
	return &GatewayDomainServiceImpl{
		engine:        impl.engine,
		executor:      session.session,
		SessionHelper: session,
	}, nil
}

func (impl *GatewayDomainServiceImpl) Update(dao *orm.GatewayDomain) error {
	if dao == nil {
		return errors.New(ERR_INVALID_ARG)
	}
	_, err := orm.Update(impl.executor, dao)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil
}

func (impl *GatewayDomainServiceImpl) Insert(dao *orm.GatewayDomain) error {
	if dao == nil {
		return errors.New(ERR_INVALID_ARG)
	}
	_, err := orm.Insert(impl.executor, dao)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil
}

func (impl *GatewayDomainServiceImpl) GetByAny(cond *orm.GatewayDomain) (*orm.GatewayDomain, error) {
	if cond == nil {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	dao := &orm.GatewayDomain{}
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

func (impl *GatewayDomainServiceImpl) SelectByAny(cond *orm.GatewayDomain) ([]orm.GatewayDomain, error) {
	var result []orm.GatewayDomain
	if cond == nil {
		return result, errors.New(ERR_INVALID_ARG)
	}
	bCond, err := orm.BuildConds(impl.engine, cond, cond.GetMustCondCols())
	if err != nil {
		return result, errors.Wrap(err, ERR_SQL_FAIL)
	}
	err = orm.SelectByAnyI(impl.executor.Asc("create_time"), bCond, &result)
	if err != nil {
		return result, errors.Wrap(err, ERR_SQL_FAIL)
	}
	return result, nil
}

func (impl *GatewayDomainServiceImpl) Get(id string) (*orm.GatewayDomain, error) {
	if len(id) == 0 {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	dao := &orm.GatewayDomain{}
	succ, err := orm.Get(impl.executor, dao, "id = ?", id)
	if err != nil {
		return nil, errors.Wrap(err, ERR_SQL_FAIL)
	}
	if !succ {
		return nil, nil
	}
	return dao, nil
}

func (impl *GatewayDomainServiceImpl) Delete(id string) error {
	if len(id) == 0 {
		return errors.New(ERR_INVALID_ARG)
	}
	_, err := orm.Delete(impl.executor, &orm.GatewayDomain{}, "id = ?", id)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil
}

func (impl *GatewayDomainServiceImpl) DeleteByAny(cond *orm.GatewayDomain) (int64, error) {
	if cond == nil {
		return 0, errors.New(ERR_INVALID_ARG)
	}
	bCond, err := orm.BuildConds(impl.engine, cond, cond.GetMustCondCols())
	if err != nil {
		return 0, errors.Wrap(err, ERR_SQL_FAIL)
	}
	changed, err := orm.DeleteByAnyI(impl.executor, bCond, cond)
	if err != nil {
		return 0, errors.Wrap(err, ERR_SQL_FAIL)
	}
	return changed, nil
}

func (impl *GatewayDomainServiceImpl) Count(options []orm.SelectOption) (int64, error) {
	count, err := orm.CountWithOption(options, impl.executor, &orm.GatewayDomain{})
	if err != nil {
		return 0, errors.Wrap(err, ERR_SQL_FAIL)
	}
	return count, nil
}

func (impl *GatewayDomainServiceImpl) GetPage(options []orm.SelectOption, page *common.Page) (*common.PageQuery, error) {
	total, err := impl.Count(options)
	if err != nil {
		return nil, errors.Wrap(err, "get total count failed")
	}
	page.SetTotalNum(total)
	result := []orm.GatewayDomain{}
	if total == 0 {
		p := common.GetPageQuery(page, result)
		return &p, nil
	}
	err = orm.SelectPageWithOption(options, impl.executor, &result, page)
	if err != nil {
		return nil, errors.Wrap(err, ERR_SQL_FAIL)
	}
	p := common.GetPageQuery(page, result)
	return &p, nil
}

func (impl *GatewayDomainServiceImpl) SelectByOptions(options []orm.SelectOption) ([]orm.GatewayDomain, error) {
	var result []orm.GatewayDomain
	err := orm.SelectWithOption(options, impl.engine, &result)
	if err != nil {
		return result, errors.Wrap(err, ERR_SQL_FAIL)
	}
	return result, nil
}
