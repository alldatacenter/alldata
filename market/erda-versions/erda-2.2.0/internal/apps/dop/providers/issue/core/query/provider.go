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

package query

import (
	"io"
	"reflect"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	commonpb "github.com/erda-project/erda-proto-go/common/pb"
	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	syncpb "github.com/erda-project/erda-proto-go/dop/issue/sync/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/conf"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	stream "github.com/erda-project/erda/internal/apps/dop/providers/issue/stream/core"
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
	Cfg    *config
	Log    logs.Logger
	DB     *gorm.DB `autowired:"mysql-client"`
	bdl    *bundle.Bundle
	db     *dao.DBClient
	Stream stream.Interface
	uc     *ucauth.UCClient
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.bdl = bundle.New(bundle.WithCoreServices())
	p.db = &dao.DBClient{
		DBEngine: &dbengine.DBEngine{
			DB: p.DB,
		},
	}
	uc := ucauth.NewUCClient(discover.UC(), p.Cfg.UCClientID, p.Cfg.UCClientSecret)
	if conf.OryEnabled() {
		uc = ucauth.NewUCClient(p.Cfg.OryKratosPrivateAddr, conf.OryCompatibleClientID(), conf.OryCompatibleClientSecret())
		uc.SetDBClient(p.DB)
	}
	p.uc = uc
	return nil
}

type Interface interface {
	Paging(req pb.PagingIssueRequest) ([]*pb.Issue, uint64, error)
	GetIssue(id int64, identityInfo *commonpb.IdentityInfo) (*pb.Issue, error)
	AfterIssueUpdate(u *IssueUpdated) error
	UpdateIssue(req *pb.UpdateIssueRequest) error
	UpdateLabels(id, projectID uint64, labelNames []string) (err error)
	SyncLabels(value *syncpb.Value, issueIDs []uint64) error
	BatchUpdateIssue(req *pb.BatchUpdateIssueRequest) error
	GetProperties(req *pb.GetIssuePropertyRequest) ([]*pb.IssuePropertyIndex, error)
	CreatePropertyRelation(req *pb.CreateIssuePropertyInstanceRequest) error
	GetIssueStage(req *pb.IssueStageRequest) ([]*pb.IssueStage, error)
	GetIssueRelationsByIssueIDs(issueID uint64, relationType []string) ([]uint64, []uint64, error)
	GetIssuesByIssueIDs(issueIDs []uint64) ([]*pb.Issue, error)
	GetBatchProperties(orgID int64, issuesType []string) ([]*pb.IssuePropertyIndex, error)
	ExportExcel(issues []*pb.Issue, properties []*pb.IssuePropertyIndex, projectID uint64, isDownload bool, orgID int64, locale string) (io.Reader, string, error)
	SyncIssueChildrenIteration(issue *pb.Issue, iterationID int64) error
	AfterIssueAppRelationCreate(issueIDs []int64) error
	GetIssueLabelsByProjectID(projectID uint64) ([]dao.IssueLabel, error)
	GetIssuesStatesByProjectID(projectID uint64, issueType string) ([]dao.IssueState, error)
	GetAllIssuesByProject(req pb.IssueListRequest) ([]dao.IssueItem, error)
	GetIssueChildren(id uint64, req pb.PagingIssueRequest) ([]dao.IssueItem, uint64, error)
	GetIssueItem(id uint64) (*dao.IssueItem, error)
	GetIssueParents(issueID uint64, relationType []string) ([]dao.IssueItem, error)
	ListStatesTransByProjectID(projectID uint64) ([]dao.IssueStateTransition, error)
	GetIssueStateIDsByTypes(req *apistructs.IssueStatesRequest) ([]int64, error)
	GetIssueStatesMap(req *pb.GetIssueStatesRequest) (map[string][]pb.IssueStatus, error)
	GetIssueStateIDs(req *pb.GetIssueStatesRequest) ([]int64, error)
	GetIssueStatesBelong(req *pb.GetIssueStateRelationRequest) ([]apistructs.IssueStateState, error)
	AfterIssueInclusionRelationChange(id uint64) error
}

func init() {
	servicehub.Register("erda.dop.issue.core.query", &servicehub.Spec{
		Services:   []string{"erda.dop.issue.core.query"},
		Types:      []reflect.Type{reflect.TypeOf((*Interface)(nil)).Elem()},
		ConfigFunc: func() interface{} { return &config{} },
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
