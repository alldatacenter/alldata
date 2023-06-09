/**
 * Drill SQL Definition (Forked from SqlServer definition)
 */

ace.define("ace/mode/sql_highlight_rules",["require","exports","module","ace/lib/oop","ace/mode/text_highlight_rules"], function(require, exports, module) {
"use strict";

var oop = require("../lib/oop");
var TextHighlightRules = require("./text_highlight_rules").TextHighlightRules;

var SqlHighlightRules = function() {

    //TODO: https://drill.apache.org/docs/reserved-keywords/
    //e.g. Cubing operators like ROLLUP are not listed
    //Covered: https://drill.apache.org/docs/supported-sql-commands/
    var keywords = (
        "select|insert|update|delete|from|where|and|or|group|by|order|limit|offset|having|as|case|" +
        "when|else|end|type|left|right|cross|join|on|outer|desc|asc|union|create|table|key|if|lateral|apply|unnest|" +
        "not|default|null|inner|database|drop|" +
        "flatten|kvgen|columns|" +
        "set|reset|alter|session|system|" +
        "temporary|function|using|jar|between|distinct|" +
        "partition|view|schema|files|" +
        "explain|plan|with|without|implementation|" +
        "show|describe|use|" +
        "analyze|refresh|metadata|none|level|compute|estimate|statistics|sample|percent|exists"
    );
    //Confirmed to be UnSupported as of Drill-1.12.0
    /* natural|primary|foreign|references|grant */

    var builtinConstants = (
        "true|false"
    );

    //Drill-specific (auto-generated: See DRILL-6084)
    var builtinFunctions = (
        "__DRILL_FUNCTIONS__"
    );

    //Drill-specific
    var dataTypes = (
        "BIGINT|BINARY|BOOLEAN|CHAR|CHARACTER|DATE|DEC|DECIMAL|DOUBLE|FIXED16CHAR|FIXEDBINARY|FLOAT|INT|" +
        "INTEGER|INTERVAL|INTERVALDAY|INTERVALYEAR|NUMERIC|NULL|SMALLINT|TIME|TIMESTAMP|VARBINARY|" +
        "VAR16CHAR|VARCHAR");
    //[Cannot supported due to space]
    //DOUBLE PRECISION|CHARACTER VARYING;

    var keywordMapper = this.createKeywordMapper({
        "support.function": builtinFunctions,
        "keyword": keywords,
        "constant.language": builtinConstants,
        "storage.type": dataTypes
    }, "identifier", true);

    this.$rules = {
        "start" : [ {
            token : "comment",
            regex : "--.*$"
        },  {
            token : "comment",
            start : "/\\*",
            end : "\\*/"
        }, {
            token : "string",           // " string
            regex : '".*?"'
        }, {
            token : "string",           // ' string
            regex : "'.*?'"
        }, {
            token : "string",           // ` string (apache drill)
            regex : "`.*?`"
        }, {
            token : "constant.numeric", // float
            regex : "[+-]?\\d+(?:(?:\\.\\d*)?(?:[eE][+-]?\\d+)?)?\\b"
        }, {
            token : keywordMapper,
            regex : "[a-zA-Z_$][a-zA-Z0-9_$]*\\b"
        }, {
            token : "keyword.operator",
            regex : "\\+|\\-|\\/|\\/\\/|%|<@>|@>|<@|&|\\^|~|<|>|<=|=>|==|!=|<>|="
        }, {
            token : "paren.lparen",
            regex : "[\\(]"
        }, {
            token : "paren.rparen",
            regex : "[\\)]"
        }, {
            token : "text",
            regex : "\\s+"
        } ]
    };
    this.normalizeRules();
};

oop.inherits(SqlHighlightRules, TextHighlightRules);

exports.SqlHighlightRules = SqlHighlightRules;
});

ace.define("ace/mode/sql",["require","exports","module","ace/lib/oop","ace/mode/text","ace/mode/sql_highlight_rules"], function(require, exports, module) {
"use strict";

var oop = require("../lib/oop");
var TextMode = require("./text").Mode;
var SqlHighlightRules = require("./sql_highlight_rules").SqlHighlightRules;

var Mode = function() {
    this.HighlightRules = SqlHighlightRules;
    this.$behaviour = this.$defaultBehaviour;
};
oop.inherits(Mode, TextMode);

(function() {

    this.lineCommentStart = "--";

    this.$id = "ace/mode/sql";
}).call(Mode.prototype);

exports.Mode = Mode;

});
