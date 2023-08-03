
````text
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
````

# Ranger External User Store Retrievers

A library to retrieve userStore entries from sources external to the Ranger Admin User Store. The top level class is called RangerMultiSourceUserStoreRetriever.

## Business Value

A counterpart of RangerAdminUserStoreRetriever, instead of retrieving items from the internal Ranger userStore,
RangerMultiSourceUserStoreRetriever retrieves userStore entries from other sources.  The userStore entries will persist
while the plugin is up, and will be refreshed at configured intervals.

This enables ABAC (Attribute-based Access Control) based on the user's attributes that are retrieved.
For example, the API could return the set of business partners that the user has been granted access to.
Thus, a row filter policy can be built using a condition like this:
````text
${{USER.partner}}.includes(partner)
````
where partner is the name of a column in a hive table.  This enables
row filter policies in which a user might match multiple conditions, which is not possible with out-of-the-box Ranger.

## Currently Supported Sources

### Arbitrary API calls (source name: "api")

This code enables additions to the UserStore to be retrieved simply by creating an API which
returns data in userStore format, and including attrName, userStoreURL, and optionally
configFile and dataFile as contextEnricher options in the host plugin's service definition.

#### Configuration Items

Configurations are specified in the host plugin's service definition, as enricherOptions in the contextEnricher
definition.  Configurations specific to this source type:

**attrName** is the attribute whose values are mapped to the user, i.e., the key to be used in the userStore:
user -> **attrName** -> attrValues.
In Ranger policies it appears in ${{USER.**attrName**}} syntax, eg ${{USER.partner}}

**userStoreURL** is the URL from which to retrieve the user-to-attribute mapping.

**dataFile** is a local java properties file from which the user-to-attribute mapping can be retrieved. It is optional,
and intended to be used primarily in development.

**configFile** is the name of the file containing the Base64-encoded secrets needed as inputs to retrieve
the Bearer Token needed for access to the userStoreURL. It is optional, for security reasons.  If configFile doesn't
appear in the EnricherOptions, a default value is constructed as "/var/ranger/security/"+attrName+".conf".

The config file is a required JSON file which must contain:
- **tokenUrl** : the name of the url to call to retrieve the Bearer Token
- **headers**: list of key-value pairs representing names and values of http headers for the call to the tokenUrl.
  Note: Content-Type is assumed to be "application/x-www-form-urlencoded".  Inclusion of a different content-type header in the config file will cause a 400 error.
- **params**: name-value pairs to be added as parameters to the URI's query portion

Here are the contents of an **example configFile**:
```json
{
  "tokenUrl": "https://security.mycompany.com/token.oauth2",
  "headers": [
    { "Content-Type": "application/x-www-form-urlencoded" } ,
    { "Accept":       "application/json" }
  ],
  "params": [
    { "client_id":     "my_user_name" },
    { "client_secret": "***************" },
    { "grant_type":    "client_credentials" },
    { "scope":         "my_project" }
  ]
}
```

### RangerRoles (source name: "role")

In this case, attributes are retrieved internally from Ranger, based on the
roles of which the user is a member.  No additional coding is needed.

#### Configuration Items

Configurations are specified in the host plugin's service definition, as enricherOptions in the contextEnricher
definition.  Instead of external storage configurations (eg URL, datafile), configurations
specify how to retrieve the roles of interest:

**attrName** is the attribute whose values are mapped to the user, i.e., the key to be used in the userStore:
user -> **attrName** -> attrValues.  It is also the string used to identify the role of interest.  By convention,
role names are assumed to have this structure:  _attrName.attrValue_,  e.g., salesRegion.northeast.

## Service Definition Configurations
In order to ensure that all new userStore entries are retained, there must be a single userStoreRetrieverClass
and a single userStore for all retrievers.

**Options at the Context Enricher Level:**

**userStoreRetrieverClassName** is the name of the context enricher that calls all subsequent retriever methods.
**userStoreRefresherPollingInterval** defines the interval at which the userStoreRefresher polls its source, seeking data changes since it was last refreshed.

Within the options for this enricher, configurations for the individual retrievers are given in a special format.
The option key is "retrieverX_*sourceType*", where X is a sequential integer and sourceType is (currently)
either "api" or "role".  The option value is a string containing configurations for the individual retrievers, as outlined above,
specified in a comma-separated Java Property-like format.

Here is the relevant section of a **sample host plugin's service definition**.  Two api retrievers and two role retrievers
are specified.

```json
{
  "contextEnrichers": [
    {
      "itemId":   1,
      "name":     "RangerMultiSourceUserStoreRetriever",
      "enricher": "org.apache.ranger.plugin.contextenricher.RangerUserStoreEnricher",
      "enricherOptions": {
        "userStoreRetrieverClassName":       "org.apache.ranger.plugin.contextenricher.externalretrievers.RangerMultiSourceUserStoreRetriever",
        "userStoreRefresherPollingInterval": "60000",
        "retriever0_api":                    "attrName=partner,userStoreURL=http://localhost:8000/security/getPartnersByUser",
        "retriever1_api":                    "attrName=ownedResources,dataFile=/var/ranger/data/userOwnerResource.txt",
        "retriever2_role":                   "attrName=salesRegion",
        "retriever3_role":                   "attrName=sensitivityLevel"
      }
    }
  ]
}
```
