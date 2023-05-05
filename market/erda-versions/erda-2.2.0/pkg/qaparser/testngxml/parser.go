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

package testngxml

import (
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-proto-go/dop/qa/unittest/pb"
	"github.com/erda-project/erda/pkg/cloudstorage"
	"github.com/erda-project/erda/pkg/qaparser"
	"github.com/erda-project/erda/pkg/qaparser/types"
)

type NgParser struct {
}

func init() {
	logrus.Info("register NGTest Parser to manager")
	(NgParser{}).Register()
}

func (ng NgParser) Register() {
	qaparser.Register(ng, types.NGTest)
}

// parse xml to entity
// 1. get file from cloud storage
// 2. parse
func (NgParser) Parse(endpoint, ak, sk, bucket, objectName string) ([]*pb.TestSuite, error) {
	client, err := cloudstorage.New(endpoint, ak, sk)
	if err != nil {
		return nil, errors.Wrap(err, "get cloud storage client")
	}

	byteArray, err := client.DownloadFile(bucket, objectName)
	if err != nil {
		return nil, errors.Wrapf(err, "download filename=%s", objectName)
	}

	var ng *NgTestResult
	if ng, err = Ingest(byteArray); err != nil {
		return nil, err
	}

	return ng.Transfer()
}
