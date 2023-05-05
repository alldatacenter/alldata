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
	"fmt"
	"hash/fnv"
	"net"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/dbclient"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	headerXFF                      = "X-Forwarded-For"
	cookieDiceMobilAppDistribution = "Dice-Mobil-App-Distribution" // 1, 2, 3, 4
	cookieSplitter                 = ", "
)

// GrayDistribution 根据用户身份进行和灰度设置进行灰度分发
func (i *PublishItem) GrayDistribution(w http.ResponseWriter, r *http.Request, publisherItem dbclient.PublishItem,
	distribution *apistructs.PublishItemDistributionData, mobileType apistructs.ResourceType, packageName string) error {

	// 获取已发布版本
	total, tmpVersions, err := i.db.GetPublicVersion(int64(publisherItem.ID), mobileType, packageName)
	if err != nil {
		return err
	}
	if total == 0 {
		return nil
	}
	versions, err := discriminateReleaseAndBeta(total, tmpVersions)
	if err != nil {
		return err
	}
	releasVersion := versions[0].ToApiData()
	distribution.Default = releasVersion
	// 线上没有beta版本时，无需灰度
	if total == 1 {
		return nil
	}
	betaVersion := versions[1].ToApiData()

	// 查询用户是否对当前发布内容已经是灰度
	cookie, err := r.Cookie(cookieDiceMobilAppDistribution)
	if err == nil {
		alreadyGrayItems := strutil.Split(cookie.Value, cookieSplitter, true)
		for _, itemID := range alreadyGrayItems {
			if itemID == fmt.Sprintf("%d", publisherItem.ID) {
				// 当前用户已经是灰度
				handleGrayVersion(w, publisherItem.ID, distribution, releasVersion, betaVersion)
				return nil
			}
		}
	}

	// 根据 X-Forwarded-For 判断用户是否需要灰度
	ip := getRemoteIP(r)
	if ip == nil {
		return nil
	}

	// 满足灰度
	hashStr := ip.String() + publisherItem.Name
	if matchGrayMod(getHashedNum([]byte(hashStr)), 100, versions[1].GrayLevelPercent-1) {
		handleGrayVersion(w, publisherItem.ID, distribution, releasVersion, betaVersion)
		addGrayCookie(w, r, publisherItem.ID)
		return nil
	}

	return nil
}

// handleGrayVersion 返回灰度版本
func handleGrayVersion(w http.ResponseWriter, publisherItemID uint64, distribution *apistructs.PublishItemDistributionData,
	releaeVersion, betaVersion *apistructs.PublishItemVersion) {

	// 设置默认版本为灰度版本
	distribution.Default = betaVersion

	// 重新处理版本列表
	// 1. 添加默认版本
	// 2. 添加非灰度版本
	newVersionList := []*apistructs.PublishItemVersion{betaVersion}
	for _, v := range distribution.Versions.List {
		if v.ID != betaVersion.ID {
			newVersionList = append(newVersionList, v)
		}
	}
	distribution.Versions.List = newVersionList
}

func addGrayCookie(w http.ResponseWriter, r *http.Request, publisherID uint64) {
	cookie, err := r.Cookie(cookieDiceMobilAppDistribution)
	var grayItemIDs []string
	if err == nil {
		grayItemIDs = strutil.Split(cookie.Value, cookieSplitter, true)
		// 若 value 异常，则全部清除
		for _, id := range grayItemIDs {
			if _, err := strconv.ParseInt(id, 10, 64); err != nil {
				grayItemIDs = nil
				break
			}
		}
	}
	grayItemIDs = strutil.DedupSlice(append(grayItemIDs, fmt.Sprintf("%d", publisherID)))
	expire := time.Now().AddDate(0, 1, 0)
	newCookie := http.Cookie{
		Name:    cookieDiceMobilAppDistribution,
		Value:   strutil.Join(grayItemIDs, cookieSplitter, true),
		Path:    "/",
		Domain:  r.Host,
		Expires: expire,
	}
	http.SetCookie(w, &newCookie)
}

// getGrayVersion 查找灰度版本：default=false 中创建时间最新的版本
func getGrayVersion(versionData *apistructs.QueryPublishItemVersionData) *apistructs.PublishItemVersion {
	if versionData == nil || versionData.List == nil {
		return nil
	}
	var grayVersion *apistructs.PublishItemVersion
	for _, v := range versionData.List {
		if v.IsDefault {
			continue
		}
		if grayVersion == nil {
			grayVersion = v
			continue
		}
		if v.CreatedAt.After(grayVersion.CreatedAt) {
			grayVersion = v
		}
	}
	return grayVersion
}

func getHashedNum(b []byte) int {
	h := fnv.New32a()
	h.Write(b)
	return int(h.Sum32())
}

func matchGrayMod(hashedNum, mod, zeroToWhich int) bool {
	return hashedNum%mod <= zeroToWhich
}

// getRemoteIP get ip from XFF header
// example: 122.235.82.217, 100.122.56.227, 9.117.157.128, 101.37.145.101, 10.118.183.0
func getRemoteIP(r *http.Request) net.IP {
	// 1.1.1.1, 2,2,2,2
	ips := strings.SplitN(r.Header.Get(headerXFF), ",", 2)
	if len(ips) == 0 {
		return nil
	}
	return net.ParseIP(ips[0])
}
