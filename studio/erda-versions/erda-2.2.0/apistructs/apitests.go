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
	"net/http"
	"net/url"
)

// APIInfo API测试详细信息
type APIInfo struct {
	ID        string        `json:"id"`
	Name      string        `json:"name"`
	URL       string        `json:"url"`
	Method    string        `json:"method"`
	Headers   []APIHeader   `json:"headers"`
	Params    []APIParam    `json:"params"`
	Body      APIBody       `json:"body"`
	OutParams []APIOutParam `json:"outParams"`
	Asserts   [][]APIAssert `json:"asserts"`
}

type APIInfoV2 struct {
	ID        string        `json:"id"`
	Name      string        `json:"name"`
	URL       string        `json:"url"`
	Method    string        `json:"method"`
	Headers   []APIHeader   `json:"headers"`
	Params    []APIParam    `json:"params"`
	Body      APIBody       `json:"body"`
	OutParams []APIOutParam `json:"out_params"`
	Asserts   []APIAssert   `json:"asserts"`
}

// APIHeader API测试请求头
type APIHeader struct {
	Key   string `json:"key"`
	Value string `json:"value"`
	Desc  string `json:"desc"`
}

// APIParam API测试参数
type APIParam struct {
	Key   string `json:"key"`
	Value string `json:"value"`
	Desc  string `json:"desc"`
}

type APIBodyType string

var (
	APIBodyTypeNone                          APIBodyType = "none"
	APIBodyTypeText                          APIBodyType = "" // non corresponding Content-Type
	APIBodyTypeTextPlain                     APIBodyType = "text/plain"
	APIBodyTypeApplicationJSON               APIBodyType = "application/json"
	APIBodyTypeApplicationJSON2              APIBodyType = "JSON(application/json)"
	APIBodyTypeApplicationXWWWFormUrlencoded APIBodyType = "application/x-www-form-urlencoded"
)

func (t APIBodyType) String() string {
	return string(t)
}

// APIBody API测试的Body
type APIBody struct {
	Type    APIBodyType `json:"type"`
	Content interface{} `json:"content"`
}

// APIOutParamSource 出参来源
type APIOutParamSource string

var (
	APIOutParamSourceStatus              APIOutParamSource = "status"
	APIOutParamSourceBodyJson            APIOutParamSource = "body:json"
	APIOutParamSourceBodyJsonJQ          APIOutParamSource = "body:json:jq"
	APIOutParamSourceBodyJsonJsonPath    APIOutParamSource = "body:json:jsonpath"
	APIOutParamSourceBodyJsonJacksonPath APIOutParamSource = "body:json:jackson"
	APIOutParamSourceBodyText            APIOutParamSource = "body:text"
	APIOutParamSourceHeader              APIOutParamSource = "header"
)

func (source APIOutParamSource) String() string {
	return string(source)
}

// APIOutParam API 测试的出参信息
type APIOutParam struct {
	Key        string            `json:"key"`
	Source     APIOutParamSource `json:"source"`
	Expression string            `json:"expression,omitempty"`
	MatchIndex string            `json:"matchIndex,omitempty"`
}

// APIAssert API测试的断言信息
type APIAssert struct {
	Arg      string `json:"arg"`
	Operator string `json:"operator"`
	Value    string `json:"value"`
}

// APIResp API测试的返回结果
type APIResp struct {
	Status  int                 `json:"status"`
	Headers map[string][]string `json:"headers"`
	Body    []byte              `json:"-"`
	BodyStr string              `json:"body"`
}

// CaseParams 传递case内出入参的全局变量
type CaseParams struct {
	Key   string      `json:"key"`
	Type  string      `json:"type"`
	Value interface{} `json:"value"`
}

// APITestsAttemptRequest 尝试执行API测试的请求
type APITestsAttemptRequest struct {
	ProjectTestEnvID int64      `json:"projectTestEnvID"`
	UsecaseTestEnvID int64      `json:"usecaseTestEnvID"`
	APIs             []*APIInfo `json:"apis"`
}

// APITestsAttemptResponse 尝试执行api测试的响应
type APITestsAttemptResponse struct {
	Header
	Data []*APITestsAttemptResponseData `json:"data"`
}

// APITestsAttemptResponseData 尝试执行api测试的响应结果数据
type APITestsAttemptResponseData struct {
	Request  *APIRequestInfo       `json:"request"`
	Response *APIResp              `json:"response"`
	Asserts  *APITestsAssertResult `json:"asserts"`
}

// APIRequestInfo API 实际请求信息
type APIRequestInfo struct {
	URL     string      `json:"url"`
	Method  string      `json:"method"`
	Headers http.Header `json:"headers"`
	Params  url.Values  `json:"params"`
	Body    APIBody     `json:"body"`
}

// APITestsAssertResult 断言结果详情
type APITestsAssertResult struct {
	Success bool                  `json:"success"`
	Result  []*APITestsAssertData `json:"result"`
}

// APITestsAssertResult 断言结果详情数据
type APITestsAssertData struct {
	Arg         string      `json:"arg"`
	Operator    string      `json:"operator"`
	Value       string      `json:"value"`
	Success     bool        `json:"success"`
	ActualValue interface{} `json:"actualValue"`
	ErrorInfo   string      `json:"errorInfo"`
}

// APITestsStatisticRequest API 测试结果统计请求
type APITestsStatisticRequest struct {
	UsecaseIDs []uint64 `json:"usecaseIDs"`
}

// APITestsStatisticResponse API 测试结果统计响应
type APITestsStatisticResponse struct {
	Header
	Data *APITestsStatisticResponseData `json:"data"`
}

// APITestsStatisticResponseData API 测试结果统计响应数据
type APITestsStatisticResponseData struct {
	Total       uint64 `json:"total"`
	Passed      uint64 `json:"passed"`
	PassPercent string `json:"passPercent"`
}

// APITestFront 组件化前端认的api test结构体
type APITestFront struct {
	APIInfo
	// AttemptTest APITestsAttemptResponseData `json:"attemptTest"`
}
