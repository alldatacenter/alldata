package interfaces

import grpc "github.com/crawlab-team/crawlab-grpc"

type Client interface {
	GetModelDelegateClient() grpc.ModelDelegateClient
	GetModelBaseServiceClient() grpc.ModelBaseServiceClient
	GetNodeClient() grpc.NodeServiceClient
	GetTaskClient() grpc.TaskServiceClient
	GetPluginClient() grpc.PluginServiceClient
}
