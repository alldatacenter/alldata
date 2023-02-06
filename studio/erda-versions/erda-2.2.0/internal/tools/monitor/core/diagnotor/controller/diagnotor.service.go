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

package controller

import (
	"context"
	"fmt"
	"net"
	"os"
	"strings"

	"google.golang.org/grpc"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes"

	"github.com/erda-project/erda-proto-go/core/monitor/diagnotor/pb"
	"github.com/erda-project/erda/pkg/clusterdialer"
	"github.com/erda-project/erda/pkg/common/errors"
)

type diagnotorService struct {
	p *provider
}

func (s *diagnotorService) StartDiagnosis(ctx context.Context, req *pb.StartDiagnosisRequest) (*pb.StartDiagnosisResponse, error) {
	client, err := s.getClient(req.ClusterName)
	if err != nil {
		return nil, err
	}
	agent, pod, err := s.createAgent(ctx, client, req.ClusterName, req.Namespace, req.PodName, req.Labels)
	if err != nil {
		if err == errNotReady {
			return nil, errors.NewInvalidParameterError("podName", "target instance is not ready")
		}
		if strings.Contains(err.Error(), "already exists") {
			return nil, errors.NewInvalidParameterError("podName", err.Error())
		}
		if strings.Contains(err.Error(), "not found") {
			return nil, errors.NewNotFoundError(fmt.Sprintf("pod %q", req.Namespace+"/"+req.PodName))
		}
		return nil, errors.NewInternalServerError(err)
	}
	return &pb.StartDiagnosisResponse{
		Data: &pb.DiagnosisInstance{
			ClusterName: req.ClusterName,
			Namespace:   agent.Namespace,
			PodName:     agent.Name,
			HostIP:      pod.Status.HostIP,
			PodIP:       pod.Status.PodIP,
			Status:      string(agent.Status.Phase),
			Message:     agent.Status.Message,
		},
	}, nil
}

func (s *diagnotorService) QueryDiagnosisStatus(ctx context.Context, req *pb.QueryDiagnosisStatusRequest) (*pb.QueryDiagnosisStatusResponse, error) {
	client, err := s.getClient(req.ClusterName)
	if err != nil {
		return nil, err
	}
	podName := req.PodName
	if !strings.HasSuffix(podName, agentPodNameSuffix) {
		podName = getAgentPodName(podName)
	}
	agent, err := client.CoreV1().Pods(req.Namespace).Get(ctx, podName, metav1.GetOptions{})
	if err != nil {
		if strings.Contains(err.Error(), "not found") {
			return nil, errors.NewNotFoundError(fmt.Sprintf("pod %q", req.Namespace+"/"+podName))
		}
		return nil, errors.NewServiceInvokingError("api-server", err)
	}
	if agent == nil || agent.Status.Phase == corev1.PodSucceeded || agent.Status.Phase == corev1.PodFailed {
		return nil, errors.NewNotFoundError(agentContainerName)
	}
	return &pb.QueryDiagnosisStatusResponse{
		Data: &pb.DiagnosisInstance{
			ClusterName: req.ClusterName,
			Namespace:   agent.Namespace,
			PodName:     agent.Name,
			HostIP:      agent.Status.HostIP,
			PodIP:       agent.Labels[targetPodIPKey], // target pod ip
			Status:      string(agent.Status.Phase),
			Message:     agent.Status.Message,
		},
	}, nil
}

func (s *diagnotorService) StopDiagnosis(ctx context.Context, req *pb.StopDiagnosisRequest) (*pb.StopDiagnosisResponse, error) {
	client, err := s.getClient(req.ClusterName)
	if err != nil {
		return nil, err
	}
	podName := req.PodName
	if !strings.HasSuffix(podName, agentPodNameSuffix) {
		podName = getAgentPodName(podName)
	}
	err = client.CoreV1().Pods(req.Namespace).Delete(ctx, podName, metav1.DeleteOptions{})
	if err != nil {
		if !strings.Contains(err.Error(), "not found") {
			return nil, errors.NewServiceInvokingError("api-server", err)
		}
	}
	return &pb.StopDiagnosisResponse{
		Data: "OK",
	}, nil
}

func (s *diagnotorService) ListProcesses(ctx context.Context, req *pb.ListProcessesRequest) (resp *pb.ListProcessesResponse, err error) {
	err = s.doRequestGrpc(req.ClusterName, req.PodIP, func(client pb.DiagnotorAgentServiceClient) error {
		r, err := client.ListTargetProcesses(ctx, &pb.ListTargetProcessesRequest{})
		if err != nil {
			return errors.NewServiceInvokingError("diagnotor-agent", err)
		}
		resp = &pb.ListProcessesResponse{
			Data: r.Data,
		}
		return nil
	})
	return resp, err
}

func (s *diagnotorService) getClient(clusterName string) (kubernetes.Interface, error) {
	client, _, err := s.p.Clients.GetClient(clusterName)
	if err != nil {
		if strings.Contains(err.Error(), "not found") {
			return nil, errors.NewNotFoundError("clusters/" + clusterName)
		}
		return nil, errors.NewInternalServerError(fmt.Errorf("failed to get client: %s", err))
	}
	return client, nil
}

func (s *diagnotorService) doRequestGrpc(clusterName, podIP string, fn func(client pb.DiagnotorAgentServiceClient) error) error {
	addr := net.JoinHostPort(podIP, "14973")
	opts := []grpc.DialOption{grpc.WithInsecure()}
	if s.isRemoteCluster(clusterName) {
		opts = append(opts, grpc.WithContextDialer(clusterdialer.DialContextTCP(clusterName)))
	}
	conn, err := grpc.Dial(addr, opts...)
	if err != nil {
		return errors.NewServiceInvokingError("diagnotor-agent", err)
	}
	defer conn.Close()
	client := pb.NewDiagnotorAgentServiceClient(conn)
	return fn(client)
}

func (s *diagnotorService) isRemoteCluster(clusterName string) bool {
	currentClusterName := os.Getenv("DICE_CLUSTER_NAME")
	if clusterName == "" || currentClusterName == clusterName {
		return false
	}
	return true
}
