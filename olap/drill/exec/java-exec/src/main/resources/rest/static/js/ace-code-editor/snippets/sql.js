/**
 * Drill SQL Syntax Snippets
 */

ace.define("ace/snippets/sql",["require","exports","module"], function(require, exports, module) {
"use strict";

exports.snippetText = "snippet info\n\
	select * from INFORMATION_SCHEMA.${1:<tableName>};\n\
snippet sysmem\n\
	select * from sys.memory;\n\
snippet sysopt\n\
	select * from sys.options;\n\
snippet sysbit\n\
	select * from sys.drillbits;\n\
snippet sysconn\n\
	select * from sys.connections;\n\
snippet sysprof\n\
	select * from sys.profiles;\n\
snippet cview\n\
	create view ${1:[workspace]}.${2:<viewName>} ( ${3:<columnName>} )  as \n\
	${4:<query>};\n\
snippet ctas\n\
	create table ${1:<tableName>} ( ${2:<columnName>} )  as \n\
	${3:<query>};\n\
snippet ctemp\n\
	create temporary table ${1:<tableName>} ( ${2:<columnName>} )  as \n\
	${3:<query>};\n\
snippet cfnjar\n\
	create function using jar '${1:<jarName>}.jar';\n\
snippet alt\n\
	alter session set `${1:<parameter>}` = ${2:<value>};\n\
snippet reset\n\
	alter session reset `${1:<parameter>}`;\n\
snippet explain\n\
	explain plan for\n\
	${1:<query>};\n\
snippet s*\n\
	select *\n\
	from ${1:<tableName>}\n\
	where ${2:<condition>};\n\
snippet cast\n\
	cast(${1:<columnName>} AS ${2:<dataType>}) ${3:<alias>}\n\
";

exports.scope = "sql";

});
