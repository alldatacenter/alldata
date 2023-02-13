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

package dao

import (
	"time"

	"github.com/google/uuid"
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/apistructs"
)

type MemberActiveRank struct {
	ID            string `gorm:"primary_key"`
	CreatedAt     time.Time
	UpdatedAt     time.Time
	OrgID         string
	UserID        string
	IssueScore    uint64
	CommitScore   uint64
	QualityScore  uint64
	TotalScore    uint64
	SoftDeletedAt uint64
}

type MemberActiveScore struct {
	OrgID        string
	UserID       string
	IssueScore   uint64
	CommitScore  uint64
	QualityScore int64
	TotalScore   uint64
}

func (MemberActiveRank) TableName() string {
	return "erda_member_active_rank"
}

func (db *DBClient) GetMemberActiveRank(orgID, userID string) (*MemberActiveRank, error) {
	var res MemberActiveRank
	if err := db.Scopes(NotDeleted).Where("org_id = ? and user_id = ?", orgID, userID).First(&res).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			return &MemberActiveRank{}, nil
		}
		return nil, err
	}
	return &res, nil
}

func (db *DBClient) FindMemberRank(orgID, userID string) (*MemberActiveRank, uint64, error) {
	var res MemberActiveRank
	if err := db.Model(&MemberActiveRank{}).Scopes(NotDeleted).Where("org_id = ? and user_id = ?", orgID, userID).First(&res).Error; err != nil {
		if err != gorm.ErrRecordNotFound {
			return nil, 0, err
		}
		res.TotalScore = 0
	}
	var count uint64
	if err := db.Model(&MemberActiveRank{}).Scopes(NotDeleted).Where("org_id = ? and total_score > ?", orgID, res.TotalScore).Count(&count).Error; err != nil {
		return nil, 0, err
	}
	return &res, count + 1, nil
}

func (db *DBClient) GetMemberActiveRankList(orgID string, limit int) ([]MemberActiveRank, error) {
	var res []MemberActiveRank
	if err := db.Model(&MemberActiveRank{}).Scopes(NotDeleted).Where("org_id = ?", orgID).Order("total_score desc").Limit(limit).Find(&res).Error; err != nil {
		return nil, err
	}
	return res, nil
}

func (db *DBClient) CreateOrUpdateMemberActiveRank(r *MemberActiveScore) error {
	var res MemberActiveRank
	if err := db.Model(&MemberActiveRank{}).Scopes(NotDeleted).Where("org_id = ? and user_id = ?", r.OrgID, r.UserID).First(&res).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			id, err := uuid.NewRandom()
			if err != nil {
				return err
			}
			qualityScore := convertScore(r.QualityScore)
			return db.Create(&MemberActiveRank{
				ID:           id.String(),
				OrgID:        r.OrgID,
				UserID:       r.UserID,
				IssueScore:   r.IssueScore,
				CommitScore:  r.CommitScore,
				QualityScore: qualityScore,
				TotalScore:   r.IssueScore + r.CommitScore + qualityScore,
			}).Error
		}
		return err
	}
	fields := make(map[string]interface{})
	if r.IssueScore > 0 {
		fields["issue_score"] = res.IssueScore + r.IssueScore
	}
	if r.CommitScore > 0 {
		fields["commit_score"] = res.CommitScore + r.CommitScore
	}
	if r.QualityScore != 0 {
		fields["quality_score"] = convertScore(int64(res.QualityScore) + r.QualityScore)
	}
	if r.QualityScore < 0 {
		fields["total_score"] = res.IssueScore + res.CommitScore + convertScore(int64(res.QualityScore)+r.QualityScore)
	} else {
		fields["total_score"] = res.TotalScore + r.IssueScore + r.CommitScore + uint64(r.QualityScore)
	}
	return db.Model(&MemberActiveRank{}).Scopes(NotDeleted).Where("org_id = ? and user_id = ?", r.OrgID, r.UserID).Updates(fields).Error
}

func (db *DBClient) BatchClearScore() error {
	return db.Model(&MemberActiveRank{}).Scopes(NotDeleted).
		Updates(map[string]interface{}{"issue_score": 0, "commit_score": 0, "quality_score": 0, "total_score": 0}).Error
}

const (
	conState   = "left join dice_issue_state b ON a.state = b.id"
	conProject = "left join erda_project c on c.id = a.project_id"
	conRepo    = "left join dice_repos b on b.id = a.repo_id"
)

type conScore struct {
	OrgID  string
	UserID string
	Count  uint64
}

type conIssueScore struct {
	OrgID  string
	UserID string
	Count  uint64
	Type   apistructs.IssueType
}

func (db *DBClient) IssueScore() error {
	var res []conIssueScore
	sql := db.Table("dice_issues a").Joins(conState).Joins(conProject).Select("a.assignee as user_id, a.type, c.org_id, count(a.id) as count")
	sql = sql.Where("b.belong = ? and DATE(a.finish_time) >= DATE_SUB(CURDATE(),INTERVAL 30 DAY)", apistructs.IssueStateBelongDone)
	sql = sql.Where("a.deleted = 0 and a.assignee != '' and c.org_id != ''")
	if err := sql.Group("c.org_id, a.assignee, a.type").Find(&res).Error; err != nil {
		return err
	}

	for _, i := range res {
		if err := db.CreateOrUpdateMemberActiveRank(&MemberActiveScore{
			OrgID:      i.OrgID,
			UserID:     i.UserID,
			IssueScore: i.Count * issueScoreCoefficient(i),
		}); err != nil {
			return err
		}
	}
	return nil
}

func (db *DBClient) CommitScore() error {
	var res []conScore
	sql := db.Table("dice_repo_merge_requests a").Joins(conRepo).Select("a.author_id as user_id ,b.org_id, count(a.id) as count")
	sql = sql.Where("a.state = 'merged' and DATE(a.merge_at) >= DATE_SUB(CURDATE(),INTERVAL 30 DAY)")
	sql = sql.Where("a.author_id != '' and b.org_id != ''")
	if err := sql.Group("b.org_id, a.author_id").Find(&res).Error; err != nil {
		return err
	}

	for _, i := range res {
		if err := db.CreateOrUpdateMemberActiveRank(&MemberActiveScore{
			OrgID:       i.OrgID,
			UserID:      i.UserID,
			CommitScore: i.Count * commitCoefficient,
		}); err != nil {
			return err
		}
	}
	return nil
}

type conSeverityScore struct {
	OrgID    string
	UserID   string
	Count    uint64
	Severity apistructs.IssueSeverity
}

const issueCreateCoefficient = 0.8
const commitCoefficient = 2

func (db *DBClient) QualityScore() error {
	var issueCreated []conScore
	sql := db.Table("dice_issues a").Joins(conState).Joins(conProject).Select("a.creator as user_id, c.org_id, count(a.id) as count")
	sql = sql.Where("a.type = ?  and DATE(a.created_at) >= DATE_SUB(CURDATE(),INTERVAL 30 DAY)", apistructs.IssueTypeBug)
	sql = sql.Where("a.deleted = 0 and a.creator != '' and c.org_id != ''")
	if err := sql.Group("c.org_id, a.creator").Find(&issueCreated).Error; err != nil {
		return err
	}

	for _, i := range issueCreated {
		if err := db.CreateOrUpdateMemberActiveRank(&MemberActiveScore{
			OrgID:        i.OrgID,
			UserID:       i.UserID,
			QualityScore: int64(float64(i.Count) * issueCreateCoefficient),
		}); err != nil {
			return err
		}
	}

	var issueClosed []conScore
	sql = db.Table("dice_issues a").Joins(conState).Joins(conProject).Select("a.owner as user_id, c.org_id, count(a.id) as count")
	sql = sql.Where("a.type = ? and b.belong = ? and DATE(a.created_at) >= DATE_SUB(CURDATE(),INTERVAL 30 DAY)", apistructs.IssueTypeBug, apistructs.IssueStateBelongClosed)
	sql = sql.Where("a.deleted = 0 and a.owner != '' and c.org_id != ''")
	if err := sql.Group("c.org_id, a.owner").Find(&issueClosed).Error; err != nil {
		return err
	}

	for _, i := range issueClosed {
		if err := db.CreateOrUpdateMemberActiveRank(&MemberActiveScore{
			OrgID:        i.OrgID,
			UserID:       i.UserID,
			QualityScore: int64(i.Count),
		}); err != nil {
			return err
		}
	}

	var issueSeverity []conSeverityScore
	sql = db.Table("dice_issues a").Joins(conState).Joins(conProject).Select("a.owner as user_id, a.severity, c.org_id, count(a.id) as count")
	sql = sql.Where("DATE(a.created_at) >= DATE_SUB(CURDATE(),INTERVAL 30 DAY)")
	sql = sql.Where("a.type = ? and a.severity in (?)", apistructs.IssueTypeBug, []apistructs.IssueSeverity{apistructs.IssueSeverityFatal, apistructs.IssueSeveritySerious, apistructs.IssueSeverityNormal})
	sql = sql.Where("a.deleted = 0 and a.owner != '' and c.org_id != ''")
	if err := sql.Group("c.org_id, a.owner, a.severity").Find(&issueSeverity).Error; err != nil {
		return err
	}
	for _, i := range issueSeverity {
		if err := db.CreateOrUpdateMemberActiveRank(&MemberActiveScore{
			OrgID:        i.OrgID,
			UserID:       i.UserID,
			QualityScore: int64(i.Count) * qualityScoreCoefficient(i),
		}); err != nil {
			return err
		}
	}
	return nil
}

func qualityScoreCoefficient(s conSeverityScore) int64 {
	switch s.Severity {
	case apistructs.IssueSeverityFatal:
		return -10
	case apistructs.IssueSeveritySerious:
		return -5
	case apistructs.IssueSeverityNormal:
		return -1
	default:
		return 1
	}
}

func issueScoreCoefficient(s conIssueScore) uint64 {
	switch s.Type {
	case apistructs.IssueTypeRequirement:
		return 5
	case apistructs.IssueTypeTask:
		return 2
	default:
		return 1
	}
}

func convertScore(s int64) uint64 {
	if s < 0 {
		return 0
	}
	return uint64(s)
}
