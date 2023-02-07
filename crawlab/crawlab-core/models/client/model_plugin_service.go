package client

import (
	"github.com/crawlab-team/crawlab-core/errors"
	"github.com/crawlab-team/crawlab-core/interfaces"
	"github.com/crawlab-team/crawlab-db/mongo"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
)

type PluginServiceDelegate struct {
	interfaces.GrpcClientModelBaseService
}

func (svc *PluginServiceDelegate) GetPluginById(id primitive.ObjectID) (n interfaces.Plugin, err error) {
	res, err := svc.GetById(id)
	if err != nil {
		return nil, err
	}
	s, ok := res.(interfaces.Plugin)
	if !ok {
		return nil, errors.ErrorModelInvalidType
	}
	return s, nil
}

func (svc *PluginServiceDelegate) GetPlugin(query bson.M, opts *mongo.FindOptions) (n interfaces.Plugin, err error) {
	res, err := svc.Get(query, opts)
	if err != nil {
		return nil, err
	}
	s, ok := res.(interfaces.Plugin)
	if !ok {
		return nil, errors.ErrorModelInvalidType
	}
	return s, nil
}

func (svc *PluginServiceDelegate) GetPluginByName(name string) (n interfaces.Plugin, err error) {
	return svc.GetPlugin(bson.M{"name": name}, nil)
}

func (svc *PluginServiceDelegate) GetPluginList(query bson.M, opts *mongo.FindOptions) (res []interfaces.Plugin, err error) {
	list, err := svc.GetList(query, opts)
	if err != nil {
		return nil, err
	}
	for _, item := range list.GetModels() {
		s, ok := item.(interfaces.Plugin)
		if !ok {
			return nil, errors.ErrorModelInvalidType
		}
		res = append(res, s)
	}
	return res, nil
}

func NewPluginServiceDelegate(opts ...ModelBaseServiceDelegateOption) (svc2 interfaces.GrpcClientModelPluginService, err error) {
	// apply options
	opts = append(opts, WithBaseServiceModelId(interfaces.ModelIdPlugin))

	// base service
	baseSvc, err := NewBaseServiceDelegate(opts...)
	if err != nil {
		return nil, err
	}

	// service
	svc := &PluginServiceDelegate{baseSvc}

	return svc, nil
}

func ProvidePluginServiceDelegate(path string, opts ...ModelBaseServiceDelegateOption) func() (svc interfaces.GrpcClientModelPluginService, err error) {
	if path != "" {
		opts = append(opts, WithBaseServiceConfigPath(path))
	}
	return func() (svc interfaces.GrpcClientModelPluginService, err error) {
		return NewPluginServiceDelegate(opts...)
	}
}
