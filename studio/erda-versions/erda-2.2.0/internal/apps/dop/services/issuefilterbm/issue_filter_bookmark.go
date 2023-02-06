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

package issuefilterbm

import (
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/pkg/strutil"
)

// IssueFilterBookmark service handler.
type IssueFilterBookmark struct {
	db *dao.DBClient
}

// Option of IssueFilterBookmark.
type Option func(*IssueFilterBookmark)

// New IssueFilterBookmark service handler.
func New(options ...Option) *IssueFilterBookmark {
	is := &IssueFilterBookmark{}
	for _, op := range options {
		op(is)
	}
	return is
}

// WithDBClient for IssueFilterBookmark.
func WithDBClient(db *dao.DBClient) Option {
	return func(is *IssueFilterBookmark) {
		is.db = db
	}
}

type MyFilterBmMap map[string][]MyFilterBm

type MyFilterBm struct {
	ID           string
	Name         string
	PageKey      string
	FilterEntity string
}

func (mp *MyFilterBmMap) GetByPageKey(pageKey string) []MyFilterBm {
	return (*mp)[pageKey]
}

func (s *IssueFilterBookmark) ListMyBms(userID, projectID string) (*MyFilterBmMap, error) {
	bms, err := s.db.ListIssueFilterBookmarkByUserIDAndProjectID(userID, projectID)
	if err != nil {
		return nil, err
	}
	mp := make(MyFilterBmMap)
	for _, bm := range bms {
		mp[bm.PageKey] = append(mp[bm.PageKey], MyFilterBm{
			ID:           bm.ID,
			Name:         bm.Name,
			PageKey:      bm.PageKey,
			FilterEntity: bm.FilterEntity,
		})
	}
	return &mp, nil
}

func (s *IssueFilterBookmark) GenPageKey(fixedIteration, fixedIssueType string) string {
	return strutil.Join([]string{fixedIteration, fixedIssueType}, "-", true)
}

func (s *IssueFilterBookmark) Delete(id string) error {
	return s.db.DeleteIssueFilterBookmarkByID(id)
}

func (s *IssueFilterBookmark) Create(bm *dao.IssueFilterBookmark) (string, error) {
	err := s.db.CreateIssueFilterBookmark(bm)
	if err != nil {
		return "", err
	}
	return bm.ID, nil
}
