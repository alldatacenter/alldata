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

type GatewayUpstreamServiceImpl struct {
	engine *orm.OrmEngine
}

func NewGatewayUpstreamServiceImpl() (*GatewayUpstreamServiceImpl, error) {
	engine, error := orm.GetSingleton()
	if error != nil {
		return nil, errors.Wrap(error, "new GatewayUpstreamServiceImpl failed")
	}
	return &GatewayUpstreamServiceImpl{engine}, nil
}

func (impl *GatewayUpstreamServiceImpl) UpdateRegister(session *xorm.Session, dao *orm.GatewayUpstream) (bool, bool, string, error) {
	needUpdate := false
	if session == nil || dao == nil {
		return needUpdate, false, "", errors.New(ERR_INVALID_ARG)
	}
	exist := &orm.GatewayUpstream{}
	// check exist without lock
	succ, err := orm.Get(session, exist, "org_id = ? and project_id = ? and env = ? and az = ? and upstream_name = ? and runtime_service_id = ?", dao.OrgId, dao.ProjectId, dao.Env, dao.Az, dao.UpstreamName, dao.RuntimeServiceId)
	if err != nil {
		return needUpdate, false, "", errors.WithStack(err)
	}
	if !succ {
		// check exist with table lock
		succ, err = orm.GetForUpdate(session, impl.engine, exist, "org_id = ? and project_id = ? and env = ? and az = ? and upstream_name = ? and runtime_service_id = ?", dao.OrgId, dao.ProjectId, dao.Env, dao.Az, dao.UpstreamName, dao.RuntimeServiceId)
		if err != nil {
			return needUpdate, false, "", errors.WithStack(err)
		}
		if !succ {
			// create
			// TODO: default 0 when ENV is staging or prod, depend on dice UI
			needUpdate = true
			dao.AutoBind = 1
			_, err := orm.Insert(session, dao)
			if err != nil {
				return needUpdate, false, "", errors.WithStack(err)
			}
			return needUpdate, true, dao.Id, nil
		}
		return false, false, "", errors.New("upstream created by other session")
	}
	// get with row lock
	_, err = orm.GetForUpdate(session, impl.engine, exist, "id = ?", exist.Id)
	if err != nil {
		return needUpdate, false, "", errors.WithStack(err)
	}
	dao.AutoBind = exist.AutoBind
	dao.Id = exist.Id
	// check registerId
	if exist.LastRegisterId == dao.LastRegisterId {
		return false, false, exist.Id, nil
	}
	// update
	needUpdate = true
	_, err = orm.Update(session, dao, "last_register_id", "zone_id")
	if err != nil {
		return needUpdate, false, "", errors.WithStack(err)
	}
	return needUpdate, false, dao.Id, nil
}

func (impl *GatewayUpstreamServiceImpl) updateFields(engine xorm.Interface, update *orm.GatewayUpstream, fields ...string) error {
	if update == nil || len(update.Id) == 0 {
		return errors.New(ERR_INVALID_ARG)
	}
	_, err := orm.Update(engine, update, fields...)
	if err != nil {
		return errors.WithStack(err)
	}
	return nil
}

func (impl *GatewayUpstreamServiceImpl) GetValidIdForUpdate(id string, session *xorm.Session) (string, error) {
	if len(id) == 0 {
		return "", errors.New(ERR_INVALID_ARG)
	}
	dao := &orm.GatewayUpstream{}
	exist, err := orm.GetForUpdate(session, impl.engine, dao, "id = ?", id)
	if err != nil {
		return "", errors.WithStack(err)
	}
	if !exist {
		return "", errors.Errorf("upstreamId[%s] not exists", id)
	}
	return dao.ValidRegisterId, nil
}

func (impl *GatewayUpstreamServiceImpl) UpdateValidId(update *orm.GatewayUpstream, session ...*xorm.Session) error {
	var engineI xorm.Interface
	if len(session) == 0 {
		engineI = impl.engine
	} else {
		if session[0] == nil {
			return errors.New(ERR_INVALID_ARG)
		}
		engineI = session[0]
	}
	return impl.updateFields(engineI, update, "valid_register_id")
}

func (impl *GatewayUpstreamServiceImpl) UpdateAutoBind(update *orm.GatewayUpstream) error {
	return impl.updateFields(impl.engine, update, "auto_bind")
}

func (impl *GatewayUpstreamServiceImpl) SelectByAny(cond *orm.GatewayUpstream) ([]orm.GatewayUpstream, error) {
	var result []orm.GatewayUpstream
	if cond == nil {
		return result, errors.New(ERR_INVALID_ARG)
	}
	err := orm.SelectByAny(impl.engine, &result, cond)
	if err != nil {
		return result, errors.Wrap(err, ERR_SQL_FAIL)
	}
	return result, nil
}
