package models

import (
	"github.com/crawlab-team/crawlab-core/env/deps/entity"
	"go.mongodb.org/mongo-driver/bson/primitive"
)

type Dependency struct {
	Id            primitive.ObjectID      `json:"_id" bson:"_id"`
	NodeId        primitive.ObjectID      `json:"node_id" bson:"node_id"`
	Type          string                  `json:"type" bson:"type"`
	Name          string                  `json:"name" bson:"name"`
	Version       string                  `json:"version" bson:"version"`
	LatestVersion string                  `json:"latest_version,omitempty" bson:"latest_version,omitempty"`
	Description   string                  `json:"description" bson:"description"`
	Result        entity.DependencyResult `json:"result" bson:"-"`
}
