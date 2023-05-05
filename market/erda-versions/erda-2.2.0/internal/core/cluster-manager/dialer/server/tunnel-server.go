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

package server

import (
	"context"
	"crypto/tls"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"net"
	"net/http"
	"net/textproto"
	"strconv"
	"strings"
	"sync"
	"sync/atomic"
	"time"

	"github.com/coreos/etcd/clientv3"
	"github.com/gorilla/mux"
	"github.com/pkg/errors"
	"github.com/rancher/remotedialer"
	"github.com/sirupsen/logrus"
	"google.golang.org/grpc/metadata"

	"github.com/erda-project/erda-infra/pkg/transport"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/core/cluster-manager/dialer/auth"
	"github.com/erda-project/erda/internal/core/cluster-manager/dialer/config"
	"github.com/erda-project/erda/pkg/http/httputil"
)

var (
	l                     sync.Mutex
	clients               = map[string]*http.Client{}
	counter               int64
	registerTimeout       = 5 * time.Second
	registerCheckInterval = 1 * time.Second
)

const (
	portalSchemeHeader  = "X-Portal-Scheme"
	portalHostHeader    = "X-Portal-Host"
	portalDestHeader    = "X-Portal-Dest"
	portalTimeoutHeader = "X-Portal-Timeout"

	ClusterManagerETCDKeyPrefix = "cluster-manager/clusterKey/"
)

type cluster struct {
	Address string `json:"address"`
	Token   string `json:"token"`
	CACert  string `json:"caCert"`
}

func clusterRegister(ctx context.Context, server *remotedialer.Server, rw http.ResponseWriter, req *http.Request, needClusterInfo bool, etcd *clientv3.Client, clusterSvc clusterpb.ClusterServiceServer) {
	registerFunc := func(clusterKey string, clusterInfo cluster) {
		ctx, cancel := context.WithTimeout(context.Background(), registerTimeout)
		defer cancel()
		ctx = transport.WithHeader(ctx, metadata.New(map[string]string{httputil.InternalHeader: "cluster-manager"}))
		for {
			select {
			case <-ctx.Done():
				logrus.Errorf("register cluster info timeout [%s]", clusterKey)
				return
			default:
				if !server.HasSession(clusterKey) {
					logrus.Debugf("session not found, try again [%s]", clusterKey)
					<-time.After(registerCheckInterval)
					continue
				}
				logrus.Infof("session exsited, patch cluster info to cluster-manager [%s]", clusterKey)
				// register to cluster manager
				c, err := clusterSvc.GetCluster(ctx, &clusterpb.GetClusterRequest{IdOrName: clusterKey})
				if err != nil {
					logrus.Errorf("failed to get cluster from cluster-manager: %s, err: %v", clusterKey, err)
					remotedialer.DefaultErrorWriter(rw, req, 500, err)
					return
				}

				if c.Data.ManageConfig != nil {
					if c.Data.ManageConfig.Type != apistructs.ManageProxy {
						logrus.Warnf("cluster is not proxy type [%s]", clusterKey)
						return
					}
					if clusterInfo.Token == c.Data.ManageConfig.Token && clusterInfo.Address == c.Data.ManageConfig.Address &&
						clusterInfo.CACert == c.Data.ManageConfig.CaData {
						logrus.Infof("cluster info isn't change [%s]", clusterKey)
						return
					}
				}

				if _, err = clusterSvc.PatchCluster(ctx, &clusterpb.PatchClusterRequest{
					Name: clusterKey,
					ManageConfig: &clusterpb.ManageConfig{
						Type:             apistructs.ManageProxy,
						Address:          clusterInfo.Address,
						CaData:           clusterInfo.CACert,
						Token:            clusterInfo.Token,
						CredentialSource: apistructs.ManageProxy,
					},
				}); err != nil {
					logrus.Errorf("failed to patch cluster [%s], err: %v", clusterKey, err)
					remotedialer.DefaultErrorWriter(rw, req, 500, err)
					return
				}
				logrus.Infof("patch cluster info success [%s]", clusterKey)

				return
			}
		}
	}

	clientType := apistructs.ClusterManagerClientType(req.Header.Get(apistructs.ClusterManagerHeaderKeyClientType.String()))
	clusterKey := req.Header.Get(apistructs.ClusterManagerHeaderKeyClusterKey.String())
	clusterKey = clientType.MakeClientKey(clusterKey)
	if clusterKey == "" {
		remotedialer.DefaultErrorWriter(rw, req, 400, errors.New("missing header:X-Erda-Cluster-Key"))
		return
	}

	podIP, err := getLocalIP()
	if err != nil {
		logrus.Errorf("failed to get local IP, %v", err)
		return
	}

	_, err = etcd.Put(ctx, ClusterManagerETCDKeyPrefix+clusterKey, podIP)
	if err != nil {
		logrus.Errorf("failed to put clusterKey to etcd, %v", err)
		return
	}

	switch clientType {
	case apistructs.ClusterManagerClientTypeDefault, apistructs.ClusterManagerClientTypeCluster:
		if needClusterInfo {
			// Get cluster info from agent request
			info := req.Header.Get(apistructs.ClusterManagerHeaderKeyClusterInfo.String())
			if info == "" {
				remotedialer.DefaultErrorWriter(rw, req, 400, errors.New("missing header:X-Erda-Cluster-Info"))
				return
			}

			if req.Header.Get(apistructs.ClusterManagerHeaderKeyAuthorization.String()) == "" {
				remotedialer.DefaultErrorWriter(rw, req, 400, errors.New("missing header:Authorization"))
				return
			}

			var clusterInfo cluster
			bytes, err := base64.StdEncoding.DecodeString(info)
			if err != nil {
				remotedialer.DefaultErrorWriter(rw, req, 400, err)
				return
			}
			if err := json.Unmarshal(bytes, &clusterInfo); err != nil {
				remotedialer.DefaultErrorWriter(rw, req, 400, err)
				return
			}
			if clusterInfo.Address == "" {
				err = errors.New("invalid cluster info, address empty")
				remotedialer.DefaultErrorWriter(rw, req, 400, err)
				return
			}
			if clusterInfo.Token == "" {
				err = errors.New("invalid cluster info, token empty")
				remotedialer.DefaultErrorWriter(rw, req, 400, err)
				return
			}
			if clusterInfo.CACert == "" {
				err = errors.New("invalid cluster info, caCert empty")
				remotedialer.DefaultErrorWriter(rw, req, 400, err)
				return
			}
			// TODO: register action after authed better.
			go registerFunc(clusterKey, clusterInfo)
		}
	default:
		clientDataStr := req.Header.Get(apistructs.ClusterManagerHeaderKeyClientDetail.String())
		if clientDataStr != "" {
			var clientData apistructs.ClusterManagerClientDetail
			if err := json.Unmarshal([]byte(clientDataStr), &clientData); err != nil {
				logrus.Errorf("failed to unmarshal client data(skip clients update), clientType: %s, clusterKey: %s, err: %v",
					clientType, clusterKey, err)
				goto register
			}
			clientData[apistructs.ClusterManagerDataKeyClusterKey] = req.Header.Get(apistructs.ClusterManagerHeaderKeyClusterKey.String())
			updateClientDetailWithEvent(clientType, clusterKey, clientData)
		}
		logrus.Infof("client type [%s]", clientType)
	}

register:
	server.ServeHTTP(rw, req)
}

func getLocalIP() (string, error) {
	netInterfaces, err := net.Interfaces()
	if err != nil {
		return "", errors.Errorf("net.Interfaces failed, %v", err.Error())
	}
	for i := 0; i < len(netInterfaces); i++ {
		if (netInterfaces[i].Flags & net.FlagUp) != 0 {
			addrs, _ := netInterfaces[i].Addrs()
			for _, address := range addrs {
				if ipnet, ok := address.(*net.IPNet); ok && !ipnet.IP.IsLoopback() {
					if ipnet.IP.To4() != nil {
						return ipnet.IP.String(), nil
					}
				}
			}
		}
	}
	return "", errors.New("can not find local IP")
}

func queryIP(rw http.ResponseWriter, req *http.Request, etcd *clientv3.Client) {
	resp := apistructs.QueryClusterManagerIPResponse{}
	clusterKey := req.URL.Query().Get("clusterKey")
	logrus.Debugf("got queryIP request, clusterKey: %s", clusterKey)
	if clusterKey == "" {
		resp.Error = errors.New("MissingParameter: clusterKey").Error()
		resp.Succeeded = false
		writeResp(rw, resp, http.StatusBadRequest)
		return
	}

	r, err := etcd.Get(req.Context(), ClusterManagerETCDKeyPrefix+clusterKey)
	if err != nil {
		resp.Error = errors.Errorf("failed to get ip for clusterKey %s from etcd, %v", clusterKey, err).Error()
		resp.Succeeded = false
		writeResp(rw, resp, http.StatusInternalServerError)
		return
	}
	if len(r.Kvs) == 0 {
		resp.Error = errors.Errorf("can not find ip for clusterKey %s", clusterKey).Error()
		resp.Succeeded = false
		writeResp(rw, resp, http.StatusNotFound)
		return
	}

	resp.Succeeded = true
	resp.IP = string(r.Kvs[0].Value)
	writeResp(rw, resp, http.StatusOK)
}

func writeResp(rw http.ResponseWriter, resp interface{}, code int) {
	data, _ := json.Marshal(resp)
	rw.WriteHeader(code)
	rw.Write(data)
}

func netportal(server *remotedialer.Server, rw http.ResponseWriter, req *http.Request, timeout time.Duration) {
	clusterKey := req.Header.Get(portalHostHeader)
	addrInCluster := req.Header.Get(portalDestHeader)
	schemeInCluster := req.Header.Get(portalSchemeHeader)
	timeoutStr := req.Header.Get(portalTimeoutHeader)
	if timeoutStr != "" {
		timeoutInt, err := strconv.Atoi(timeoutStr)
		if err == nil {
			timeout = time.Duration(timeoutInt) * time.Second
		}
	}
	if schemeInCluster == "" {
		schemeInCluster = "http"
	}
	url := fmt.Sprintf("%s://%s%s", schemeInCluster, addrInCluster, req.URL.EscapedPath())
	if req.URL.RawQuery != "" {
		url = fmt.Sprintf("%s?%s", url, req.URL.RawQuery)
	}
	client := getClusterClient(server, clusterKey, timeout)
	id := atomic.AddInt64(&counter, 1)
	logrus.Infof("[%d] REQ cluster=%s setting-timeout=%s %s", id, clusterKey, timeout, url)
	proxyReq, err := http.NewRequest(req.Method, url, req.Body)
	if err != nil {
		logrus.Errorf("[%d] NEW REQ cluster=%s %s: %v", id, clusterKey, url, err)
		remotedialer.DefaultErrorWriter(rw, req, 500, err)
		return
	}
	req.Header.Del(portalHostHeader)
	req.Header.Del(portalDestHeader)
	req.Header.Del(portalSchemeHeader)
	req.Header.Del(portalTimeoutHeader)
	proxyReq.Header = req.Header
	start := time.Now()
	resp, err := client.Do(proxyReq)
	if err != nil {
		logrus.Errorf("[%d] REQ ERR cluster=%s latency=%dms %s: %v", id, clusterKey, time.Since(start).Milliseconds(), url, err)
		remotedialer.DefaultErrorWriter(rw, req, 500, err)
		return
	}
	defer resp.Body.Close()
	logrus.Infof("[%d] REQ OK cluster=%s code=%d latency=%dms %s", id, clusterKey, resp.StatusCode, time.Since(start).Milliseconds(), url)
	rwHeader := rw.Header()
	for key, values := range resp.Header {
		rwHeader[textproto.CanonicalMIMEHeaderKey(key)] = values
	}
	rw.WriteHeader(resp.StatusCode)
	io.Copy(rw, resp.Body)
	logrus.Infof("[%d] REQ DONE cluster=%s latency=%dms %s", id, clusterKey, time.Since(start).Milliseconds(), url)
}

func checkClusterIsExisted(server *remotedialer.Server, rw http.ResponseWriter, req *http.Request) {
	clusterKey := req.URL.Query().Get("clusterKey")
	clientType := apistructs.ClusterManagerClientType(req.URL.Query().Get("clientType"))
	clusterKey = clientType.MakeClientKey(clusterKey)
	isExisted := server.HasSession(clusterKey)
	rw.Write([]byte(strconv.FormatBool(isExisted)))
}

func getClusterClientData(server *remotedialer.Server, rw http.ResponseWriter, req *http.Request) {
	clusterKey := mux.Vars(req)["clusterKey"]
	clientType := apistructs.ClusterManagerClientType(mux.Vars(req)["clientType"])
	clusterKey = clientType.MakeClientKey(clusterKey)
	isExisted := server.HasSession(clusterKey)
	if !isExisted {
		remotedialer.DefaultErrorWriter(rw, req, 404, errors.New("cluster not found"))
		return
	}
	clientData, ok := getClientDetail(clientType, clusterKey)
	if !ok {
		remotedialer.DefaultErrorWriter(rw, req, 404, errors.New("client data not found"))
		return
	}
	clientByteData, err := clientData.Marshal()
	if err != nil {
		remotedialer.DefaultErrorWriter(rw, req, 500, err)
		return
	}
	rw.Header().Set("Content-Type", "application/json")
	rw.Write(clientByteData)
}

func listClusterClientsByType(server *remotedialer.Server, rw http.ResponseWriter, req *http.Request) {
	clientType := apistructs.ClusterManagerClientType(mux.Vars(req)["clientType"])
	clientDetails := listClientDetailByType(clientType)
	clientDetailBytes, err := json.Marshal(clientDetails)
	if err != nil {
		remotedialer.DefaultErrorWriter(rw, req, 500, err)
		return
	}
	rw.Header().Set("Content-Type", "application/json")
	rw.Write(clientDetailBytes)
}

func getClusterClient(server *remotedialer.Server, clusterKey string, timeout time.Duration) *http.Client {
	l.Lock()
	defer l.Unlock()

	key := fmt.Sprintf("%s/%s", clusterKey, timeout)
	client := clients[key]
	if client != nil {
		return client
	}

	dialer := server.Dialer(clusterKey)
	client = &http.Client{
		Transport: &http.Transport{
			DialContext: dialer,
			TLSClientConfig: &tls.Config{
				InsecureSkipVerify: true,
			},
		},
		Timeout: timeout,
	}

	clients[key] = client
	return client
}

func NewDialerRouter(ctx context.Context, clusterSvc clusterpb.ClusterServiceServer, credential tokenpb.TokenServiceServer, cfg *config.Config, etcd *clientv3.Client, bdl *bundle.Bundle) *mux.Router {
	authorizer := auth.New(
		auth.WithCredentialClient(credential),
		auth.WithConfig(cfg),
	)

	initClientData(bdl)
	handler := remotedialer.New(authorizer.Authorizer, remotedialer.DefaultErrorWriter)
	handler.ClientConnectAuthorizer = func(proto, address string) bool {
		if strings.HasSuffix(proto, "::tcp") {
			return true
		}
		if strings.HasSuffix(proto, "::unix") {
			return address == "/var/run/docker.sock"
		}
		if strings.HasSuffix(proto, "::npipe") {
			return address == "//./pipe/docker_engine"
		}
		return false
	}
	// TODO: support handler.AddPeer
	router := mux.NewRouter()
	router.Handle("/clusterdialer", handler)
	router.HandleFunc("/clusterdialer/ip", func(rw http.ResponseWriter, req *http.Request) {
		queryIP(rw, req, etcd)
	})
	router.HandleFunc("/clusteragent/connect", func(rw http.ResponseWriter,
		req *http.Request) {
		clusterRegister(ctx, handler, rw, req, cfg.NeedClusterInfo, etcd, clusterSvc)
	})
	router.HandleFunc("/clusteragent/check", func(rw http.ResponseWriter,
		req *http.Request) {
		checkClusterIsExisted(handler, rw, req)
	})
	router.HandleFunc("/clusteragent/client-detail/{clientType}/{clusterKey}", func(rw http.ResponseWriter,
		req *http.Request) {
		getClusterClientData(handler, rw, req)
	})
	router.HandleFunc("/clusteragent/client-detail/{clientType}", func(rw http.ResponseWriter,
		req *http.Request) {
		listClusterClientsByType(handler, rw, req)
	})
	router.PathPrefix("/").HandlerFunc(func(rw http.ResponseWriter,
		req *http.Request) {
		netportal(handler, rw, req, cfg.Timeout)
	})
	return router
}

func Start(ctx context.Context, clusterSvc clusterpb.ClusterServiceServer, credential tokenpb.TokenServiceServer, cfg *config.Config, etcd *clientv3.Client, bdl *bundle.Bundle) error {
	server := &http.Server{
		BaseContext: func(net.Listener) context.Context {
			return ctx
		},
		Addr:    cfg.Listen,
		Handler: NewDialerRouter(ctx, clusterSvc, nil, cfg, etcd, bdl),
	}
	return server.ListenAndServe()
}
