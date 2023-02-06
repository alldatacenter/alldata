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

type GatewayUpstreamRegisterRecordServiceImpl struct {
	engine *orm.OrmEngine
}

func NewGatewayUpstreamRegisterRecordServiceImpl() (*GatewayUpstreamRegisterRecordServiceImpl, error) {
	engine, error := orm.GetSingleton()
	if error != nil {
		return nil, errors.Wrap(error, "new GatewayUpstreamRegisterRecordServiceImpl failed")
	}
	return &GatewayUpstreamRegisterRecordServiceImpl{engine}, nil
}

func (impl *GatewayUpstreamRegisterRecordServiceImpl) Insert(session *xorm.Session, item *orm.GatewayUpstreamRegisterRecord) error {
	if session == nil || item == nil || len(item.UpstreamId) == 0 || item.RegisterId == "" {
		return errors.New(ERR_INVALID_ARG)
	}
	_, err := orm.Insert(session, item)
	if err != nil {
		return errors.WithStack(err)
	}
	return nil
}

func (impl *GatewayUpstreamRegisterRecordServiceImpl) count(upstreamId string) (int64, error) {
	if len(upstreamId) == 0 {
		return 0, errors.New(ERR_INVALID_ARG)
	}
	total, err := orm.Count(impl.engine, &orm.GatewayUpstreamRegisterRecord{}, "upstream_id = ?", upstreamId)
	if err != nil {
		return 0, errors.WithStack(err)
	}
	return total, nil
}

func (impl *GatewayUpstreamRegisterRecordServiceImpl) GetPage(upstreamId string, page *common.Page) (*common.PageQuery, error) {
	total, err := impl.count(upstreamId)
	if err != nil {
		return nil, errors.WithStack(err)
	}
	page.SetTotalNum(total)
	result := []orm.GatewayUpstreamRegisterRecord{}
	if total == 0 {
		p := common.GetPageQuery(page, result)
		return &p, nil
	}
	err = orm.SelectPage(impl.engine.Desc("create_time"), &result, page, "upstream_id = ?", upstreamId)
	if err != nil {
		return nil, errors.WithStack(err)
	}
	p := common.GetPageQuery(page, result)
	return &p, nil
}

func (impl *GatewayUpstreamRegisterRecordServiceImpl) Get(upstreamId string, registerId string) (*orm.GatewayUpstreamRegisterRecord, error) {
	if len(upstreamId) == 0 || registerId == "" {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	record := &orm.GatewayUpstreamRegisterRecord{}
	succ, err := orm.Get(impl.engine, record, "upstream_id = ? and register_id = ?", upstreamId, registerId)
	if err != nil {
		return nil, errors.WithStack(err)
	}
	if !succ {
		return nil, nil
	}
	return record, nil
}
