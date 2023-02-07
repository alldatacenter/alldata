package client

import (
	config2 "github.com/crawlab-team/crawlab-core/config"
	"github.com/crawlab-team/crawlab-core/grpc/client"
	"github.com/crawlab-team/crawlab-core/interfaces"
	"go.uber.org/dig"
)

type ServiceDelegate struct {
	// settings
	cfgPath string

	// internals
	c interfaces.GrpcClient
}

func (d *ServiceDelegate) GetConfigPath() string {
	return d.cfgPath
}

func (d *ServiceDelegate) SetConfigPath(path string) {
	d.cfgPath = path
}

func (d *ServiceDelegate) NewBaseServiceDelegate(id interfaces.ModelId) (svc interfaces.GrpcClientModelBaseService, err error) {
	var opts []ModelBaseServiceDelegateOption
	opts = append(opts, WithBaseServiceModelId(id))
	if d.cfgPath != "" {
		opts = append(opts, WithBaseServiceConfigPath(d.cfgPath))
	}
	return NewBaseServiceDelegate(opts...)
}

func NewServiceDelegate(opts ...ModelServiceDelegateOption) (svc2 interfaces.GrpcClientModelService, err error) {
	// service delegate
	svc := &ServiceDelegate{
		cfgPath: config2.DefaultConfigPath,
	}

	// apply options
	for _, opt := range opts {
		opt(svc)
	}

	// dependency injection
	c := dig.New()
	if err := c.Provide(client.ProvideGetClient(svc.cfgPath)); err != nil {
		return nil, err
	}
	if err := c.Invoke(func(client interfaces.GrpcClient) {
		svc.c = client
	}); err != nil {
		return nil, err
	}

	return svc, nil
}

func ProvideServiceDelegate(path string) func() (svc interfaces.GrpcClientModelService, err error) {
	return func() (svc interfaces.GrpcClientModelService, err error) {
		return NewServiceDelegate(WithServiceConfigPath(path))
	}
}
