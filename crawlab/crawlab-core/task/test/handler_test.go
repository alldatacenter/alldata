package test

import (
	"github.com/crawlab-team/crawlab-core/constants"
	"github.com/crawlab-team/crawlab-core/interfaces"
	"github.com/stretchr/testify/require"
	"testing"
	"time"
)

func TestHandlerService_Run(t *testing.T) {
	var err error
	T.Setup(t)

	task := T.NewTask()
	err = T.schedulerSvc.Enqueue(task)
	require.Nil(t, err)

	err = T.handlerSvc.Run(task.GetId())
	require.Nil(t, err)
	time.Sleep(1 * time.Second)

	task, err = T.modelSvc.GetTaskById(task.GetId())
	require.Nil(t, err)
	require.Equal(t, constants.TaskStatusFinished, task.GetStatus())
}

func TestHandlerService_Cancel(t *testing.T) {
	var err error
	T.Setup(t)

	task := T.NewTaskLong()
	err = T.schedulerSvc.Enqueue(task)
	require.Nil(t, err)
	time.Sleep(1 * time.Second)

	err = T.handlerSvc.Run(task.GetId())
	require.Nil(t, err)
	time.Sleep(2 * time.Second)

	err = T.handlerSvc.Cancel(task.GetId())
	require.Nil(t, err)
	time.Sleep(2 * time.Second)

	task, err = T.modelSvc.GetTaskById(task.GetId())
	require.Nil(t, err)
	require.Equal(t, constants.TaskStatusCancelled, task.GetStatus())

	var n interfaces.Node
	n, err = T.modelSvc.GetNodeByKey(T.TestNode.GetKey(), nil)
	require.Nil(t, err)
	require.Equal(t, n.GetMaxRunners(), n.GetAvailableRunners())
}

func TestHandlerService_ReportStatus(t *testing.T) {
	var err error
	T.Setup(t)

	task := T.NewTaskLong()
	err = T.schedulerSvc.Enqueue(task)
	require.Nil(t, err)
	time.Sleep(1 * time.Second)

	go T.handlerSvc.ReportStatus()
	time.Sleep(2 * time.Second)

	var n interfaces.Node
	n, err = T.modelSvc.GetNodeByKey(T.TestNode.GetKey(), nil)
	require.Nil(t, err)
	require.Equal(t, n.GetMaxRunners(), n.GetAvailableRunners())

	err = T.handlerSvc.Run(task.GetId())
	require.Nil(t, err)
	time.Sleep(2 * time.Second)

	task, err = T.modelSvc.GetTaskById(task.GetId())
	require.Nil(t, err)
	require.Equal(t, constants.TaskStatusRunning, task.GetStatus())

	n, err = T.modelSvc.GetNodeByKey(T.TestNode.GetKey(), nil)
	require.Nil(t, err)
	require.Equal(t, n.GetMaxRunners()-1, n.GetAvailableRunners())
}
