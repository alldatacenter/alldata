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

package dbclient

import (
	"database/sql/driver"
	"encoding/json"
	"fmt"
	"time"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/spec"
	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/parser/diceyml"
)

const deploymentsTableName = "ps_v2_deployments"

type Deployment struct {
	dbengine.BaseModel
	RuntimeId uint64 `gorm:"not null;index:idx_runtime_id"`
	ReleaseId string
	Outdated  bool
	// Deprecated: use ReleaseID instead, or only use for redundancy
	Dice string `gorm:"type:text"`
	// Deprecated
	BuiltDockerImages string                      `gorm:"type:text"`
	Operator          string                      `gorm:"not null;index:idx_operator"`
	Status            apistructs.DeploymentStatus `gorm:"not null;index:idx_status"`
	Phase             apistructs.DeploymentPhase  `gorm:"column:step"`
	FailCause         string                      `gorm:"type:text"`
	Extra             DeploymentExtra             `gorm:"type:text"`
	// 需要审批
	NeedApproval bool
	// userid
	ApprovedByUser string
	ApprovedAt     *time.Time
	ApprovalStatus string
	ApprovalReason string

	FinishedAt *time.Time
	BuildId    uint64
	Type       string
	DiceType   uint64
	// TODO: add a column to indicate normal deploy or rollback or redeploy ...
	// TODO: add a column rollbackFrom

	SkipPushByOrch    bool
	Param             string `gorm:"type:text"`
	DeploymentOrderId string
}

func (Deployment) TableName() string {
	return deploymentsTableName
}

type DeploymentExtra struct {
	FakeHealthyCount    uint64     `json:"fakeHealthyCount,omitempty"`
	AddonPhaseStartAt   *time.Time `json:"addonPhaseStartAt,omitempty"`
	AddonPhaseEndAt     *time.Time `json:"addonPhaseEndAt,omitempty"`
	ServicePhaseStartAt *time.Time `json:"servicePhaseStartAt,omitempty"`
	ServicePhaseEndAt   *time.Time `json:"servicePhaseEndAt,omitempty"`
	CancelStartAt       *time.Time `json:"cancelStartAt,omitempty"`
	CancelEndAt         *time.Time `json:"cancelEndAt,omitempty"`
	ForceCanceled       bool       `json:"forceCanceled,omitempty"`
	AutoTimeout         bool       `json:"autoTimeout,omitempty"`
}

func (ex DeploymentExtra) Value() (driver.Value, error) {
	if b, err := json.Marshal(ex); err != nil {
		return nil, errors.Wrapf(err, "failed to marshal DeploymentExtra")
	} else {
		return string(b), nil
	}
}

func (ex *DeploymentExtra) Scan(value interface{}) error {
	if value == nil {
		return nil
	}
	v, ok := value.([]byte)
	if !ok {
		return errors.New("invalid scan source for DeploymentExtra")
	}
	if len(v) == 0 {
		return nil
	}
	if err := json.Unmarshal(v, ex); err != nil {
		return errors.Wrapf(err, "failed to unmarshal DeploymentExtra")
	}
	return nil
}

// Deprecated
type PreDeployment struct {
	dbengine.BaseModel
	ApplicationId uint64 `gorm:"column:project_id;unique_index:idx_unique_project_env_branch"`
	Workspace     string `gorm:"column:env;unique_index:idx_unique_project_env_branch"`
	RuntimeName   string `gorm:"column:git_branch;unique_index:idx_unique_project_env_branch"`
	Dice          string `gorm:"type:text"`
	DiceOverlay   string `gorm:"type:text"`
	DiceType      uint64
}

func (PreDeployment) TableName() string {
	return "ps_v2_pre_builds"
}

func (db *DBClient) CreateDeployment(deployment *Deployment) error {
	if err := db.Save(deployment).Error; err != nil {
		return errors.Wrapf(err, "failed to create deployment, runtimeId: %d", deployment.RuntimeId)
	}
	return nil
}

func (db *DBClient) UpdateDeployment(deployment *Deployment) error {
	if err := db.Save(deployment).Error; err != nil {
		return errors.Wrapf(err, "failed to update deployment, id: %v, runtimeId: %v",
			deployment.ID, deployment.RuntimeId)
	}
	return nil
}

func (db *DBClient) GetDeployment(id uint64) (*Deployment, error) {
	var deployment Deployment
	if err := db.
		Where("id = ?", id).
		Find(&deployment).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to get deployment %d", id)
	}
	return &deployment, nil
}

type DeploymentFilter struct {
	StatusIn       []string
	NeedApproved   *bool
	Approved       *bool
	ApprovedByUser *string
	ApprovalStatus *string
	OperateUsers   []string
	Types          []string
	IDs            []uint64
}

func (db *DBClient) FindDeployments(runtimeId uint64, filter DeploymentFilter, offset int, limit int) ([]Deployment, int, error) {
	r := db.Where("runtime_id = ?", runtimeId)
	if len(filter.StatusIn) > 0 {
		r = r.Where("status in (?)", filter.StatusIn)
	}
	var total int
	var deployments []Deployment
	r = r.Order("id desc").Offset(offset).Limit(limit).Find(&deployments).
		// clear offset before count, bug: https://github.com/jinzhu/gorm/issues/1752
		Offset(0).Limit(-1).Count(&total)
	if err := r.Error; err != nil {
		return nil, 0, errors.Wrap(err, "failed to find deployments")
	}
	return deployments, total, nil
}

func (db *DBClient) FindAllDeployments(runtimeId uint64, filter DeploymentFilter) ([]Deployment, error) {
	r := db.Where("runtime_id = ?", runtimeId)
	if len(filter.StatusIn) > 0 {
		r = r.Where("status in (?)", filter.StatusIn)
	}
	var deployments []Deployment
	r = r.Order("id desc").Find(&deployments)
	if err := r.Error; err != nil {
		return nil, errors.Wrap(err, "failed to find deployments")
	}
	return deployments, nil
}

func (db *DBClient) FindMultiRuntimesDeployments(runtimeids []uint64, filter DeploymentFilter, offset int, limit int) ([]Deployment, int, error) {
	r := db.Where("runtime_id in (?)", runtimeids)
	if len(filter.StatusIn) > 0 {
		r = r.Where("status in (?)", filter.StatusIn)
	}
	if filter.NeedApproved != nil {
		r = r.Where(fmt.Sprintf("need_approval = %d", map[bool]int{false: 0, true: 1}[*filter.NeedApproved]))
	}
	if filter.ApprovedByUser != nil {
		r = r.Where("approved_by_user = ?", *filter.ApprovedByUser)
	}
	if len(filter.OperateUsers) > 0 {
		r = r.Where("operator in (?)", filter.OperateUsers)
	}
	if len(filter.Types) > 0 {
		r = r.Where("type in (?)", filter.Types)
	}
	if len(filter.IDs) > 0 {
		r = r.Where("id in (?)", filter.IDs)
	}
	if filter.ApprovalStatus != nil {
		if *filter.ApprovalStatus == "WaitApprove" {
			r = r.Where("approval_status = ?", *filter.ApprovalStatus)
			r = r.Where("status = ?", apistructs.DeploymentStatusWaitApprove)
		} else {
			r = r.Where("approval_status = ?", *filter.ApprovalStatus)
		}
	}
	if filter.Approved != nil {
		if *filter.Approved {
			r = r.Where("approved_at IS NOT NULL")
		} else {
			r = r.Where("approved_at IS NULL")
		}
	}
	var total int
	var deployments []Deployment
	r = r.Order("id desc").Offset(offset).Limit(limit).Find(&deployments).
		Offset(0).Limit(-1).Count(&total)
	if err := r.Error; err != nil {
		return nil, 0, errors.Wrap(err, "failed to find deployments")
	}
	return deployments, total, nil
}

// FindLastDeploymentIDsByRutimeIDs
func (db *DBClient) FindLastDeploymentIDsByRutimeIDs(runtimeIDs []uint64) ([]uint64, error) {
	var deploymentIDs []uint64
	var id uint64
	rows, err := db.Table(deploymentsTableName).
		Select("MAX(id) as id").
		Group("runtime_id").
		Having("runtime_id in (?)", runtimeIDs).Rows()
	if err != nil {
		return nil, errors.Wrapf(err, "failed to find last deployment, runtimeId: %v", runtimeIDs)
	}
	defer rows.Close()
	for rows.Next() {
		if err := rows.Scan(&id); err != nil {
			return nil, errors.Wrapf(err, "failed to find last deployment, runtimeId: %v", runtimeIDs)
		}
		deploymentIDs = append(deploymentIDs, id)
	}
	return deploymentIDs, nil
}

func (db *DBClient) FindDeploymentsByIDs(ids []uint64) (map[uint64]Deployment, error) {
	var (
		d           = make(map[uint64]Deployment)
		deployments []Deployment
	)

	if err := db.
		Where("id in (?)", ids).
		Find(&deployments).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to get deployment by ids: %+v", ids)
	}
	for _, deployment := range deployments {
		d[deployment.RuntimeId] = deployment
	}
	return d, nil
}

func (db *DBClient) FindUnfinishedDeployments() ([]Deployment, error) {
	var deployments []Deployment
	if err := db.
		Where("status in ('INIT', 'WAITING', 'DEPLOYING', 'CANCELING')").
		Find(&deployments).Error; err != nil {
		return nil, errors.Wrap(err, "failed to find unfinished deployments")
	}
	return deployments, nil
}

func (db *DBClient) FindSuccessfulDeployments(runtimeId uint64, limit int) ([]Deployment, error) {
	var deployments []Deployment
	if err := db.
		Where("runtime_id = ? AND status = 'OK'", runtimeId).
		Order("id desc").Limit(limit).
		Find(&deployments).Error; err != nil {
		return nil, errors.Wrap(err, "failed to find successful deployments")
	}
	return deployments, nil
}

// FindLastDeployment first deployment has no previous, nothing found does not matter
func (db *DBClient) FindLastDeployment(runtimeId uint64) (*Deployment, error) {
	var deployment Deployment
	r := db.
		Where("runtime_id = ?", runtimeId).Order("id desc").Limit(1).
		Find(&deployment)
	if r.Error != nil {
		if r.RecordNotFound() {
			return nil, nil
		}
		return nil, errors.Wrapf(r.Error, "failed to find last deployment, runtimeId: %v", runtimeId)
	}
	return &deployment, nil
}

func (db *DBClient) FindTopDeployments(runtimeId uint64, limit int) ([]Deployment, error) {
	var deployments []Deployment
	if err := db.
		Where("runtime_id = ?", runtimeId).
		Order("id desc").Limit(limit).
		Find(&deployments).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to find top %d deployments", limit)
	}
	return deployments, nil
}

// find not-outdated deployments older than maxId (id < maxId)
func (db *DBClient) FindNotOutdatedOlderThan(runtimeId uint64, maxId uint64) ([]Deployment, error) {
	var deployments []Deployment
	maxID, _ := driver.DefaultParameterConverter.ConvertValue(maxId)
	if err := db.Debug().
		Where("runtime_id = ? AND id < ? AND outdated = false", runtimeId, maxID).
		Find(&deployments).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to find not outdated deployments < %d, related runtime: %d",
			maxId, runtimeId)
	}
	return deployments, nil
}
func (db *DBClient) FindPreDeployment(uniqueId spec.RuntimeUniqueId) (*PreDeployment, error) {
	var preBuild PreDeployment
	if err := db.Table("ps_v2_pre_builds").
		Where("project_id = ? AND env = ? AND git_branch = ?", uniqueId.ApplicationId, uniqueId.Workspace, uniqueId.Name).
		Take(&preBuild).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to find PreDeployment, uniqueId: %v", uniqueId)
	}
	return &preBuild, nil
}

func (db *DBClient) FindPreDeploymentOrCreate(uniqueId spec.RuntimeUniqueId, dice *diceyml.DiceYaml) (*PreDeployment, error) {
	// 直接dice.yml对接，不用legace
	diceJson, err := dice.JSON()
	if err != nil {
		return nil, errors.Wrapf(err, "failed to FindPreDeploymentOrCreate, uniqueId is %v", uniqueId)
	}

	var pre PreDeployment
	result := db.Table("ps_v2_pre_builds").
		Where("project_id = ? AND env = ? AND git_branch = ?", uniqueId.ApplicationId, uniqueId.Workspace, uniqueId.Name).
		Take(&pre)
	isNoRecord := false
	if result.Error != nil {
		if result.RecordNotFound() {
			isNoRecord = true
		} else {
			return nil, errors.Wrapf(result.Error, "failed to FindPreDeploymentOrCreate, uniqueId is %v", uniqueId)
		}
	}
	if isNoRecord {
		pre = PreDeployment{
			ApplicationId: uniqueId.ApplicationId,
			Workspace:     uniqueId.Workspace,
			RuntimeName:   uniqueId.Name,
			Dice:          diceJson,
			DiceType:      1,
			DiceOverlay:   "",
		}
	} else {
		pre.Dice = diceJson
	}

	if err := db.Table("ps_v2_pre_builds").Save(&pre).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to FindPreDeploymentOrCreate, uniqueId is %v", uniqueId)
	}
	return &pre, nil
}

func (db *DBClient) UpdatePreDeployment(pre *PreDeployment) error {
	if err := db.Table("ps_v2_pre_builds").Save(pre).Error; err != nil {
		return errors.Wrapf(err, "failed to update PreDeployment, pre: %v", pre)
	}
	return nil
}

func (db *DBClient) ResetPreDice(uniqueId spec.RuntimeUniqueId) error {
	if err := db.Table("ps_v2_pre_builds").
		Where("project_id = ? AND env = ? AND git_branch = ?", uniqueId.ApplicationId, uniqueId.Workspace, uniqueId.Name).
		Update("dice_overlay", "").Error; err != nil {
		return errors.Wrapf(err, "failed to reset PreDice, uniqueId: %v", uniqueId)
	}
	return nil
}

// TODO: refactor the convert logic
func (d *Deployment) Convert() *apistructs.Deployment {
	if d == nil {
		return nil
	}
	return &apistructs.Deployment{
		ID:             d.ID,
		RuntimeID:      d.RuntimeId,
		BuildID:        d.BuildId,
		ReleaseID:      d.ReleaseId,
		Type:           d.Type,
		Status:         d.Status,
		Phase:          d.Phase,
		Step:           d.Phase,
		FailCause:      d.FailCause,
		Outdated:       d.Outdated,
		Operator:       d.Operator,
		RollbackFrom:   0,
		CreatedAt:      d.CreatedAt,
		FinishedAt:     d.FinishedAt,
		NeedApproval:   d.NeedApproval,
		ApprovedByUser: d.ApprovedByUser,
		ApprovedAt:     d.ApprovedAt,
		ApprovalStatus: d.ApprovalStatus,
		ApprovalReason: d.ApprovalReason,
	}
}
