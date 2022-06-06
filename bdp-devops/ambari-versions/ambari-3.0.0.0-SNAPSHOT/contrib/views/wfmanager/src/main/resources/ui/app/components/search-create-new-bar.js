/*
 *    Licensed to the Apache Software Foundation (ASF) under one or more
 *    contributor license agreements.  See the NOTICE file distributed with
 *    this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0
 *    (the "License"); you may not use this file except in compliance with
 *    the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

import Ember from 'ember';

export default Ember.Component.extend(Ember.Evented,{
    filter : {},
    history: Ember.inject.service(),
    startDate : '',
    endDate : '',
    tags : Ember.A([]),
    jobTypeChanged : Ember.observer('jobType', function() {
      this.$('#search-field').tagsinput('removeAll');
      this.set('startDate','');
      this.set('endDate','');
    }),
    populateFilters : function (){
      this.set('tags', Ember.A([]));
      var previousFilters = this.get('history').getSearchParams();
      if(!previousFilters || !previousFilters.filter){
        return;
      }else{
        var filterArray = previousFilters.filter.split(";");
        filterArray.forEach(function(value){
          if (value.length < 1) {
            return;
          }
          var valueArr = value.split("=");
          if(valueArr[0] !== 'startCreatedTime' && valueArr[0] !== 'endCreatedTime'){
            this.get('tags').pushObject(valueArr[0] + ":" + valueArr[1]);
          }else if(valueArr[0] === 'startCreatedTime'){
            this.set('startDate',valueArr[1]);
          }else if(valueArr[0] === 'endCreatedTime'){
            this.set('endDate',valueArr[1]);
          }
        }.bind(this));
      }
    }.on('init'),

    displayType : Ember.computed('jobType', function() {
      if(this.get('jobType') === 'wf'){
          return "Workflows";
      }else if(this.get('jobType') === 'coords'){
          return "Coordinators";
      }
      else if(this.get('jobType') === 'bundles'){
          return "Bundles";
      }
      return "Workflows";
    }),
    initializeDatePickers : function(){
      this.$('#startDate').datetimepicker({
        useCurrent: false,
        showClose : true,
        defaultDate : this.get('startDate')
      });
      this.$('#endDate').datetimepicker({
          useCurrent: false, //Important! See issue #1075
          showClose : true,
            defaultDate : this.get('endDate')
      });
      this.$("#startDate").on("dp.change", function (e) {
          this.$('#endDate').data("DateTimePicker").minDate(e.date);
          this.filterByDate(e.date,'start');
      }.bind(this));
      this.$("#endDate").on("dp.change", function (e) {
          this.$('#startDate').data("DateTimePicker").maxDate(e.date);
          this.filterByDate(e.date,'end');
      }.bind(this));
    }.on('didInsertElement'),
    refresh : function(){
      var self = this;
      var source = ['Status:RUNNING',
                    'Status:SUSPENDED',
                    'Status:SUCCEEDED',
                    'Status:KILLED',
                    'Status:FAILED',
                    'Status:PREP'];
      var substringMatcher = function(strs) {
        return function findMatches(q, cb) {
          var searchTerm =  self.$('#search-field').tagsinput('input').val();
          var originalLength = strs.length;
          if(self.get('jobType') && self.get('jobType') !== 'wf'){
            strs.pushObjects(['Status:PREPSUSPENDED','Status:PREPPAUSED','Status:DONEWITHERROR']);
          }
          strs.pushObjects(['Name:'+ searchTerm, 'User:'+ searchTerm, 'Job id:'+ searchTerm]);
          var newLength = strs.length;
          var matches, substrRegex;
          matches = [];
          substrRegex = new RegExp(q, 'i');
          strs.forEach(function(str) {
            if (substrRegex.test(str)) {
              matches.push(str);
            }
          });
          strs.splice(originalLength, newLength - originalLength);
          cb(matches);
        };
      };
      this.$('#search-field').tagsinput({
          typeaheadjs: {
            name: 'source',
            source: substringMatcher(source),
            highlight : true
          }
      });
      this.get('tags').forEach(function(value){
        this.$('#search-field').tagsinput('add', value);
      }.bind(this));
      this.$('#search-field').tagsinput('refresh');

      this.$('#search-field').on('itemAdded itemRemoved',function(){
        var searchTerms = this.$('#search-field').tagsinput('items');
        var filter = searchTerms.map(function(value){

          var eachTag = value.split(":");
          return self.mapSearchItems(eachTag[0])+"="+eachTag[1];
        });
        if(filter.length > 0){
          this.filter.tags = filter.join(";");
        }else {
          this.filter.tags = [];
        }
        this.sendAction('onSearch', { type: this.get('jobType'), filter: this.getAllFilters()});
      }.bind(this));
      this.$('#search-field').on('beforeItemAdd',function(event){
        var tag = event.item.split(":");
        if(tag.length < 2){
          event.cancel = true;
          this.$('#search-field').tagsinput('add', 'Name:'+tag[0]);
        }
      }.bind(this));
    }.on('didInsertElement'),
    mapSearchItems(key){
      key = key.replace(" ", "_").toLowerCase();
      var keys = {"job_id":"id","jobid":"id"};
      if(keys[key]){
        return keys[key];
      }
      return key;
    },
    filterByDate(date, dateType){
      var queryParam;
      if(dateType === 'start'){
        queryParam = "startCreatedTime";
      }else{
        queryParam = "endCreatedTime";
      }
      if (date._isAMomentObject) {
        var dateFilter = queryParam +"="+ date.format("YYYY-MM-DDTHH:mm")+'Z';
        this.filter[queryParam] = dateFilter;
      } else {
        delete this.filter[queryParam];
      }
      this.sendAction('onSearch', { type: this.get('jobType'), filter: this.getAllFilters() });
    },
    doClearFilters(){
      this.filter={};
      this.sendAction('onSearch', { type: this.get('jobType'), filter: this.getAllFilters() });
    },
    getAllFilters(){
      var allFilters = [];
      Object.keys(this.filter).forEach(function(value){
        allFilters.push(this.filter[value]);
      }.bind(this));
      return allFilters.join(";");
    },

    actions: {
        launchDesign() {
            this.sendAction('onCreate');
        },
        search(type) {
            var filter = this.get('filterValue'),
                elem = this.$("#" + type + "_btn");
            this.$(".scope-btn").removeClass("btn-primary");
            elem.addClass("btn-primary");
            this.sendAction('onSearch', { type: type, filter: filter });
        },
        onSearchClicked(){
          var searchValue=this.$('.tt-input').val();
          if(!Ember.isBlank(searchValue)) {
            this.$('#search-field').tagsinput('add', 'Name:'+searchValue);
          }
        },
        refresh(){
          this.sendAction('onSearch', this.get('history').getSearchParams());
        },
        showDatePicker(dateType) {
          if (dateType === 'start') {
            this.$("#startDate").trigger("dp.show");
          } else {
            this.$("#endDate").trigger("dp.show");
          }
        },
        clearFilters() {
          this.$("#startDate").val('');
          this.$("#endDate").val('');
          this.$('#search-field').tagsinput('removeAll');
          this.$('.tt-input').val('');
          this.doClearFilters();
        },
        onClear(type) {
          if (type ==='start' && this.get('startDate') === "") {
            this.filterByDate("", type);
          } else if (type ==='end' && this.get('endDate') === "") {
            this.filterByDate("", type);
          }

        }
    }

});
