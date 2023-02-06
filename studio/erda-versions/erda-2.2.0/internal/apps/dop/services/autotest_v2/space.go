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

package autotestv2

import (
	"fmt"
	"strconv"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
)

// CreateSpace 创建测试空间
func (svc *Service) CreateSpace(req apistructs.AutoTestSpaceCreateRequest) (*apistructs.AutoTestSpace, error) {
	fmt.Println("userID: ", req.UserID)
	fmt.Println("projectID: ", req.ProjectID)
	// TODO: 鉴权
	if !req.IsInternalClient() {
		// Authorize
		access, err := svc.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   req.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  uint64(req.ProjectID),
			Resource: apistructs.TestSpaceResource,
			Action:   apistructs.CreateAction,
		})
		if err != nil {
			return nil, err
		}
		if !access.Access {
			return nil, apierrors.ErrCreateAutoTestSpace.AccessDenied()
		}
	}

	if err := svc.validateSpace(dao.AutoTestSpace{
		Name:        req.Name,
		ProjectID:   req.ProjectID,
		Description: req.Description,
	}); err != nil {
		return nil, err
	}
	autoTestSpace := dao.AutoTestSpace{
		Name:          req.Name,
		ProjectID:     req.ProjectID,
		Description:   req.Description,
		CreatorID:     req.UserID,
		UpdaterID:     req.UserID,
		Status:        apistructs.TestSpaceOpen,
		ArchiveStatus: apistructs.TestSpaceInit,
		SourceSpaceID: req.SourceSpaceID,
	}
	if req.ArchiveStatus.Valid() {
		autoTestSpace.ArchiveStatus = req.ArchiveStatus
	}
	res, err := svc.db.CreateAutoTestSpace(&autoTestSpace)
	// 创建
	if err != nil {
		return nil, apierrors.ErrCreateAutoTestSpace.InternalError(err)
	}
	// 转换
	return convertToUnifiedFileSpace(res), nil
}

// GetSpace 返回autoTestSpace详情
func (svc *Service) GetSpace(id uint64) (*apistructs.AutoTestSpace, error) {
	res, err := svc.db.GetAutoTestSpace(id)
	if err != nil {
		return nil, apierrors.ErrGetAutoTestSpace.InternalError(err)
	}
	return convertToUnifiedFileSpace(res), nil
}

// GetSpaceList 返回autoTestSpace列表
func (svc *Service) GetSpaceList(req apistructs.AutoTestSpaceListRequest) (*apistructs.AutoTestSpaceList, error) {
	if req.PageNo == 0 {
		req.PageNo = 1
	}
	if req.PageSize == 0 {
		req.PageSize = 10
	}
	res, total, err := svc.db.ListAutoTestSpaceByProject(req)
	if err != nil {
		return nil, apierrors.ErrListAutoTestSpace.InternalError(err)
	}
	var ans []apistructs.AutoTestSpace
	for _, each := range res {
		ans = append(ans, *convertToUnifiedFileSpace(&each))
	}
	return &apistructs.AutoTestSpaceList{
		Total: total,
		List:  ans,
	}, nil
}

func (svc *Service) validateSpace(req dao.AutoTestSpace) error {
	var res []dao.AutoTestSpace
	res, total, err := svc.db.ListAutoTestSpaceByProject(apistructs.AutoTestSpaceListRequest{
		ProjectID: req.ProjectID,
		PageNo:    1,
		PageSize:  100,
	})
	if err != nil {
		return err
	}
	if total >= 500 && req.ID == 0 {
		return fmt.Errorf("一个项目下限制500个空间数量")
	}
	for _, each := range res {
		if each.ID == req.ID {
			continue
		}
		if each.Name == req.Name {
			return fmt.Errorf("the name [%s] is existed", req.Name)
		}
	}
	return nil
}

// UpdateAutoTestSpace 更新测试空间
func (svc *Service) UpdateAutoTestSpace(req apistructs.AutoTestSpace, UserID string) (*apistructs.AutoTestSpace, error) {
	autoTestSpace, err := svc.db.GetAutoTestSpace(req.ID)
	if err != nil {
		return nil, apierrors.ErrUpdateAutoTestSpace.InternalError(err)
	}
	autoTestSpace.UpdaterID = UserID
	if len(req.Status) > 0 {
		autoTestSpace.Status = req.Status
	}
	changedField := getChangedFields(autoTestSpace, req)
	if err := svc.validateSpace(*autoTestSpace); err != nil {
		return nil, apierrors.ErrUpdateAutoTestSpace.InternalError(err)
	}
	res, err := svc.db.UpdateAutoTestSpace(autoTestSpace)
	if err != nil {
		return nil, apierrors.ErrUpdateAutoTestSpace.InternalError(err)
	}
	if len(changedField) > 0 {
		go svc.createAudits(autoTestSpace, changedField)
	}
	return convertToUnifiedFileSpace(res), nil
}

func getChangedFields(autoTestSpace *dao.AutoTestSpace, req apistructs.AutoTestSpace) map[string][]string {
	changedField := make(map[string][]string)
	if len(req.Name) > 0 {
		if req.Name != autoTestSpace.Name {
			changedField["Name"] = []string{autoTestSpace.Name, req.Name}
		}
		autoTestSpace.Name = req.Name
	}
	if len(req.ArchiveStatus) > 0 {
		if req.ArchiveStatus != autoTestSpace.ArchiveStatus {
			changedField["ArchiveStatus"] = []string{autoTestSpace.ArchiveStatus.GetZhName(), req.ArchiveStatus.GetZhName()}
		}
		autoTestSpace.ArchiveStatus = req.ArchiveStatus
	}
	if len(req.Description) > 0 {
		if req.Description != autoTestSpace.Description {
			changedField["Description"] = []string{autoTestSpace.Description, req.Description}
		}
		autoTestSpace.Description = req.Description
	}
	return changedField
}

func (svc *Service) createAudits(space *dao.AutoTestSpace, changedField map[string][]string) {
	now := strconv.FormatInt(time.Now().Unix(), 10)
	projectID := uint64(space.ProjectID)
	project, err := svc.bdl.GetProject(projectID)
	if err != nil {
		logrus.Error(apierrors.ErrUpdateAutoTestSpace.InternalError(err))
		return
	}
	audits := make([]apistructs.Audit, 0)
	for i, v := range changedField {
		audits = append(audits, apistructs.Audit{
			UserID:       space.UpdaterID,
			ScopeType:    apistructs.ProjectScope,
			ScopeID:      projectID,
			OrgID:        project.OrgID,
			ProjectID:    projectID,
			Result:       "success",
			StartTime:    now,
			EndTime:      now,
			TemplateName: apistructs.TemplateName(apistructs.UpdateAutoTestSpaceTemplatePrefix + i),
			Context: map[string]interface{}{
				"projectName": project.Name,
				"spaceName":   space.Name,
				"from":        v[0],
				"to":          v[1],
			},
		})
	}
	if err := svc.bdl.BatchCreateAuditEvent(&apistructs.AuditBatchCreateRequest{Audits: audits}); err != nil {
		logrus.Error(apierrors.ErrUpdateAutoTestSpace.InternalError(err))
	}
}

// DeleteAutoTestSpace 删除测试空间
func (svc *Service) DeleteAutoTestSpace(req apistructs.AutoTestSpace, identityInfo apistructs.IdentityInfo) (*apistructs.AutoTestSpace, error) {
	space, err := svc.GetSpace(req.ID)
	if err != nil {
		return nil, apierrors.ErrDeleteAutoTestSpace.InternalError(err)
	}
	// 检测是否有执行计划绑定空间，如果有，不可删除
	total, _, _, err := svc.db.PagingTestPlanV2(&apistructs.TestPlanV2PagingRequest{SpaceID: req.ID})
	if err != nil {
		return nil, apierrors.ErrDeleteAutoTestSpace.InternalError(err)
	}
	if total > 0 {
		return nil, apierrors.ErrDeleteAutoTestSpace.InternalError(fmt.Errorf("exist test_plan"))
	}
	// 回改源空间状态
	if space.Status == apistructs.TestSpaceFailed && space.SourceSpaceID != nil {
		sourceSpace, err := svc.GetSpace(*space.SourceSpaceID)
		if err != nil {
			return nil, apierrors.ErrDeleteAutoTestSpace.InternalError(err)
		}
		sourceSpace.Status = apistructs.TestSpaceOpen
		_, err = svc.UpdateAutoTestSpace(*sourceSpace, identityInfo.UserID)
		if err != nil {
			return nil, apierrors.ErrDeleteAutoTestSpace.InternalError(err)
		}
	}

	autoTestSpace := dao.AutoTestSpace{ProjectID: req.ProjectID}
	autoTestSpace.ID = req.ID
	res, err := svc.db.DeleteAutoTestSpace(&autoTestSpace)
	if err != nil {
		return nil, apierrors.ErrDeleteAutoTestSpace.InternalError(err)
	}
	if err = svc.db.DeleteAutoTestSpaceRelation(req.ID); err != nil {
		return nil, apierrors.ErrDeleteAutoTestSpace.InternalError(err)
	}
	return convertToUnifiedFileSpace(res), nil
}

func convertToUnifiedFileSpace(req *dao.AutoTestSpace) *apistructs.AutoTestSpace {
	return &apistructs.AutoTestSpace{
		ID:            req.ID,
		Name:          req.Name,
		ProjectID:     req.ProjectID,
		Description:   req.Description,
		CreatorID:     req.CreatorID,
		UpdaterID:     req.UpdaterID,
		Status:        req.Status,
		SourceSpaceID: req.SourceSpaceID,
		CreatedAt:     req.CreatedAt,
		UpdatedAt:     req.UpdatedAt,
		DeletedAt:     req.DeletedAt,
		ArchiveStatus: req.ArchiveStatus,
	}
}

// CopyAutotestSpaceV2 use AutoTestSpaceDirector make space data, then use space data copy self
func (svc *Service) CopyAutotestSpaceV2(req apistructs.AutoTestSpace, identityInfo apistructs.IdentityInfo) *apistructs.AutoTestSpace {
	spaceDBData := AutoTestSpaceDB{Data: &AutoTestSpaceData{
		svc:          svc,
		IdentityInfo: identityInfo,
		SpaceID:      req.ID,
		IsCopy:       true,
		ProjectID:    uint64(req.ProjectID),
	},
	}

	go func() {
		creator := AutoTestSpaceDirector{}
		creator.New(&spaceDBData)
		if err := creator.Construct(); err != nil {
			logrus.Error(apierrors.ErrCopyAutoTestSpace.InternalError(err))
			return
		}
		spaceData := creator.Creator.GetSpaceData()
		_, err := spaceData.Copy()
		if err != nil {
			logrus.Error(apierrors.ErrCopyAutoTestSpace.InternalError(err))
			return
		}
	}()
	return &apistructs.AutoTestSpace{}
}

// copy after update scene ref sceneSet id
func (svc *Service) UpdateAutoTestSceneRefSet(copyRefs []apistructs.AutoTestSceneCopyRef) error {
	for _, ref := range copyRefs {
		err := svc.db.UpdateSceneRefSetID(ref)
		if err != nil {
			return err
		}
	}
	return nil
}

// GenerateSceneName 生成场景名，追加 _N
func (svc *Service) GenerateSpaceName(name string, projectID int64) (string, error) {
	finalName, err := getTitleName(name)
	if err != nil {
		return "", err
	}

	for {
		// find by name
		exist, err := svc.db.GetAutotestSpaceByName(finalName, projectID)
		if err != nil {
			return "", err
		}
		// not exist
		if exist == nil {
			return finalName, nil
		}
		// exist and is others, generate (N) and query again
		finalName, err = getTitleName(finalName)
		if err != nil {
			return "", err
		}
	}
}

func (svc *Service) SpaceStatRetriever(spaceIDs []uint64) (map[uint64]*apistructs.AutoTestSpaceStats, error) {
	set, scene, step, err := svc.db.GetAutoTestSpaceStats(spaceIDs)
	if err != nil {
		return nil, err
	}
	statsMap := make(map[uint64]*apistructs.AutoTestSpaceStats)
	for _, i := range spaceIDs {
		statsMap[i] = &apistructs.AutoTestSpaceStats{}
	}
	for _, i := range set {
		statsMap[i.SpaceID].SetNum = i.SetNum
	}
	for _, i := range scene {
		statsMap[i.SpaceID].SceneNum = i.SceneNum
	}
	for _, i := range step {
		statsMap[i.SpaceID].StepNum = i.StepNum
	}
	return statsMap, nil
}
