package client

import (
	"github.com/crawlab-team/crawlab-core/errors"
	"github.com/crawlab-team/crawlab-core/interfaces"
	"github.com/crawlab-team/crawlab-db/mongo"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
)

type PluginStatusServiceDelegate struct {
	interfaces.GrpcClientModelBaseService
}

func (svc *PluginStatusServiceDelegate) GetPluginStatusById(id primitive.ObjectID) (t interfaces.PluginStatus, err error) {
	res, err := svc.GetById(id)
	if err != nil {
		return nil, err
	}
	s, ok := res.(interfaces.PluginStatus)
	if !ok {
		return nil, errors.ErrorModelInvalidType
	}
	return s, nil
}

func (svc *PluginStatusServiceDelegate) GetPluginStatus(query bson.M, opts *mongo.FindOptions) (t interfaces.PluginStatus, err error) {
	res, err := svc.Get(query, opts)
	if err != nil {
		return nil, err
	}
	s, ok := res.(interfaces.PluginStatus)
	if !ok {
		return nil, errors.ErrorModelInvalidType
	}
	return s, nil
}

func (svc *PluginStatusServiceDelegate) GetPluginStatusList(query bson.M, opts *mongo.FindOptions) (res []interfaces.PluginStatus, err error) {
	list, err := svc.GetList(query, opts)
	if err != nil {
		return nil, err
	}
	for _, item := range list.GetModels() {
		s, ok := item.(interfaces.PluginStatus)
		if !ok {
			return nil, errors.ErrorModelInvalidType
		}
		res = append(res, s)
	}
	return res, nil
}

func NewPluginStatusServiceDelegate(opts ...ModelBaseServiceDelegateOption) (svc2 interfaces.GrpcClientModelPluginStatusService, err error) {
	// apply options
	opts = append(opts, WithBaseServiceModelId(interfaces.ModelIdPluginStatus))

	// base service
	baseSvc, err := NewBaseServiceDelegate(opts...)
	if err != nil {
		return nil, err
	}

	// service
	svc := &PluginStatusServiceDelegate{baseSvc}

	return svc, nil
}

func ProvidePluginStatusServiceDelegate(path string, opts ...ModelBaseServiceDelegateOption) func() (svc interfaces.GrpcClientModelPluginStatusService, err error) {
	if path != "" {
		opts = append(opts, WithBaseServiceConfigPath(path))
	}
	return func() (svc interfaces.GrpcClientModelPluginStatusService, err error) {
		return NewPluginStatusServiceDelegate(opts...)
	}
}
