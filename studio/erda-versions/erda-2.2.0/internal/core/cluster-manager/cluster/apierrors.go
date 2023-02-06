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

package cluster

import "github.com/erda-project/erda/pkg/http/httpserver/errorresp"

var (
	ErrPreCheckCluster = err("ErrPreCheckCluster", "auth failed")
	ErrCreateCluster   = err("ErrCreateCluster", "failed to create cluster")
	ErrUpdateCluster   = err("ErrUpdateCluster", "failed to update cluster")
	ErrPatchCluster    = err("ErrPatchCluster", "failed to patch cluster")
	ErrGetCluster      = err("ErrGetCluster", "failed to get cluster")
	ErrListCluster     = err("ErrListCluster", "failed to list cluster")
	ErrDeleteCluster   = err("ErrDeleteCluster", "failed to delete cluster")
	ErrGetClusterInfo  = err("ErrGetClusterInfo", "failed to get cluster info")
)

func err(template, defaultValue string) *errorresp.APIError {
	return errorresp.New(errorresp.WithTemplateMessage(template, defaultValue))
}
