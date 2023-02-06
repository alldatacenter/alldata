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

package apitestsv2

import (
	"bytes"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"reflect"
	"regexp"
	"strings"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/mock"
	"github.com/erda-project/erda/pkg/strutil"
)

// APITest API 测试的结构信息
type APITest struct {
	// dice api test 声明，包括入参、出参、断言等
	API *apistructs.APIInfo

	// 运行时结果，包括 response、断言结果、接口测试结果等
	APIResult *apistructs.ApiTestInfo

	opt option
}

type APIParam struct {
	Key   string      `json:"key"`
	Value interface{} `json:"value"`
	Desc  string      `json:"desc"`
}

func (p APIParam) convert() apistructs.APIParam {
	return apistructs.APIParam{
		Key:   p.Key,
		Value: strutil.String(p.Value),
		Desc:  p.Desc,
	}
}

func New(api *apistructs.APIInfo, opOptions ...OpOption) *APITest {
	var at APITest
	at.API = api

	for _, opt := range opOptions {
		opt(&at.opt)
	}

	return &at
}

var (
	regexForRender = regexp.MustCompile("{{([^{}]+)}}")

	regexSubmatchReplaceGenerateFun = func(caseParams map[string]*apistructs.CaseParams) func(sub []string) string {
		return func(sub []string) string {
			origin := sub[0]
			inner := sub[1]

			// mock
			if strings.HasPrefix(inner, "@") {
				mockType := strings.TrimPrefix(inner, "@")
				mockValue := mock.MockValue(mockType)
				if mockValue != nil {
					return fmt.Sprint(mockValue)
				}
			}

			// context ref
			if v, ok := caseParams[inner]; ok {
				return fmt.Sprint(v.Value)
			}

			// not found, return origin
			return origin
		}
	}

	renderFunc = func(strWaitRender string, caseParams map[string]*apistructs.CaseParams) string {
		// Use url.PathUnescape other than url.QueryUnescape,
		// avoid to parse time.Time like "2020-12-04T00:00:00+08:00" to "2020-12-04T00:00:00 08:00".
		// PathUnescape is identical to QueryUnescape except that it does not unescape '+' to ' ' (space).
		escapedStr, err := url.PathUnescape(strWaitRender)
		// 若 unescape 成功，则使用转义后的值；否则使用原值
		if err == nil {
			strWaitRender = escapedStr
		}
		return strutil.ReplaceAllStringSubmatchFunc(regexForRender, strWaitRender, regexSubmatchReplaceGenerateFun(caseParams))

	}
)

// Invoke 执行 API 测试
func (at *APITest) Invoke(httpClient *http.Client, testEnv *apistructs.APITestEnvData, caseParams map[string]*apistructs.CaseParams) (
	*apistructs.APIRequestInfo, *apistructs.APIResp, error) {

	// render at once
	if err := at.renderAtOnce(at.API, caseParams); err != nil {
		return nil, nil, err
	}

	// generate api request for invoking
	var apiReq apistructs.APIRequestInfo

	// url
	var domain string
	if testEnv != nil {
		domain = testEnv.Domain
	}
	polishedURL, err := polishURL(at.API.URL, domain)
	if err != nil {
		return nil, nil, err
	}
	apiReq.URL = polishedURL

	// method
	apiReq.Method = at.API.Method

	// params: queryParams and form values
	params := make(url.Values)
	for _, p := range at.API.Params {
		// add to params
		if p.Key != "" {
			params.Add(p.Key, p.Value)
		}
	}
	apiReq.Params = params

	// request body
	var reqBody string
	if at.API.Body.Content != nil && fmt.Sprint(at.API.Body.Content) != "" {

		switch at.API.Body.Type {
		case apistructs.APIBodyTypeNone:
			reqBody = ""

		case apistructs.APIBodyTypeApplicationXWWWFormUrlencoded:
			values := url.Values{}
			for _, param := range at.API.Body.Content.([]apistructs.APIParam) {
				values.Add(param.Key, param.Value)
			}
			reqBody = values.Encode()

		case apistructs.APIBodyTypeApplicationJSON:
			var reqBodyStr string
			switch at.API.Body.Content.(type) {
			case string:
				reqBodyStr = at.API.Body.Content.(string)
			case []byte:
				reqBodyStr = string(at.API.Body.Content.([]byte))
			default:
				return nil, nil, fmt.Errorf("invalid request body content while body type is application/json, type: %s", reflect.TypeOf(at.API.Body.Content).Kind())
			}
			// check if json is invalid
			var o interface{}
			if err := json.Unmarshal([]byte(reqBodyStr), &o); err != nil {
				// 提前赋值，apiReq 不返回 nil，用于错误时的详情展示
				apiReq.Body.Type = at.API.Body.Type
				apiReq.Body.Content = reqBodyStr
				return &apiReq, nil, fmt.Errorf("failed to json unmarshal request body, value: %s, err: %v", reqBodyStr, err)
			}
			reqBody = reqBodyStr

		default:
			reqBody = fmt.Sprint(at.API.Body.Content)
		}
	}
	// headers
	if apiReq.Headers == nil {
		apiReq.Headers = make(http.Header)
	}

	apiReq.Body.Type = at.API.Body.Type
	if apiReq.Body.Type != "" && apiReq.Body.Type != apistructs.APIBodyTypeNone {
		apiReq.Headers.Set("Content-Type", apiReq.Body.Type.String())
	}
	apiReq.Body.Content = reqBody

	if testEnv != nil && testEnv.Header != nil {
		for k, v := range testEnv.Header {
			apiReq.Headers.Set(strings.TrimSpace(k), strings.TrimSpace(v))
		}
	}
	for _, h := range at.API.Headers {
		apiReq.Headers.Set(h.Key, h.Value)
	}

	// use netportal
	customReq, err := handleCustomNetportalRequest(&apiReq, at.opt.netportalOption)
	if err != nil {
		return nil, nil, err
	}

	// polish headers for compression
	apiReq.Headers = polishHeadersForCompression(apiReq.Headers)

	var buffer bytes.Buffer
	req := httpclient.New(httpclient.WithCompleteRedirect()).
		Method(apiReq.Method, customReq.URL.Scheme+"://"+customReq.URL.Host, httpclient.NoRetry).
		Path(customReq.URL.Path).
		Headers(apiReq.Headers)
	httpResp, err := req.Params(apiReq.Params).
		RawBody(bytes.NewBufferString(apiReq.Body.Content.(string))).
		Do().Body(&buffer)

	// resp
	apiResp := apistructs.APIResp{
		Body:    buffer.Bytes(),
		BodyStr: buffer.String(),
	}
	if httpResp != nil {
		apiResp.Status = httpResp.StatusCode()
		apiResp.Headers = httpResp.Headers()
	}

	return &apiReq, &apiResp, err
}

func (at *APITest) renderAtOnce(apiReq *apistructs.APIInfo, caseParams map[string]*apistructs.CaseParams) error {
	if apiReq == nil {
		return nil
	}
	// url
	apiReq.URL = renderFunc(strings.TrimSpace(apiReq.URL), caseParams)
	// method
	apiReq.Method = renderFunc(strings.TrimSpace(apiReq.Method), caseParams)
	// params
	for i := range apiReq.Params {
		param := apiReq.Params[i]
		apiReq.Params[i].Key = renderFunc(strings.TrimSpace(param.Key), caseParams)
		apiReq.Params[i].Value = renderFunc(strings.TrimSpace(param.Value), caseParams)
	}
	// request body
	if apiReq.Body.Content != nil {
		switch apiReq.Body.Type {
		case apistructs.APIBodyTypeNone:
			// do nothing
		case apistructs.APIBodyTypeText, apistructs.APIBodyTypeTextPlain:
			// check type
			if err := checkBodyType(apiReq.Body, reflect.String); err != nil {
				return err
			}
			// render
			apiReq.Body.Content = renderFunc(apiReq.Body.Content.(string), caseParams)
		case apistructs.APIBodyTypeApplicationJSON, apistructs.APIBodyTypeApplicationJSON2:
			apiReq.Body.Type = apistructs.APIBodyTypeApplicationJSON
			// check type
			kind := reflect.TypeOf(apiReq.Body.Content).Kind()
			var bodyStr string
			switch kind {
			case reflect.String:
				bodyStr = apiReq.Body.Content.(string)
			default:
				// 若不是 string 类型，则 json 序列化处理
				b, err := json.Marshal(apiReq.Body.Content)
				if err != nil {
					return fmt.Errorf("failed to json marshal request body, type: %s, err: %v", kind.String(), err)
				}
				bodyStr = string(b)
			}
			// don't check json before render, maybe quote problems
			// render
			if at.opt.tryV1RenderJsonBodyFirst {
				bodyStr = tryV1RenderRequestBodyStr(bodyStr, caseParams)
			}
			apiReq.Body.Content = renderFunc(bodyStr, caseParams)
		case apistructs.APIBodyTypeApplicationXWWWFormUrlencoded:
			// check type: []APIParam
			// after check convert to []apistructs.APIParam
			b, err := json.Marshal(apiReq.Body.Content)
			if err != nil {
				return err
			}
			var content []APIParam
			if err := json.Unmarshal(b, &content); err != nil {
				return err
			}
			var renderedContent []apistructs.APIParam
			for i := range content {
				param := content[i].convert()
				param.Key = renderFunc(strings.TrimSpace(param.Key), caseParams)
				param.Value = renderFunc(strings.TrimSpace(param.Value), caseParams)
				param.Desc = renderFunc(strings.TrimSpace(param.Desc), caseParams)
				renderedContent = append(renderedContent, param)
			}
			apiReq.Body.Content = renderedContent
		}
	}
	// headers
	for i := range apiReq.Headers {
		header := apiReq.Headers[i]
		apiReq.Headers[i].Key = renderFunc(strings.TrimSpace(header.Key), caseParams)
		apiReq.Headers[i].Value = renderFunc(strings.TrimSpace(header.Value), caseParams)
	}
	// out params
	for i := range apiReq.OutParams {
		out := apiReq.OutParams[i]
		apiReq.OutParams[i].Key = renderFunc(strings.TrimSpace(out.Key), caseParams)
		apiReq.OutParams[i].Expression = renderFunc(strings.TrimSpace(out.Expression), caseParams)
		apiReq.OutParams[i].Source = apistructs.APIOutParamSource(renderFunc(strings.TrimSpace(out.Source.String()), caseParams))
	}
	// asserts
	for i := range apiReq.Asserts {
		for j := range apiReq.Asserts[i] {
			assert := apiReq.Asserts[i][j]
			apiReq.Asserts[i][j].Arg = renderFunc(strings.TrimSpace(assert.Arg), caseParams)
			apiReq.Asserts[i][j].Operator = renderFunc(strings.TrimSpace(assert.Operator), caseParams)
			apiReq.Asserts[i][j].Value = renderFunc(strings.TrimSpace(assert.Value), caseParams)
		}
	}

	return nil
}
