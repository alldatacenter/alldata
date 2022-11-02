Spline ArangoDB AQL functions are defined in JS files in the following pattern:

(e.g. for a function SPLINE::FOO_BAR_BAZ taking two arguments x and y)

Filename:

    foo_bar_baz.js

Content:

    (function () {
        'use strict';

        return (x, y) => z
    })
