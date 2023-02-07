package interfaces

import (
	"github.com/crawlab-team/crawlab-db/mongo"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
)

type GrpcClientModelPluginService interface {
	ModelBaseService
	GetPluginById(id primitive.ObjectID) (p Plugin, err error)
	GetPlugin(query bson.M, opts *mongo.FindOptions) (p Plugin, err error)
	GetPluginByName(name string) (p Plugin, err error)
	GetPluginList(query bson.M, opts *mongo.FindOptions) (res []Plugin, err error)
}
