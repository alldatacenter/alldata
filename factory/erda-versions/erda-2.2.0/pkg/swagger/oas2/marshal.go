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

package oas2

import (
	"bytes"
	"encoding/json"

	"github.com/getkin/kin-openapi/openapi2"
	"sigs.k8s.io/yaml"
)

func MarshalJson(v2 *openapi2.Swagger) ([]byte, error) {
	return json.Marshal(v2)
}

func MarshalJsonIndent(v2 *openapi2.Swagger, prefix, indent string) ([]byte, error) {
	return json.MarshalIndent(v2, prefix, indent)
}

func MarshalYaml(v2 *openapi2.Swagger) ([]byte, error) {
	var buf = bytes.NewBuffer(nil)
	swagger, _ := yaml.Marshal(v2.Swagger)
	buf.WriteString("swagger: ")
	buf.Write(swagger)

	info, _ := yaml.Marshal(v2.Info)
	buf.WriteString("\ninfo:")
	buf.Write(indent(info))

	if len(v2.Host) > 0 {
		host, _ := yaml.Marshal(v2.Host)
		buf.WriteString("\nhost: ")
		buf.Write(host)
	}

	if len(v2.BasePath) > 0 {
		basePath, _ := yaml.Marshal(v2.BasePath)
		buf.WriteString("\nbasePath: ")
		buf.Write(basePath)
	}

	if len(v2.Schemes) > 0 {
		schemes, _ := yaml.Marshal(v2.Schemes)
		buf.WriteString("\nschemes:")
		buf.Write(indent(schemes))
	}

	paths, _ := yaml.Marshal(v2.Paths)
	buf.WriteString("\npaths:")
	buf.Write(indent(paths))

	if len(v2.Definitions) > 0 {
		definitions, _ := yaml.Marshal(v2.Definitions)
		buf.WriteString("\ndefinitions:")
		buf.Write(indent(definitions))
	}

	if len(v2.Parameters) > 0 {
		parameters, _ := yaml.Marshal(v2.Parameters)
		buf.WriteString("\nparameters:")
		buf.Write(indent(parameters))
	}

	if len(v2.Responses) > 0 {
		responses, _ := yaml.Marshal(v2.Responses)
		buf.WriteString("\nresponses:")
		buf.Write(indent(responses))
	}

	if len(v2.SecurityDefinitions) > 0 {
		securityDefinitions, _ := yaml.Marshal(v2.SecurityDefinitions)
		buf.WriteString("\nsecurityDefinitions:")
		buf.Write(indent(securityDefinitions))
	}

	if len(v2.Security) > 0 {
		security, _ := yaml.Marshal(v2.Security)
		buf.WriteString("\nsecurity:")
		buf.Write(indent(security))
	}

	if len(v2.Tags) > 0 {
		tags, _ := yaml.Marshal(v2.Tags)
		buf.WriteString("\ntags:")
		buf.Write(indent(tags))
	}

	if v2.ExternalDocs != nil {
		externalDocs, _ := yaml.Marshal(v2.ExternalDocs)
		buf.WriteString("\nexternalDocs:")
		buf.Write(indent(externalDocs))
	}

	if len(v2.Extensions) > 0 {
		extensions, _ := yaml.Marshal(v2.Extensions)
		buf.Write(extensions)
	}

	return buf.Bytes(), nil
}

func indent(s []byte) []byte {
	split := bytes.Split(s, []byte{'\n'})
	var buf = bytes.NewBuffer(nil)
	for _, ele := range split {
		buf.WriteString("\n")
		buf.WriteString("  ")
		buf.Write(ele)
	}
	return buf.Bytes()
}
