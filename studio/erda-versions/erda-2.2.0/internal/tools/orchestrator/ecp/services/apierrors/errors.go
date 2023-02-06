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

package apierrors

import (
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
)

var (
	ErrCreateFoobar = err("ErrCreateFoobar", "failed to create foobar")
)

var (
	ErrGetAddonConfig    = err("ErrGetAddonConfig", "failed to get addon configuration")
	ErrGetAddonStatus    = err("ErrGetAddonStatus", "failed to get addon status")
	ErrUpdateAddonConfig = err("ErrUpdateAddonConfig", "failed to update addon configuration")
	ErrCreateCluster     = err("ErrCreateCluster", "failed to create cluster")
)

var (
	ErrListEdgeSite         = err("ErrListEdgeSite", "failed to list edge site")
	ErrGetEdgeSite          = err("ErrGetEdgeSite", "failed to get edge site")
	ErrCreateEdgeSite       = err("ErrCreateEdgeSite", "failed to create edge site")
	ErrUpdateEdgeSite       = err("ErrUpdateEdgeSite", "failed to update edge site")
	ErrDeleteEdgeSite       = err("ErrDeleteEdgeSite", "failed to delete edge site")
	ErrGetEdgeSiteInit      = err("ErrGetEdgeSiteInit", "failed to get edge site init shell")
	ErrOfflineEdgeSite      = err("ErrOfflineEdgeSite", "failed to offline edge site")
	ErrListEdgeConfigSet    = err("ErrListEdgeConfigSet", "failed to list configSet")
	ErrCreateEdgeConfigSet  = err("ErrCreateEdgeConfigSet", "failed to create edge configSet")
	ErrUpdateEdgeConfigSet  = err("ErrUpdateEdgeConfigSet", "failed to update edge configSet")
	ErrDeleteEdgeConfigSet  = err("ErrDeleteEdgeConfigSet", "failed to delete edge configSet")
	ErrListEdgeCfgSetItem   = err("ErrListEdgeCfgSetItem", "failed to list configSet")
	ErrCreateEdgeCfgSetItem = err("ErrCreateEdgeCfgSetItem", "failed to create edge configSet item")
	ErrUpdateEdgeCfgSetItem = err("ErrUpdateEdgeCfgSetItem", "failed to update edge configSet item")
	ErrDeleteEdgeCfgSetItem = err("ErrDeleteEdgeCfgSetItem", "failed to delete edge configSet item")
	ErrListEdgeApp          = err("ErrListEdgeSite", "failed to list edge app")
	ErrCreateEdgeApp        = err("ErrCreateEdgeApp", "failed to create edge app")
	ErrUpdateEdgeApp        = err("ErrUpdateEdgeSite", "failed to update edge app")
	ErrDeleteEdgeApp        = err("ErrDeleteEdgeSite", "failed to delete edge app")
	ErrRestartEdgeApp       = err("ErrRestartEdgeApp", "failed to restart edge app")
	ErrOfflineEdgeAppSite   = err("ErrOfflineEdgeAppSite", "failed to offline specified site in edge app ")
	AccessDeny              = err("ErrAccessDeny", "permission denied")
)

func err(template, defaultValue string) *errorresp.APIError {
	return errorresp.New(errorresp.WithTemplateMessage(template, defaultValue))
}
