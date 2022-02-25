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

/*jshint node:true*/
/* global require, module */
var EmberApp = require('ember-cli/lib/broccoli/ember-app');

module.exports = function(defaults) {


    var app = new EmberApp(defaults, {
        // Add options here
  codemirror: {
     modes: ['xml'],
     keyMaps: ['vim'],
     themes: ['solarized']
  }
    });

    // Use `app.import` to add additional libraries to the generated
    // output files.
    //
    // If you need to use different assets in different
    // environments, specify an object as the first parameter. That
    // object's keys should be the environment name and the values
    // should be the asset to use in that environment.
    //
    // If the library that you are including contains AMD or ES6
    // modules that you would like to import into your application
    // please specify an object with the list of modules as keys
    // along with the exports of each module as its value.

    app.import('bower_components/bootstrap/dist/css/bootstrap.css');
    app.import('bower_components/bootstrap/dist/js/bootstrap.js');

    

    app.import('bower_components/bootstrap/dist/fonts/glyphicons-halflings-regular.woff', {
        destDir: 'fonts'
    });
    app.import('bower_components/bootstrap/dist/fonts/glyphicons-halflings-regular.woff2', {
        destDir: 'fonts'
    });


    app.import('bower_components/font-awesome/css/font-awesome.min.css');
    app.import('bower_components/font-awesome/fonts/FontAwesome.otf', {
        destDir: 'fonts'
    });
    app.import('bower_components/font-awesome/fonts/fontawesome-webfont.eot', {
        destDir: 'fonts'
    });
    app.import('bower_components/font-awesome/fonts/fontawesome-webfont.svg', {
        destDir: 'fonts'
    });
    app.import('bower_components/font-awesome/fonts/fontawesome-webfont.ttf', {
        destDir: 'fonts'
    });
    app.import('bower_components/font-awesome/fonts/fontawesome-webfont.woff', {
        destDir: 'fonts'
    });
    app.import('bower_components/font-awesome/fonts/fontawesome-webfont.woff2', {
        destDir: 'fonts'
    });

    app.import('bower_components/x2js/xml2json.js');
    app.import('bower_components/form2js/src/form2js.js');
    app.import('bower_components/jquery-ui/jquery-ui.js');
    app.import('bower_components/lodash/lodash.js');

    app.import('bower_components/moment/moment.js');

    app.import('bower_components/handlebars/handlebars.js');

    app.import('bower_components/graphlib/dist/graphlib.core.js');
    app.import('bower_components/dagre/dist/dagre.core.js');

    //Datatable

    app.import('bower_components/datatables/media/js/jquery.dataTables.js');
    app.import('bower_components/datatables/media/js/dataTables.bootstrap4.js');
    app.import('bower_components/datatables/media/css/dataTables.bootstrap4.css');

    //Typeahead
    app.import('bower_components/typeahead.js/dist/typeahead.bundle.js');

    //Bootstrap Tags input
    app.import('bower_components/bootstrap-tagsinput/src/bootstrap-tagsinput.css');
    app.import('bower_components/bootstrap-tagsinput/src/bootstrap-tagsinput.js');

    //DateTimePicker
    app.import('bower_components/eonasdan-bootstrap-datetimepicker/build/js/bootstrap-datetimepicker.min.js');
    app.import('bower_components/eonasdan-bootstrap-datetimepicker/build/css/bootstrap-datetimepicker.min.css');

    //Code prettify
    app.import('bower_components/code-prettify/src/run_prettify.js')

    //x2js
    app.import('bower_components/abdmob/x2js/xml2json.min.js')

    //vkBeautify
    app.import('bower_components/vkBeautify/vkbeautify.js')
	
	//cytoscape
	app.import('bower_components/cytoscape/dist/cytoscape.js');
	
	//cytoscape-dagre
	app.import('bower_components/cytoscape-dagre/cytoscape-dagre.js');

	//cytoscape-panzoom
	app.import('bower_components/cytoscape-panzoom/cytoscape-panzoom.js');
	app.import('bower_components/cytoscape-panzoom/cytoscape.js-panzoom.css');

	// Fuse js
	app.import('bower_components/fuse.js/src/fuse.min.js');
  app.import('bower_components/jsog/lib/JSOG.js');

    return app.toTree();
};
