---
name: Glossary
route: /Glossary
menu: Documentation
submenu: Features
---
import  themen  from 'theme/styles/styled-colors';
import  * as theme  from 'react-syntax-highlighter/dist/esm/styles/hljs';
import SyntaxHighlighter from 'react-syntax-highlighter';

import Img from 'theme/components/shared/Img'

# Glossary

A Glossary provides appropriate vocabularies for business users and it allows the terms (words) to be related to each
other and categorized so that they can be understood in different contexts. These terms can be then mapped to assets
like a Database, tables, columns etc. This helps abstract the technical jargon associated with the repositories and
allows the user to discover/work with data in the vocabulary that is more familiar to them.

### Use cases

* Ability to define rich glossary vocabularies using the natural terminology (technical terms and/or business terms).
* Ability to semantically relate the term(s) to each other.
* Ability to map assets to glossary terms(s).
* Ability to organize these terms by categories. This adds more context to the term(s).
* Allow categories to be arranged in hierarchy - to express broader and finer scopes.
* Separate management of glossary term(s) from the metadata.

### What is a Glossary term ?

A term is a useful word for an enterprise. For the term(s) to be useful and meaningful, they need to grouped around their
use and context. A term in Apache Atlas must have a unique qualifiedName, there can be term(s) with same name but they
cannot belong to the same glossary. Term(s) with same name can exist only across different glossaries. A term name can
contain spaces, underscores and dashes (as natural ways of referring to words) but no "." or "@", as the qualifiedName
takes the following form `term name`@`glossary qualified name`. The fully qualified name makes it easier to work with
a specific term.

A term can only belong to single glossary and it's lifecycle is bound to the same i.e. if the Glossary is deleted then
the term gets deleted as well. A term can belong to zero or more categories, which allows scoping them into narrower or
wider contexts. A term can be assigned/linked to zero or more entities in Apache Atlas. A term can be classified using
classifications (tags) and the same classification gets applied to the entities that the term is assigned to.

### What is a Glossary category ?

A category is a way of organizing the term(s) so that the term's context can be enriched. A category may or may not have
contained hierarchies i.e. child category hierarchy. A category's qualifiedName is derived using it's hierarchical location
within the glossary e.g. `Category name`.`parent category qualifiedName`. This qualified name gets updated when any
hierarchical change happens, e.g. addition of a parent category, removal of parent category or change of parent category.

### UI interactions

Apache Atlas UI has been updated to provide user-friendly interface to work with various aspects of glossary, including:

* create glossaries, terms and categories
* create various relationships between terms - like synonymns, antonymns, seeAlso
* organize categories in hierarchies
* assign terms to entities
* search for entities using associated terms

Most of glossary related UI can be found under a new tab named GLOSSARY, which is present right next to existing
familiar tabs SEARCH and CLASSIFICATION.

#### **Glossary tab**

Apache Atlas UI provides two ways to work with a glossary - term view and category view.

Term view allows an user to perform the following operations:

* create, update and delete terms
* add, remove and update classifications associated with a term
* add, remove and update categorization of a term
* create various relationships between terms
* view entities associated with a term

Category view allows an user to perform the following operations:

* create, update and delete categories and sub-categories
* associate terms to categories

Users can switch between term view and category view using toggle provided in GLOSSARY tab.

<Img src={`/images/markdown/terms_view.png`}  width="300px" />

<Img src={`/images/markdown/category_view_1.png`}  width="300px" />


##### Term context menu

* Create a new term
Clicking on the **ellipsis (...)** next to a glossary name shows a pop-over menu that allows users to create a term in
the glossary or delete the glossary - as shown below.

<Img src={`/images/markdown/term_view_context.png`}  width="360px" />


* To delete a term
Clicking on the **ellipsis (...)** next to a term name shows a pop-over menu that allows users to delete the term - as
shown below.

<Img src={`/images/markdown/term_delete_context.png`} width="600px" />


##### Term detail page
Various details of a term can be viewed by clicking on the term name in the glossary UI. Each tabs under the details
page provides different details of the term.

* Entities tab shows the entities that are assigned to the selected term
* Classifications tab shows the classification(s) associated with the selected term
* Related terms tab shows the terms that are related to the selected term

<Img src={`/images/markdown/term_details.png`} />


##### Add classification to Term

Clicking on **+** next to classification label to add a classification to the term.

<Img src={`/images/markdown/term_add_classification_1.png`} alt="Add classification"  width="400px" />

<Img src={`/images/markdown/term_add_classification_2.png`} alt="Add classification - details"  width="400px" />

<Img src={`/images/markdown/term_with_classification.png`} alt="Classifications associated with term"  width="400px" />


##### Create term relationship with other term
Click on "Related Terms" tab when viewing term details. Clicking on **+** will allow linking a term with the current term.

<Img src={`/images/markdown/terms_related_terms.png`} />


##### Categorize a term

Click on **+** next to categories label to categorize a term. A modal dialog will be presented for choosing a category.

<Img src={`/images/markdown/term_add_category.png`}  width="400px" />


#### **Category view**

When the toggle switch is on category, the panel will list down all glossaries along-with the category hierarchy. Here's a list of possible
interactions under this view.

* Expanded view
<Img src={`/images/markdown/category_view_2.png`}  width="300px" />


##### Category context menu

Clicking on **ellipsis (...)** next to the category name will present a category context menu.

* To create a new category

<Img src={`/images/markdown/category_view_glossary_context.png`}  width="600px" />

* To create a sub-category or delete a category

<Img src={`/images/markdown/category_view_category_context_1.png`}  width="600px" />


##### Category detail page

Once a category is selected, the details will be presented in the right pane.

<Img src={`/images/markdown/category_details_with_terms.png`}  width="600px" />


##### Categorize term

Click on **+** next to the terms label to link a term under selected category.

<Img src={`/images/markdown/category_add_term.png`}  width="600px" />

<Img src={`/images/markdown/category_add_term_1.png`}  width="400px" />

<Img src={`/images/markdown/category_add_term_2.png`}  width="400px" />


#### **Term assignment flow**

Terms can be assigned to an entity either from the results page or entity details page.

##### Assign term

Click on **+** under "terms" column (if associating term from search page)

<Img src={`/images/markdown/entity_search_add_term.png`} />


Click on **+** next to "terms" label (if viewing a specific entity details)

<Img src={`/images/markdown/entity_details_add_term.png`} />


Both the actions will present the following modal, follow prompts on screen to complete term assignment.

<Img src={`/images/markdown/entity_add_term_modal.png`}  width="400px" />


##### Propagated classification

If a term has classification then the entity is has been assigned inherits the same.

<Img src={`/images/markdown/term_details_with_classification.png`} />

<Img src={`/images/markdown/entity_assigned_term_with_tag.png`}  width="600px" />


#### **Search using a term**

Apache Atlas basic-search API and UI have been updated to support term as a search criteria. This allows users to find
entities associated with a given term. Basic search UI now includes a new input drop-down for term based searches.

<Img src={`/images/markdown/term_search.png`}  width="300px" />


***
#### Summary of REST interactions

Following operations are supported by Atlas, the details of REST interface can be found [here](api/v2/index.html)

##### JSON structure

* Glossary

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`{
	"guid": "2f341934-f18c-48b3-aa12-eaa0a2bfce85",
	"qualifiedName": "SampleBank",
	"displayName": "Banking",
	"shortDescription": "Glossary of bank",
	"longDescription": "Glossary of bank - long description",
	"language": "English",
	"usage": "N/A",
	"terms": [
	{
		"termGuid": "502d34f1-b85f-4ad9-9d9f-fe7020ff0acb",
		"relationGuid": "6bb803e4-3af6-4924-aad6-6ad9f95ecd14",
		"displayText": "A savings account"
	}, {
		"termGuid": "e441a540-ee55-4fc8-8eaf-4b9943d8929c",
		"relationGuid": "dbc46795-76ff-4f68-9043-be0eff0bc0f3",
		"displayText": "15-30 yr mortgage"
	}, {
		"termGuid": "998e3692-51a8-47fe-b3a0-0d9f794437eb",
		"relationGuid": "0dcd31b9-a81c-4185-ad4b-9209a97c305b",
		"displayText": "A checking account"
	}, {
		"termGuid": "c4e2b956-2589-4648-8596-240d3bea5e44",
		"relationGuid": "e71c4a5d-694b-47a5-a41e-126ade857279",
		"displayText": "ARM loans"
	}],
	"categories": [{
		"categoryGuid": "dd94859e-7453-4bc9-b634-a17fc14590f8",
		"parentCategoryGuid": "e6a3df1f-5670-4f9e-84da-91f77d008ce3",
		"relationGuid": "a0b7da02-1ccd-4415-bc54-3d0cdb8857e7",
		"displayText": "Accounts"
	}, {
		"categoryGuid": "e6a3df1f-5670-4f9e-84da-91f77d008ce3",
		"relationGuid": "0e84a358-a4aa-4bd3-b806-497a6962ae1d",
		"displayText": "Customer"
	}, {
		"categoryGuid": "7f041401-de8c-443f-a3b7-7bf5a910ff6f",
		"parentCategoryGuid": "e6a3df1f-5670-4f9e-84da-91f77d008ce3",
		"relationGuid": "7757b031-4e25-43a8-bf77-946f7f06c67a",
		"displayText": "Loans"
	}]
}`}
</SyntaxHighlighter>

* Term

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`{
	"guid": "e441a540-ee55-4fc8-8eaf-4b9943d8929c",
	"qualifiedName": "fixed_mtg@SampleBank",
	"displayName": "15-30 yr mortgage",
	"shortDescription": "Short description",
	"longDescription": "Long description",
	"examples": ["N/A"],
	"abbreviation": "FMTG",
	"anchor": {
		"glossaryGuid": "2f341934-f18c-48b3-aa12-eaa0a2bfce85",
		"relationGuid": "dbc46795-76ff-4f68-9043-be0eff0bc0f3"
	},
	"categories": [{
		"categoryGuid": "7f041401-de8c-443f-a3b7-7bf5a910ff6f",
		"relationGuid": "b4cddd33-7b0c-41e2-9324-afe549ec6ada",
		"displayText": "Loans"
	}],
    "seeAlso"           : [],
    "synonyms"          : [],
    "antonyms"          : [],
    "replacedBy"        : [],
    "replacementTerms"  : [],
    "translationTerms"  : [],
    "translatedTerms"   : [],
    "isA"               : [],
    "classifies"        : [],
    "preferredTerms"    : [],
    "preferredToTerms": [ {
                           "termGuid"   : "c4e2b956-2589-4648-8596-240d3bea5e44",
                           "displayText": "ARM Loans"
                         }]
}`}
</SyntaxHighlighter>

* Category

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`{
	"guid": "7f041401-de8c-443f-a3b7-7bf5a910ff6f",
	"qualifiedName": "Loans.Customer@HortoniaBank",
	"displayName": "Loans",
	"shortDescription": "Loan categorization",
	"anchor": {
		"glossaryGuid": "2f341934-f18c-48b3-aa12-eaa0a2bfce85",
		"relationGuid": "7757b031-4e25-43a8-bf77-946f7f06c67a"
	},
	"parentCategory": {
		"categoryGuid": "e6a3df1f-5670-4f9e-84da-91f77d008ce3",
		"relationGuid": "8a0a8e11-0bb5-483b-b7d6-cfe0b1d55ef6"
	},
	"childrenCategories" : [],
	"terms": [{
		"termGuid": "e441a540-ee55-4fc8-8eaf-4b9943d8929c",
		"relationGuid": "b4cddd33-7b0c-41e2-9324-afe549ec6ada",
		"displayText": "15-30 yr mortgage"
	}, {
		"termGuid": "c4e2b956-2589-4648-8596-240d3bea5e44",
		"relationGuid": "8db1e784-4f04-4eda-9a58-6c9535a95451",
		"displayText": "ARM loans"
	}]
}`}
</SyntaxHighlighter>

##### CREATE operations

1. Create a glossary
2. Create a term
3. Create a categorized term
4. Create term with relations
5. Create a category
6. Create a category with hierarchy
7. Create category and categorize term(s)
8. Assign term to entities

**NOTE:**

* During create operations glossary, term and category get an auto assigned GUID and qualifiedName.
* To create a category with children, the children MUST be created beforehand.
* To create a term belonging to a category, the category MUST be created beforehand.
* To create term with relations, related term(s) MUST be created beforehand.

***

##### READ operations

1. Get glossary by GUID - Gives all terms and categories (headers) belonging to the glossary.
2. Get all Glossaries - Gives all glossaries with terms and categories (headers) belonging to the respective glossary.
3. Get a term by GUID - Gives details about the term, categories it belongs to (if any) and any related term(s).
4. Get a category by GUID - Gives details about the category, category hierarchy (if any) and term(s) belonging to the category.
5. Get all terms of a given glossary - Gives all terms (with details as mentioned in #3) belonging to given glossary.
6. Get all categories of a given glossary - Gives all categories (with details as mentioned in #4) belonging to given glossary.
7. Get all terms related to given term - Gives all terms related/linked to the given term.
8. Get all categories related to a given category (parent and children)
9. Get all terms for a given category

***

##### UPDATE operations

1. Partial update of glossary
2. Partial update of term
3. Partial update of category
4. Update a given glossary
5. Update a given term
6. Update a given category

**NOTE:**

* Partial update only deals with the **_primitive attributes_** defined in the Glossary model file.
* GUID and qualifiedName can't be changed once assigned. Only way to this is to delete and recreate the required object.
* Anchors **can't** be removed in any updates
* Update APIs expect the JSON to be modified **in-place** after the **GET** call. Any **_missing_** attributes/relations will be **_deleted_**.
* Any update to category's hierarchy leads to a cascaded update of the hierarchy under it e.g. anchor change would affect all children,
   parent change would affect the qualifiedName of self and children.

***
##### DELETE operations

1. Delete glossary - Also deletes all categories and terms anchored to given glossary. This delete is **_blocked_** if any term has been **_assigned_** to an entity.
2. Delete term - Only deletes the term **_if_** it's not associated/assigned to any entity.
3. Delete category - **_Only_** deletes the given category, all children become top-level categories.
4. Remove term assignment from entity
