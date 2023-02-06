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

package publish_item

import (
	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/dbclient"
	"github.com/erda-project/erda/pkg/i18n"
)

// publicReleaseVersion 上架正式版
func (i *PublishItem) publicReleaseVersion(total int, versions []dbclient.PublishItemVersion,
	req apistructs.UpdatePublishItemVersionStatesRequset, local *i18n.LocaleResource) error {
	// 无发布版本，上架正式版
	if total == 0 {
		if err := i.db.UpdatePublicVersionByID(req.PublishItemVersionID, map[string]interface{}{
			"version_states": req.VersionStates, "gray_level_percent": 100, "public": true}); err != nil {
			return err
		}

		return nil
	}

	// 只有正式版，再次上架正式版
	if total == 1 {
		return errors.Errorf(local.Get("dicehub.publish.err.alreadyhaverelaseversion")+": %v", versions[0].Version)
	}

	// 已有内测版和正式版
	if total == 2 {
		// 修改线上正式版的灰度值
		if req.PublishItemVersionID == int64(versions[0].ID) {
			if err := i.db.UpdatePublicVersionByID(req.PublishItemVersionID, map[string]interface{}{
				"gray_level_percent": req.GrayLevelPercent}); err != nil {
				return err
			}

			if err := i.db.UpdatePublicVersionByID(int64(versions[1].ID), map[string]interface{}{
				"gray_level_percent": 100 - req.GrayLevelPercent}); err != nil {
				return err
			}

			return nil
		}

		// 内测版转正
		if req.PublishItemVersionID == int64(versions[1].ID) {
			if err := i.db.UpdatePublicVersionByID(int64(versions[0].ID), map[string]interface{}{
				"version_states": "", "gray_level_percent": 0, "public": false}); err != nil {
				return err
			}
			// 内测版转正
			if err := i.db.UpdatePublicVersionByID(req.PublishItemVersionID, map[string]interface{}{
				"version_states": req.VersionStates, "gray_level_percent": 100, "public": true}); err != nil {
				return err
			}

			return nil
		}

		return errors.Errorf(local.Get("dicehub.publish.err.alreadyhaverelaseversion")+": %v", versions[0].Version)
	}

	return nil
}

// unPublicReleaseVersion 下架正式版
func (i *PublishItem) unPublicReleaseVersion(total int, versions []dbclient.PublishItemVersion,
	req apistructs.UpdatePublishItemVersionStatesRequset, local *i18n.LocaleResource) error {
	// 只有正式版，下架正式版
	if total == 1 {
		if req.PublishItemVersionID == int64(versions[0].ID) {
			if err := i.db.UpdatePublicVersionByID(req.PublishItemVersionID, map[string]interface{}{
				"version_states": "", "gray_level_percent": 0, "public": false}); err != nil {
				return err
			}
		}
	}

	// 已有正式版和内测版，下架正式版
	if total == 2 {
		return errors.Errorf(local.Get("dicehub.publish.err.musthaveareleaseverison"))
	}

	return nil
}

// publicBetaVersion 上架beta版本
func (i *PublishItem) publicBetaVersion(total int, versions []dbclient.PublishItemVersion,
	req apistructs.UpdatePublishItemVersionStatesRequset, local *i18n.LocaleResource) error {
	// 无发布版本，上架beta版
	if total == 0 {
		return errors.New(local.Get("dicehub.publish.err.noreleaseversiononline"))
	}

	// 已有正式版，上架beta版
	if total == 1 {
		if req.PublishItemVersionID == int64(versions[0].ID) {
			return errors.New(local.Get("dicehub.publish.err.musthaveareleaseverison"))
		}

		if err := i.db.UpdatePublicVersionByID(req.PublishItemVersionID, map[string]interface{}{
			"version_states": req.VersionStates, "gray_level_percent": req.GrayLevelPercent, "public": true}); err != nil {
			return err
		}

		if err := i.db.UpdatePublicVersionByID(int64(versions[0].ID), map[string]interface{}{
			"gray_level_percent": 100 - req.GrayLevelPercent}); err != nil {
			return err
		}

		return nil
	}

	// 已有正式版和beta版
	if total == 2 {
		// 线上release版本变beta版
		if req.PublishItemVersionID == int64(versions[0].ID) {
			return errors.New(local.Get("dicehub.publish.err.musthaveareleaseverison"))
		}

		// 修改线上beta版的灰度值
		if req.PublishItemVersionID == int64(versions[1].ID) {
			if err := i.db.UpdatePublicVersionByID(req.PublishItemVersionID, map[string]interface{}{
				"gray_level_percent": req.GrayLevelPercent}); err != nil {
				return err
			}

			if err := i.db.UpdatePublicVersionByID(int64(versions[0].ID), map[string]interface{}{
				"gray_level_percent": 100 - req.GrayLevelPercent}); err != nil {
				return err
			}

			return nil
		}

		return errors.Errorf(local.Get("dicehub.publish.err.alreadyhavebetaversion")+": %v", versions[1].Version)
	}

	return nil
}

// unPublicBetaVersion 下架beta版本
func (i *PublishItem) unPublicBetaVersion(total int, versions []dbclient.PublishItemVersion,
	req apistructs.UpdatePublishItemVersionStatesRequset, local *i18n.LocaleResource) error {
	// 已有正式版和内测版，下架内测版
	if total == 2 {
		if req.PublishItemVersionID == int64(versions[1].ID) {
			if err := i.db.UpdatePublicVersionByID(req.PublishItemVersionID, map[string]interface{}{
				"version_states": "", "gray_level_percent": 0, "public": false}); err != nil {
				return err
			}

			if err := i.db.UpdatePublicVersionByID(int64(versions[0].ID), map[string]interface{}{
				"gray_level_percent": 100}); err != nil {
				return err
			}

			return nil
		}
	}

	return nil
}

func discriminateReleaseAndBeta(l int, versions []dbclient.PublishItemVersion) ([]dbclient.PublishItemVersion, error) {
	result := make([]dbclient.PublishItemVersion, 2)
	if l > 2 {
		return nil, errors.Errorf("public version is over than 3")
	}

	if l == 0 {
		return nil, nil
	}

	if l == 1 {
		// 上架版本只有一个版本，为beta版本
		if versions[0].VersionStates == apistructs.PublishItemBetaVersion {
			return nil, errors.Errorf("data error: only have one beta version %v", versions[0].Version)
		}

		return versions, nil
	}

	// 线上两个版本一样
	if l == 2 && versions[0].VersionStates == versions[1].VersionStates {
		return nil, errors.Errorf("public version have 2 same version")
	}

	// result[0] -- 正式版 result[1] -- 预览版
	if versions[0].VersionStates == apistructs.PublishItemReleaseVersion {
		result[0], result[1] = versions[0], versions[1]
	} else {
		result[0], result[1] = versions[1], versions[0]
	}

	return result, nil
}
