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

	. "github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/vars"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
)

type GatewayMockServiceImpl struct {
	engine *orm.OrmEngine
}

func NewGatewayMockServiceImpl() (*GatewayMockServiceImpl, error) {
	engine, error := orm.GetSingleton()
	if error != nil {
		return nil, errors.Wrap(error, "new GatewayMockServiceImpl failed")
	}
	return &GatewayMockServiceImpl{engine}, nil
}

func (impl *GatewayMockServiceImpl) GetMockByAny(cond *orm.GatewayMock) (*orm.GatewayMock, error) {
	if cond == nil {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	find := &orm.GatewayMock{}
	succ, err := orm.GetByAny(impl.engine, find, cond)
	if err != nil {
		return nil, errors.Wrap(err, ERR_SQL_FAIL)
	}
	if !succ {
		return nil, nil
	}
	return find, nil
}

func (impl *GatewayMockServiceImpl) Update(mock *orm.GatewayMock) error {
	if mock == nil {
		return errors.New(ERR_INVALID_ARG)
	}
	_, err := orm.Update(impl.engine, mock)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil
}

func (impl *GatewayMockServiceImpl) Insert(mock *orm.GatewayMock) error {
	if mock == nil {
		return errors.New(ERR_INVALID_ARG)
	}
	_, err := orm.Insert(impl.engine, mock)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil
}
