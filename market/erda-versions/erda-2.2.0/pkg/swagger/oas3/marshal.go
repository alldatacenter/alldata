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

package oas3

import (
	"bytes"
	"encoding/json"

	"github.com/getkin/kin-openapi/openapi3"
	"sigs.k8s.io/yaml"
)

func MarshalJson(v3 *openapi3.Swagger) ([]byte, error) {
	return json.Marshal(v3)
}

func MarshalJsonIndent(v3 *openapi3.Swagger, prefix, indent string) ([]byte, error) {
	return json.MarshalIndent(v3, prefix, indent)
}

func MarshalYaml(v3 *openapi3.Swagger) ([]byte, error) {
	var buf = bytes.NewBuffer(nil)
	openapi, _ := yaml.Marshal(v3.OpenAPI)
	buf.WriteString("openapi: ")
	buf.Write(openapi)

	info, _ := yaml.Marshal(v3.Info)
	buf.WriteString("\ninfo:")
	buf.Write(indent(info))
	if len(v3.Servers) > 0 {
		servers, _ := yaml.Marshal(v3.Servers)
		buf.WriteString("\nservers:")
		buf.Write(indent(servers))
	}

	paths, _ := yaml.Marshal(v3.Paths)
	buf.WriteString("\npaths:")
	buf.Write(indent(paths))

	components, _ := yaml.Marshal(v3.Components)
	buf.WriteString("\ncomponents:")
	buf.Write(indent(components))

	if len(v3.Security) > 0 {
		security, _ := yaml.Marshal(v3.Security)
		buf.WriteString("\nsecurity:")
		buf.Write(indent(security))
	}

	if len(v3.Tags) > 0 {
		tags, _ := yaml.Marshal(v3.Tags)
		buf.WriteString("\ntags:")
		buf.Write(indent(tags))
	}

	if v3.ExternalDocs != nil {
		externalDocs, _ := yaml.Marshal(v3.ExternalDocs)
		buf.WriteString("\nexternalDocs:")
		buf.Write(indent(externalDocs))
	}

	if len(v3.Extensions) > 0 {
		extensions, _ := yaml.Marshal(v3.Extensions)
		buf.WriteString("\n")
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
