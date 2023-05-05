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
	"encoding/json"
	"strconv"

	"github.com/getkin/kin-openapi/openapi3"
	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/swagger/oas3"
)

// SearchAPIOperationHandle 操作集市 API 搜索的手柄
type SearchAPIOperationHandle interface {
	// SearchAPIOperations 从集市中搜索 API 列表
	// keyword 可以为接口名(对应 oas3 文件中的 operationId) 或路径
	SearchAPIOperations(keyword string) ([]*apistructs.APIOperationSummary, error)

	// APIOperationSummary 接口摘要信息, 作为搜索结果列表的 item
	// 其中 AssetID + Version + Path + Method 能确定唯一的一篇文档
	GetAPIOperation(assetID, version, path, method string) (*apistructs.APIOperation, error)
}

// SearchAPIOperations 从集市中搜索 API 列表
// keyword 可以为接口名(对应 oas3 文件中的 operationId) 或路径
func (b *Bundle) SearchAPIOperations(orgID uint64, userID string, keyword string) ([]*apistructs.APIOperationSummary, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return nil, err
	}

	var response apistructs.BaseResponse
	resp, err := b.hc.Get(host).Path("/api/apim/operations").
		Param("keyword", keyword).
		Header("Internal-Client", "bundle").
		Header("User-ID", userID).
		Header("Org-ID", strconv.FormatUint(orgID, 10)).
		Do().
		JSON(&response)
	if err != nil {
		return nil, err
	}
	if !resp.IsOK() || !response.Success {
		return nil, errors.Errorf("failed to Get operations, %+v", response.Err)
	}

	var indices []*apistructs.APIOAS3IndexModel
	if err = json.Unmarshal(response.Data, &indices); err != nil {
		return nil, err
	}

	var results []*apistructs.APIOperationSummary
	for _, index := range indices {
		results = append(results, &apistructs.APIOperationSummary{
			ID:          index.ID,
			AssetID:     index.AssetID,
			AssetName:   index.AssetName,
			Version:     index.InfoVersion,
			Path:        index.Path,
			Method:      index.Method,
			OperationID: index.OperationID,
		})
	}

	return results, nil
}

// GetAPIOperation 查询 operation 详情
// id: apistructs.APIOperationSummary{}.ID
func (b *Bundle) GetAPIOperation(orgID uint64, userID string, id uint64) (*apistructs.APIOperation, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return nil, err
	}

	// 发送请求
	var response apistructs.BaseResponse
	resp, err := b.hc.Get(host).Path("/api/apim/operations/"+strconv.FormatUint(id, 10)).
		Header("Internal-Client", "bundle").
		Header("User-ID", userID).
		Header("Org-ID", strconv.FormatUint(orgID, 10)).
		Do().
		JSON(&response)
	if err != nil {
		return nil, err
	}
	if !resp.IsOK() || !response.Success {
		return nil, errors.Errorf("failed to Get operations, %+v", response.Err)
	}

	// 解析响应
	var data apistructs.GetOperationResp
	if err = json.Unmarshal(response.Data, &data); err != nil {
		return nil, err
	}

	var operation openapi3.Operation
	if err = json.Unmarshal(data.Operation, &operation); err != nil {
		return nil, err
	}

	// 构造返回结构
	var result = apistructs.APIOperation{
		ID:                     id,
		AssetID:                data.AssetID,
		AssetName:              data.AssetName,
		Version:                data.Version,
		Path:                   data.Path,
		Method:                 data.Method,
		Description:            operation.Description,
		OperationID:            operation.OperationID,
		Headers:                nil,
		Parameters:             nil,
		RequestBodyDescription: "",
		RequestBodyRequired:    false,
		RequestBody:            nil,
		Responses:              nil,
	}

	// 处理 parameters
	for _, p := range operation.Parameters {
		if p == nil || p.Value == nil || p.Value.Schema == nil || p.Value.Schema.Value == nil {
			continue
		}
		v := p.Value
		schema := v.Schema.Value
		parameter := apistructs.Parameter{
			Name:            v.Name,
			Description:     v.Description,
			AllowEmptyValue: v.AllowEmptyValue,
			AllowReserved:   v.AllowReserved,
			Deprecated:      v.Deprecated,
			Required:        v.Required,
			Type:            schema.Type,
			Enum:            schema.Enum,
			Default:         schema.Default,
			Example:         schema.Example,
		}
		switch v.In {
		case "query":
			result.Parameters = append(result.Parameters, &parameter)
		case "header":
			result.Headers = append(result.Headers, &parameter)
		}
	}

	// 处理 request body
	if operation.RequestBody != nil && operation.RequestBody.Value != nil {
		v := operation.RequestBody.Value
		result.RequestBodyDescription = v.Description
		result.RequestBodyRequired = v.Required
		for mediaType, c := range v.Content {
			if c.Schema == nil || c.Schema.Value == nil {
				continue
			}
			if c.Schema.Value != nil {
				oas3.GenExampleFromExpandedSchema(httputil.ContentType(mediaType), c.Schema.Value)
			}
			result.RequestBody = append(result.RequestBody, &apistructs.RequestBody{
				MediaType: mediaType,
				Body:      c.Schema.Value,
			})
		}
	}

	// 处理 responses
	for statusCode, res := range operation.Responses {
		if res.Value == nil {
			continue
		}
		for mediaType, c := range res.Value.Content {
			if c.Schema == nil || c.Schema.Value == nil {
				continue
			}
			resp := apistructs.Response{
				StatusCode:  statusCode,
				MediaType:   mediaType,
				Description: "",
				Body:        c.Schema.Value,
			}
			if res.Value.Description != nil {
				resp.Description = *res.Value.Description
			}
		}
	}

	return &result, nil
}
