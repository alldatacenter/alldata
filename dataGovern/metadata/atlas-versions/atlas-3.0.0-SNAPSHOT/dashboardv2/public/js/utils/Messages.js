/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

define(['require'], function(require) {
    'use strict';

    var Messages = {
        defaultErrorMessage: "Something went wrong",
        addSuccessMessage: " created successfully",
        addErrorMessage: " could not be Created",
        removeTag: "Remove Classification Assignment",
        deleteSuccessMessage: " deleted successfully",
        deleteErrorMessage: " could not be deleted",
        removeSuccessMessage: " removed successfully",
        removeErrorMessage: " could not be removed",
        editSuccessMessage: " updated successfully",
        assignDeletedEntity: " is deleted, Classification cannot be assigned",
        assignTermDeletedEntity: " is deleted, Term cannot be assigned",
        conformation: {
            deleteMessage: "Are you sure you want to delete "
        },
        search: {
            noRecordForPage: "No record found at ",
            onSamePage: "You are on the same page!",
            notExists: "Invalid Expression or Classification/Type has been deleted.",
            favoriteSearch: {
                save: "Do you want to overwrite ",
                notSelectedFavoriteElement: "Please select any one favorite search",
                notSelectedSearchFilter: "Please select at least one filter"
            }
        },
        tag: {
            addAttributeSuccessMessage: "Classification attribute is added successfully",
            updateTagDescriptionMessage: "Classification description is updated successfully"
        },
        glossary: {
            removeTermfromCategory: "Remove Term Assignment",
            removeTermfromEntity: "Remove Term Assignment",
            removeCategoryfromTerm: "Remove Category Assignment"
        },
        getAbbreviationMsg: function(abbrev, type) {
            var msg = abbrev ? "s were" : " was";
            return msg + this[type];
        }
    };
    return Messages;
});