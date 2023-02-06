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

package contribution

import (
	"context"
	"fmt"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/dop/contribution/pb"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/pkg/common/apis"
)

type contributionService struct {
	logger logs.Logger
	db     *dao.DBClient
	i18n   i18n.Translator
}

func (s *contributionService) GetPersonalContribution(ctx context.Context, req *pb.GetPersonalContributionRequest) (*pb.GetPersonalContributionResponse, error) {
	userID := apis.GetUserID(ctx)
	if userID == "" {
		return nil, fmt.Errorf("not login")
	}
	if req.UserID == "" || req.OrgID == "" {
		return nil, fmt.Errorf("invalid parameters")
	}
	rank, err := s.db.GetMemberActiveRank(req.OrgID, req.UserID)
	if err != nil {
		return nil, err
	}
	data := &pb.PersonalContribution{
		Data: &pb.Contribution{
			Events:  rank.IssueScore,
			Commits: rank.CommitScore,
			Cases:   rank.QualityScore,
		},
	}
	pivot := findMaxSlice([]uint64{rank.IssueScore, rank.CommitScore, rank.QualityScore})
	// default max value
	if pivot == 0 {
		pivot = 1
	}
	lang := apis.Language(ctx)
	indicator := &pb.Indicator{
		Data: []*pb.IndicatorData{
			{Data: []uint64{data.Data.Events, data.Data.Commits, data.Data.Cases}},
		},
		Max:   []uint64{pivot, pivot, pivot},
		Title: []string{s.i18n.Text(lang, "coordination"), s.i18n.Text(lang, "code"), s.i18n.Text(lang, "quality")},
	}
	data.Indicators = indicator
	return &pb.GetPersonalContributionResponse{Data: data}, nil
}

func findMaxSlice(v []uint64) (m uint64) {
	if len(v) > 0 {
		m = v[0]
	}
	for _, i := range v {
		if i > m {
			m = i
		}
	}
	return
}

func (s *contributionService) GetActiveRank(ctx context.Context, req *pb.GetActiveRankRequest) (*pb.GetActiveRankRequestResponse, error) {
	userID := apis.GetUserID(ctx)
	if userID == "" {
		return nil, fmt.Errorf("not login")
	}

	list, err := s.db.GetMemberActiveRankList(req.OrgID, 10)
	if err != nil {
		return nil, err
	}
	var res []*pb.UserRank
	var userIDs []string
	var meExist bool
	for i, v := range list {
		res = append(res, &pb.UserRank{
			Id:    v.UserID,
			Rank:  uint64(i + 1),
			Value: v.TotalScore,
		})
		userIDs = append(userIDs, v.UserID)
		if !meExist && v.UserID == userID {
			meExist = true
		}
	}

	if len(list) > 0 && !meExist {
		rankInfo, rank, err := s.db.FindMemberRank(req.OrgID, userID)
		if err != nil {
			return nil, err
		}
		res = append(res, &pb.UserRank{
			Id:    userID,
			Rank:  rank,
			Value: rankInfo.TotalScore,
		})
		userIDs = append(userIDs, userID)
	}

	return &pb.GetActiveRankRequestResponse{Data: res, UserIDs: userIDs}, nil
}
