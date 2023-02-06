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
	"context"
	"crypto/tls"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"os"
	"path/filepath"
	"strconv"
	"time"

	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/dbclient"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/i18n"
)

// PublishItemVersion 创建发布版本
func (i *PublishItem) PublishItemVersion(req *apistructs.CreatePublishItemVersionRequest) (*apistructs.PublishItemVersion, error) {
	metaJson := ""
	logo := ""
	resource := ""
	if req.ReleaseID != "" {
		release, err := i.db.GetRelease(req.ReleaseID)
		if err != nil {
			return nil, err
		}

		meta := map[string]interface{}{}
		meta["appId"] = release.ApplicationID
		meta["appName"] = release.ApplicationName
		meta["projectId"] = release.ProjectID
		meta["projectName"] = release.ProjectName
		meta["orgId"] = release.OrgID
		meta["releaseId"] = release.ReleaseID
		metaBytes, err := json.Marshal(meta)

		if err == nil {
			metaJson = string(metaBytes)
		}
		logo = getLogo(release.Resources)
		resource = release.Resources
	} else if req.AppID != 0 {
		app, err := i.bdl.GetApp(req.AppID)
		if err != nil {
			return nil, err
		}
		meta := map[string]interface{}{}
		meta["appId"] = app.ID
		meta["appName"] = app.Name
		meta["projectId"] = app.ProjectID
		meta["projectName"] = app.ProjectName
		meta["orgId"] = app.OrgID
		metaBytes, err := json.Marshal(meta)

		if err == nil {
			metaJson = string(metaBytes)
		}

	}
	_, err := i.db.GetPublishItem(req.PublishItemID)
	if err != nil {
		return nil, err
	}

	versionInfo := apistructs.VersionInfo{
		PackageName: req.PackageName,
		Version:     req.Version,
		BuildID:     req.BuildID,
	}

	version, err := i.db.GetPublishItemVersionByName(req.OrgID, req.PublishItemID, req.MobileType, versionInfo)
	if err == nil {
		version.Public = req.Public
		version.BuildID = req.BuildID
		version.PackageName = req.PackageName
		version.Desc = req.Desc
		version.Resources = resource
		version.Meta = metaJson
		version.Creator = req.Creator
		version.IsDefault = req.IsDefault
		version.Spec = req.Spec
		version.Readme = req.Readme
		version.Logo = logo
		version.MobileType = string(req.MobileType)
		version.Swagger = req.Swagger
		err := i.db.UpdatePublishItemVersion(version)
		if err != nil {
			return nil, err
		}
		if version.IsDefault {
			i.db.SetPublishItemVersionDefault(version.PublishItemID, int64(version.ID))
		}
		return version.ToApiData(), nil
	}

	itemVersion := dbclient.PublishItemVersion{
		Version:       req.Version,
		BuildID:       req.BuildID,
		PackageName:   req.PackageName,
		Public:        req.Public,
		OrgID:         req.OrgID,
		Desc:          req.Desc,
		Logo:          logo,
		PublishItemID: req.PublishItemID,
		MobileType:    string(req.MobileType),
		IsDefault:     req.IsDefault,
		Resources:     resource,
		Creator:       req.Creator,
		Meta:          metaJson,
		Readme:        req.Readme,
		Spec:          req.Spec,
		Swagger:       req.Swagger,
	}
	err = i.db.CreatePublishItemVersion(&itemVersion)
	if err != nil {
		return nil, err
	}
	if itemVersion.IsDefault {
		i.db.SetPublishItemVersionDefault(itemVersion.PublishItemID, int64(itemVersion.ID))
	}

	// 创建H5的目标版本关系
	if req.MobileType == apistructs.ResourceTypeH5 {
		h5version, err := i.db.GetPublishItemVersionByName(req.OrgID, req.PublishItemID, req.MobileType, versionInfo)
		if err != nil {
			return nil, err
		}
		for k, v := range req.H5VersionInfo.TargetMobiles {
			for _, v1 := range v {
				if err := i.db.CreateH5Targets(&dbclient.PublishItemH5Targets{
					H5VersionID:      h5version.ID,
					TargetVersion:    v1,
					TargetMobileType: k,
				}); err != nil {
					return nil, err
				}
			}
		}
	}

	return itemVersion.ToApiData(), nil
}

type PKGInfo struct {
	PackageName string
	Version     string
	BuildID     string
	DisplayName string
	Logo        string
}

// CreateOffLineVersion 创建离线包版本
func (i *PublishItem) CreateOffLineVersion(param apistructs.CreateOffLinePublishItemVersionRequest) (string, error) {
	fileHeader := param.FileHeader
	// 校验publishItem是否存在
	_, err := i.db.GetPublishItem(param.PublishItemID)
	if err != nil {
		return "", err
	}

	var (
		resourceType apistructs.ResourceType
		meta         map[string]interface{}
		pkgInfo      *PKGInfo
		logoURL      string
	)

	// 校验文件后缀
	ext := filepath.Ext(fileHeader.Filename)
	if ext == ".apk" {
		resourceType = apistructs.ResourceTypeAndroid
	} else if ext == ".ipa" {
		resourceType = apistructs.ResourceTypeIOS
	} else {
		return "", errors.Errorf("unknow file type: %s", ext)
	}

	// 上传移动应用文件
	mobileFileUploadResult, err := i.UploadFileFromReader(fileHeader)
	if err != nil {
		return "", err
	}

	switch resourceType {
	case apistructs.ResourceTypeAndroid:
		info, err := GetAndoridInfo(fileHeader)
		if err != nil {
			return "", err
		}
		if info.Icon != nil {
			logoTmpPath := GenrateTmpImagePath(time.Now().String())
			if err := SaveImageToFile(info.Icon, logoTmpPath); err != nil {
				return "", errors.Errorf("error encode jpeg icon %s %v", logoTmpPath, err)
			}
			defer func() {
				if err := os.Remove(logoTmpPath); err != nil {
					logrus.Errorf("remove logoFile %s err: %v", logoTmpPath, err)
				}
			}()
			logoUploadResult, err := i.UploadFileFromFile(logoTmpPath)
			if err != nil {
				return "", err
			}
			logoURL = logoUploadResult.DownloadURL
		}
		versionCodeStr := strconv.FormatInt(int64(info.VersionCode), 10)
		pkgInfo = &PKGInfo{
			PackageName: info.PackageName,
			Version:     info.Version,
			BuildID:     versionCodeStr,
			DisplayName: info.Version,
			Logo:        logoURL,
		}
		meta = map[string]interface{}{"packageName": info.PackageName, "version": info.Version, "buildID": pkgInfo.BuildID,
			"displayName": info.Version, "logo": logoURL}
	case apistructs.ResourceTypeIOS:
		info, err := GetIosInfo(fileHeader)
		if err != nil {
			return "", err
		}
		if info.Icon != nil {
			logoTmpPath := GenrateTmpImagePath(time.Now().String())
			err = SaveImageToFile(info.Icon, logoTmpPath)
			if err != nil {
				return "", errors.Errorf("error encode jpeg icon %s %v", logoTmpPath, err)
			}
			defer func() {
				if err := os.Remove(logoTmpPath); err != nil {
					logrus.Errorf("remove logoFile %s err: %v", logoTmpPath, err)
				}
			}()
			logoUploadResult, err := i.UploadFileFromFile(logoTmpPath)
			if err != nil {
				return "", err
			}
			logoURL = logoUploadResult.DownloadURL
		}
		installPlistContent := GenerateInstallPlist(info, mobileFileUploadResult.DownloadURL)
		t := time.Now().Format("20060102150405")
		if err := os.Mkdir("/tmp/"+t, os.ModePerm); err != nil {
			return "", err
		}
		plistFile := "/tmp/" + t + "/install.plist"
		if err = ioutil.WriteFile(plistFile, []byte(installPlistContent), os.ModePerm); err != nil {
			return "", err
		}
		defer func() {
			if err := os.RemoveAll("/tmp/" + t); err != nil {
				logrus.Errorf("remove plistFile %s err: %v", plistFile, err)
			}
		}()
		plistFileUpploadResult, err := i.UploadFileFromFile(plistFile)
		if err != nil {
			return "", err
		}
		pkgInfo = &PKGInfo{
			PackageName: info.Name,
			Version:     info.Version,
			BuildID:     info.Build,
			DisplayName: info.Name,
			Logo:        logoURL,
		}
		meta = map[string]interface{}{"packageName": info.Name, "displayName": info.Name, "version": info.Version,
			"buildID": info.Build, "bundleId": info.BundleId, "installPlist": plistFileUpploadResult.DownloadURL,
			"log": logoURL, "build": info.Build, "appStoreURL": getAppStoreURL(info.BundleId)}
	}

	meta["byteSize"] = mobileFileUploadResult.ByteSize
	meta["fileId"] = mobileFileUploadResult.ID
	releaseResources := []apistructs.ReleaseResource{{
		Type: resourceType,
		Name: mobileFileUploadResult.DisplayName,
		URL:  mobileFileUploadResult.DownloadURL,
		Meta: meta,
	}}
	resourceBytes, err := json.Marshal(releaseResources)
	if err != nil {
		return "", err
	}
	resource := string(resourceBytes)

	versionInfo := apistructs.VersionInfo{
		PackageName: pkgInfo.PackageName,
		Version:     pkgInfo.Version,
		BuildID:     pkgInfo.BuildID,
	}

	// 离线包没有项目和应用名，展示需要，拿这个顶一下
	itemVersionMeta := map[string]string{"appName": pkgInfo.PackageName, "projectName": "OFFLINE"}
	itemVersionMetaBytes, err := json.Marshal(itemVersionMeta)
	if err != nil {
		return "", err
	}
	itemVersionMetaStr := string(itemVersionMetaBytes)

	version, err := i.db.GetPublishItemVersionByName(param.OrgID, param.PublishItemID, resourceType, versionInfo)
	if err != nil && !gorm.IsRecordNotFoundError(err) {
		return "", err
	}
	// 相同版本不会直接覆盖
	if version != nil {
		return "", errors.Errorf("The version already existed, please check the version, versionCode or BundleVersion: %s-%s",
			version.Version, version.BuildID)
	}

	itemVersion := dbclient.PublishItemVersion{
		Version:       pkgInfo.Version,
		BuildID:       pkgInfo.BuildID,
		PackageName:   pkgInfo.PackageName,
		Public:        false,
		OrgID:         param.OrgID,
		Desc:          param.Desc,
		Logo:          pkgInfo.Logo,
		PublishItemID: param.PublishItemID,
		MobileType:    string(resourceType),
		IsDefault:     false,
		Resources:     resource,
		Creator:       param.IdentityInfo.UserID,
		Meta:          itemVersionMetaStr,
		Readme:        "",
		Spec:          "",
		Swagger:       "",
	}
	if err = i.db.CreatePublishItemVersion(&itemVersion); err != nil {
		return "", err
	}

	return string(resourceType), nil
}

// DeletePublishItem 删除发布版本
func (i *PublishItem) DeletePublishItemVersion(id int64) error {
	itemVersion, err := i.db.GetPublishItemVersion(id)
	if err != nil {
		return err
	}
	return i.db.Delete(itemVersion).Error
}

// QueryPublishItemVersions 查询发布版本
func (i *PublishItem) QueryPublishItemVersions(req *apistructs.QueryPublishItemVersionRequest) (*apistructs.QueryPublishItemVersionData, error) {
	itemVersions, err := i.db.QueryPublishItemVersions(req)
	if err != nil {
		return nil, err
	}
	return itemVersions, nil
}

// SetPublishItemVersionDefault 设置发布版本默认状态
func (i *PublishItem) SetPublishItemVersionDefault(itemVersionID, itemID int64) error {
	return i.db.SetPublishItemVersionDefault(itemID, itemVersionID)
}

// SetPublishItemVersionPublic 设置发布版本为公开
func (i *PublishItem) SetPublishItemVersionPublic(id, itemID int64) error {
	return i.db.SetPublishItemVersionPublic(id, itemID)
}

// SetPublishItemVersionUnPublic 设置发布版本为公开
func (i *PublishItem) SetPublishItemVersionUnPublic(id, itemID int64) error {
	return i.db.SetPublishItemVersionUnPublic(id, itemID)
}

// PublicPublishItemVersion 上架或下架版本
func (i *PublishItem) PublicPublishItemVersion(req apistructs.UpdatePublishItemVersionStatesRequset,
	local *i18n.LocaleResource) error {
	v, err := i.db.GetPublishItemVersion(req.PublishItemVersionID)
	if err != nil {
		return err
	}

	// 在发布版本的时候，ios和android暂时不区分包名，即认为一个发布内容下的ios和android版本没有区别
	if v.MobileType == "ios" || v.MobileType == "android" {
		req.PackageName = ""
	}

	total, tmpVersions, err := i.db.GetPublicVersion(req.PublishItemID, apistructs.ResourceType(v.MobileType), req.PackageName)
	if err != nil {
		return err
	}

	versions, err := discriminateReleaseAndBeta(total, tmpVersions)
	if err != nil {
		return err
	}

	switch req.VersionStates {
	case apistructs.PublishItemReleaseVersion:
		if req.Public {
			// 上架正式版
			err = i.publicReleaseVersion(total, versions, req, local)
		} else {
			// 下架正式版
			err = i.unPublicReleaseVersion(total, versions, req, local)
		}
	case apistructs.PublishItemBetaVersion:
		if req.Public {
			// 上架beta版
			err = i.publicBetaVersion(total, versions, req, local)
		} else {
			err = i.unPublicBetaVersion(total, versions, req, local)
		}
	default:
		err = errors.Errorf("unknow version state: %v", req.VersionStates)
	}

	if err != nil {
		return err
	}

	return nil
}

// GetPublicPublishItemVersion 获取线上已发布的版本
func (i *PublishItem) GetPublicPublishItemVersion(itemID int64, mobileType, packageName string) (*apistructs.QueryPublishItemVersionData, error) {
	total, tmpVersions, err := i.db.GetPublicVersion(itemID, apistructs.ResourceType(mobileType), packageName)
	if err != nil {
		return nil, err
	}

	versions, err := discriminateReleaseAndBeta(total, tmpVersions)
	if err != nil {
		return nil, err
	}

	var result []*apistructs.PublishItemVersion
	for _, v := range versions {
		result = append(result, v.ToApiData())
	}

	return &apistructs.QueryPublishItemVersionData{
		Total: total,
		List:  result,
	}, nil
}

// GetH5PackageName 获取H5的包名列表
func (i *PublishItem) GetH5PackageName(itemID int64) ([]string, error) {
	H5Versions, err := i.db.GetH5VersionByItemID(itemID)
	if err != nil {
		return nil, err
	}
	result := make([]string, 0, len(H5Versions))
	for _, v := range H5Versions {
		result = append(result, v.PackageName)
	}

	return result, nil
}

// GetPublicPublishItemLaststVersion 获取线上最新版本的包
func (i *PublishItem) GetPublicPublishItemLaststVersion(ctx context.Context, r *http.Request, req apistructs.GetPublishItemLatestVersionRequest) (*apistructs.GetPublishItemLatestVersionData, error) {
	resp, err := i.bdl.QueryAppPublishItemRelations(&apistructs.QueryAppPublishItemRelationRequest{
		AK: req.AK,
		AI: req.AI,
	})
	if err != nil {
		return nil, errors.Errorf("get app publishItem relations fail: %v", err)
	}

	if len(resp.Data) != 1 {
		return nil, errors.Errorf("invalid ak, ai: %s, %s", req.AK, req.AI)
	}

	publishItem, err := i.db.GetPublishItem(resp.Data[0].PublishItemID)
	if err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, errors.Errorf("invalid publishItemID: %v", resp.Data[0].PublishItemID)
		}
		return nil, err
	}

	// 获取当前的app版本
	currentAppVersion, err := i.db.GetPublishItemVersionByName(publishItem.OrgID, int64(publishItem.ID), req.MobileType, req.CurrentAppInfo)
	if err != nil {
		if err == gorm.ErrRecordNotFound {
			return nil, errors.Errorf("can't find the app version: %v-%v-%v-%v", req.MobileType,
				req.CurrentAppInfo.PackageName, req.CurrentAppInfo.Version, req.CurrentAppInfo.BuildID)
		}
		return nil, err
	}

	// 获取灰度出来的最新app版本
	apps := []*dbclient.PublishItemVersion{currentAppVersion}
	publishItemDistribution, err := i.GetPublishItemDistribution(int64(publishItem.ID), req.MobileType, "",
		ctx.Value(httpserver.ResponseWriter).(http.ResponseWriter), r)
	if err != nil {
		return nil, err
	}

	if publishItemDistribution.Default != nil {
		distributionVersion, err := i.db.GetPublishItemVersion(int64(publishItemDistribution.Default.ID))
		if err != nil {
			return nil, err
		}
		apps = append(apps, distributionVersion)
	}
	newestAppVersion := getNewestVersion(apps...)
	// 服务端校验版本是否需要更新，如果当前版本已经是最新了，返回空版本即可
	if req.Check && newestAppVersion.ID == currentAppVersion.ID {
		// 校验app版本
		newestAppVersion = nil
	}

	newestH5Versions := make(map[string]*apistructs.PublishItemVersion, 0)
	for _, v := range req.CurrentH5Info {
		// 当前H5版本
		currentH5Version, err := i.db.GetPublishItemVersionByName(publishItem.OrgID, int64(publishItem.ID), apistructs.ResourceTypeH5, v)
		if err != nil {
			if err == gorm.ErrRecordNotFound {
				return nil, errors.Errorf("can't find the h5 version: %v-%v-%v-%v", req.MobileType, v.PackageName, v.Version, v.BuildID)
			}
			return nil, err
		}

		// 强制下载预览版H5包
		if req.ForceBetaH5 {
			versions, err := i.GetPublicPublishItemVersion(int64(publishItem.ID), string(apistructs.ResourceTypeH5), v.PackageName)
			if err != nil {
				return nil, err
			}
			if len(versions.List) < 2 {
				goto LABEL1
			}
			if req.Check && versions.List[1].ID == currentH5Version.ID {
				// 校验h5版本
				newestH5Versions[v.PackageName] = nil
			} else {
				newestH5Versions[v.PackageName] = versions.List[1]
			}
			continue
		}

		// 正常最新版本逻辑
	LABEL1:
		// 获取灰度出来的最新H5版本
		h5s := []*dbclient.PublishItemVersion{currentH5Version}
		publishItemDistributionH5, err := i.GetPublishItemDistribution(int64(publishItem.ID), apistructs.ResourceTypeH5, v.PackageName,
			ctx.Value(httpserver.ResponseWriter).(http.ResponseWriter), r)
		if err != nil {
			return nil, err
		}
		if publishItemDistributionH5.Default != nil {
			distributionVersionH5, err := i.db.GetPublishItemVersion(int64(publishItemDistributionH5.Default.ID))
			if err != nil {
				return nil, err
			}
			h5s = append(h5s, distributionVersionH5)
		}
		newestH5Version := getNewestVersion(h5s...)
		// 服务端校验版本是否需要更新，如果当前版本已经是最新了，返回空版本即可
		if req.Check && newestH5Version.ID == currentH5Version.ID {
			// 校验h5版本
			newestH5Version = nil
		}
		newestH5Versions[v.PackageName] = newestH5Version
	}

	return &apistructs.GetPublishItemLatestVersionData{
		AppVersion: newestAppVersion,
		H5Versions: newestH5Versions,
	}, nil
}

func getLogo(resourcedStr string) string {
	var resources []apistructs.ReleaseResource
	err := json.Unmarshal([]byte(resourcedStr), &resources)
	if err != nil {
		return ""
	}
	for _, resource := range resources {
		if resource.Type == apistructs.ResourceTypeAndroid || resource.Type == apistructs.ResourceTypeIOS {
			logo, ok := resource.Meta["logo"]
			if ok {
				return logo.(string)
			}
		}
	}
	return ""
}

// getNewerVersion 获取两个版本里更新的那个
// 有versionCode一定比没versionCode的新，都有则versionCode大的新，versioncode都没有则比较时间
// 一样新则返回version2
func getNewestVersion(versions ...*dbclient.PublishItemVersion) *apistructs.PublishItemVersion {
	if len(versions) == 1 {
		return versions[0].ToApiData()
	}
	tmpV := versions[0]
	for _, v := range versions[1:] {
		if !tmpV.IsLater(v) {
			tmpV = v
		}
	}

	return tmpV.ToApiData()
}

// getAppStoreURL 根据bundleID从app store搜索链接，目前只从中国区查找
func getAppStoreURL(bundleID string) string {
	tr := &http.Transport{
		TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
	}
	hc := &http.Client{
		Transport: tr,
	}
	var getResp apistructs.AppStoreResponse
	resp, err := hc.Get("https://itunes.apple.com/cn/lookup?bundleId=" + bundleID)
	if err != nil {
		logrus.Errorf("get app store url err: %v", err)
		return ""
	}
	defer resp.Body.Close()

	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		logrus.Errorf("get app store url err: %v", err)
		return ""
	}
	err = json.Unmarshal(body, &getResp)
	if err != nil {
		logrus.Errorf("get app store url err: %v", err)
		return ""
	}

	fmt.Println(getResp)

	if getResp.ResultCount == 0 {
		return ""
	}

	return getResp.Results[0].TrackViewURL
}
