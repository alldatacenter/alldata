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

package addon

import "github.com/sirupsen/logrus"

// CleanRemainingAddonAttachment clean remain addon attachment
// Deleting the addon before the addon is created will cause the addon to leak
// Deleting the runtime during deployment will cause this to happen
func (a *Addon) CleanRemainingAddonAttachment() (bool, error) {
	attachments, err := a.db.ListAttachmentIDRuntimeIDNotExist()
	if err != nil {
		logrus.Errorf("failed to ListAttachmentIDRuntimeIDNotExist, err: %v", err)
		return false, err
	}
	if len(attachments) == 0 {
		return false, nil
	}
	ids := make([]uint64, 0, len(attachments))
	for _, v := range attachments {
		ids = append(ids, v.ID)
	}

	logrus.Infof("begin delete %d addon attachments, ids: %v", len(ids), ids)
	if err = a.db.DeleteAttachmentByIDs(ids...); err != nil {
		logrus.Errorf("failed to DeleteAttachmentByIDs, err: %v", err)
		return false, err
	}
	return false, nil
}
