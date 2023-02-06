// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package jsonpath

import (
	"encoding/json"
	"reflect"
	"testing"
)

var data = map[string]interface{}{
	"user": map[string]interface{}{
		"firstname": "seth",
		"lastname":  "rogen",
	},
	"age": 35,
	"filmography": map[string]interface{}{
		"movies": []string{
			"This Is The End",
			"Superbad",
			"Neighbors",
		},
	},
}

func TestGet(t *testing.T) {
	result, err := Get(data, "user.firstname")
	if err != nil {
		t.Errorf("failed to get user.firstname")
	}
	if result != "seth" {
		t.Errorf("wrong get value, wanted %v, got %v", "seth", result)
	}

	result, err = Get(data, "filmography.movies[1]")
	if err != nil {
		t.Errorf("failed to get filmography.movies[1]")
	}
	if result != "Superbad" {
		t.Errorf("wrong get value, wanted %v, got %v", "Superbad", result)
	}

	result, err = Get(data, "age")
	if err != nil {
		t.Errorf("failed to get age: %v", err)
	}
	if result != 35 {
		t.Errorf("wrong get value, wanted: %v, got: %v", 35, result)
	}

	result, err = Get(data, "this.does.not[0].exist")
	if _, ok := err.(DoesNotExist); result != nil || !ok {
		t.Errorf("does not handle non-existent path correctly")
	}
}

func TestSet(t *testing.T) {
	err := Set(&data, "user.firstname", "chris")
	if err != nil {
		t.Errorf("failed to set user.firstname: %v", err)
	}

	firstname := reflect.ValueOf(data["user"]).MapIndex(reflect.ValueOf("firstname")).Interface()
	if firstname != "chris" {
		t.Errorf("set user.firstname to wrong value, wanted: %v, got: %v", "chris", firstname)
	}

	err = Set(&data, "filmography.movies[2]", "The Disaster Artist")
	if err != nil {
		t.Errorf("failed to set filmography.movies[2]: %v", err)
	}

	secondMovie := reflect.ValueOf(data["filmography"]).MapIndex(reflect.ValueOf("movies")).Elem().Index(2).Interface()
	if secondMovie != "The Disaster Artist" {
		t.Errorf("set filmography.movies[2] to wrong value, wanted: %v, got %v", "The Disaster Artist", secondMovie)
	}

	newUser := map[string]interface{}{
		"firstname": "james",
		"lastname":  "franco",
	}

	err = Set(&data, "user", &newUser)
	if err != nil {
		t.Errorf("failed to set user: %v", err)
	}

	user := data["user"]
	if !reflect.DeepEqual(newUser, user) {
		t.Errorf("set user is not equal, wanted: %v, got %v", newUser, user)
	}

	newData := map[string]interface{}{
		"hello": 12,
	}

	err = Set(&data, "this.does.not[0].exist", newData)
	if err != nil {
		t.Errorf("failed to set: %v", err)
	} else {
		exist := reflect.ValueOf(data["this"]).MapIndex(reflect.ValueOf("does")).Elem().MapIndex(reflect.ValueOf("not")).Elem().Index(0).Elem().MapIndex(reflect.ValueOf("exist")).Interface()
		if !reflect.DeepEqual(exist, newData) {
			t.Errorf("setting a nonexistant field did not work well, wanted: %#v, got %#v", newData, exist)
		}
	}
}

func TestJSON(t *testing.T) {
	test := `
{
	"pet": {
		"name": "baxter",
		"owner": {
      "name": "john doe",
      "contact": {
			  "phone": "859-289-9290"
      }
		},
		"type": "dog",
    "age": "4"
	},
	"tags": [
		12,
		true,
		{
			"hello": [
				"world"
			]
		}
	]
}
`
	var payload interface{}

	err := json.Unmarshal([]byte(test), &payload)
	if err != nil {
		t.Errorf("failed to parse: %v", err)
	}

	result, err := Get(payload, "tags[2].hello[0]")
	if result != "world" {
		t.Errorf("got wrong value from path, wanted: %v, got: %v", "world", result)
	}

	err = Set(&payload, "tags[2].hello[0]", "bobby")
	if err != nil {
		t.Errorf("failed to set: %v", err)
	}

	result, err = Get(payload, "tags[2].hello[0]")
	if result != "bobby" {
		t.Errorf("got wrong value after setting, wanted: %v, got: %v", "bobby", result)
	}

	newContact := map[string]string{
		"phone": "555-555-5555",
		"email": "baxterowner@johndoe.com",
	}
	err = Set(&payload, "pet.owner.contact", newContact)
	if err != nil {
		t.Errorf("failed to set: %v", err)
	}

	contact, err := Get(&payload, "pet.owner.contact")
	if !reflect.DeepEqual(newContact, contact) {
		t.Errorf("contact set do not equal, wanted: %v, got %v", newContact, contact)
	}

	small := `{}`

	err = json.Unmarshal([]byte(small), &payload)
	if err != nil {
		t.Errorf("failed to parse: %v", err)
	}

	err = Set(&payload, "this.is.new[3]", map[string]interface{}{
		"hello": "world",
	})
	if err != nil {
		t.Errorf("setting a nonexistant field did not work well, %v", err)
	}

	b, err := json.Marshal(payload)
	output := string(b)
	expected := `{"this":{"is":{"new":[null,null,null,{"hello":"world"}]}}}`
	if output != expected {
		t.Errorf("did not set correctly, wanted: %v, got: %v", expected, output)
	}
}

func TestErrors(t *testing.T) {
	_, err := Get(data, "where.is.this")
	if _, ok := err.(DoesNotExist); !ok && err != nil {
		t.Errorf("error retrieving value %v", err)
	}
}
