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

// Package registry docker registry manifest操作
package registry

import (
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/pkg/registryhelper"
)

// DeleteManifests deletes manifests from the cluster inner image registry
func DeleteManifests(bdl *bundle.Bundle, clusterName string, images []string) (err error) {
	var l = logrus.WithField("func", "DeleteManifests").
		WithField("clusterName", clusterName).
		WithField("images", images)
	if len(images) == 0 {
		return nil
	}
	req := registryhelper.RemoveManifestsRequest{
		Images:     images,
		ClusterKey: clusterName,
	}
	if req.RegistryURL, err = bdl.GetRegistryAddress(clusterName); err != nil {
		l.WithError(err).Errorln("failed to GetRegistryAddress")
		return errors.Wrap(err, "failed to GetRegistryAddress")
	}
	removeResp, err := registryhelper.RemoveManifests(req)
	if err != nil {
		return err
	}
	if len(removeResp.Failed) > 0 {
		return errors.Errorf("recycle image fail: %+v", removeResp.Failed)
	}
	return nil
}
