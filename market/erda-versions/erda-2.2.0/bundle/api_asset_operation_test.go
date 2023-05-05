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

package bundle_test

const requestSchema = `{
                "required": [
                  "approval",
                  "default",
                  "desc",
                  "limits",
                  "name"
                ],
                "type": "object",
                "properties": {
                  "name": {
                    "maxLength": 36,
                    "minLength": 2,
                    "type": "string",
                    "example": "Example"
                  },
                  "desc": {
                    "maxLength": 191,
                    "type": "string",
                    "example": "Example"
                  },
                  "approval": {
                    "type": "string",
                    "example": "auto",
                    "enum": [
                      "auto"
                    ]
                  },
                  "bindDomains": {
                    "type": "array",
                    "items": {
                      "type": "string",
                      "example": "Example"
                    }
                  }
                }
              }`

const responseSchema = `{
        "required": [
          "success"
        ],
        "type": "object",
        "properties": {
          "success": {
            "type": "boolean",
            "example": true
          },
          "err": {
            "required": [
              "code",
              "msg"
            ],
            "type": "object",
            "properties": {
              "code": {
                "type": "string",
                "example": "Example"
              },
              "msg": {
                "type": "string",
                "example": "Example"
              }
            }
          }
        }
      }`

// go test -v -run TestOpenapiSummary
//func TestOpenapiSummary(t *testing.T) {
//	var summaries = []*bundle.APIOperationSummary{{
//		ID:          0,
//		AssetID:     "asset_id",
//		AssetName:   "asset_name",
//		Version:     "jave-demo",
//		Path:        "/some/uniform/resource/identifier",
//		Method:      "GET",
//		OperationID: "retrieve_some_resource",
//	}}
//	data, _ := json.MarshalIndent(summaries, "", "  ")
//	t.Log(string(data))
//}

//func TestAPIOperation(t *testing.T) {
//	requestBody := openapi3.NewObjectSchema()
//	if err := requestBody.UnmarshalJSON([]byte(requestSchema)); err != nil {
//		t.Errorf("failed to UnmarshalJSON requestSchema")
//	}
//	responseBody := openapi3.NewObjectSchema()
//	if err := responseBody.UnmarshalJSON([]byte(responseSchema)); err != nil {
//		t.Errorf("failed to UnmarshalJSON responseBody")
//	}
//
//	oas3.GenExample(requestBody)
//	oas3.GenExample(responseBody)
//
//	var o = bundle.APIOperation{
//		AssetID:     "some_asset",
//		Version:     "java-demo",
//		Path:        "/api/uniform/resource/identifier/{id}",
//		Method:      "GET",
//		Summary:     "查询某某资源的接口",
//		Description: "查询某某资源的接口",
//		OperationID: "查询xx资源",
//		Headers: []*bundle.Parameter{{
//			Name:            "sessionid",
//			Description:     "session id",
//			AllowEmptyValue: false,
//			AllowReserved:   false,
//			Deprecated:      false,
//			Required:        true,
//			Type:            "string",
//			Enum:            nil,
//			Default:         "uuid",
//			Example:         "xxx-xxx-xxx",
//		}},
//		Parameters: []*bundle.Parameter{{
//			Name:            "createAt",
//			Description:     "创建时间",
//			AllowEmptyValue: false,
//			AllowReserved:   false,
//			Deprecated:      false,
//			Required:        false,
//			Type:            "string",
//			Enum:            nil,
//			Default:         time.Now(),
//			Example:         time.Now(),
//		}},
//		RequestBodyDescription: "描述请求体",
//		RequestBodyRequired:    true,
//		RequestBody: []*bundle.RequestBody{{
//			MediaType: "application/json",
//			Body:      requestBody,
//		}},
//		Responses: []*bundle.Response{{
//			StatusCode:  "200",
//			MediaType:   "application/json",
//			Description: "请求成功的响应体",
//			Headers: []*bundle.Parameter{{
//				Name:            "expiredAt",
//				Description:     "过期时间",
//				AllowEmptyValue: false,
//				AllowReserved:   false,
//				Deprecated:      false,
//				Required:        false,
//				Type:            "string",
//				Enum:            nil,
//				Default:         time.Now(),
//				Example:         time.Now(),
//			}},
//			Body: responseBody,
//		}},
//	}
//
//	data, _ := json.MarshalIndent(o, "", "  ")
//	t.Log(string(data), "\n")
//}
