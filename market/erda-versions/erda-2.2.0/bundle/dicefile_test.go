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

package bundle

//import (
//	"os"
//	"testing"
//	"time"
//
//	"github.com/stretchr/testify/assert"
//
//	"github.com/erda-project/erda/apistructs"
//	"github.com/erda-project/erda/pkg/filehelper"
//)
//
//func TestDownloadDiceFile(t *testing.T) {
//	os.Setenv("CMDB_ADDR", "cmdb.default.svc.cluster.local:9093")
//	b := New(WithCMDB())
//	r, err := b.DownloadDiceFile("1e5c1abc142446699437fe3ba7e70245")
//	assert.NoError(t, err)
//	assert.NoError(t, filehelper.CreateFile2("/Users/sfwn/111.jpg", r, 0644))
//}
//
//func BenchmarkFileEOF(b *testing.B) {
//	os.Setenv("CMDB_ADDR", "cmdb.default.svc.cluster.local:9093")
//	bdl := New(WithAllAvailableClients())
//	fi, err := os.Stat("/tmp/bug.log")
//	assert.NoError(b, err)
//	f, err := os.Open("/tmp/bug.log")
//	defer f.Close()
//
//	for i := 0; i < b.N; i++ {
//		assert.NoError(b, err)
//		r, err := bdl.UploadFile(apistructs.FileUploadRequest{
//			FileNameWithExt: "bug.log",
//			ByteSize:        fi.Size(),
//			FileReader:      f,
//			From:            "bundle-test",
//			IsPublic:        true,
//			Encrypt:         false,
//			Creator:         "my",
//			ExpiredAt:       &[]time.Time{time.Now().Add(time.Hour * 24)}[0],
//		})
//		assert.NoError(b, err)
//		if err != nil {
//			b.Logf("upload failed, err: %v", err)
//		}
//
//		_, err = bdl.DownloadDiceFile(r.UUID)
//		assert.NoError(b, err)
//		if err != nil {
//			b.Logf("upload failed, err: %v", err)
//		}
//	}
//}
