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

package cms

import (
	"context"
	"sort"

	"github.com/erda-project/erda-infra/providers/mysqlxorm"
	"github.com/erda-project/erda-proto-go/core/pipeline/cms/pb"
	"github.com/erda-project/erda/internal/tools/pipeline/services/apierrors"
	"github.com/erda-project/erda/pkg/common/apis"
)

type cmsService struct {
	p *provider

	cm ConfigManager
	db mysqlxorm.Interface
}

func getCtxWithSource(ctx context.Context, source string) context.Context {
	return context.WithValue(ctx, CtxKeyPipelineSource, source)
}

func (s *cmsService) CreateNs(ctx context.Context, req *pb.CmsCreateNsRequest) (*pb.CmsCreateNsResponse, error) {
	// authentication
	if !apis.IsInternalClient(ctx) {
		return nil, apierrors.ErrCreatePipelineCmsNs.AccessDenied()
	}

	ctx = getCtxWithSource(ctx, req.PipelineSource)
	if err := s.cm.IdempotentCreateNs(ctx, req.Ns); err != nil {
		return nil, err
	}

	return &pb.CmsCreateNsResponse{}, nil
}

func (s *cmsService) ListCmsNs(ctx context.Context, req *pb.CmsListNsRequest) (*pb.CmsListNsResponse, error) {
	// authentication
	if !apis.IsInternalClient(ctx) {
		return nil, apierrors.ErrListPipelineCmsNs.AccessDenied()
	}

	ctx = getCtxWithSource(ctx, req.PipelineSource)
	namespaces, err := s.cm.PrefixListNs(ctx, req.NsPrefix)
	if err != nil {
		return nil, err
	}

	var data []*pb.PipelineCmsNs
	for _, ns := range namespaces {
		data = append(data, &pb.PipelineCmsNs{
			PipelineSource: ns.PipelineSource,
			Ns:             ns.Ns,
			TimeCreated:    ns.TimeCreated,
			TimeUpdated:    ns.TimeUpdated,
		})
	}

	return &pb.CmsListNsResponse{Data: data}, nil
}

func (s *cmsService) UpdateCmsNsConfigs(ctx context.Context, req *pb.CmsNsConfigsUpdateRequest) (*pb.CmsNsConfigsUpdateResponse, error) {
	// authentication
	if !apis.IsInternalClient(ctx) {
		return nil, apierrors.ErrUpdatePipelineCmsConfigs.AccessDenied()
	}

	ctx = getCtxWithSource(ctx, req.PipelineSource)
	err := s.cm.UpdateConfigs(ctx, req.Ns, req.KVs)
	if err != nil {
		return nil, err
	}

	return &pb.CmsNsConfigsUpdateResponse{}, nil
}

func (s *cmsService) DeleteCmsNsConfigs(ctx context.Context, req *pb.CmsNsConfigsDeleteRequest) (*pb.CmsNsConfigsDeleteResponse, error) {
	// authentication
	if !apis.IsInternalClient(ctx) {
		return nil, apierrors.ErrDeletePipelineCmsConfigs.AccessDenied()
	}

	ctx = getCtxWithSource(ctx, req.PipelineSource)
	var opErr error
	// 删除 ns
	if req.DeleteNs {
		opErr = s.cm.IdempotentDeleteNs(ctx, req.Ns)
	} else {
		ctx = context.WithValue(ctx, CtxKeyForceDelete, req.DeleteForce)
		opErr = s.cm.DeleteConfigs(ctx, req.Ns, req.DeleteKeys...)
	}

	if opErr != nil {
		return nil, opErr
	}

	return &pb.CmsNsConfigsDeleteResponse{}, nil
}

func (s *cmsService) GetCmsNsConfigs(ctx context.Context, req *pb.CmsNsConfigsGetRequest) (*pb.CmsNsConfigsGetResponse, error) {
	// authentication
	if !apis.IsInternalClient(ctx) {
		return nil, apierrors.ErrGetPipelineCmsConfigs.AccessDenied()
	}

	ctx = getCtxWithSource(ctx, req.PipelineSource)
	configs, err := s.cm.GetConfigs(ctx, req.Ns, req.GlobalDecrypt, req.Keys...)
	if err != nil {
		return nil, err
	}
	results := make([]*pb.PipelineCmsConfig, 0, len(configs))
	for key, value := range configs {
		results = append(results, &pb.PipelineCmsConfig{
			Key:         key,
			Value:       value.Value,
			EncryptInDB: value.EncryptInDB,
			Type:        value.Type,
			Operations:  value.Operations,
			Comment:     value.Comment,
			From:        value.From,
			TimeCreated: value.TimeCreated,
			TimeUpdated: value.TimeUpdated,
		})
	}

	// order by timeCreated
	sortResult(results)

	return &pb.CmsNsConfigsGetResponse{Data: results}, nil
}

func (s *cmsService) BatchGetCmsNsConfigs(ctx context.Context, req *pb.CmsNsConfigsBatchGetRequest) (*pb.CmsNsConfigsBatchGetResponse, error) {
	if !apis.IsInternalClient(ctx) {
		return nil, apierrors.ErrGetPipelineCmsConfigs.AccessDenied()
	}

	result, err := s.cm.BatchGetConfigs(ctx, req)
	if err != nil {
		return nil, err
	}

	sortResult(result)

	return &pb.CmsNsConfigsBatchGetResponse{Configs: result}, nil
}

func sortResult(results []*pb.PipelineCmsConfig) {
	// order by timeCreated
	sort.SliceStable(results, func(i, j int) bool {
		if results[i].TimeCreated == nil {
			return true
		}
		if results[j].TimeCreated == nil {
			return false
		}
		return results[i].TimeCreated.AsTime().Before(results[j].TimeCreated.AsTime())
	})
}
