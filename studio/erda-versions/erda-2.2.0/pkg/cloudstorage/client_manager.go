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

package cloudstorage

import (
	"fmt"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/cloudstorage/minioclient"
	"github.com/erda-project/erda/pkg/cloudstorage/ossclient"
)

type Client interface {
	UploadFile(bucketName, objectName, file string) (string, error)
	DownloadFile(bucketName, objectName string) ([]byte, error)
	GetFileUrl(bucketName, objectName string) (string, error)
	HealthCheck() error
}

func New(endpoint, accessKey, secretKey string) (Client, error) {
	var client Client
	var err error

	// return oss client if connected
	client, err = ossclient.New(endpoint, accessKey, secretKey)
	if err == nil {
		return client, nil
	}

	// return minio client if connected
	client, err = minioclient.New(endpoint, accessKey, secretKey)
	if err == nil {
		return client, nil
	}

	errMsg := fmt.Sprintf("get cloud storage failed. Either oss or minio client found by endpoint=%s", endpoint)
	logrus.Errorf(errMsg)
	return nil, errors.Errorf(errMsg)
}
