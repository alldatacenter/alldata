package interfaces

import (
	"go.mongodb.org/mongo-driver/bson/primitive"
	"time"
)

type TaskSchedulerService interface {
	TaskBaseService
	// Enqueue task into the task queue
	Enqueue(t Task) (err error)
	// DequeueAndSchedule continuously dequeue task and schedule to corresponding node
	DequeueAndSchedule()
	// Dequeue task with node info from the task queue
	Dequeue() (tasks []Task, err error)
	// Schedule task to corresponding node
	Schedule(tasks []Task) (err error)
	// Cancel task to corresponding node
	Cancel(id primitive.ObjectID, args ...interface{}) (err error)
	// SetInterval set the interval or duration between two adjacent fetches
	SetInterval(interval time.Duration)
}
