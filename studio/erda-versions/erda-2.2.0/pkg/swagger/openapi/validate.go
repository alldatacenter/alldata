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

package openapi

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

func ValidateOAS3(ctx context.Context, oas3 openapi3.Swagger) error {
	// 校验文档是否为空
	if oas3.OpenAPI == "" {
		return errors.New("value of openapi must be a non-empty JSON string")
	}

	// 校验 components
	if err := ValidateComponents(ctx, oas3.Components); err != nil {
		return errors.Wrap(err, "invalid components")
	}

	// 校验 info
	if oas3.Info == nil {
		err := errors.New("must be an object")
		return errors.Wrap(err, "invalid info")
	}
	if err := ValidateInfo(ctx, oas3.Info); err != nil {
		return errors.Wrap(err, "invalid info")
	}

	// 校验 path
	if len(oas3.Paths) == 0 {
		err := errors.New("paths must be an object")
		return errors.Wrap(err, "invalid paths")
	}
	if err := ValidatePaths(ctx, oas3.Paths); err != nil {
		return errors.Wrap(err, "invalid paths")
	}

	// 校验 security
	if security := oas3.Security; len(security) != 0 {
		if err := ValidateSecurity(ctx, security); err != nil {
			return errors.Wrap(err, "invalid security")
		}
	}

	// 校验 servers
	if servers := oas3.Servers; len(servers) != 0 {
		if err := ValidateServers(ctx, servers); err != nil {
			return errors.Wrap(err, "invalid servers")
		}
	}

	return nil
}

func ValidateComponents(ctx context.Context, components openapi3.Components) (err error) {
	for k, v := range components.Schemas {
		if err = ValidateIdentifier(k); err != nil {
			return
		}
		if err = ValidateSchemaRef(ctx, v, nil); err != nil {
			return
		}
	}

	for k, v := range components.Parameters {
		if err = ValidateIdentifier(k); err != nil {
			return
		}
		if err = ValidateParameterRef(ctx, v); err != nil {
			return
		}
	}

	for k, v := range components.RequestBodies {
		if err = ValidateIdentifier(k); err != nil {
			return
		}
		if err = ValidateRequestBodyRef(ctx, v); err != nil {
			return
		}
	}

	for k, v := range components.Responses {
		if err = ValidateIdentifier(k); err != nil {
			return
		}
		if err = ValidateResponseRef(ctx, v); err != nil {
			return
		}
	}

	for k, v := range components.Headers {
		if err = ValidateIdentifier(k); err != nil {
			return
		}
		if err = ValidateHeaderRef(ctx, v); err != nil {
			return
		}
	}

	for k, v := range components.SecuritySchemes {
		if err = ValidateIdentifier(k); err != nil {
			return
		}
		if err = ValidateSecuritySchemeRef(ctx, v); err != nil {
			return
		}
	}

	return nil
}

func ValidateInfo(ctx context.Context, info *openapi3.Info) error {
	if info.Contact != nil {
		if err := ValidateContact(ctx, info.Contact); err != nil {
			return err
		}
	}

	if info.License != nil {
		if err := ValidateLicense(ctx, info.License); err != nil {
			return err
		}
	}

	if info.Version == "" {
		return errors.New("value of version must be a non-empty JSON string")
	}

	if info.Title == "" {
		return errors.New("value of title must be a non-empty JSON string")
	}

	return nil
}

func ValidateContact(_ context.Context, _ *openapi3.Contact) error {
	// 不会有错
	return nil
}

func ValidateLicense(_ context.Context, license *openapi3.License) error {
	if license.Name == "" {
		return errors.New("value of license name must be a non-empty JSON string")
	}
	return nil
}

func ValidatePaths(ctx context.Context, paths openapi3.Paths) error {
	normalizedPaths := make(map[string]string)
	for path, pathItem := range paths {
		if path == "" || path[0] != '/' {
			return fmt.Errorf("path %q does not start with a forward slash (/)", path)
		}

		normalizedPath, pathParamsCount := normalizeTemplatedPath(path)
		if oldPath, ok := normalizedPaths[normalizedPath]; ok {
			return fmt.Errorf("conflicting paths %q and %q", path, oldPath)
		}
		normalizedPaths[path] = path

		var globalCount uint
		for _, parameterRef := range pathItem.Parameters {
			if parameterRef != nil {
				if parameter := parameterRef.Value; parameter != nil && parameter.In == openapi3.ParameterInPath {
					globalCount++
				}
			}
		}
		for method, operation := range pathItem.Operations() {
			var count uint
			for _, parameterRef := range operation.Parameters {
				if parameterRef != nil {
					if parameter := parameterRef.Value; parameter != nil && parameter.In == openapi3.ParameterInPath {
						count++
					}
				}
			}
			if count+globalCount != pathParamsCount {
				return fmt.Errorf("operation %s %s must define exactly all path parameters", method, path)
			}
		}

		if err := ValidatePathItem(ctx, pathItem); err != nil {
			return err
		}
	}
	return nil
}

func ValidatePathItem(ctx context.Context, pathItem *openapi3.PathItem) error {
	for _, operation := range pathItem.Operations() {
		if err := ValidateOperation(ctx, operation); err != nil {
			return err
		}
	}
	return nil
}

func ValidateOperation(ctx context.Context, operation *openapi3.Operation) error {
	if v := operation.Parameters; v != nil {
		if err := v.Validate(ctx); err != nil {
			return err
		}
	}
	if v := operation.RequestBody; v != nil {
		if err := v.Validate(ctx); err != nil {
			return err
		}
	}
	if v := operation.Responses; v != nil {
		if err := v.Validate(ctx); err != nil {
			return err
		}
	} else {
		return errors.New("value of responses must be a JSON object")
	}
	return nil
}

func ValidateSecurity(ctx context.Context, securities openapi3.SecurityRequirements) error {
	for _, item := range securities {
		if err := ValidateSecurityRequirement(ctx, item); err != nil {
			return err
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

func ValidateServer(ctx context.Context, server *openapi3.Server) (err error) {
	if server.URL == "" {
		return errors.New("value of url must be a non-empty JSON string")
	}
	for _, v := range server.Variables {
		if err = v.Validate(ctx); err != nil {
			return
		}
	}
	return
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

func ValidateSchema(ctx context.Context, schema *openapi3.Schema, stack []*openapi3.Schema) (err error) {
	for _, existing := range stack {
		if existing == schema {
			return
		}
	}
	stack = append(stack, schema)

	for _, item := range schema.OneOf {
		if err = ValidateSchemaRef(ctx, item, stack); err != nil {
			return err
		}
	}

	for _, item := range schema.AnyOf {
		if err = ValidateSchemaRef(ctx, item, stack); err != nil {
			return err
		}
	}

	for _, item := range schema.AllOf {
		if err = ValidateSchemaRef(ctx, item, stack); err != nil {
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
		if format := schema.Format; len(format) > 0 {
			switch format {
			case "float", "double":
			default:
				if !openapi3.SchemaFormatValidationDisabled {
					return unsupportedFormat(format)
				}
			}
		}
	case "integer":
		if format := schema.Format; len(format) > 0 {
			switch format {
			case "int32", "int64":
			default:
				if !openapi3.SchemaFormatValidationDisabled {
					return unsupportedFormat(format)
				}
			}
		}
	case "string":
		if format := schema.Format; len(format) > 0 {
			switch format {
			// Supported by OpenAPIv3.0.1:
			case "byte", "binary", "date", "date-time", "password":
				// In JSON Draft-07 (not validated yet though):
			case "regex":
			case "time", "email", "idn-email":
			case "hostname", "idn-hostname", "ipv4", "ipv6":
			case "uri", "uri-reference", "iri", "iri-reference", "uri-template":
			case "json-pointer", "relative-json-pointer":
			default:
				// Try to check for custom defined formats
				if _, ok := openapi3.SchemaStringFormats[format]; !ok && !openapi3.SchemaFormatValidationDisabled {
					return unsupportedFormat(format)
				}
			}
		}
	case "array":
		if schema.Items == nil {
			return errors.New("When schema type is 'array', schema 'items' must be non-null")
		}
	case "object":
	default:
		return errors.Errorf("Unsupported 'type' value '%s'", schemaType)
	}

	if ref := schema.Items; ref != nil {
		if err := ValidateSchemaRef(ctx, ref, stack); err != nil {
			return err
		}
	}

	for _, ref := range schema.Properties {
		if err := ValidateSchemaRef(ctx, ref, stack); err != nil {
			return err
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
		return errors.New("parameter name can't be blank")
	}
	in := parameter.In
	switch in {
	case
		openapi3.ParameterInPath,
		openapi3.ParameterInQuery,
		openapi3.ParameterInHeader,
		openapi3.ParameterInCookie:
	default:
		return fmt.Errorf("parameter can't have 'in' value %q", parameter.In)
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
		e := fmt.Errorf("serialization method with style=%q and explode=%v is not supported by a %s parameter", sm.Style, sm.Explode, in)
		return fmt.Errorf("parameter %q schema is invalid: %v", parameter.Name, e)
	}

	if (parameter.Schema == nil) == (parameter.Content == nil) {
		e := errors.New("parameter must contain exactly one of content and schema")
		return fmt.Errorf("parameter %q schema is invalid: %v", parameter.Name, e)
	}
	if schema := parameter.Schema; schema != nil {
		if err := ValidateSchemaRef(ctx, schema, nil); err != nil {
			return fmt.Errorf("parameter %q schema is invalid: %v", parameter.Name, err)
		}
	}
	if content := parameter.Content; content != nil {
		if err := ValidateContent(ctx, content); err != nil {
			return errors.Errorf("parameter %q content is invalid: %v", parameter.Name, err)
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
	return ValidateContent(ctx, requestBody.Content)
}

func ValidateResponseRef(ctx context.Context, ref *openapi3.ResponseRef) error {
	if ref.Value == nil {
		return foundUnresolvedRef(ref.Ref)
	}
	return ValidateResponse(ctx, ref.Value)
}

func ValidateResponse(ctx context.Context, response *openapi3.Response) error {
	if response.Description == nil {
		return errors.New("a short description of the response is required")
	}

	if content := response.Content; content != nil {
		if err := ValidateContent(ctx, content); err != nil {
			return err
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
	return ValidateSchemaRef(ctx, header.Schema, nil)
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
		return fmt.Errorf("Security scheme of type '%v' can't have 'bearerFormat'", ss.Type)
	}

	// Validate "flow"
	if hasFlow {
		flow := ss.Flows
		if flow == nil {
			return fmt.Errorf("Security scheme of type '%v' should have 'flows'", ss.Type)
		}
		if err := ValidateOAuthFlows(ctx, flow); err != nil {
			return fmt.Errorf("Security scheme 'flow' is invalid: %v", err)
		}
	} else if ss.Flows != nil {
		return fmt.Errorf("Security scheme of type '%s' can't have 'flows'", ss.Type)
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
	for _, mediaType := range content {
		if err := ValidateMediaType(ctx, mediaType); err != nil {
			return err
		}
	}
	return nil
}

func ValidateMediaType(ctx context.Context, mediaType *openapi3.MediaType) error {
	if mediaType == nil {
		return nil
	}
	if schema := mediaType.Schema; schema != nil {
		if err := ValidateSchemaRef(ctx, schema, nil); err != nil {
			return err
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
