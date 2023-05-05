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

package handler

import (
	"context"
	"encoding/json"
	"fmt"
	"sort"
	"strconv"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/apps/gallery/pb"
	commonPb "github.com/erda-project/erda-proto-go/common/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/gallery/apierr"
	"github.com/erda-project/erda/internal/apps/gallery/cache"
	"github.com/erda-project/erda/internal/apps/gallery/dao"
	"github.com/erda-project/erda/internal/apps/gallery/model"
	"github.com/erda-project/erda/internal/apps/gallery/types"
	"github.com/erda-project/erda/pkg/common/apis"
)

type GalleryHandler struct {
	C    *cache.Cache
	L    *logrus.Entry
	Tran i18n.Translator
}

func (p *GalleryHandler) ListOpusTypes(ctx context.Context, _ *commonPb.VoidRequest) (*pb.ListOpusTypesRespData, error) {
	orgID, err := apis.GetIntOrgID(ctx)
	if err != nil {
		return nil, apierr.ListOpus.InvalidParameter("invalid orgID")
	}

	_, opuses, err := dao.ListOpuses(dao.Q(), dao.WhereOption("org_id = ? OR level = ?", orgID, types.OpusLevelSystem))
	opusCatalogNames := make(map[string][]*pb.CatalogInfo)
	langCodes := apis.Language(ctx)
	existed := make(map[string]map[string]struct{})
	for _, opus := range opuses {
		if opus.Type == types.OpusTypeArtifactsProject.String() {
			continue
		}
		if _, ok := existed[opus.Type]; !ok {
			existed[opus.Type] = make(map[string]struct{})
		}
		catalogName := p.Tran.Text(langCodes, "others")
		if opus.Catalog != "" {
			catalogName = p.Tran.Text(langCodes, opus.Catalog)
		}
		if _, ok := existed[opus.Type][opus.Catalog]; !ok {
			existed[opus.Type][opus.Catalog] = struct{}{}
			opusCatalogNames[opus.Type] = append(opusCatalogNames[opus.Type], &pb.CatalogInfo{
				Key:  opus.Catalog,
				Name: catalogName,
			})
		}
	}

	opusTypesList := ListOpusTypes(ctx, p.Tran)
	for i := range opusTypesList.List {
		children := opusCatalogNames[opusTypesList.List[i].Type]
		sort.Slice(children, func(i, j int) bool {
			return children[i].Key < children[j].Key
		})
		opusTypesList.List[i].Children = children
	}
	return opusTypesList, nil
}

func (p *GalleryHandler) ListOpus(ctx context.Context, req *pb.ListOpusReq) (*pb.ListOpusResp, error) {
	var l = p.L.WithField("func", "ListOpus")

	orgID, err := apis.GetIntOrgID(ctx)
	if err != nil {
		return nil, apierr.ListOpus.InvalidParameter("invalid orgID")
	}
	var pageSize, pageNo = AdjustPaging(req.GetPageSize(), req.GetPageNo())

	// prepare options for querying opuses
	// query opuses by options
	options := PrepareListOpusesOptions(orgID, req.GetType(), req.GetName(), req.GetKeyword(), pageSize, pageNo)
	total, opuses, err := dao.ListOpuses(dao.Q(), options...)
	if err != nil {
		l.WithError(err).Errorln("failed to Find opuses")
		return nil, apierr.ListOpus.InternalError(err)
	}
	if total == 0 {
		l.Warnln("not found")
		return new(pb.ListOpusResp), nil
	}

	return ComposeListOpusResp(ctx, total, opuses, p.Tran), nil
}

func (p *GalleryHandler) ListOpusVersions(ctx context.Context, req *pb.ListOpusVersionsReq) (*pb.ListOpusVersionsResp, error) {
	var l = p.L.WithField("func", "listOpusVersions").WithField("opusID", req.GetOpusID())

	// query opus
	opus, ok, err := dao.GetOpusByID(dao.Q(), req.GetOpusID())
	if err != nil {
		l.WithError(err).Errorln("failed to GetOpusByID")
		return nil, apierr.ListOpusVersions.InternalError(err)
	}
	if !ok {
		return nil, apierr.ListOpusVersions.NotFound()
	}

	// query versions
	total, versions, err := dao.ListVersions(dao.Q(), dao.WhereOption("opus_id = ?", req.GetOpusID()))
	if err != nil {
		l.WithError(err).Errorln("failed to Find versions")
		return nil, apierr.ListOpusVersions.InternalError(err)
	}
	if total == 0 {
		l.WithError(err).Errorln("opus's version not found")
		return nil, apierr.ListOpusVersions.NotFound()
	}

	// query presentations
	_, presentations, err := dao.ListPresentations(dao.Q(), dao.WhereOption("opus_id = ?", req.GetOpusID()))
	if err != nil {
		l.WithError(err).Errorln("failed to Find presentations")
		return nil, apierr.ListOpusVersions.InternalError(err)
	}

	// query readmes
	_, readmes, err := dao.ListReadmes(dao.Q(), dao.WhereOption("opus_id = ?", req.GetOpusID()))
	if err != nil {
		l.WithError(err).Errorln("failed to Find readmes")
		return nil, apierr.ListOpusVersions.InternalError(err)
	}

	var resp pb.ListOpusVersionsResp
	lang := AdjustLang(apis.GetLang(ctx))
	ComposeListOpusVersionRespWithOpus(lang, &resp, opus)
	if err = ComposeListOpusVersionRespWithVersions(lang, &resp, versions); err != nil {
		l.WithError(err).Errorln("failed to ComposeListOpusVersionRespWithVersions")
	}
	ComposeListOpusVersionRespWithPresentations(lang, &resp, presentations)
	ComposeListOpusVersionRespWithReadmes(&resp, AdjustLang(apis.GetLang(ctx)), readmes)
	return &resp, nil
}

func (p *GalleryHandler) PutOnArtifacts(ctx context.Context, req *pb.PutOnArtifactsReq) (*pb.PutOnOpusResp, error) {
	l := p.L.WithField("func", "PutOnArtifacts").
		WithField("name", req.GetName()).
		WithField("version", req.GetVersion())

	// check parameters and get org info and user info
	orgDto, userID, err := p.putOnArtifactsPreCheck(ctx, req)
	if err != nil {
		l.WithError(err).Errorln("failed to putOnArtifactsPreCheck")
		return nil, err
	}

	tx := dao.Begin()
	defer tx.CommitOrRollback()

	var common = model.Common{
		OrgID:     uint32(orgDto.ID),
		OrgName:   orgDto.Name,
		CreatorID: userID,
		UpdaterID: userID,
	}

	// get the opus by options, if the opus does not exist, create it
	opus, err := p.putOnArtifactsGetOrCreateOpus(tx, common, req)
	if err != nil {
		l.WithError(err).Errorln("failed to putOnArtifactsGetOrCreateOpus")
		return nil, err
	}

	// get the version by options, if the version exist, return 'already exists' or else create it
	version, err := p.putOnArtifactsCreateVersion(tx, opus.ID.String, common, req)
	if err != nil {
		l.WithError(err).Errorln("failed to putOnArtifactsCreateVersion")
		return nil, err
	}

	// create presentation
	if _, err = p.putOnArtifactsCreatePresentation(tx, opus.ID.String, version.ID.String, common, req); err != nil {
		l.WithError(err).Errorln("failed to putOnArtifactsCreatePresentation")
		return nil, err
	}

	// create readmes
	if _, err = p.putOnArtifactsCreateReadmes(tx, opus.ID.String, version.ID.String, common, req); err != nil {
		l.WithError(err).Errorln("failed to putOnArtifactsCreateReadmes")
		return nil, err
	}

	// create installation
	if _, err = p.putOnArtifactsCreateInstallation(tx, opus.ID.String, version.ID.String, common, req); err != nil {
		l.WithError(err).Errorln("failed to putOnArtifactsCreateInstallation")
		return nil, err
	}

	// update opus
	updates := GenOpusUpdates(userID, version.ID.String, req.GetSummary(), "", req.GetDisplayName(), "", req.GetLogoURL(), true)
	if err = tx.Updates(&opus, updates, dao.ByIDOption(opus.ID)); err != nil {
		l.WithError(err).Errorln("failed to Updates opus")
		return nil, apierr.PutOnArtifacts.InternalError(err)
	}

	return &pb.PutOnOpusResp{OpusID: version.OpusID, VersionID: version.ID.String}, nil
}

func (p *GalleryHandler) PutOffArtifacts(ctx context.Context, req *pb.PutOffArtifactsReq) (*commonPb.VoidResponse, error) {
	l := p.L.WithField("func", "PutOffArtifacts").
		WithField("opus_id", req.GetOpusID()).
		WithField("version_id", req.GetVersionID())

	// check parameters and get org info and user info
	var userID = apis.GetUserID(ctx)
	if userID == "" {
		userID = req.GetUserID()
	}
	if userID == "" {
		return nil, apierr.PutOffArtifacts.InvalidParameter("missing userID")
	}

	// query version
	var byIDOption = dao.ByIDOption(req.GetVersionID())
	version, ok, err := dao.GetOpusVersion(dao.Q(), byIDOption)
	if err != nil {
		l.WithError(err).Errorln("failed to GetOpusVersion")
		return nil, apierr.PutOffArtifacts.InternalError(err)
	}
	if !ok {
		l.WithError(err).Warnln("delete version not found")
		return new(commonPb.VoidResponse), nil
	}
	if req.GetOpusID() != version.OpusID {
		return nil, apierr.PutOffArtifacts.InvalidParameter("invalid opusID and versionID")
	}

	tx := dao.Begin()
	defer tx.CommitOrRollback()

	total, _, err := dao.ListVersions(tx, dao.MapOption(map[string]interface{}{"opus_id": req.GetOpusID()}))
	if err != nil {
		l.WithError(err).Errorln("failed to ListVersions")
		return nil, apierr.PutOffArtifacts.InternalError(err)
	}
	if err = tx.Delete(new(model.OpusVersion), byIDOption); err != nil {
		l.WithError(err).Errorln("failed to Delete version")
		return nil, apierr.PutOffArtifacts.InternalError(err)
	}
	var versionIDOption = dao.MapOption(map[string]interface{}{"version_id": req.GetVersionID()})
	if err = tx.Delete(new(model.OpusPresentation), versionIDOption); err != nil {
		l.WithError(err).Errorln("failed to Delete presentation")
		return nil, apierr.PutOffArtifacts.InternalError(err)
	}
	if err = tx.Delete(new(model.OpusReadme), versionIDOption); err != nil {
		l.WithError(err).Errorln("failed to Delete readme")
		return nil, apierr.PutOffArtifacts.InternalError(err)
	}
	if err = tx.Delete(new(model.OpusInstallation), versionIDOption); err != nil {
		l.WithError(err).Errorln("failed to Delete installation")
		return nil, apierr.PutOffArtifacts.InternalError(err)
	}
	if total == 1 {
		if err = tx.Delete(new(model.Opus), dao.ByIDOption(req.GetOpusID())); err != nil {
			l.WithError(err).Errorln("failed to Delete opus")
			return nil, apierr.PutOffArtifacts.InternalError(err)
		}
	}

	return new(commonPb.VoidResponse), nil
}

func (p *GalleryHandler) PutOnExtensions(ctx context.Context, req *pb.PutOnExtensionsReq) (*pb.PutOnOpusResp, error) {
	l := p.L.WithField("func", "PutOnExtensions").WithFields(map[string]interface{}{
		"orgID":   req.GetOrgID(),
		"type":    req.GetType(),
		"name":    req.GetName(),
		"version": req.GetVersion(),
		"level":   req.GetLevel(),
		"mode":    req.GetMode(),
	})

	// check parameters and get org info and user info
	var orgID, err = apis.GetIntOrgID(ctx)
	if err != nil {
		orgID = int64(req.GetOrgID())
	}
	if orgID == 0 && types.OpusLevelOrg.Equal(req.GetLevel()) {
		return nil, apierr.PutOnExtension.MissingParameter("missing orgID")
	}
	var orgName string
	if orgID != 0 {
		orgDTO, ok := p.C.GetOrgByOrgID(strconv.FormatUint(uint64(orgID), 10))
		if !ok {
			return nil, apierr.PutOnExtension.InvalidParameter("invalid orgID")
		}
		orgName = orgDTO.Name
	}

	var userID = apis.GetUserID(ctx)
	if userID == "" {
		userID = req.GetUserID()
	}
	if userID == "" && (types.OpusLevelOrg.Equal(req.GetLevel())) {
		return nil, apierr.PutOnExtension.InvalidParameter("missing userID")
	}
	if types.OpusTypeExtensionAddon.Equal(req.GetType()) && types.OpusTypeExtensionAction.Equal(req.GetType()) {
		return nil, apierr.PutOnExtension.InvalidParameter("invalid type: " + req.GetType())
	}
	if req.GetName() == "" {
		return nil, apierr.PutOnExtension.InvalidParameter("missing name")
	}
	if req.GetVersion() == "" {
		return nil, apierr.PutOnExtension.InvalidParameter("missing version")
	}
	if types.OpusLevelSystem.Equal(req.GetLevel()) && types.OpusLevelOrg.Equal(req.GetLevel()) {
		return nil, apierr.PutOnExtension.InvalidParameter("invalid level: " + req.GetLevel())
	}

	// Check if extension already exist
	var where = map[string]interface{}{
		"type":  req.GetType(),
		"name":  req.GetName(),
		"level": req.GetLevel(),
	}
	if types.OpusLevelOrg.Equal(req.GetLevel()) {
		where["org_id"] = orgID
	}
	opus, ok, err := dao.GetOpus(dao.Q(), dao.MapOption(where))
	if err != nil {
		l.WithError(err).WithFields(where).Errorln("failed to First opus")
		return nil, apierr.PutOnExtension.InternalError(err)
	}
	if !ok {
		return p.createExtensions(ctx, l, uint32(orgID), orgName, userID, nil, req)
	}

	where = map[string]interface{}{
		"opus_id": opus.ID,
		"version": req.GetVersion(),
	}
	version, ok, err := dao.GetOpusVersion(dao.Q(), dao.MapOption(where))
	if err != nil {
		l.WithError(err).WithFields(where).Errorln("failed to First version")
		return nil, apierr.PutOnExtension.InternalError(err)
	}
	if !ok {
		return p.createExtensions(ctx, l, opus.OrgID, opus.OrgName, userID, opus, req)
	}

	if types.PutOnOpusModeAppend.Equal(req.GetMode()) {
		l.Warnln("failed to put on extension, the version already exists, can not append")
		return nil, apierr.PutOnExtension.AlreadyExists()
	}
	return p.updateExtension(ctx, l, userID, opus, version, req)
}

func (p *GalleryHandler) putOnArtifactsPreCheck(ctx context.Context, req *pb.PutOnArtifactsReq) (orgDto *apistructs.OrgDTO, userID string, err error) {
	orgID, err := apis.GetIntOrgID(ctx)
	if err != nil {
		orgID = int64(req.GetOrgID())
	}
	userID = apis.GetUserID(ctx)
	if userID == "" {
		userID = req.GetUserID()
	}
	if userID == "" {
		return nil, "", apierr.PutOnArtifacts.InvalidParameter("missing userID")
	}
	orgDto, ok := p.C.GetOrgByOrgID(strconv.FormatInt(orgID, 10))
	if !ok {
		return nil, "", apierr.PutOnArtifacts.InvalidParameter(fmt.Sprintf("invalid orgID: %d", orgID))
	}
	if req.GetName() == "" {
		return nil, "", apierr.PutOnArtifacts.InvalidParameter("missing Opus name")
	}
	if req.GetVersion() == "" {
		return nil, "", apierr.PutOnArtifacts.InvalidParameter("missing Opus version")
	}
	return orgDto, userID, nil
}

func (p *GalleryHandler) putOnArtifactsGetOrCreateOpus(tx *dao.TX, common model.Common, req *pb.PutOnArtifactsReq) (*model.Opus, error) {
	opus, ok, err := dao.GetOpus(tx, dao.MapOption(map[string]interface{}{
		"org_id": common.OrgID,
		"type":   types.OpusTypeArtifactsProject,
		"name":   req.GetName(),
	}))
	if err != nil {
		return nil, apierr.PutOnArtifacts.InternalError(errors.Wrap(err, "failed to GetOpus"))
	}
	if ok {
		return opus, nil
	}

	opus = &model.Opus{
		Common:      common,
		Level:       string(types.OpusLevelOrg),
		Type:        string(types.OpusTypeArtifactsProject),
		Name:        req.GetName(),
		DisplayName: req.GetDisplayName(),
		Summary:     req.GetSummary(),
		LogoURL:     req.GetLogoURL(),
		Catalog:     req.GetCatalog(),
	}
	if err = tx.Create(opus); err != nil {
		return nil, apierr.PutOnArtifacts.InternalError(errors.Wrap(err, "failed to Create opus"))
	}
	return opus, nil
}

func (p *GalleryHandler) putOnArtifactsCreateVersion(tx *dao.TX, opusID string, common model.Common, req *pb.PutOnArtifactsReq) (*model.OpusVersion, error) {
	_, ok, err := dao.GetOpusVersion(tx, dao.MapOption(map[string]interface{}{
		"opus_id": opusID,
		"version": req.GetVersion(),
	}))
	if err != nil {
		return nil, apierr.PutOnArtifacts.InternalError(errors.Wrap(err, "failed to GetOpusVersion"))
	}
	if ok {
		return nil, apierr.PutOnArtifacts.AlreadyExists()
	}
	labels, _ := json.Marshal(req.GetLabels())
	var version = model.OpusVersion{
		Common:  common,
		OpusID:  opusID,
		Version: req.GetVersion(),
		Summary: req.GetSummary(),
		Labels:  string(labels),
		LogoURL: req.GetLogoURL(),
		IsValid: true,
	}
	if err = tx.Create(&version); err != nil {
		return nil, apierr.PutOnArtifacts.InternalError(errors.Wrap(err, "failed to Create version"))
	}
	return &version, nil
}

func (p *GalleryHandler) putOnArtifactsCreatePresentation(tx *dao.TX, opusID, versionID string, common model.Common, req *pb.PutOnArtifactsReq) (*model.OpusPresentation, error) {
	var presentation = model.OpusPresentation{
		Common:          common,
		OpusID:          opusID,
		VersionID:       versionID,
		Desc:            req.GetDesc(),
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
	}
	if err := tx.Create(&presentation); err != nil {
		return nil, apierr.PutOnArtifacts.InternalError(errors.Wrap(err, "failed to Create presentation"))
	}
	return &presentation, nil
}

func (p *GalleryHandler) putOnArtifactsCreateReadmes(tx *dao.TX, opusID, versionID string, common model.Common, req *pb.PutOnArtifactsReq) ([]*model.OpusReadme, error) {
	var readmes []*model.OpusReadme
	for _, item := range req.GetReadme() {
		readme := &model.OpusReadme{
			Model:     model.Model{},
			Common:    common,
			OpusID:    opusID,
			VersionID: versionID,
			Lang:      item.Lang,
			LangName:  item.LangName,
			Text:      item.Text,
		}
		readmes = append(readmes, readme)
	}
	if err := tx.CreateInBatches(&readmes, len(readmes)); err != nil {
		return nil, apierr.PutOnArtifacts.InternalError(errors.Wrap(err, "failed to CreateInBatches readmes"))
	}
	return readmes, nil
}

func (p *GalleryHandler) putOnArtifactsCreateInstallation(tx *dao.TX, opusID, versionID string, common model.Common, req *pb.PutOnArtifactsReq) (*model.OpusInstallation, error) {
	spec, err := json.Marshal(req.GetInstallation())
	if err != nil {
		return nil, apierr.PutOnArtifacts.InternalError(err)
	}
	var installation = model.OpusInstallation{
		Model:     model.Model{},
		Common:    common,
		OpusID:    opusID,
		VersionID: versionID,
		Installer: string(types.OpusTypeArtifactsProject),
		Spec:      string(spec),
	}
	if err = tx.Create(&installation); err != nil {
		return nil, apierr.PutOnArtifacts.InternalError(errors.Wrap(err, "failed to Create installation"))
	}
	return nil, err
}

func (p *GalleryHandler) updateExtension(ctx context.Context, l *logrus.Entry, userID string, opus *model.Opus, version *model.OpusVersion, req *pb.PutOnExtensionsReq) (*pb.PutOnOpusResp, error) {
	l = l.WithField("func", "updateExtension")

	var err error
	tx := dao.Begin()
	defer tx.CommitOrRollback()

	// update version
	labels, _ := json.Marshal(req.GetLabels())
	if err = tx.Updates(&version, map[string]interface{}{
		"summary":      req.GetSummary(),
		"summary_i18n": req.GetSummaryI18N(),
		"labels":       string(labels),
		"logo_url":     req.GetLogoURL(),
		"updater_id":   userID,
	}, dao.ByIDOption(version.ID)); err != nil {
		l.WithError(err).Errorln("failed to Updates")
		return nil, apierr.PutOnExtension.InternalError(err)
	}

	// update presentation
	if err = tx.Updates(new(model.OpusPresentation), map[string]interface{}{
		"desc":              req.GetDesc(),
		"desc_i18n":         req.GetDescI18N(),
		"contact_name":      req.GetContactName(),
		"contact_url":       req.GetContactURL(),
		"contact_email":     req.GetContactEmail(),
		"is_open_sourced":   req.GetIsOpenSourced(),
		"opensource_url":    req.GetOpensourceURL(),
		"license_name":      req.GetLicenseName(),
		"license_url":       req.GetLicenseURL(),
		"homepage_name":     req.GetHomepageName(),
		"homepage_url":      req.GetHomepageURL(),
		"homepage_logo_url": req.GetHomepageLogoURL(),
		"is_downloadable":   req.GetIsDownloadable(),
		"download_url":      req.GetDownloadURL(),
		"updater_id":        userID,
	}, dao.WhereOption("version_id = ?", version.ID)); err != nil {
		l.WithError(err).Errorln("failed to Updates presentation")
		return nil, apierr.PutOnExtension.InternalError(err)
	}

	// create or update readmes
	for _, item := range req.GetReadme() {
		if item.GetLang() == "" {
			continue
		}
		var where = map[string]interface{}{
			"version_id": version.ID,
			"lang":       item.GetLang(),
		}
		var option = dao.MapOption(where)
		var updates = map[string]interface{}{
			"lang_name": types.LangTypes[types.Lang(item.GetLang())],
			"text":      item.GetText(),
		}
		_, ok, err := dao.GetReadme(tx, option)
		if err != nil {
			l.WithError(err).WithFields(where).Errorln("failed to First readme")
			return nil, apierr.PutOnExtension.InternalError(err)
		}
		if ok {
			if err = tx.Updates(new(model.OpusReadme), updates, option); err != nil {
				l.WithError(err).WithField("lang", item.GetLang()).Errorln("failed to Updates readme")
				return nil, apierr.PutOnExtension.InternalError(err)
			}
		} else {
			if err = tx.Create(&model.OpusReadme{
				Common: model.Common{
					OrgID:     opus.OrgID,
					OrgName:   opus.OrgName,
					CreatorID: userID,
					UpdaterID: userID,
				},
				OpusID:    version.OpusID,
				VersionID: version.ID.String,
				Lang:      item.GetLang(),
				LangName:  types.LangTypes[types.Lang(item.GetLang())],
				Text:      item.GetText(),
			}); err != nil {
				l.WithError(err).WithField("lang", item.GetLang()).Errorln("failed to Create readme")
				return nil, apierr.PutOnExtension.InternalError(err)
			}
		}
	}

	// update opus
	updates := GenOpusUpdates(userID, version.ID.String, req.GetSummary(), req.GetSummaryI18N(), req.GetDisplayName(),
		req.GetDisplayNameI18N(), req.GetLogoURL(), req.GetIsDefault() || opus.DefaultVersionID == "")
	if err = tx.Updates(opus, updates, dao.ByIDOption(opus.ID)); err != nil {
		l.WithError(err).Errorln("failed to Updates opus")
		return nil, apierr.PutOnExtension.InternalError(err)
	}

	return &pb.PutOnOpusResp{OpusID: version.OpusID, VersionID: version.ID.String}, nil
}

func (p *GalleryHandler) createExtensions(ctx context.Context, l *logrus.Entry, orgID uint32, orgName, userID string, opus *model.Opus, req *pb.PutOnExtensionsReq) (*pb.PutOnOpusResp, error) {
	l = l.WithField("func", "createExtensions")

	var err error
	tx := dao.Begin()
	defer tx.CommitOrRollback()

	// create version
	common := model.Common{
		OrgID:     orgID,
		OrgName:   orgName,
		CreatorID: userID,
		UpdaterID: userID,
	}

	if opus == nil {
		opus = &model.Opus{
			Model:           model.Model{},
			Common:          common,
			Level:           req.GetLevel(),
			Type:            req.GetType(),
			Name:            req.GetName(),
			DisplayName:     req.GetDisplayName(),
			DisplayNameI18n: req.GetDisplayNameI18N(),
			Summary:         req.GetSummary(),
			SummaryI18n:     req.GetSummaryI18N(),
			LogoURL:         req.GetLogoURL(),
			Catalog:         req.GetCatalog(),
		}
		if err = tx.Create(opus); err != nil {
			l.WithError(err).Errorln("failed to Create opus")
			return nil, apierr.PutOnExtension.InternalError(err)
		}
	}
	labels, _ := json.Marshal(req.GetLabels())
	var version = model.OpusVersion{
		Common:      common,
		OpusID:      opus.ID.String,
		Version:     req.GetVersion(),
		Summary:     req.GetSummary(),
		SummaryI18n: req.GetSummaryI18N(),
		Labels:      string(labels),
		LogoURL:     req.GetLogoURL(),
		IsValid:     true,
	}
	if err = tx.Create(&version); err != nil {
		l.WithError(err).Errorln("failed to Create version")
		return nil, apierr.PutOnExtension.InternalError(err)
	}

	// create presentation
	var presentation = GenPresentationFromReq(opus.ID.String, version.ID.String, common, req)
	if err = tx.Create(&presentation); err != nil {
		l.WithError(err).Errorln("failed to Create presentation")
		return nil, apierr.PutOnExtension.InternalError(err)
	}

	// create readmes
	var readmes []*model.OpusReadme
	for _, item := range req.GetReadme() {
		if item.GetLang() == "" {
			continue
		}
		readme := model.OpusReadme{
			Common:    common,
			OpusID:    opus.ID.String,
			VersionID: version.ID.String,
			Lang:      item.GetLang(),
			LangName:  types.LangTypes[types.Lang(item.GetLang())],
			Text:      item.GetText(),
		}
		readmes = append(readmes, &readme)
	}
	if err = tx.CreateInBatches(readmes, len(readmes)); err != nil {
		l.WithError(err).Errorln("failed to CreateInBatches readmes")
		return nil, apierr.PutOnExtension.InternalError(err)
	}

	// update opus
	updates := GenOpusUpdates(userID, version.ID.String, req.GetSummary(), req.GetSummaryI18N(),
		req.GetDisplayName(), req.GetDisplayNameI18N(), req.GetLogoURL(), req.GetIsDefault())
	if err = tx.Updates(opus, updates, dao.ByIDOption(opus.ID)); err != nil {
		l.WithError(err).Errorln("failed to Updates opus")
		return nil, apierr.PutOnExtension.InternalError(err)
	}

	return &pb.PutOnOpusResp{OpusID: version.OpusID, VersionID: version.ID.String}, nil
}
