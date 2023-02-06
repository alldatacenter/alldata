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

package minioclient

//import (
//	"testing"
//
//	"github.com/sirupsen/logrus"
//	"github.com/stretchr/testify/assert"
//)
//
//func before() *MinioClient {
//	c, err := New("127.0.0.1:9009", "accesskey", "secretkey")
//	if err != nil {
//		panic(err)
//	}
//	return c
//}
//
//func TestNewClient(t *testing.T) {
//	c, err := New("127.0.0.1:9000", "accesskey", "secretkey")
//	assert.Nil(t, err)
//	assert.Nil(t, c.HealthCheck())
//}
//
//func TestClient_GetFileUrl(t *testing.T) {
//	c := before()
//	url, err := c.GetFileUrl("test1", "001.png")
//	assert.Nil(t, err)
//	assert.NotEmpty(t, url)
//}
//
//func TestClient_UploadFile(t *testing.T) {
//	c := before()
//	url, err := c.UploadFile("test1", "d1.xml", "../testdata/d1.xml")
//	assert.Nil(t, err)
//	assert.NotEmpty(t, url)
//}
//
//func TestMinioClient_DownloadFile(t *testing.T) {
//	c := before()
//	bs, err := c.DownloadFile("test1", "TEST-surefile.xml")
//	assert.Nil(t, err)
//	logrus.Info(string(bs))
//}
