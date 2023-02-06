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
//	"fmt"
//	"os"
//	"testing"
//
//	"github.com/stretchr/testify/assert"
//
//	"github.com/erda-project/erda/apistructs"
//)
//
//func TestCreateDevOpsAPIAsset(t *testing.T) {
//	os.Setenv("APIM_ADDR", "localhost:3083")
//	b := New(WithAPIM())
//	assetID, err := b.CreateAPIAsset(apistructs.APIAssetCreateRequest{
//		AssetID: "devops-api-asset",
//		Versions: []apistructs.APIAssetVersionCreateRequest{
//			{
//				Major:            1, // 1
//				Minor:            0, // 0
//				Patch:            0, // 0
//				Desc:             "1.0.0 desc",
//				SpecProtocol:     apistructs.APISpecProtocolOAS2Json,
//				Spec:             "spec example",                     // spec 文本
//				SpecDiceFileUUID: "9b1223402dfa4643866dd4e19ee41d70", // spec from dice file uuid (与 spec 二选一)
//				Instances: []apistructs.APIAssetVersionInstanceCreateRequest{
//					{
//						InstanceType: apistructs.APIInstanceTypeService,
//						RuntimeID:    1,
//						ServiceName:  "service-a",
//					},
//				},
//			},
//		},
//		OrgID:     1,
//		ProjectID: 2,
//		AppID:     3,
//		IdentityInfo: apistructs.IdentityInfo{
//			UserID: "2",
//		},
//	})
//	assert.NoError(t, err)
//	fmt.Println(assetID)
//}
