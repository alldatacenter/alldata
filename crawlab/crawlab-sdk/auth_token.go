package sdk

import (
	"context"
	"google.golang.org/grpc"
	"google.golang.org/grpc/metadata"
	"os"
)

func getAuthKeyEnv() (key string) {
	return os.Getenv(GrpcAuthKeyEnv)
}

func GetAuthTokenUnaryChainInterceptor() grpc.UnaryClientInterceptor {
	md := metadata.Pairs(GrpcHeaderAuthorization, getAuthKeyEnv())
	return func(ctx context.Context, method string, req, reply interface{}, cc *grpc.ClientConn, invoker grpc.UnaryInvoker, opts ...grpc.CallOption) error {
		ctx = metadata.NewOutgoingContext(context.Background(), md)
		return invoker(ctx, method, req, reply, cc, opts...)
	}
}

func GetAuthTokenStreamChainInterceptor() grpc.StreamClientInterceptor {
	md := metadata.Pairs(GrpcHeaderAuthorization, getAuthKeyEnv())
	return func(ctx context.Context, desc *grpc.StreamDesc, cc *grpc.ClientConn, method string, streamer grpc.Streamer, opts ...grpc.CallOption) (grpc.ClientStream, error) {
		ctx = metadata.NewOutgoingContext(context.Background(), md)
		s, err := streamer(ctx, desc, cc, method, opts...)
		if err != nil {
			return nil, err
		}
		return s, nil
	}
}
