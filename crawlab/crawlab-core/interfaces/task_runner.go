package interfaces

import (
	"go.mongodb.org/mongo-driver/bson/primitive"
	"time"
)

type TaskRunner interface {
	Init() (err error)
	Run() (err error)
	Cancel() (err error)
	Dispose() (err error)
	SetSubscribeTimeout(timeout time.Duration)
	GetTaskId() (id primitive.ObjectID)
}
