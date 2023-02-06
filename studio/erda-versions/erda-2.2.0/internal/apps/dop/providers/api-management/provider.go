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

package apim

import (
	"context"
	"strings"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"google.golang.org/protobuf/types/known/timestamppb"
	"gorm.io/gorm"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-proto-go/dop/apim/pb"
	orgCache "github.com/erda-project/erda/internal/apps/dop/cache/org"
	"github.com/erda-project/erda/internal/apps/dop/providers/api-management/apierr"
	"github.com/erda-project/erda/internal/apps/dop/providers/api-management/model"
	"github.com/erda-project/erda/pkg/common/apis"
)

var (
	name = "erda.dop.apim"
	spec = servicehub.Spec{
		Services:             pb.ServiceNames(),
		Types:                pb.Types(),
		OptionalDependencies: []string{"service-register"},
		Description:          "api management service",
		ConfigFunc: func() interface{} {
			return &struct{}{}
		},
		Creator: func() servicehub.Provider {
			return new(provider)
		},
	}
)

func init() {
	servicehub.Register(name, &spec)
}

// +provider
type provider struct {
	l        *logrus.Entry
	Register transport.Register `autowired:"service-register" required:"true"`
	DB       *gorm.DB           `autowired:"mysql-gorm.v2-client"`
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.l = logrus.WithField("provider", name)
	p.l.Infoln("init")
	if p.Register != nil {
		p.l.Infoln("register self")
		pb.RegisterExportRecordsImp(p.Register, p, apis.Options())
	}
	return nil
}

// CreateExportRecord POST /api/apim/export
func (p *provider) CreateExportRecord(ctx context.Context, req *pb.CreateExportRecordsReq) (*pb.CreateExportRecordsResp, error) {
	// get orgID and name
	var orgID = apis.GetOrgID(ctx)
	org, ok := orgCache.GetOrgByOrgID(orgID)
	if !ok {
		p.l.WithField("orgID", orgID).Errorln("org name not found")
		return nil, apierr.CreateExportRecord.InternalError(errors.Errorf("org name not found: %v", orgID))
	}

	// get userID
	userID := apis.GetUserID(ctx)

	// select the version
	var (
		version   model.APIAssetVersion
		condition = map[string]interface{}{"id": req.VersionID, "org_id": orgID}
	)
	if err := p.DB.First(&version, condition).Error; err != nil {
		p.l.WithError(err).WithFields(condition).Errorln("failed to First")
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, apierr.CreateExportRecord.NotFound()
		}
	}

	// create record
	var m = model.APIMExportRecord{
		Common: model.Common{
			OrgID:     uint32(org.ID),
			OrgName:   org.Name,
			CreatorID: userID,
			UpdaterID: userID,
		},
		AssetID:        version.AssetID,
		AssetName:      version.AssetName,
		VersionID:      req.GetVersionID(),
		SwaggerVersion: version.SwaggerVersion,
		Major:          version.Major,
		Minor:          version.Minor,
		Patch:          version.Patch,
		SpecProtocol:   req.GetSpecProtocol(),
	}
	if err := p.DB.Create(&m).Error; err != nil {
		p.l.WithError(err).Errorln("failed to create")
		return nil, err
	}
	return &pb.CreateExportRecordsResp{
		Data: &pb.ExportRecord{
			Id:             m.ID.String,
			CreatedAt:      timestamppb.New(m.CreatedAt),
			UpdatedAt:      timestamppb.New(m.UpdatedAt),
			OrgID:          m.OrgID,
			OrgName:        m.OrgName,
			DeletedAt:      timestamppb.New(m.DeletedAt.Time),
			CreatorID:      m.CreatorID,
			UpdaterID:      m.UpdaterID,
			AssetID:        m.AssetID,
			AssetName:      m.AssetName,
			VersionID:      m.VersionID,
			SwaggerVersion: m.SwaggerVersion,
			Major:          m.Major,
			Minor:          m.Minor,
			Patch:          m.Patch,
			SpecProtocol:   m.SpecProtocol,
			Valid:          true,
		},
		UserIDs: []string{m.CreatorID, m.UpdaterID},
	}, nil
}

// ListExportRecords GET /api/apim/export
func (p *provider) ListExportRecords(ctx context.Context, req *pb.ListExportRecordsReq) (*pb.ListExportRecordsResp, error) {
	// adjust params
	if req.GetPageSize() == 0 {
		req.PageSize = 10
	}
	if req.GetPageNo() == 0 {
		req.PageNo = 1
	}

	// get orgID
	orgID, err := apis.GetIntOrgID(ctx)
	if err != nil {
		p.l.WithError(err).Infoln("failed to GetIntOrgID")
	}

	var (
		data = &pb.ListExportRecordsData{
			Total: 0,
			List:  nil,
		}
		records []*model.APIMExportRecord
	)
	where := p.DB.Where(map[string]interface{}{"org_id": orgID})
	if err := where.Model(model.APIMExportRecord{}).Count(&data.Total).Error; err != nil {
		p.l.WithError(err).Errorln("failed to count")
		return nil, apierr.ListExportRecords.InternalError(err)
	}
	if data.Total == 0 {
		return &pb.ListExportRecordsResp{}, nil
	}
	switch orderBy := strings.ToLower(req.GetOrderBy()); orderBy {
	case "", "-updatedat", "-updated_at", "-createdat", "-created_at":
		where = where.Order("updated_at DESC, created_at DESC")
	default:
		where = where.Order("updated_at ASC, created_at ASC")
	}

	if err := where.Limit(int(req.GetPageSize())).Offset(int((req.GetPageNo() - 1) * req.GetPageSize())).
		Find(&records).Error; err != nil {
		p.l.WithError(err).Errorln("failed to Find")
		return nil, apierr.ListExportRecords.InternalError(err)
	}
	for _, record := range records {
		data.List = append(data.List, &pb.ExportRecord{
			Id:             record.ID.String,
			CreatedAt:      timestamppb.New(record.CreatedAt),
			UpdatedAt:      timestamppb.New(record.UpdatedAt),
			OrgID:          record.OrgID,
			OrgName:        record.OrgName,
			DeletedAt:      timestamppb.New(record.DeletedAt.Time),
			CreatorID:      record.CreatorID,
			UpdaterID:      record.UpdaterID,
			AssetID:        record.AssetID,
			AssetName:      record.AssetName,
			VersionID:      record.VersionID,
			SwaggerVersion: record.SwaggerVersion,
			Major:          record.Major,
			Minor:          record.Minor,
			Patch:          record.Patch,
			SpecProtocol:   record.SpecProtocol,
			Valid: p.DB.Where("id = ?", record.VersionID).
				First(new(model.APIAssetVersion)).Error == nil,
		})
	}

	var resp = &pb.ListExportRecordsResp{
		Data:    data,
		UserIDs: nil,
	}
	for _, item := range data.List {
		resp.UserIDs = append(resp.UserIDs, item.CreatorID, item.UpdaterID)
	}
	return resp, nil
}

// DeleteExportRecord DELETE /api/apim/export/{id}
func (p *provider) DeleteExportRecord(ctx context.Context, req *pb.DeleteExportRecordReq) (*pb.Empty, error) {
	if err := p.DB.Where("id = ?", req.Id).Delete(new(model.APIMExportRecord)).Error; err != nil {
		p.l.WithError(err).WithField("id", req.Id).Infoln("failed to Delete")
		return nil, err
	}
	return new(pb.Empty), nil
}
