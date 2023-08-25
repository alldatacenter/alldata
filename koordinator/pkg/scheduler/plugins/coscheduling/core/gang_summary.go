package core

import (
	"time"

	"k8s.io/apimachinery/pkg/util/sets"
)

type GangSummary struct {
	Name                     string         `json:"name"`
	WaitTime                 time.Duration  `json:"waitTime"`
	CreateTime               time.Time      `json:"createTime"`
	Mode                     string         `json:"mode"`
	MinRequiredNumber        int            `json:"minRequiredNumber"`
	TotalChildrenNum         int            `json:"totalChildrenNum"`
	GangGroup                []string       `json:"gangGroup"`
	Children                 sets.String    `json:"children"`
	WaitingForBindChildren   sets.String    `json:"waitingForBindChildren"`
	BoundChildren            sets.String    `json:"boundChildren"`
	OnceResourceSatisfied    bool           `json:"onceResourceSatisfied"`
	ScheduleCycleValid       bool           `json:"scheduleCycleValid"`
	ScheduleCycle            int            `json:"scheduleCycle"`
	ChildrenScheduleRoundMap map[string]int `json:"childrenScheduleRoundMap"`
	GangFrom                 string         `json:"gangFrom"`
	HasGangInit              bool           `json:"hasGangInit"`
}

func (gang *Gang) GetGangSummary() *GangSummary {
	gangSummary := &GangSummary{
		Children:                 sets.NewString(),
		WaitingForBindChildren:   sets.NewString(),
		BoundChildren:            sets.NewString(),
		ChildrenScheduleRoundMap: make(map[string]int),
	}

	if gang == nil {
		return gangSummary
	}

	gang.lock.Lock()
	defer gang.lock.Unlock()

	gangSummary.Name = gang.Name
	gangSummary.WaitTime = gang.WaitTime
	gangSummary.CreateTime = gang.CreateTime
	gangSummary.Mode = gang.Mode
	gangSummary.MinRequiredNumber = gang.MinRequiredNumber
	gangSummary.TotalChildrenNum = gang.TotalChildrenNum
	gangSummary.OnceResourceSatisfied = gang.OnceResourceSatisfied
	gangSummary.ScheduleCycleValid = gang.ScheduleCycleValid
	gangSummary.ScheduleCycle = gang.ScheduleCycle
	gangSummary.GangFrom = gang.GangFrom
	gangSummary.HasGangInit = gang.HasGangInit
	gangSummary.GangGroup = append(gangSummary.GangGroup, gang.GangGroup...)

	for podName := range gang.Children {
		gangSummary.Children.Insert(podName)
	}
	for podName := range gang.WaitingForBindChildren {
		gangSummary.WaitingForBindChildren.Insert(podName)
	}
	for podName := range gang.BoundChildren {
		gangSummary.BoundChildren.Insert(podName)
	}
	for key, value := range gang.ChildrenScheduleRoundMap {
		gangSummary.ChildrenScheduleRoundMap[key] = value
	}

	return gangSummary
}
