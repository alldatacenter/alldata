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

package oas3_test

import (
	"encoding/json"
	"testing"

	"github.com/getkin/kin-openapi/openapi3"

	oas32 "github.com/erda-project/erda/pkg/swagger/oas3"
)

const text = `{
    "openapi": "3.0.1",
    "info": {
        "title": "New API",
        "version": "1.0"
    },
    "paths": {
        "/api/api-assets": {
            "post": {
                "operationId": "创建 API 资源",
                "requestBody": {
                    "content": {
                        "application/json": {
                            "schema": {
                                "$ref": "#/components/schemas/CreateAPIAssetReqBody",
                                "properties": {
									"tag": {
										"example": "Example",
										"type": "string"
                  					}
								}
                            }
                        },
						"application/yaml": {
							"schema": {
								"properties": {
									"tag": {
										"example": "example",
										"type": "string"
									}
								},
								"x-amf-merge": [
                  					{
                    					"$ref": "#/components/schemas/CreateAPIAssetReqBody"
                  					}
                				]
							}
						}
                    },
                    "required": false
                }
            }
        }
    },
    "components": {
        "schemas": {
            "CreateAPIAssetReqBody": {
                "required": [
                    "Versions",
                    "assetID",
                    "assetName",
                    "desc",
                    "logo",
                    "orgID"
                ],
                "type": "object",
                "properties": {
                    "assetID": {
                        "type": "string",
                        "example": "Example"
                    },
                    "assetName": {
                        "type": "string",
                        "example": "Example"
                    },
                    "desc": {
                        "type": "string",
                        "example": "Example"
                    },
                    "logo": {
                        "type": "string",
                        "example": "Example"
                    },
                    "orgID": {
                        "type": "string",
                        "example": "Example"
                    },
                    "projectID": {
                        "type": "string",
                        "description": "关联的项目ID",
                        "example": "Example"
                    },
                    "appID": {
                        "type": "string",
                        "description": "关联的应用ID",
                        "example": "Example"
                    }
                },
                "description": "创建 API 的请求体",
                "x-http": "group"
            }
        }
    }
}`

// go test -v -run TestExpandRequestBody
func TestExpandRequestBody(t *testing.T) {
	oas3, err := openapi3.NewSwaggerLoader().LoadSwaggerFromData([]byte(text))
	if err != nil {
		t.Fatalf("failed to LoadSwaggerFromData, err: %v", err)
	}
	if oas3.Paths == nil {
		t.Fatal("Paths is nil")
	}
	pathItem := oas3.Paths.Find("/api/api-assets")
	if pathItem == nil {
		t.Fatal("failed to Find")
	}

	if pathItem.Post == nil {
		t.Error("Post operation is nil")
	}
	if pathItem.Post.RequestBody == nil {
		t.Fatal("ExpandRequestBody is nil")
	}

	t.Logf("ExpandRequestBody Ref: %s", pathItem.Post.RequestBody.Ref)

	bodyRef := pathItem.Post.RequestBody
	if err = oas32.ExpandRequestBody(bodyRef, oas3); err != nil {
		t.Errorf("failed to expand.ExpandRequestBody")
	}

	data, _ := json.MarshalIndent(oas3, "", " ")
	t.Logf("expand result: %s", data)
}

// go test -v -run TestExpandSchemaRef
func TestExpandSchemaRef(t *testing.T) {
	oas3, err := openapi3.NewSwaggerLoader().LoadSwaggerFromData([]byte(text))
	if err != nil {
		t.Fatalf("failed to LoadSwaggerFromData, err: %v", err)
	}
	if oas3.Paths == nil {
		t.Fatal("Paths is nil")
	}
	pathItem := oas3.Paths.Find("/api/api-assets")
	if pathItem == nil {
		t.Fatal("failed to Find")
	}

	if pathItem.Post == nil {
		t.Error("Post operation is nil")
	}
	if pathItem.Post.RequestBody == nil {
		t.Fatal("ExpandRequestBody is nil")
	}

	body := pathItem.Post.RequestBody.Value
	if body == nil {
		t.Fatal("ExpandRequestBody.Value is nil")
	}
	t.Logf("body: %+v", *body)
	t.Logf("Body.Content: %v", body.Content)

	applicationJson := body.Content["application/json"]
	applicationYaml := body.Content["application/yaml"]

	if err = oas32.ExpandSchemaRef(applicationJson.Schema, oas3); err != nil {
		t.Errorf("failed to ExpandSchemaRef applicationJson.Schema, err: %v", err)
	}
	if err = oas32.ExpandSchemaRef(applicationYaml.Schema, oas3); err != nil {
		t.Errorf("failed to ExpandSchemaRef applicationYaml.Schema, err: %v", err)
	}

	data, _ := json.MarshalIndent(oas3, "", "  ")
	t.Logf("data: %s", string(data))
}
