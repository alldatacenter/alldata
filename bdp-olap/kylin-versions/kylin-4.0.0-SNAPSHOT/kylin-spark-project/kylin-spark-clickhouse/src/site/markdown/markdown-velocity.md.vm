Markdown Format with Velocity works
---------------

#[[###]]# But Markdown conflicts with Velocity on `${esc.h}${esc.h}` syntax

Since `${esc.h}${esc.h}` denotes a
[single line comment](http://velocity.apache.org/engine/1.7/vtl-reference.html#single-line-comments) in Velocity,
Markdown headers using this syntax are suppressed from generated content.

You can use [unparsed content syntax](http://velocity.apache.org/engine/1.7/vtl-reference.html#unparsed-content)
`${esc.h}[[#[[##]]#]]${esc.h}` or
[escape tool](http://velocity.apache.org/tools/2.0/apidocs/org/apache/velocity/tools/generic/EscapeTool.html)
like `${esc.d}{esc.h}${esc.d}{esc.h}`.
