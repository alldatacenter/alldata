package entity

import (
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
)

type Result bson.M

func (r *Result) GetTaskId() (id primitive.ObjectID) {
	res, ok := (*r)["_tid"]
	if !ok {
		return id
	}
	id, _ = res.(primitive.ObjectID)
	return id
}

func (r *Result) SetTaskId(id primitive.ObjectID) {
	(*r)["_tid"] = id
}
