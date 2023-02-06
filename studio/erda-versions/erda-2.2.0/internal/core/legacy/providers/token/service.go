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

package token

import (
	"context"
	"fmt"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/crypto/uuid"
	tokenstore "github.com/erda-project/erda/pkg/oauth2/tokenstore/mysqltokenstore"
	"github.com/erda-project/erda/pkg/secret"
)

type TokenService struct {
	logger logs.Logger

	dao Dao
}

func (s *TokenService) QueryTokens(ctx context.Context, req *pb.QueryTokensRequest) (*pb.QueryTokensResponse, error) {
	objs, total, err := s.dao.QueryToken(ctx, req)
	if err != nil {
		return nil, err
	}
	res := make([]*pb.Token, len(objs))
	for i, obj := range objs {
		res[i] = obj.ToPbToken()
	}
	return &pb.QueryTokensResponse{Data: res, Total: total}, nil
}

func (s *TokenService) GetToken(ctx context.Context, req *pb.GetTokenRequest) (*pb.GetTokenResponse, error) {
	obj, err := s.dao.GetToken(ctx, req)
	if err != nil {
		return nil, err
	}

	return &pb.GetTokenResponse{Data: obj.ToPbToken()}, nil
}

func (s *TokenService) CreateToken(ctx context.Context, req *pb.CreateTokenRequest) (*pb.CreateTokenResponse, error) {
	userID := apis.GetUserID(ctx)
	token, err := ToModelToken(userID, req)
	if err != nil {
		return nil, err
	}
	obj, err := s.dao.CreateToken(ctx, token)
	if err != nil {
		return nil, err
	}
	return &pb.CreateTokenResponse{Data: obj.ToPbToken()}, nil
}

func (s *TokenService) UpdateToken(ctx context.Context, req *pb.UpdateTokenRequest) (*pb.UpdateTokenResponse, error) {
	return &pb.UpdateTokenResponse{}, s.dao.UpdateToken(ctx, req)
}

func (s *TokenService) DeleteToken(ctx context.Context, req *pb.DeleteTokenRequest) (*pb.DeleteTokenResponse, error) {
	return &pb.DeleteTokenResponse{}, s.dao.DeleteToken(ctx, req)
}

func ToModelToken(userID string, req *pb.CreateTokenRequest) (tokenstore.TokenStoreItem, error) {
	tokenType := req.Type
	switch tokenType {
	case tokenstore.AccessKey.String():
		pair := secret.CreateAkSkPair()
		return tokenstore.TokenStoreItem{
			AccessKey:   pair.AccessKeyID,
			SecretKey:   pair.SecretKey,
			Description: req.Description,
			Scope:       req.Scope,
			ScopeId:     req.ScopeId,
			CreatorID:   req.CreatorId,
			Type:        req.Type,
		}, nil
	case tokenstore.PAT.String():
		if userID == "" {
			if req.Scope == string(apistructs.OrgScope) {
				userID = req.CreatorId
			} else {
				return tokenstore.TokenStoreItem{}, fmt.Errorf("user id is empty")
			}
		}
		return tokenstore.TokenStoreItem{
			AccessKey:   uuid.UUID(),
			Description: req.Description,
			Scope:       req.Scope,
			ScopeId:     req.ScopeId,
			CreatorID:   userID,
			Type:        req.Type,
		}, nil
	default:
		return tokenstore.TokenStoreItem{}, fmt.Errorf("token type: %s is not valid", tokenType)
	}
}
