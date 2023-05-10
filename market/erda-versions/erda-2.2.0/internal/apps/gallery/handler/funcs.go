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
// limitations under the License.o

package handler

import (
	"context"
	"encoding/json"
	"sort"
	"strings"

	"github.com/pkg/errors"
	"google.golang.org/protobuf/types/known/timestamppb"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/apps/gallery/pb"
	"github.com/erda-project/erda/internal/apps/gallery/dao"
	"github.com/erda-project/erda/internal/apps/gallery/model"
	"github.com/erda-project/erda/internal/apps/gallery/types"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/strutil"
)

func ListOpusTypes(ctx context.Context, tran i18n.Translator) *pb.ListOpusTypesRespData {
	var data pb.ListOpusTypesRespData
	langCodes := apis.Language(ctx)
	for type_, name := range types.OpusTypeNames {
		data.List = append(data.List, &pb.OpusType{
			Type:        type_.String(),
			Name:        name,
			DisplayName: tran.Text(langCodes, type_.String()),
		})
	}
	sort.Slice(data.List, func(i, j int) bool {
		return data.List[i].Type > data.List[j].Type
	})
	data.Total = uint32(len(data.List))
	return &data
}

func AdjustPaging(pageSize, pageNo int32) (int, int) {
	var size, no = 10, 1
	if pageSize >= 10 && pageSize <= 1000 {
		size = int(pageSize)
	}
	if pageNo >= 1 {
		no = int(pageNo)
	}
	return size, no
}

func PrepareListOpusesOptions(orgID int64, type_, name, keyword string, pageSize, pageNo int) []dao.Option {
	var options = []dao.Option{
		dao.PageOption(pageSize, pageNo),
		dao.OrderByOption("type", ""),
		dao.OrderByOption("name", ""),
		dao.OrderByOption("updated_at", ""),
	}
	if type_ != "" {
		options = append(options, dao.WhereOption("type = ?", type_))
	}
	if name != "" {
		options = append(options, dao.WhereOption("name = ?", name))
	}
	if keyword != "" {
		keyword = "%" + keyword + "%"
		options = append(options, dao.WhereOption("display_name LIKE ? OR display_name_i18n LIKE ? OR summary LIKE ? OR summary_i18n LIKE ?",
			keyword, keyword, keyword, keyword))
	}
	options = append(options, dao.WhereOption("org_id = ? OR level = ?", orgID, types.OpusLevelSystem))
	return options
}

func PrepareListOpusesKeywordFilterOption(keyword string, versions []*model.OpusVersion) dao.Option {
	var opusesIDs []string
	for _, version := range versions {
		opusesIDs = append(opusesIDs, version.OpusID)
	}
	keyword = "%" + keyword + "%"
	return dao.WhereOption("name LIKE ? OR display_name LIKE ? OR id IN (?)", keyword, keyword, opusesIDs)
}

func PrepareListVersionsInOpusesIDsOption(opuses []*model.Opus) dao.Option {
	var opusesIDs = make(map[interface{}]struct{})
	for _, opus := range opuses {
		opusesIDs[opus.ID] = struct{}{}
	}
	return dao.InOption("opus_id", opusesIDs)
}

func ComposeListOpusResp(ctx context.Context, total int64, opuses []*model.Opus, trans i18n.Translator) *pb.ListOpusResp {
	var result = pb.ListOpusResp{Data: &pb.ListOpusRespData{Total: int32(total)}}
	langCodes := apis.Language(ctx)
	lang := AdjustLang(apis.GetLang(ctx))
	for _, opus := range opuses {
		item := pb.ListOpusRespDataItem{
			Id:          opus.ID.String,
			CreatedAt:   timestamppb.New(opus.CreatedAt),
			UpdatedAt:   timestamppb.New(opus.UpdatedAt),
			OrgID:       opus.OrgID,
			OrgName:     opus.OrgName,
			CreatorID:   opus.CreatorID,
			UpdaterID:   opus.UpdaterID,
			Type:        opus.Type,
			TypeName:    types.OpusTypeNames[types.OpusType(opus.Type)],
			Name:        opus.Name,
			DisplayName: opus.DisplayName,
			Summary:     opus.Summary,
			Catalog:     opus.Catalog,
			CatalogName: trans.Text(langCodes, opus.Catalog),
			LogoURL:     opus.LogoURL,
		}
		if types.OpusLevelSystem.Equal(opus.Level) {
			item.OrgName = "Erda"
		}
		_ = RenderI18n(&item.DisplayName, opus.DisplayNameI18n, lang)
		_ = RenderI18n(&item.Summary, opus.SummaryI18n, lang)
		result.Data.List = append(result.Data.List, &item)
	}
	return &result
}

func ComposeListOpusVersionRespWithOpus(lang string, resp *pb.ListOpusVersionsResp, opus *model.Opus) {
	resp.Data = &pb.ListOpusVersionsRespData{
		Id:               opus.ID.String,
		CreatedAt:        timestamppb.New(opus.CreatedAt),
		UpdatedAt:        timestamppb.New(opus.UpdatedAt),
		OrgID:            opus.OrgID,
		OrgName:          opus.OrgName,
		CreatorID:        opus.CreatorID,
		UpdaterID:        opus.UpdaterID,
		Level:            opus.Level,
		Type:             opus.Type,
		Name:             types.OpusTypeNames[types.OpusType(opus.Type)],
		DisplayName:      opus.DisplayName,
		Catalog:          opus.Catalog,
		DefaultVersionID: opus.DefaultVersionID,
		LatestVersionID:  opus.LatestVersionID,
	}
	if types.OpusLevelSystem.Equal(opus.Level) {
		resp.Data.OrgName = "Erda"
	}
	_ = RenderI18n(&resp.Data.DisplayName, opus.DisplayNameI18n, lang)
}

func ComposeListOpusVersionRespWithVersions(lang string, resp *pb.ListOpusVersionsResp, versions []*model.OpusVersion) error {
	sort.Slice(versions, func(i, j int) bool {
		return versions[i].CreatedAt.After(versions[j].CreatedAt)
	})
	for _, version := range versions {
		item := pb.ListOpusVersionRespDataVersion{
			Id:        version.ID.String,
			CreatedAt: timestamppb.New(version.CreatedAt),
			UpdatedAt: timestamppb.New(version.UpdatedAt),
			CreatorID: version.CreatorID,
			UpdaterID: version.UpdaterID,
			Version:   version.Version,
			Summary:   version.Summary,
			Labels:    nil,
			LogoURL:   version.LogoURL,
			IsValid:   version.IsValid,
		}
		if err := json.Unmarshal([]byte(version.Labels), &item.Labels); err != nil {
			return errors.Wrapf(err, "failed to Unmarshal version.Labels, labels: %s", version.Labels)
		}
		_ = RenderI18n(&item.Summary, version.SummaryI18n, lang)
		resp.Data.Versions = append(resp.Data.Versions, &item)
		resp.UserIDs = append(resp.UserIDs, version.CreatorID)
	}
	return nil
}

func ComposeListOpusVersionRespWithPresentations(lang string, resp *pb.ListOpusVersionsResp, presentations []*model.OpusPresentation) {
	var presentationMap = make(map[string]*model.OpusPresentation)
	for _, item := range presentations {
		presentationMap[item.VersionID] = item
	}
	for _, version := range resp.Data.Versions {
		if pre, ok := presentationMap[version.GetId()]; ok {
			version.Ref = pre.Ref
			version.Desc = pre.Desc
			version.ContactName = pre.ContactName
			version.ContactURL = pre.ContactURL
			version.ContactEmail = pre.ContactEmail
			version.IsOpenSourced = pre.IsOpenSourced
			version.OpensourceURL = pre.OpensourceURL
			version.LicenceName = pre.LicenseName
			version.LicenceURL = pre.LicenseURL
			version.HomepageName = pre.HomepageName
			version.HomepageURL = pre.HomepageURL
			version.HomepageLogoURL = pre.HomepageLogoURL
			version.IsDownloadable = pre.IsDownloadable
			version.DownloadURL = pre.DownloadURL
			_ = RenderI18n(&version.Desc, pre.DescI18n, lang)
		}
	}
}

func ComposeListOpusVersionRespWithReadmes(resp *pb.ListOpusVersionsResp, lang string, readmes []*model.OpusReadme) {
	var readmesMap = make(map[string]map[string]*model.OpusReadme)
	for _, item := range readmes {
		m, ok := readmesMap[item.VersionID]
		if !ok {
			m = make(map[string]*model.OpusReadme)
		}
		m[item.Lang] = item
		readmesMap[item.VersionID] = m
	}
	for _, version := range resp.Data.Versions {
		langs, ok := readmesMap[version.GetId()]
		if !ok {
			continue
		}
		if readme, ok := langs[lang]; ok {
			version.ReadmeLang = readme.Lang
			version.ReadmeLangName = readme.LangName
			version.Readme = readme.Text
			continue
		}
		if readme, ok := langs[types.LangUnknown.String()]; ok {
			version.ReadmeLang = readme.Lang
			version.ReadmeLangName = readme.LangName
			version.Readme = readme.Text
			continue
		}
		for k := range langs {
			version.ReadmeLang = langs[k].Lang
			version.ReadmeLangName = langs[k].LangName
			version.Readme = langs[k].Text
			break
		}
	}
}

func AdjustLang(lang string) string {
	if lang == "" {
		return "en-us"
	}
	return strings.ToLower(lang)
}

func RenderI18n(value *string, values, lang string) error {
	if value == nil {
		return nil
	}
	exprLeft := "${{"
	exprRight := "}}"
	exprF := func(s string) bool { return strings.HasPrefix(s, "i18n.") }
	_, start, end, err := strutil.FirstCustomExpression(*value, exprLeft, exprRight, exprF)
	if err != nil {
		return err
	}
	if start == end {
		return nil
	}
	var m = make(map[string]string)
	if err = json.Unmarshal([]byte(values), &m); err != nil {
		return err
	}
	for k := range m {
		*value = m[k]
		if k == lang {
			return nil
		}
	}
	return nil
}

func GenOpusUpdates(userID, versionID, summary, summaryI18n, displayName, displayNameI18n, logoURL string, isDefault bool) map[string]interface{} {
	var updates = map[string]interface{}{
		"updater_id":        userID,
		"latest_version_id": versionID,
	}
	if isDefault {
		updates["default_version_id"] = versionID
		updates["summary"] = summary
		if summaryI18n != "" {
			updates["summary_i18n"] = summaryI18n
		}
		updates["display_name"] = displayName
		if displayNameI18n != "" {
			updates["display_name_i18n"] = displayNameI18n
		}
		updates["logo_url"] = logoURL
	}
	return updates
}

func GenPresentationFromReq(opusID, versionID string, common model.Common, req *pb.PutOnExtensionsReq) model.OpusPresentation {
	return model.OpusPresentation{
		Common:          common,
		OpusID:          opusID,
		VersionID:       versionID,
		Ref:             "",
		Desc:            req.GetDesc(),
		DescI18n:        req.GetDescI18N(),
		ContactName:     req.GetContactName(),
		ContactURL:      req.GetContactURL(),
		ContactEmail:    req.GetContactEmail(),
		IsOpenSourced:   req.GetIsOpenSourced(),
		OpensourceURL:   req.GetOpensourceURL(),
		LicenseName:     req.GetLicenseName(),
		LicenseURL:      req.GetLicenseURL(),
		HomepageName:    req.GetHomepageName(),
		HomepageURL:     req.GetHomepageURL(),
		HomepageLogoURL: req.GetHomepageLogoURL(),
		IsDownloadable:  req.GetIsDownloadable(),
		DownloadURL:     req.GetDownloadURL(),
		I18n:            req.GetI18N(),
	}
}
