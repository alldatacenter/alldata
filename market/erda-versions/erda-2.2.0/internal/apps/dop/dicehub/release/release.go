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
	"reflect"
	"sort"
	"strings"
	"time"

	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"google.golang.org/protobuf/types/known/timestamppb"
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda-proto-go/core/dicehub/release/pb"
	"github.com/erda-project/erda/apistructs"
	imagedb "github.com/erda-project/erda/internal/apps/dop/dicehub/image/db"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/registry"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/release/db"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/service/apierrors"
	"github.com/erda-project/erda/pkg/crypto/uuid"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/strutil"
)

func (s *ReleaseService) ToFormal(releaseIDs []string) error {
	releases, err := s.db.GetReleases(releaseIDs)
	if err != nil {
		return err
	}
	var failed []string
	for i := range releases {
		if !releases[i].IsStable {
			failed = append(failed, fmt.Sprintf("%s(%s)", releases[i].ReleaseID, "stable release can not be formaled"))
		}
		if err := s.formalRelease(&releases[i]); err != nil {
			failed = append(failed, fmt.Sprintf("%s(%s)", releases[i].ReleaseID, err.Error()))
		}
	}
	if len(failed) == 0 {
		return nil
	}
	return errors.Errorf("failed to formal releases: %s", strings.Join(failed, ","))
}

func (s *ReleaseService) formalRelease(release *db.Release) (err error) {
	tx := s.db.Begin()
	defer func() {
		if err != nil {
			tx.Rollback()
		}
	}()

	if release.IsProjectRelease {
		var modes map[string]apistructs.ReleaseDeployMode
		if err = json.Unmarshal([]byte(release.Modes), &modes); err != nil {
			return err
		}

		for name := range modes {
			for i := 0; i < len(modes[name].ApplicationReleaseList); i++ {
				for j := 0; j < len(modes[name].ApplicationReleaseList[i]); j++ {
					if err = tx.Model(&db.Release{}).Where("release_id = ?",
						modes[name].ApplicationReleaseList[i][j]).Updates(map[string]interface{}{
						"is_stable": true,
						"is_formal": true,
					}).Error; err != nil {
						return
					}
				}
			}
		}
	}

	if err = tx.Model(&db.Release{}).Where("release_id = ?", release.ReleaseID).Update("is_formal", true).Error; err != nil {
		return
	}
	return tx.Commit().Error
}

// Create create Release
func (s *ReleaseService) Create(req *pb.ReleaseCreateRequest) (string, error) {
	if err := limitLabelsLength(req); err != nil {
		return "", err
	}

	// 确保Version唯一
	if req.IsProjectRelease {
		releases, err := s.db.GetReleasesByProjectAndVersion(req.OrgID, req.ProjectID, req.Version)
		if err != nil {
			return "", err
		}
		if len(releases) > 0 {
			return "", errors.Errorf("release version: %s already exist in target project", req.Version)
		}
	} else if req.Version != "" && req.ApplicationID > 0 {
		releases, err := s.db.GetReleasesByAppAndVersion(req.OrgID, req.ProjectID, req.ApplicationID, req.Version)
		if err != nil {
			return "", err
		}
		if len(releases) > 0 {
			return "", errors.Errorf("release version: %s already exist in target application", req.Version)
		}
	}

	var (
		appReleases []db.Release
		err         error
	)
	if req.IsProjectRelease {
		var list []string
		if len(req.Modes) == 0 {
			return "", errors.New("project release modes can not be empty")
		}

		for _, mode := range req.Modes {
			if mode == nil {
				return "", errors.New("mode can not be empty")
			}
			for _, depend := range mode.DependOn {
				if _, ok := req.Modes[depend]; !ok {
					return "", errors.Errorf("depend mode %s does not exist in modes list", depend)
				}
			}
			if len(mode.ApplicationReleaseList) == 0 {
				return "", errors.New("application release list can not be empty")
			}
			for i := 0; i < len(mode.ApplicationReleaseList); i++ {
				if len(mode.ApplicationReleaseList[i].List) == 0 {
					return "", errors.New("application release group can not be empty")
				}
				sort.Strings(mode.ApplicationReleaseList[i].List)
				list = append(list, mode.ApplicationReleaseList[i].List...)
			}
		}
		if hasLoopDependence(req.Modes) {
			return "", errors.New("there is a loop dependence between deployment modes")
		}
		appReleases, err = s.db.GetReleases(strutil.DedupSlice(list))
		if err != nil {
			return "", err
		}
		if len(appReleases) < len(list) {
			existed := make(map[string]struct{})
			for _, release := range appReleases {
				existed[release.ReleaseID] = struct{}{}
			}
			for _, id := range list {
				if _, ok := existed[id]; !ok {
					return "", errors.Errorf("release %s not found", id)
				}
			}
		}
	}

	// 创建Release
	release, err := s.Convert(req, appReleases)
	if err != nil {
		return "", err
	}

	var dices []string
	if req.IsProjectRelease {
		if err = s.createProjectReleaseAndUpdateReference(release, appReleases); err != nil {
			return "", err
		}
	} else {
		dices = append(dices, req.Dice)
		if err = s.createAppReleaseAndSetLatest(release); err != nil {
			return "", err
		}
	}

	// 创建Image
	images := s.GetImages(dices)
	for _, v := range images {
		v.ReleaseID = release.ReleaseID
		if err := s.imageDB.CreateImage(v); err != nil {
			return "", err
		}
	}

	// create label relations
	if len(req.Tags) > 0 {
		tags, err := s.bdl.ListLabelByIDs(req.Tags)
		if err != nil {
			return "", apierrors.ErrCreateRelease.InternalError(err)
		}
		for _, tag := range tags {
			labelRelation := &db.LabelRelation{
				LabelID: uint64(tag.ID),
				RefType: apistructs.LabelTypeRelease,
				RefID:   release.ReleaseID,
			}
			if err := s.labelRelationDB.CreateLabelRelation(labelRelation); err != nil {
				logrus.Errorf("failed to create label relation for label %s when create release %s, %v", tag.Name, release.ReleaseID, err)
				continue
			}
		}
	}
	return release.ReleaseID, nil
}

func (s *ReleaseService) createAppReleaseAndSetLatest(release *db.Release) (err error) {
	tx := s.db.Begin()
	defer func() {
		if err != nil {
			tx.Rollback()
		}
	}()

	var latest db.Release
	if err = tx.Where("project_id = ?", release.ProjectID).Where("application_id = ?", release.ApplicationID).
		Where("git_branch = ?", release.GitBranch).Where("is_latest = true").
		Where("is_project_release = false").Find(&latest).Error; err == nil {
		latest.IsLatest = false
		tx.Save(&latest)
	} else if !gorm.IsRecordNotFoundError(err) {
		return
	}

	if err = tx.Create(release).Error; err != nil {
		return
	}
	return tx.Commit().Error
}

// Update update Release
func (s *ReleaseService) Update(orgID int64, releaseID string, req *pb.ReleaseUpdateRequest) error {
	release, err := s.db.GetRelease(releaseID)
	if err != nil {
		logrus.Errorf("failed to get release %s, %v", releaseID, err)
		return err
	}
	if orgID != 0 && release.OrgID != orgID {
		return errors.Errorf("release not found")
	}

	if release.IsFormal {
		return errors.New("formal release can not be updated")
	}
	// 若version不为空时，确保Version在应用层或项目层唯一
	if req.Version != "" && req.Version != release.Version {
		var releases []db.Release
		if !release.IsProjectRelease {
			releases, err = s.db.GetReleasesByAppAndVersion(req.OrgID, req.ProjectID, req.ApplicationID, req.Version)
		} else {
			releases, err = s.db.GetReleasesByProjectAndVersion(req.OrgID, req.ProjectID, req.Version)
		}
		if err != nil {
			return err
		}
		if len(releases) > 0 {
			return errors.Errorf("release version: %s already exist", req.Version)
		}
	}

	// 更新描述
	if req.Desc != "" {
		release.Desc = req.Desc
	}
	// 更新Version
	if req.Version != "" {
		release.Version = req.Version
	}

	// update changelog
	if req.Changelog != "" {
		release.Changelog = req.Changelog
	}

	if release.IsProjectRelease {
		release.ApplicationID = 0
		release.ApplicationName = ""
		release.Dice = ""

		if len(req.Modes) == 0 {
			return errors.New("project release modes can not be empty")
		}

		var newList []string
		for _, mode := range req.Modes {
			if mode == nil {
				return errors.New("mode can not be empty")
			}
			for _, depend := range mode.DependOn {
				if _, ok := req.Modes[depend]; !ok {
					return errors.Errorf("depend mode %s does not exist in modes list", depend)
				}
			}
			for i := 0; i < len(mode.ApplicationReleaseList); i++ {
				if len(mode.ApplicationReleaseList[i].List) == 0 {
					return errors.New("application release group can not be empty")
				}
				sort.Strings(mode.ApplicationReleaseList[i].List)
				newList = append(newList, mode.ApplicationReleaseList[i].List...)
			}
		}
		if hasLoopDependence(req.Modes) {
			return errors.New("there is a loop dependence between deployment modes")
		}
		newAppReleases, err := s.db.GetReleases(strutil.DedupSlice(newList))
		if err != nil {
			return errors.Errorf("failed to get application releases: %v", err)
		}

		if len(newAppReleases) < len(newList) {
			existed := make(map[string]struct{})
			for _, release := range newAppReleases {
				existed[release.ReleaseID] = struct{}{}
			}
			for _, id := range newList {
				if _, ok := existed[id]; !ok {
					return errors.Errorf("release %s not found", err)
				}
			}
		}

		selectedApp := make(map[int64]struct{})
		for i := 0; i < len(newAppReleases); i++ {
			if _, ok := selectedApp[newAppReleases[i].ApplicationID]; ok {
				return errors.New("one application can only be selected once")
			}
			selectedApp[newAppReleases[i].ApplicationID] = struct{}{}
		}

		var oldList []string
		oldList, err = unmarshalApplicationReleaseList(release.Modes)
		if err != nil {
			return errors.Errorf("failed to json unmarshal release list, %v", err)
		}

		oldAppReleases, err := s.db.GetReleases(oldList)
		if err != nil {
			return err
		}

		// update modes
		newModesData, err := json.Marshal(convertPbModesToModes(req.Modes))
		if err != nil {
			return errors.Errorf("failed to marshal release list, %v", err)
		}
		release.Modes = string(newModesData)

		if err = s.updateProjectReleaseAndReference(release, oldAppReleases, newAppReleases); err != nil {
			return err
		}

	} else {
		if err := s.db.UpdateRelease(release); err != nil {
			return err
		}
	}

	s.updateReleaseTags(releaseID, req)
	return nil
}

// update tags
func (s *ReleaseService) updateReleaseTags(releaseID string, req *pb.ReleaseUpdateRequest) {
	oldLrs, err := s.labelRelationDB.GetLabelRelationsByRef(apistructs.LabelTypeRelease, releaseID)
	if err != nil {
		logrus.Errorf("failed to get label relation by release %s, %v", releaseID, err)
		return
	}

	var oldTagIDs []uint64
	for i := range oldLrs {
		oldTagIDs = append(oldTagIDs, oldLrs[i].LabelID)
	}
	sort.Slice(oldTagIDs, func(i, j int) bool {
		return oldTagIDs[i] < oldTagIDs[j]
	})

	newTagIDs := req.Tags
	sort.Slice(newTagIDs, func(i, j int) bool {
		return newTagIDs[i] < newTagIDs[j]
	})

	if reflect.DeepEqual(oldTagIDs, newTagIDs) {
		return
	}

	if err := s.labelRelationDB.DeleteLabelRelations(apistructs.LabelTypeRelease, releaseID); err != nil {
		logrus.Errorf("failed to delete label relations for release %s, %v", releaseID, err)
		return
	}
	logrus.Errorf("failed to delete label relations for release %s, %v", releaseID, err)
	for _, id := range newTagIDs {
		lr := &db.LabelRelation{
			LabelID: id,
			RefType: apistructs.LabelTypeRelease,
			RefID:   releaseID,
		}
		if err := s.labelRelationDB.CreateLabelRelation(lr); err != nil {
			logrus.Errorf("failed to create label %d for release %s, %v", id, releaseID, err)
			continue
		}
	}
	return
}

func (s *ReleaseService) updateProjectReleaseAndReference(release *db.Release, oldAppReleases, newAppReleases []db.Release) (err error) {
	type pair = struct {
		release  *db.Release
		deltaRef int
	}
	idToRelease := make(map[string]pair)
	for i := range oldAppReleases {
		idToRelease[oldAppReleases[i].ReleaseID] = pair{&oldAppReleases[i], -1}
	}
	for i := range newAppReleases {
		p := idToRelease[newAppReleases[i].ReleaseID]
		idToRelease[newAppReleases[i].ReleaseID] = pair{&newAppReleases[i], p.deltaRef + 1}
	}

	tx := s.db.Begin()
	defer func() {
		if err != nil {
			tx.Rollback()
		}
	}()

	for _, p := range idToRelease {
		if p.deltaRef == 0 {
			continue
		}
		release := p.release
		if p.deltaRef < 0 {
			release.Reference--
		} else {
			release.Reference++
		}
		if err = tx.Save(release).Error; err != nil {
			return
		}
	}

	if err = tx.Save(release).Error; err != nil {
		return
	}
	return tx.Commit().Error
}

// UpdateReference update Release reference
func (s *ReleaseService) UpdateReference(orgID int64, releaseID string, req *pb.ReleaseReferenceUpdateRequest) error {
	release, err := s.db.GetRelease(releaseID)
	if err != nil {
		logrus.Errorf("failed to get release %s, %v", releaseID, err)
		return err
	}
	if orgID != 0 && release.OrgID != orgID {
		return errors.Errorf("release not found")
	}

	if req.Increase {
		release.Reference++
	} else {
		release.Reference--
	}
	if err := s.db.UpdateRelease(release); err != nil {
		return err
	}

	return nil
}

// Delete delete Release
func (s *ReleaseService) Delete(orgID int64, opusMap map[string]*pb.ListArtifactsRespItem, releaseIDs ...string) error {
	var failed []string
	for _, releaseID := range releaseIDs {
		if _, ok := opusMap[releaseID]; ok {
			failed = append(failed, fmt.Sprintf("%s(%s)", releaseID, "can not delete release which has been put on to gallery"))
			continue
		}
		release, err := s.db.GetRelease(releaseID)
		if err != nil {
			failed = append(failed, fmt.Sprintf("%s(%s)", releaseID, err.Error()))
			continue
		}
		if orgID != 0 && release.OrgID != orgID {
			failed = append(failed, fmt.Sprintf("%s(%s)", releaseID, "not fount"))
			continue
		}

		if release.IsFormal {
			failed = append(failed, fmt.Sprintf("%s(%s)", releaseID, "formal release can not be deleted"))
		}
		// Release被使用时，不可删除
		if release.Reference > 0 {
			failed = append(failed, fmt.Sprintf("%s(%s)", releaseID, "release was referenced"))
			logrus.Errorf("failed to delete release %s, which was referenced", releaseID)
			continue
		}

		images, err := s.imageDB.GetImagesByRelease(releaseID)
		if err != nil {
			failed = append(failed, fmt.Sprintf("%s(%s)", releaseID, "failed to get images"+err.Error()))
			continue
		}

		// delete manifests
		if release.ClusterName != "" {
			var imgs []string
			for _, v := range images {
				imgs = append(imgs, v.Image)
			}
			if err := registry.DeleteManifests(s.bdl, release.ClusterName, imgs); err != nil {
				logrus.Errorf(err.Error())
			}
		}

		// delete images from db
		for _, v := range images {
			if err := s.imageDB.DeleteImage(int64(v.ID)); err != nil {
				logrus.Errorf("[alert] delete image: %s fail, err: %v", v.Image, err)
			}
			logrus.Infof("deleted image: %s", v.Image)
		}

		// delete release info
		if release.IsProjectRelease {
			list, err := unmarshalApplicationReleaseList(release.Modes)
			if err != nil {
				failed = append(failed, fmt.Sprintf("%s(%s)", releaseID, "failed to unmarshal release list, "+err.Error()))
				continue
			}
			appReleases, err := s.db.GetReleases(strutil.DedupSlice(list))
			if err != nil {
				failed = append(failed, fmt.Sprintf("%s(%s)", releaseID, "failed to get app releases, "+err.Error()))
				continue
			}
			if err = s.deleteReleaseAndUpdateReference(release, appReleases); err != nil {
				failed = append(failed, fmt.Sprintf("%s(%s)", releaseID, err.Error()))
				continue
			}
		} else {
			if err := s.db.DeleteRelease(releaseID); err != nil {
				failed = append(failed, fmt.Sprintf("%s(%s)", releaseID, err.Error()))
				logrus.Errorf("failed to delete release %s, %v", releaseID, err)
				continue
			}
			if !release.IsProjectRelease && release.IsLatest {
				latest, err := s.GetBranchLatestRelease(release.ProjectID, release.ApplicationID, release.GitBranch)
				if err != nil {
					logrus.Errorf("failed to get latest release after delete release %s, %v", releaseID, err)
					continue
				}
				if latest == nil {
					continue
				}
				latest.IsLatest = true
				if err = s.db.Save(latest).Error; err != nil {
					logrus.Errorf("failed to set latest release after delete release %s, %v", releaseID, err)
					continue
				}
			}
		}

		if err := s.labelRelationDB.DeleteLabelRelations(apistructs.LabelTypeRelease, release.ReleaseID); err != nil {
			logrus.Errorf("failed to delete label relations for release %s, %v", releaseID, err)
			continue
		}
	}
	if len(failed) != 0 {
		return errors.New(strings.Join(failed, ", "))
	}
	return nil
}

func (s *ReleaseService) deleteReleaseAndUpdateReference(release *db.Release, appReleases []db.Release) (err error) {
	tx := s.db.Begin()
	defer func() {
		if err != nil {
			tx.Rollback()
		}
	}()

	for i := range appReleases {
		appReleases[i].Reference--
		if err = tx.Save(appReleases[i]).Error; err != nil {
			return
		}
	}

	if err = tx.Delete(release).Error; err != nil {
		return
	}
	return tx.Commit().Error
}

// Get get Release
func (s *ReleaseService) Get(orgID int64, releaseID string) (*pb.ReleaseGetResponseData, error) {
	release, err := s.db.GetRelease(releaseID)
	if err != nil {
		return nil, err
	}
	if orgID != 0 && release.OrgID != orgID {
		return nil, errors.Errorf("release not found")
	}

	return s.convertToReleaseResponse(release)
}

// List Search based on search parameters
func (s *ReleaseService) List(ctx context.Context, orgID int64, req *pb.ReleaseListRequest) (*pb.ReleaseListResponseData, error) {
	if len(req.Tags) != 0 {
		lrs, err := s.labelRelationDB.GetLabelRelationsByLabels(apistructs.LabelTypeRelease, req.Tags)
		if err != nil {
			return nil, errors.Errorf("failed to get release tags, %v", err)
		}

		if len(lrs) == 0 {
			return &pb.ReleaseListResponseData{}, nil
		}

		var releaseIDs []string
		for i := range lrs {
			releaseIDs = append(releaseIDs, lrs[i].RefID)
		}

		if req.ReleaseID != "" {
			requestedIDs := make(map[string]struct{})
			splits := strings.Split(req.ReleaseID, ",")
			for _, id := range splits {
				requestedIDs[strings.TrimSpace(id)] = struct{}{}
			}

			var targetIDs []string
			for _, id := range releaseIDs {
				if _, ok := requestedIDs[id]; !ok {
					continue
				}
				targetIDs = append(targetIDs, id)
			}

			if len(targetIDs) == 0 {
				return &pb.ReleaseListResponseData{List: []*pb.ReleaseData{}}, nil
			}
			releaseIDs = targetIDs
		}

		ids := strings.Join(releaseIDs, ",")
		req.ReleaseID = ids
	}

	total, releases, err := s.db.GetReleasesByParams(orgID, req)
	if err != nil {
		return nil, err
	}

	var tags []apistructs.ProjectLabel
	if req.ProjectID != 0 {
		resp, err := s.bdl.ListLabel(apistructs.ProjectLabelListRequest{
			ProjectID: uint64(req.ProjectID),
			Type:      apistructs.LabelTypeRelease,
			PageNo:    1,
			PageSize:  1000,
		})
		if err != nil {
			return nil, errors.Errorf("failed to list labels, %v", err)
		}
		tags = resp.List
	}
	tagMap := make(map[int64]*apistructs.ProjectLabel)
	for i := range tags {
		tagMap[tags[i].ID] = &tags[i]
	}

	var releaseIDs []string
	for i := range releases {
		releaseIDs = append(releaseIDs, releases[i].ReleaseID)
	}
	releaseTagMap, err := s.labelRelationDB.BatchQueryReleaseTagIDMap(releaseIDs)
	if err != nil {
		return nil, errors.Errorf("failed to batcy query release label id map, %v", err)
	}

	opuses, err := s.opus.ListArtifacts(ctx, &pb.ListArtifactsReq{OrgID: uint32(orgID), ReleaseIDs: releaseIDs})
	if err != nil {
		return nil, errors.Errorf("failed to list opus, %v", err)
	}

	releaseList := make([]*pb.ReleaseData, 0, len(releases))
	//projects, err := s.bdl.GetAllProjects()
	//if err != nil {
	//	return nil, errors.Errorf("failed to list projects, %v", err)
	//}
	//
	//id2DisplayName := make(map[uint64]string)
	//for i := range projects {
	//	id2DisplayName[projects[i].ID] = projects[i].DisplayName
	//}

	for _, v := range releases {
		release, err := convertToListReleaseResponse(&v, releaseTagMap[v.ReleaseID], tagMap, opuses.Data)
		if err != nil {
			logrus.WithField("func", "*ReleaseList").Errorln("failed to convertToListReleaseResponse")
			continue
		}
		releaseList = append(releaseList, release)
	}

	return &pb.ReleaseListResponseData{
		Total: total,
		List:  releaseList,
	}, nil
}

func convertToListReleaseResponse(release *db.Release, tagIDs []uint64, tagsMap map[int64]*apistructs.ProjectLabel, opusMap map[string]*pb.ListArtifactsRespItem) (*pb.ReleaseData, error) {
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

	var tags []*pb.Tag
	for _, id := range tagIDs {
		tag, ok := tagsMap[int64(id)]
		if !ok {
			continue
		}
		tags = append(tags, &pb.Tag{
			CreatedAt: timestamppb.New(tag.CreatedAt),
			UpdatedAt: timestamppb.New(tag.UpdatedAt),
			Creator:   tag.Creator,
			Id:        tag.ID,
			Color:     tag.Color,
			Name:      tag.Name,
			Type:      string(tag.Type),
			ProjectID: int64(tag.ProjectID),
		})
	}

	var opusID, opusVersionID string
	opusInfo := opusMap[release.ReleaseID]
	if opusInfo != nil {
		opusID = opusInfo.OpusID
		opusVersionID = opusInfo.OpusVersionID
	}
	respData := &pb.ReleaseData{
		ReleaseID:          release.ReleaseID,
		ReleaseName:        release.ReleaseName,
		Diceyml:            release.Dice,
		Desc:               release.Desc,
		Addon:              release.Addon,
		Changelog:          release.Changelog,
		IsStable:           release.IsStable,
		IsFormal:           release.IsFormal,
		IsProjectRelease:   release.IsProjectRelease,
		Modes:              release.Modes,
		Resources:          resources,
		Labels:             labels,
		Tags:               tags,
		Version:            release.Version,
		CrossCluster:       release.CrossCluster,
		Reference:          release.Reference,
		OrgID:              release.OrgID,
		ProjectID:          release.ProjectID,
		ApplicationID:      release.ApplicationID,
		ProjectName:        release.ProjectName,
		ProjectDisplayName: release.ProjectName,
		ApplicationName:    release.ApplicationName,
		UserID:             release.UserID,
		ClusterName:        release.ClusterName,
		CreatedAt:          timestamppb.New(release.CreatedAt),
		UpdatedAt:          timestamppb.New(release.UpdatedAt),
		IsLatest:           release.IsLatest,
		OpusID:             opusID,
		OpusVersionID:      opusVersionID,
	}
	return respData, nil
}

// GetImages get image by ReleaseRequest
func (s *ReleaseService) GetImages(dices []string) []*imagedb.Image {
	existed := make(map[string]struct{})
	images := make([]*imagedb.Image, 0)
	for _, yml := range dices {
		var dice diceyml.Object
		err := yaml.Unmarshal([]byte(yml), &dice)
		if err != nil {
			continue
		}

		// Get images from dice.yml
		for key, service := range dice.Services {
			// Check service if contain any image
			if service.Image == "" {
				logrus.Errorf("service %s doesn't contain any image", key)
				continue
			}
			repoName, tag := parseImage(service.Image)
			image := &imagedb.Image{
				Image:     service.Image,
				ImageName: repoName,
				ImageTag:  tag,
			}
			if _, ok := existed[image.Image]; !ok {
				images = append(images, image)
				existed[image.Image] = struct{}{}
			}
		}
		for key, job := range dice.Jobs {
			// Check service if contain any image
			if job.Image == "" {
				logrus.Errorf("job %s doesn't contain any image", key)
				continue
			}
			repoName, tag := parseImage(job.Image)
			image := &imagedb.Image{
				Image:     job.Image,
				ImageName: repoName,
				ImageTag:  tag,
			}
			if _, ok := existed[image.Image]; !ok {
				images = append(images, image)
				existed[image.Image] = struct{}{}
			}
		}
	}
	return images
}

// GetBranchLatestRelease return the latest release with target gitBranch.
// return nil if not found.
func (s *ReleaseService) GetBranchLatestRelease(projectID, appID int64, gitBranch string) (*db.Release, error) {
	releases, err := s.db.GetReleasesByBranch(projectID, appID, gitBranch)
	if err != nil {
		return nil, err
	}

	var latest *db.Release
	for i := range releases {
		if latest == nil || latest.CreatedAt.Before(releases[i].CreatedAt) {
			latest = &releases[i]
		}
	}
	return latest, nil
}

func (s *ReleaseService) CreateByFile(req *pb.ReleaseUploadRequest, file io.ReadCloser) (string, string, error) {
	projectRelease, appReleases, err := s.parseReleaseFile(req, file)
	if err != nil {
		return "", "", err
	}

	releases, err := s.db.GetReleasesByProjectAndVersion(req.OrgID, req.ProjectID, projectRelease.Version)
	if err != nil {
		return "", "", err
	}
	if len(releases) > 0 {
		return "", "", errors.Errorf("release version: %s already exist", projectRelease.Version)
	}

	err = s.createReleases(append(appReleases, *projectRelease))
	if err != nil {
		return "", "", err
	}

	if len(req.Tags) > 0 {
		tags, err := s.bdl.ListLabelByIDs(req.Tags)
		if err != nil {
			return "", "", errors.Errorf("failed to list tags, %v", err)
		}
		for _, tag := range tags {
			labelRelation := &db.LabelRelation{
				LabelID: uint64(tag.ID),
				RefType: apistructs.LabelTypeRelease,
				RefID:   projectRelease.ReleaseID,
			}
			if err := s.labelRelationDB.CreateLabelRelation(labelRelation); err != nil {
				logrus.Errorf("failed to create label relation for label %s when create release %s, %v", tag.Name, projectRelease.ReleaseID, err)
				continue
			}
		}
	}
	return projectRelease.Version, projectRelease.ReleaseID, nil
}

func (s *ReleaseService) parseReleaseFile(req *pb.ReleaseUploadRequest, file io.ReadCloser) (*db.Release, []db.Release, error) {
	var metadata apistructs.ReleaseMetadata
	dices := make(map[string]string)
	buf := bytes.Buffer{}
	if _, err := io.Copy(&buf, file); err != nil {
		return nil, nil, err
	}
	zr, err := zip.NewReader(bytes.NewReader(buf.Bytes()), int64(buf.Len()))
	if err != nil {
		return nil, nil, err
	}
	hasMetadata := false
	for _, f := range zr.File {
		rc, err := f.Open()
		if err != nil {
			return nil, nil, err
		}
		buf := bytes.Buffer{}
		if _, err = io.Copy(&buf, rc); err != nil {
			return nil, nil, err
		}

		splits := strings.Split(f.Name, "/")
		if len(splits) == 2 && splits[1] == "metadata.yml" {
			hasMetadata = true
			if err := yaml.Unmarshal(buf.Bytes(), &metadata); err != nil {
				return nil, nil, err
			}
		} else if len(splits) == 4 && splits[3] == "dice.yml" {
			appName := splits[2]
			dices[appName] = buf.String()
		}

		if err := rc.Close(); err != nil {
			return nil, nil, err
		}
	}

	if !hasMetadata {
		return nil, nil, errors.New("invalid file, metadata.yml not found")
	}
	if len(dices) == 0 {
		return nil, nil, errors.Errorf("invalid file, dice.yml not found")
	}

	projectReleaseID := uuid.UUID()

	var names []string
	for appName := range dices {
		names = append(names, appName)
	}
	resp, err := s.bdl.GetAppIDByNames(uint64(req.ProjectID), req.UserID, names)
	if err != nil {
		return nil, nil, err
	}
	appName2ID := resp.AppNameToID

	now := time.Now()

	var appReleases []db.Release
	appVersion2ID := make(map[string]string)
	modes := make(map[string]apistructs.ReleaseDeployMode)
	for name := range metadata.Modes {
		appReleaseList := make([][]string, len(metadata.Modes[name].AppList))
		for i := 0; i < len(metadata.Modes[name].AppList); i++ {
			appReleaseList[i] = make([]string, len(metadata.Modes[name].AppList[i]))
			for j := 0; j < len(metadata.Modes[name].AppList[i]); j++ {
				version := metadata.Modes[name].AppList[i][j].Version
				appName := metadata.Modes[name].AppList[i][j].AppName
				if version == "" {
					return nil, nil, errors.New("version can not be empty")
				}
				if id, ok := appVersion2ID[appName+"_"+version]; ok {
					appReleaseList[i][j] = id
					continue
				}
				id := uuid.UUID()
				appVersion2ID[appName+"_"+version] = id

				gitBranch := metadata.Modes[name].AppList[i][j].GitBranch
				gitCommitID := metadata.Modes[name].AppList[i][j].GitCommitID
				gitCommitMsg := metadata.Modes[name].AppList[i][j].GitCommitMessage
				gitRepo := metadata.Modes[name].AppList[i][j].GitRepo
				changelog := metadata.Modes[name].AppList[i][j].ChangeLog

				if _, ok := appName2ID[appName]; !ok {
					return nil, nil, errors.Errorf("app %s is not existed", appName)
				}

				existedReleases, err := s.db.GetReleasesByAppAndVersion(req.OrgID, req.ProjectID, appName2ID[appName], version)
				if err != nil {
					return nil, nil, errors.Errorf("failed to get releases by app and version, %v", err)
				}
				if len(existedReleases) > 0 {
					var oldDice *diceyml.DiceYaml
					if len(existedReleases[0].Dice) != 0 {
						oldDice, err = diceyml.New([]byte(existedReleases[0].Dice), true)
						if err != nil {
							return nil, nil, errors.Errorf("dice yml for release %s is invalid, %v", existedReleases[0].ReleaseID, err)
						}
					}
					if dices[appName] == "" {
						return nil, nil, errors.Errorf("dice yml for app %s release is empty", appName)
					}
					newDice, err := diceyml.New([]byte(dices[appName]), true)
					if err != nil {
						return nil, nil, errors.Errorf("dice yml for app %s release is invalid, %v", appName, err)
					}
					if !reflect.DeepEqual(oldDice.Obj(), newDice.Obj()) {
						logrus.Warningf("app release %s (ID: %s) to upload was already existed but has different dice yml. Use old dice yml instead. Old: %v. New:%v",
							version, existedReleases[0].ReleaseID, oldDice.Obj(), newDice.Obj())
					}
					appReleaseList[i][j] = existedReleases[0].ReleaseID
					continue
				}

				labels := map[string]string{
					"gitBranch":        gitBranch,
					"gitCommitId":      gitCommitID,
					"gitCommitMessage": gitCommitMsg,
					"gitRepo":          gitRepo,
				}
				data, err := json.Marshal(labels)
				if err != nil {
					return nil, nil, errors.Errorf("failed to marshal labels, %v", err)
				}

				appReleaseList[i][j] = id
				appReleases = append(appReleases, db.Release{
					ReleaseID:        id,
					ReleaseName:      gitBranch,
					Desc:             fmt.Sprintf("referenced by project release %s", projectReleaseID),
					Dice:             dices[appName],
					Changelog:        changelog,
					IsStable:         true,
					IsFormal:         false,
					IsProjectRelease: false,
					Labels:           string(data),
					GitBranch:        gitBranch,
					Version:          version,
					OrgID:            req.OrgID,
					ProjectID:        req.ProjectID,
					ApplicationID:    appName2ID[appName],
					ProjectName:      req.ProjectName,
					ApplicationName:  appName,
					UserID:           req.UserID,
					ClusterName:      req.ClusterName,
					Reference:        1,
					CreatedAt:        now,
					UpdatedAt:        now,
					IsLatest:         true,
				})
			}
		}
		modes[name] = apistructs.ReleaseDeployMode{
			DependOn:               metadata.Modes[name].DependOn,
			Expose:                 metadata.Modes[name].Expose,
			ApplicationReleaseList: appReleaseList,
		}
	}

	modesData, err := json.Marshal(modes)
	if err != nil {
		return nil, nil, errors.Errorf("faield to marshal modes, %v", err)
	}
	projectRelease := &db.Release{
		ReleaseID:        projectReleaseID,
		Desc:             metadata.Desc,
		Changelog:        metadata.ChangeLog,
		IsStable:         true,
		IsFormal:         false,
		IsProjectRelease: true,
		Modes:            string(modesData),
		Version:          metadata.Version,
		OrgID:            req.OrgID,
		ProjectID:        req.ProjectID,
		ProjectName:      req.ProjectName,
		UserID:           req.UserID,
		ClusterName:      req.ClusterName,
		CreatedAt:        now,
		UpdatedAt:        now,
	}
	return projectRelease, appReleases, nil
}

func (s *ReleaseService) createReleases(releases []db.Release) (err error) {
	tx := s.db.Begin()
	defer func() {
		if err != nil {
			tx.Rollback()
		}
	}()

	for i := range releases {
		if !releases[i].IsProjectRelease {
			var latest *db.Release
			latest, err = s.GetBranchLatestRelease(releases[i].ProjectID, releases[i].ApplicationID, releases[i].GitBranch)
			if err != nil {
				return
			}
			if latest != nil {
				latest.IsLatest = false
				tx.Save(latest)
			}
		}
		if err = tx.Create(releases[i]).Error; err != nil {
			return
		}
	}
	return tx.Commit().Error
}

func (s *ReleaseService) createProjectReleaseAndUpdateReference(release *db.Release, appReleases []db.Release) (err error) {
	tx := s.db.Begin()
	defer func() {
		if err != nil {
			tx.Rollback()
		}
	}()

	if err = tx.Save(release).Error; err != nil {
		return
	}

	for i := range appReleases {
		appReleases[i].Reference++
		if err = tx.Save(appReleases[i]).Error; err != nil {
			return
		}
	}
	return tx.Commit().Error
}

// image format: docker-registry.registry.marathon.mesos:5000/pampas-blog/blog-service:v0.2
func parseImage(image string) (repoName, tag string) {
	ss := strings.SplitN(image, "/", 2)
	if len(ss) == 2 {
		repo := strings.Split(ss[1], ":")[0]
		var repoTag string
		if strings.Contains(ss[1], ":") {
			repoTag = strings.Split(ss[1], ":")[1]
		} else {
			repoTag = "latest"
		}
		return repo, repoTag
	}
	return "", ""
}

func unmarshalApplicationReleaseList(str string) ([]string, error) {
	modes := make(map[string]apistructs.ReleaseDeployMode)
	if err := json.Unmarshal([]byte(str), &modes); err != nil {
		return nil, err
	}
	var res []string
	for _, mode := range modes {
		for i := 0; i < len(mode.ApplicationReleaseList); i++ {
			res = append(res, mode.ApplicationReleaseList[i]...)
		}
	}
	return strutil.DedupSlice(res), nil
}

func isSliceEqual(a, b []string) bool {
	if len(a) != len(b) {
		return false
	}
	for i := range a {
		if a[i] != b[i] {
			return false
		}
	}
	return true
}

func hasLoopDependence(modes map[string]*pb.Mode) bool {
	valid := true
	visited := make(map[string]int)
	var dfs func(string)
	dfs = func(u string) {
		visited[u] = 1
		for _, v := range modes[u].DependOn {
			if visited[v] == 0 {
				dfs(v)
				if !valid {
					return
				}
			} else if visited[v] == 1 {
				valid = false
				return
			}
		}
		visited[u] = 2
	}

	for name := range modes {
		if !valid {
			break
		}
		if visited[name] == 0 {
			dfs(name)
		}
	}
	return !valid
}

func convertPbModesToModes(pbModes map[string]*pb.Mode) map[string]apistructs.ReleaseDeployMode {
	modes := make(map[string]apistructs.ReleaseDeployMode)
	for name, mode := range pbModes {
		var list [][]string
		for _, l := range mode.ApplicationReleaseList {
			list = append(list, l.List)
		}
		modes[name] = apistructs.ReleaseDeployMode{
			DependOn:               mode.DependOn,
			Expose:                 mode.Expose,
			ApplicationReleaseList: list,
		}
	}
	return modes
}
