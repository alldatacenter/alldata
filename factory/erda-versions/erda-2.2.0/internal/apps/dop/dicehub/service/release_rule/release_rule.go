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

package release_rule

import (
	"fmt"
	"strings"
	"time"

	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/dbclient"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/service/apierrors"
	"github.com/erda-project/erda/pkg/crypto/uuid"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
)

// ReleaseRule is the handle to operate release rule
type ReleaseRule struct {
	db *dbclient.DBClient
}

//New returns a *ReleaseRule
func New(options ...Option) *ReleaseRule {
	var rule = new(ReleaseRule)
	for _, opt := range options {
		opt(rule)
	}
	return rule
}

// Create creates the release rule record
func (rule *ReleaseRule) Create(request *apistructs.CreateUpdateDeleteReleaseRuleRequest) (*apistructs.BranchReleaseRuleModel, *errorresp.APIError) {
	var l = logrus.WithField("func", "*ReleaseRule.Create").
		WithField("project_id", request.ProjectID)
	// 查找已有的分支制品规则, 检查要创建的模式是否已存在了
	var records []*apistructs.BranchReleaseRuleModel
	err := rule.db.Find(&records, map[string]interface{}{
		"project_id":      request.ProjectID,
		"soft_deleted_at": 0,
	}).Error
	if err != nil && !gorm.IsRecordNotFoundError(err) {
		l.WithError(err).Errorln("failed to Find records")
		return nil, apierrors.ErrCreateReleaseRule.InternalError(err)
	}
	var (
		creating = make(map[string]struct{})
	)
	creatingPatterns := strings.Split(request.Body.Pattern, ",")
	for _, pat := range creatingPatterns {
		creating[pat] = struct{}{}
	}
	for _, record := range records {
		pats := strings.Split(record.Pattern, ",")
		for _, pat := range pats {
			if _, ok := creating[pat]; ok {
				return nil, apierrors.ErrCreateReleaseRule.InvalidParameter(fmt.Sprintf("the rule %s is already exists", pat))
			}
		}
	}
	var record = &apistructs.BranchReleaseRuleModel{
		ID:        uuid.New(),
		ProjectID: request.ProjectID,
		Pattern:   request.Body.Pattern,
		IsEnabled: request.Body.IsEnabled,
	}
	if err = rule.db.Create(record).Error; err != nil {
		l.WithError(err).Errorf("failed to Create record: %+v", record)
		return nil, apierrors.ErrCreateReleaseRule.InternalError(err)
	}
	return record, nil
}

// List lists the release rules
func (rule *ReleaseRule) List(request *apistructs.CreateUpdateDeleteReleaseRuleRequest) (*apistructs.ListReleaseRuleResponse, *errorresp.APIError) {
	var l = logrus.WithField("func", "*ReleaseRule.List").
		WithField("project_id", request.ProjectID)
	var records []*apistructs.BranchReleaseRuleModel
	err := rule.db.Find(&records, map[string]interface{}{
		"project_id":      request.ProjectID,
		"soft_deleted_at": 0,
	}).Error
	if err != nil && !gorm.IsRecordNotFoundError(err) {
		l.WithError(err).Errorln("failed to Find records")
		return nil, apierrors.ErrListReleaseRule.InternalError(err)
	}
	if len(records) > 0 {
		return &apistructs.ListReleaseRuleResponse{
			Total: uint64(len(records)),
			List:  records,
		}, nil
	}
	record, apiError := rule.Create(&apistructs.CreateUpdateDeleteReleaseRuleRequest{
		OrgID:     request.OrgID,
		ProjectID: request.ProjectID,
		UserID:    request.UserID,
		Body: &apistructs.CreateUpdateReleaseRuleRequestBody{
			Pattern:   "release/*",
			IsEnabled: true,
		},
	})
	if apiError != nil {
		l.WithError(apiError).Errorln("failed to Create default release rule")
		return nil, apiError.InternalError(errors.New("failed to Create default release rule"))
	}
	return &apistructs.ListReleaseRuleResponse{
		Total: 1,
		List:  []*apistructs.BranchReleaseRuleModel{record},
	}, nil
}

// Update updates the release rule
func (rule *ReleaseRule) Update(request *apistructs.CreateUpdateDeleteReleaseRuleRequest) (*apistructs.BranchReleaseRuleModel, *errorresp.APIError) {
	var l = logrus.WithField("func", "*ReleaseRule.Update").
		WithField("project_id", request.ProjectID).
		WithField("id", request.RuleID)

	// 检查要修改的规则是否存在
	var record apistructs.BranchReleaseRuleModel
	err := rule.db.First(&record, map[string]interface{}{
		"id":              request.RuleID,
		"project_id":      request.ProjectID,
		"soft_deleted_at": 0,
	}).Error
	if gorm.IsRecordNotFoundError(err) {
		l.WithError(err).Errorln("the release rule not found")
		return nil, apierrors.ErrUpdateReleaseRule.InvalidParameter("the release rule not found")
	}
	if err != nil {
		l.WithError(err).Errorln("failed to First record")
		return nil, apierrors.ErrUpdateReleaseRule.InternalError(err)
	}

	// 检查要修改的 pattern 是否存在于其他记录中
	var records []*apistructs.BranchReleaseRuleModel
	err = rule.db.Find(&records, map[string]interface{}{
		"project_id":      request.ProjectID,
		"soft_deleted_at": 0,
	}).
		Not("id", request.RuleID).Error
	if err == nil {
		var updating = make(map[string]struct{})
		pats := strings.Split(request.Body.Pattern, ",")
		for _, pat := range pats {
			updating[pat] = struct{}{}
		}
		for _, record := range records {
			pats := strings.Split(record.Pattern, ",")
			for _, pat := range pats {
				if _, ok := updating[pat]; ok {
					l.Errorln("the pattern in the updating is exists in some other record")
					return nil, apierrors.ErrUpdateReleaseRule.InvalidParameter(fmt.Sprintf("the pattern %s in the updating is exists in some other record %s",
						pat, record.Pattern))
				}
			}
		}
	}
	if err != nil && !gorm.IsRecordNotFoundError(err) {
		l.WithError(err).Errorln("failed to Find")
		return nil, apierrors.ErrUpdateReleaseRule.InternalError(err)
	}

	record.Pattern = request.Body.Pattern
	record.IsEnabled = request.Body.IsEnabled
	if err = rule.db.Update(&record).Error; err != nil {
		l.WithError(err).Errorf("failed to Update: %+v", record)
		return nil, apierrors.ErrListReleaseRule.InternalError(err)
	}
	return &record, nil
}

// Delete deletes the release rule record
func (rule *ReleaseRule) Delete(request *apistructs.CreateUpdateDeleteReleaseRuleRequest) *errorresp.APIError {
	var l = logrus.WithField("func", "*Release.Delete").WithField("id", request.RuleID).WithField("project_id", request.ProjectID)
	var count uint64
	if err := rule.db.Model(new(apistructs.BranchReleaseRuleModel)).
		Where(map[string]interface{}{
			"project_id":      request.ProjectID,
			"soft_deleted_at": 0,
		}).
		Count(&count).Error; err != nil {
		l.WithError(err).Errorln("failed to Count")
		return apierrors.ErrDeleteReleaseRule.InternalError(err)
	}
	if count == 1 {
		l.Errorln("there is at least one release rule")
		return apierrors.ErrDeleteReleaseRule.InvalidState("there is at least one release rule")
	}
	if err := rule.db.Model(new(apistructs.BranchReleaseRuleModel)).
		Where(map[string]interface{}{"id": request.RuleID}).
		Update("soft_deleted_at", time.Now().UnixNano()/1e6).Error; err != nil && !gorm.IsRecordNotFoundError(err) {
		l.WithError(err).Errorln("failed to Delete")
		return apierrors.ErrDeleteReleaseRule.InternalError(err)
	}
	return nil
}

func (rule *ReleaseRule) Get(projectID uint64, ID string) (*apistructs.BranchReleaseRuleModel, *errorresp.APIError) {
	var l = logrus.WithField("func", "*Release.Delete").WithField("id", ID)
	var releaseRule *apistructs.BranchReleaseRuleModel
	if err := rule.db.Find(releaseRule, map[string]interface{}{
		"project_id": projectID,
		"id":         ID,
	}).Error; err != nil {
		l.WithError(err).Errorln("failed to get")
		return nil, apierrors.ErrGetReleaseRule.InternalError(err)
	}
	return releaseRule, nil
}
