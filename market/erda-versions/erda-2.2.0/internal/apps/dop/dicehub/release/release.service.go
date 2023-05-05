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
	"archive/zip"
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"path/filepath"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/coreos/etcd/clientv3"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"google.golang.org/protobuf/types/known/timestamppb"
	"gopkg.in/yaml.v2"

	gallerypb "github.com/erda-project/erda-proto-go/apps/gallery/pb"
	"github.com/erda-project/erda-proto-go/core/dicehub/release/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	extensiondb "github.com/erda-project/erda/internal/apps/dop/dicehub/extension/db"
	imagedb "github.com/erda-project/erda/internal/apps/dop/dicehub/image/db"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/registry"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/release/db"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/service/apierrors"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/service/release_rule"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/crypto/uuid"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/strutil"
	"github.com/erda-project/erda/pkg/template"
)

type ReleaseService struct {
	p               *provider
	db              *db.ReleaseConfigDB
	labelRelationDB *db.LabelRelationConfigDB
	imageDB         *imagedb.ImageConfigDB
	extensionDB     *extensiondb.ExtensionConfigDB
	bdl             *bundle.Bundle
	opus            pb.OpusServer
	gallery         gallerypb.GalleryServer
	Etcd            *clientv3.Client
	Config          *releaseConfig
	ReleaseRule     *release_rule.ReleaseRule
}

// CreateRelease POST /api/releases release create release
func (s *ReleaseService) CreateRelease(ctx context.Context, req *pb.ReleaseCreateRequest) (*pb.ReleaseCreateResponseData, error) {
	var l = logrus.WithField("func", "*Endpoint.CreateRelease")
	orgID, err := getPermissionHeader(ctx)
	if err != nil {
		return nil, apierrors.ErrCreateRelease.NotLogin()
	}

	if req == nil {
		return nil, apierrors.ErrCreateRelease.MissingParameter("body")
	}

	// 如果没有传 version, 则查找规则列表, 如果当前分支能匹配上某个规则, 则将 version 生成出来
	l.WithFields(map[string]interface{}{
		"releaseRequest.Version":          req.Version,
		"releaseRequest.IsStable":         req.IsStable,
		"releaseRequest.IsProjectRelease": req.IsProjectRelease,
	}).Infoln("releaseRequest parameters")
	if req.Version == "" {
		branch, ok := req.Labels["gitBranch"]
		if !ok {
			return nil, apierrors.ErrCreateRelease.InvalidParameter("no gitBranch label")
		}
		rules, apiError := s.ReleaseRule.List(&apistructs.CreateUpdateDeleteReleaseRuleRequest{
			ProjectID: uint64(req.ProjectID),
		})
		if apiError != nil {
			return nil, apiError
		}
		for _, rule := range rules.List {
			l.WithField("rule pattern", rule.Pattern).WithField("is_enabled", rule.IsEnabled).Debugln()
			if rule.Match(branch) && strutil.PrefixWithSemVer(filepath.Base(branch)) {
				req.Version = filepath.Base(branch) + "+" + time.Now().Format("20060102150405")
				req.IsStable = true
				break
			}
		}
	}
	// 如果没传 IsStable 或 IsStable==false, 则 version 非空时 IsStable=true
	if !req.IsStable {
		req.IsStable = req.Version != ""
	}
	// 项目级 release 一定是 Stable
	if req.IsProjectRelease {
		req.IsStable = true
	}

	if req.OrgID == 0 {
		req.OrgID = orgID
	}

	if req.ProjectID == 0 {
		return nil, apierrors.ErrCreateRelease.MissingParameter("projectID")
	}

	if req.ProjectName == "" {
		project, err := s.bdl.GetProject(uint64(req.ProjectID))
		if err != nil {
			return nil, apierrors.ErrCreateRelease.InternalError(err)
		}
		req.ProjectName = project.Name
	}

	if !req.IsProjectRelease && req.ReleaseName == "" {
		return nil, apierrors.ErrCreateRelease.MissingParameter("releaseName")
	}

	identityInfo, err := getIdentityInfo(ctx)
	if err != nil {
		return nil, apierrors.ErrCreateRelease.NotLogin()
	}
	if !identityInfo.IsInternalClient() {
		if !req.IsProjectRelease {
			return nil, apierrors.ErrCreateRelease.InvalidParameter("can not create application release manually")
		}
		hasAccess, err := s.hasWriteAccess(identityInfo, req.ProjectID, true, 0)
		if err != nil {
			return nil, apierrors.ErrCreateRelease.InternalError(err)
		}
		if !hasAccess {
			return nil, apierrors.ErrCreateRelease.AccessDenied()
		}
	}
	logrus.Debugf("creating release...request body: %v\n", req)
	// create Release
	releaseID, err := s.Create(req)
	if err != nil {
		return nil, apierrors.ErrCreateRelease.InternalError(err)
	}

	if !identityInfo.IsInternalClient() {
		go func() {
			if err := s.audit(auditParams{
				orgID:        orgID,
				projectID:    req.ProjectID,
				userID:       identityInfo.UserID,
				templateName: string(apistructs.CreateProjectTemplate),
				ctx: map[string]interface{}{
					"version":   req.Version,
					"releaseId": releaseID,
				},
			}); err != nil {
				logrus.Errorf("failed to create audit event for creating project release")
			}
		}()
	}

	respBody := &pb.ReleaseCreateResponseData{
		ReleaseID: releaseID,
	}

	return respBody, nil
}

func (s *ReleaseService) UploadRelease(ctx context.Context, req *pb.ReleaseUploadRequest) (*pb.ReleaseUploadResponse, error) {
	orgID, err := getPermissionHeader(ctx)
	if err != nil {
		return nil, apierrors.ErrCreateRelease.NotLogin()
	}

	if req == nil {
		return nil, apierrors.ErrCreateRelease.MissingParameter("body")
	}

	if req.OrgID == 0 {
		req.OrgID = orgID
	}

	if req.DiceFileID == "" {
		return nil, apierrors.ErrCreateRelease.MissingParameter("diceFileID")
	}

	if req.ProjectID == 0 {
		return nil, apierrors.ErrCreateRelease.MissingParameter("projectID")
	}

	if req.ProjectName == "" {
		project, err := s.bdl.GetProject(uint64(req.ProjectID))
		if err != nil {
			return nil, apierrors.ErrCreateRelease.InternalError(err)
		}
		req.ProjectName = project.Name
	}

	identityInfo, err := getIdentityInfo(ctx)
	if err != nil {
		return nil, apierrors.ErrCreateRelease.NotLogin()
	}
	if !identityInfo.IsInternalClient() {
		hasAccess, err := s.hasWriteAccess(identityInfo, req.ProjectID, true, 0)
		if err != nil {
			return nil, apierrors.ErrCreateRelease.InternalError(err)
		}
		if !hasAccess {
			return nil, apierrors.ErrCreateRelease.AccessDenied()
		}
	}

	file, err := s.bdl.DownloadDiceFile(req.DiceFileID)
	if err != nil {
		return nil, apierrors.ErrCreateRelease.InternalError(err)
	}
	defer file.Close()

	version, releaseID, err := s.CreateByFile(req, file)
	if err != nil {
		return nil, apierrors.ErrCreateRelease.InternalError(err)
	}

	if err := s.bdl.DeleteDiceFile(req.DiceFileID); err != nil {
		logrus.Errorf("failed to delete diceFile %s", req.DiceFileID)
	}

	if !identityInfo.IsInternalClient() {
		go func() {
			if err := s.audit(auditParams{
				orgID:        orgID,
				projectID:    req.ProjectID,
				userID:       identityInfo.UserID,
				templateName: string(apistructs.CreateProjectTemplate),
				ctx: map[string]interface{}{
					"version":   version,
					"releaseId": releaseID,
				},
			}); err != nil {
				logrus.Errorf("failed to create audit event for creating project release")
			}
		}()
	}

	return &pb.ReleaseUploadResponse{
		Data: &pb.ReleaseCreateResponseData{
			ReleaseID: releaseID,
		},
	}, nil
}

func (s *ReleaseService) ParseReleaseFile(ctx context.Context, req *pb.ParseReleaseFileRequest) (*pb.ParseReleaseFileResponse, error) {
	_, err := getPermissionHeader(ctx)
	if err != nil {
		return nil, apierrors.ErrParseReleaseFile.NotLogin()
	}

	if req.DiceFileID == "" {
		return nil, apierrors.ErrParseReleaseFile.MissingParameter("diceFileID")
	}

	file, err := s.bdl.DownloadDiceFile(req.DiceFileID)
	if err != nil {
		return nil, apierrors.ErrCreateRelease.InternalError(err)
	}
	defer file.Close()

	metadata, err := parseMetadata(file)
	if err != nil {
		return nil, apierrors.ErrParseReleaseFile.InternalError(err)
	}
	return &pb.ParseReleaseFileResponse{
		Data: &pb.ParseReleaseFileResponseData{
			Version: metadata.Version,
		},
	}, nil
}

func (s *ReleaseService) UpdateRelease(ctx context.Context, req *pb.ReleaseUpdateRequest) (*pb.ReleaseUpdateResponse, error) {
	orgID, err := getPermissionHeader(ctx)
	if err != nil {
		return nil, apierrors.ErrUpdateRelease.NotLogin()
	}

	// Check releaseId if exist in path or not
	releaseID := req.ReleaseID
	if releaseID == "" {
		return nil, apierrors.ErrUpdateRelease.MissingParameter("releaseId")
	}

	identityInfo, err := getIdentityInfo(ctx)
	if err != nil {
		return nil, apierrors.ErrUpdateRelease.NotLogin()
	}
	release, err := s.db.GetRelease(releaseID)
	if err != nil {
		logrus.Errorf("failed to get release %s, %v", releaseID, err)
		return nil, apierrors.ErrUpdateRelease.InternalError(err)
	}
	if !identityInfo.IsInternalClient() {
		hasAccess, err := s.hasWriteAccess(identityInfo, req.ProjectID, release.IsProjectRelease, release.ApplicationID)
		if err != nil {
			return nil, apierrors.ErrUpdateRelease.InternalError(err)
		}
		if !hasAccess {
			return nil, apierrors.ErrUpdateRelease.AccessDenied()
		}
	}
	logrus.Infof("update release info: %+v", req)

	if err := s.Update(orgID, releaseID, req); err != nil {
		return nil, apierrors.ErrUpdateRelease.InternalError(err)
	}

	if !identityInfo.IsInternalClient() {
		releaseType := "application"
		templateName := apistructs.UpdateAppReleaseTemplate
		if release.IsProjectRelease {
			releaseType = "project"
			templateName = apistructs.UpdateProjectReleaseTemplate
		}
		go func() {
			if err := s.audit(auditParams{
				orgID:        orgID,
				projectID:    req.ProjectID,
				userID:       identityInfo.UserID,
				templateName: string(templateName),
				ctx: map[string]interface{}{
					"version":   release.Version,
					"releaseId": releaseID,
				},
			}); err != nil {
				logrus.Errorf("failed to create audit event for updating %s release", releaseType)
			}
		}()
	}

	return &pb.ReleaseUpdateResponse{
		Data: "Update succ",
	}, nil
}

func (s *ReleaseService) UpdateReleaseReference(ctx context.Context, req *pb.ReleaseReferenceUpdateRequest) (*pb.ReleaseDataResponse, error) {
	orgID, err := getPermissionHeader(ctx)
	if err != nil {
		return nil, apierrors.ErrUpdateRelease.NotLogin()
	}

	// Check releaseId if exist in path or not
	releaseID := req.ReleaseID
	if releaseID == "" {
		return nil, apierrors.ErrUpdateRelease.MissingParameter("releaseId")
	}

	// Only update reference
	if req == nil {
		return nil, apierrors.ErrUpdateRelease.MissingParameter("body")
	}

	updateReferenceRequest := req

	if err := s.UpdateReference(orgID, releaseID, updateReferenceRequest); err != nil {
		return nil, apierrors.ErrUpdateRelease.InternalError(err)
	}

	return &pb.ReleaseDataResponse{Data: "Update succ"}, nil
}

func (s *ReleaseService) GetIosPlist(ctx context.Context, req *pb.GetIosPlistRequest) (*pb.GetIosPlistResponse, error) {
	orgID, err := getPermissionHeader(ctx)
	if err != nil {
		return nil, apierrors.ErrUpdateRelease.NotLogin()
	}
	releaseID := req.ReleaseID
	if releaseID == "" {
		return nil, apierrors.ErrGetIosPlist.MissingParameter("releaseId")
	}

	plist, err := s.GetIosPlistService(orgID, releaseID)
	if err != nil {
		return nil, apierrors.ErrGetIosPlist.NotFound()
	}
	fmt.Println(plist)
	return &pb.GetIosPlistResponse{Data: plist}, nil
}

func (s *ReleaseService) GetRelease(ctx context.Context, req *pb.ReleaseGetRequest) (*pb.ReleaseGetResponse, error) {
	orgID, err := getPermissionHeader(ctx)
	if err != nil {
		logrus.Errorf("failed to get orgID: %d", orgID)
		return nil, apierrors.ErrGetRelease.NotLogin()
	}

	releaseID := req.ReleaseID
	if releaseID == "" {
		return nil, apierrors.ErrGetRelease.MissingParameter("releaseId")
	}
	logrus.Infof("getting release...releaseId: %s\n", releaseID)

	identityInfo, err := getIdentityInfo(ctx)
	if err != nil {
		logrus.Errorf("failed to get identityInfo, %v", err)
		return nil, apierrors.ErrGetRelease.NotLogin()
	}
	if !identityInfo.IsInternalClient() {
		release, err := s.db.GetRelease(releaseID)
		if err != nil {
			return nil, apierrors.ErrGetRelease.InternalError(err)
		}
		hasAccess, err := s.hasReadAccess(identityInfo, release.ProjectID)
		if err != nil {
			return nil, apierrors.ErrGetRelease.InternalError(err)
		}
		if !hasAccess {
			return nil, apierrors.ErrGetRelease.AccessDenied()
		}
	}

	resp, err := s.Get(orgID, releaseID)
	if err != nil {
		return nil, apierrors.ErrGetRelease.InternalError(err)
	}
	return &pb.ReleaseGetResponse{Data: resp, UserIDs: []string{resp.UserID}}, nil
}

func (s *ReleaseService) DeleteRelease(ctx context.Context, req *pb.ReleaseDeleteRequest) (*pb.ReleaseDeleteResponse, error) {
	orgID, err := getPermissionHeader(ctx)
	if err != nil {
		return nil, apierrors.ErrDeleteRelease.NotLogin()
	}

	// Get releaseId
	releaseID := req.ReleaseID
	if releaseID == "" {
		return nil, apierrors.ErrDeleteRelease.MissingParameter("releaseId")
	}

	identityInfo, err := getIdentityInfo(ctx)
	if err != nil {
		return nil, apierrors.ErrCreateRelease.NotLogin()
	}
	release, err := s.db.GetRelease(releaseID)
	if err != nil {
		return nil, apierrors.ErrDeleteRelease.InternalError(err)
	}
	if !identityInfo.IsInternalClient() {
		hasAccess, err := s.hasWriteAccess(identityInfo, release.ProjectID, release.IsProjectRelease, release.ApplicationID)
		if err != nil {
			return nil, apierrors.ErrDeleteRelease.InternalError(err)
		}
		if !hasAccess {
			return nil, apierrors.ErrDeleteRelease.AccessDenied()
		}
	}

	logrus.Infof("deleting release...releaseId: %s\n", releaseID)

	opuses, err := s.opus.ListArtifacts(ctx, &pb.ListArtifactsReq{OrgID: uint32(orgID), ReleaseIDs: []string{req.ReleaseID}})
	if err := s.Delete(orgID, opuses.Data, releaseID); err != nil {
		return nil, apierrors.ErrDeleteRelease.InternalError(err)
	}

	releaseType := "application"
	templateName := apistructs.DeleteAppReleaseTemplate
	if release.IsProjectRelease {
		releaseType = "project"
		templateName = apistructs.DeleteProjectReleaseTemplate
	}
	go func() {
		if err := s.audit(auditParams{
			orgID:        orgID,
			projectID:    release.ProjectID,
			userID:       identityInfo.UserID,
			templateName: string(templateName),
			ctx: map[string]interface{}{
				"version":   release.Version,
				"releaseId": releaseID,
			},
		}); err != nil {
			logrus.Errorf("failed to create audit event for deleting %s release, %v", releaseType, err)
		}
	}()

	return &pb.ReleaseDeleteResponse{
		Data: "Delete succ",
	}, nil
}

func (s *ReleaseService) DeleteReleases(ctx context.Context, req *pb.ReleasesDeleteRequest) (*pb.ReleasesDeleteResponse, error) {
	orgID, err := getPermissionHeader(ctx)
	if err != nil {
		return nil, apierrors.ErrDeleteRelease.NotLogin()
	}

	if len(req.ReleaseId) == 0 {
		return nil, apierrors.ErrDeleteRelease.InvalidParameter("releaseID can not be empty")
	}
	identityInfo, err := getIdentityInfo(ctx)
	if err != nil {
		return nil, apierrors.ErrDeleteRelease.NotLogin()
	}
	releases, err := s.db.GetReleases(req.ReleaseId)
	if err != nil {
		return nil, apierrors.ErrDeleteRelease.InternalError(err)
	}
	if len(releases) == 0 {
		return nil, apierrors.ErrDeleteRelease.InternalError(errors.New("release not found"))
	}
	if !identityInfo.IsInternalClient() {
		for i := range releases {
			hasAccess, err := s.hasWriteAccess(identityInfo, req.ProjectId, releases[i].IsProjectRelease, releases[i].ApplicationID)
			if err != nil {
				return nil, apierrors.ErrDeleteRelease.InternalError(err)
			}
			if !hasAccess {
				return nil, apierrors.ErrDeleteRelease.AccessDenied()
			}
		}
	}

	opuses, err := s.opus.ListArtifacts(ctx, &pb.ListArtifactsReq{OrgID: uint32(orgID), ReleaseIDs: req.ReleaseId})
	if err := s.Delete(orgID, opuses.Data, req.ReleaseId...); err != nil {
		return nil, apierrors.ErrDeleteRelease.InternalError(err)
	}

	var versionList []string
	for _, release := range releases {
		versionList = append(versionList, release.Version)
	}

	go func() {
		templateName := ""
		auditCtx := map[string]interface{}{}
		if len(req.ReleaseId) == 1 {
			templateName = string(apistructs.DeleteAppReleaseTemplate)
			if releases[0].IsProjectRelease {
				templateName = string(apistructs.DeleteProjectReleaseTemplate)
			}
			auditCtx["version"] = releases[0].Version
			auditCtx["releaseId"] = releases[0].ReleaseID
		} else {
			templateName = string(apistructs.BatchDeleteAppReleaseTemplate)
			if req.IsProjectRelease {
				templateName = string(apistructs.BatchDeleteProjectReleaseTemplate)
			}
			auditCtx["versionList"] = strings.Join(versionList, ", ")
		}
		if err := s.audit(auditParams{
			orgID:        orgID,
			projectID:    req.ProjectId,
			userID:       identityInfo.UserID,
			templateName: templateName,
			ctx:          auditCtx,
		}); err != nil {
			logrus.Errorf("failed to create audit event for deleting release, %v", err)
		}
	}()

	return &pb.ReleasesDeleteResponse{
		Data: "Delete succ",
	}, nil
}

func (s *ReleaseService) ListRelease(ctx context.Context, req *pb.ReleaseListRequest) (*pb.ReleaseListResponse, error) {
	identityInfo, err := getIdentityInfo(ctx)
	if err != nil {
		return nil, apierrors.ErrListRelease.NotLogin()
	}

	if req.PageSize == 0 {
		req.PageSize = 20
	}
	if req.PageNo == 0 {
		req.PageNo = 1
	}
	params := req

	orgID, _ := getPermissionHeader(ctx)

	if !identityInfo.IsInternalClient() {
		if orgID == 0 {
			return nil, apierrors.ErrListRelease.NotLogin()
		}

		var (
			req      apistructs.PermissionCheckRequest
			permResp *apistructs.PermissionCheckResponseData
			access   bool
		)

		if !access {
			req = apistructs.PermissionCheckRequest{
				UserID:   identityInfo.UserID,
				Scope:    apistructs.OrgScope,
				ScopeID:  uint64(orgID),
				Resource: "release",
				Action:   apistructs.ListAction,
			}

			if permResp, err = s.bdl.CheckPermission(&req); err != nil || !permResp.Access {
				return nil, apierrors.ErrListRelease.AccessDenied()
			}
		}
	}

	if req.GetFrom() == "gallery" {
		artifacts, err := s.opus.ListArtifacts(ctx, &pb.ListArtifactsReq{
			OrgID:  uint32(orgID),
			UserID: identityInfo.UserID,
		})
		if err != nil {
			logrus.WithError(err).Errorln("failed to ListArtifacts")
			return nil, errors.Wrap(err, "failed to ListArtifacts")
		}
		if len(artifacts.Data) == 0 {
			return &pb.ReleaseListResponse{}, nil
		}
		var releaseIDs []string
		for k := range artifacts.Data {
			releaseIDs = append(releaseIDs, k)
		}
		req.ReleaseID = strings.Join(releaseIDs, ",")
		req.ProjectID = 0
	}
	resp, err := s.List(ctx, orgID, params)
	if err != nil {
		return nil, apierrors.ErrListRelease.InternalError(err)
	}
	userIDs := make([]string, 0, len(resp.List))
	for _, v := range resp.List {
		userIDs = append(userIDs, v.UserID)
	}

	return &pb.ReleaseListResponse{
		Data:    resp,
		UserIDs: userIDs,
	}, nil
}

func (s *ReleaseService) ListReleaseName(ctx context.Context, req *pb.ListReleaseNameRequest) (*pb.ListReleaseNameResponse, error) {
	orgID, err := apis.GetIntOrgID(ctx)
	if err != nil {
		return nil, apierrors.ErrListRelease.NotLogin()
	}

	// get applicationID
	applicationIDStr := GetApplicationId(ctx)
	if applicationIDStr == "" {
		return nil, apierrors.ErrListRelease.MissingParameter("applicationId")
	}
	applicationID, err := strconv.ParseInt(applicationIDStr, 10, 64)
	if err != nil { // Prevent SQL injection
		return nil, apierrors.ErrListRelease.InvalidParameter("applicationId")
	}

	releaseNames, err := s.GetReleaseNamesByApp(orgID, applicationID)
	if err != nil {
		return nil, apierrors.ErrListRelease.InternalError(err)
	}

	return &pb.ListReleaseNameResponse{Data: releaseNames}, nil
}

func (s *ReleaseService) GetLatestReleases(ctx context.Context, req *pb.GetLatestReleasesRequest) (*pb.GetLatestReleasesResponse, error) {
	// Check the legitimacy of the projectID
	projectIDStr := GetProjectID(ctx)
	if projectIDStr == "" {
		return nil, apierrors.ErrListRelease.MissingParameter("projectId")
	}
	projectID, err := strutil.Atoi64(projectIDStr)
	if err != nil {
		return nil, apierrors.ErrListRelease.InvalidParameter(err)
	}

	// Check the legitimacy of the version
	version := GetVersion(ctx)
	if version == "" {
		return nil, apierrors.ErrListRelease.MissingParameter("version")
	}

	latests, err := s.GetLatestReleasesByProjectAndVersion(projectID, version)
	if err != nil {
		return nil, apierrors.ErrListRelease.InternalError(err)
	}
	resp, err := batchConvert(latests)
	if err != nil {
		return nil, apierrors.ErrListRelease.InternalError(err)
	}
	return &pb.GetLatestReleasesResponse{Data: resp}, nil
}

func (s *ReleaseService) ReleaseGC(ctx context.Context, req *pb.ReleaseGCRequest) (*pb.ReleaseDataResponse, error) {
	logrus.Infof("trigger release gc by api[ POST /gc ]!")
	go func() {
		if err := s.RemoveDeprecatedsReleases(time.Now()); err != nil {
			logrus.Warnf("remove deprecated release error: %v", err)
		}
	}()

	return &pb.ReleaseDataResponse{Data: "trigger release gc success"}, nil
}

func (s *ReleaseService) ToFormalReleases(ctx context.Context, req *pb.FormalReleasesRequest) (*pb.FormalReleasesResponse, error) {
	orgID, err := getPermissionHeader(ctx)
	if err != nil {
		return nil, apierrors.ErrFormalRelease.NotLogin()
	}

	identityInfo, err := getIdentityInfo(ctx)
	if err != nil {
		return nil, apierrors.ErrFormalRelease.NotLogin()
	}
	if !identityInfo.IsInternalClient() {
		hasAccess, err := s.hasWriteAccess(identityInfo, req.ProjectId, true, 0)
		if err != nil {
			return nil, apierrors.ErrFormalRelease.InternalError(err)
		}
		if !hasAccess {
			return nil, apierrors.ErrFormalRelease.AccessDenied()
		}
	}
	if err := s.ToFormal(req.ReleaseId); err != nil {
		return nil, apierrors.ErrFormalRelease.InternalError(err)
	}

	releases, err := s.db.GetReleases(req.ReleaseId)
	if err != nil {
		logrus.Errorf("failed to get releases %v, %v", req.ReleaseId, err)
	} else {
		var versionList []string
		for _, release := range releases {
			versionList = append(versionList, release.Version)
		}
		go func() {
			templateName := ""
			auditCtx := map[string]interface{}{}
			if len(req.ReleaseId) == 1 {
				templateName = string(apistructs.FormalAppReleaseTemplate)
				if releases[0].IsProjectRelease {
					templateName = string(apistructs.FormalProjectReleaseTemplate)
				}
				auditCtx["version"] = releases[0].Version
				auditCtx["releaseId"] = releases[0].ReleaseID
			} else {
				templateName = string(apistructs.BatchFormalReleaseAppTemplate)
				if req.IsProjectRelease {
					templateName = string(apistructs.BatchFormalReleaseProjectTemplate)
				}
				auditCtx["versionList"] = strings.Join(versionList, ", ")
			}
			if err := s.audit(auditParams{
				orgID:        orgID,
				projectID:    req.ProjectId,
				userID:       identityInfo.UserID,
				templateName: templateName,
				ctx:          auditCtx,
			}); err != nil {
				logrus.Errorf("failed to create audit event for formaling release, %v", err)
			}
		}()
	}
	return &pb.FormalReleasesResponse{
		Data: "Formal succ",
	}, nil
}

func (s *ReleaseService) ToFormalRelease(ctx context.Context, req *pb.FormalReleaseRequest) (*pb.FormalReleaseResponse, error) {
	orgID, err := getPermissionHeader(ctx)
	if err != nil {
		return nil, apierrors.ErrFormalRelease.NotLogin()
	}

	releaseID := req.ReleaseId
	if releaseID == "" {
		return nil, apierrors.ErrFormalRelease.MissingParameter("releaseId")
	}

	identityInfo, err := getIdentityInfo(ctx)
	if err != nil {
		return nil, apierrors.ErrFormalRelease.NotLogin()
	}

	release, err := s.db.GetRelease(releaseID)
	if err != nil {
		return nil, apierrors.ErrFormalRelease.InternalError(err)
	}
	if !release.IsStable {
		return nil, apierrors.ErrFormalRelease.InvalidParameter("temp release can not be formaled")
	}
	if !identityInfo.IsInternalClient() {
		hasAccess, err := s.hasWriteAccess(identityInfo, release.ProjectID, true, 0)
		if err != nil {
			return nil, apierrors.ErrFormalRelease.InternalError(err)
		}
		if !hasAccess {
			return nil, apierrors.ErrFormalRelease.AccessDenied()
		}
	}
	if err := s.ToFormal([]string{releaseID}); err != nil {
		return nil, apierrors.ErrFormalRelease.InternalError(err)
	}

	releaseType := "application"
	templateName := apistructs.FormalAppReleaseTemplate
	if release.IsProjectRelease {
		releaseType = "project"
		templateName = apistructs.FormalProjectReleaseTemplate
	}
	go func() {
		if err := s.audit(auditParams{
			orgID:        orgID,
			projectID:    release.ProjectID,
			userID:       identityInfo.UserID,
			templateName: string(templateName),
			ctx: map[string]interface{}{
				"version":   release.Version,
				"releaseId": releaseID,
			},
		}); err != nil {
			logrus.Errorf("failed to create audit event for formaling %s release, %v", releaseType, err)
		}
	}()

	return &pb.FormalReleaseResponse{
		Data: "Formal succ",
	}, nil
}

func (s *ReleaseService) CheckVersion(ctx context.Context, req *pb.CheckVersionRequest) (*pb.CheckVersionResponse, error) {
	_, err := getPermissionHeader(ctx)
	if err != nil {
		return nil, apierrors.ErrCheckReleaseVersion.NotLogin()
	}

	var appID int64 = 0
	if !req.IsProjectRelease {
		appID = req.AppID
		if appID == 0 {
			return nil, apierrors.ErrCheckReleaseVersion.MissingParameter("appID")
		}
	}
	orgID := req.OrgID
	if orgID == 0 {
		return nil, apierrors.ErrCheckReleaseVersion.MissingParameter("orgID")
	}
	projectID := req.ProjectID
	if projectID == 0 {
		return nil, apierrors.ErrCheckReleaseVersion.MissingParameter("projectID")
	}
	version := req.Version
	if version == "" {
		return nil, apierrors.ErrCheckReleaseVersion.MissingParameter("version")
	}

	identityInfo, err := getIdentityInfo(ctx)
	if err != nil {
		return nil, apierrors.ErrCheckReleaseVersion.NotLogin()
	}
	if !identityInfo.IsInternalClient() {
		hasAccess, err := s.hasReadAccess(identityInfo, projectID)
		if err != nil {
			return nil, apierrors.ErrCheckReleaseVersion.InternalError(err)
		}
		if !hasAccess {
			return nil, apierrors.ErrCheckReleaseVersion.AccessDenied()
		}
	}

	var releases []db.Release
	if req.IsProjectRelease {
		releases, err = s.db.GetReleasesByProjectAndVersion(orgID, projectID, version)
		if err != nil {
			return nil, apierrors.ErrCheckReleaseVersion
		}
	} else {
		releases, err = s.db.GetReleasesByAppAndVersion(orgID, projectID, appID, version)
		if err != nil {
			return nil, apierrors.ErrCheckReleaseVersion
		}
	}
	return &pb.CheckVersionResponse{
		Data: &pb.CheckVersionResponseData{
			IsUnique: len(releases) == 0,
		},
	}, nil
}

// PutOnRelease puts on release to gallery.
// Internal call only
func (s *ReleaseService) PutOnRelease(ctx context.Context, req *pb.ReleasePutOnRequest) (*pb.ReleasePutOnResponse, error) {
	internalClient := apis.GetHeader(ctx, httputil.InternalHeader)
	if internalClient == "" {
		return nil, apierrors.ErrPutOnRelease.AccessDenied()
	}

	resp, err := s.gallery.PutOnArtifacts(ctx, req.Req)
	if err != nil {
		return nil, apierrors.ErrPutOnRelease.InternalError(err)
	}

	org, err := s.bdl.GetOrg(req.Req.OrgID)
	if err != nil {
		return nil, apierrors.ErrPutOnRelease.InternalError(err)
	}
	_, err = s.opus.PutOnArtifacts(ctx, &pb.PutOnArtifactsReq{
		OrgID:         req.Req.OrgID,
		OrgName:       org.Name,
		UserID:        req.Req.UserID,
		OpusID:        resp.OpusID,
		OpusVersionID: resp.VersionID,
		ReleaseID:     req.Req.Installation.ReleaseID,
	})
	return &pb.ReleasePutOnResponse{}, err
}

func (s *ReleaseService) PutOffRelease(ctx context.Context, req *pb.ReleasePutOffRequest) (*pb.ReleasePutOffResponse, error) {
	internalClient := apis.GetHeader(ctx, httputil.InternalHeader)
	if internalClient == "" {
		return nil, apierrors.ErrPutOnRelease.AccessDenied()
	}

	_, err := s.gallery.PutOffArtifacts(ctx, req.Req)
	if err != nil {
		return nil, apierrors.ErrPutOffRelease.InternalError(err)
	}

	_, err = s.opus.PutOffArtifacts(ctx, &pb.PutOffArtifactsReq{
		OrgID:     req.Req.OrgID,
		UserID:    req.Req.UserID,
		ReleaseID: req.ReleaseID,
	})
	return &pb.ReleasePutOffResponse{}, nil
}

// GetDiceYAML get dice.yml context
func (s *ReleaseService) GetDiceYAML(orgID int64, releaseID string) (string, error) {
	release, err := s.db.GetRelease(releaseID)
	if err != nil {
		return "", err
	}
	if orgID != 0 && release.OrgID != orgID { // when calling internal，orgID is 0
		return "", errors.Errorf("release not found")
	}

	return release.Dice, nil
}

// GetIosPlist Read the download address plist in the ios type release
func (s *ReleaseService) GetIosPlistService(orgID int64, releaseID string) (string, error) {
	release, err := s.db.GetRelease(releaseID)
	if err != nil {
		return "", err
	}
	if orgID != 0 && release.OrgID != orgID { // 内部调用时，orgID为0
		return "", errors.Errorf("release not found")
	}

	releaseData, err := s.convertToReleaseResponse(release)
	if err != nil {
		return "", err
	}
	for _, resource := range releaseData.Resources {
		if ResourceType(resource.Type) == ResourceTypeIOS {
			plistTemplate := `<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
   <key>items</key>
   <array>
       <dict>
           <key>assets</key>
           <array>
               <dict>
                   <key>kind</key>
                   <string>software-package</string>
                   <key>url</key>
                   <string>{{appUrl}}</string>
               </dict>
           </array>
           <key>metadata</key>
           <dict>
               <key>bundle-identifier</key>
               <string>{{bundleId}}</string>
               <key>bundle-version</key>
               <string>{{version}}</string>
               <key>kind</key>
               <string>software</string>
               <key>subtitle</key>
               <string>{{displayName}}</string>
               <key>title</key>
               <string>{{displayName}}</string>
           </dict>
       </dict>
   </array>
</dict>
</plist>`
			bundleId := resource.Meta["bundleId"].GetStringValue()
			version := resource.Meta["version"].GetStringValue()
			displayName := resource.Meta["displayName"].GetStringValue()
			appUrl := resource.URL
			plistContent := template.Render(plistTemplate, map[string]string{
				"bundleId":    bundleId,
				"version":     version,
				"displayName": displayName,
				"appUrl":      appUrl,
			})
			return plistContent, nil
		}
	}
	return "", errors.New("not ios release")
}

// GetReleaseNamesByApp get releaseName list by appID
func (s *ReleaseService) GetReleaseNamesByApp(orgID, appID int64) ([]string, error) {
	// releaseNames := make([]string, 0)
	// for _, item := range releases {
	// 	releaseNames = append(releaseNames, item.ReleaseName)
	// }
	return s.db.GetReleaseNamesByApp(orgID, appID)
}

// GetLatestReleasesByProjectAndVersion get latelest Release by projectID & version
func (s *ReleaseService) GetLatestReleasesByProjectAndVersion(projectID int64, version string) (*[]db.Release, error) {
	appIDs, err := s.db.GetAppIDsByProjectAndVersion(projectID, version)
	if err != nil {
		return nil, err
	}
	latests := make([]db.Release, 0, len(appIDs))
	for _, v := range appIDs {
		release, err := s.db.GetLatestReleaseByAppAndVersion(v, version)
		if err != nil {
			logrus.Warnf("failed to get latest release, (%v)", err)
			continue
		}
		latests = append(latests, *release)
	}

	return &latests, nil
}

// RemoveDeprecatedsReleases Recycling expired release
func (s *ReleaseService) RemoveDeprecatedsReleases(now time.Time) error {
	d, err := time.ParseDuration(strutil.Concat("-", s.Config.MaxTimeReserved, "h")) // one month before, eg: -720h
	if err != nil {
		return err
	}
	before := now.Add(d)

	releases, err := s.db.GetUnReferedReleasesBefore(before)
	if err != nil {
		return err
	}
	for i := range releases {
		release := releases[i]
		if release.Version != "" {
			logrus.Debugf("release %s have been tagged, can't be recycled", release.ReleaseID)
			continue
		}

		images, err := s.imageDB.GetImagesByRelease(release.ReleaseID)
		if err != nil {
			logrus.Warnf(err.Error())
			continue
		}

		deletable := true // if image manifest delete false，release can not be deleted
		for _, image := range images {
			// If there are other releases that refer to this mirror, the mirror manifest cannot be deleted, only the DB meta information is deleted (multiple builds, the same mirror exists)
			count, err := s.imageDB.GetImageCount(release.ReleaseID, image.Image)
			if err != nil {
				logrus.Errorf(err.Error())
				continue
			}
			if count == 0 && release.ClusterName != "" && !strings.HasPrefix(image.Image, AliYunRegistry) {
				if err := registry.DeleteManifests(s.bdl, release.ClusterName, []string{image.Image}); err != nil {
					deletable = false
					logrus.Errorf(err.Error())
					continue
				}
			}

			// Delete image info
			if err := s.imageDB.DeleteImage(int64(image.ID)); err != nil {
				logrus.Errorf("[alert] delete image: %s fail, err: %v", image.Image, err)
			}
			logrus.Infof("deleted image: %s", image.Image)
		}

		if deletable {
			// Delete release info
			if err := s.db.DeleteRelease(release.ReleaseID); err != nil {
				logrus.Errorf("[alert] delete release: %s fail, err: %v", release.ReleaseID, err)
			}
			if err := s.labelRelationDB.DeleteLabelRelations(apistructs.LabelTypeRelease, release.ReleaseID); err != nil {
				logrus.Errorf("failed to delete label relations for release %s, %v", release.ReleaseID, err)
			}
			logrus.Infof("deleted release: %s", release.ReleaseID)
		}
	}
	return nil
}

// Convert 从ReleaseRequest中提取Release元信息, 若为应用级制品, appReleases填nil
func (s *ReleaseService) Convert(releaseRequest *pb.ReleaseCreateRequest, appReleases []db.Release) (*db.Release, error) {
	release := db.Release{
		ReleaseID:        uuid.UUID(),
		ReleaseName:      releaseRequest.ReleaseName,
		Desc:             releaseRequest.Desc,
		Dice:             releaseRequest.Dice,
		Addon:            releaseRequest.Addon,
		Changelog:        releaseRequest.Changelog,
		IsStable:         releaseRequest.IsStable,
		IsFormal:         false,
		IsProjectRelease: releaseRequest.IsProjectRelease,
		Version:          releaseRequest.Version,
		OrgID:            releaseRequest.OrgID,
		ProjectID:        releaseRequest.ProjectID,
		ApplicationID:    releaseRequest.ApplicationID,
		ProjectName:      releaseRequest.ProjectName,
		ApplicationName:  releaseRequest.ApplicationName,
		UserID:           releaseRequest.UserID,
		ClusterName:      releaseRequest.ClusterName,
		CrossCluster:     releaseRequest.CrossCluster,
		CreatedAt:        time.Time{},
		UpdatedAt:        time.Time{},
	}

	if release.ProjectName == "" {
		project, err := s.bdl.GetProject(uint64(release.ProjectID))
		if err != nil {
			return nil, err
		}
		release.ProjectName = project.Name
	}

	if len(releaseRequest.Labels) > 0 {
		labelBytes, err := json.Marshal(releaseRequest.Labels)
		if err != nil {
			return nil, err
		}
		release.Labels = string(labelBytes)
		release.GitBranch = releaseRequest.Labels["gitBranch"]
	}

	if len(releaseRequest.Resources) > 0 {
		resourceBytes, err := json.Marshal(releaseRequest.Resources)
		if err != nil {
			return nil, err
		}
		release.Resources = string(resourceBytes)
	}

	if releaseRequest.IsProjectRelease {
		if len(appReleases) == 0 {
			return nil, errors.New("application release list can not be null for project release when dice yaml is empty")
		}

		selectedApp := make(map[int64]struct{})
		for i := range appReleases {
			if _, ok := selectedApp[appReleases[i].ApplicationID]; ok {
				return nil, errors.New("one application can only be selected once")
			}
			selectedApp[appReleases[i].ApplicationID] = struct{}{}
		}
		release.ApplicationID = 0
		release.ApplicationName = ""
		release.Dice = ""

		modesData, err := json.Marshal(convertPbModesToModes(releaseRequest.Modes))
		if err != nil {
			return nil, errors.Errorf("failed to marshal release list, %v", err)
		}
		release.Modes = string(modesData)
	} else {
		release.IsLatest = true
	}
	return &release, nil
}

func (s *ReleaseService) convertToReleaseResponse(release *db.Release) (*pb.ReleaseGetResponseData, error) {
	var labels map[string]string
	err := json.Unmarshal([]byte(release.Labels), &labels)
	if err != nil {
		labels = make(map[string]string)
	}

	var resources []*pb.ReleaseResource
	err = json.Unmarshal([]byte(release.Resources), &resources)
	if err != nil {
		resources = make([]*pb.ReleaseResource, 0)
	}

	var (
		summary   map[string]*pb.ModeSummary
		addons    []*pb.AddonInfo
		addonYaml string
	)
	addonSet := make(map[string]*diceyml.AddOn)
	if release.IsProjectRelease {
		logrus.Infoln("[DEBUG] start unmarshal modes")
		modes := make(map[string]apistructs.ReleaseDeployMode)
		if err = json.Unmarshal([]byte(release.Modes), &modes); err != nil {
			return nil, errors.Errorf("failed to Unmarshal appReleaseIDs, %v", err)
		}
		logrus.Infoln("[DEBUG] end unmarshal modes")

		var list []string
		for _, mode := range modes {
			for _, applicationList := range mode.ApplicationReleaseList {
				for _, id := range applicationList {
					list = append(list, id)
				}
			}
		}

		logrus.Infoln("[DEBUG] start get releases")
		appReleases, err := s.db.GetReleases(list)
		if err != nil {
			return nil, err
		}
		logrus.Infoln("[DEBUG] end get releases")

		id2Release := make(map[string]*db.Release)
		logrus.Infoln("start parse dice yaml")

		wg := sync.WaitGroup{}
		mux := sync.Mutex{}
		for i := 0; i < len(appReleases); i++ {
			id2Release[appReleases[i].ReleaseID] = &appReleases[i]

			wg.Add(1)
			go func(i int) {
				defer wg.Done()
				if len(appReleases[i].Dice) == 0 {
					return
				}
				dice, err := diceyml.New([]byte(appReleases[i].Dice), true)
				if err != nil {
					logrus.Errorf("failed to parse diceyml for release %s, %v", appReleases[i].ReleaseID, err)
					return
				}
				obj := dice.Obj()
				if obj == nil || obj.AddOns == nil {
					return
				}

				for name, addon := range obj.AddOns {
					version := addon.Options["version"]
					splits := strings.Split(addon.Plan, ":")
					if len(splits) != 2 {
						logrus.Errorf("plan field of addon %s for release %s is invalid", name, appReleases[i].ReleaseID)
						return
					}
					mux.Lock()
					addonSet[strings.Join([]string{splits[0], version, splits[1]}, "_")] = addon
					mux.Unlock()
				}
			}(i)
		}
		wg.Wait()
		logrus.Infoln("[DEBUG] end parse dice yaml")

		summary = make(map[string]*pb.ModeSummary)
		logrus.Infoln("[DEBUG] start make summary")
		for k, mode := range modes {
			modeSummary := &pb.ModeSummary{
				Expose:                 mode.Expose,
				DependOn:               mode.DependOn,
				ApplicationReleaseList: make([]*pb.ReleaseSummaryArray, len(mode.ApplicationReleaseList)),
			}
			for i := 0; i < len(mode.ApplicationReleaseList); i++ {
				modeSummary.ApplicationReleaseList[i] = &pb.ReleaseSummaryArray{
					List: make([]*pb.ApplicationReleaseSummary, len(mode.ApplicationReleaseList[i])),
				}
				for j := 0; j < len(mode.ApplicationReleaseList[i]); j++ {
					id := mode.ApplicationReleaseList[i][j]
					release, ok := id2Release[id]
					if !ok {
						return nil, errors.Errorf("release %s not found", id)
					}
					modeSummary.ApplicationReleaseList[i].List[j] = &pb.ApplicationReleaseSummary{
						ReleaseID:       release.ReleaseID,
						ReleaseName:     release.ReleaseName,
						Version:         release.Version,
						ApplicationID:   release.ApplicationID,
						ApplicationName: release.ApplicationName,
						CreatedAt:       release.CreatedAt.Format("2006/01/02 15:04:05"),
						DiceYml:         release.Dice,
					}
				}
			}
			summary[k] = modeSummary
		}
		logrus.Infoln("[DEBUG] end make summary")

		extensionMap := make(map[string]*extensiondb.Extension)
		if len(addonSet) > 0 {
			logrus.Infoln("[DEBUG] start query extensions")
			extensions, err := s.extensionDB.QueryExtensions(true, "", "")
			if err != nil {
				return nil, errors.Errorf("failed to query extensions, %v", err)
			}
			logrus.Infoln("[DEBUG] end query extensions")

			for i := 0; i < len(extensions); i++ {
				extensionMap[extensions[i].Name] = &extensions[i]
			}
		}

		logrus.Infoln("[DEBUG] start make addon info")
		for _, addon := range addonSet {
			version := addon.Options["version"]
			splits := strings.Split(addon.Plan, ":")
			name := splits[0]
			plan := splits[1]
			if version == "" {
				extensionVersion, err := s.extensionDB.GetExtensionDefaultVersion(name)
				if err != nil {
					return nil, errors.Errorf("failed to get default version for addon %s, %v", name, err)
				}
				version = extensionVersion.Version
			}
			addons = append(addons, &pb.AddonInfo{
				DisplayName: extensionMap[name].DisplayName,
				Plan:        plan,
				Version:     version,
				Category:    extensionMap[name].Category,
				LogoURL:     extensionMap[name].LogoUrl,
			})
		}
		logrus.Infoln("[DEBUG] end make addon info")

		logrus.Infoln("[DEBUG] start marshal addonSet")
		data, err := yaml.Marshal(addonSet)
		if err != nil {
			return nil, errors.Errorf("failed to marshal addonSet, %v", err)
		}
		logrus.Infoln("[DEBUG] end marshal addonSet")
		addonYaml = string(data)
	}

	tags, err := s.getReleaseTags(release.ReleaseID)
	if err != nil {
		return nil, err
	}

	var opusID, opusVersionID string
	if release.IsProjectRelease {
		opuses, err := s.opus.ListArtifacts(context.Background(), &pb.ListArtifactsReq{
			OrgID:      uint32(release.OrgID),
			ReleaseIDs: []string{release.ReleaseID},
		})
		if err != nil {
			logrus.Errorf("failed to get opus for release %s, %v", release.ReleaseID, err)
			return nil, err
		}
		opusInfo := opuses.Data[release.ReleaseID]
		if opusInfo != nil {
			opusID = opusInfo.OpusID
			opusVersionID = opusInfo.OpusVersionID
		}
	}
	respData := &pb.ReleaseGetResponseData{
		ReleaseID:        release.ReleaseID,
		ReleaseName:      release.ReleaseName,
		Diceyml:          release.Dice,
		Desc:             release.Desc,
		Addon:            release.Addon,
		Changelog:        release.Changelog,
		IsStable:         release.IsStable,
		IsFormal:         release.IsFormal,
		IsProjectRelease: release.IsProjectRelease,
		Modes:            summary,
		Resources:        resources,
		Labels:           labels,
		Tags:             tags,
		Version:          release.Version,
		CrossCluster:     release.CrossCluster,
		Reference:        release.Reference,
		OrgID:            release.OrgID,
		ProjectID:        release.ProjectID,
		ApplicationID:    release.ApplicationID,
		ProjectName:      release.ProjectName,
		ApplicationName:  release.ApplicationName,
		UserID:           release.UserID,
		ClusterName:      release.ClusterName,
		CreatedAt:        timestamppb.New(release.CreatedAt),
		UpdatedAt:        timestamppb.New(release.UpdatedAt),
		IsLatest:         release.IsLatest,
		Addons:           addons,
		AddonYaml:        addonYaml,
		OpusID:           opusID,
		OpusVersionID:    opusVersionID,
	}

	if err = respDataReLoadImages(respData); err != nil {
		logrus.WithError(err).Errorln("failed to ReLoadImages")
		return nil, err
	}

	return respData, nil
}

func (s *ReleaseService) getReleaseTags(releaseID string) ([]*pb.Tag, error) {
	lrs, err := s.labelRelationDB.GetLabelRelationsByRef(apistructs.LabelTypeRelease, releaseID)
	if err != nil {
		return nil, errors.Errorf("failed to get label relation, %v", err)
	}
	var tagIDs []uint64
	for i := range lrs {
		tagIDs = append(tagIDs, lrs[i].LabelID)
	}

	tags, err := s.bdl.ListLabelByIDs(tagIDs)
	if err != nil {
		return nil, err
	}

	var pbTags []*pb.Tag
	for _, tag := range tags {
		pbTags = append(pbTags, &pb.Tag{
			CreatedAt: timestamppb.New(tag.CreatedAt.In(time.Local)),
			UpdatedAt: timestamppb.New(tag.UpdatedAt.In(time.Local)),
			Creator:   tag.Creator,
			Id:        tag.ID,
			Color:     tag.Color,
			Name:      tag.Name,
			Type:      string(tag.Type),
			ProjectID: int64(tag.ProjectID),
		})
	}
	return pbTags, nil
}

func respDataReLoadImages(r *pb.ReleaseGetResponseData) error {
	if r.Diceyml == "" {
		return nil
	}
	deployable, err := diceyml.NewDeployable([]byte(r.Diceyml), diceyml.WS_PROD, false)
	if err != nil {
		return err
	}
	var obj = deployable.Obj()
	r.Images = nil
	r.ServiceImages = nil
	for name, service := range obj.Services {
		r.Images = append(r.Images, service.Image)
		r.ServiceImages = append(r.ServiceImages, &pb.ServiceImagePair{
			ServiceName: name,
			Image:       service.Image,
		})
	}
	for name, job := range obj.Jobs {
		r.Images = append(r.Images, job.Image)
		r.ServiceImages = append(r.ServiceImages, &pb.ServiceImagePair{
			ServiceName: name,
			Image:       job.Image,
		})
	}
	return nil
}

// hasReadAccess check whether user has access to get project
func (s *ReleaseService) hasReadAccess(identityInfo apistructs.IdentityInfo, projectID int64) (bool, error) {
	access, err := s.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   identityInfo.UserID,
		Scope:    apistructs.ProjectScope,
		ScopeID:  uint64(projectID),
		Resource: apistructs.ProjectResource,
		Action:   apistructs.GetAction,
	})
	if err != nil {
		return false, err
	}
	if !access.Access {
		return false, nil
	}
	return true, nil
}

// hasWriteAccess check whether user is project owner or project lead
func (s *ReleaseService) hasWriteAccess(identity apistructs.IdentityInfo, projectID int64, isProjectRelease bool, applicationID int64) (bool, error) {
	req := &apistructs.ScopeRoleAccessRequest{
		Scope: apistructs.Scope{
			Type: apistructs.ProjectScope,
			ID:   strconv.FormatInt(projectID, 10),
		},
	}
	rsp, err := s.bdl.ScopeRoleAccess(identity.UserID, req)
	if err != nil {
		return false, err
	}

	hasProjectAccess := false
	for _, role := range rsp.Roles {
		if role == bundle.RoleProjectOwner || role == bundle.RoleProjectLead || role == bundle.RoleProjectPM {
			hasProjectAccess = true
			break
		}
	}

	if isProjectRelease || hasProjectAccess {
		return hasProjectAccess, nil
	}

	req = &apistructs.ScopeRoleAccessRequest{
		Scope: apistructs.Scope{
			Type: apistructs.AppScope,
			ID:   strconv.FormatInt(applicationID, 10),
		},
	}
	rsp, err = s.bdl.ScopeRoleAccess(identity.UserID, req)
	if err != nil {
		logrus.Errorf("failed to check app access for release of app %d, %v", applicationID, err)
		return hasProjectAccess, nil
	}

	hasAppAccess := false
	for _, role := range rsp.Roles {
		if role == bundle.RoleAppOwner || role == bundle.RoleAppLead {
			hasAppAccess = true
			break
		}
	}
	return hasAppAccess, nil
}

func batchConvert(releases *[]db.Release) ([]*pb.GetLatestReleasesResponseData, error) {
	var data []*pb.GetLatestReleasesResponseData
	for _, v := range *releases {
		data = append(data, &pb.GetLatestReleasesResponseData{
			ReleaseID:       v.ReleaseID,
			ReleaseName:     v.ReleaseName,
			Dice:            v.Dice,
			Desc:            v.Desc,
			Addon:           v.Addon,
			Resources:       v.Resources,
			Labels:          v.Labels,
			Version:         v.Version,
			CrossCluster:    v.CrossCluster,
			Reference:       v.Reference,
			OrgID:           v.OrgID,
			ProjectID:       v.ProjectID,
			ApplicationID:   v.ApplicationID,
			ProjectName:     v.ProjectName,
			ApplicationName: v.ApplicationName,
			UserID:          v.UserID,
			ClusterName:     v.ClusterName,
			CreatedAt:       timestamppb.New(v.CreatedAt),
			UpdatedAt:       timestamppb.New(v.UpdatedAt),
		})
	}
	return data, nil
}

func getPermissionHeader(ctx context.Context) (int64, error) {
	orgIDStr := apis.GetOrgID(ctx)
	if orgIDStr == "" {
		return 0, nil
	}
	return strconv.ParseInt(orgIDStr, 10, 64)
}

func getIdentityInfo(ctx context.Context) (apistructs.IdentityInfo, error) {
	userID := apis.GetUserID(ctx)
	internalClient := apis.GetHeader(ctx, httputil.InternalHeader)

	// not login
	if userID == "" && internalClient == "" {
		return apistructs.IdentityInfo{}, errors.Errorf("invalid identity info")
	}

	identity := apistructs.IdentityInfo{
		UserID:         userID,
		InternalClient: internalClient,
	}

	return identity, nil
}

func limitLabelsLength(req *pb.ReleaseCreateRequest) error {
	if len(req.Labels) == 0 {
		return nil
	}
	labelBytes, err := json.Marshal(req.Labels)
	if err != nil {
		return err
	}
	if len([]rune(string(labelBytes))) <= 1000 {
		return nil
	}

	for k, v := range req.Labels {
		runes := []rune(v)
		if len(runes) > 100 {
			req.Labels[k] = string(runes[:100]) + "..."
		}
	}
	return nil
}

func parseMetadata(file io.ReadCloser) (*apistructs.ReleaseMetadata, error) {
	var metadata apistructs.ReleaseMetadata
	buf := bytes.Buffer{}
	if _, err := io.Copy(&buf, file); err != nil {
		return nil, err
	}
	found := false
	r, err := zip.NewReader(bytes.NewReader(buf.Bytes()), int64(buf.Len()))
	if err != nil {
		return nil, err
	}
	for _, f := range r.File {
		rc, err := f.Open()
		if err != nil {
			return nil, err
		}
		buf := bytes.Buffer{}
		if _, err = io.Copy(&buf, rc); err != nil {
			return nil, err
		}

		splits := strings.Split(f.Name, "/")
		if len(splits) == 2 && splits[1] == "metadata.yml" {
			if err := yaml.Unmarshal(buf.Bytes(), &metadata); err != nil {
				return nil, err
			}
			found = true
			break
		}
		if err := rc.Close(); err != nil {
			return nil, err
		}
	}
	if !found {
		return nil, errors.New("invalid file, metadata.yml not found")
	}
	return &metadata, nil
}
