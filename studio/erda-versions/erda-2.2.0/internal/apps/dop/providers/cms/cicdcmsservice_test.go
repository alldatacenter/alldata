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
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/alecthomas/assert"

	cmspb "github.com/erda-project/erda-proto-go/core/pipeline/cms/pb"
)

type CmsServiceServerImpl struct {
}

func (c *CmsServiceServerImpl) CreateNs(ctx context.Context, request *cmspb.CmsCreateNsRequest) (*cmspb.CmsCreateNsResponse, error) {
	panic("implement me")
}

func (c *CmsServiceServerImpl) ListCmsNs(ctx context.Context, request *cmspb.CmsListNsRequest) (*cmspb.CmsListNsResponse, error) {
	panic("implement me")
}

func (c *CmsServiceServerImpl) UpdateCmsNsConfigs(ctx context.Context, request *cmspb.CmsNsConfigsUpdateRequest) (*cmspb.CmsNsConfigsUpdateResponse, error) {
	panic("implement me")
}

func (c *CmsServiceServerImpl) DeleteCmsNsConfigs(ctx context.Context, request *cmspb.CmsNsConfigsDeleteRequest) (*cmspb.CmsNsConfigsDeleteResponse, error) {
	panic("implement me")
}

func (c *CmsServiceServerImpl) GetCmsNsConfigs(ctx context.Context, request *cmspb.CmsNsConfigsGetRequest) (*cmspb.CmsNsConfigsGetResponse, error) {
	panic("implement me")
}

func (c *CmsServiceServerImpl) BatchGetCmsNsConfigs(ctx context.Context, request *cmspb.CmsNsConfigsBatchGetRequest) (*cmspb.CmsNsConfigsBatchGetResponse, error) {
	panic("implement me")
}

func TestCICDCmsService_deleteNotNeedKeys(t *testing.T) {
	type fields struct {
		p *provider
	}
	type args struct {
		ctx            context.Context
		pipelineSource string
		ns             string
		keys           []string
	}
	tests := []struct {
		name       string
		fields     fields
		args       args
		wantErr    bool
		deleteKeys []string
		dbKeys     []string
	}{
		{
			name: "test key",
			fields: fields{
				p: &provider{},
			},
			args: args{
				pipelineSource: "source",
				ns:             "ns",
				ctx:            context.Background(),
				keys:           []string{"a", "c"},
			},
			wantErr:    false,
			deleteKeys: []string{"b", "d"},
			dbKeys:     []string{"a", "b", "c", "d"},
		},

		{
			name: "not find delete keys",
			fields: fields{
				p: &provider{},
			},
			args: args{
				pipelineSource: "source",
				ns:             "ns",
				ctx:            context.Background(),
				keys:           []string{"a", "c"},
			},
			wantErr:    false,
			deleteKeys: []string{},
			dbKeys:     []string{"a", "c"},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var cms = CmsServiceServerImpl{}
			patch := monkey.PatchInstanceMethod(reflect.TypeOf(&cms), "GetCmsNsConfigs", func(cms *CmsServiceServerImpl, ctx context.Context, req *cmspb.CmsNsConfigsGetRequest) (*cmspb.CmsNsConfigsGetResponse, error) {
				assert.Equal(t, req.PipelineSource, tt.args.pipelineSource)
				assert.Equal(t, req.Ns, tt.args.ns)

				var configs []*cmspb.PipelineCmsConfig
				for _, key := range tt.dbKeys {
					configs = append(configs, &cmspb.PipelineCmsConfig{
						Key: key,
					})
				}
				return &cmspb.CmsNsConfigsGetResponse{
					Data: configs,
				}, nil
			})
			defer patch.Unpatch()

			patch1 := monkey.PatchInstanceMethod(reflect.TypeOf(&cms), "DeleteCmsNsConfigs", func(cms *CmsServiceServerImpl, ctx context.Context, req *cmspb.CmsNsConfigsDeleteRequest) (*cmspb.CmsNsConfigsDeleteResponse, error) {
				assert.Equal(t, req.PipelineSource, tt.args.pipelineSource)
				assert.Equal(t, req.Ns, tt.args.ns)
				assert.Equal(t, req.DeleteKeys, tt.deleteKeys)
				return &cmspb.CmsNsConfigsDeleteResponse{}, nil
			})
			defer patch1.Unpatch()

			tt.fields.p.PipelineCms = &cms
			s := &CICDCmsService{
				p: tt.fields.p,
			}
			if err := s.deleteNotNeedKeys(tt.args.ctx, tt.args.pipelineSource, tt.args.ns, tt.args.keys); (err != nil) != tt.wantErr {
				t.Errorf("deleteNotNeedKeys() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
