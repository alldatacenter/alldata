package entity

import "go.mongodb.org/mongo-driver/bson/primitive"

type TaskMessage struct {
	TaskId primitive.ObjectID `json:"task_id"`
	Status string             `json:"status"`
	Error  string             `json:"error"`
}
