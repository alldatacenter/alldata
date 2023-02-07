package notification

import "go.mongodb.org/mongo-driver/bson/primitive"

type Setting struct {
	Id          primitive.ObjectID `json:"_id" bson:"_id"`
	Type        string             `json:"type" bson:"type"`
	Name        string             `json:"name" bson:"name"`
	Description string             `json:"description" bson:"description"`
	Enabled     bool               `json:"enabled" bson:"enabled"`
	Global      bool               `json:"global" bson:"global"`
	Title       string             `json:"title,omitempty" bson:"title,omitempty"`
	Template    string             `json:"template,omitempty" bson:"template,omitempty"`
	Triggers    []string           `json:"triggers" bson:"triggers"`
	Targets     []SettingTarget    `json:"targets" bson:"targets"` // TODO: implement
	Mail        SettingMail        `json:"mail,omitempty" bson:"mail,omitempty"`
	Mobile      SettingMobile      `json:"mobile,omitempty" bson:"mobile,omitempty"`
}

type SettingMail struct {
	Server         string `json:"server" bson:"server"`
	Port           string `json:"port,omitempty" bson:"port,omitempty"`
	User           string `json:"user,omitempty" bson:"user,omitempty"`
	Password       string `json:"password,omitempty" bson:"password,omitempty"`
	SenderEmail    string `json:"sender_email,omitempty" bson:"sender_email,omitempty"`
	SenderIdentity string `json:"sender_identity,omitempty" bson:"sender_identity,omitempty"`
	To             string `json:"to,omitempty" bson:"to,omitempty"`
	Cc             string `json:"cc,omitempty" bson:"cc,omitempty"`
}

type SettingMobile struct {
	Webhook string `json:"webhook" bson:"webhook"`
}

type SettingTarget struct {
	Id    primitive.ObjectID `json:"_id" bson:"_id"`
	Model string             `json:"model" bson:"model"`
}

type SettingTrigger struct {
	Name  string `json:"name" bson:"name"`
	Event string `json:"event" bson:"event"`
}
