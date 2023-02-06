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

	"github.com/erda-project/erda/internal/tools/kms/endpoints/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/kms/kmstypes"
)

func (e *Endpoints) KmsEncrypt(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var req kmstypes.EncryptRequest
	if err := e.parseRequestBody(r, &req); err != nil {
		return err.ToResp(), nil
	}

	plugin, err := e.getPluginByKeyID(req.KeyID)
	if err != nil {
		return apierrors.ErrEncrypt.InternalError(err).ToResp(), nil
	}
	encryptResp, err := plugin.Encrypt(ctx, &req)
	if err != nil {
		return apierrors.ErrEncrypt.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(encryptResp)
}

func (e *Endpoints) KmsDecrypt(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var req kmstypes.DecryptRequest
	if err := e.parseRequestBody(r, &req); err != nil {
		return err.ToResp(), nil
	}

	plugin, err := e.getPluginByKeyID(req.KeyID)
	if err != nil {
		return apierrors.ErrDecrypt.InternalError(err).ToResp(), nil
	}
	decryptResp, err := plugin.Decrypt(ctx, &req)
	if err != nil {
		return apierrors.ErrDecrypt.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(decryptResp)
}

func (e *Endpoints) KmsGenerateDataKey(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var req kmstypes.GenerateDataKeyRequest
	if err := e.parseRequestBody(r, &req); err != nil {
		return err.ToResp(), nil
	}

	plugin, err := e.getPluginByKeyID(req.KeyID)
	if err != nil {
		return apierrors.ErrGenerateDataKey.InvalidParameter(err).ToResp(), nil
	}
	generateResp, err := plugin.GenerateDataKey(ctx, &req)
	if err != nil {
		return apierrors.ErrGenerateDataKey.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(generateResp)
}

func (e *Endpoints) KmsRotateKeyVersion(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var req kmstypes.RotateKeyVersionRequest
	if err := e.parseRequestBody(r, &req); err != nil {
		return err.ToResp(), nil
	}

	plugin, err := e.getPluginByKeyID(req.KeyID)
	if err != nil {
		return apierrors.ErrRotateKeyVersion.InvalidParameter(err).ToResp(), nil
	}
	rotateResp, err := plugin.RotateKeyVersion(ctx, &req)
	if err != nil {
		return apierrors.ErrRotateKeyVersion.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(rotateResp)
}
