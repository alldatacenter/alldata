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

package release

import (
	"strings"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/conf"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/dbclient"
	imagedb "github.com/erda-project/erda/internal/apps/dop/dicehub/image/db"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/registry"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	// AliYunRegistry 阿里云registry前缀
	AliYunRegistry = "registry.cn-hangzhou.aliyuncs.com"
)

// Release Release操作封装
type Release struct {
	db      *dbclient.DBClient
	bdl     *bundle.Bundle
	imageDB *imagedb.ImageConfigDB
}

// Option 定义 Release 对象的配置选项
type Option func(*Release)

// New 新建 Release 实例，操作 Release 资源
func New(options ...Option) *Release {
	app := &Release{}
	for _, op := range options {
		op(app)
	}
	return app
}

// WithDBClient 配置 db client
func WithDBClient(db *dbclient.DBClient) Option {
	return func(a *Release) {
		a.db = db
	}
}

// WithDBClient 配置 db client
func WithImageDBClient(db *imagedb.ImageConfigDB) Option {
	return func(a *Release) {
		a.imageDB = db
	}
}

// WithBundle 配置 bundle
func WithBundle(bdl *bundle.Bundle) Option {
	return func(a *Release) {
		a.bdl = bdl
	}
}

// GetDiceYAML 获取dice.yml内容
func (r *Release) GetDiceYAML(orgID int64, releaseID string) (string, error) {
	release, err := r.db.GetRelease(releaseID)
	if err != nil {
		return "", err
	}
	if orgID != 0 && release.OrgID != orgID { // when calling internally，orgID is 0
		return "", errors.Errorf("release not found")
	}

	return release.Dice, nil
}

// RemoveDeprecatedsReleases 回收过期release具体逻辑
func (r *Release) RemoveDeprecatedsReleases(now time.Time) error {
	d, err := time.ParseDuration(strutil.Concat("-", conf.MaxTimeReserved(), "h")) // one month before, eg: -720h
	if err != nil {
		return err
	}
	before := now.Add(d)

	releases, err := r.db.GetUnReferedReleasesBefore(before)
	if err != nil {
		return err
	}
	for i := range releases {
		release := releases[i]
		if release.Version != "" {
			logrus.Debugf("release %s have been tagged, can't be recycled", release.ReleaseID)
			continue
		}

		images, err := r.imageDB.GetImagesByRelease(release.ReleaseID)
		if err != nil {
			logrus.Warnf(err.Error())
			continue
		}

		deletable := true // 若release下的image manifest删除失败，release不可删除
		for _, image := range images {
			// 若有其他release引用此镜像，镜像manifest不可删，只删除DB元信息(多次构建，存在镜像相同的情况)
			count, err := r.imageDB.GetImageCount(release.ReleaseID, image.Image)
			if err != nil {
				logrus.Errorf(err.Error())
				continue
			}
			if count == 0 && release.ClusterName != "" && !strings.HasPrefix(image.Image, AliYunRegistry) {
				if err := registry.DeleteManifests(r.bdl, release.ClusterName, []string{image.Image}); err != nil {
					deletable = false
					logrus.Errorf(err.Error())
					continue
				}
			}

			// Delete image info
			if err := r.imageDB.DeleteImage(int64(image.ID)); err != nil {
				logrus.Errorf("[alert] delete image: %s fail, err: %v", image.Image, err)
			}
			logrus.Infof("deleted image: %s", image.Image)
		}

		if deletable {
			// Delete release info
			if err := r.db.DeleteRelease(release.ReleaseID); err != nil {
				logrus.Errorf("[alert] delete release: %s fail, err: %v", release.ReleaseID, err)
			}
			logrus.Infof("deleted release: %s", release.ReleaseID)
		}
	}
	return nil
}
