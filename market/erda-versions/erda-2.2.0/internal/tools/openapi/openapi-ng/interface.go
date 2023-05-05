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

	transhttp "github.com/erda-project/erda-infra/pkg/transport/http"
)

// Interface .
type Interface interface {
	transhttp.Router
	WrapHandler(handler transhttp.HandlerFunc) transhttp.HandlerFunc
}

// RouteSource .
type RouteSource interface {
	Name() string
	RegisterTo(r transhttp.Router) error
}

// RouteSourceWatcher .
type RouteSourceWatcher interface {
	Watch(ctx context.Context) <-chan RouteSource
}
