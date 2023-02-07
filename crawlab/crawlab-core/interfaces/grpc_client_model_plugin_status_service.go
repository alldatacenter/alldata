package interfaces

import (
	"github.com/crawlab-team/crawlab-db/mongo"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
)

type GrpcClientModelPluginStatusService interface {
	ModelBaseService
	GetPluginStatusById(id primitive.ObjectID) (ps PluginStatus, err error)
	GetPluginStatus(query bson.M, opts *mongo.FindOptions) (ps PluginStatus, err error)
	GetPluginStatusList(query bson.M, opts *mongo.FindOptions) (res []PluginStatus, err error)
}
