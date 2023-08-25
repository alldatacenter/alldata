/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package main

import (
	"flag"
	"os"
	"path/filepath"

	"github.com/spf13/pflag"
	genericapiserver "k8s.io/apiserver/pkg/server"
	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/cmd/koord-runtime-proxy/options"
	"github.com/koordinator-sh/koordinator/pkg/runtimeproxy/server/cri"
	"github.com/koordinator-sh/koordinator/pkg/runtimeproxy/server/docker"
)

func main() {
	flag.StringVar(&options.RuntimeProxyEndpoint, "koord-runtimeproxy-endpoint", options.DefaultRuntimeProxyEndpoint,
		"koord-runtimeproxy service endpoint.")
	flag.StringVar(&options.RemoteRuntimeServiceEndpoint, "remote-runtime-service-endpoint", options.DefaultContainerdRuntimeServiceEndpoint,
		"backend runtime service endpoint.")
	flag.StringVar(&options.RemoteImageServiceEndpoint, "remote-image-service-endpoint", options.DefaultContainerdImageServiceEndpoint,
		"backend image service endpoint.")
	flag.StringVar(&options.BackendRuntimeMode, "backend-runtime-mode", options.DefaultBackendRuntimeMode,
		"backend container engine(Containerd|Docker).")
	flag.StringVar(&options.RuntimeHookServerKey, "runtime-hook-server-key", options.DefaultHookServerKey,
		"if pod tag itself with runtime-hook-server-key in annotations, runtime-proxy would regard this pod as runtime hook server and "+
			"skip transferring cri events to hook server")
	flag.StringVar(&options.RuntimeHookServerVal, "runtime-hook-server-val", options.DefaultHookServerVal,
		"working combined with runtime-hook-server-key")

	pflag.CommandLine.AddGoFlagSet(flag.CommandLine)
	pflag.Parse()

	if err := os.Remove(options.RuntimeProxyEndpoint); err != nil && !os.IsNotExist(err) {
		klog.Fatalf("failed to unlink %v: %v", options.RuntimeProxyEndpoint, err)
	}

	if err := os.MkdirAll(filepath.Dir(options.RuntimeProxyEndpoint), 0755); err != nil {
		klog.Fatalf("failed to mkdir %v: %v", filepath.Dir(options.RuntimeProxyEndpoint), err)
	}

	switch options.BackendRuntimeMode {
	case options.BackendRuntimeModeContainerd:
		server := cri.NewRuntimeManagerCriServer()
		go server.Run()
	case options.BackendRuntimeModeDocker:
		server := docker.NewRuntimeManagerDockerServer()
		go server.Run()
	default:
		klog.Fatalf("unknown runtime engine backend %v", options.BackendRuntimeMode)
	}

	stopCh := genericapiserver.SetupSignalHandler()
	<-stopCh
	klog.Info("koordiantor runtime-proxy shutting down")
}
