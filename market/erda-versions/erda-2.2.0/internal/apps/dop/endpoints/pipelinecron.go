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
	"fmt"
	"net/http"
	"strconv"

	"github.com/sirupsen/logrus"
	"google.golang.org/protobuf/types/known/wrapperspb"

	pb1 "github.com/erda-project/erda-proto-go/core/pipeline/base/pb"
	cronpb "github.com/erda-project/erda-proto-go/core/pipeline/cron/pb"
	common "github.com/erda-project/erda-proto-go/core/pipeline/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cron/crontypes"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
	"github.com/erda-project/erda/pkg/strutil"
)

func (e *Endpoints) pipelineCronPaging(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {

	var req apistructs.PipelineCronPagingRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrPagingPipelineCron.InvalidParameter(err).ToResp(), nil
	}

	var cronReq = &cronpb.CronPagingRequest{
		AllSources: req.AllSources,
		Sources: func() []string {
			var sources []string
			for _, v := range req.Sources {
				sources = append(sources, v.String())
			}
			return sources
		}(),
		YmlNames:             req.YmlNames,
		PageSize:             int64(req.PageSize),
		PageNo:               int64(req.PageNo),
		PipelineDefinitionID: req.PipelineDefinitionIDList,
	}
	if req.Enable != nil {
		cronReq.Enable = wrapperspb.Bool(*req.Enable)
	}

	result, err := e.PipelineCron.CronPaging(context.Background(), cronReq)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	return httpserver.OkResp(result)
}

func (e *Endpoints) pipelineCronStart(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {

	cronID, err := strconv.ParseUint(vars[pathCronID], 10, 64)
	if err != nil {
		return apierrors.ErrStartPipelineCron.InvalidParameter(err).ToResp(), nil
	}

	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrGetUser.InvalidParameter(err).ToResp(), nil
	}

	// get cron info for check permission
	result, err := e.PipelineCron.CronGet(context.Background(), &cronpb.CronGetRequest{
		CronID: cronID,
	})
	if err != nil {
		return errorresp.ErrResp(err)
	}
	if result.Data == nil {
		return errorresp.ErrResp(apierrors.ErrNotFoundPipelineCron.InternalError(crontypes.ErrCronNotFound))
	}
	cronInfo := result.Data

	appID, err := getAppIDFromCronExtraLabels(cronInfo)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	branch, err := getBranchFromCronExtraLabels(cronInfo)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	if err := e.permission.CheckRuntimeBranch(identityInfo, appID, branch, apistructs.OperateAction); err != nil {
		return errorresp.ErrResp(err)
	}

	// update CmsNsConfigs
	appDto, err := e.bdl.GetApp(appID)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	if err = e.UpdateCmsNsConfigs(identityInfo.UserID, appDto.OrgID); err != nil {
		return errorresp.ErrResp(err)
	}

	cron, err := e.PipelineCron.CronStart(context.Background(), &cronpb.CronStartRequest{
		CronID: cronID,
	})
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(cron)
}

func (e *Endpoints) pipelineCronStop(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {

	cronID, err := strconv.ParseUint(vars[pathCronID], 10, 64)
	if err != nil {
		return apierrors.ErrStopPipelineCron.InvalidParameter(err).ToResp(), nil
	}

	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrGetUser.InvalidParameter(err).ToResp(), nil
	}

	// get cron info for check permission
	result, err := e.PipelineCron.CronGet(context.Background(), &cronpb.CronGetRequest{
		CronID: cronID,
	})
	if err != nil {
		return errorresp.ErrResp(err)
	}
	if result.Data == nil {
		return errorresp.ErrResp(apierrors.ErrNotFoundPipelineCron.InternalError(crontypes.ErrCronNotFound))
	}
	cronInfo := result.Data

	appID, err := getAppIDFromCronExtraLabels(cronInfo)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	branch, err := getBranchFromCronExtraLabels(cronInfo)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	if err := e.permission.CheckRuntimeBranch(identityInfo, appID, branch, apistructs.OperateAction); err != nil {
		return errorresp.ErrResp(err)
	}

	cron, err := e.PipelineCron.CronStop(context.Background(), &cronpb.CronStopRequest{
		CronID: cronID,
	})
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(cron)
}

// pipelineCronCreate accept
func (e *Endpoints) pipelineCronCreate(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	var req pb1.PipelineCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrCreatePipelineCron.InvalidParameter(err).ToResp(), nil
	}

	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		logrus.Errorf("failed to get identityInfo when create pipeline cron, req: %+v, err: %v", req, err)
		return apierrors.ErrCreatePipelineCron.NotLogin().ToResp(), nil
	}
	if !identityInfo.IsInternalClient() {
		return apierrors.ErrCreatePipelineCron.AccessDenied().ToResp(), nil
	}

	pipelineYml, err := pipelineyml.New([]byte(req.PipelineYml))
	if err != nil {
		return nil, err
	}

	result, err := e.PipelineCron.CronCreate(context.Background(), &cronpb.CronCreateRequest{
		CronExpr:               pipelineYml.Spec().Cron,
		PipelineYmlName:        req.PipelineYmlName,
		PipelineSource:         req.PipelineSource,
		Enable:                 wrapperspb.Bool(false),
		PipelineYml:            req.PipelineYml,
		ClusterName:            req.ClusterName,
		FilterLabels:           req.Labels,
		NormalLabels:           req.NormalLabels,
		Envs:                   req.Envs,
		ConfigManageNamespaces: strutil.DedupSlice(append(req.ConfigManageNamespaces, req.ConfigManageNamespaces...), true),
		CronStartFrom:          req.CronStartFrom,
		IncomingSecrets:        req.GetSecrets(),
		PipelineDefinitionID:   req.DefinitionID,
	})
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(result.Data)
}

func (e *Endpoints) pipelineCronDelete(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	cronID, err := strconv.ParseUint(vars[pathCronID], 10, 64)
	if err != nil {
		return apierrors.ErrDeletePipelineCron.InvalidParameter(err).ToResp(), nil
	}

	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrGetUser.InvalidParameter(err).ToResp(), nil
	}

	// get cron info for check permission
	result, err := e.PipelineCron.CronGet(context.Background(), &cronpb.CronGetRequest{
		CronID: cronID,
	})
	if err != nil {
		return errorresp.ErrResp(err)
	}
	if result.Data == nil {
		return errorresp.ErrResp(apierrors.ErrNotFoundPipelineCron.InternalError(crontypes.ErrCronNotFound))
	}
	cronInfo := result.Data

	appID, err := getAppIDFromCronExtraLabels(cronInfo)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	branch, err := getBranchFromCronExtraLabels(cronInfo)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	if err := e.permission.CheckRuntimeBranch(identityInfo, appID, branch, apistructs.OperateAction); err != nil {
		return errorresp.ErrResp(err)
	}

	_, err = e.PipelineCron.CronDelete(context.Background(), &cronpb.CronDeleteRequest{
		CronID: cronID,
	})
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(nil)
}

func getAppIDFromCronExtraLabels(cronInfo *common.Cron) (uint64, error) {
	if cronInfo == nil || cronInfo.Extra == nil || cronInfo.Extra.NormalLabels == nil {
		return 0, fmt.Errorf("not find appID from cronInfo")
	}

	appIDStr := cronInfo.Extra.NormalLabels[apistructs.LabelAppID]
	appID, err := strconv.ParseInt(appIDStr, 10, 64)
	if err != nil {
		return 0, apierrors.ErrGetApp.InternalError(fmt.Errorf("app %v ParseInt error %v", appIDStr, err))
	}
	return uint64(appID), nil
}

func getBranchFromCronExtraLabels(cronInfo *common.Cron) (string, error) {
	if cronInfo == nil || cronInfo.Extra == nil || cronInfo.Extra.NormalLabels == nil {
		return "", fmt.Errorf("not find branch from cronInfo")
	}

	return cronInfo.Extra.NormalLabels[apistructs.LabelBranch], nil
}

// pipelineUpdate pipeline cron update
func (e *Endpoints) pipelineCronUpdate(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var req apistructs.GittarPushPayloadEvent
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrUpdatePipeline.InvalidParameter(err).ToResp(), nil
	}

	if err := e.pipeline.PipelineCronUpdate(req); err != nil {
		return apierrors.ErrUpdatePipeline.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp("ok")
}
