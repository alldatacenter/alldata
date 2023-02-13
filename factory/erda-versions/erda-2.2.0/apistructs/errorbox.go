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

package apistructs

import (
	"net/url"
	"strconv"
	"time"

	"github.com/pkg/errors"
)

type ErrorResourceType string

const (
	PipelineError ErrorResourceType = "pipeline"
	RuntimeError  ErrorResourceType = "runtime"
	AddonError    ErrorResourceType = "addon"
)

// ErrorLogListRequest 错误日志查询请求
type ErrorLogListRequest struct {
	// +required 鉴权需要
	ScopeType ScopeType `schema:"scopeType"`
	// +required 鉴权需要
	ScopeID uint64 `schema:"scopeId"`
	// +required 资源类型
	ResourceType ErrorResourceType `schema:"resourceType"`
	// +required 资源id
	ResourceID string `schema:"resourceId"`
	// +option 根据时间过滤错误日志
	StartTime string `schema:"startTime"`
}

// Check 检查错误日志创建请求是否合法
func (el *ErrorLogListRequest) Check() error {
	if el.ScopeType == "" {
		return errors.Errorf("invalid request, scopeType couldn't be empty")
	}
	if el.ScopeID == 0 {
		return errors.Errorf("invalid request, scopeId couldn't be empty")
	}
	if el.ResourceType == "" {
		return errors.Errorf("invalid request, resourceType couldn't be empty")
	}

	if el.ResourceID == "" {
		return errors.Errorf("invalid request, ResourceID couldn't be empty")
	}

	return nil
}

type TaskErrorListRequest struct {
	ResourceTypes []ErrorResourceType `schema:"resourceTypes"`
	StartTime     string              `schema:"startTime"`
	ResourceIDS   []string            `schema:"resourceIds"`
}

// GetFormartStartTime format start time
func (te *TaskErrorListRequest) GetFormartStartTime() (*time.Time, error) {
	if te.StartTime == "" {
		return nil, errors.Errorf("OccurrenceTime is empty")
	}
	result, err := time.ParseInLocation("2006-01-02 15:04:05", te.StartTime, time.Local)
	if err != nil {
		return nil, err
	}

	return &result, nil
}

func (te *TaskErrorListRequest) ConvertToQueryParams() url.Values {
	values := make(url.Values)
	if te.StartTime != "" {
		values.Add("startTime", te.StartTime)
	}
	for _, resourceID := range te.ResourceIDS {
		values.Add("resourceIds", resourceID)
	}
	for _, resourceType := range te.ResourceTypes {
		values.Add("resourceTypes", string(resourceType))
	}
	return values
}

// ErrorLogListResponse 错误日志查询具体响应
type ErrorLogListResponse struct {
	Header
	UserInfoHeader
	Data *ErrorLogListResponseData `json:"data"`
}

// ErrorLogListResponseData 错误日志查询具体响应
type ErrorLogListResponseData struct {
	List []ErrorLog `json:"list"`
}

type ErrorLogLevel string

const (
	SuccessLevel ErrorLogLevel = "success"
	InfoLevel    ErrorLogLevel = "info"
	ErrorLevel   ErrorLogLevel = "error"
)

// ErrorLog 错误日志具体信息
type ErrorLog struct {
	ID             int64             `json:"id"`
	Level          ErrorLogLevel     `json:"level"`
	ResourceType   ErrorResourceType `json:"resourceType"`
	ResourceID     string            `json:"resourceId"`
	OccurrenceTime string            `json:"occurrenceTime"`
	HumanLog       string            `json:"humanLog"`
	PrimevalLog    string            `json:"primevalLog"`
	DedupID        string            `json:"deDupId,omitempty"`
}

// ErrorLogCreateRequest 错误日志创建接口
type ErrorLogCreateRequest struct {
	ErrorLog `json:"errorLog"`
}

// Check 检查错误日志创建请求是否合法
func (el *ErrorLogCreateRequest) Check() error {
	if el.ErrorLog.ResourceType == "" {
		return errors.Errorf("invalid request, ResourceType couldn't be empty")
	}
	if el.ErrorLog.ResourceID == "" {
		return errors.Errorf("invalid request, ResourceID couldn't be empty")
	}
	if el.ErrorLog.OccurrenceTime == "" {
		return errors.Errorf("invalid request, OccurrenceTime couldn't be empty")
	}
	if el.ErrorLog.PrimevalLog == "" {
		return errors.Errorf("invalid request, PrimevalLog couldn't be empty")
	}
	if el.ErrorLog.Level == "" {
		el.ErrorLog.Level = ErrorLevel
	}

	return nil
}

// FormartTime 返回time.Time
func (el *ErrorLog) FormartTime() (*time.Time, error) {
	if el.OccurrenceTime == "" {
		return nil, errors.Errorf("OccurrenceTime is empty")
	}
	timeInt, err := strconv.ParseInt(el.OccurrenceTime, 10, 64)
	if err != nil {
		return nil, err
	}
	timeStr := time.Unix(timeInt, 0).Format("2006-01-02 15:04:05")
	result, err := time.ParseInLocation("2006-01-02 15:04:05", timeStr, time.Local)
	if err != nil {
		return nil, err
	}

	return &result, nil
}

// ErrorLogCreateResponse 错误日志创建响应
type ErrorLogCreateResponse struct {
	Header
	Data string `json:"data"`
}

// ErrorLogBatchCreateRequest 错误批量创建请求
type ErrorLogBatchCreateRequest struct {
	Audits []Audit `json:"audits"`
}

// ErrorLogBatchCreateResponse 错误批量创建响应
type ErrorLogBatchCreateResponse struct {
	Header
	Data string `json:"data"`
}
