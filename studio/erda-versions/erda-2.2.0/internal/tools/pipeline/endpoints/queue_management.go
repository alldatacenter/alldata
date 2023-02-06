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

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/internal/tools/pipeline/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
)

func (e *Endpoints) createPipelineQueue(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// validate request
	if r.ContentLength == 0 {
		return apierrors.ErrCreatePipelineQueue.MissingParameter("request body").ToResp(), nil
	}

	// decode request
	var req apistructs.PipelineQueueCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrCreatePipelineQueue.InvalidParameter(fmt.Errorf("failed to unmarshal request body, err: %v", err)).ToResp(), nil
	}

	// check authentication
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	if !identityInfo.IsInternalClient() {
		return apierrors.ErrCreatePipelineQueue.AccessDenied().ToResp(), nil
	}

	// do create
	queue, err := e.queueManage.CreatePipelineQueue(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(queue)
}

func (e *Endpoints) getPipelineQueue(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// validate request
	queueIDStr := vars[pathQueueID]
	queueID, err := strconv.ParseUint(queueIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrGetPipelineQueue.InvalidParameter(fmt.Errorf("invalid queueID: %s, err: %v", queueIDStr, err)).ToResp(), nil
	}

	// check authentication
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	if !identityInfo.IsInternalClient() {
		return apierrors.ErrGetPipelineQueue.AccessDenied().ToResp(), nil
	}

	// do get
	queue, err := e.queueManage.GetPipelineQueue(queueID)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	// set usage
	queue.Usage = e.queueManager.DistributedQueryQueueUsage(ctx, queue)

	return httpserver.OkResp(queue)
}

func (e *Endpoints) pagingPipelineQueues(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// handle request
	var req apistructs.PipelineQueuePagingRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrPagingPipelineQueues.InvalidParameter(err).ToResp(), nil
	}

	// check authentication
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	if !identityInfo.IsInternalClient() {
		return apierrors.ErrPagingPipelineQueues.AccessDenied().ToResp(), nil
	}

	// do paging
	queues, err := e.queueManage.PagingPipelineQueues(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(queues)
}

func (e *Endpoints) updatePipelineQueue(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// validate request
	if r.ContentLength == 0 {
		return httpserver.OkResp(nil)
	}

	// handle request
	var req apistructs.PipelineQueueUpdateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrUpdatePipelineQueue.InvalidParameter(err).ToResp(), nil
	}

	// get queue id
	queueIDStr := vars[pathQueueID]
	queueID, err := strconv.ParseUint(queueIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrUpdatePipelineQueue.InvalidParameter(fmt.Errorf("invalid queueID: %s, err: %v", queueIDStr, err)).ToResp(), nil
	}
	req.ID = queueID

	// check authentication
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	if !identityInfo.IsInternalClient() {
		return apierrors.ErrUpdatePipelineQueue.AccessDenied().ToResp(), nil
	}

	// do update
	queue, err := e.queueManage.UpdatePipelineQueue(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	// update queue in manager
	e.queueManager.DistributedUpdateQueue(ctx, queueID)

	return httpserver.OkResp(queue)
}

func (e *Endpoints) deletePipelineQueue(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// get queue id
	queueIDStr := vars[pathQueueID]
	queueID, err := strconv.ParseUint(queueIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrDeletePipelineQueue.InvalidParameter(fmt.Errorf("invalid queueID: %s, err: %v", queueIDStr, err)).ToResp(), nil
	}

	// check authentication
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	if !identityInfo.IsInternalClient() {
		return apierrors.ErrPagingPipelineQueues.AccessDenied().ToResp(), nil
	}

	// do update
	if err := e.queueManage.DeletePipelineQueue(queueID); err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(nil)
}

func (e *Endpoints) batchUpgradePipelinePriority(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var req apistructs.PipelinePriorityBatchUpgradeRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrUpgradePipelinePriority.InvalidParameter(fmt.Errorf("failed to unmarshal request body, err: %v", err)).ToResp(), nil
	}

	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	if !identityInfo.IsInternalClient() {
		return apierrors.ErrUpgradePipelinePriority.AccessDenied().ToResp(), nil
	}

	if req.QueueID == 0 {
		return nil, apierrors.ErrUpgradePipelinePriority.InvalidParameter("queueID")
	}
	if len(req.PipelineIDsOrderByPriorityFromHighToLow) == 0 {
		return nil, apierrors.ErrUpgradePipelinePriority.InvalidParameter("pipelineIDsOrderByPriorityFromHighToLow")
	}

	queue, err := e.queueManage.GetPipelineQueue(req.QueueID)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	e.queueManager.DistributedBatchUpdatePipelinePriority(ctx, queue.ID, req.PipelineIDsOrderByPriorityFromHighToLow)

	return httpserver.OkResp(nil)
}
