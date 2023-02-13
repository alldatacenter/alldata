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

// Package project 封装项目资源相关操作
package project

import (
	"context"
	"encoding/json"
	"fmt"
	"strconv"
	"strings"
	"time"

	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	projectCache "github.com/erda-project/erda/internal/core/legacy/cache/project"
	"github.com/erda-project/erda/internal/core/legacy/conf"
	"github.com/erda-project/erda/internal/core/legacy/dao"
	"github.com/erda-project/erda/internal/core/legacy/model"
	"github.com/erda-project/erda/internal/core/legacy/types"
	"github.com/erda-project/erda/pkg/filehelper"
	local "github.com/erda-project/erda/pkg/i18n"
	calcu "github.com/erda-project/erda/pkg/resourcecalculator"
	"github.com/erda-project/erda/pkg/ucauth"
)

// Project 资源对象操作封装
type Project struct {
	db    *dao.DBClient
	uc    *ucauth.UCClient
	bdl   *bundle.Bundle
	trans i18n.Translator
}

// Option 定义 Project 对象的配置选项
type Option func(*Project)

// New 新建 Project 实例，通过 Project 实例操作企业资源
func New(opts ...Option) *Project {
	var p Project
	for _, f := range opts {
		f(&p)
	}
	return &p
}

// WithDBClient 配置 db client
func WithDBClient(db *dao.DBClient) Option {
	return func(project *Project) {
		project.db = db
	}
}

// WithUCClient 配置 uc client
func WithUCClient(uc *ucauth.UCClient) Option {
	return func(project *Project) {
		project.uc = uc
	}
}

// WithBundle 配置 bundle
func WithBundle(bdl *bundle.Bundle) Option {
	return func(project *Project) {
		project.bdl = bdl
	}
}

// WithI18n set the translator
func WithI18n(trans i18n.Translator) Option {
	return func(project *Project) {
		project.trans = trans
	}
}

// CreateWithEvent 创建项目 & 发送事件
func (p *Project) CreateWithEvent(userID string, createReq *apistructs.ProjectCreateRequest) (int64, error) {
	// 创建项目
	if createReq.DisplayName == "" {
		createReq.DisplayName = createReq.Name
	}
	project, err := p.Create(userID, createReq)
	if err != nil {
		return 0, err
	}
	ev := &apistructs.EventCreateRequest{
		EventHeader: apistructs.EventHeader{
			Event:         bundle.ProjectEvent,
			Action:        bundle.CreateAction,
			OrgID:         strconv.FormatInt(project.OrgID, 10),
			ProjectID:     strconv.FormatInt(project.ID, 10),
			ApplicationID: "-1",
			TimeStamp:     time.Now().Format("2006-01-02 15:04:05"),
		},
		Sender:  bundle.SenderCoreServices,
		Content: *project,
	}
	// 发送项目创建事件
	if err = p.bdl.CreateEvent(ev); err != nil {
		logrus.Warnf("failed to send project create event, (%v)", err)
	}

	return project.ID, nil
}

// Create 创建项目
func (p *Project) Create(userID string, createReq *apistructs.ProjectCreateRequest) (*model.Project, error) {
	// 参数合法性检查
	if createReq.Name == "" {
		return nil, errors.Errorf("failed to create project(name is empty)")
	}
	if createReq.OrgID == 0 {
		return nil, errors.Errorf("failed to create project(org id is empty)")
	}
	// 只有 DevOps 类型的项目，才能配置 quota
	if createReq.Template != apistructs.DevopsTemplate {
		createReq.ResourceConfigs = nil
	}
	var clusterConfig []byte
	if rc := createReq.ResourceConfigs; rc != nil {
		if err := rc.Check(); err != nil {
			return nil, err
		}
		createReq.ClusterConfig = map[string]string{
			"PROD":    rc.PROD.ClusterName,
			"STAGING": rc.STAGING.ClusterName,
			"TEST":    rc.TEST.ClusterName,
			"DEV":     rc.DEV.ClusterName,
		}
		clusterConfig, _ = json.Marshal(createReq.ClusterConfig)
		createReq.CpuQuota = float64(calcu.CoreToMillcore(rc.PROD.CPUQuota + rc.STAGING.CPUQuota + rc.TEST.CPUQuota + rc.DEV.CPUQuota))
		createReq.MemQuota = float64(calcu.GibibyteToByte(rc.PROD.MemQuota + rc.STAGING.MemQuota + rc.TEST.MemQuota + rc.DEV.MemQuota))
	}

	if err := initRollbackConfig(&createReq.RollbackConfig); err != nil {
		return nil, err
	}
	rollbackConfig, err := json.Marshal(createReq.RollbackConfig)
	if err != nil {
		logrus.Infof("failed to marshal rollbackConfig, (%v)", err)
		return nil, errors.Errorf("failed to marshal rollbackConfig")
	}

	functions, err := json.Marshal(createReq.Template.GetProjectFunctionsByTemplate())
	if err != nil {
		logrus.Infof("failed to marshal projectFunctions, (%v)", err)
		return nil, errors.Errorf("failed to marshal projectFunctions")
	}

	project, err := p.db.GetProjectByOrgAndName(int64(createReq.OrgID), createReq.Name)
	if err != nil {
		if !gorm.IsRecordNotFoundError(err) {
			return nil, err
		}
	}
	if project != nil {
		return nil, errors.Errorf("failed to create project(name already exists)")
	}

	tx := p.db.Begin()
	defer tx.RollbackUnlessCommitted()

	now := time.Now()
	// 添加项目至DB
	project = &model.Project{
		BaseModel:      model.BaseModel{},
		Name:           createReq.Name,
		DisplayName:    createReq.DisplayName,
		Desc:           createReq.Desc,
		Logo:           createReq.Logo,
		OrgID:          int64(createReq.OrgID),
		UserID:         userID,
		DDHook:         createReq.DdHook,
		ClusterConfig:  string(clusterConfig),
		RollbackConfig: string(rollbackConfig),
		CpuQuota:       createReq.CpuQuota,
		MemQuota:       createReq.MemQuota,
		Functions:      string(functions),
		ActiveTime:     now,
		EnableNS:       conf.EnableNS(),
		IsPublic:       false,
		Type:           string(createReq.Template),
	}
	if err := tx.Create(&project).Error; err != nil {
		logrus.WithError(err).WithField("model", project.TableName()).
			Errorln("failed to Create")
		return nil, errors.Errorf("failed to insert project to database")
	}

	// record quota if it is configured
	logrus.WithField("createReq.ResourceConfigs", createReq.ResourceConfigs).Infoln()
	if createReq.ResourceConfigs != nil {
		quota := &apistructs.ProjectQuota{
			ProjectID:          uint64(project.ID),
			ProjectName:        createReq.Name,
			ProdClusterName:    createReq.ResourceConfigs.PROD.ClusterName,
			StagingClusterName: createReq.ResourceConfigs.STAGING.ClusterName,
			TestClusterName:    createReq.ResourceConfigs.TEST.ClusterName,
			DevClusterName:     createReq.ResourceConfigs.DEV.ClusterName,
			ProdCPUQuota:       calcu.CoreToMillcore(createReq.ResourceConfigs.PROD.CPUQuota),
			ProdMemQuota:       calcu.GibibyteToByte(createReq.ResourceConfigs.PROD.MemQuota),
			StagingCPUQuota:    calcu.CoreToMillcore(createReq.ResourceConfigs.STAGING.CPUQuota),
			StagingMemQuota:    calcu.GibibyteToByte(createReq.ResourceConfigs.STAGING.MemQuota),
			TestCPUQuota:       calcu.CoreToMillcore(createReq.ResourceConfigs.TEST.CPUQuota),
			TestMemQuota:       calcu.GibibyteToByte(createReq.ResourceConfigs.TEST.MemQuota),
			DevCPUQuota:        calcu.CoreToMillcore(createReq.ResourceConfigs.DEV.CPUQuota),
			DevMemQuota:        calcu.GibibyteToByte(createReq.ResourceConfigs.DEV.MemQuota),
			CreatorID:          userID,
			UpdaterID:          userID,
		}
		if err := tx.Debug().Create(&quota).Error; err != nil {
			logrus.WithError(err).WithField("model", quota.TableName()).
				Errorln("failed to Create")
			return nil, errors.Errorf("failed to insert project quota to database")
		}
	}

	// 新增项目管理员至admin_members表
	users, err := p.uc.FindUsers([]string{userID})
	if err != nil {
		logrus.Warnf("user query error: %v", err)
	}
	if len(users) > 0 {
		member := model.Member{
			ScopeType:  apistructs.ProjectScope,
			ScopeID:    project.ID,
			ScopeName:  project.Name,
			ParentID:   project.OrgID,
			UserID:     userID,
			Email:      users[0].Email,
			Mobile:     users[0].Phone,
			Name:       users[0].Name,
			Nick:       users[0].Nick,
			Avatar:     users[0].AvatarURL,
			UserSyncAt: time.Now(),
			OrgID:      project.OrgID,
			ProjectID:  project.ID,
		}
		memberExtra := model.MemberExtra{
			UserID:        userID,
			ScopeID:       project.ID,
			ScopeType:     apistructs.ProjectScope,
			ParentID:      project.OrgID,
			ResourceKey:   apistructs.RoleResourceKey,
			ResourceValue: types.RoleProjectOwner,
		}
		if err := tx.Create(&member).Error; err != nil {
			logrus.WithError(err).WithField("model", member.TableName()).
				Errorln("failed to add member to database while creating project")
			return nil, errors.Errorf("failed to add member to database while creating project")
		}
		if err := tx.Create(&memberExtra).Error; err != nil {
			logrus.WithError(err).WithField("model", memberExtra.TableName()).
				Errorln("failed to add member roles to database while creating project")
			return nil, errors.Errorf("failed to add member roles to database while creating project")
		}
	}

	tx.Commit()

	return project, nil
}

// UpdateWithEvent 更新项目 & 发送事件
func (p *Project) UpdateWithEvent(ctx context.Context, orgID, projectID int64, userID string, updateReq *apistructs.ProjectUpdateBody) error {
	// 更新项目
	project, err := p.Update(ctx, orgID, projectID, userID, updateReq)
	if err != nil {
		return err
	}

	ev := &apistructs.EventCreateRequest{
		EventHeader: apistructs.EventHeader{
			Event:         bundle.ProjectEvent,
			Action:        bundle.UpdateAction,
			OrgID:         strconv.FormatInt(project.OrgID, 10),
			ProjectID:     strconv.FormatInt(project.ID, 10),
			ApplicationID: "-1",
			TimeStamp:     time.Now().Format("2006-01-02 15:04:05"),
		},
		Sender:  bundle.SenderCoreServices,
		Content: *project,
	}
	// 发送项目更新事件
	if err = p.bdl.CreateEvent(ev); err != nil {
		logrus.Warnf("failed to send project update event, (%v)", err)
	}

	return nil
}

// Update 更新项目
func (p *Project) Update(ctx context.Context, orgID, projectID int64, userID string, updateReq *apistructs.ProjectUpdateBody) (*model.Project, error) {
	if rc := updateReq.ResourceConfigs; rc != nil {
		updateReq.ClusterConfig = map[string]string{
			"PROD":    updateReq.ResourceConfigs.PROD.ClusterName,
			"STAGING": updateReq.ResourceConfigs.STAGING.ClusterName,
			"TEST":    updateReq.ResourceConfigs.TEST.ClusterName,
			"DEV":     updateReq.ResourceConfigs.DEV.ClusterName,
		}
		if err := updateReq.ResourceConfigs.Check(); err != nil {
			return nil, err
		}
		updateReq.CpuQuota = float64(calcu.CoreToMillcore(rc.PROD.CPUQuota + rc.STAGING.CPUQuota + rc.TEST.CPUQuota + rc.DEV.CPUQuota))
		updateReq.MemQuota = float64(calcu.GibibyteToByte(rc.PROD.MemQuota + rc.STAGING.MemQuota + rc.TEST.MemQuota + rc.DEV.MemQuota))
	}
	if err := checkRollbackConfig(&updateReq.RollbackConfig); err != nil {
		return nil, err
	}

	// 检查待更新的project是否存在
	project, err := p.db.GetProjectByID(projectID)
	if err != nil {
		return nil, errors.Wrap(err, "failed to GetProjectByID")
	}
	var oldClusterConfig = make(map[string]string)
	_ = json.Unmarshal([]byte(project.ClusterConfig), &oldClusterConfig)
	oldQuota, err := p.db.GetQuotaByProjectID(projectID)
	if err != nil {
		return nil, errors.Wrap(err, "failed to GetQuotaByProjectID")
	}
	hasOldQuota := oldQuota != nil
	if hasOldQuota {
		project.Quota = new(apistructs.ProjectQuota)
		*project.Quota = *oldQuota
	}

	if err := patchProject(&project, updateReq, userID); err != nil {
		return nil, err
	}

	tx := p.db.Begin()
	defer tx.RollbackUnlessCommitted()

	if err = tx.Save(&project).Error; err != nil {
		logrus.Warnf("failed to update project, (%v)", err)
		return nil, errors.Errorf("failed to update project")
	}

	if project.Quota == nil {
		tx.Commit()
		return &project, nil
	}

	// check new quota is less than request
	var dto = new(apistructs.ProjectDTO)
	dto.ID = uint64(project.ID)
	setProjectDtoQuotaFromModel(dto, project.Quota)
	p.fetchPodInfo(dto)
	changedRecord := make(map[string]bool)
	if oldQuota == nil {
		oldQuota = new(apistructs.ProjectQuota)
		oldQuota.ProdClusterName = oldClusterConfig["PROD"]
		oldQuota.StagingClusterName = oldClusterConfig["STAGING"]
		oldQuota.TestClusterName = oldClusterConfig["TEST"]
		oldQuota.DevClusterName = oldClusterConfig["DEV"]
	}
	isQuotaChangedOnTheWorkspace(changedRecord, *oldQuota, *project.Quota)
	if msg, ok := p.checkNewQuotaIsLessThanRequest(ctx, dto, changedRecord); !ok {
		logrus.Errorf("checkNewQuotaIsLessThanRequest is not ok: %s", msg)
		return nil, errors.New(msg)
	}

	if hasOldQuota {
		err = tx.Debug().Save(project.Quota).Error
	} else {
		err = tx.Debug().Create(project.Quota).Error
	}
	if err != nil {
		logrus.WithError(err).Errorln("failed to update project quota")
		return nil, errors.Errorf("failed to update project quota: %v", err)
	}
	if err = tx.Commit().Error; err != nil {
		err = errors.Wrap(err, "failed commit to update project and quota")
		logrus.WithError(err).Errorln()
		return nil, err
	}

	// audit
	go func() {
		if project.Quota == nil || !isQuotaChanged(*oldQuota, *project.Quota) {
			proCtx, _ := json.Marshal(map[string]string{"projectName": project.Name})
			if err := p.db.CreateAudit(&model.Audit{
				ScopeType:    apistructs.ProjectScope,
				ScopeID:      uint64(projectID),
				ProjectID:    uint64(projectID),
				TemplateName: apistructs.UpdateProjectTemplate,
				UserID:       userID,
				OrgID:        uint64(orgID),
				Context:      string(proCtx),
				Result:       "success",
				StartTime:    time.Now(),
				EndTime:      time.Now(),
			}); err != nil {
				logrus.Errorf("failed to create project audit event when update project %s, %v", project.Name, err)
			}
			return
		}
		var orgName = strconv.FormatInt(orgID, 10)
		if org, err := p.db.GetOrg(orgID); err == nil {
			orgName = fmt.Sprintf("%s", org.Name)
		}
		auditCtx := map[string]interface{}{
			"orgName":           orgName,
			"projectName":       project.Name,
			"newDevCluster":     project.Quota.DevClusterName,
			"newDevCPU":         fmt.Sprintf("%.3f", updateReq.ResourceConfigs.DEV.CPUQuota),
			"newDevMem":         fmt.Sprintf("%.3fGB", updateReq.ResourceConfigs.DEV.MemQuota),
			"newTestCluster":    project.Quota.TestClusterName,
			"newTestCPU":        fmt.Sprintf("%.3f", updateReq.ResourceConfigs.TEST.CPUQuota),
			"newTestMem":        fmt.Sprintf("%.3fGB", updateReq.ResourceConfigs.TEST.MemQuota),
			"newStagingCluster": project.Quota.StagingClusterName,
			"newStagingCPU":     fmt.Sprintf("%.3f", updateReq.ResourceConfigs.STAGING.CPUQuota),
			"newStagingMem":     fmt.Sprintf("%.3fGB", updateReq.ResourceConfigs.STAGING.MemQuota),
			"newProdCluster":    project.Quota.ProdClusterName,
			"newProdCPU":        fmt.Sprintf("%.3f", updateReq.ResourceConfigs.PROD.CPUQuota),
			"newProdMem":        fmt.Sprintf("%.3fGB", updateReq.ResourceConfigs.PROD.MemQuota),
			"oldDevCluster":     oldQuota.DevClusterName,
			"oldDevCPU":         calcu.ResourceToString(float64(oldQuota.DevCPUQuota), "cpu"),
			"oldDevMem":         calcu.ResourceToString(float64(oldQuota.DevMemQuota), "memory"),
			"oldTestCluster":    oldQuota.TestClusterName,
			"oldTestCPU":        calcu.ResourceToString(float64(oldQuota.TestCPUQuota), "cpu"),
			"oldTestMem":        calcu.ResourceToString(float64(oldQuota.TestMemQuota), "memory"),
			"oldStagingCluster": oldQuota.StagingClusterName,
			"oldStagingCPU":     calcu.ResourceToString(float64(oldQuota.StagingCPUQuota), "cpu"),
			"oldStagingMem":     calcu.ResourceToString(float64(oldQuota.StagingMemQuota), "memory"),
			"oldProdCluster":    oldQuota.ProdClusterName,
			"oldProdCPU":        calcu.ResourceToString(float64(oldQuota.ProdCPUQuota), "cpu"),
			"oldProdMem":        calcu.ResourceToString(float64(oldQuota.ProdMemQuota), "memory"),
		}
		now := time.Now().Format("2006-01-02 15:04:05")
		audit := apistructs.Audit{
			UserID:       userID,
			ScopeType:    apistructs.OrgScope,
			ScopeID:      uint64(orgID),
			OrgID:        uint64(orgID),
			ProjectID:    uint64(projectID),
			Context:      auditCtx,
			TemplateName: "updateProjectResourceConfig",
			Result:       "success",
			StartTime:    now,
			EndTime:      now,
		}
		auditRecord, err := convertAuditCreateReq2Model(audit)
		if err != nil {
			logrus.WithError(err).WithField("audit", audit).
				Errorf("failed to convertAuditCreateReq2Model")
			return
		}
		if err = p.db.CreateAudit(auditRecord); err != nil {
			logrus.Errorf("failed to create quota audit event when update project %s, %v", project.Name, err)
		}
	}()

	return &project, nil
}

func setQuotaFromResourceConfig(quota *apistructs.ProjectQuota, resourceConfigs *apistructs.ResourceConfigs) {
	if quota == nil || resourceConfigs == nil {
		return
	}
	if resourceConfigs.PROD != nil {
		quota.ProdClusterName = resourceConfigs.PROD.ClusterName
		quota.ProdCPUQuota = calcu.CoreToMillcore(resourceConfigs.PROD.CPUQuota)
		quota.ProdMemQuota = calcu.GibibyteToByte(resourceConfigs.PROD.MemQuota)
	}
	if resourceConfigs.STAGING != nil {
		quota.StagingClusterName = resourceConfigs.STAGING.ClusterName
		quota.StagingCPUQuota = calcu.CoreToMillcore(resourceConfigs.STAGING.CPUQuota)
		quota.StagingMemQuota = calcu.GibibyteToByte(resourceConfigs.STAGING.MemQuota)
	}
	if resourceConfigs.TEST != nil {
		quota.TestClusterName = resourceConfigs.TEST.ClusterName
		quota.TestCPUQuota = calcu.CoreToMillcore(resourceConfigs.TEST.CPUQuota)
		quota.TestMemQuota = calcu.GibibyteToByte(resourceConfigs.TEST.MemQuota)
	}
	if resourceConfigs.DEV != nil {
		quota.DevClusterName = resourceConfigs.DEV.ClusterName
		quota.DevCPUQuota = calcu.CoreToMillcore(resourceConfigs.DEV.CPUQuota)
		quota.DevMemQuota = calcu.GibibyteToByte(resourceConfigs.DEV.MemQuota)
	}
}

func isQuotaChanged(oldQuota, newQuota apistructs.ProjectQuota) bool {
	var changedRecord = make(map[string]bool)
	isQuotaChangedOnTheWorkspace(changedRecord, oldQuota, newQuota)
	return changedRecord["PROD"] || changedRecord["STAGING"] || changedRecord["TEST"] || changedRecord["DEV"]
}

func isQuotaChangedOnTheWorkspace(workspaces map[string]bool, oldQuota, newQuota apistructs.ProjectQuota) {
	if workspaces == nil {
		return
	}
	workspaces["PROD"] = oldQuota.ProdCPUQuota != newQuota.ProdCPUQuota ||
		oldQuota.ProdMemQuota != newQuota.ProdMemQuota ||
		oldQuota.ProdClusterName != newQuota.ProdClusterName
	workspaces["STAGING"] = oldQuota.StagingCPUQuota != newQuota.StagingCPUQuota ||
		oldQuota.StagingMemQuota != newQuota.StagingMemQuota ||
		oldQuota.StagingClusterName != newQuota.StagingClusterName
	workspaces["TEST"] = oldQuota.TestCPUQuota != newQuota.TestCPUQuota ||
		oldQuota.TestMemQuota != newQuota.TestMemQuota ||
		oldQuota.TestClusterName != newQuota.TestClusterName
	workspaces["DEV"] = oldQuota.DevCPUQuota != newQuota.DevCPUQuota ||
		oldQuota.DevMemQuota != newQuota.DevMemQuota ||
		oldQuota.DevClusterName != newQuota.DevClusterName
}

func patchProject(project *model.Project, updateReq *apistructs.ProjectUpdateBody, userID string) error {
	clusterConf, err := json.Marshal(updateReq.ClusterConfig)
	if err != nil {
		logrus.Errorf("failed to marshal clusterConfig, (%v)", err)
		return errors.Errorf("failed to marshal clusterConfig")
	}

	rollbackConf, err := json.Marshal(updateReq.RollbackConfig)
	if err != nil {
		logrus.Errorf("failed to marshal rollbackConfig, (%v)", err)
		return errors.Errorf("failed to marshal rollbackConfig")
	}

	if updateReq.DisplayName != "" {
		project.DisplayName = updateReq.DisplayName
	}

	if updateReq.ResourceConfigs != nil {
		project.ClusterConfig = string(clusterConf)
	}

	if len(updateReq.RollbackConfig) != 0 {
		project.RollbackConfig = string(rollbackConf)
	}

	if updateReq.ResourceConfigs != nil {
		if project.Quota == nil {
			project.Quota = new(apistructs.ProjectQuota)
			project.Quota.ProjectID = uint64(project.ID)
			project.Quota.ProjectName = updateReq.Name
			project.Quota.CreatorID = userID
		}
		setQuotaFromResourceConfig(project.Quota, updateReq.ResourceConfigs)
		project.Quota.UpdaterID = userID
	}

	project.Desc = updateReq.Desc
	project.Logo = updateReq.Logo
	project.DDHook = updateReq.DdHook
	project.ActiveTime = time.Now()
	project.IsPublic = updateReq.IsPublic
	project.CpuQuota = updateReq.CpuQuota
	project.MemQuota = updateReq.MemQuota

	return nil
}

func convertAuditCreateReq2Model(req apistructs.Audit) (*model.Audit, error) {
	context, err := json.Marshal(req.Context)
	if err != nil {
		return nil, err
	}
	startAt, err := time.ParseInLocation("2006-01-02 15:04:05", req.StartTime, time.Local)
	if err != nil {
		return nil, err
	}
	endAt, err := time.ParseInLocation("2006-01-02 15:04:05", req.EndTime, time.Local)
	if err != nil {
		return nil, err
	}
	audit := &model.Audit{
		StartTime:    startAt,
		EndTime:      endAt,
		UserID:       req.UserID,
		ScopeType:    req.ScopeType,
		ScopeID:      req.ScopeID,
		FDPProjectID: req.FDPProjectID,
		AppID:        req.AppID,
		OrgID:        req.OrgID,
		ProjectID:    req.ProjectID,
		Context:      string(context),
		TemplateName: req.TemplateName,
		AuditLevel:   req.AuditLevel,
		Result:       req.Result,
		ErrorMsg:     req.ErrorMsg,
		ClientIP:     req.ClientIP,
		UserAgent:    req.UserAgent,
	}

	return audit, nil
}

// DeleteWithEvent 删除项目 & 发送事件
func (p *Project) DeleteWithEvent(projectID int64) error {
	// 删除项目
	project, err := p.Delete(projectID)
	if err != nil {
		return err
	}

	ev := &apistructs.EventCreateRequest{
		EventHeader: apistructs.EventHeader{
			Event:         bundle.ProjectEvent,
			Action:        bundle.DeleteAction,
			OrgID:         strconv.FormatInt(project.OrgID, 10),
			ProjectID:     strconv.FormatInt(projectID, 10),
			ApplicationID: "-1",
			TimeStamp:     time.Now().Format("2006-01-02 15:04:05"),
		},
		Sender:  bundle.SenderCoreServices,
		Content: *project,
	}
	// 发送项目删除事件
	if err = p.bdl.CreateEvent(ev); err != nil {
		logrus.Warnf("failed to send project delete event, (%v)", err)
	}

	return nil
}

// Delete 删除项目
func (p *Project) Delete(projectID int64) (*model.Project, error) {
	langCodes, _ := i18n.ParseLanguageCode(local.GetGoroutineBindLang())
	// check if application exists
	if count, err := p.db.GetApplicationCountByProjectID(projectID); err != nil || count > 0 {
		return nil, errors.Errorf(p.trans.Text(langCodes, "DeleteProjectErrorApplicationExist"))
	}

	project, err := p.db.GetProjectByID(projectID)
	if err != nil {
		return nil, errors.Errorf(p.trans.Text(langCodes, "FailedGetProject")+"(%v)", err)
	}

	if err = p.db.DeleteProject(projectID); err != nil {
		return nil, errors.Errorf(p.trans.Text(langCodes, "FailedDeleteProject")+"(%v)", err)
	}
	_ = p.db.DeleteProjectQutoa(projectID)
	logrus.Infof("deleted project %d", projectID)

	// 删除权限表记录
	if err = p.db.DeleteMembersByScope(apistructs.ProjectScope, projectID); err != nil {
		logrus.Warnf("failed to delete members, (%v)", err)
	}
	if err = p.db.DeleteMemberExtraByScope(apistructs.ProjectScope, projectID); err != nil {
		logrus.Warnf("failed to delete members extra, (%v)", err)
	}
	return &project, nil
}

// Get 获取项目
func (p *Project) Get(ctx context.Context, projectID int64, withQuota bool) (*apistructs.ProjectDTO, error) {
	project, err := p.db.GetProjectByID(projectID)
	if err != nil {
		return nil, err
	}
	totalApp, err := p.db.GetApplicationCountByProjectID(project.ID)
	if err != nil {
		return nil, err
	}
	totalMember, _, err := p.db.GetMembersWithoutExtraByScope(apistructs.ProjectScope, project.ID)
	if err != nil {
		return nil, err
	}
	stats := apistructs.ProjectStats{
		CountApplications: int(totalApp),
		CountMembers:      totalMember,
	}
	projectDTO := p.convertToProjectDTO(true, &project, stats)

	owners, err := p.db.GetMemberByScopeAndRole(apistructs.ProjectScope, []uint64{uint64(projectID)}, []string{"owner"})
	if err != nil {
		return nil, err
	}
	for _, v := range owners {
		projectDTO.Owners = append(projectDTO.Owners, v.UserID)
	}

	if withQuota {
		// 查询项目 quota
		p.fetchQuota(&projectDTO)
		// 查询项目下的 pod 的 request 数据
		p.fetchPodInfo(&projectDTO)
		// 根据已有统计值计算比率
		p.calcuRequestRate(&projectDTO)
	}

	return &projectDTO, nil
}

func (p *Project) fetchQuota(dto *apistructs.ProjectDTO) {
	defaultResourceConfig(dto)

	var projectQuota apistructs.ProjectQuota
	if err := p.db.First(&projectQuota, map[string]interface{}{"project_id": dto.ID}).Error; err != nil {
		logrus.WithError(err).WithField("project_id", dto.ID).
			Warnln("failed to select the quota record of the project")
		return
	}
	setProjectDtoQuotaFromModel(dto, &projectQuota)
}

func defaultResourceConfig(dto *apistructs.ProjectDTO) {
	if dto == nil || dto.ClusterConfig == nil {
		return
	}
	var (
		prodCluster, hasProdCluster       = dto.ClusterConfig["PROD"]
		stagingCluster, hasStagingCluster = dto.ClusterConfig["STAGING"]
		testCluster, hasTestCluster       = dto.ClusterConfig["TEST"]
		devCluster, hasDevCluster         = dto.ClusterConfig["DEV"]
	)

	if hasProdCluster && hasStagingCluster && hasTestCluster && hasDevCluster {
		dto.ResourceConfig = apistructs.NewResourceConfig()
		dto.ResourceConfig.PROD.ClusterName = prodCluster
		dto.ResourceConfig.STAGING.ClusterName = stagingCluster
		dto.ResourceConfig.TEST.ClusterName = testCluster
		dto.ResourceConfig.DEV.ClusterName = devCluster
		return
	}
	if !hasProdCluster && !hasStagingCluster && !hasTestCluster && !hasDevCluster {
		return
	}
	logrus.Warnf("the config of cluster must be all empty or all not empty: prod: %s, staging: %s, test: %s, dev: %s",
		prodCluster, stagingCluster, testCluster, devCluster)
}

func setProjectDtoQuotaFromModel(dto *apistructs.ProjectDTO, quota *apistructs.ProjectQuota) {
	if dto == nil || quota == nil {
		return
	}

	dto.ClusterConfig = make(map[string]string)
	dto.ResourceConfig = apistructs.NewResourceConfig()
	dto.ClusterConfig["PROD"] = quota.ProdClusterName
	dto.ClusterConfig["STAGING"] = quota.StagingClusterName
	dto.ClusterConfig["TEST"] = quota.TestClusterName
	dto.ClusterConfig["DEV"] = quota.DevClusterName
	dto.ResourceConfig.PROD.ClusterName = quota.ProdClusterName
	dto.ResourceConfig.STAGING.ClusterName = quota.StagingClusterName
	dto.ResourceConfig.TEST.ClusterName = quota.TestClusterName
	dto.ResourceConfig.DEV.ClusterName = quota.DevClusterName
	dto.ResourceConfig.PROD.CPUQuota = calcu.MillcoreToCore(quota.ProdCPUQuota, 3)
	dto.ResourceConfig.STAGING.CPUQuota = calcu.MillcoreToCore(quota.StagingCPUQuota, 3)
	dto.ResourceConfig.TEST.CPUQuota = calcu.MillcoreToCore(quota.TestCPUQuota, 3)
	dto.ResourceConfig.DEV.CPUQuota = calcu.MillcoreToCore(quota.DevCPUQuota, 3)
	dto.ResourceConfig.PROD.MemQuota = calcu.ByteToGibibyte(quota.ProdMemQuota, 3)
	dto.ResourceConfig.STAGING.MemQuota = calcu.ByteToGibibyte(quota.StagingMemQuota, 3)
	dto.ResourceConfig.TEST.MemQuota = calcu.ByteToGibibyte(quota.TestMemQuota, 3)
	dto.ResourceConfig.DEV.MemQuota = calcu.ByteToGibibyte(quota.DevMemQuota, 3)
	dto.CpuQuota = calcu.MillcoreToCore(quota.ProdCPUQuota+quota.StagingCPUQuota+quota.TestCPUQuota+quota.DevCPUQuota, 3)
	dto.MemQuota = calcu.ByteToGibibyte(quota.ProdMemQuota+quota.StagingMemQuota+quota.TestMemQuota+quota.DevMemQuota, 3)
}

// 从 s_pod_info 中读取 cpu/mem_request 数据记录到 dto
func (p *Project) fetchPodInfo(dto *apistructs.ProjectDTO) {
	if dto == nil || dto.ResourceConfig == nil {
		return
	}
	var podInfos []apistructs.PodInfo
	if err := p.db.Find(&podInfos, RunningPodCond(dto.ID)).Error; err != nil {
		logrus.WithError(err).WithField("project_id", dto.ID).
			Warnln("failed to Find the namespaces info in the project")
		return
	}

	for workspace, rc := range map[string]*apistructs.ResourceConfigInfo{
		"prod":    dto.ResourceConfig.PROD,
		"staging": dto.ResourceConfig.STAGING,
		"test":    dto.ResourceConfig.TEST,
		"dev":     dto.ResourceConfig.DEV,
	} {
		if rc == nil {
			continue
		}
		for _, podInfo := range podInfos {
			if !strings.EqualFold(podInfo.Workspace, workspace) {
				continue
			}
			if podInfo.Cluster != rc.ClusterName && podInfo.Cluster != "" {
				continue
			}
			rc.CPURequest += podInfo.CPURequest
			rc.MemRequest += podInfo.MemRequest / 1024
			switch podInfo.ServiceType {
			case "addon":
				rc.CPURequestByAddon += podInfo.CPURequest
				rc.MemRequestByAddon += podInfo.MemRequest / 1024
			case "stateless-service":
				rc.CPURequestByService += podInfo.CPURequest
				rc.MemRequestByService += podInfo.MemRequest / 1024
			}
		}

		rc.CPURequest = calcu.Accuracy(rc.CPURequest, 3)
		rc.CPURequestByAddon = calcu.Accuracy(rc.CPURequestByAddon, 3)
		rc.CPURequestByService = calcu.Accuracy(rc.CPURequestByService, 3)
		rc.MemRequest = calcu.Accuracy(rc.MemRequest, 3)
		rc.MemRequestByAddon = calcu.Accuracy(rc.MemRequestByAddon, 3)
		rc.MemRequestByService = calcu.Accuracy(rc.MemRequestByService, 3)
	}
}

// 根据已有统计值计算比率
func (p *Project) calcuRequestRate(dto *apistructs.ProjectDTO) {
	if dto == nil || dto.ResourceConfig == nil {
		return
	}
	for _, rc := range []*apistructs.ResourceConfigInfo{
		dto.ResourceConfig.PROD,
		dto.ResourceConfig.STAGING,
		dto.ResourceConfig.TEST,
		dto.ResourceConfig.DEV,
	} {
		if rc == nil {
			continue
		}
		if rc.CPUQuota != 0 {
			rc.CPURequestRate = calcu.Accuracy(rc.CPURequest/rc.CPUQuota*100, 2)
			rc.CPURequestByAddonRate = calcu.Accuracy(rc.CPURequestByAddon/rc.CPUQuota*100, 2)
			rc.CPURequestByServiceRate = calcu.Accuracy(rc.CPURequestByService/rc.CPUQuota*100, 2)
		}
		if rc.MemQuota != 0 {
			rc.MemRequestRate = calcu.Accuracy(rc.MemRequest/rc.MemQuota*100, 2)
			rc.MemRequestByAddonRate = calcu.Accuracy(rc.MemRequestByAddon/rc.MemQuota*100, 2)
			rc.MemRequestByServiceRate = calcu.Accuracy(rc.MemRequestByService/rc.MemQuota*100, 2)
		}
	}
}

// GetModelProject 获取项目
func (p *Project) GetModelProject(projectID int64) (*model.Project, error) {
	project, err := p.db.GetProjectByID(projectID)
	if err != nil {
		return nil, errors.Wrap(err, "failed to get project")
	}

	return &project, nil
}

func (p *Project) GetModelProjectsMap(projectIDs []uint64, keepMsp bool) (map[int64]*model.Project, error) {
	_, projects, err := p.db.GetProjectsByIDs(projectIDs, &apistructs.ProjectListRequest{
		PageNo:   1,
		PageSize: len(projectIDs),
		KeepMsp:  keepMsp,
	})
	if err != nil {
		return nil, errors.Errorf("failed to get projects, (%v)", err)
	}

	projectMap := make(map[int64]*model.Project)
	for i, p := range projects {
		projectMap[p.ID] = &projects[i]
	}
	return projectMap, nil
}

// GetAllProjects list all project
func (p *Project) GetAllProjects() ([]apistructs.ProjectDTO, error) {
	projects, err := p.db.GetAllProjects()
	if err != nil {
		return nil, err
	}
	projectsDTO := p.BatchConvertProjectDTO(nil, projects)
	return projectsDTO, nil
}

// ListAllProjects 企业管理员可查看当前企业下所有项目，包括未加入的项目
func (p *Project) ListAllProjects(userID string, params *apistructs.ProjectListRequest) (
	*apistructs.PagingProjectDTO, error) {
	total, projects, err := p.db.GetProjectsByOrgIDAndName(int64(params.OrgID), params)
	if err != nil {
		return nil, errors.Errorf("failed to get projects, (%v)", err)
	}
	members, err := p.db.GetMembersByScopeTypeAndUser(apistructs.ProjectScope, userID)
	if err != nil {
		return nil, errors.Errorf("failed to get projects, (%v)", err)
	}

	// 转换成所需格式
	projectIDs := make([]uint64, 0, len(projects))
	flags := make(map[int64]bool)
	for i := range projects {
		// 找出企业管理员已加入的项目
		for j := range members {
			if projects[i].ID == members[j].ScopeID {
				flags[projects[i].ID] = true
			}
		}
		projectIDs = append(projectIDs, uint64(projects[i].ID))
	}
	projectDTOs := p.BatchConvertProjectDTO(flags, projects)
	projectOwnerMap := make(map[uint64][]string)
	owners, err := p.db.GetMemberByScopeAndRole(apistructs.ProjectScope, projectIDs, []string{types.RoleProjectOwner})
	if err != nil {
		return nil, err
	}
	for _, v := range owners {
		projectID := uint64(v.ScopeID)
		projectOwnerMap[projectID] = append(projectOwnerMap[projectID], v.UserID)
	}

	for i := range projectDTOs {
		projectDTOs[i].CpuQuota = calcu.MillcoreToCore(uint64(projectDTOs[i].CpuQuota), 3)
		projectDTOs[i].MemQuota = calcu.ByteToGibibyte(uint64(projectDTOs[i].MemQuota), 3)
		if v, ok := projectOwnerMap[projectDTOs[i].ID]; ok {
			projectDTOs[i].Owners = v
		}
	}

	return &apistructs.PagingProjectDTO{Total: total, List: projectDTOs}, nil
}

// ListPublicProjects 获取公开项目列表
func (p *Project) ListPublicProjects(userID string, params *apistructs.ProjectListRequest) (
	*apistructs.PagingProjectDTO, error) {
	// 查找有权限的列表
	members, err := p.db.GetMembersByParentID(apistructs.ProjectScope, int64(params.OrgID), userID)
	if err != nil {
		return nil, errors.Errorf("failed to get permission when get projects, (%v)", err)
	}

	projectIDs := make([]uint64, 0, len(members))
	isManager := map[uint64]bool{} // 是否有管理权限
	isJoined := map[int64]bool{}   // 是否加入了项目
	for i := range members {
		if members[i].ResourceKey == apistructs.RoleResourceKey {
			isJoined[members[i].ScopeID] = true
			if members[i].ResourceValue == types.RoleProjectOwner ||
				members[i].ResourceValue == types.RoleProjectLead ||
				members[i].ResourceValue == types.RoleProjectPM {
				isManager[uint64(members[i].ScopeID)] = true
			} else {
				isManager[uint64(members[i].ScopeID)] = false
			}
		}
	}

	approves, err := p.db.ListUnblockApplicationApprove(params.OrgID)
	if err != nil {
		return nil, errors.Errorf("failed to ListUnblockApplicationApprove: %v", err)
	}

	org, err := p.db.GetOrg(int64(params.OrgID))
	if err != nil {
		return nil, errors.Errorf("failed to getorg(%d): %v", params.OrgID, err)
	}

	isOrgBlocked := false
	projectBlockStatus := map[uint64]string{}
	if org.BlockoutConfig.BlockDEV ||
		org.BlockoutConfig.BlockTEST ||
		org.BlockoutConfig.BlockStage ||
		org.BlockoutConfig.BlockProd {
		isOrgBlocked = true
		for _, i := range projectIDs {
			projectBlockStatus[i] = "blocked"
		}
	}

	for _, approve := range approves {
		if approve.Status == string(apistructs.ApprovalStatusPending) {
			projectBlockStatus[approve.TargetID] = "unblocking"
		}
	}
	// 获取项目列表
	total, projects, err := p.db.GetProjectsByOrgIDAndName(int64(params.OrgID), params)
	if err != nil {
		return nil, errors.Errorf("failed to get projects, (%v)", err)
	}

	// 转换成所需格式
	projectIDs = make([]uint64, 0, len(projects))
	for i := range projects {
		projectIDs = append(projectIDs, uint64(projects[i].ID))
	}
	projectDTOs := p.BatchConvertProjectDTO(isJoined, projects)

	unblockAppCounts, err := p.ListUnblockAppCountsByProjectIDS(projectIDs)
	if err != nil {
		return nil, errors.Errorf("failed to get unblock apps, err: %v", err)
	}
	for _, counter := range unblockAppCounts {
		if counter.UnblockAppCount > 0 {
			projectBlockStatus[uint64(counter.ProjectID)] = "unblocked"
		}
	}

	projectOwnerMap := make(map[uint64][]string)
	owners, err := p.db.GetMemberByScopeAndRole(apistructs.ProjectScope, projectIDs, []string{"owner"})
	if err != nil {
		return nil, err
	}
	for _, v := range owners {
		projectID := uint64(v.ScopeID)
		projectOwnerMap[projectID] = append(projectOwnerMap[projectID], v.UserID)
	}

	for i := range projectDTOs {
		if v, ok := projectOwnerMap[projectDTOs[i].ID]; ok {
			projectDTOs[i].Owners = v
		}
	}

	for i := range projectDTOs {
		if isOrgBlocked {
			projectDTOs[i].BlockStatus = projectBlockStatus[projectDTOs[i].ID]
			canunblock := isManager[projectDTOs[i].ID]
			projectDTOs[i].CanUnblock = &canunblock
		}
		if isManager[projectDTOs[i].ID] {
			projectDTOs[i].CanManage = isManager[projectDTOs[i].ID]
		}
	}

	return &apistructs.PagingProjectDTO{Total: total, List: projectDTOs}, nil
}

// ListJoinedProjects 返回用户有权限的项目
func (p *Project) ListJoinedProjects(orgID int64, userID string, params *apistructs.ProjectListRequest) (
	*apistructs.PagingProjectDTO, error) {
	// 查找有权限的列表
	members, err := p.db.GetMembersByParentID(apistructs.ProjectScope, int64(params.OrgID), userID)
	if err != nil {
		return nil, errors.Errorf("failed to get permission when get projects, (%v)", err)
	}

	projectIDs := make([]uint64, 0, len(members))
	isManager := map[uint64]bool{}
	for i := range members {
		if members[i].ResourceKey == apistructs.RoleResourceKey {
			projectIDs = append(projectIDs, uint64(members[i].ScopeID))
			if members[i].ResourceValue == types.RoleProjectOwner ||
				members[i].ResourceValue == types.RoleProjectLead ||
				members[i].ResourceValue == types.RoleProjectPM {
				isManager[uint64(members[i].ScopeID)] = true
			} else {
				isManager[uint64(members[i].ScopeID)] = false
			}
		}
	}

	approves, err := p.db.ListUnblockApplicationApprove(uint64(orgID))
	if err != nil {
		return nil, errors.Errorf("failed to ListUnblockApplicationApprove: %v", err)
	}

	org, err := p.db.GetOrg(orgID)
	if err != nil {
		return nil, errors.Errorf("failed to getorg(%d): %v", orgID, err)
	}

	isOrgBlocked := false
	projectBlockStatus := map[uint64]string{}
	if org.BlockoutConfig.BlockDEV ||
		org.BlockoutConfig.BlockTEST ||
		org.BlockoutConfig.BlockStage ||
		org.BlockoutConfig.BlockProd {
		isOrgBlocked = true
		for _, i := range projectIDs {
			projectBlockStatus[i] = "blocked"
		}
	}

	for _, approve := range approves {
		if approve.Status == string(apistructs.ApprovalStatusPending) {
			projectBlockStatus[approve.TargetID] = "unblocking"
		}
	}
	// 获取项目列表
	total, projects, err := p.db.GetProjectsByIDs(projectIDs, params)
	if err != nil {
		return nil, errors.Errorf("failed to get projects, (%v)", err)
	}
	unblockAppCounts, err := p.ListUnblockAppCountsByProjectIDS(projectIDs)
	if err != nil {
		return nil, errors.Errorf("failed to get unblock apps, err: %v", err)
	}
	for _, counter := range unblockAppCounts {
		if counter.UnblockAppCount > 0 {
			projectBlockStatus[uint64(counter.ProjectID)] = "unblocked"
		}
	}

	// 转换成所需格式
	isJoined := map[int64]bool{}
	projectIDs = make([]uint64, 0, len(projects))
	for i := range projects {
		isJoined[projects[i].ID] = params.Joined
		projectIDs = append(projectIDs, uint64(projects[i].ID))
	}
	projectDTOs := p.BatchConvertProjectDTO(isJoined, projects)

	projectOwnerMap := make(map[uint64][]string)
	owners, err := p.db.GetMemberByScopeAndRole(apistructs.ProjectScope, projectIDs, []string{"owner"})
	if err != nil {
		return nil, err
	}
	for _, v := range owners {
		projectID := uint64(v.ScopeID)
		projectOwnerMap[projectID] = append(projectOwnerMap[projectID], v.UserID)
	}

	for i := range projectDTOs {
		if v, ok := projectOwnerMap[projectDTOs[i].ID]; ok {
			projectDTOs[i].Owners = v
		}
	}

	for i := range projectDTOs {
		if isOrgBlocked {
			projectDTOs[i].BlockStatus = projectBlockStatus[projectDTOs[i].ID]
			canunblock := isManager[projectDTOs[i].ID]
			projectDTOs[i].CanUnblock = &canunblock
		}
		if isManager[projectDTOs[i].ID] {
			projectDTOs[i].CanManage = isManager[projectDTOs[i].ID]
		}
	}

	return &apistructs.PagingProjectDTO{Total: total, List: projectDTOs}, nil
}

// ReferCluster 检查 cluster 是否被某个项目所使用
func (p *Project) ReferCluster(clusterName string, orgID uint64) bool {
	projects, err := p.db.ListProjectByOrgCluster(clusterName, orgID)
	if err != nil {
		logrus.Warnf("check cluster if referred by project")
		return true
	}
	if len(projects) > 0 {
		return true
	}

	return false
}

// UpdateProjectFunction 更新项目的功能开关
func (p *Project) UpdateProjectFunction(req *apistructs.ProjectFunctionSetRequest) (int64, error) {
	// 检查待更新的project是否存在
	project, err := p.db.GetProjectByID(int64(req.ProjectID))
	if err != nil {
		return 0, errors.Wrap(err, "failed to update project function")
	}

	var pf map[apistructs.ProjectFunction]bool
	json.Unmarshal([]byte(project.Functions), &pf)
	for k, v := range req.ProjectFunction {
		pf[k] = v
	}
	newFunction, err := json.Marshal(pf)
	if err != nil {
		return 0, errors.Wrap(err, "failed to update project function")
	}
	project.Functions = string(newFunction)

	if err = p.db.UpdateProject(&project); err != nil {
		logrus.Warnf("failed to update project, (%v)", err)
		return 0, errors.Errorf("failed to update project function")
	}

	return project.ID, nil
}

// UpdateProjectActiveTime 更新项目活跃时间
func (p *Project) UpdateProjectActiveTime(req *apistructs.ProjectActiveTimeUpdateRequest) error {
	// 检查待更新的project是否存在
	project, err := p.db.GetProjectByID(int64(req.ProjectID))
	if err != nil {
		return errors.Wrap(err, "failed to update project function")
	}

	if project.ActiveTime.Unix() > req.ActiveTime.Unix() {
		return nil
	}

	project.ActiveTime = req.ActiveTime
	if err = p.db.UpdateProject(&project); err != nil {
		logrus.Warnf("failed to update project, (%v)", err)
		return errors.Errorf("failed to update project function")
	}

	return nil
}

func checkRollbackConfig(rollbackConf *map[string]int) error {
	// DEV/TEST/STAGING/PROD
	l := len(*rollbackConf)

	// if empty then don't update
	if l == 0 {
		return nil
	}

	// check
	if l != 4 {
		return errors.Errorf("invalid param(clusterConfig is empty)")
	}

	for key := range *rollbackConf {
		switch key {
		case string(types.DevWorkspace), string(types.TestWorkspace), string(types.StagingWorkspace),
			string(types.ProdWorkspace):
		default:
			return errors.Errorf("invalid param, rollback config: %s", key)

		}
	}
	return nil
}

// initRollbackConfig init rollback config when create a project
func initRollbackConfig(rollbackConfig *map[string]int) error {
	if len(*rollbackConfig) != 4 {
		*rollbackConfig = map[string]int{
			string(types.DevWorkspace):     5,
			string(types.TestWorkspace):    5,
			string(types.StagingWorkspace): 5,
			string(types.ProdWorkspace):    5,
		}
	}
	return checkRollbackConfig(rollbackConfig)
}

func (p *Project) convertToProjectDTO(joined bool, project *model.Project, stats apistructs.ProjectStats) apistructs.ProjectDTO {
	l := logrus.WithField("func", "convertToProjectDTO")
	var rollbackConfig map[string]int
	if err := json.Unmarshal([]byte(project.RollbackConfig), &rollbackConfig); err != nil {
		rollbackConfig = make(map[string]int)
	}

	var clusterConfig = make(map[string]string)
	if err := json.Unmarshal([]byte(project.ClusterConfig), &clusterConfig); err != nil {
		l.WithError(err).Errorln("failed to Unmarshal project.ClusterConfig")
	}

	projectDto := apistructs.ProjectDTO{
		ID:             uint64(project.ID),
		Name:           project.Name,
		DisplayName:    project.DisplayName,
		Desc:           project.Desc,
		Logo:           filehelper.APIFileUrlRetriever(project.Logo),
		OrgID:          uint64(project.OrgID),
		Joined:         joined,
		Creator:        project.UserID,
		DDHook:         project.DDHook,
		Stats:          stats,
		ClusterConfig:  clusterConfig,
		RollbackConfig: rollbackConfig,
		CpuQuota:       project.CpuQuota,
		MemQuota:       project.MemQuota,
		CreatedAt:      project.CreatedAt,
		UpdatedAt:      project.UpdatedAt,
		ActiveTime:     project.ActiveTime.Format("2006-01-02 15:04:05"),
		Owners:         []string{},
		IsPublic:       project.IsPublic,
		Type:           project.Type,
	}
	if projectDto.DisplayName == "" {
		projectDto.DisplayName = projectDto.Name
	}

	return projectDto
}

func (p *Project) BatchConvertProjectDTO(joined map[int64]bool, projects []model.Project) []apistructs.ProjectDTO {
	l := logrus.WithField("func", "BatchConvertProjectDTO")

	projectIDs := make([]int64, 0, len(projects))
	for _, i := range projects {
		projectIDs = append(projectIDs, i.ID)
	}
	appCount, err := p.db.GetApplicationCountByProjectIDs(projectIDs)
	if err != nil {
		l.WithError(err).Errorln("failed to count app")
	}
	appCountMap := make(map[int64]int)
	for _, i := range appCount {
		appCountMap[i.ProjectID] = i.Count
	}

	memberCount, err := p.db.GetMemberCountByScopeIDs(apistructs.ProjectScope, projectIDs)
	if err != nil {
		l.WithError(err).Errorln("failed to count app")
	}
	memberCountMap := make(map[int64]int)
	for _, i := range memberCount {
		memberCountMap[i.ScopeID] = i.Count
	}
	res := make([]apistructs.ProjectDTO, 0, len(projects))
	for _, project := range projects {
		var join = true
		if joined != nil {
			join = joined[project.ID]
		}
		stats := apistructs.ProjectStats{
			CountApplications: appCountMap[project.ID],
			CountMembers:      memberCountMap[project.ID],
		}
		res = append(res, p.convertToProjectDTO(join, &project, stats))
	}
	return res
}

// GetProjectStats 获取项目状态
func (p *Project) GetProjectStats(projectID int64) (*apistructs.ProjectStats, error) {
	totalApp, err := p.db.GetApplicationCountByProjectID(projectID)
	if err != nil {
		return nil, errors.Errorf("get project states err: get app err: %v", err)
	}
	totalMembers, _, err := p.db.GetMembersWithoutExtraByScope(apistructs.ProjectScope, projectID)
	if err != nil {
		return nil, errors.Errorf("get project states err: get member err: %v", err)
	}
	return &apistructs.ProjectStats{
		CountApplications:      int(totalApp),
		CountMembers:           totalMembers,
		TotalApplicationsCount: int(totalApp),
		TotalMembersCount:      totalMembers,
	}, nil
}

// GetProjectNSInfo 获取项目级别命名空间信息
func (p *Project) GetProjectNSInfo(projectID int64) (*apistructs.ProjectNameSpaceInfo, error) {
	prj, err := p.db.GetProjectByID(projectID)
	if err != nil {
		return nil, err
	}
	prjIDStr := strconv.FormatInt(projectID, 10)

	prjNsInfo := &apistructs.ProjectNameSpaceInfo{
		Enabled:    false,
		Namespaces: make(map[string]string, 0),
	}

	if prj.EnableNS {
		prjNsInfo.Enabled = true
		prjNsInfo.Namespaces = genProjectNamespace(prjIDStr)
	}

	return prjNsInfo, nil
}

// help func
// genProjectNamespace 生成项目级命名空间
func genProjectNamespace(prjIDStr string) map[string]string {
	return map[string]string{"DEV": "project-" + prjIDStr + "-dev", "TEST": "project-" + prjIDStr + "-test",
		"STAGING": "project-" + prjIDStr + "-staging", "PROD": "project-" + prjIDStr + "-prod"}
}

func (p *Project) GetMyProjectIDList(parentID int64, userID string) ([]uint64, error) {
	members, err := p.db.GetMembersByParentID(apistructs.ProjectScope, parentID, userID)
	if err != nil {
		return nil, errors.Errorf("failed to get permission when get projects, (%v)", err)
	}

	projectIDList := make([]uint64, 0, len(members))
	for i := range members {
		if members[i].ResourceKey == apistructs.RoleResourceKey {
			projectIDList = append(projectIDList, uint64(members[i].ScopeID))
		}
	}
	return projectIDList, nil
}

func (p *Project) GetProjectIDListByStates(req apistructs.IssuePagingRequest, projectIDList []uint64) (int, []apistructs.ProjectDTO, error) {
	var res []apistructs.ProjectDTO
	total, pros, err := p.db.GetProjectIDListByStates(req, projectIDList)
	if err != nil {
		return total, res, err
	}
	res = p.BatchConvertProjectDTO(nil, pros)
	return total, res, nil
}

func (p *Project) GetQuotaOnClusters(orgID int64, clusterNames []string) (*apistructs.GetQuotaOnClustersResponse, error) {
	l := logrus.WithField("func", "GetQuotaOnClusters")

	var response = new(apistructs.GetQuotaOnClustersResponse)
	response.ClusterNames = clusterNames

	if len(clusterNames) == 0 {
		l.Warnln("no clusters for GetQuotaOnClusters")
		return response, nil
	}

	var clusterNamesM = make(map[string]bool)
	for _, clusterName := range clusterNames {
		clusterNamesM[clusterName] = true
	}

	// query all projects
	var projects []*model.Project
	if err := p.db.Find(&projects, map[string]interface{}{"org_id": orgID}).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			logrus.WithError(err).Warnln("project record not found")
			return response, nil
		}
		err = errors.Wrap(err, "failed to Find projects")
		l.WithError(err).Errorln()
		return nil, err
	}

	// query all project quota
	var projectIDs []int64
	for _, project := range projects {
		projectIDs = append(projectIDs, project.ID)
	}
	var projectsQuota []*apistructs.ProjectQuota
	if err := p.db.Where("project_id IN (?)", projectIDs).Find(&projectsQuota).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			l.WithError(err).Warnln("quota record not found")
			return response, nil
		}
		err = errors.Wrap(err, "failed to Find project quota")
		return nil, err
	}
	var projectsQuotaConfigs = make(map[int64]*apistructs.ProjectQuota)
	for _, projectQuota := range projectsQuota {
		projectsQuotaConfigs[int64(projectQuota.ProjectID)] = projectQuota
	}

	var ownerM = make(map[string]*apistructs.OwnerQuotaOnClusters)
	for _, project := range projects {
		// query project owner
		var member = model.Member{
			UserID: "0",
			Name:   "unknown",
			Nick:   "unknown",
		}
		if mb, ok := projectCache.GetMemberByProjectID(uint64(project.ID)); ok && mb.UserID != 0 {
			member.UserID = strconv.FormatUint(uint64(mb.UserID), 10)
			member.Name = mb.Name
			member.Nick = mb.Nick
		}

		owner, ok := ownerM[member.UserID]
		if !ok {
			userID, _ := strconv.ParseInt(member.UserID, 10, 64)
			owner = &apistructs.OwnerQuotaOnClusters{
				ID:       uint64(userID),
				Name:     member.Name,
				Nickname: member.Nick,
				CPUQuota: 0,
				MemQuota: 0,
				Projects: nil,
			}
			ownerM[member.UserID] = owner
		}

		projectQuotaOnCluster := apistructs.ProjectQuotaOnClusters{
			ID:          uint64(project.ID),
			Name:        project.Name,
			DisplayName: project.DisplayName,
			CPUQuota:    0,
			MemQuota:    0,
		}
		// if the project has quota config, accumulate it
		if config, ok := projectsQuotaConfigs[project.ID]; ok {
			// if a cluster of the project is in the specified clusters, its cpu and mem will be accumulated
			for clusterName, quota := range map[string][2]uint64{
				config.ProdClusterName:    {config.ProdCPUQuota, config.ProdMemQuota},
				config.StagingClusterName: {config.StagingCPUQuota, config.StagingMemQuota},
				config.TestClusterName:    {config.TestCPUQuota, config.TestMemQuota},
				config.DevClusterName:     {config.DevCPUQuota, config.DevMemQuota},
			} {
				if _, ok := clusterNamesM[clusterName]; ok {
					projectQuotaOnCluster.AccuQuota(quota[0], quota[1])
				}
			}
		}
		owner.Projects = append(owner.Projects, &projectQuotaOnCluster)
	}

	for _, owner := range ownerM {
		response.Owners = append(response.Owners, owner)
	}

	response.ReCalcu()

	return response, nil
}

func (p *Project) GetNamespacesBelongsTo(ctx context.Context, orgID uint64, clusterNames []string) (*apistructs.GetProjectsNamesapcesResponseData, error) {
	var (
		l            = logrus.WithField("func", "GetNamespacesBelongsTo")
		langCodes, _ = ctx.Value("lang_codes").(i18n.LanguageCodes)
		unknownName  = p.trans.Text(langCodes, "OwnerUnknown")
		projects     []*model.Project
		projectIDs   []uint64
		quotas       []*apistructs.ProjectQuota
		quotasM      = make(map[uint64]*apistructs.ProjectQuota)
		data         apistructs.GetProjectsNamesapcesResponseData
	)

	// 1) 查找项目列表
	if err := p.db.Find(&projects, map[string]interface{}{"org_id": orgID}).Error; err != nil {
		l.WithError(err).Errorln("failed to Find projects")
		if gorm.IsRecordNotFoundError(err) {
			return new(apistructs.GetProjectsNamesapcesResponseData), nil
		}
		return nil, err
	}
	for _, item := range projects {
		projectIDs = append(projectIDs, uint64(item.ID))
	}

	// 2) 查询 quota 表
	if err := p.db.Where("project_id IN (?)", projectIDs).Find(&quotas).Error; err != nil {
		l.WithError(err).Error("failed to Find quotas")
		if !gorm.IsRecordNotFoundError(err) {
			return nil, err
		}
	}
	for _, item := range quotas {
		quotasM[item.ProjectID] = item
	}

	// 3) 查询 quota and owner
	for _, proj := range projects {
		var item = &apistructs.ProjectNamespaces{
			ProjectID:          uint(proj.ID),
			ProjectName:        proj.Name,
			ProjectDisplayName: proj.DisplayName,
			ProjectDesc:        proj.Desc,
			// let owner default unknown
			OwnerUserID:       0,
			OwnerUserName:     unknownName,
			OwnerUserNickname: unknownName,
			Clusters:          make(map[string][]string),
		}
		item.PatchClusters(quotasM[uint64(item.ProjectID)], clusterNames)
		if namespaces, ok := projectCache.GetNamespacesByProjectID(uint64(proj.ID)); ok {
			item.PatchClustersNamespaces(namespaces.Namespaces)
		}

		item.PatchQuota(quotasM[uint64(item.ProjectID)])

		if member, ok := projectCache.GetMemberByProjectID(uint64(proj.ID)); ok && member.UserID != 0 {
			item.OwnerUserID = member.UserID
			item.OwnerUserName = member.Name
			item.OwnerUserNickname = member.Nick
		}

		data.List = append(data.List, item)
	}

	data.Total = uint32(len(data.List))
	return &data, nil
}

func (p *Project) ListQuotaRecords(ctx context.Context) ([]*apistructs.ProjectQuota, error) {
	var records []*apistructs.ProjectQuota
	if err := p.db.Find(&records).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return records, nil
}

func (p *Project) checkNewQuotaIsLessThanRequest(ctx context.Context, dto *apistructs.ProjectDTO, changeRecord map[string]bool) (string, bool) {
	if dto == nil || dto.ResourceConfig == nil {
		return "", true
	}
	langCodes, _ := ctx.Value("lang_codes").(i18n.LanguageCodes)
	var messages []string
	for workspace, resource := range map[string]*apistructs.ResourceConfigInfo{
		"PROD":    dto.ResourceConfig.PROD,
		"STAGING": dto.ResourceConfig.STAGING,
		"TEST":    dto.ResourceConfig.TEST,
		"DEV":     dto.ResourceConfig.DEV,
	} {
		if resource == nil {
			continue
		}
		if changeRecord != nil && !changeRecord[workspace] {
			continue
		}
		if resource.CPUQuota < resource.CPURequest {
			workspaceStr := p.trans.Text(langCodes, workspace)
			messages = append(messages, fmt.Sprintf(p.trans.Text(langCodes, "CPUQuotaIsLessThanRequest"), workspaceStr, resource.CPUQuota, resource.CPURequest))
		}
		if resource.MemQuota < resource.MemRequest {
			workspaceStr := p.trans.Text(langCodes, workspace)
			messages = append(messages, fmt.Sprintf(p.trans.Text(langCodes, "MemQuotaIsLessThanRequest"), workspaceStr, resource.MemQuota, resource.MemRequest))
		}
	}
	if len(messages) == 0 {
		return "", true
	}
	return strings.Join(messages, "; "), false
}

func (p *Project) ListUnblockAppCountsByProjectIDS(projectIDS []uint64) ([]model.ProjectUnblockAppCount, error) {
	if len(projectIDS) == 0 {
		return nil, nil
	}
	return p.db.ListUnblockAppCountsByProjectIDS(projectIDS)
}

func RunningPodCond(projectID uint64) map[string]interface{} {
	return map[string]interface{}{
		"project_id": strconv.FormatUint(projectID, 10),
		"phase":      "running",
	}
}
