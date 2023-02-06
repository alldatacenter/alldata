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

package release

import (
	"context"

	"github.com/pkg/errors"

	commonPb "github.com/erda-project/erda-proto-go/common/pb"
	"github.com/erda-project/erda-proto-go/core/dicehub/release/pb"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/release/db"
)

type opus struct {
	d *db.OpusDB
}

func (o *opus) PutOnArtifacts(ctx context.Context, req *pb.PutOnArtifactsReq) (*commonPb.VoidResponse, error) {
	if req.GetOrgID() == 0 {
		return nil, errors.New("invalid orgID")
	}
	if req.GetOrgName() == "" {
		return nil, errors.New("invalid orgName")
	}
	var opus = db.ReleaseOpus{
		Common: db.Common{
			OrgID:     req.GetOrgID(),
			OrgName:   req.GetOrgName(),
			CreatorID: req.GetUserID(),
			UpdaterID: req.GetUserID(),
		},
		ReleaseID:     req.GetReleaseID(),
		OpusID:        req.GetOpusID(),
		OpusVersionID: req.GetOpusVersionID(),
	}
	if err := o.d.CreateOpus(&opus); err != nil {
		return nil, errors.Wrap(err, "failed to CreateOpus")
	}
	return new(commonPb.VoidResponse), nil
}

func (o *opus) PutOffArtifacts(ctx context.Context, req *pb.PutOffArtifactsReq) (*commonPb.VoidResponse, error) {
	if req.GetOrgID() == 0 {
		return nil, errors.New("invalid orgID")
	}
	if err := o.d.DeleteOpusByReleaseID(req.GetOrgID(), req.GetReleaseID()); err != nil {
		return nil, errors.Wrap(err, "failed to DeleteOpusByReleaseID")
	}
	return new(commonPb.VoidResponse), nil
}

func (o *opus) ListArtifacts(ctx context.Context, req *pb.ListArtifactsReq) (*pb.ListArtifactsResp, error) {
	if req.GetOrgID() == 0 {
		return nil, errors.New("invalid orgID")
	}
	total, opuses, err := o.d.QueryReleaseOpus(req.GetOrgID(), req.GetReleaseIDs(), int(req.GetPageNo()), int(req.GetPageSize()))
	if err != nil {
		return nil, errors.Wrap(err, "failed to QueryReleaseOpus")
	}
	var resp = &pb.ListArtifactsResp{
		Total: uint32(total),
		Data:  make(map[string]*pb.ListArtifactsRespItem),
	}
	for _, opus := range opuses {
		resp.Data[opus.ReleaseID] = &pb.ListArtifactsRespItem{
			OpusID:        opus.OpusID,
			OpusVersionID: opus.OpusVersionID,
			ReleaseID:     opus.ReleaseID,
		}
	}
	return resp, nil
}
