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

package extension

import (
	"context"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"os"
	"os/exec"
	"strings"
	"sync"
	"time"

	"github.com/Masterminds/semver"
	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"google.golang.org/protobuf/types/known/structpb"
	"google.golang.org/protobuf/types/known/timestamppb"
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda-infra/base/version"
	gallerypb "github.com/erda-project/erda-proto-go/apps/gallery/pb"
	"github.com/erda-project/erda-proto-go/core/dicehub/extension/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/extension/db"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/service/apierrors"
	galleryTypes "github.com/erda-project/erda/internal/apps/gallery/types"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/i18n"
	"github.com/erda-project/erda/pkg/limit_sync_group"
	"github.com/erda-project/erda/pkg/strutil"
)

type extensionService struct {
	p             *provider
	db            *db.ExtensionConfigDB
	bdl           *bundle.Bundle
	extensionMenu map[string][]string

	cacheExtensionSpecs sync.Map
}

func (s *extensionService) SearchExtensions(ctx context.Context, req *pb.ExtensionSearchRequest) (*pb.ExtensionSearchResponse, error) {
	result := map[string]*pb.ExtensionVersion{}

	worker := limit_sync_group.NewWorker(5)
	for _, fullName := range req.Extensions {
		worker.AddFunc(func(locker *limit_sync_group.Locker, i ...interface{}) error {
			fnm := i[0].(string)
			splits := strings.SplitN(fnm, "@", 2)
			name := splits[0]
			version := ""
			if len(splits) > 1 {
				version = splits[1]
			}
			if version == "" {
				extVersion, _ := s.GetExtensionDefaultVersion(name, req.YamlFormat)

				locker.Lock()
				result[fnm] = extVersion
				locker.Unlock()
			} else if strings.HasPrefix(version, "https://") || strings.HasPrefix(version, "http://") {
				extensionVersion, err := s.getExtensionByGit(name, version, "spec.yml", "dice.yml", "README.md")

				locker.Lock()
				if err != nil {
					result[fnm] = nil
				} else {
					result[fnm] = extensionVersion
				}
				locker.Unlock()
			} else {
				extensionVersion, err := s.GetExtension(name, version, req.YamlFormat)

				locker.Lock()
				if err != nil {
					result[fnm] = nil
				} else {
					result[fnm] = extensionVersion
				}
				locker.Unlock()
			}
			return nil
		}, fullName)
	}

	err := worker.Do().Error()
	if err != nil {
		return nil, err
	}

	return &pb.ExtensionSearchResponse{Data: result}, nil
}

func (i *extensionService) getExtensionByGit(name, d string, file ...string) (*pb.ExtensionVersion, error) {
	files, err := getGitFileContents(d, file...)
	if err != nil {
		return nil, err
	}

	return &pb.ExtensionVersion{
		Name:      name,
		Type:      "action",
		Version:   "1.0",
		Spec:      structpb.NewStringValue(files[0]),
		Dice:      structpb.NewStringValue(files[1]),
		Swagger:   structpb.NewStringValue(""),
		Readme:    files[2],
		CreatedAt: timestamppb.New(time.Now()),
		UpdatedAt: timestamppb.New(time.Now()),
		IsDefault: false,
		Public:    true,
	}, nil
}

func getGitFileContents(d string, file ...string) ([]string, error) {
	var resp []string
	// dirName is a random string
	dir, err := ioutil.TempDir(os.TempDir(), "*")
	if err != nil {
		return nil, err
	}
	defer os.RemoveAll(dir)

	// git init
	command := exec.Command("sh", "-c", "git init")
	command.Dir = dir
	err = command.Run()
	if err != nil {
		return nil, err
	}

	// git remote
	remoteCmd := "git remote add -f origin " + d
	command = exec.Command("sh", "-c", remoteCmd)
	command.Dir = dir
	err = command.Run()
	if err != nil {
		return nil, err
	}

	// set git config
	command = exec.Command("sh", "-c", "git config core.sparsecheckout true")
	command.Dir = dir
	err = command.Run()
	if err != nil {
		return nil, err
	}

	// set sparse-checkout
	for _, v := range file {
		echoCmd := "echo " + v + " >> .git/info/sparse-checkout"
		command = exec.Command("sh", "-c", echoCmd)
		command.Dir = dir
		err = command.Run()
		if err != nil {
			return nil, err
		}
	}

	// git pull
	command = exec.Command("sh", "-c", "git pull origin master")
	command.Dir = dir
	err = command.Run()
	if err != nil {
		return nil, err
	}

	// read .yml
	for _, v := range file {
		f, err := os.Open(dir + "/" + v)
		if err != nil {
			resp = append(resp, "")
			continue
		}
		str, err := ioutil.ReadAll(f)
		if err != nil {
			return nil, err
		}
		f.Close()
		resp = append(resp, string(str))
	}

	return resp, nil
}

func (s *extensionService) CreateExtension(ctx context.Context, req *pb.ExtensionCreateRequest) (*pb.ExtensionCreateResponse, error) {
	err := s.checkPushPermission(ctx)
	if err != nil {
		return nil, apierrors.ErrCreateExtensionVersion.AccessDenied()
	}

	if req.Type != "action" && req.Type != "addon" {
		return nil, apierrors.ErrCreateExtension.InvalidParameter("type")
	}

	result, err := s.Create(req)

	if err != nil {
		return nil, apierrors.ErrCreateExtension.InternalError(err)
	}

	return &pb.ExtensionCreateResponse{Data: result}, nil
}

func (s *extensionService) QueryExtensions(ctx context.Context, req *pb.QueryExtensionsRequest) (*pb.QueryExtensionsResponse, error) {
	result, err := s.QueryExtensionList(req.All, req.Type, req.Labels)
	if err != nil {
		return nil, apierrors.ErrQueryExtension.InternalError(err)
	}

	locale := s.bdl.GetLocale(apis.GetLang(ctx))
	data, err := s.menuExtWithLocale(result, locale, req.All)
	if err != nil {
		return nil, apierrors.ErrQueryExtension.InternalError(err)
	}

	var newResult []*pb.Extension
	for _, menu := range data {
		for _, value := range menu {
			for _, extension := range value.Items {
				newResult = append(newResult, extension)
			}
		}
	}

	if req.Menu == "true" {
		menuExtResult := menuExt(newResult, s.extensionMenu)
		resp, err := ToProtoValue(menuExtResult)
		if err != nil {
			logrus.Errorf("fail transform interface to any type")
			return nil, apierrors.ErrQueryExtension.InternalError(err)
		}
		return &pb.QueryExtensionsResponse{Data: resp}, nil
	}

	resp, err := ToProtoValue(newResult)
	if err != nil {
		logrus.Errorf("fail transform interface to any type")
		return nil, apierrors.ErrQueryExtension.InternalError(err)
	}
	return &pb.QueryExtensionsResponse{Data: resp}, nil
}

func (i *extensionService) menuExtWithLocale(extensions []*pb.Extension, locale *i18n.LocaleResource, all bool) (map[string][]pb.ExtensionMenu, error) {
	var result = map[string][]pb.ExtensionMenu{}

	var extensionName []string
	for _, v := range extensions {
		extensionName = append(extensionName, v.Name)
	}
	extensionVersionMap, err := i.db.ListExtensionVersions(extensionName, all)
	if err != nil {
		return nil, apierrors.ErrQueryExtension.InternalError(err)
	}

	extMap := i.extMap(extensions)
	// Traverse categories with large categories
	for categoryType, typeItems := range apistructs.CategoryTypes {
		// Each category belongs to a map
		menuList := result[categoryType]
		// Traverse subcategories in a large category
		for _, v := range typeItems {
			// Gets the object data of the extension of this subcategory
			extensionListWithKeyName, ok := extMap[v]
			if !ok {
				continue
			}

			// extension displayName desc Internationalization settings
			for _, extension := range extensionListWithKeyName {
				defaultExtensionVersion := getDefaultExtensionVersion("", extensionVersionMap[extension.Name])
				if defaultExtensionVersion.ID <= 0 {
					logrus.Errorf("extension %v not find default extension version", extension.Name)
					continue
				}

				// get from caches and set to caches
				localeDisplayName, localeDesc, err := i.getLocaleDisplayNameAndDesc(defaultExtensionVersion)
				if err != nil {
					return nil, err
				}

				if localeDisplayName != "" {
					extension.DisplayName = localeDisplayName
				}

				if localeDesc != "" {
					extension.Desc = localeDesc
				}
			}

			// Whether this subcategory is internationalized or not is the name of the word category
			var displayName string
			if locale != nil {
				displayNameTemplate := locale.GetTemplate(apistructs.DicehubExtensionsMenu + "." + categoryType + "." + v)
				if displayNameTemplate != nil {
					displayName = displayNameTemplate.Content()
				}
			}

			if displayName == "" {
				displayName = v
			}
			// Assign these word categories to the array
			menuList = append(menuList, pb.ExtensionMenu{
				Name:        v,
				DisplayName: displayName,
				Items:       extensionListWithKeyName,
			})
		}
		// Set the array back into the map
		result[categoryType] = menuList
	}

	return result, nil
}

func (i *extensionService) extMap(extensions []*pb.Extension) map[string][]*pb.Extension {
	extMap := map[string][]*pb.Extension{}
	for _, v := range extensions {
		extList, exist := extMap[v.Category]
		if exist {
			extList = append(extList, v)
		} else {
			extList = []*pb.Extension{v}
		}
		extMap[v.Category] = extList
	}
	return extMap
}

func getDefaultExtensionVersion(version string, extensionVersions []db.ExtensionVersion) db.ExtensionVersion {
	var defaultVersion db.ExtensionVersion
	if version == "" {
		for _, extensionVersion := range extensionVersions {
			if extensionVersion.IsDefault {
				defaultVersion = extensionVersion
				break
			}
		}
		if defaultVersion.ID <= 0 && len(extensionVersions) > 0 {
			defaultVersion = extensionVersions[0]
		}
	} else {
		for _, extensionVersion := range extensionVersions {
			if extensionVersion.Version == version {
				defaultVersion = extensionVersion
				break
			}
		}
	}
	return defaultVersion
}

func (s *extensionService) getLocaleDisplayNameAndDesc(extensionVersion db.ExtensionVersion) (string, string, error) {
	value, ok := s.cacheExtensionSpecs.Load(extensionVersion.Spec)
	var specData apistructs.Spec
	if !ok {
		err := yaml.Unmarshal([]byte(extensionVersion.Spec), &specData)
		if err != nil {
			return "", "", apierrors.ErrQueryExtension.InternalError(err)
		}
		s.cacheExtensionSpecs.Store(extensionVersion.Spec, specData)
		go func(spec string) {
			// caches expiration time
			time.AfterFunc(30*25*time.Hour, func() {
				s.cacheExtensionSpecs.Delete(spec)
			})
		}(extensionVersion.Spec)
	} else {
		specData = value.(apistructs.Spec)
	}

	displayName := specData.GetLocaleDisplayName(i18n.GetGoroutineBindLang())
	desc := specData.GetLocaleDesc(i18n.GetGoroutineBindLang())

	return displayName, desc, nil
}

func (s *extensionService) QueryExtensionsMenu(ctx context.Context, req *pb.QueryExtensionsMenuRequest) (*pb.QueryExtensionsMenuResponse, error) {
	locale := s.bdl.GetLocale(apis.GetLang(ctx))
	result, err := s.QueryExtensionList(req.All, req.Type, req.Labels)
	if err != nil {
		return nil, apierrors.ErrQueryExtension.InternalError(err)
	}

	data, err := s.menuExtWithLocale(result, locale, req.All)
	if err != nil {
		return nil, apierrors.ErrQueryExtension.InternalError(err)
	}

	resp := make(map[string]*structpb.Value)
	for k, v := range data {
		val, err := ToProtoValue(v)
		if err != nil {
			return nil, apierrors.ErrQueryExtension.InternalError(err)
		}
		resp[k] = val
	}

	return &pb.QueryExtensionsMenuResponse{Data: resp}, nil
}

func (s *extensionService) CreateExtensionVersion(ctx context.Context, req *pb.ExtensionVersionCreateRequest) (*pb.ExtensionVersionCreateResponse, error) {
	err := s.checkPushPermission(ctx)
	if err != nil {
		return nil, apierrors.ErrCreateExtensionVersion.AccessDenied()
	}
	return s.CreateExtensionVersionByRequest(req)
}

func (s *extensionService) CreateExtensionVersionByRequest(req *pb.ExtensionVersionCreateRequest) (*pb.ExtensionVersionCreateResponse, error) {
	specData := apistructs.Spec{}
	err := yaml.Unmarshal([]byte(req.SpecYml), &specData)
	if err != nil {
		return nil, apierrors.ErrQueryExtension.InternalError(err)
	}
	if specData.DisplayName == "" {
		specData.DisplayName = specData.Name
	}
	// Non-semantic version cannot set public
	_, err = semver.NewVersion(specData.Version)
	if err != nil && req.Public {
		return nil, apierrors.ErrQueryExtension.InternalError(err)
	}

	if !specData.CheckDiceVersion(version.Version) {
		err := s.db.DeleteExtensionVersion(specData.Name, specData.Version)
		if err != nil {
			return nil, apierrors.ErrQueryExtension.InternalError(err)
		}
		count, err := s.db.GetExtensionVersionCount(specData.Name)
		if err != nil {
			return nil, apierrors.ErrQueryExtension.InternalError(err)
		}
		if count == 0 {
			err = s.db.DeleteExtension(specData.Name)
			if err != nil {
				return nil, apierrors.ErrQueryExtension.InternalError(err)
			}
		}
		s.triggerPushEvent(specData, "delete")
		return nil, nil
	}
	labels := ""
	if specData.Labels != nil {
		for k, v := range specData.Labels {
			labels += k + ":" + v + ","
		}
	}
	extModel, err := s.db.GetExtension(req.Name)
	var ext *pb.Extension
	if err == nil {
		ext = extModel.ToApiData()
	} else if err == gorm.ErrRecordNotFound {
		// no same name,create
		ext, err = s.Create(&pb.ExtensionCreateRequest{
			Type:        specData.Type,
			Name:        req.Name,
			DisplayName: specData.DisplayName,
			Desc:        specData.Desc,
			Category:    specData.Category,
			LogoUrl:     specData.LogoUrl,
			Public:      req.Public,
			Labels:      labels,
		})
		if err != nil {
			return nil, apierrors.ErrQueryExtension.InternalError(err)
		}
	} else {
		return nil, apierrors.ErrQueryExtension.InternalError(err)
	}

	ver, err := s.db.GetExtensionVersion(req.Name, req.Version)
	switch {
	case err == nil && req.GetForceUpdate():
		ver.Spec = req.SpecYml
		ver.Dice = req.DiceYml
		ver.Swagger = req.SwaggerYml
		ver.Readme = req.Readme
		ver.Public = req.Public
		ver.IsDefault = req.IsDefault
		if ver.IsDefault {
			err := s.db.SetUnDefaultVersion(ver.Name)
			if err != nil {
				return nil, apierrors.ErrQueryExtension.InternalError(err)
			}
		}
		err := s.db.Save(&ver).Error
		if err != nil {
			return nil, apierrors.ErrQueryExtension.InternalError(err)
		}
		s.triggerPushEvent(specData, "update")
		if req.All {
			extModel.Category = specData.Category
			extModel.LogoUrl = specData.LogoUrl
			extModel.DisplayName = specData.DisplayName
			extModel.Desc = specData.Desc
			extModel.Public = req.Public
			extModel.Labels = labels
			err = s.db.Save(&extModel).Error
			if err != nil {
				return nil, apierrors.ErrQueryExtension.InternalError(err)
			}
		}
		data, err := ver.ToApiData(ext.Type, false)
		if err != nil {
			return nil, apierrors.ErrQueryExtension.InternalError(err)
		}

		go s.PutOnExtensionWithNameVersion(context.Background(), *ver)
		return &pb.ExtensionVersionCreateResponse{Data: data}, nil
	case err == nil:
		return nil, apierrors.ErrQueryExtension.AlreadyExists()
	case err == gorm.ErrRecordNotFound:
		ver = &db.ExtensionVersion{
			ExtensionId: ext.Id,
			Name:        specData.Name,
			Version:     specData.Version,
			Dice:        req.DiceYml,
			Spec:        req.SpecYml,
			Swagger:     req.SwaggerYml,
			Readme:      req.Readme,
			Public:      req.Public,
			IsDefault:   req.IsDefault,
		}
		err = s.db.CreateExtensionVersion(ver)
		s.triggerPushEvent(specData, "create")
		if err != nil {
			return nil, apierrors.ErrQueryExtension.InternalError(err)
		}

		data, err := ver.ToApiData(ext.Type, false)
		if err != nil {
			return nil, apierrors.ErrQueryExtension.InternalError(err)
		}
		go s.PutOnExtensionWithNameVersion(context.Background(), *ver)
		return &pb.ExtensionVersionCreateResponse{Data: data}, nil
	default:
		return nil, apierrors.ErrQueryExtension.InternalError(err)
	}
}

func (s *extensionService) GetExtensionVersion(ctx context.Context, req *pb.GetExtensionVersionRequest) (*pb.GetExtensionVersionResponse, error) {
	result, err := s.GetExtension(req.Name, req.Version, req.YamlFormat)

	if err != nil {
		if err == gorm.ErrRecordNotFound {
			return nil, apierrors.ErrQueryExtension.NotFound()
		}
		return nil, apierrors.ErrQueryExtension.InternalError(err)
	}

	return &pb.GetExtensionVersionResponse{Data: result}, nil
}

func (s *extensionService) QueryExtensionVersions(ctx context.Context, req *pb.ExtensionVersionQueryRequest) (*pb.ExtensionVersionQueryResponse, error) {
	ext, err := s.db.GetExtension(req.Name)
	if err != nil {
		return nil, err
	}
	versions, err := s.db.QueryExtensionVersions(req.Name, req.All, req.OrderByVersionDesc)
	if err != nil {
		return nil, err
	}
	result := []*pb.ExtensionVersion{}
	for _, v := range versions {
		data, err := v.ToApiData(ext.Type, req.YamlFormat)
		if err != nil {
			return nil, err
		}
		result = append(result, data)
	}
	return &pb.ExtensionVersionQueryResponse{Data: result}, nil
}

// Create Create Extension
func (s *extensionService) Create(req *pb.ExtensionCreateRequest) (*pb.Extension, error) {
	if req.Type != "addon" && req.Type != "action" {
		return nil, apierrors.ErrCreateExtension.InvalidParameter("type")
	}
	if req.Name == "" {
		return nil, apierrors.ErrCreateExtension.InvalidParameter("name")
	}
	ext := db.Extension{
		Name:        req.Name,
		Type:        req.Type,
		Desc:        req.Desc,
		Category:    req.Category,
		DisplayName: req.DisplayName,
		LogoUrl:     req.LogoUrl,
		Public:      req.Public,
		Labels:      req.Labels,
	}
	err := s.db.CreateExtension(&ext)
	if err != nil {
		return nil, err
	}
	return ext.ToApiData(), nil
}

// QueryExtensionList Get Extension List
func (s *extensionService) QueryExtensionList(all bool, typ string, labels string) ([]*pb.Extension, error) {
	extensions, err := s.db.QueryExtensions(all, typ, labels)
	if err != nil {
		return nil, err
	}
	result := []*pb.Extension{}
	for _, v := range extensions {
		apiData := v.ToApiData()
		result = append(result, apiData)
	}
	return result, nil
}

// GetExtension Get extension with specified version
func (s *extensionService) GetExtension(name string, version string, yamlFormat bool) (*pb.ExtensionVersion, error) {
	ext, err := s.db.GetExtension(name)
	if err != nil {
		return nil, err
	}
	var extensionVersion *db.ExtensionVersion
	if version == "default" {
		extensionVersion, err = s.db.GetExtensionDefaultVersion(name)
	} else {
		extensionVersion, err = s.db.GetExtensionVersion(name, version)
	}
	if err != nil {
		return nil, err
	}
	return extensionVersion.ToApiData(ext.Type, yamlFormat)
}

// GetExtensionDefaultVersion Get extension default version
func (s *extensionService) GetExtensionDefaultVersion(name string, yamlFormat bool) (*pb.ExtensionVersion, error) {
	ext, err := s.db.GetExtension(name)
	if err != nil {
		return nil, err
	}
	extensionVersion, err := s.db.GetExtensionDefaultVersion(name)
	if err != nil {
		return nil, err
	}
	return extensionVersion.ToApiData(ext.Type, yamlFormat)
}

func (s *extensionService) PutOnExtensionsToGallery(ctx context.Context) error {
	l := logrus.WithField("impl", "extensionService").WithField("func", "PutOnExtensionsToGallery")
	extensions, err := s.db.QueryAllExtensions()
	if err != nil {
		l.WithError(err).Errorln("failed to QueryAllExtensions")
		return errors.Wrap(err, "failed to QueryAllExtensions")
	}
	for _, ext := range extensions {
		if err := s.PutOnExtensionWithName(ctx, ext.Name); err != nil {
			l.WithError(err).WithField("name", ext.Name).Errorln("failed to PutOnExtensionWithName")
		}
	}
	return nil
}

func (s *extensionService) PutOnExtensionWithName(ctx context.Context, name string) error {
	l := logrus.WithField("impl", "extensionService").
		WithField("func", "PutOnExtensionWithNameVersion").
		WithField("name", name)

	versions, err := s.db.QueryExtensionVersions(name, true, true)
	if err != nil {
		l.WithError(err).Errorln("failed to QueryExtensionVersions")
		return errors.Wrap(err, "failed to QueryExtensionVersions")
	}
	for _, ver := range versions {
		if !ver.Public {
			continue
		}
		if err := s.PutOnExtensionWithNameVersion(ctx, ver); err != nil {
			l.WithError(err).Errorln("failed to PutOnExtensionWithNameVersion")
		}
	}
	return nil
}

func (s *extensionService) PutOnExtensionWithNameVersion(ctx context.Context, ver db.ExtensionVersion) error {
	l := logrus.WithField("impl", "extensionService").
		WithField("func", "PutOnExtensionWithNameVersion").
		WithField("name", ver.Name).
		WithField("version", ver.Version)

	var spec apistructs.Spec
	if err := yaml.Unmarshal([]byte(ver.Spec), &spec); err != nil {
		l.WithError(err).Errorln("failed to yaml.Unmarshal ver.Spec")
		return err
	}
	if !isExtensionPublic(&spec) {
		l.Debugf("extension %s is private, skip putting on", spec.Name)
		return errors.New("private extension can not be put on")
	}

	if spec.DisplayName == "" {
		spec.DisplayName = spec.Name
	}
	types := map[string]string{"action": galleryTypes.OpusTypeExtensionAction.String(), "addon": galleryTypes.OpusTypeExtensionAddon.String()}
	item := &gallerypb.PutOnExtensionsReq{
		Type:        types[strings.ToLower(spec.Type)],
		Name:        ver.Name,
		Version:     ver.Version,
		DisplayName: spec.DisplayName,
		Summary:     spec.Desc,
		Catalog:     spec.Category,
		LogoURL:     spec.LogoUrl,
		Level:       galleryTypes.OpusLevelSystem.String(),
		Mode:        galleryTypes.PutOnOpusModeOverride.String(),
		Desc:        spec.Desc,
		Readme: []*gallerypb.Readme{{
			Lang:     galleryTypes.LangUnknown.String(),
			LangName: galleryTypes.LangTypes[galleryTypes.LangUnknown],
			Text:     ver.Readme,
		}},
		IsDefault: spec.IsDefault,
	}
	for k, v := range spec.Labels {
		item.Labels = append(item.Labels, k+"="+v)
	}
	local, _ := json.Marshal(spec.Locale)
	item.DisplayNameI18N = ConvertFieldI18n(item.GetDisplayName(), spec.Locale)
	item.SummaryI18N = ConvertFieldI18n(item.GetSummary(), spec.Locale)
	item.DescI18N = ConvertFieldI18n(item.GetDesc(), spec.Locale)
	item.I18N = string(local)

	l.Infoln("GalleryServer.PutOnExtensions")
	resp, err := s.p.GalleryServer.PutOnExtensions(ctx, item)
	if err != nil {
		l.WithError(err).Errorln("failed to PutOnExtensions")
		return err
	}
	l.WithField("opus id", resp.OpusID).
		WithField("opus version id", resp.VersionID).
		Infoln("the extension is pushed to the gallery")
	return nil
}

func (s *extensionService) triggerPushEvent(specData apistructs.Spec, action string) {
	if specData.Type != "addon" {
		return
	}
	go func() {
		err := s.bdl.CreateEvent(&apistructs.EventCreateRequest{
			EventHeader: apistructs.EventHeader{
				Event:  "addon_extension_push",
				Action: action,
			},
			Sender: "dicehub",
			Content: apistructs.ExtensionPushEventData{
				Name:    specData.Name,
				Version: specData.Version,
				Type:    specData.Type,
			},
		})
		if err != nil {
			logrus.Errorf("failed to create event :%v", err)
		}
	}()
}

func (s *extensionService) checkPushPermission(ctx context.Context) error {
	userID := apis.GetUserID(ctx)
	if userID == "" {
		return errors.Errorf("failed to get permission(User-ID is empty)")
	}
	data, err := s.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   userID,
		Scope:    apistructs.SysScope,
		ScopeID:  1,
		Action:   apistructs.CreateAction,
		Resource: apistructs.OrgResource,
	})
	if err != nil {
		return err
	}
	if !data.Access {
		return errors.New("no permission to push")
	}
	return nil
}

func ToProtoValue(i interface{}) (*structpb.Value, error) {
	data, err := json.Marshal(i)
	if err != nil {
		return nil, err
	}
	v := &structpb.Value{}
	err = v.UnmarshalJSON(data)
	if err != nil {
		return nil, err
	}
	return v, nil
}

func ConvertFieldI18n(key string, locale map[string]map[string]string) string {
	if key == "" || len(locale) == 0 {
		return ""
	}
	left := "${{"
	right := "}}"
	prefix := "i18n."
	exprF := func(s string) bool { return strings.HasPrefix(s, prefix) }
	expr, start, end, err := strutil.FirstCustomExpression(key, left, right, exprF)
	fmt.Printf("expr: %s, start: %d, end: %d, err: %v\n", expr, start, end, err)
	if err != nil || start == end || expr == "" {
		return ""
	}
	expr = strings.TrimPrefix(expr, prefix)
	var m = make(map[string]string)
	for lang, kvs := range locale {
		if len(kvs) == 0 {
			continue
		}
		if v, ok := kvs[expr]; ok {
			m[strings.ToLower(lang)] = v
		}
	}
	data, _ := json.Marshal(m)
	return string(data)
}

func isExtensionPublic(s *apistructs.Spec) bool {
	return s.Public
}
