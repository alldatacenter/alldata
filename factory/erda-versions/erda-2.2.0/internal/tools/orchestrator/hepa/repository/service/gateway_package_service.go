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

type GatewayPackageServiceImpl struct {
	engine *orm.OrmEngine
	*SessionHelper
	executor xorm.Interface
}

func NewGatewayPackageServiceImpl() (*GatewayPackageServiceImpl, error) {
	engine, error := orm.GetSingleton()
	if error != nil {
		return nil, errors.Wrap(error, "new GatewayPackageServiceImpl failed")
	}
	return &GatewayPackageServiceImpl{
		engine:   engine,
		executor: engine,
	}, nil
}

func (impl *GatewayPackageServiceImpl) NewSession(helper ...*SessionHelper) (GatewayPackageService, error) {
	var session *SessionHelper
	var err error
	if len(helper) == 0 {
		session, err = NewSessionHelper()
		if err != nil {
			return nil, err
		}
	} else if helper[0] == nil {
		return &GatewayPackageServiceImpl{
			engine:   impl.engine,
			executor: impl.engine,
		}, nil

	} else {
		session = helper[0]
	}
	return &GatewayPackageServiceImpl{
		engine:        impl.engine,
		executor:      session.session,
		SessionHelper: session,
	}, nil
}

func (impl *GatewayPackageServiceImpl) Update(dao *orm.GatewayPackage, columns ...string) error {
	if dao == nil {
		return errors.New(ERR_INVALID_ARG)
	}
	_, err := orm.Update(impl.executor, dao, columns...)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil
}

func (impl *GatewayPackageServiceImpl) Insert(dao *orm.GatewayPackage) error {
	if dao == nil {
		return errors.New(ERR_INVALID_ARG)
	}
	_, err := orm.Insert(impl.executor, dao)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil
}

func (impl *GatewayPackageServiceImpl) GetByAny(cond *orm.GatewayPackage) (*orm.GatewayPackage, error) {
	if cond == nil {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	dao := &orm.GatewayPackage{}
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

func (impl *GatewayPackageServiceImpl) SelectByAny(cond *orm.GatewayPackage) ([]orm.GatewayPackage, error) {
	var result []orm.GatewayPackage
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

func (impl *GatewayPackageServiceImpl) Get(id string) (*orm.GatewayPackage, error) {
	if len(id) == 0 {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	dao := &orm.GatewayPackage{}
	succ, err := orm.Get(impl.executor, dao, "id = ?", id)
	if err != nil {
		return nil, errors.Wrap(err, ERR_SQL_FAIL)
	}
	if !succ {
		return nil, nil
	}
	return dao, nil
}

func (impl *GatewayPackageServiceImpl) Delete(id string, realDelete ...bool) error {
	if len(id) == 0 {
		return errors.New(ERR_INVALID_ARG)
	}
	if len(realDelete) > 0 && realDelete[0] {
		_, err := orm.RealDelete(impl.executor, &orm.GatewayPackage{}, "id = ?", id)
		if err != nil {
			return errors.Wrap(err, ERR_SQL_FAIL)
		}
		return nil
	}
	_, err := orm.Delete(impl.executor, &orm.GatewayPackage{}, "id = ?", id)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil
}

func (impl *GatewayPackageServiceImpl) GetPage(options []orm.SelectOption, page *common.Page) (*common.PageQuery, error) {
	total, err := impl.Count(options)
	if err != nil {
		return nil, errors.Wrap(err, "get total count failed")
	}
	page.SetTotalNum(total)
	result := []orm.GatewayPackage{}
	if total == 0 {
		p := common.GetPageQuery(page, result)
		return &p, nil
	}
	err = orm.SelectPageWithOption(options, impl.executor.OrderBy(`field (scene, 'unity') desc`).Desc("create_time"), &result, page)
	if err != nil {
		return nil, errors.Wrap(err, ERR_SQL_FAIL)
	}
	p := common.GetPageQuery(page, result)
	return &p, nil
}

func (impl *GatewayPackageServiceImpl) Count(options []orm.SelectOption) (int64, error) {
	count, err := orm.CountWithOption(options, impl.executor, &orm.GatewayPackage{})
	if err != nil {
		return 0, errors.Wrap(err, ERR_SQL_FAIL)
	}
	return count, nil
}

func (impl *GatewayPackageServiceImpl) CheckUnique(dao *orm.GatewayPackage) (bool, error) {
	if dao == nil {
		return false, errors.New(ERR_INVALID_ARG)
	}
	exist, err := impl.GetByAny(&orm.GatewayPackage{
		DiceOrgId:       dao.DiceOrgId,
		DiceProjectId:   dao.DiceProjectId,
		DiceEnv:         dao.DiceEnv,
		DiceClusterName: dao.DiceClusterName,
		PackageName:     dao.PackageName,
	})
	if err != nil {
		return false, errors.Wrap(err, ERR_SQL_FAIL)
	}
	unique := (exist == nil) || (exist.Id == dao.Id)
	return unique, nil
}
