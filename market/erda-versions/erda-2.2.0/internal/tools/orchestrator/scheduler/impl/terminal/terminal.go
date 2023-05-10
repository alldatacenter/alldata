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

package terminal

import (
	"encoding/json"
	"net/http"
	"net/url"
	"os"
	"strconv"
	"strings"
	"sync"

	"github.com/gorilla/websocket"
	"github.com/sirupsen/logrus"
	"google.golang.org/grpc/metadata"

	"github.com/erda-project/erda-infra/pkg/transport"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/orchestrator/conf"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/executortypes"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/cluster/clusterutil"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/instanceinfo"
	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/strutil"
)

var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool {
		origin := r.Header.Get("Origin")
		if strutil.Contains(origin, os.Getenv("DICE_ROOT_DOMAIN")) {
			return true
		}

		for _, domain := range strutil.Split(conf.WsDiceRootDomain(), ",") {
			if strutil.Contains(origin, domain) {
				return true
			}
		}
		return false
	},
}

type ContainerInfo struct {
	Env  []string        `json:"env"`
	Name string          `json:"name"`
	Args json.RawMessage `json:"args"`
}

type ContainerInfoArg struct {
	Host      string `json:"host"`
	Port      int    `json:"port"`
	Container string `json:"container"`
}

func Terminal(clusterSvc clusterpb.ClusterServiceServer, w http.ResponseWriter, r *http.Request) {
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		logrus.Errorf("upgrade: %v", err)
		return
	}
	defer conn.Close()

	// 1. First get the information of the container to be connected sent from the front end
	t, message, err := conn.ReadMessage()
	if err != nil {
		logrus.Infof("failed to ReadMessage: %v", err)
		return
	}
	if t != websocket.TextMessage {
		return
	}
	containerinfo := ContainerInfo{}
	if err := json.Unmarshal(message, &containerinfo); err != nil {
		logrus.Errorf("failed to unmarshal containerinfo: %v, content: %s", err, string(message))
		return
	}
	if containerinfo.Name != "docker" {
		// Not a container console, as a soldier as a proxy
		SoldierTerminal(clusterSvc, r, message, conn)
		return
	}
	var args ContainerInfoArg
	if err := json.Unmarshal(containerinfo.Args, &args); err != nil {
		logrus.Errorf("failed to unmarshal containerinfoArgs: %v", err)
		return
	}

	// 2. Query the containerid in the instance list
	instances, err := instanceinfo.New(dbengine.MustOpen()).InstanceReader().ByContainerID(args.Container).Do()
	if err != nil {
		logrus.Errorf("failed to get instance by containerid: %v", err)
		return
	}

	if len(instances) == 0 {
		logrus.Errorf("no instances found: containerid: %v", args.Container)
		return
	}
	if len(instances) > 1 {
		logrus.Errorf("more than one instance found: containerid: %v", args.Container)
		return
	}
	instance := instances[0]

	// 3. Check permissions
	access := false
	if instance.OrgID != "" {
		orgid, err := strconv.ParseUint(instance.OrgID, 10, 64)
		if err != nil {
			logrus.Errorf("failed to parse orgid for instance: %v, %v", instance.ContainerID, err)
			return
		}
		p, err := bundle.New(bundle.WithCoreServices()).CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   r.Header.Get("User-ID"),
			Scope:    apistructs.OrgScope,
			ScopeID:  orgid,
			Resource: "terminal",
			Action:   "OPERATE",
		})
		if err != nil {
			logrus.Errorf("failed to check permissions for terminal: %v", err)
			return
		}
		if p.Access {
			access = true
		}
	}
	if !access && instance.ApplicationID != "" {
		appid, err := strconv.ParseUint(instance.ApplicationID, 10, 64)
		if err != nil {
			logrus.Errorf("failed to parse applicationid for instance: %v, %v", instance.ContainerID, err)
			return
		}
		p, err := bundle.New(bundle.WithCoreServices()).CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   r.Header.Get("User-ID"),
			Scope:    apistructs.AppScope,
			ScopeID:  appid,
			Resource: "terminal",
			Action:   "OPERATE",
		})
		if err != nil {
			logrus.Errorf("failed to check permissions for terminal: %v", err)
			return
		}
		if !p.Access {
			logrus.Errorf("permission denied for terminal, userid: %v, appid: %d", r.Header.Get("User-ID"), appid)
			return
		}
	}

	// 4. Determine whether it is a dcos path
	k8snamespace, ok1 := instance.Metadata("k8snamespace")
	k8spodname, ok2 := instance.Metadata("k8spodname")
	k8scontainername, ok3 := instance.Metadata("k8scontainername")
	clustername := instance.Cluster

	if !ok1 || !ok2 || !ok3 {
		// If there is no corresponding namespace, name, containername in the meta, it is considered to be the dcos path, and the original soldier is taken
		logrus.Errorf("get terminial info failed, namespace %v, pod name %v, container name %v", ok1, ok2, ok3)
		return
	}

	K8STerminal(clustername, k8snamespace, k8spodname, k8scontainername, conn)
}

// SoldierTerminal proxy of soldier
func SoldierTerminal(clusterSvc clusterpb.ClusterServiceServer, r *http.Request, initmessage []byte, upperConn *websocket.Conn) {
	clusterName := r.URL.Query().Get("clusterName")
	ctx := transport.WithHeader(r.Context(), metadata.New(map[string]string{httputil.InternalHeader: "cmp"}))
	resp, err := clusterSvc.GetCluster(ctx, &clusterpb.GetClusterRequest{IdOrName: clusterName})
	if err != nil {
		logrus.Errorf("failed to get cluster info with bundle err :%v", err)
	}

	clusterInfo := resp.Data
	soldierAddr, err := url.Parse(clusterInfo.Urls["colonySoldier"])
	if err != nil {
		logrus.Errorf("failed to url parse: %v, err: %v", r.URL.Query().Get("url"), err)
	}
	soldierAddr.Path = r.URL.Path
	switch soldierAddr.Scheme {
	case "https":
		soldierAddr.Scheme = "wss"
	case "http":
		soldierAddr.Scheme = "ws"
	default:
		soldierAddr.Scheme = "ws"
	}

	conn, _, err := websocket.DefaultDialer.Dial(soldierAddr.String(), nil)
	if err != nil {
		logrus.Errorf("failed to dial with %s: %v", soldierAddr, err)
		return
	}
	if err := conn.WriteMessage(websocket.TextMessage, initmessage); err != nil {
		logrus.Errorf("failed to write message: %v, err: %v", string(initmessage), err)
		return
	}
	var wait sync.WaitGroup
	wait.Add(2)
	go func() {
		defer func() {
			wait.Done()
			conn.Close()
			upperConn.Close()
		}()
		for {
			tp, m, err := upperConn.ReadMessage()
			if err != nil {
				return
			}
			if err := conn.WriteMessage(tp, m); err != nil {
				return
			}
		}
	}()
	go func() {
		defer func() {
			wait.Done()
			conn.Close()
			upperConn.Close()
		}()
		for {
			tp, m, err := conn.ReadMessage()
			if err != nil {
				return
			}
			if err := upperConn.WriteMessage(tp, m); err != nil {
				return
			}
		}
	}()
	wait.Wait()
}

func K8STerminal(clustername, namespace, podname, containername string, upperConn *websocket.Conn) {
	executorname := clusterutil.GenerateExecutorByClusterName(clustername)
	logrus.Infof("terminal get executor name %s", executorname)
	if strings.Contains(executorname, "EDAS") {
		executorname = "K8SFOR" + strings.ToUpper(strings.Replace(clustername, "-", "", -1))
	}
	executor, err := executor.GetManager().Get(executortypes.Name(executorname))
	if err != nil {
		logrus.Errorf("failed to get executor by executorname(%s)", executorname)
		return
	}
	terminalExecutor, ok := executor.(executortypes.TerminalExecutor)
	if !ok {
		logrus.Errorf("executor(%s) not impl executortypes.TerminalExecutor", executorname)
		return
	}
	terminalExecutor.Terminal(namespace, podname, containername, upperConn)
}
