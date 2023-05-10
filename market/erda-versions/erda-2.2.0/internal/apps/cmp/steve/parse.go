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
	"strings"

	"github.com/rancher/apiserver/pkg/parse"
	"github.com/rancher/apiserver/pkg/types"
	"github.com/rancher/apiserver/pkg/urlbuilder"
)

var (
	allowedFormats = map[string]bool{
		"html": true,
		"json": true,
		"yaml": true,
	}
)

func Parse(apiOp *types.APIRequest, urlParser parse.URLParser) error {
	var err error

	if apiOp.Request == nil {
		apiOp.Request, err = http.NewRequest("GET", "/", nil)
		if err != nil {
			return err
		}
	}

	apiOp = types.StoreAPIContext(apiOp)

	if apiOp.Method == "" {
		apiOp.Method = parseMethod(apiOp.Request)
	}
	if apiOp.ResponseFormat == "" {
		apiOp.ResponseFormat = parseResponseFormat(apiOp.Request)
	}

	// The response format is guaranteed to be set even in the event of an error
	parsedURL, err := urlParser(apiOp.Response, apiOp.Request, apiOp.Schemas)
	// wait to check error, want to set as much as possible

	if apiOp.Type == "" {
		apiOp.Type = parsedURL.Type
	}
	if apiOp.Name == "" {
		apiOp.Name = parsedURL.Name
	}
	if apiOp.Link == "" {
		apiOp.Link = parsedURL.Link
	}
	if apiOp.Action == "" {
		apiOp.Action = parsedURL.Action
	}
	if apiOp.Query == nil {
		apiOp.Query = parsedURL.Query
	}
	if apiOp.Method == "" && parsedURL.Method != "" {
		apiOp.Method = parsedURL.Method
	}
	if apiOp.URLPrefix == "" {
		apiOp.URLPrefix = parsedURL.Prefix
	}
	if apiOp.Namespace == "" {
		apiOp.Namespace = parsedURL.Namespace
	}

	if apiOp.URLBuilder == nil {
		// make error local to not override the outer error we have yet to check
		var err error
		apiOp.URLBuilder, err = urlbuilder.New(apiOp.Request, &urlbuilder.DefaultPathResolver{
			Prefix: apiOp.URLPrefix,
		}, apiOp.Schemas)
		if err != nil {
			return err
		}
	}

	if err != nil {
		return err
	}

	if apiOp.Schema == nil && apiOp.Schemas != nil {
		apiOp.Schema = apiOp.Schemas.LookupSchema(apiOp.Type)
	}

	if apiOp.Schema != nil {
		apiOp.Type = apiOp.Schema.ID
	}

	if apiOp.Schema != nil && apiOp.ErrorHandler != nil {
		apiOp.ErrorHandler = apiOp.Schema.ErrorHandler
	}

	return nil
}

func parseMethod(req *http.Request) string {
	method := req.URL.Query().Get("_method")
	if method == "" {
		method = req.Method
	}
	return method
}

func parseResponseFormat(req *http.Request) string {
	format := req.URL.Query().Get("_format")

	if format != "" {
		format = strings.TrimSpace(strings.ToLower(format))
	}

	/* Format specified */
	if allowedFormats[format] {
		return format
	}

	// User agent has Mozilla and browser accepts */*
	if parse.IsBrowser(req, true) {
		return "html"
	}

	if isYaml(req) {
		return "yaml"
	}
	return "json"
}

func isYaml(req *http.Request) bool {
	return strings.Contains(req.Header.Get("Accept"), "application/yaml")
}
