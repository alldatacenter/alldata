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

	"github.com/rancher/apiserver/pkg/types"
	"github.com/rancher/apiserver/pkg/urlbuilder"
)

func NewPrefixed(r *http.Request, schemas *types.APISchemas, prefix string) (types.URLBuilder, error) {
	requestURL := urlbuilder.ParseRequestURL(r)
	responseURLBase, err := urlbuilder.ParseResponseURLBase(requestURL, r)
	if err != nil {
		return nil, err
	}

	builder, err := urlbuilder.New(r, &urlbuilder.DefaultPathResolver{Prefix: prefix + "/v1"}, schemas)
	if err != nil {
		return nil, err
	}

	prefixedBuilder := &PrefixedURLBuilder{
		URLBuilder: builder,
		prefix:     prefix,
		base:       responseURLBase,
	}
	return prefixedBuilder, nil
}

type PrefixedURLBuilder struct {
	types.URLBuilder

	prefix string
	base   string
}

func (u *PrefixedURLBuilder) RelativeToRoot(path string) string {
	return urlbuilder.ConstructBasicURL(u.base, u.prefix, path)
}
