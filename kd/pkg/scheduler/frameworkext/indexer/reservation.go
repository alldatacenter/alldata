/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package indexer

import (
	"fmt"

	"k8s.io/client-go/tools/cache"

	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	koordinatorinformers "github.com/koordinator-sh/koordinator/pkg/client/informers/externalversions"
)

const (
	// ReservationStatusNodeNameIndex is the lookup name for the index function, which is to index by the status.nodeName field.
	ReservationStatusNodeNameIndex string = "status.nodeName"
)

func init() {
	addIndexerFuncList = append(addIndexerFuncList, addReservationIndexer)
}

// reservationStatusNodeNameIndexFunc is an index function that indexes based on a reservation's status.nodeName
func reservationStatusNodeNameIndexFunc(obj interface{}) ([]string, error) {
	r, ok := obj.(*schedulingv1alpha1.Reservation)
	if !ok {
		return []string{}, nil
	}
	if len(r.Status.NodeName) <= 0 {
		return []string{}, nil
	}
	return []string{r.Status.NodeName}, nil
}

func addReservationIndexer(koordinatorSharedInformerFactory koordinatorinformers.SharedInformerFactory) error {
	reservationInterface := koordinatorSharedInformerFactory.Scheduling().V1alpha1().Reservations()
	reservationInformer := reservationInterface.Informer()
	// index reservation with status.nodeName; avoid duplicate add
	if reservationInformer.GetIndexer().GetIndexers()[ReservationStatusNodeNameIndex] == nil {
		err := reservationInformer.AddIndexers(cache.Indexers{ReservationStatusNodeNameIndex: reservationStatusNodeNameIndexFunc})
		if err != nil {
			return fmt.Errorf("failed to add indexer, err: %s", err)
		}
	}
	return nil
}
