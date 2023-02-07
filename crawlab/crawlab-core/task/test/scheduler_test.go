package test

import (
	"github.com/crawlab-team/crawlab-core/interfaces"
	"github.com/stretchr/testify/require"
	"testing"
)

func TestSchedulerService_Enqueue(t *testing.T) {
	var err error
	T.Setup(t)

	task := T.NewTask()

	err = T.schedulerSvc.Enqueue(task)
	require.Nil(t, err)

	task, err = T.modelSvc.GetTask(nil, nil)
	require.Nil(t, err)
	require.False(t, task.GetId().IsZero())

	tq, err := T.modelSvc.GetTaskQueueItemById(task.GetId())
	require.Nil(t, err)
	require.Equal(t, task.GetId(), tq.Id)
}

func TestSchedulerService_Dequeue(t *testing.T) {
	var err error
	T.Setup(t)

	task := T.NewTask()

	err = T.schedulerSvc.Enqueue(task)
	require.Nil(t, err)

	tasks, err := T.schedulerSvc.Dequeue()
	require.Nil(t, err)
	require.Len(t, tasks, 1)

	count, err := T.modelSvc.GetBaseService(interfaces.ModelIdTaskQueue).Count(nil)
	require.Nil(t, err)
	require.Equal(t, 0, count)

	arOld := T.TestNode.GetAvailableRunners()
	n, err := T.modelSvc.GetNodeById(T.TestNode.GetId())
	require.Nil(t, err)
	require.Equal(t, arOld-1, n.GetAvailableRunners())
}

func TestSchedulerService_Dequeue_Max(t *testing.T) {
	var err error
	T.Setup(t)

	for i := 0; i < T.TestNode.GetAvailableRunners(); i++ {
		err = T.schedulerSvc.Enqueue(T.NewTask())
		require.Nil(t, err)
	}

	tasks, err := T.schedulerSvc.Dequeue()
	require.Nil(t, err)
	require.Len(t, tasks, T.TestNode.GetAvailableRunners())

	count, err := T.modelSvc.GetBaseService(interfaces.ModelIdTaskQueue).Count(nil)
	require.Nil(t, err)
	require.Equal(t, 0, count)

	n, err := T.modelSvc.GetNodeById(T.TestNode.GetId())
	require.Nil(t, err)
	require.Equal(t, 0, n.GetAvailableRunners())
}

func TestSchedulerService_Dequeue_Overflow(t *testing.T) {
	var err error
	T.Setup(t)

	ar := T.TestNode.GetAvailableRunners()

	overflow := 10
	for i := 0; i < ar+overflow; i++ {
		err = T.schedulerSvc.Enqueue(T.NewTask())
		require.Nil(t, err)
	}

	tasks, err := T.schedulerSvc.Dequeue()
	require.Nil(t, err)
	require.Len(t, tasks, T.TestNode.GetAvailableRunners())

	count, err := T.modelSvc.GetBaseService(interfaces.ModelIdTaskQueue).Count(nil)
	require.Nil(t, err)
	require.Equal(t, overflow, count)

	n, err := T.modelSvc.GetNodeById(T.TestNode.GetId())
	require.Nil(t, err)
	require.Equal(t, 0, n.GetAvailableRunners())
}

func TestSchedulerService_Schedule(t *testing.T) {
	var err error
	T.Setup(t)

	err = T.schedulerSvc.Enqueue(T.NewTask())
	require.Nil(t, err)

	tasks, err := T.schedulerSvc.Dequeue()
	require.Nil(t, err)
	require.Len(t, tasks, 1)

	err = T.schedulerSvc.Schedule(tasks)
	require.Nil(t, err)
}

func TestSchedulerService_Schedule_Max(t *testing.T) {
	var err error
	T.Setup(t)

	ar := T.TestNode.GetAvailableRunners()
	for i := 0; i < ar; i++ {
		err = T.schedulerSvc.Enqueue(T.NewTask())
		require.Nil(t, err)
	}

	tasks, err := T.schedulerSvc.Dequeue()
	require.Nil(t, err)
	require.Len(t, tasks, ar)

	err = T.schedulerSvc.Schedule(tasks)
	require.Nil(t, err)
}
