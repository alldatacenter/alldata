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

package steve

import (
	"net/http"
	"time"

	"github.com/rancher/apiserver/pkg/types"
	"github.com/sirupsen/logrus"
)

type Response struct {
	StatusCode   int
	ResponseData interface{}
}

func (rw *Response) Write(apiOp *types.APIRequest, code int, obj types.APIObject) {
	rw.StatusCode = code
	rw.ResponseData = convert(apiOp, obj)
}

func (rw *Response) WriteList(apiOp *types.APIRequest, code int, obj types.APIObjectList) {
	rw.StatusCode = code
	logrus.Infof("[DEBUG %s] start convert list at %s", apiOp.Type, time.Now().Format(time.StampNano))
	rw.ResponseData = convertList(apiOp, obj)
	logrus.Infof("[DEBUG %s] end convert list at %s", apiOp.Type, time.Now().Format(time.StampNano))
}

func newCollection(apiOp *types.APIRequest, list types.APIObjectList) *types.GenericCollection {
	result := &types.GenericCollection{
		Collection: types.Collection{
			Type:         "collection",
			ResourceType: apiOp.Type,
			CreateTypes:  map[string]string{},
			Links: map[string]string{
				"self": apiOp.URLBuilder.Current(),
			},
			Actions:  map[string]string{},
			Continue: list.Continue,
			Revision: list.Revision,
		},
	}

	if apiOp.Method == http.MethodGet {
		if apiOp.AccessControl.CanCreate(apiOp, apiOp.Schema) == nil {
			result.CreateTypes[apiOp.Schema.ID] = apiOp.URLBuilder.Collection(apiOp.Schema)
		}
	}

	return result
}

func convertList(apiOp *types.APIRequest, input types.APIObjectList) *types.GenericCollection {
	collection := newCollection(apiOp, input)
	for _, value := range input.Objects {
		converted := convert(apiOp, value)
		collection.Data = append(collection.Data, converted)
	}

	if apiOp.Schema.CollectionFormatter != nil {
		apiOp.Schema.CollectionFormatter(apiOp, collection)
	}

	if collection.Data == nil {
		collection.Data = []*types.RawResource{}
	}

	return collection
}

func convert(context *types.APIRequest, input types.APIObject) *types.RawResource {
	schema := context.Schemas.LookupSchema(input.Type)
	if schema == nil {
		schema = context.Schema
	}
	if schema == nil {
		return nil
	}

	rawResource := &types.RawResource{
		ID:          input.ID,
		Type:        schema.ID,
		Schema:      schema,
		Links:       map[string]string{},
		Actions:     map[string]string{},
		ActionLinks: context.Request.Header.Get("X-API-Action-Links") != "",
		APIObject:   input,
	}

	if schema.Formatter != nil {
		schema.Formatter(context, rawResource)
	}

	return rawResource
}

type StatusCodeGetter struct {
	Response *Response
}

func (scg *StatusCodeGetter) Header() http.Header {
	header := make(map[string][]string)
	return header
}

func (scg *StatusCodeGetter) Write([]byte) (int, error) {
	return 0, nil
}

func (scg *StatusCodeGetter) WriteHeader(code int) {
	scg.Response.StatusCode = code
}
