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

package apis

import (
	"fmt"
	"net/http"
	"net/url"
	"reflect"
	"regexp"
	"sort"
	"strings"
	"time"

	"github.com/getkin/kin-openapi/openapi3"
	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/spec"
)

const (
	defaultDesc     = "write description here"
	refPrefix       = "#/components/schemas/"
	emptyObjectName = "emptyObject"
)

var (
	emptyObject = openapi3.NewSchemaRef("", openapi3.NewObjectSchema())
)

// ApiSpec 转换成 openapi.api.Spec，方便用户写的类型
type ApiSpec struct {
	Path        string
	BackendPath string
	Method      string
	Host        string
	// 正常情况下，使用 internal/pkg/innerdomain 能解析转换出 `Host` 对应的 marathonHost 和 k8sHost,
	// 但是，当 `Host` 中的地址是老版的 marathon 内部地址，那么就无法确定 k8s地址会是什么，需要用 `K8SHost` 显式指定
	// 比如以下地址就无法转换
	// "hepa-gateway-1.hepagateway.addon-hepa-gateway.v1.runtimes.marathon.l4lb.thisdcos.directory"
	K8SHost         string
	Scheme          string
	Custom          func(rw http.ResponseWriter, req *http.Request)
	CustomResponse  func(*http.Response) error // 如果是 websocket，没意义，在 generator 里检查
	Audit           func(ctx *spec.AuditContext) error
	NeedDesensitize bool // 是否需要对返回的 userinfo 进行脱敏处理
	CheckLogin      bool
	TryCheckLogin   bool
	CheckToken      bool
	CheckBasicAuth  bool
	ChunkAPI        bool
	Doc             string
	// API 请求 & 应答 类型, 定义在 apistructs
	RequestType  interface{}
	ResponseType interface{}
	// 是否为真正的openapi，会生成2份 swagger doc， 一份是只有openapi的，另一份有所有注册的API
	IsOpenAPI bool
	// API 分类， 默认为Path的第二部分 /a/b/c -> b
	Group string

	// Parameters describes the request and response parameters
	Parameters *Parameters
	operation  *openapi3.Operation
	v3         *openapi3.Swagger
}

// Convert2AccessibleApi 直接从 openapi 定义生成 openapi oauth2 token 可访问的 api 格式
func (api ApiSpec) Convert2AccessibleApi() apistructs.AccessibleAPI {
	return apistructs.AccessibleAPI{
		Path:   api.Path,
		Method: api.Method,
		Schema: api.Scheme,
	}
}

// AddOperationTo generates self as an *openapi3.Operation and add it to the *openapi3.Swagger
func (api *ApiSpec) AddOperationTo(v3 *openapi3.Swagger) error {
	if v3 == nil {
		return errors.New("invalid swagger v3 object")
	}
	api.v3 = v3
	if _, err := api.generateOperation(); err != nil {
		return errors.Wrap(err, "failed to get operation from api spec")
	}
	api.v3.AddOperation(api.Parameters.path, strings.ToUpper(api.Method), api.operation)
	api.pathParametersToPathItem()
	return nil
}

// itoOpenapiSchema generates an interface
func (api *ApiSpec) itoOpenapiSchema(i interface{}) (schemaRef *openapi3.SchemaRef, err error) {
	if i == nil {
		ref := refPrefix + emptyObjectName
		api.v3.Components.Schemas[emptyObjectName] = emptyObject
		return openapi3.NewSchemaRef(ref, nil), nil
	}

	switch i.(type) {
	case time.Time, *time.Time:
		return openapi3.NewSchemaRef("", openapi3.NewDateTimeSchema()), nil
	}

	switch reflect.ValueOf(i).Type().Kind() {
	case reflect.Bool:
		return openapi3.NewSchemaRef("", openapi3.NewBoolSchema()), nil

	case reflect.Int, reflect.Int8, reflect.Int16, reflect.Int32, reflect.Int64,
		reflect.Uint, reflect.Uint8, reflect.Uint16, reflect.Uint32, reflect.Uint64, reflect.Uintptr:
		return openapi3.NewSchemaRef("", openapi3.NewIntegerSchema()), nil

	case reflect.Float32, reflect.Float64, reflect.Complex64, reflect.Complex128:
		return openapi3.NewSchemaRef("", openapi3.NewFloat64Schema()), nil

	case reflect.Array, reflect.Slice:
		schema := openapi3.NewArraySchema()
		eleSchemaRef, err := api.itoOpenapiSchema(reflect.New(reflect.ValueOf(i).Type().Elem()).Elem().Interface())
		if err != nil {
			return nil, err
		}
		schema.Items = eleSchemaRef
		return openapi3.NewSchemaRef("", schema), nil

	case reflect.Ptr:
		if vo := reflect.ValueOf(i); !vo.IsValid() || vo.IsZero() {
			ref := refPrefix + emptyObjectName
			api.v3.Components.Schemas[emptyObjectName] = emptyObject
			return openapi3.NewSchemaRef(ref, nil), nil
		}
		return api.itoOpenapiSchema(reflect.ValueOf(i).Elem().Interface())

	case reflect.String:
		return openapi3.NewSchemaRef("", openapi3.NewStringSchema()), nil

	case reflect.Struct:
		typeOf := reflect.TypeOf(i)
		valueOf := reflect.ValueOf(i)
		schema := openapi3.NewObjectSchema()
		typeName := typeOf.Name()
		ref := refPrefix + typeName

		schemeRef, ok := api.v3.Components.Schemas[ref]
		if ok {
			return schemeRef, nil
		}

		for j := 0; j < typeOf.NumField(); j++ {
			if n := typeOf.Field(j).Name[0]; ('a' <= n && n <= 'z') || n == '_' {
				continue
			}
			field := valueOf.Field(j)
			tag := typeOf.Field(j).Tag.Get("json")
			tag = strings.Split(tag, ",")[0]
			if tag == "-" {
				continue
			}
			if tag == "" {
				tag = typeOf.Field(j).Name
			}
			if !field.IsValid() {
				ref := refPrefix + emptyObjectName
				api.v3.Components.Schemas[emptyObjectName] = emptyObject
				schema.Properties[tag] = openapi3.NewSchemaRef(ref, nil)
				continue
			}

			propertySchemaRef, err := api.itoOpenapiSchema(field.Interface())
			if err != nil {
				fmt.Printf("itoOpenapiSchema error: %v", err)
				continue
			}
			schema.Properties[tag] = propertySchemaRef
		}

		api.v3.Components.Schemas[typeName] = openapi3.NewSchemaRef("", schema)
		return openapi3.NewSchemaRef(ref, nil), nil

	default:
		ref := refPrefix + emptyObjectName
		api.v3.Components.Schemas[emptyObjectName] = emptyObject
		return openapi3.NewSchemaRef(ref, nil), nil
	}
}

func (api *ApiSpec) generateOperation() (*openapi3.Operation, error) {
	if api.Scheme == "" {
		api.Scheme = "http"
	}

	if !api.IsValidForOperation() {
		return nil, errors.New("the ApiSpec is invalid to describe an operation")
	}

	if api.Parameters == nil {
		api.Parameters = &Parameters{
			Tag:         api.Group,
			Header:      nil,
			QueryValues: nil,
			Body:        api.RequestType,
			Response:    api.ResponseType,
			method:      api.Method,
			path:        api.Path,
		}
	}

	return api.operationByParameters()
}

func (api *ApiSpec) operationByParameters() (*openapi3.Operation, error) {
	api.Parameters.method = strings.ToLower(api.Method)
	api.Parameters.Tag = api.Group
	api.standardizeAPIPathPlaceholder()

	// make the new operation
	api.newOperation()

	// process doc and summary
	api.docToOperation()

	// process tag
	api.tagToOperation()

	// process parameters in the header
	api.headerParametersToOperation()

	// process parameters in the query
	api.queryParametersToOperation()

	// process request body
	api.requestBodyToOperation()

	// process response
	api.responseToOperation()

	return api.operation, nil
}

func (api *ApiSpec) standardizeAPIPathPlaceholder() {
	if api.Parameters == nil {
		return
	}
	api.Parameters.path = strings.ReplaceAll(api.Path, "<", "{")
	api.Parameters.path = strings.ReplaceAll(api.Parameters.path, ">", "}")
}

func (api *ApiSpec) IsValidForOperation() bool {
	if api == nil {
		return false
	}
	if !strings.HasPrefix(api.Path, "/") {
		return false
	}
	switch strings.ToUpper(api.Method) {
	case http.MethodGet,
		http.MethodHead,
		http.MethodPost,
		http.MethodPut,
		http.MethodPatch,
		http.MethodDelete,
		http.MethodConnect,
		http.MethodOptions,
		http.MethodTrace:
	default:
		return false
	}
	if !strings.EqualFold(api.Scheme, "http") && !strings.EqualFold(api.Scheme, "https") {
		return false
	}
	return true
}

func (api *ApiSpec) headerParametersToOperation() {
	for key, v := range api.Parameters.Header {
		var description = strings.Join(v, "\n")
		parameter := newParameter(key, "header", description)
		api.operation.Parameters = append(api.operation.Parameters, &openapi3.ParameterRef{Value: &parameter})
	}
}

func (api *ApiSpec) pathParametersToPathItem() {
	pathItem := api.v3.Paths.Find(api.Parameters.path)
	if pathItem == nil {
		return
	}

	re := regexp.MustCompile(`{[^/]*}`)
	placeholders := re.FindAllString(api.Parameters.path, -1)
	for _, placeholder := range placeholders {
		paramName := strings.TrimSuffix(strings.TrimPrefix(placeholder, "{"), "}")
		parameter := newParameter(paramName, "path", "")
		pathItem.Parameters = append(pathItem.Parameters, &openapi3.ParameterRef{Value: &parameter})
	}
	api.deDupPathParameters(pathItem)
}

func (api *ApiSpec) queryParametersToOperation() {
	for key, v := range api.Parameters.QueryValues {
		description := strings.Join(v, "\n")
		parameter := newParameter(key, "query", description)
		api.operation.Parameters = append(api.operation.Parameters, &openapi3.ParameterRef{Value: &parameter})
	}
}

func (api *ApiSpec) docToOperation() {
	api.operation.Summary = strings.Split(strings.TrimSpace(api.Doc), "\n")[0]
	api.operation.Description = api.Doc
}

func (api *ApiSpec) tagToOperation() {
	if api.Parameters.Tag == "" {
		api.Parameters.Tag = "default"
	}
	api.operation.Tags = []string{api.Parameters.Tag}

	defer sort.Slice(api.v3.Tags, func(i, j int) bool {
		return api.v3.Tags[i].Name < api.v3.Tags[j].Name
	})

	for _, v := range api.v3.Tags {
		if v.Name == api.Parameters.Tag {
			return
		}
	}

	api.v3.Tags = append(api.v3.Tags, &openapi3.Tag{Name: api.Parameters.Tag})
}

func (api *ApiSpec) newOperation() {
	api.operation = openapi3.NewOperation()
	api.operation.RequestBody = newRequestBodyRef()
	api.operation.Responses = openapi3.Responses{"200": newResponseRef()}
}

func (api *ApiSpec) requestBodyToOperation() {
	switch strings.ToUpper(api.Method) {
	case http.MethodGet, http.MethodHead, http.MethodDelete:
		api.operation.RequestBody = nil
		return
	}

	schemaRef, err := api.itoOpenapiSchema(api.Parameters.Body)
	if err != nil {
		api.operation.RequestBody = nil
		return
	}

	api.operation.RequestBody.Value.Content["application/json"].Schema = schemaRef
}

func (api *ApiSpec) responseToOperation() {
	schemaRef, err := api.itoOpenapiSchema(api.Parameters.Response)
	if err != nil {
		api.operation.Responses["200"].Value.Content["application/json"].Schema.Value = &openapi3.Schema{Type: "object"}
		return
	}

	api.operation.Responses["200"].Value.Content["application/json"].Schema = schemaRef
}

func (api *ApiSpec) movePathParametersToPathItem() {
	for _, pathItem := range api.v3.Paths {
		for _, operation := range pathItem.Operations() {
			for i := len(operation.Parameters) - 1; i >= 0; i-- {
				if operation.Parameters[i].Value.In == "path" {
					pathItem.Parameters = append(pathItem.Parameters, operation.Parameters[i])
					operation.Parameters = append(operation.Parameters[:i], operation.Parameters[i+1:]...)
				}
			}
		}
	}
}

func (api *ApiSpec) deDupPathParameters(pathItem *openapi3.PathItem) {
	var (
		names      = make(map[string]*openapi3.ParameterRef)
		parameters []*openapi3.ParameterRef
	)
	for _, parameter := range pathItem.Parameters {
		if _, ok := names[parameter.Value.Name]; !ok {
			parameters = append(parameters, parameter)
			names[parameter.Value.Name] = parameter
		}
	}
	pathItem.Parameters = parameters
}

type Parameters struct {
	Tag         string
	Header      http.Header
	QueryValues url.Values
	Body        interface{}
	Response    interface{}

	method string
	path   string
}

func NewSwagger(title string) *openapi3.Swagger {
	return &openapi3.Swagger{
		ExtensionProps: openapi3.ExtensionProps{},
		OpenAPI:        "3.0.0",
		Components: openapi3.Components{
			Schemas: make(map[string]*openapi3.SchemaRef),
		},
		Info: &openapi3.Info{
			ExtensionProps: openapi3.ExtensionProps{},
			Title:          title,
			Description:    "",
			TermsOfService: "",
			Contact:        nil,
			License:        nil,
			Version:        "default",
		},
		Paths:        make(openapi3.Paths),
		Security:     nil,
		Servers:      nil,
		Tags:         nil,
		ExternalDocs: nil,
	}
}

func newRequestBodyRef() *openapi3.RequestBodyRef {
	return &openapi3.RequestBodyRef{
		Value: &openapi3.RequestBody{
			Description: defaultDesc,
			Content: map[string]*openapi3.MediaType{
				"application/json": {
					Schema: &openapi3.SchemaRef{
						Ref:   "",
						Value: nil,
					},
					Example:  nil,
					Examples: nil,
					Encoding: nil,
				},
			},
		},
	}
}

func newResponseRef() *openapi3.ResponseRef {
	description := defaultDesc
	return &openapi3.ResponseRef{
		Value: &openapi3.Response{
			Description: &description,
			Content: map[string]*openapi3.MediaType{
				"application/json": {
					Schema: &openapi3.SchemaRef{
						Ref:   "",
						Value: nil,
					},
				},
			},
		},
	}
}

func newParameter(param, in, description string) openapi3.Parameter {
	return openapi3.Parameter{
		ExtensionProps:  openapi3.ExtensionProps{},
		Name:            param,
		In:              in,
		Description:     description,
		Style:           "",
		Explode:         nil,
		AllowEmptyValue: false,
		AllowReserved:   false,
		Deprecated:      false,
		Required:        in == "path" || strings.Contains(description, "required"),
		Schema:          openapi3.NewSchemaRef("", openapi3.NewStringSchema()),
		Example:         nil,
		Examples:        nil,
		Content:         nil,
	}
}
