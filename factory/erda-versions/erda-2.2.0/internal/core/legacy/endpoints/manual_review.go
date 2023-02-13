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

package endpoints

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"strconv"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/conf"
	"github.com/erda-project/erda/internal/core/legacy/services/apierrors"
	"github.com/erda-project/erda/internal/core/legacy/utils"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/i18n"
	"github.com/erda-project/erda/pkg/loop"
	"github.com/erda-project/erda/pkg/strutil"
)

func (e *Endpoints) GetReviewByTaskId(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {

	var review apistructs.GetReviewByTaskIdIdRequest
	taskID, err := strutil.Atoi64(vars["taskId"])
	if err != nil {
		return apierrors.ErrGetReviewByTaskId.InvalidParameter(err).ToResp(), nil
	}
	review.TaskId = taskID
	// 创建/更新成员信息至DB
	result, err := e.ManualReview.GetReviewByTaskId(&review)
	if err != nil {
		return apierrors.ErrGetReviewByTaskId.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(apistructs.GetReviewByTaskIdIdResponse{Total: result.Total, ApprovalStatus: result.ApprovalStatus, Id: result.Id})
}

func (e *Endpoints) CreateReviewUser(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	// 获取当前用户
	orgID, err := strconv.ParseInt(r.Header.Get("Org-ID"), 10, 64)
	if err != nil {
		return apierrors.ErrCreateReviewUser.MissingParameter("Org-ID header is nil").ToResp(), nil
	}
	// 检查body是否为空
	if r.Body == nil {
		return apierrors.ErrCreateReviewUser.MissingParameter("body").ToResp(), nil
	}
	// 检查body格式
	var reviewCreateReq apistructs.CreateReviewUser
	if err := json.NewDecoder(r.Body).Decode(&reviewCreateReq); err != nil {
		return apierrors.ErrCreateReviewUser.InvalidParameter(err).ToResp(), nil
	}
	reviewCreateReq.OrgId = orgID

	// 创建/更新成员信息至DB
	var resp apistructs.CreateReviewUserResponse
	var insertID int64
	if err, insertID = e.ManualReview.CreateReviewUser(&reviewCreateReq); err != nil {
		return apierrors.ErrCreateReviewUser.InternalError(err).ToResp(), nil
	}

	ucUser, err := e.uc.GetUser(reviewCreateReq.Operator)
	if err != nil {
		return apierrors.ErrCreateReviewUser.InternalError(err).ToResp(), nil
	}
	userInfo := convertToUserInfo(ucUser, false)
	resp.OperatorUserInfo = userInfo
	resp.ID = insertID

	return httpserver.OkResp(resp)
}

func (e *Endpoints) CreateReview(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	// 获取当前用户
	orgID, err := strconv.ParseInt(r.Header.Get("Org-ID"), 10, 64)
	if err != nil {
		return apierrors.ErrCreateReview.MissingParameter("Org-ID header is nil").ToResp(), nil
	}

	// 检查body是否为空
	if r.Body == nil {
		return apierrors.ErrCreateReview.MissingParameter("body").ToResp(), nil
	}
	// 检查body格式
	var reviewCreateReq apistructs.CreateReviewRequest
	if err := json.NewDecoder(r.Body).Decode(&reviewCreateReq); err != nil {
		return apierrors.ErrCreateReview.InvalidParameter(err).ToResp(), nil
	}
	reviewCreateReq.OrgId = orgID
	// 创建/更新成员信息至DB
	var reviewID int64
	if err, reviewID = e.ManualReview.CreateReview(&reviewCreateReq); err != nil {
		return apierrors.ErrCreateReview.InternalError(err).ToResp(), nil
	}
	// creat eventBox message
	go func() {
		if err := loop.New(loop.WithInterval(time.Second), loop.WithMaxTimes(3)).
			Do(func() (bool, error) {
				return e.createEventBoxMessage(&reviewCreateReq)
			}); err != nil {
			logrus.Errorf("fail to createEventBoxMessage, err: %s", err.Error())
		}
	}()
	return httpserver.OkResp(reviewID)
}

// createEventBox create eventBox message
func (e *Endpoints) createEventBoxMessage(req *apistructs.CreateReviewRequest) (bool, error) {
	org, err := e.db.GetOrg(req.OrgId)
	if err != nil {
		return false, err
	}

	sponsor, err := e.uc.GetUser(req.SponsorId)
	if err != nil {
		return false, err
	}

	reviewers, err := e.db.GetOperatorByTaskID([]int{req.TaskId})
	if err != nil {
		return false, err
	}
	if len(reviewers) == 0 {
		return false, errors.New("the reviewers' len is 0")
	}

	err = e.bdl.CreateMboxNotify("notify.deployapproval.launch.markdown_template",
		map[string]string{
			"title":       fmt.Sprintf("【重要】请及时审核%s项目%s应用部署合规性", req.ProjectName, req.ApplicationName),
			"member":      sponsor.Name,
			"projectname": req.ProjectName,
			"appName":     req.ApplicationName,
			"url": fmt.Sprintf("%s://%s/%s/dop/projects/%d/apps/%d/pipeline?pipelineID=%d",
				utils.GetProtocol(), conf.UIDomain(), org.Name, req.ProjectId, req.ApplicationId, req.BuildId),
		},
		i18n.ZH, uint64(req.OrgId), []string{reviewers[0].Operator})
	if err != nil {
		return false, err
	}
	return true, nil
}

func (e *Endpoints) GetReviewsBySponsorId(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	// 获取当前用户
	orgID, err := strconv.ParseInt(r.Header.Get("Org-ID"), 10, 64)
	if err != nil {
		return apierrors.ErrGetReviewsBySponsorId.MissingParameter("Org-ID header is nil").ToResp(), nil
	}
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrListLibReference.NotLogin().ToResp(), nil
	}
	var reviews apistructs.GetReviewsBySponsorIdRequest
	if err := e.queryStringDecoder.Decode(&reviews, r.URL.Query()); err != nil {
		return apierrors.ErrListLibReference.InvalidParameter(err).ToResp(), nil
	}
	user, _ := strconv.ParseInt(identityInfo.UserID, 10, 64)
	reviews.SponsorId = user
	reviews.OrgId = orgID
	total, list, err := e.ManualReview.GetReviewsBySponsorId(&reviews)

	uesrIDsMap := make(map[string]int)
	var userIDs []string
	for _, operators := range list {
		for _, node := range operators.Approver {
			if _, ok := uesrIDsMap[node]; ok {
				continue
			} else {
				userIDs = append(userIDs, node)
			}
		}
	}

	if err != nil {
		return apierrors.ErrGetReviewsBySponsorId.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(apistructs.ReviewsBySponsorList{List: list, Total: total}, strutil.DedupSlice(userIDs))
}

func (e *Endpoints) GetReviewsByUserId(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	// 获取当前用户
	orgID, err := strconv.ParseInt(r.Header.Get("Org-ID"), 10, 64)
	if err != nil {
		return apierrors.ErrGetReviewsBySponsorId.MissingParameter("Org-ID header is nil").ToResp(), nil
	}

	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrListLibReference.NotLogin().ToResp(), nil
	}

	var reviews apistructs.GetReviewsByUserIdRequest
	if err := e.queryStringDecoder.Decode(&reviews, r.URL.Query()); err != nil {
		return apierrors.ErrListLibReference.InvalidParameter(err).ToResp(), nil
	}
	user, _ := strconv.ParseInt(identityInfo.UserID, 10, 64)
	reviews.UserId = user
	reviews.OrgId = orgID
	total, list, err := e.ManualReview.GetReviewsByUserId(&reviews)
	if err != nil {
		return apierrors.ErrGetReviewsBySponsorId.InternalError(err).ToResp(), nil
	}
	var userIDs []string
	for _, node := range list {
		userIDs = append(userIDs, node.Operator)
	}
	return httpserver.OkResp(apistructs.ReviewsByUserList{List: list, Total: total}, strutil.DedupSlice(userIDs))
}

func (e *Endpoints) UpdateApproval(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	orgID, err := strconv.ParseInt(r.Header.Get("Org-ID"), 10, 64)
	if err != nil {
		return apierrors.ErrGetReviewsBySponsorId.MissingParameter("Org-ID header is nil").ToResp(), nil
	}
	if r.Body == nil {
		return apierrors.ErrUpdateApproval.MissingParameter("body").ToResp(), nil
	}
	// 检查body格式
	var reviewUpdateReq apistructs.UpdateApproval
	if err := json.NewDecoder(r.Body).Decode(&reviewUpdateReq); err != nil {
		return apierrors.ErrUpdateApproval.InvalidParameter(err).ToResp(), nil
	}
	reviewUpdateReq.OrgId = orgID
	// 创建/更新成员信息至DB
	if err := e.ManualReview.UpdateApproval(&reviewUpdateReq); err != nil {
		return apierrors.ErrUpdateApproval.InternalError(err).ToResp(), nil
	}
	// creat eventBox message
	go func() {
		if err := loop.New(loop.WithInterval(time.Second), loop.WithMaxTimes(3)).
			Do(func() (bool, error) {
				return e.createApproveDoneEventBoxMessage(reviewUpdateReq.Id)
			}); err != nil {
			logrus.Errorf("fail to createApproveDoneEventBoxMessage, err: %s", err.Error())
		}
	}()
	return httpserver.OkResp("update review succ")
}

// createApproveDoneEventBoxMessage create approve done eventBox message
func (e *Endpoints) createApproveDoneEventBoxMessage(id int64) (bool, error) {
	review, err := e.db.GetReviewByID(id)
	if err != nil {
		return false, err
	}

	org, err := e.db.GetOrg(review.OrgId)
	if err != nil {
		return false, err
	}

	err = e.bdl.CreateMboxNotify("notify.deployapproval.done.markdown_template",
		map[string]string{
			"title":       fmt.Sprintf("【通知】%s项目%s应用部署审核完成", review.ProjectName, review.ApplicationName),
			"projectName": review.ProjectName,
			"appName":     review.ApplicationName,
			"url": fmt.Sprintf("%s://%s/%s/dop/projects/%d/apps/%d/pipeline?pipelineID=%d",
				utils.GetProtocol(), conf.UIDomain(), org.Name, review.ProjectId, review.ApplicationId, review.BuildId),
		}, i18n.ZH, uint64(review.OrgId), []string{review.SponsorId})
	if err != nil {
		return false, err
	}
	return true, nil
}

func (e *Endpoints) GetAuthorityByUserId(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	// 获取当前用户
	orgID, err := strconv.ParseInt(r.Header.Get("Org-ID"), 10, 64)
	if err != nil {
		return apierrors.ErrGetReviewsBySponsorId.MissingParameter("Org-ID header is nil").ToResp(), nil
	}

	var reviews apistructs.GetAuthorityByUserIdRequest
	if err := e.queryStringDecoder.Decode(&reviews, r.URL.Query()); err != nil {
		return apierrors.ErrListLibReference.InvalidParameter(err).ToResp(), nil
	}
	reviews.OrgId = orgID
	authority, err := e.ManualReview.GetAuthorityByUserId(&reviews)
	if err != nil {
		return apierrors.ErrGetReviewsBySponsorId.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(authority)
}
