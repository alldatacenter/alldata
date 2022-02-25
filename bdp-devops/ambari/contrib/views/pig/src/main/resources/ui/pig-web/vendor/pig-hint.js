(function () {

  var pigKeywordsU = pigKeywordsL = pigTypesU = pigTypesL = pigBuiltinsU = pigBuiltinsL = [];

  var mimeMode = CodeMirror.mimeModes['text/x-pig'];

  Object.keys(mimeMode.keywords).forEach( function(w) {
    pigKeywordsU.push(w.toUpperCase());
    pigKeywordsL.push(w.toLowerCase());
  });

  Object.keys(mimeMode.types).forEach( function(w) {
    pigTypesU.push(w.toUpperCase());
    pigTypesL.push(w.toLowerCase());
  });

  Object.keys(mimeMode.builtins).forEach( function(w) {
    pigBuiltinsU.push(w.toUpperCase());
    pigBuiltinsL.push(w.toLowerCase());
  });

  function forEach(arr, f) {
    for (var i = 0, e = arr.length; i < e; ++i) {
      f(arr[i]);
    }
  }

  function arrayContains(arr, item) {
    if (!Array.prototype.indexOf) {
      var i = arr.length;
      while (i--) {
        if (arr[i] === item) {
          return true;
        }
      }
      return false;
    }
    return arr.indexOf(item) != -1;
  }

  function scriptHint(editor, keywords, getToken) {
    // Find the token at the cursor
    var cur = editor.getCursor(), token = getToken(editor, cur), tprop = token;
    // If it's not a 'word-style' token, ignore the token.

    if (!/^[\w$_]*$/.test(token.string)) {
        token = tprop = {start: cur.ch, end: cur.ch, string: "", state: token.state,
                         type: token.string == ":" ? "pig-type" : null};
    }

    if (!context) var context = [];
    context.push(tprop);

    completionList = getCompletions(token, context);
    completionList = completionList.sort();

    return {list: completionList,
              from: {line: cur.line, ch: token.start},
              to: {line: cur.line, ch: token.end}};
  }

  function toTitleCase(str) {
     return str.replace(/(?:^|\s)\w/g, function(match) {
         return match.toUpperCase();
     });
  }


  function getCompletions(token, context) {
    var found = [], start = token.string;
    function maybeAdd(str) {
      if (str.indexOf(start) == 0 && !arrayContains(found, str)) found.push(str);
    }

    function gatherCompletions(obj) {
      if(obj == ":") {
        forEach(pigTypesL, maybeAdd);
      }
      else {
        forEach(pigBuiltinsU, maybeAdd);
        forEach(pigBuiltinsL, maybeAdd);
        forEach(pigTypesU, maybeAdd);
        forEach(pigTypesL, maybeAdd);
        forEach(pigKeywordsU, maybeAdd);
        forEach(pigKeywordsL, maybeAdd);
      }
    }

    if (context) {
      // If this is a property, see if it belongs to some object we can
      // find in the current environment.
      var obj = context.pop(), base;

      if (obj.type == "pig-word")
          base = obj.string;
      else if(obj.type == "pig-type")
          base = ":" + obj.string;

      while (base != null && context.length)
        base = base[context.pop().string];
      if (base != null) gatherCompletions(base);
    }
    return found;
  }

  CodeMirror.registerHelper("hint", "pig", function(cm, options) {
    return scriptHint(cm, pigKeywordsU, function (e, cur) {return e.getTokenAt(cur);});
  });

})();
