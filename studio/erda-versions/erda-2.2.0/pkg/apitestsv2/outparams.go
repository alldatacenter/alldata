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
	"fmt"
	"strings"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/encoding/jsonparse"
)

// ParseOutParams 解析 API 执行结果的出参，存储为全局变量，供后续使用
func (at *APITest) ParseOutParams(apiOutParams []apistructs.APIOutParam, apiResp *apistructs.APIResp,
	caseParams map[string]*apistructs.CaseParams) map[string]interface{} {
	defer func() {
		if r := recover(); r != nil {
			fmt.Println("recovered from ", r)
		}
	}()
	outParams := make(map[string]interface{})
	jqOrJsonPath := ""
	for _, t := range apiOutParams {
		pam := &apistructs.CaseParams{}
		switch t.Source {
		case apistructs.APIOutParamSourceStatus:
			pam.Type = apistructs.APIOutParamSourceStatus.String()
			pam.Value = apiResp.Status
		case apistructs.APIOutParamSourceBodyJsonJsonPath:
			jqOrJsonPath = "jsonpath"
			fallthrough
		case apistructs.APIOutParamSourceBodyJsonJQ:
			jqOrJsonPath = "jq"
			fallthrough
		case apistructs.APIOutParamSourceBodyJsonJacksonPath:
			jqOrJsonPath = "jackson"
			fallthrough
		case apistructs.APIOutParamSourceBodyJson:
			pam.Type = apistructs.APIOutParamSourceStatus.String()
			pam.Value = jsonparse.FilterJson(apiResp.Body, t.Expression, jqOrJsonPath)
		case apistructs.APIOutParamSourceBodyText:
			pam.Type = apistructs.APIOutParamSourceStatus.String()
			pam.Value = fmt.Sprint(apiResp.Body)
		case apistructs.APIOutParamSourceHeader:
			pam.Type = apistructs.APIOutParamSourceStatus.String()
			express := strings.TrimSpace(t.Expression)
			if express == "" {
				pam.Value = apiResp.Headers
				break
			}
			if val, ok := apiResp.Headers[express]; ok {
				for i, h := range val {
					if i == 0 {
						pam.Value = h
						continue
					}
					pam.Value = fmt.Sprint(pam.Value, ",", h)
				}
			}
		}

		if t.Key != "" {
			outParams[t.Key] = pam.Value

			// store to case params
			caseParams[t.Key] = pam
		}
	}

	return outParams
}
