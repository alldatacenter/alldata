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

package actionagent

//import (
//	"bytes"
//	"encoding/base64"
//	"encoding/json"
//	"fmt"
//	"os"
//	"testing"
//
//	"github.com/stretchr/testify/require"
//)
//
//func TestExecute(t *testing.T) {
//	arg := NewAgentArgForPull(10000990, 3154)
//
//	os.Setenv(CONTEXTDIR, "/.pipeline/container/context")
//	os.Setenv(WORKDIR, "/.pipeline/container/context/custom")
//	os.Setenv(METAFILE, "/.pipeline/container/metadata/custom/metadata")
//	os.Setenv("DICE_OPENAPI_URL", "http://openapi.dev.terminus.io")
//	os.Setenv("DICE_OPENAPI_TOKEN", "111")
//	os.Setenv("XXX_PUBLIC_URL", "xxx public url")
//	os.Setenv("IS_EDGE_CLUSTER", "false")
//	os.Setenv("DICE_IS_EDGE", "false")
//
//	b, err := json.Marshal(arg)
//	require.NoError(t, err)
//
//	reqStr := base64.StdEncoding.EncodeToString(b)
//
//	agent := &Agent{}
//	agent.Execute(bytes.NewBufferString(reqStr))
//	fmt.Println("IsEdgeCluster:", agent.EasyUse.IsEdgeCluster)
//	agent.Callback()
//	agent.Exit()
//}
