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

package credential

import (
	"bytes"
	"context"
	"encoding/csv"
	"strconv"
	"strings"

	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda-proto-go/msp/credential/pb"
	tenantpb "github.com/erda-project/erda-proto-go/msp/tenant/pb"
	"github.com/erda-project/erda/internal/pkg/audit"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/common/errors"
	"github.com/erda-project/erda/pkg/oauth2/tokenstore/mysqltokenstore"
)

type accessKeyService struct {
	p *provider
}

func (a *accessKeyService) QueryAccessKeys(ctx context.Context, request *pb.QueryAccessKeysRequest) (*pb.QueryAccessKeysResponse, error) {
	req := &tokenpb.QueryTokensRequest{
		Access:   request.AccessKey,
		PageNo:   request.PageNo,
		PageSize: request.PageSize,
		Scope:    strings.ToLower(tokenpb.ScopeEnum_MSP_ENV.String()),
		ScopeId:  request.ScopeId,
	}
	accessKeyList, err := a.p.TokenService.QueryTokens(ctx, req)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	akList := make([]*pb.QueryAccessKeys, 0)
	var userIDs []string
	userIDMap := make(map[string]bool)
	for _, v := range accessKeyList.Data {
		ak := &pb.QueryAccessKeys{
			Id:        v.Id,
			Token:     v.AccessKey,
			CreatedAt: v.CreatedAt,
			Creator:   v.CreatorId,
		}
		userId := v.CreatorId
		if userId != "" && !userIDMap[userId] {
			userIDs = append(userIDs, userId)
			userIDMap[userId] = true
		}
		akList = append(akList, ak)
	}
	result := &pb.QueryAccessKeysResponse{
		Data: &pb.QueryAccessKeysData{
			List:  akList,
			Total: accessKeyList.Total,
		},
		UserIDs: userIDs,
	}
	return result, nil
}

func (a *accessKeyService) DownloadAccessKeyFile(ctx context.Context, request *pb.DownloadAccessKeyFileRequest) (*pb.DownloadAccessKeyFileResponse, error) {
	buf := &bytes.Buffer{}
	w := csv.NewWriter(buf)
	akRequest := &tokenpb.GetTokenRequest{
		Id: request.Id,
	}
	accessKey, err := a.p.TokenService.GetToken(ctx, akRequest)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	fileData := [][]string{
		{"secretKey", accessKey.Data.SecretKey},
		{"accessKey", accessKey.Data.AccessKey},
	}
	err = w.WriteAll(fileData)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	w.Flush()
	return &pb.DownloadAccessKeyFileResponse{
		Content: buf.Bytes(),
	}, nil
}

func (a *accessKeyService) CreateAccessKey(ctx context.Context, request *pb.CreateAccessKeyRequest) (*pb.CreateAccessKeyResponse, error) {
	userIdStr := apis.GetUserID(ctx)
	req := &tokenpb.CreateTokenRequest{
		Type:        mysqltokenstore.AccessKey.String(),
		Description: request.Description,
		Scope:       strings.ToLower(tokenpb.ScopeEnum_MSP_ENV.String()),
		ScopeId:     request.ScopeId,
		CreatorId:   userIdStr,
	}
	accessKey, err := a.p.TokenService.CreateToken(ctx, req)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	projectId, err := a.auditContextInfo(ctx, request.ScopeId, accessKey.Data.AccessKey)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	result := &pb.CreateAccessKeyResponse{
		Data: &pb.CreateAccessKeyData{
			Id:        accessKey.Data.AccessKey,
			ProjectId: projectId,
		},
	}
	return result, nil
}

func (a *accessKeyService) auditContextInfo(ctx context.Context, scopeId, token string) (uint64, error) {
	projectData, err := a.p.Tenant.GetTenantProject(context.Background(), &tenantpb.GetTenantProjectRequest{
		ScopeId: scopeId,
	})
	if err != nil {
		return 0, err
	}
	auditProjectId, err := strconv.Atoi(projectData.Data.ProjectId)
	if err != nil {
		return 0, err
	}
	project, err := a.p.bdl.GetProject(uint64(auditProjectId))
	if err != nil {
		return 0, err
	}
	auditContext := map[string]interface{}{
		"projectName": project.Name,
		"token":       token,
	}
	audit.ContextEntryMap(ctx, auditContext)
	return uint64(auditProjectId), nil
}

func (a *accessKeyService) DeleteAccessKey(ctx context.Context, request *pb.DeleteAccessKeyRequest) (*pb.DeleteAccessKeyResponse, error) {
	token, err := a.GetAccessKeyItem(ctx, request.Id)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	akRequest := &tokenpb.DeleteTokenRequest{
		Id: request.Id,
	}
	_, err = a.p.TokenService.DeleteToken(ctx, akRequest)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	projectId, err := a.auditContextInfo(ctx, token.ScopeId, token.AccessKey)
	return &pb.DeleteAccessKeyResponse{
		Data: projectId,
	}, nil
}

func (a *accessKeyService) GetAccessKey(ctx context.Context, request *pb.GetAccessKeyRequest) (*pb.GetAccessKeyResponse, error) {
	//akRequest := &akpb.GetAccessKeyRequest{
	//	Id: request.Id,
	//}
	//accessKey, err := a.p.AccessKeyService.GetAccessKey(ctx, akRequest)
	token, err := a.GetAccessKeyItem(ctx, request.Id)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	result := &pb.GetAccessKeyResponse{
		Data: &pb.AccessKeysItem{
			Id:          token.Id,
			AccessKey:   token.AccessKey,
			SecretKey:   token.SecretKey,
			Description: token.Description,
			CreatedAt:   token.CreatedAt,
			Scope:       token.Scope,
			ScopeId:     token.ScopeId,
			CreatorId:   token.CreatorId,
		},
	}
	return result, nil
}

func (a *accessKeyService) GetAccessKeyItem(ctx context.Context, Id string) (*tokenpb.Token, error) {
	akRequest := &tokenpb.GetTokenRequest{
		Id: Id,
	}
	accessKey, err := a.p.TokenService.GetToken(ctx, akRequest)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	return accessKey.Data, nil
}
