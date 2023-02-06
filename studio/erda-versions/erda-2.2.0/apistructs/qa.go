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
	"strings"
	"time"
)

// test type
const (
	UT TestType = "UT"
	IT TestType = "IT"
)

type ApiTestStatus string

// Api测试对应的状态
const (
	ApiTestCreated ApiTestStatus = "Created"
	ApiTestRunning ApiTestStatus = "Running"
	ApiTestPassed  ApiTestStatus = "Passed"
	ApiTestFailed  ApiTestStatus = "Failed"
)

// test case result status
const (
	TestStatusPassed  TestStatus = "passed"
	TestStatusSkipped TestStatus = "skipped"
	TestStatusFailed  TestStatus = "failed"
	TestStatusError   TestStatus = "error"
)

type APITestEnvType string

// API 测试环境变量类型
const (
	ProjectEnv APITestEnvType = "project"
	UsecaseEnv APITestEnvType = "usecase"
)

type SonarStoreRequest struct {
	ApplicationID    int64                `json:"applicationId"`
	BuildID          int64                `json:"buildId"`
	ProjectID        int64                `json:"projectId"`
	ApplicationName  string               `json:"applicationName"`
	Branch           string               `json:"branch"`
	GitRepo          string               `json:"gitRepo"`
	CommitID         string               `json:"commitId"`
	ProjectName      string               `json:"projectName"`
	OperatorID       string               `json:"operatorId"`
	LogID            string               `json:"logId"`
	Key              string               `json:"key"`
	Bugs             []*TestIssues        `json:"bugs"`
	CodeSmells       []*TestIssues        `json:"code_smells"`
	Vulnerabilities  []*TestIssues        `json:"vulnerabilities"`
	Coverage         []*TestIssuesTree    `json:"coverage"`
	Duplications     []*TestIssuesTree    `json:"duplications"`
	IssuesStatistics TestIssuesStatistics `json:"issues_statistics"`
}

type TextRange struct {
	EndLine     int `json:"endLine"`
	EndOffset   int `json:"endOffset"`
	StartLine   int `json:"startLine"`
	StartOffset int `json:"startOffset"`
}

type TestIssues struct {
	Path      string    `json:"path"`
	TreeID    string    `json:"treeId"`
	Component string    `json:"component"`
	Message   string    `json:"message"`
	Rule      string    `json:"rule"`
	TextRange TextRange `json:"textRange"`
	Severity  string    `json:"severity"`
	Status    string    `json:"status"`
	Line      int       `json:"line"`
	Code      []string  `json:"code"`
}

type TestIssuesTree struct {
	Path     string          `json:"path"`
	TreeID   string          `json:"treeId"`
	Lines    []string        `json:"lines"`
	Name     string          `json:"name"`
	Language string          `json:"language"`
	Measures []*TestMeasures `json:"measures"`
}

type TestMeasures struct {
	Metric string `json:"metric"`
	Value  string `json:"value"`
}

type TestIssuesStatistics struct {
	Bugs            string                     `json:"bugs"`
	Coverage        string                     `json:"coverage"`
	Vulnerabilities string                     `json:"vulnerabilities"`
	CodeSmells      string                     `json:"codeSmells"`
	Duplications    string                     `json:"duplications"`
	Rating          *TestIssueStatisticsRating `json:"rating,omitempty"`
	SonarKey        string                     `json:"sonarKey"`
	Path            string                     `json:"path"`
	UT              string                     `json:"ut"`
	CommitID        string                     `json:"commitId,omitempty"`
	Branch          string                     `json:"branch,omitempty"`
	Time            time.Time                  `json:"time,omitempty"`
}

type CodeQualityRatingLevel string

var (
	CodeQualityRatingLevelA       CodeQualityRatingLevel = "A"
	CodeQualityRatingLevelB       CodeQualityRatingLevel = "B"
	CodeQualityRatingLevelC       CodeQualityRatingLevel = "C"
	CodeQualityRatingLevelD       CodeQualityRatingLevel = "D"
	CodeQualityRatingLevelE       CodeQualityRatingLevel = "E"
	CodeQualityRatingLevelUnknown CodeQualityRatingLevel = "-"
)

type TestIssueStatisticsRating struct {
	Bugs            CodeQualityRatingLevel `json:"bugs"`
	Vulnerabilities CodeQualityRatingLevel `json:"vulnerabilities"`
	CodeSmells      CodeQualityRatingLevel `json:"codeSmells"`
}

type SonarStoreResponse struct {
	Header
	Data interface{} `json:"data"`
}

type TestStatus string

// The following relation should hold true.
//   Tests == (Passed + Skipped + Failed + Error)
type TestTotals struct {
	Tests    int                `json:"tests" yaml:"tests"`
	Duration time.Duration      `json:"duration" yaml:"duration"`
	Statuses map[TestStatus]int `json:"statuses" yaml:"statuses"`
}

// Suite represents a logical grouping (suite) of tests.
type TestSuite struct {
	// Name is a descriptor given to the suite.
	Name string `json:"name" yaml:"name"`

	// Package is an additional descriptor for the hierarchy of the suite.
	Package string `json:"package" yaml:"package"`

	// Properties is a mapping of key-value pairs that were available when the
	// tests were run.
	Properties map[string]string `json:"properties,omitempty" yaml:"properties"`

	// Tests is an ordered collection of tests with associated results.
	Tests []*Test `json:"tests" yaml:"tests"`

	// SystemOut is textual test output for the suite. Usually output that is
	// written to stdout.
	SystemOut string `json:"stdout,omitempty"`

	// SystemErr is textual test error output for the suite. Usually output that is
	// written to stderr.
	SystemErr string `json:"stderr,omitempty"`

	// Totals is the aggregated results of all tests.
	Totals *TestTotals `json:"totals" yaml:"totals"`

	Extra map[string]string `json:"extra,omitempty"`

	Children []*TestSuite `json:"children,omitempty"`
}

func (t *TestSuite) Calculate() *TestTotals {
	if t.Totals == nil {
		t.Totals = &TestTotals{Statuses: make(map[TestStatus]int)}
	}

	for _, node := range t.Children {
		tmpTotal := node.Calculate()
		node.Name = strings.TrimPrefix(node.Name, t.Name+"/")
		if tmpTotal != nil {
			if tmpTotal.Statuses == nil {
				tmpTotal.Statuses = make(map[TestStatus]int)
			}
			t.Totals.Statuses[TestStatusPassed] += tmpTotal.Statuses[TestStatusPassed]
			t.Totals.Statuses[TestStatusSkipped] += tmpTotal.Statuses[TestStatusSkipped]
			t.Totals.Statuses[TestStatusFailed] += tmpTotal.Statuses[TestStatusFailed]
			t.Totals.Statuses[TestStatusError] += tmpTotal.Statuses[TestStatusError]
		}
	}
	return t.Totals
}

// Test represents the results of a single test run.
type Test struct {
	Name      string        `json:"name" yaml:"name"`
	Classname string        `json:"classname" yaml:"classname"`
	Duration  time.Duration `json:"duration" yaml:"duration"`
	Status    TestStatus    `json:"status" yaml:"status"`
	Error     interface{}   `json:"error" yaml:"error"`
	SystemOut string        `json:"stdout,omitempty"`
	SystemErr string        `json:"stderr,omitempty"`
}

type TestError struct {
	Message string `json:"message,omitempty" yaml:"message"`
	Type    string `json:"type,omitempty" yaml:"type"`
	Body    string `json:"body,omitempty" yaml:"body"`
}

type TestType string

type TestTypesResponse struct {
	Header
	Data []TestType `json:"data"`
}

type TestRecordPagingRequest struct {
	PageNo   int `schema:"pageNo"`
	PageSize int `schema:"pageSize"`

	AppID uint64 `schema:"applicationId,required"`
}
type TestRecordsResponse struct {
	Header
	Data interface{} `json:"data"`
}

type TestDetailRecordResponse struct {
	Header
	Data interface{} `json:"data"`
}

type TestCallBackResponse struct {
	Header
	Data interface{} `json:"data"`
}

type TestResults struct {
	ApplicationID   int64             `json:"applicationId"`
	BuildID         int64             `json:"buildId"`
	ProjectID       int64             `json:"projectId"`
	Type            TestType          `json:"type"`
	Name            string            `json:"name"`
	ApplicationName string            `json:"applicationName"`
	Branch          string            `json:"branch"`
	GitRepo         string            `json:"gitRepo"`
	CommitID        string            `json:"commitId"`
	OperatorName    string            `json:"operatorName"`
	OperatorID      string            `json:"operatorId"`
	Status          string            `json:"status"`
	Workspace       string            `json:"workspace"`
	ParserType      string            `json:"parserType"`
	UUID            string            `json:"uuid"`
	Extra           map[string]string `json:"extra,omitempty"`
}

type TestCallBackRequest struct {
	Results *TestResults `json:"results"`

	// Totals is the aggregated results of all tests.
	Totals *TestTotals  `json:"totals"`
	Suites []*TestSuite `json:"suites,omitempty"`
}

type SonarIssueResponse struct {
	Header
	Data interface{} `json:"data"`
}

// ApiTestsCreateRequest 创建api测试信息请求体
type ApiTestsCreateRequest struct {
	ApiTestInfo
}

// ApiTestsCreateResponse 创建api测试信息的响应
type ApiTestsCreateResponse struct {
	Header
	Data interface{} `json:"data"`
}

// ApiTestInfoResponse 获取api测试信息响应
type ApiTestsGetResponse struct {
	Header
	Data *ApiTestInfo `json:"data"`
}

// ApiTestsListResponse 获取api测试信息列表响应
type ApiTestsListResponse struct {
	Header
	Data []*ApiTestInfo `json:"data"`
}

// ApiTestsUpdateRequest 更新api测试信息请求体
type ApiTestsUpdateRequest struct {
	ApiTestInfo
	IsResult bool `json:"isResult"`
}

// ApiTestsUpdateResponse 更新api测试信息的响应
type ApiTestsUpdateResponse struct {
	Header
	Data interface{} `json:"data"`
}

// ApiTestsDeleteResponse 删除api测试信息响应
type ApiTestsDeleteResponse struct {
	Header
	Data string `json:"data"`
}

// ApiTestsActionRequest 执行api测试的请求
type ApiTestsActionRequest struct {
	Header
	ProjectID        int64    `json:"projectID"`
	ProjectTestEnvID int64    `json:"projectTestEnvID"`
	TestPlanID       int64    `json:"testPlanID"`
	UsecaseIDs       []uint64 `json:"usecaseIDs"`
}

// ApiTestsActionResponse 执行api测试的响应
type ApiTestsActionResponse struct {
	Header
	Data uint64 `json:"data"`
}

// ApiTestInfo api测试的信息
type ApiTestInfo struct {
	ApiID        int64         `json:"apiID"`
	UsecaseID    int64         `json:"usecaseID"`
	UsecaseOrder int64         `json:"usecaseOrder"`
	ProjectID    int64         `json:"projectID"`
	Status       ApiTestStatus `json:"status"`
	ApiInfo      string        `json:"apiInfo"`
	ApiRequest   string        `json:"apiRequest"`
	ApiResponse  string        `json:"apiResponse"`
	AssertResult string        `json:"assertResult"`
}

// APITestEnvCreateRequest 创建API测试环境变量信息请求体
type APITestEnvCreateRequest struct {
	APITestEnvData
}

// APITestEnvCreateResponse 创建API测试环境变量信息的响应
type APITestEnvCreateResponse struct {
	Header
	Data *APITestEnvData `json:"data"`
}

// APITestEnvUpdateRequest 更新API测试环境变量信息请求体
type APITestEnvUpdateRequest struct {
	APITestEnvData
}

// APITestEnvUpdateResponse 更新API测试环境变量信息的响应
type APITestEnvUpdateResponse struct {
	Header
	Data int64 `json:"data"`
}

// APITestEnvData API 测试环境变量数据信息
type APITestEnvData struct {
	ID      int64                          `json:"id"`
	EnvID   int64                          `json:"envID"`
	EnvType APITestEnvType                 `json:"envType"`
	Name    string                         `json:"name"`
	Domain  string                         `json:"domain"`
	Header  map[string]string              `json:"header"`
	Global  map[string]*APITestEnvVariable `json:"global"`
}

// APITestEnvVariable API 测试环境变量值信息
type APITestEnvVariable struct {
	Value string `json:"value"`
	Type  string `json:"type"`
	Desc  string `json:"desc,omitempty"`
}

// APITestEnvResponse API测试环境变量信息响应
type APITestEnvGetResponse struct {
	Header
	Data *APITestEnvData `json:"data"`
}

// APITestEnvListResponse 获取API测试环境变量信息列表响应
type APITestEnvListResponse struct {
	Header
	Data []*APITestEnvData `json:"data"`
}

// APITestEnvDeleteResponse 删除API测试环境信息响应
type APITestEnvDeleteResponse struct {
	Header
	Data *APITestEnvData `json:"data"`
}

// ApiTestCancelRequest 测试计划取消请求
type ApiTestCancelRequest struct {
	PipelineID uint64 `json:"pipelineId"`
}

// ApiTestCancelResponse 测试计划取消响应
type ApiTestCancelResponse struct {
	Header
	Data string `json:"data"`
}

// QaBuildCreateResponse
type QaBuildCreateResponse struct {
	Header
	Data QaBuildCreateResponseData `json:"data"`
}

// QaBuildCreateResponseData
type QaBuildCreateResponseData struct {
	ID   int64  `json:"id"`
	UUID string `json:"uuid"`
}

// SonarCredentialGetResponse
type SonarCredentialGetResponse struct {
	Header
	Data *SonarCredential `json:"data,omitempty"`
}

// SonarCredential sonar credential for invoking
type SonarCredential struct {
	Server string `json:"server,omitempty"`
	Token  string `json:"token,omitempty"`
}
