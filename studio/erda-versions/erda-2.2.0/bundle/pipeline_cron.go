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

package bundle

import (
	"fmt"

	cronpb "github.com/erda-project/erda-proto-go/core/pipeline/cron/pb"
	"github.com/erda-project/erda-proto-go/core/pipeline/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
)

func (b *Bundle) CronCreate(req *cronpb.CronCreateRequest) (*pb.Cron, error) {
	host, err := b.urls.Pipeline()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var createResp apistructs.PipelineCronCreateResponse
	resp, err := hc.Post(host).Path("/api/pipeline-crons").
		Header(httputil.InternalHeader, "bundle").JSONBody(req).Do().JSON(&createResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !createResp.Success {
		return nil, toAPIError(resp.StatusCode(), createResp.Error)
	}

	return createResp.Data, nil
}

func (b *Bundle) CronUpdate(req *cronpb.CronUpdateRequest) error {
	host, err := b.urls.Pipeline()
	if err != nil {
		return err
	}
	hc := b.hc

	var createResp apistructs.PipelineCronUpdateResponse
	resp, err := hc.Put(host).Path(fmt.Sprintf("/api/pipeline-crons/%v", req.CronID)).
		Header(httputil.InternalHeader, "bundle").JSONBody(req).Do().JSON(&createResp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !createResp.Success {
		return toAPIError(resp.StatusCode(), createResp.Error)
	}

	return nil
}

func (b *Bundle) CronStart(req *cronpb.CronStartRequest) (*pb.Cron, error) {
	host, err := b.urls.Pipeline()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var result apistructs.PipelineCronStartResponse
	resp, err := hc.Put(host).Path(fmt.Sprintf("/api/pipeline-crons/%v/actions/start", req.CronID)).
		Header(httputil.InternalHeader, "bundle").JSONBody(req).Do().JSON(&result)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !result.Success {
		return nil, toAPIError(resp.StatusCode(), result.Error)
	}

	return result.Data, nil
}

func (b *Bundle) CronStop(req *cronpb.CronStopRequest) (*pb.Cron, error) {
	host, err := b.urls.Pipeline()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var result apistructs.PipelineCronStopResponse
	resp, err := hc.Put(host).Path(fmt.Sprintf("/api/pipeline-crons/%v/actions/stop", req.CronID)).
		Header(httputil.InternalHeader, "bundle").JSONBody(req).Do().JSON(&result)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !result.Success {
		return nil, toAPIError(resp.StatusCode(), result.Error)
	}

	return result.Data, nil
}

func (b *Bundle) GetCron(cronID uint64) (*pb.Cron, error) {
	host, err := b.urls.Pipeline()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var cronResp apistructs.PipelineCronGetResponse
	resp, err := hc.Get(host).Path(fmt.Sprintf("/api/pipeline-crons/%v", cronID)).
		Header(httputil.InternalHeader, "bundle").Do().JSON(&cronResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !cronResp.Success {
		return nil, toAPIError(resp.StatusCode(), cronResp.Error)
	}

	return cronResp.Data, nil
}

func (b *Bundle) DeleteCron(cronID uint64) error {
	host, err := b.urls.Pipeline()
	if err != nil {
		return err
	}
	hc := b.hc

	var cronResp apistructs.PipelineCronDeleteResponse
	resp, err := hc.Delete(host).Path(fmt.Sprintf("/api/pipeline-crons/%v", cronID)).
		Header(httputil.InternalHeader, "bundle").Do().JSON(&cronResp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !cronResp.Success {
		return toAPIError(resp.StatusCode(), cronResp.Error)
	}

	return nil
}
