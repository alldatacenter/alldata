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

package releaseTable

import (
	"context"
	"fmt"

	"github.com/pkg/errors"
	"google.golang.org/grpc/metadata"

	"github.com/erda-project/erda-infra/pkg/transport"
	gallerypb "github.com/erda-project/erda-proto-go/apps/gallery/pb"
	"github.com/erda-project/erda-proto-go/core/dicehub/release/pb"
	"github.com/erda-project/erda/internal/apps/gallery/types"
	"github.com/erda-project/erda/pkg/http/httputil"
)

func (r *ComponentReleaseTable) putOnRelease(ctx context.Context, releaseID string) error {
	userID := r.sdk.Identity.UserID
	orgID := r.sdk.Identity.OrgID
	projectID := r.State.ProjectID

	org, err := r.bdl.GetOrg(orgID)
	if err != nil {
		return err
	}
	project, err := r.bdl.GetProject(uint64(projectID))
	if err != nil {
		return err
	}

	ctx = transport.WithHeader(ctx, metadata.New(map[string]string{httputil.InternalHeader: "dop"}))
	getReleaseResp, err := r.svc.GetRelease(ctx, &pb.ReleaseGetRequest{ReleaseID: releaseID})
	if err != nil {
		return err
	}
	release := getReleaseResp.Data
	if release.OpusID != "" || release.OpusVersionID != "" {
		return errors.Errorf("release %s is already in gallery", release.ReleaseID)
	}

	req := &gallerypb.PutOnArtifactsReq{
		OrgID:          uint32(release.OrgID),
		UserID:         userID,
		Name:           project.Name,
		Version:        release.Version,
		DisplayName:    project.DisplayName,
		Summary:        project.Desc,
		LogoURL:        project.Logo,
		Desc:           project.Desc,
		IsOpenSourced:  false,
		IsDownloadable: true,
		DownloadURL:    fmt.Sprintf("/api/%s/releases/%s/actions/download", org.Name, release.ReleaseID),
		Readme: []*gallerypb.Readme{
			{
				Lang:     types.LangUnknown.String(),
				LangName: types.LangUnknown.String(),
				Text:     release.Changelog,
			},
		},
		Installation: &gallerypb.ArtifactsInstallation{ReleaseID: release.ReleaseID},
	}

	_, err = r.svc.PutOnRelease(ctx, &pb.ReleasePutOnRequest{Req: req})
	return err
}

func (r *ComponentReleaseTable) putOffRelease(ctx context.Context, releaseID string) error {
	userID := r.sdk.Identity.UserID

	ctx = transport.WithHeader(ctx, metadata.New(map[string]string{httputil.InternalHeader: "dop"}))
	getReleaseResp, err := r.svc.GetRelease(ctx, &pb.ReleaseGetRequest{ReleaseID: releaseID})
	if err != nil {
		return err
	}
	release := getReleaseResp.Data
	if release.OpusID == "" || release.OpusVersionID == "" {
		return errors.Errorf("release %s is not in gallery", release.ReleaseID)
	}

	req := &gallerypb.PutOffArtifactsReq{
		OrgID:     uint32(release.OrgID),
		UserID:    userID,
		OpusID:    release.OpusID,
		VersionID: release.OpusVersionID,
	}

	_, err = r.svc.PutOffRelease(ctx, &pb.ReleasePutOffRequest{Req: req, ReleaseID: releaseID})
	return err
}
