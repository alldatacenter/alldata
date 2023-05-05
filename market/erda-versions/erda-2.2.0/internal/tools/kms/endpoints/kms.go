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

package endpoints

import (
	"context"
	"net/http"

	"github.com/erda-project/erda/internal/tools/kms/conf"
	"github.com/erda-project/erda/internal/tools/kms/endpoints/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/kms/kmstypes"
)

func (e *Endpoints) KmsCreateKey(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var req kmstypes.CreateKeyRequest
	if err := e.parseRequestBody(r, &req); err != nil {
		return err.ToResp(), nil
	}

	plugin, err := e.KmsMgr.GetPlugin(req.PluginKind, conf.KmsStoreKind())
	if err != nil {
		return apierrors.ErrCreateKey.InternalError(err).ToResp(), nil
	}

	createKeyResp, err := plugin.CreateKey(ctx, &req)
	if err != nil {
		return apierrors.ErrCreateKey.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(createKeyResp)
}

func (e *Endpoints) KmsDescribeKey(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var req kmstypes.DescribeKeyRequest
	if err := e.parseRequestBody(r, &req); err != nil {
		return err.ToResp(), nil
	}

	plugin, err := e.getPluginByKeyID(req.KeyID)
	if err != nil {
		return apierrors.ErrDescribeKey.InternalError(err).ToResp(), nil
	}

	descResp, err := plugin.DescribeKey(ctx, &req)
	if err != nil {
		return apierrors.ErrDescribeKey.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(descResp)
}
