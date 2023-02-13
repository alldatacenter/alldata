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
	"context"
	"fmt"
	"strings"

	"github.com/getkin/kin-openapi/openapi3"
	"github.com/pkg/errors"
)

type oAuthFlowType int

const (
	oAuthFlowTypeImplicit oAuthFlowType = iota
	oAuthFlowTypePassword
	oAuthFlowTypeClientCredentials
	oAuthFlowAuthorizationCode
)

type ValidateError struct {
	error string
	path_ []string
}

func (e *ValidateError) Error() string {
	return strings.Join(e.path_, ".") + ": " + e.error
}

func (e *ValidateError) Wrap(err error) error {
	switch err.(type) {
	case nil:
	case *ValidateError:
		e.path_ = append(e.path_, err.(*ValidateError).path_...)
		e.error = err.(*ValidateError).error
	default:
		e.error += ": " + err.Error()
	}

	return e
}

func ValidateOAS3(ctx context.Context, oas3 openapi3.Swagger) error {
	var ve ValidateError

	// 校验文档是否为空
	ve.path_ = []string{"/"}
	if oas3.OpenAPI == "" {
		ve.error = "value of openapi must be a non-empty JSON string"
		return &ve
	}

	// 校验 components
	ve.path_ = []string{"components"}
	if err := ValidateComponents(ctx, oas3.Components); err != nil {
		return ve.Wrap(err)
	}

	// 校验 info
	ve.path_ = []string{"info"}
	if oas3.Info == nil {
		ve.error = "document info is required"
		return &ve
	}
	if err := ValidateInfo(ctx, oas3.Info); err != nil {
		return ve.Wrap(err)
	}

	// 校验 paths
	ve.path_ = []string{"paths"}
	if len(oas3.Paths) == 0 {
		ve.error = "document paths is required"
		return &ve
	}
	if err := ValidatePaths(ctx, oas3.Paths); err != nil {
		return ve.Wrap(err)
	}

	// 校验 security
	ve.path_ = []string{"security"}
	if security := oas3.Security; len(security) != 0 {
		if err := ValidateSecurity(ctx, security); err != nil {
			return ve.Wrap(err)
		}
	}

	// 校验 servers
	ve.path_ = []string{"servers"}
	if servers := oas3.Servers; len(servers) != 0 {
		if err := ValidateServers(ctx, servers); err != nil {
			return ve.Wrap(err)
		}
	}

	return nil
}

func ValidateComponents(ctx context.Context, components openapi3.Components) error {
	var ve ValidateError

	for k, v := range components.Schemas {
		ve.path_ = []string{"schemas", k}
		if err := ValidateIdentifier(k); err != nil {
			return ve.Wrap(err)
		}
		if err := ValidateSchemaRef(ctx, v, nil); err != nil {
			return ve.Wrap(err)
		}
	}

	for k, v := range components.Parameters {
		ve.path_ = []string{"parameters", k}
		if err := ValidateIdentifier(k); err != nil {
			return ve.Wrap(err)
		}
		if err := ValidateParameterRef(ctx, v); err != nil {
			return ve.Wrap(err)
		}
	}

	for k, v := range components.RequestBodies {
		ve.path_ = []string{"requestBodies", k}
		if err := ValidateIdentifier(k); err != nil {
			return ve.Wrap(err)
		}
		if err := ValidateRequestBodyRef(ctx, v); err != nil {
			return ve.Wrap(err)
		}
	}

	for k, v := range components.Responses {
		ve.path_ = []string{"responses", k}
		if err := ValidateIdentifier(k); err != nil {
			return ve.Wrap(err)
		}
		if err := ValidateResponseRef(ctx, v); err != nil {
			return ve.Wrap(err)
		}
	}

	for k, v := range components.Headers {
		ve.path_ = []string{"headers", k}
		if err := ValidateIdentifier(k); err != nil {
			return ve.Wrap(err)
		}
		if err := ValidateHeaderRef(ctx, v); err != nil {
			return ve.Wrap(err)
		}
	}

	for k, v := range components.SecuritySchemes {
		ve.path_ = []string{"securitySchemes", k}
		if err := ValidateIdentifier(k); err != nil {
			return ve.Wrap(err)
		}
		if err := ValidateSecuritySchemeRef(ctx, v); err != nil {
			return ve.Wrap(err)
		}
	}

	return nil
}

func ValidateInfo(ctx context.Context, info *openapi3.Info) error {
	var ve ValidateError

	ve.path_ = []string{"contact"}
	if info.Contact != nil {
		if err := ValidateContact(ctx, info.Contact); err != nil {
			return ve.Wrap(err)
		}
	}

	ve.path_[0] = "license"
	if info.License != nil {
		if err := ValidateLicense(ctx, info.License); err != nil {
			return err
		}
	}

	ve.path_[0] = "version"
	if info.Version == "" {
		return ve.Wrap(errors.New("version 的值必须是非空的 JSON string"))
	}

	ve.path_[0] = "title"
	if info.Title == "" {
		return ve.Wrap(errors.New("title 的值必须是非空的 JSON string"))
	}

	return nil
}

func ValidateContact(_ context.Context, _ *openapi3.Contact) error {
	// 不会有错
	return nil
}

func ValidateLicense(_ context.Context, license *openapi3.License) error {
	var ve ValidateError

	ve.path_ = []string{"name"}
	if license.Name == "" {
		return ve.Wrap(errors.New("license name 的值必须是非空的 JSON string"))
	}
	return nil
}

func ValidatePaths(ctx context.Context, paths openapi3.Paths) error {
	var ve ValidateError

	normalizedPaths := make(map[string]string)
	for path, pathItem := range paths {
		ve.path_ = []string{path}

		if path == "" || path[0] != '/' {
			return ve.Wrap(errors.New("path 应当以 (/) 开头"))
		}

		normalizedPath, pathParamsCount := normalizeTemplatedPath(path)
		if oldPath, ok := normalizedPaths[normalizedPath]; ok {
			return ve.Wrap(errors.Errorf("paths 冲突 %q and %q", path, oldPath))
		}
		normalizedPaths[path] = path

		var globalCount uint
		for i, parameterRef := range pathItem.Parameters {
			ve.path_ = []string{path, fmt.Sprintf("[%d]", i)}

			if parameterRef != nil {
				if parameter := parameterRef.Value; parameter != nil && parameter.In == openapi3.ParameterInPath {
					globalCount++
				}
			}
		}
		for method, operation := range pathItem.Operations() {
			ve.path_ = []string{path, method}

			var count uint
			for i, parameterRef := range operation.Parameters {
				ve.path_ = []string{path, method, fmt.Sprintf("[%d]", i)}

				if parameterRef != nil {
					if parameter := parameterRef.Value; parameter != nil && parameter.In == openapi3.ParameterInPath {
						count++
					}
				}
			}
			if count+globalCount != pathParamsCount {
				return ve.Wrap(errors.New("路径中含有路径参数时应当定义 path 层级的 parameters"))
			}
		}

		ve.path_ = []string{path}
		if err := ValidatePathItem(ctx, pathItem); err != nil {
			return ve.Wrap(err)
		}
	}
	return nil
}

func ValidatePathItem(ctx context.Context, pathItem *openapi3.PathItem) error {
	for k, operation := range pathItem.Operations() {
		var ve = ValidateError{path_: []string{k}}
		if err := ValidateOperation(ctx, operation); err != nil {
			return ve.Wrap(err)
		}
	}
	return nil
}

func ValidateOperation(ctx context.Context, operation *openapi3.Operation) error {
	var ve = ValidateError{path_: []string{"parameters"}}
	if v := operation.Parameters; v != nil {
		if err := ValidateParameters(ctx, v); err != nil {
			return ve.Wrap(err)
		}
	}

	ve.path_[0] = "requestBody"
	if v := operation.RequestBody; v != nil {
		if err := ValidateRequestBodyRef(ctx, v); err != nil {
			return ve.Wrap(err)
		}
	}

	ve.path_[0] = "responses"
	if operation.Responses == nil {
		return ve.Wrap(errors.New("responses 是 required 字段, 不可缺失"))
	}
	if len(operation.Responses) == 0 {
		return ve.Wrap(errors.New("responses 对象至少有一个 response code"))
	}
	for code, v := range operation.Responses {
		ve.path_ = []string{"responses", code}
		if err := ValidateResponseRef(ctx, v); err != nil {
			return ve.Wrap(err)
		}
	}

	return nil
}

func ValidateParameters(ctx context.Context, parameters openapi3.Parameters) error {
	dupes := make(map[string]struct{})
	for _, item := range parameters {
		if v := item.Value; v != nil {
			key := v.In + ":" + v.Name
			if _, ok := dupes[key]; ok {
				return errors.Errorf("more than one %q parameter has name %q", v.In, v.Name)
			}
			dupes[key] = struct{}{}
		}

		if err := ValidateParameterRef(ctx, item); err != nil {
			return err
		}
	}
	return nil
}

func ValidateSecurity(ctx context.Context, securities openapi3.SecurityRequirements) error {
	for i, item := range securities {
		var ve = ValidateError{path_: []string{fmt.Sprintf("[%d]", i)}}
		if err := ValidateSecurityRequirement(ctx, item); err != nil {
			return ve.Wrap(err)
		}
	}
	return nil
}

func ValidateSecurityRequirement(_ context.Context, _ openapi3.SecurityRequirement) error {
	// 不会有错
	return nil
}

func ValidateServers(ctx context.Context, servers openapi3.Servers) error {
	for _, v := range servers {
		if err := ValidateServer(ctx, v); err != nil {
			return err
		}
	}
	return nil
}

func ValidateServer(ctx context.Context, server *openapi3.Server) error {
	var ve = ValidateError{path_: []string{"URL"}}
	if server.URL == "" {
		return ve.Wrap(errors.New("url 的值必须是非空的 JSON string"))
	}
	for k, v := range server.Variables {
		ve.path_ = []string{k}
		if err := ValidateServerVariable(ctx, v); err != nil {
			return ve.Wrap(err)
		}
	}
	return nil
}

func ValidateServerVariable(ctx context.Context, serverVariable *openapi3.ServerVariable) error {
	var ve ValidateError
	switch serverVariable.Default.(type) {
	case float64, string:
	default:
		return ve.Wrap(errors.New("默认值应当是 JSON number 或 JSON string"))
	}
	for i, item := range serverVariable.Enum {
		ve.path_ = []string{fmt.Sprintf("[%d]", i)}
		switch item.(type) {
		case float64, string:
		default:
			return ve.Wrap(errors.New("枚举值必须是 number 或 string"))
		}
	}
	return nil
}

func ValidateIdentifier(_ string) error {
	// 允许任何字符
	return nil
}

func ValidateSchemaRef(ctx context.Context, ref *openapi3.SchemaRef, stack []*openapi3.Schema) error {
	if ref.Value == nil {
		return foundUnresolvedRef(ref.Ref)
	}

	return ValidateSchema(ctx, ref.Value, stack)
}

func ValidateSchema(ctx context.Context, schema *openapi3.Schema, stack []*openapi3.Schema) error {
	for _, existing := range stack {
		if existing == schema {
			return nil
		}
	}
	stack = append(stack, schema)

	for _, item := range schema.OneOf {
		if err := ValidateSchemaRef(ctx, item, stack); err != nil {
			return err
		}
	}

	for _, item := range schema.AnyOf {
		if err := ValidateSchemaRef(ctx, item, stack); err != nil {
			return err
		}
	}

	for _, item := range schema.AllOf {
		if err := ValidateSchemaRef(ctx, item, stack); err != nil {
			return err
		}
	}

	if ref := schema.Not; ref != nil {
		if err := ValidateSchemaRef(ctx, ref, stack); err != nil {
			return err
		}
	}

	schemaType := schema.Type
	switch schemaType {
	case "":
	case "boolean":
	case "number":
		// format is an open value, do not to validate it
	case "integer":
		// format is an open value, do not to validate it
	case "string":
		// format is an open value, do not to validate it
	case "array":
		if schema.Items == nil {
			return errors.New("'array' items can not be non-null")
		}
	case "object":
	default:
		return errors.Errorf("unsuport type '%s'", schemaType)
	}

	if ref := schema.Items; ref != nil {
		if err := ValidateSchemaRef(ctx, ref, stack); err != nil {
			return err
		}
	}

	for _, ref := range schema.Properties {
		var ve = ValidateError{path_: []string{"properties"}}
		if err := ValidateSchemaRef(ctx, ref, stack); err != nil {
			return ve.Wrap(err)
		}
	}

	if ref := schema.AdditionalProperties; ref != nil {
		if err := ValidateSchemaRef(ctx, ref, stack); err != nil {
			return err
		}
	}

	return nil
}

func ValidateParameterRef(ctx context.Context, ref *openapi3.ParameterRef) error {
	if ref.Value == nil {
		return foundUnresolvedRef(ref.Ref)
	}
	return ValidateParameter(ctx, ref.Value)
}

func ValidateParameter(ctx context.Context, parameter *openapi3.Parameter) error {
	if parameter.Name == "" {
		return errors.New("parameter 名不能为空")
	}
	in := parameter.In
	switch in {
	case
		openapi3.ParameterInPath,
		openapi3.ParameterInQuery,
		openapi3.ParameterInHeader,
		openapi3.ParameterInCookie:
	default:
		return errors.Errorf("parameter can't have 'in' value %q", parameter.In)
	}

	// Validate a parameter's serialization method.
	sm, err := parameter.SerializationMethod()
	if err != nil {
		return err
	}
	var smSupported bool
	switch {
	case parameter.In == openapi3.ParameterInPath && sm.Style == openapi3.SerializationSimple && !sm.Explode,
		parameter.In == openapi3.ParameterInPath && sm.Style == openapi3.SerializationSimple && sm.Explode,
		parameter.In == openapi3.ParameterInPath && sm.Style == openapi3.SerializationLabel && !sm.Explode,
		parameter.In == openapi3.ParameterInPath && sm.Style == openapi3.SerializationLabel && sm.Explode,
		parameter.In == openapi3.ParameterInPath && sm.Style == openapi3.SerializationMatrix && !sm.Explode,
		parameter.In == openapi3.ParameterInPath && sm.Style == openapi3.SerializationMatrix && sm.Explode,

		parameter.In == openapi3.ParameterInQuery && sm.Style == openapi3.SerializationForm && sm.Explode,
		parameter.In == openapi3.ParameterInQuery && sm.Style == openapi3.SerializationForm && !sm.Explode,
		parameter.In == openapi3.ParameterInQuery && sm.Style == openapi3.SerializationSpaceDelimited && sm.Explode,
		parameter.In == openapi3.ParameterInQuery && sm.Style == openapi3.SerializationSpaceDelimited && !sm.Explode,
		parameter.In == openapi3.ParameterInQuery && sm.Style == openapi3.SerializationPipeDelimited && sm.Explode,
		parameter.In == openapi3.ParameterInQuery && sm.Style == openapi3.SerializationPipeDelimited && !sm.Explode,
		parameter.In == openapi3.ParameterInQuery && sm.Style == openapi3.SerializationDeepObject && sm.Explode,

		parameter.In == openapi3.ParameterInHeader && sm.Style == openapi3.SerializationSimple && !sm.Explode,
		parameter.In == openapi3.ParameterInHeader && sm.Style == openapi3.SerializationSimple && sm.Explode,

		parameter.In == openapi3.ParameterInCookie && sm.Style == openapi3.SerializationForm && !sm.Explode,
		parameter.In == openapi3.ParameterInCookie && sm.Style == openapi3.SerializationForm && sm.Explode:
		smSupported = true
	}
	if !smSupported {
		e := errors.Errorf("serialization method with style=%q and explode=%v is not supported by a %s parameter", sm.Style, sm.Explode, in)
		return errors.Errorf("parameter %q schema is invalid: %v", parameter.Name, e)
	}

	if (parameter.Schema == nil) == (parameter.Content == nil) {
		e := errors.New("parameter 必须包含一个 content 和 schema")
		return errors.Errorf("不合法的 parameter %q schema: %v", parameter.Name, e)
	}
	if schema := parameter.Schema; schema != nil {
		if err := ValidateSchemaRef(ctx, schema, nil); err != nil {
			return errors.Errorf("不合法的 parameter %q schema: %v", parameter.Name, err)
		}
	}
	if content := parameter.Content; content != nil {
		if err := ValidateContent(ctx, content); err != nil {
			return errors.Errorf("不合法的 parameter %q content: %v", parameter.Name, err)
		}
	}
	return nil
}

func ValidateRequestBodyRef(ctx context.Context, ref *openapi3.RequestBodyRef) error {
	if ref.Value == nil {
		return foundUnresolvedRef(ref.Ref)
	}

	return ValidateRequestBody(ctx, ref.Value)
}

func ValidateRequestBody(ctx context.Context, requestBody *openapi3.RequestBody) error {
	if requestBody.Content == nil {
		return nil
	}

	var ve = ValidateError{path_: []string{"content"}}
	if err := ValidateContent(ctx, requestBody.Content); err != nil {
		return ve.Wrap(err)
	}
	return nil
}

func ValidateResponseRef(ctx context.Context, ref *openapi3.ResponseRef) error {
	if ref.Value == nil {
		return foundUnresolvedRef(ref.Ref)
	}
	return ValidateResponse(ctx, ref.Value)
}

func ValidateResponse(ctx context.Context, response *openapi3.Response) error {
	if response.Description == nil {
		var ve = ValidateError{path_: []string{"description"}}
		return ve.Wrap(errors.New("description 是 required 字段, 不可缺失"))
	}

	if content := response.Content; content != nil {
		var ve = ValidateError{path_: []string{"content"}}
		if err := ValidateContent(ctx, content); err != nil {
			return ve.Wrap(err)
		}
	}

	return nil
}

func ValidateHeaderRef(ctx context.Context, ref *openapi3.HeaderRef) error {
	if ref.Value == nil {
		return foundUnresolvedRef(ref.Ref)
	}
	return ValidateHeader(ctx, ref.Value)
}

func ValidateHeader(ctx context.Context, header *openapi3.Header) error {
	if header.Schema == nil {
		return nil
	}

	var ve = ValidateError{path_: []string{"schema"}}
	if err := ValidateSchemaRef(ctx, header.Schema, nil); err != nil {
		return ve.Wrap(err)
	}
	return nil
}

func ValidateSecuritySchemeRef(ctx context.Context, ref *openapi3.SecuritySchemeRef) error {
	if ref.Value == nil {
		return foundUnresolvedRef(ref.Ref)
	}
	return ValidateSecurityScheme(ctx, ref.Value)
}

func ValidateSecurityScheme(ctx context.Context, ss *openapi3.SecurityScheme) error {
	hasIn := false
	hasBearerFormat := false
	hasFlow := false
	switch ss.Type {
	case "apiKey":
		hasIn = true
	case "http":
		scheme := ss.Scheme
		switch scheme {
		case "bearer":
			hasBearerFormat = true
		case "basic":
		default:
			return errors.Errorf("Security scheme of type 'http' has invalid 'scheme' value '%s'", scheme)
		}
	case "oauth2":
		hasFlow = true
	case "openIdConnect":
		return errors.Errorf("Support for security schemes with type '%v' has not been implemented", ss.Type)
	default:
		return errors.Errorf("Security scheme 'type' can't be '%v'", ss.Type)
	}

	// Validate "in" and "name"
	if hasIn {
		switch ss.In {
		case "query", "header", "cookie":
		default:
			return errors.Errorf("Security scheme of type 'apiKey' should have 'in'. It can be 'query', 'header' or 'cookie', not '%s'", ss.In)
		}
		if ss.Name == "" {
			return errors.New("Security scheme of type 'apiKey' should have 'name'")
		}
	} else if len(ss.In) > 0 {
		return errors.Errorf("Security scheme of type '%s' can't have 'in'", ss.Type)
	} else if len(ss.Name) > 0 {
		return errors.New("Security scheme of type 'apiKey' can't have 'name'")
	}

	// Validate "format"
	// "bearerFormat" is an arbitrary string so we only check if the scheme supports it
	if !hasBearerFormat && len(ss.BearerFormat) > 0 {
		return errors.Errorf("Security scheme of type '%v' can't have 'bearerFormat'", ss.Type)
	}

	// Validate "flow"
	if hasFlow {
		flow := ss.Flows
		if flow == nil {
			return errors.Errorf("Security scheme of type '%v' should have 'flows'", ss.Type)
		}
		if err := ValidateOAuthFlows(ctx, flow); err != nil {
			return errors.Errorf("Security scheme 'flow' is invalid: %v", err)
		}
	} else if ss.Flows != nil {
		return errors.Errorf("Security scheme of type '%s' can't have 'flows'", ss.Type)
	}
	return nil
}

func ValidateOAuthFlows(ctx context.Context, flows *openapi3.OAuthFlows) error {
	if v := flows.Implicit; v != nil {
		return ValidateOAuthFlow(ctx, v, oAuthFlowTypeImplicit)
	}
	if v := flows.Password; v != nil {
		return ValidateOAuthFlow(ctx, v, oAuthFlowTypePassword)
	}
	if v := flows.ClientCredentials; v != nil {
		return ValidateOAuthFlow(ctx, v, oAuthFlowTypeClientCredentials)
	}
	if v := flows.AuthorizationCode; v != nil {
		return ValidateOAuthFlow(ctx, v, oAuthFlowAuthorizationCode)
	}
	return errors.New("No OAuth flow is defined")
}

func ValidateOAuthFlow(_ context.Context, flow *openapi3.OAuthFlow, typ oAuthFlowType) error {
	if typ == oAuthFlowAuthorizationCode || typ == oAuthFlowTypeImplicit {
		if v := flow.AuthorizationURL; v == "" {
			return errors.New("An OAuth flow is missing 'authorizationUrl in authorizationCode or implicit '")
		}
	}
	if typ != oAuthFlowTypeImplicit {
		if v := flow.TokenURL; v == "" {
			return errors.New("An OAuth flow is missing 'tokenUrl in not implicit'")
		}
	}
	if v := flow.Scopes; v == nil {
		return errors.New("An OAuth flow is missing 'scopes'")
	}
	return nil
}

func ValidateContent(ctx context.Context, content openapi3.Content) error {
	for mt, mediaType := range content {
		var ve = ValidateError{path_: []string{mt}}
		if err := ValidateMediaType(ctx, mediaType); err != nil {
			return ve.Wrap(err)
		}
	}
	return nil
}

func ValidateMediaType(ctx context.Context, mediaType *openapi3.MediaType) error {
	if mediaType == nil {
		return nil
	}
	if schema := mediaType.Schema; schema != nil {
		var ve = ValidateError{path_: []string{"schema"}}
		if err := ValidateSchemaRef(ctx, schema, nil); err != nil {
			return ve.Wrap(err)
		}
	}
	return nil
}

func foundUnresolvedRef(ref string) error {
	return errors.Errorf("Found unresolved ref: '%s'", ref)
}

func failedToResolveRefFragment(value string) error {
	return errors.Errorf("Failed to resolve fragment in URI: '%s'", value)
}

func failedToResolveRefFragmentPart(value string, what string) error {
	return errors.Errorf("Failed to resolve '%s' in fragment in URI: '%s'", what, value)
}

func unsupportedFormat(format string) error {
	return errors.Errorf("Unsupported 'format' value '%s'", format)
}

func normalizeTemplatedPath(path string) (string, uint) {
	if strings.IndexByte(path, '{') < 0 {
		return path, 0
	}

	var buf strings.Builder
	buf.Grow(len(path))

	var (
		cc         rune
		count      uint
		isVariable bool
	)
	for i, c := range path {
		if isVariable {
			if c == '}' {
				// End path variables
				// First append possible '*' before this character
				// The character '}' will be appended
				if i > 0 && cc == '*' {
					buf.WriteRune(cc)
				}
				isVariable = false
			} else {
				// Skip this character
				continue
			}
		} else if c == '{' {
			// Begin path variable
			// The character '{' will be appended
			isVariable = true
			count++
		}

		// Append the character
		buf.WriteRune(c)
		cc = c
	}
	return buf.String(), count
}
