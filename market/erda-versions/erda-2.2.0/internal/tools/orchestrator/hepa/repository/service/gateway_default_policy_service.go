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

type GatewayDefaultPolicyServiceImpl struct {
	engine *orm.OrmEngine
	*SessionHelper
	executor xorm.Interface
}

func NewGatewayDefaultPolicyServiceImpl() (*GatewayDefaultPolicyServiceImpl, error) {
	engine, error := orm.GetSingleton()
	if error != nil {
		return nil, errors.Wrap(error, "new GatewayDefaultPolicyServiceImpl failed")
	}
	return &GatewayDefaultPolicyServiceImpl{
		engine:   engine,
		executor: engine,
	}, nil
}

func (impl *GatewayDefaultPolicyServiceImpl) NewSession(helper ...*SessionHelper) (GatewayDefaultPolicyService, error) {
	var session *SessionHelper
	var err error
	if len(helper) == 0 {
		session, err = NewSessionHelper()
		if err != nil {
			return nil, err
		}
	} else if helper[0] == nil {
		return &GatewayDefaultPolicyServiceImpl{
			engine:   impl.engine,
			executor: impl.engine,
		}, nil
	} else {
		session = helper[0]
	}
	return &GatewayDefaultPolicyServiceImpl{
		engine:        impl.engine,
		executor:      session.session,
		SessionHelper: session,
	}, nil
}

func (impl *GatewayDefaultPolicyServiceImpl) Insert(defaultPolicy *orm.GatewayDefaultPolicy) error {
	if defaultPolicy == nil {
		return errors.New(ERR_INVALID_ARG)
	}
	_, err := orm.Insert(impl.executor, defaultPolicy)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil
}

func (impl *GatewayDefaultPolicyServiceImpl) GetByAny(cond *orm.GatewayDefaultPolicy) (*orm.GatewayDefaultPolicy, error) {
	if cond == nil {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	defaultPolicy := &orm.GatewayDefaultPolicy{}
	bCond, err := orm.BuildConds(impl.engine, cond, cond.GetMustCondCols())
	if err != nil {
		return nil, errors.Wrap(err, "buildConds failed")
	}
	succ, err := orm.GetByAnyI(impl.executor, bCond, defaultPolicy)
	if err != nil {
		return nil, errors.Wrap(err, ERR_SQL_FAIL)
	}
	if !succ {
		return nil, nil
	}
	return defaultPolicy, nil
}

func (impl *GatewayDefaultPolicyServiceImpl) CreateOrUpdate(dao *orm.GatewayDefaultPolicy) error {
	if dao == nil {
		return errors.New(ERR_INVALID_ARG)
	}
	policy := &orm.GatewayDefaultPolicy{}
	exist, err := orm.Get(impl.executor, policy, "level = ? and package_id = ? and name = ?", dao.Level, dao.PackageId, dao.Name)
	if err != nil {
		return errors.WithStack(err)
	}
	if !exist {
		_, err = orm.Insert(impl.executor, dao)
		if err != nil {
			return errors.WithStack(err)
		}
		return nil
	}
	dao.Id = policy.Id
	_, err = orm.Update(impl.executor, dao)
	if err != nil {
		return errors.WithStack(err)
	}
	return nil
}

func (impl *GatewayDefaultPolicyServiceImpl) SelectByAny(cond *orm.GatewayDefaultPolicy) ([]orm.GatewayDefaultPolicy, error) {
	var result []orm.GatewayDefaultPolicy
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

func (impl *GatewayDefaultPolicyServiceImpl) Update(policy *orm.GatewayDefaultPolicy) error {
	if policy == nil {
		return errors.New(ERR_INVALID_ARG)
	}
	_, err := orm.Update(impl.executor, policy)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil
}
