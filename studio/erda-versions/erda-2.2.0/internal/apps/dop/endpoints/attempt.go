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
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"net/http/cookiejar"
	"net/url"
	"strconv"
	"strings"

	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"golang.org/x/net/publicsuffix"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dbclient"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/apps/dop/services/signauth"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/apitestsv2"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

// API 返回对应的错误类型
const (
	ApiTest            = "API_TEST"
	PipelineYmlVersion = "1.1"
	ApiTestType        = "api-test"
	ApiTestIDs         = "api_ids"
	UsecaseID          = "usecase_id"
	PipelineStageLen   = 10
	Project            = "project"
	Usecase            = "case"
)

func (e *Endpoints) ExecuteAttemptTest(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.UpdateClient.NotLogin().ToResp(), nil
	}

	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.UpdateClient.MissingParameter(apierrors.MissingOrgID).ToResp(), nil
	}

	var uriParams = apistructs.AttempTestURIParams{
		AssetID:        vars[urlPathAssetID],
		SwaggerVersion: vars[urlPathSwaggerVersion],
	}

	var body apistructs.APITestReq
	bodyData, err := ioutil.ReadAll(r.Body)
	if err != nil {
		return apierrors.AttemptExecuteAPITest.InvalidParameter("invalid body").ToResp(), nil
	}
	if err := json.Unmarshal(bodyData, &body); err != nil {
		return apierrors.AttemptExecuteAPITest.InvalidParameter(err).ToResp(), nil
	}
	if len(body.APIs) == 0 {
		return apierrors.AttemptExecuteAPITest.InvalidParameter(fmt.Errorf("API 个数为 0")).ToResp(), nil
	}
	apiInfo := body.APIs[0]

	// 利用 clientID 查找到 相应的 contract
	var (
		clientModel apistructs.ClientModel
		contract    apistructs.ContractModel
	)
	if err := e.assetSvc.FirstRecord(&clientModel, map[string]interface{}{
		"org_id":    orgID,
		"client_id": body.ClientID,
	}); err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return apierrors.AttemptExecuteAPITest.InvalidParameter("无效的 clientID").ToResp(), nil
		}
		return apierrors.AttemptExecuteAPITest.InternalError(err).ToResp(), nil
	}
	if err := e.assetSvc.FirstRecord(&contract, map[string]interface{}{
		"org_id":          orgID,
		"asset_id":        uriParams.AssetID,
		"client_id":       clientModel.ID,
		"swagger_version": uriParams.SwaggerVersion,
		"status":          "proved",
	}); err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return apierrors.AttemptExecuteAPITest.InternalError(errors.Wrap(err, "客户端未授权")).ToResp(), nil
		}
		return apierrors.AttemptExecuteAPITest.InternalError(err).ToResp(), nil
	}

	// 查找访问管理
	var access apistructs.APIAccessesModel
	if err = e.assetSvc.FirstRecord(&access, map[string]interface{}{
		"org_id":          orgID,
		"asset_id":        uriParams.AssetID,
		"swagger_version": uriParams.SwaggerVersion,
	}); err != nil {
		return apierrors.AttemptExecuteAPITest.InternalError(errors.Wrap(err, "no access")).ToResp(), nil
	}

	// 查找域名
	domains := e.assetSvc.GetEndpointDomains(strconv.FormatUint(orgID, 10), identity.UserID, access.EndpointID)
	if len(domains) == 0 {
		return apierrors.AttemptExecuteAPITest.InternalError(errors.New("no domain")).ToResp(), nil
	}
	domain := domains[0]

	var request *http.Request
	switch method := strings.ToUpper(apiInfo.Method); method {
	case http.MethodGet, http.MethodHead:
		if request, err = http.NewRequest(method, domain, nil); err != nil {
			return apierrors.AttemptExecuteAPITest.InternalError(errors.New("转发测试请求失败")).ToResp(), nil
		}
	default:
		var (
			content     string
			contentType string
		)
		switch contentType = apiInfo.Body.Type; contentType {
		case "application/json":
			content = string(apiInfo.Body.Content)
		case "application/x-www-form-urlencoded":
			var (
				contentValues = make(url.Values)
				contents      []apistructs.ProxyContent
			)
			if err := json.Unmarshal(apiInfo.Body.Content, &contents); err != nil {
				logrus.Errorf("failed to Unmarshal contents, err: %v", err)
				return apierrors.AttemptExecuteAPITest.InvalidParameter("invalid contents").ToResp(), nil
			}
			for _, v := range contents {
				contentValues.Add(v.Key, v.Value)
			}
			content = contentValues.Encode()
			logrus.Infof("urlEncodedBody: %+v, contents: %v, content: %v", string(apiInfo.Body.Content), contents, content)
		}
		requestBody := bytes.NewBufferString(content)
		if request, err = http.NewRequest(method, domain, requestBody); err != nil {
			return apierrors.AttemptExecuteAPITest.InternalError(errors.New("转发测试请求失败")).ToResp(), nil
		}
		request.Header.Set("content-type", contentType)
	}

	request.Host = domain
	request.URL.Scheme = apiInfo.Schema
	if request.URL.Scheme == "" {
		request.URL.Scheme = "https"
	}
	request.URL.Host = domain
	request.URL.Path = apiInfo.URL
	request.Proto = "HTTP/1.1"
	request.ProtoMajor = 1
	request.ProtoMinor = 1

	// 传入 query parameters
	var params = request.URL.Query()
	for _, v := range apiInfo.Params {
		params.Add(v.Key, v.Value)
	}
	if access.Authentication == apistructs.AuthenticationSignAuth {
		if body.ClientSecret == "" {
			return apierrors.AttemptExecuteAPITest.InvalidParameter("无效的 secret ").ToResp(), nil
		}
		params.Set("appKey", body.ClientID)
	}
	request.URL.RawQuery = params.Encode()

	// 传入 headers
	for _, v := range apiInfo.Header {
		request.Header.Add(v.Key, v.Value)
	}
	if access.Authentication == apistructs.AuthenticationKeyAuth {
		request.Header.Add("X-App-Key", body.ClientID)
	}

	if access.Authentication == apistructs.AuthenticationSignAuth {
		signer, err := signauth.NewSigner(request, body.ClientID, body.ClientSecret)
		if err != nil {
			logrus.Errorf("failed to NewSigner, err: %v", err)
			return apierrors.AttemptExecuteAPITest.InternalError(errors.Errorf("计算 sign-auth 失败: %v", err)).ToResp(), nil
		}
		sign, err := signer.Sign()
		if err != nil {
			logrus.Errorf("failed to Sign, err: %v", err)
			return apierrors.AttemptExecuteAPITest.InternalError(errors.Errorf("计算 sign-auth 失败: %v", err)).ToResp(), nil
		}
		logrus.Debugf("sign-auth: %s", sign)
	}

	if request.Body != nil {
		if data, err := ioutil.ReadAll(request.Body); err == nil {
			logrus.Infof("requesting body: %s", string(data))
			request.Body = ioutil.NopCloser(bytes.NewBuffer(data))
		}
	}
	logrus.Infof("requesting: %+v", *request)

	client := *http.DefaultClient
	client.CheckRedirect = func(req *http.Request, via []*http.Request) error {
		return http.ErrUseLastResponse
	}
	response, err := client.Do(request)
	if err != nil {
		logrus.Errorf("failed Do request, request: %+v, err: %v", *request, err)
		return apierrors.AttemptExecuteAPITest.InternalError(errors.Wrap(err, "请求失败")).ToResp(), nil
	}
	content, err := ioutil.ReadAll(response.Body)
	if err != nil {
		return apierrors.AttemptExecuteAPITest.InternalError(errors.Wrap(err, "读取响应失败")).ToResp(), nil
	}
	defer response.Body.Close()

	apiResp := apistructs.APIResp{
		Status:  response.StatusCode,
		Headers: response.Header,
		Body:    content,
		BodyStr: string(content),
	}
	apiReq := apistructs.ProxyAPIRequestInfo{
		Host:    request.URL.Host,
		URL:     request.URL.Path,
		Method:  apiInfo.Method,
		Headers: request.Header,
		Params:  params,
		Body:    apiInfo.Body,
	}

	return httpserver.OkResp(map[string]interface{}{"request": apiReq, "response": apiResp})
}

// ExecuteManualTestAPI 用户尝试执行单个或者多个API测试
func (e *Endpoints) ExecuteManualTestAPI(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	envData := &apistructs.APITestEnvData{
		Header: make(map[string]string),
		Global: make(map[string]*apistructs.APITestEnvVariable),
	}

	var req apistructs.APITestsAttemptRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrAttemptExecuteAPITest.InvalidParameter(err).ToResp(), nil
	}

	if len(req.APIs) == 0 {
		return apierrors.ErrAttemptExecuteAPITest.InvalidParameter(fmt.Errorf("API 个数为 0")).ToResp(), nil
	}

	// 获取测试环境变量
	if req.ProjectTestEnvID != 0 {
		envDB, err := dbclient.GetTestEnv(req.ProjectTestEnvID)
		if err != nil || envDB == nil {
			// 忽略错误
			logrus.Warningf("failed to get project test env info, projectID:%d", req.ProjectTestEnvID)
		}

		envData, err = convert2TestEnvResp(envDB)
		if err != nil || envData == nil {
			// 忽略错误
			logrus.Warningf("failed to convert project test env info, env:%+v", envDB)
		}
	}

	if req.UsecaseTestEnvID != 0 {
		envList, err := dbclient.GetTestEnvListByEnvID(req.UsecaseTestEnvID, Usecase)
		if err != nil || envList == nil {
			// 忽略错误
			logrus.Warningf("failed to get usecase test env info, usecaseID:%d", req.UsecaseTestEnvID)
		}

		var envDB dbclient.APITestEnv
		if len(envList) > 0 {
			envDB = envList[0]
			usecaseEnvData, err := convert2TestEnvResp(&envDB)
			if err != nil || usecaseEnvData == nil {
				// 忽略错误
				logrus.Warningf("failed to convert project test env info, env:%+v", envDB)
			}

			if usecaseEnvData != nil {
				// render usecase env data
				if usecaseEnvData.Domain != "" {
					envData.Domain = usecaseEnvData.Domain
				}

				for k, v := range usecaseEnvData.Global {
					envData.Global[k] = v
				}

				for k, v := range usecaseEnvData.Header {
					envData.Header[k] = v
				}
			}
		}
	}

	caseParams := make(map[string]*apistructs.CaseParams)
	// render project env global params, least low priority
	if envData != nil && envData.Global != nil {
		for k, v := range envData.Global {
			caseParams[k] = &apistructs.CaseParams{
				Type:  v.Type,
				Value: v.Value,
			}
		}
	}

	// add cookie jar
	cookieJar, err := cookiejar.New(&cookiejar.Options{PublicSuffixList: publicsuffix.List})
	if err != nil {
		logrus.Warningf("failed to new cookie jar")
	}

	httpClient := &http.Client{}

	if cookieJar != nil {
		httpClient.Jar = cookieJar
	}

	respDataList := make([]*apistructs.APITestsAttemptResponseData, 0, len(req.APIs))
	for _, apiInfo := range req.APIs {
		respData := &apistructs.APITestsAttemptResponseData{}
		apiTest := apitestsv2.New(apiInfo, apitestsv2.WithTryV1RenderJsonBodyFirst())
		apiReq, apiResp, err := apiTest.Invoke(httpClient, envData, caseParams)
		if err != nil {
			// 单个 API 执行失败，不返回失败，继续执行下一个
			logrus.Warningf("invoke api error, apiInfo:%+v, (%+v)", apiTest.API, err)
			respData.Response = &apistructs.APIResp{
				BodyStr: err.Error(),
			}
			respData.Request = apiReq
			respDataList = append(respDataList, respData)
			continue
		}
		respData.Response = apiResp
		respData.Request = apiReq

		outParams := apiTest.ParseOutParams(apiTest.API.OutParams, apiResp, caseParams)

		if len(apiTest.API.Asserts) > 0 {
			asserts := apiTest.API.Asserts[0]
			succ, assertResult := apiTest.JudgeAsserts(outParams, asserts)
			logrus.Infof("judge assert result: %v", succ)

			respData.Asserts = &apistructs.APITestsAssertResult{
				Success: succ,
				Result:  assertResult,
			}
		}

		respDataList = append(respDataList, respData)
	}

	return httpserver.OkResp(respDataList)
}
