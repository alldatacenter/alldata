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

package subscribe

import (
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/conf"
	"github.com/erda-project/erda/internal/core/legacy/dao"
	"github.com/erda-project/erda/internal/core/legacy/model"
)

type Subscribe struct {
	db *dao.DBClient
}

type Option func(*Subscribe)

func New(opts ...Option) *Subscribe {
	s := &Subscribe{}
	for _, f := range opts {
		f(s)
	}
	return s
}

func WithDBClient(dbClient *dao.DBClient) Option {
	return func(subscribe *Subscribe) {
		subscribe.db = dbClient
	}
}

func (s *Subscribe) Subscribe(req apistructs.CreateSubscribeReq) (string, error) {
	// subscribe limit number check
	count, err := s.db.GetSubscribeCount(req.Type.String(), req.UserID, req.OrgID)
	if err != nil {
		logrus.Errorf("get subscribe count failed, request: %v, error: %v", req, err)
		return "", errors.Errorf("get subscribe count failed, error: %v", err)
	}

	limit := conf.SubscribeLimitNum()

	if uint64(count) >= limit {
		err := errors.Errorf("reach subscribe limit: %v, count: %v", limit, count)
		logrus.Errorf(err.Error())
		return "", err
	}

	// subscribe duplication check
	d, err := s.db.GetSubscribe(req.Type.String(), req.TypeID, req.UserID, req.OrgID)
	if err != nil {
		logrus.Errorf("get subscribe failed, request: %v, error:%v", req, err)
		return "", errors.Errorf("get subscribe failed, error:%v", err)
	}
	if d != nil && d.TypeID == req.TypeID {
		return "", errors.Errorf("already subscribed, type: %v, id: %v", req.Type.String(), req.TypeID)
	}

	// create subscribe
	data := model.Subscribe{
		Type:   req.Type.String(),
		TypeID: req.TypeID,
		Name:   req.Name,
		UserID: req.UserID,
		OrgID:  req.OrgID,
	}
	err = s.db.CreateSubscribe(&data)
	if err != nil {
		return "", err
	}
	return data.ID, nil
}

func (s *Subscribe) UnSubscribe(req apistructs.UnSubscribeReq) error {
	if req.UserID == "" || req.OrgID == 0 {
		return errors.Errorf("user id or org id is empty")
	}

	if (req.TypeID <= 0 && !req.Type.IsEmpty()) || (req.TypeID > 0 && req.Type.IsEmpty()) {
		return errors.Errorf("invalid unsubscribe request, both type and typeID should be empty or non-empty. request: %v", req)
	}

	// unsubscribe by id
	if req.ID != "" {
		return s.db.DeleteBySubscribeID(req.ID)
	}

	// unsubscribe by userID & orgID
	if req.TypeID == 0 && req.Type.IsEmpty() {
		logrus.Debugf("delete subscribes by userid, userid: %v", req.UserID)
		return s.db.DeleteSubscribeByUserOrgID(req.UserID, req.OrgID)
	}

	// unsubscribe by type & typeID & userID & orgID
	return s.db.DeleteSubscribe(req.Type.String(), req.TypeID, req.UserID, req.OrgID)
}

func (s *Subscribe) GetSubscribes(req apistructs.GetSubscribeReq) ([]apistructs.Subscribe, error) {
	var (
		sub  *model.Subscribe
		raw  []model.Subscribe
		list []apistructs.Subscribe
		err  error
	)
	if req.TypeID == 0 {
		raw, err = s.db.GetSubscribesByUserOrgID(req.Type.String(), req.UserID, req.OrgID)
		if err != nil {
			return nil, err
		}
	}
	sub, err = s.db.GetSubscribe(req.Type.String(), req.TypeID, req.UserID, req.OrgID)
	if err != nil {
		return nil, err
	}

	if sub != nil {
		raw = []model.Subscribe{*sub}
	}

	for _, v := range raw {
		list = append(list, s.Convert(v))
	}

	return list, nil
}

func (s *Subscribe) Convert(subscribe model.Subscribe) apistructs.Subscribe {
	return apistructs.Subscribe{
		ID:        subscribe.ID,
		Type:      subscribe.Type,
		TypeID:    subscribe.TypeID,
		Name:      subscribe.Name,
		UserID:    subscribe.UserID,
		OrgID:     subscribe.OrgID,
		CreatedAt: &subscribe.CreatedAt,
		UpdateAt:  &subscribe.UpdatedAt,
	}
}
