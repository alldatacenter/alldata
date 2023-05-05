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

package logic

import (
	"context"
	"encoding/json"
	"strconv"
	"time"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/actionagent"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/pipelinefunc"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/apitestsv2"
	"github.com/erda-project/erda/pkg/apitestsv2/cookiejar"
	"github.com/erda-project/erda/pkg/encoding/jsonparse"
	"github.com/erda-project/erda/pkg/loop"
	"github.com/erda-project/erda/pkg/metadata"
)

const (
	ResultSuccess = "success"
	ResultFailed  = "failed"
)

const (
	MetaKeyResult           = "result"
	metaKeyAPIRequest       = "api_request"
	metaKeyAPIResponse      = "api_response"
	metaKeyAPISetCookie     = "api_set_cookie"
	metaKeyAPIAssertSuccess = "api_assert_success" // true; false
	metaKeyAPIAssertDetail  = "api_assert_detail"
)

type Meta struct {
	Result          string
	AssertResult    bool
	AssertDetail    string
	Req             *apistructs.APIRequestInfo
	Resp            *apistructs.APIResp
	OutParamsDefine []apistructs.APIOutParam
	CookieJar       cookiejar.Cookies
	OutParamsResult map[string]interface{}
}

func NewMeta() *Meta {
	return &Meta{
		OutParamsResult: map[string]interface{}{},
	}
}

type KVs []kv
type kv struct {
	k string
	v string
}

func (kvs *KVs) add(k, v string) {
	*kvs = append(*kvs, kv{k, v})
}

func writeMetaFile(ctx context.Context, task *spec.PipelineTask, meta *Meta) {
	log := clog(ctx)

	// kvs 保证顺序
	kvs := &KVs{}

	kvs.add(MetaKeyResult, meta.Result)
	if meta.AssertDetail != "" {
		kvs.add(metaKeyAPIAssertSuccess, strconv.FormatBool(meta.AssertResult))
		kvs.add(metaKeyAPIAssertDetail, meta.AssertDetail)
	}
	if meta.Req != nil {
		kvs.add(metaKeyAPIRequest, jsonOneLine(ctx, meta.Req))
	}
	if meta.Resp != nil {
		kvs.add(metaKeyAPIResponse, jsonOneLine(ctx, meta.Resp))
		if len(meta.Resp.Headers) > 0 {
			if headerSetCookie := meta.Resp.Headers[apitestsv2.HeaderSetCookie]; len(headerSetCookie) > 0 {
				kvs.add(metaKeyAPISetCookie, jsonparse.JsonOneLine(headerSetCookie)) // format: []string
			}
		}
	}
	if len(meta.OutParamsResult) > 0 {
		for _, define := range meta.OutParamsDefine {
			v, ok := meta.OutParamsResult[define.Key]
			if !ok {
				continue
			}
			kvs.add(define.Key, jsonOneLine(ctx, v))
		}
	}

	var fields []*metadata.MetadataField
	for _, kv := range *kvs {
		fields = append(fields, &metadata.MetadataField{Name: kv.k, Value: kv.v})
	}

	var cb actionagent.Callback
	cb.AppendMetadataFields(fields)
	cb.PipelineID = task.PipelineID
	cb.PipelineTaskID = task.ID

	cbData, _ := json.Marshal(&cb)
	// apitest should ensure that callback to pipeline after doing request
	_ = loop.New(loop.WithDeclineRatio(2), loop.WithDeclineLimit(10*time.Second)).
		Do(func() (bool, error) {
			err := pipelinefunc.CallbackActionFunc(cbData)
			if err != nil {
				log.Errorf("failed to callback, err: %v", err)
				return false, err
			}
			return true, nil
		})
	return
}
