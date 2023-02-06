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

package core

import (
	"reflect"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/conf"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/stream/common"
	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/ucauth"
)

type config struct {
	UCClientID           string `default:"dice" file:"UC_CLIENT_ID"`
	UCClientSecret       string `default:"secret" file:"UC_CLIENT_SECRET"`
	OryKratosPrivateAddr string `default:"kratos-admin" file:"ORY_KRATOS_ADMIN_ADDR"`
}

type provider struct {
	Cfg        *config
	Log        logs.Logger
	DB         *gorm.DB `autowired:"mysql-client"`
	bdl        *bundle.Bundle
	db         *dao.DBClient
	I18n       i18n.Translator `translator:"issue-manage"`
	CPTran     i18n.I18n       `autowired:"i18n@cp"`
	commonTran i18n.Translator
	uc         *ucauth.UCClient
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.bdl = bundle.New(bundle.WithCoreServices())
	p.db = &dao.DBClient{
		DBEngine: &dbengine.DBEngine{
			DB: p.DB,
		},
	}
	p.commonTran = p.CPTran.Translator("")
	uc := ucauth.NewUCClient(discover.UC(), p.Cfg.UCClientID, p.Cfg.UCClientSecret)
	if conf.OryEnabled() {
		uc = ucauth.NewUCClient(p.Cfg.OryKratosPrivateAddr, conf.OryCompatibleClientID(), conf.OryCompatibleClientSecret())
		uc.SetDBClient(p.DB)
	}
	p.uc = uc
	return nil
}

type Interface interface {
	Create(req *common.IssueStreamCreateRequest) (int64, error)
	CreateIssueEvent(req *common.IssueStreamCreateRequest) error
	CreateIssueStreamBySystem(id uint64, streamFields map[string][]interface{}) error
	CreateStream(updateReq *pb.UpdateIssueRequest, streamFields map[string][]interface{}) error
	GetDefaultContent(req StreamTemplateRequest) (string, error)
}

func (p *provider) Create(req *common.IssueStreamCreateRequest) (int64, error) {
	// TODO 请求校验
	// TODO 鉴权
	is := &dao.IssueStream{
		IssueID:      req.IssueID,
		Operator:     req.Operator,
		StreamType:   req.StreamType,
		StreamParams: req.StreamParams,
	}
	if err := p.db.CreateIssueStream(is); err != nil {
		return 0, err
	}

	if req.StreamType == common.ISTRelateMR {
		// 添加事件应用关联关系
		issueAppRel := dao.IssueAppRelation{
			IssueID:   req.IssueID,
			CommentID: int64(is.ID),
			AppID:     req.StreamParams.MRInfo.AppID,
			MRID:      req.StreamParams.MRInfo.MrID,
		}
		if err := p.db.CreateIssueAppRelation(&issueAppRel); err != nil {
			return 0, err
		}
	}

	return int64(is.ID), nil
}

func init() {
	servicehub.Register("erda.dop.issue.stream.core", &servicehub.Spec{
		Services:   []string{"erda.dop.issue.stream.CoreService"},
		Types:      []reflect.Type{reflect.TypeOf((*Interface)(nil)).Elem()},
		ConfigFunc: func() interface{} { return &config{} },
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
