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

package runtime

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
)

type bundleServiceImpl struct {
	bdl *bundle.Bundle
}

func (b *bundleServiceImpl) GetApp(id uint64) (*apistructs.ApplicationDTO, error) {
	return b.bdl.GetApp(id)
}

func (b *bundleServiceImpl) InspectServiceGroupWithTimeout(namespace string, name string) (*apistructs.ServiceGroup, error) {
	return b.bdl.InspectServiceGroupWithTimeout(namespace, name)
}

func (b *bundleServiceImpl) GetCluster(name string) (*apistructs.ClusterInfo, error) {
	return b.bdl.GetCluster(name)
}

func (b *bundleServiceImpl) CheckPermission(req *apistructs.PermissionCheckRequest) (*apistructs.PermissionCheckResponseData, error) {
	return b.bdl.CheckPermission(req)
}

func NewBundleService() BundleService {
	return &bundleServiceImpl{
		bdl: bundle.New(
			bundle.WithCoreServices(),
			bundle.WithClusterManager(),
			bundle.WithScheduler(),
		),
	}
}
