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

package proxy

import (
	"github.com/rancher/apiserver/pkg/apierror"
	"github.com/rancher/apiserver/pkg/types"
	"github.com/rancher/wrangler/pkg/schemas/validation"
	"k8s.io/apimachinery/pkg/api/errors"
)

type errorStore struct {
	types.Store
}

func (e *errorStore) ByID(apiOp *types.APIRequest, schema *types.APISchema, id string) (types.APIObject, error) {
	data, err := e.Store.ByID(apiOp, schema, id)
	return data, translateError(err)
}

func (e *errorStore) List(apiOp *types.APIRequest, schema *types.APISchema) (types.APIObjectList, error) {
	data, err := e.Store.List(apiOp, schema)
	return data, translateError(err)
}

func (e *errorStore) Create(apiOp *types.APIRequest, schema *types.APISchema, data types.APIObject) (types.APIObject, error) {
	data, err := e.Store.Create(apiOp, schema, data)
	return data, translateError(err)

}

func (e *errorStore) Update(apiOp *types.APIRequest, schema *types.APISchema, data types.APIObject, id string) (types.APIObject, error) {
	data, err := e.Store.Update(apiOp, schema, data, id)
	return data, translateError(err)

}

func (e *errorStore) Delete(apiOp *types.APIRequest, schema *types.APISchema, id string) (types.APIObject, error) {
	data, err := e.Store.Delete(apiOp, schema, id)
	return data, translateError(err)

}

func (e *errorStore) Watch(apiOp *types.APIRequest, schema *types.APISchema, wr types.WatchRequest) (chan types.APIEvent, error) {
	data, err := e.Store.Watch(apiOp, schema, wr)
	return data, translateError(err)
}

func translateError(err error) error {
	if apiError, ok := err.(errors.APIStatus); ok {
		status := apiError.Status()
		return apierror.NewAPIError(validation.ErrorCode{
			Status: int(status.Code),
			Code:   string(status.Reason),
		}, status.Message)
	}
	return err
}
