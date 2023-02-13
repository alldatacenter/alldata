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

package validator

import (
	"context"
	"fmt"
	"strings"

	"github.com/getkin/kin-openapi/openapi2"
	"github.com/getkin/kin-openapi/openapi2conv"
	"github.com/getkin/kin-openapi/openapi3"
	"github.com/go-openapi/loads"
	"github.com/go-openapi/spec"
	"github.com/pkg/errors"
	yaml2 "k8s.io/apimachinery/pkg/util/yaml"
)

const (
	OAS2JSON = "oas2-json"
	OAS2YAML = "oas2-yaml"
	OAS3YAML = "oas3-yaml"
	OAS3JSON = "oas3-json"
)

// 校验 swagger 2.x 格式合法性
func ValidateOAS2(data []byte, protocol string) (swagger *openapi3.Swagger, err error) {
	switch strings.ToLower(protocol) {
	case OAS2JSON:
	case OAS2YAML:
		if data, err = yaml2.ToJSON(data); err != nil {
			return nil, err
		}
	default:
		return nil, fmt.Errorf("unsupported protocol: %s", protocol)
	}

	v2Swagger := new(openapi2.Swagger)
	if err := v2Swagger.UnmarshalJSON(data); err != nil {
		return nil, err
	}
	v3Swagger, err := openapi2conv.ToV3Swagger(v2Swagger)
	if err != nil {
		return nil, err
	}

	if err = v3Swagger.Validate(context.Background()); err != nil {
		return nil, err
	}
	if v3Swagger.Info == nil {
		return nil, errors.New("no Info in the swagger")
	}
	return v3Swagger, nil
}

// 对 swagger 合法性进行基础性校验
func BasicallyValidateSwagger(data []byte) (*spec.Swagger, error) {
	doc, err := loads.Analyzed(data, "")
	if err != nil {
		return nil, err
	}
	s := doc.Spec()
	if s == nil {
		return nil, errors.New("swagger is invalid")
	}
	if s.Info == nil {
		return nil, errors.New("swagger is invalid: no info")
	}
	return s, nil
}
