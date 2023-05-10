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

package models

import (
	"encoding/json"
	"errors"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/gittar/pkg/gitmodule"
	"github.com/erda-project/erda/internal/tools/gittar/pkg/util/guid"
	"github.com/erda-project/erda/internal/tools/gittar/uc"
)

type NoteRequest struct {
	Note         string `json:"note"`
	Type         string `json:"type"`
	DiscussionId string `json:"discussionId"` //讨论id
	OldCommitId  string `json:"oldCommitId"`
	NewCommitId  string `json:"newCommitId"`
	OldPath      string `json:"oldPath"`
	NewPath      string `json:"newPath"`
	OldLine      int    `json:"oldLine"`
	NewLine      int    `json:"newLine"`
	Score        int    `json:"score" default:"-1"`
}

const MAX_NOTE_DIFF_LINE_COUNT = 15

var (
	NoteTypeNormal        = "normal"
	NoteTypeDiffNote      = "diff_note"
	NoteTypeDiffNoteReply = "diff_note_reply"
)

// Note struct
type Note struct {
	ID           int64                   `json:"id"`
	RepoID       int64                   `json:"repoId"`
	Type         string                  `json:"type" gorm:"size:150;index:idx_type"` // normal diff_note
	DiscussionId string                  `json:"discussionId"`                        //讨论id
	OldCommitId  string                  `json:"oldCommitId"`
	NewCommitId  string                  `json:"newCommitId"`
	MergeId      int64                   `json:"-" gorm:"index:idx_merge_id"`
	Note         string                  `json:"note" gorm:"type:text"`
	Data         string                  `json:"-" gorm:"type:text"`
	DataResult   NoteData                `json:"data" gorm:"-"`
	AuthorId     string                  `json:"authorId"`
	AuthorUser   *apistructs.UserInfoDto `json:"author",gorm:"-"`
	CreatedAt    time.Time               `json:"createdAt"`
	UpdatedAt    time.Time               `json:"updatedAt"`
	Score        int                     `json:"score" gorm:"size:150;index:idx_score"`
}

type NoteData struct {
	DiffLines   []*gitmodule.DiffLine `json:"diffLines"`
	OldPath     string                `json:"oldPath"`
	NewPath     string                `json:"newPath"`
	OldLine     int                   `json:"oldLine"`
	NewLine     int                   `json:"newLine"`
	OldCommitId string                `json:"oldCommitId"`
	NewCommitId string                `json:"newCommitId"`
}

func (svc *Service) CreateNote(repo *gitmodule.Repository, user *User, mergeId int64, note NoteRequest) (*Note, error) {
	switch note.Type {
	case NoteTypeNormal:
		return svc.CreateNormalNote(repo, user, mergeId, note)
	case NoteTypeDiffNote:
		return svc.CreateDiscussionNote(repo, user, mergeId, note)
	case NoteTypeDiffNoteReply:
		return svc.ReplyDiscussionNote(repo, user, mergeId, note)
	default:
		return nil, errors.New("not support note type")
	}
}

const (
	lineTypeOld = "old"
	lineTypeNew = "new"
)

func (svc *Service) CreateDiscussionNote(repo *gitmodule.Repository, user *User, mergeId int64, request NoteRequest) (*Note, error) {
	commitTo, err := repo.GetCommit(request.OldCommitId)
	if err != nil {
		return nil, err
	}
	commitFrom, err := repo.GetCommit(request.NewCommitId)
	if err != nil {
		return nil, err
	}

	diff, err := repo.GetDiffFile(commitFrom, commitTo, request.OldPath, request.NewPath)
	if err != nil {
		return nil, err
	}
	lineType := ""
	line := -1
	if request.OldLine > 0 {
		lineType = lineTypeOld
		line = request.OldLine
	} else if request.NewLine > 0 {
		lineType = lineTypeNew
		line = request.NewLine
	}

	//找出section
	var diffSection *gitmodule.DiffSection
	for _, section := range diff.Sections {
		for _, diffLine := range section.Lines {
			if lineType == lineTypeOld {
				if diffLine.OldLineNo == line {
					diffSection = section
					break
				}
			} else {
				if diffLine.NewLineNo == line {
					diffSection = section
					break
				}
			}
		}
		if diffSection != nil {
			break
		}
	}

	if diffSection == nil {
		return nil, errors.New("invalid lineNo")
	}

	var lines []*gitmodule.DiffLine
	for _, v := range diffSection.Lines {
		if lineType == lineTypeOld {
			if v.OldLineNo <= line {
				lines = append(lines, v)
			} else {
				break
			}
		}
		if lineType == lineTypeNew {
			if v.NewLineNo <= line {
				lines = append(lines, v)
			} else {
				break
			}
		}
	}

	lineCount := len(lines)
	if lineCount > MAX_NOTE_DIFF_LINE_COUNT {
		lines = lines[lineCount-MAX_NOTE_DIFF_LINE_COUNT : lineCount]
	}

	noteData := NoteData{
		DiffLines:   lines,
		OldLine:     request.OldLine,
		NewLine:     request.NewLine,
		OldPath:     request.OldPath,
		NewPath:     request.NewPath,
		OldCommitId: request.OldCommitId,
		NewCommitId: request.NewCommitId,
	}

	noteDataBytes, err := json.Marshal(noteData)
	if err != nil {
		return nil, err
	}

	Note := Note{
		RepoID:       repo.ID,
		MergeId:      mergeId,
		Note:         request.Note,
		AuthorId:     user.Id,
		OldCommitId:  request.OldCommitId,
		NewCommitId:  request.NewCommitId,
		DiscussionId: guid.NewString(),
		Type:         NoteTypeDiffNote,
		Data:         string(noteDataBytes),
	}
	err = svc.db.Create(&Note).Error
	if err != nil {
		return nil, err
	}
	return &Note, nil
}

func (svc *Service) ReplyDiscussionNote(repo *gitmodule.Repository, user *User, mergeId int64, request NoteRequest) (*Note, error) {
	var oldDiffNote Note
	err := svc.db.Where("repo_id = ? and merge_id=? and discussion_id= ?",
		repo.ID, mergeId, request.DiscussionId).
		Limit(1).First(&oldDiffNote).Error

	//是否已存在,不存在已有DiscussionId不能新增
	if err != nil {
		return nil, err
	}

	Note := Note{
		RepoID:       repo.ID,
		MergeId:      mergeId,
		Type:         NoteTypeDiffNoteReply,
		Note:         request.Note,
		AuthorId:     user.Id,
		DiscussionId: oldDiffNote.DiscussionId,
		OldCommitId:  oldDiffNote.OldCommitId,
		NewCommitId:  oldDiffNote.NewCommitId,
	}

	err = svc.db.Create(&Note).Error
	if err != nil {
		return nil, err
	}
	return &Note, nil
}

func (svc *Service) CreateNormalNote(repo *gitmodule.Repository, user *User, mergeId int64, note NoteRequest) (*Note, error) {
	Note := Note{
		MergeId:  mergeId,
		Note:     note.Note,
		AuthorId: user.Id,
		RepoID:   repo.ID,
		Type:     NoteTypeNormal,
		Score:    note.Score,
	}
	err := svc.db.Create(&Note).Error
	if err != nil {
		return nil, err
	}
	return &Note, nil
}

func (svc *Service) QueryAllNotes(repo *gitmodule.Repository, mergeId int64) ([]Note, error) {
	var notes []Note
	err := svc.db.Where("repo_id = ?  and merge_id=?", repo.ID, mergeId).Order("updated_at desc").Find(&notes).Error
	if err != nil {
		return nil, err
	}
	result := []Note{}
	for _, v := range notes {
		if v.AuthorId != "" {
			dto, err := uc.FindUserById(v.AuthorId)
			if err == nil {
				v.AuthorUser = dto
			} else {
				logrus.Errorf("get user from uc error: %v", err)
			}
		}
		if v.Data != "" {
			var data NoteData
			err := json.Unmarshal([]byte(v.Data), &data)
			if err != nil {
				logrus.Error(err)
			}
			v.DataResult = data
		}
		result = append(result, v)
	}
	return result, nil
}

func (svc *Service) QueryDiffNotes(repo *gitmodule.Repository, mergeId int64, oldCommitId string, newCommitId string) ([]Note, error) {
	var notes []Note
	err := svc.db.Where("repo_id = ?and merge_id=? and oldCommitId=? and newCommitId= ?",
		repo.ID, mergeId, oldCommitId, newCommitId).Find(&notes).Error
	if err != nil {
		return nil, err
	}
	for _, v := range notes {
		if v.AuthorId != "" {
			dto, err := uc.FindUserById(v.AuthorId)
			if err == nil {
				v.AuthorUser = dto
			} else {
				logrus.Errorf("get user from uc error: %v", err)
			}
		}
	}
	return notes, nil
}
