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

package proto

import (
	"net/http"
	"reflect"

	common "github.com/erda-project/erda-proto-go/common/pb"
	"github.com/erda-project/erda/internal/tools/openapi/openapi-ng/auth"
)

// GetAuthOption .
func GetAuthOption(opt *common.APIAuth, fn func(opts map[string]interface{}) map[string]interface{}) func(r *http.Request) auth.Options {
	if opt == nil {
		return func(r *http.Request) auth.Options {
			return &authOption{}
		}
	}
	opts := fieldsToMap(reflect.ValueOf(opt).Elem())
	if fn != nil {
		opts = fn(opts)
	}
	return func(r *http.Request) auth.Options {
		return &authOption{opts: opts}
	}
}

func fieldsToMap(val reflect.Value) map[string]interface{} {
	typ := val.Type()
	n := typ.NumField()
	fields := make(map[string]interface{})
	for i := 0; i < n; i++ {
		v := val.Field(i)
		if v.CanInterface() {
			field := typ.Field(i)
			fields[field.Name] = v.Interface()
		}
	}
	return fields
}

type authOption struct {
	opts  map[string]interface{}
	other map[string]interface{}
}

func (o *authOption) Get(key string) interface{} {
	if val, ok := o.opts[key]; ok {
		return val
	}
	return o.other[key]
}

func (o *authOption) Set(key string, val interface{}) {
	if o.other == nil {
		o.other = make(map[string]interface{})
	}
	o.other[key] = val
}
