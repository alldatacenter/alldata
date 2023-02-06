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

package audit

import (
	"context"
	"math"
	"strconv"

	"github.com/recallsong/go-utils/conv"

	"github.com/erda-project/erda/pkg/common/apis"
)

type (
	// Option .
	Option  func(opts *options)
	options struct {
		getEntries func(ctx context.Context) (map[string]interface{}, error)
		entries    *entry
		getUserID  func(ctx context.Context) string
		getOrgID   func(ctx context.Context) int64
	}
	entry struct {
		key  string
		val  interface{}
		prev *entry
	}
)

func newOptions() *options {
	return &options{
		getUserID: func(ctx context.Context) string { return apis.GetUserID(ctx) },
		getOrgID: func(ctx context.Context) int64 {
			id, _ := apis.GetIntOrgID(ctx)
			return id
		},
	}
}

// special keys
const (
	UserIDKey      = "userId"
	OrgIDKey       = "orgId"
	OrgNameKey     = "orgName"
	ProjectIDKey   = "projectId"
	ProjectNameKey = "projectName"
	AppIDKey       = "appId"
	AppNameKey     = "appName"
)

// Entry .
func Entry(key string, val interface{}) Option {
	return func(opts *options) {
		opts.entries = &entry{
			key:  key,
			val:  val,
			prev: opts.entries,
		}
	}
}

// Entries .
func Entries(h func(ctx context.Context) (map[string]interface{}, error)) Option {
	return func(opts *options) {
		opts.getEntries = h
	}
}

// OrgID .
func OrgID(id interface{}) Option {
	return func(opts *options) {
		opts.getOrgID = func(ctx context.Context) int64 {
			if str, ok := id.(string); ok {
				v, _ := strconv.ParseInt(str, 10, 64)
				return v
			}
			return conv.ToInt64(id, 0)
		}
	}
}

// UserID .
func UserID(id interface{}) Option {
	return func(opts *options) {
		opts.getUserID = func(ctx context.Context) string {
			if str, ok := id.(string); ok {
				return str
			}
			id := conv.ToInt64(id, math.MinInt64)
			if id == math.MinInt64 {
				return ""
			}
			return strconv.FormatInt(id, 10)
		}
	}
}

type optionContextKey_ uint8

const optionContextKey optionContextKey_ = 0

type optionContextData struct {
	opts []Option
}

func withOptionDataContext(ctx context.Context, opts *optionContextData) context.Context {
	return context.WithValue(ctx, optionContextKey, opts)
}

// ContextOptions .
func ContextOptions(ctx context.Context, opts ...Option) {
	data, ok := ctx.Value(optionContextKey).(*optionContextData)
	if ok {
		data.opts = append(data.opts, opts...)
	}
}

// ContextEntry .
func ContextEntry(ctx context.Context, key string, val interface{}) {
	ContextOptions(ctx, Entry(key, val))
}

func ContextEntryMap(ctx context.Context, kv map[string]interface{}) {
	for k, v := range kv {
		ContextEntry(ctx, k, v)
	}
}
