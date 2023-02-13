//@ts-nocheck
import Quill from 'quill';
import CalcFieldBlot from './CalcFieldBlot';

Quill.register(CalcFieldBlot);
function _classCallCheck(instance, Constructor) {
  if (!(instance instanceof Constructor)) {
    throw new TypeError('Cannot call a class as a function');
  }
}

function _defineProperties(target, props) {
  for (var i = 0; i < props.length; i++) {
    var descriptor = props[i];
    descriptor.enumerable = descriptor.enumerable || false;
    descriptor.configurable = true;
    if ('value' in descriptor) descriptor.writable = true;
    Object.defineProperty(target, descriptor.key, descriptor);
  }
}

function _createClass(Constructor, protoProps, staticProps) {
  if (protoProps) _defineProperties(Constructor.prototype, protoProps);
  if (staticProps) _defineProperties(Constructor, staticProps);
  return Constructor;
}

function _extends() {
  const _extends =
    Object.assign ||
    function (target) {
      for (var i = 1; i < arguments.length; i++) {
        var source = arguments[i];

        for (var key in source) {
          if (Object.prototype.hasOwnProperty.call(source, key)) {
            target[key] = source[key];
          }
        }
      }

      return target;
    };

  return _extends.apply(this, arguments);
}

function _unsupportedIterableToArray(o, minLen) {
  if (!o) return;
  if (typeof o === 'string') return _arrayLikeToArray(o, minLen);
  var n = Object.prototype.toString.call(o).slice(8, -1);
  if (n === 'Object' && o.constructor) n = o.constructor.name;
  if (n === 'Map' || n === 'Set') return Array.from(o);
  if (n === 'Arguments' || /^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array$/.test(n))
    return _arrayLikeToArray(o, minLen);
}

function _arrayLikeToArray(arr, len) {
  if (len == null || len > arr.length) len = arr.length;

  for (var i = 0, arr2 = new Array(len); i < len; i++) arr2[i] = arr[i];

  return arr2;
}

function _createForOfIteratorHelper(o, allowArrayLike) {
  var it;

  if (typeof Symbol === 'undefined' || o[Symbol.iterator] == null) {
    if (
      Array.isArray(o) ||
      (it = _unsupportedIterableToArray(o)) ||
      (allowArrayLike && o && typeof o.length === 'number')
    ) {
      if (it) o = it;
      var i = 0;

      var F = function () {};

      return {
        s: F,
        n: function () {
          if (i >= o.length)
            return {
              done: true,
            };
          return {
            done: false,
            value: o[i++],
          };
        },
        e: function (e) {
          throw e;
        },
        f: F,
      };
    }

    throw new TypeError(
      'Invalid attempt to iterate non-iterable instance.\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method.',
    );
  }

  var normalCompletion = true,
    didErr = false,
    err;
  return {
    s: function () {
      it = o[Symbol.iterator]();
    },
    n: function () {
      var step = it.next();
      normalCompletion = step.done;
      return step;
    },
    e: function (e) {
      didErr = true;
      err = e;
    },
    f: function () {
      try {
        if (!normalCompletion && it.return != null) it.return();
      } finally {
        if (didErr) throw err;
      }
    },
  };
}

var Keys = {
  TAB: 9,
  ENTER: 13,
  ESCAPE: 27,
  UP: 38,
  DOWN: 40,
};

function getFieldCharIndex(text, numberFieldDenotationChars) {
  return numberFieldDenotationChars.reduce(
    function (prev, numberFieldChar) {
      var numberFieldCharIndex = text.lastIndexOf(numberFieldChar);

      if (numberFieldCharIndex > prev.numberFieldCharIndex) {
        return {
          numberFieldChar: numberFieldChar,
          numberFieldCharIndex: numberFieldCharIndex,
        };
      }

      return {
        numberFieldChar: prev.numberFieldChar,
        numberFieldCharIndex: prev.numberFieldCharIndex,
      };
    },
    {
      numberFieldChar: null,
      numberFieldCharIndex: -1,
    },
  );
}

function hasValidChars(text, allowedChars) {
  return allowedChars.test(text);
}

function hasValidFieldCharIndex(numberFieldCharIndex, text, isolateChar) {
  if (numberFieldCharIndex > -1) {
    if (
      isolateChar &&
      !(
        numberFieldCharIndex === 0 ||
        !!text[numberFieldCharIndex - 1].match(/\s/g)
      )
    ) {
      return false;
    }

    return true;
  }

  return false;
}

var CalcField = (function () {
  function CalcField(quill, options) {
    var _this = this;

    _classCallCheck(this, CalcField);

    this.isOpen = false;
    this.itemIndex = 0;
    this.numberFieldCharPos = null;
    this.cursorPos = null;
    this.values = [];
    this.suspendMouseEnter = false; //this token is an object that may contains one key "abandoned", set to
    //true when the previous source call should be ignored in favor or a
    //more recent execution.  This token will be null unless a source call
    //is in progress.

    this.existingSourceExecutionToken = null;
    this.quill = quill;
    this.options = {
      source: null,
      numberFieldDenotationChars: ['@'],
      showDenotationChar: true,
      allowedChars: /^[a-zA-Z0-9_]*$/,
      minChars: 0,
      maxChars: 31,
      offsetTop: 2,
      offsetLeft: 0,
      isolateCharacter: false,
      fixFieldsToQuill: false,
      positioningStrategy: 'normal',
      defaultMenuOrientation: 'bottom',
      blotName: 'calcfield',
      dataAttributes: [
        'id',
        'value',
        'denotationChar',
        'link',
        'target',
        'disabled',
        'viewId',
        'model',
        'text',
        'agg',
        'size',
        'font-size',
      ],
      linkTarget: '_blank',

      // Style options
      spaceAfterInsert: true,
      selectKeys: [Keys.ENTER],
    };
    _extends(this.options, options, {
      dataAttributes: Array.isArray(options.dataAttributes)
        ? this.options.dataAttributes.concat(options.dataAttributes)
        : this.options.dataAttributes,
    }); //create calcfield container

    quill.on('text-change', this.onTextChange.bind(this));
    quill.on('selection-change', this.onSelectionChange.bind(this)); //Pasting doesn't fire selection-change after the pasted text is
    //inserted, so here we manually trigger one

    quill.container.addEventListener('paste', function () {
      setTimeout(function () {
        var range = quill.getSelection();
        _this.onSelectionChange(range);
      });
    });

    var _iterator = _createForOfIteratorHelper(this.options.selectKeys),
      _step;

    try {
      for (_iterator.s(); !(_step = _iterator.n()).done; ) {
        var selectKey = _step.value;
        quill.keyboard.addBinding({
          key: selectKey,
        });
      }
    } catch (err) {
      _iterator.e(err);
    } finally {
      _iterator.f();
    }
  }

  _createClass(CalcField, [
    {
      key: 'insertItem',
      value: function insertItem(data, programmaticInsert) {
        var render = data;

        if (render === null) {
          return;
        }

        if (!this.options.showDenotationChar) {
          render.denotationChar = '';
        }

        var insertAtPos;

        if (!programmaticInsert) {
          insertAtPos = this.numberFieldCharPos;
          this.quill.deleteText(
            this.numberFieldCharPos,
            this.cursorPos - this.numberFieldCharPos,
            Quill.sources.USER,
          );
        } else {
          insertAtPos = this.cursorPos;
        }

        this.quill.insertEmbed(
          insertAtPos,
          this.options.blotName,
          render,
          Quill.sources.USER,
        );

        if (this.options.spaceAfterInsert) {
          this.quill.insertText(insertAtPos + 1, ' ', Quill.sources.USER); // setSelection here sets cursor position

          this.quill.setSelection(insertAtPos + 2, Quill.sources.USER);
        } else {
          this.quill.setSelection(insertAtPos + 1, Quill.sources.USER);
        }
      },
    },
    {
      key: 'getTextBeforeCursor',
      value: function getTextBeforeCursor() {
        var startPos = Math.max(0, this.cursorPos - this.options.maxChars);
        var textBeforeCursorPos = this.quill.getText(
          startPos,
          this.cursorPos - startPos,
        );
        return textBeforeCursorPos;
      },
    },
    {
      key: 'onSomethingChange',
      value: function onSomethingChange() {
        var _this5 = this;

        var range = this.quill.getSelection();
        if (range == null) return;
        this.cursorPos = range.index;
        var textBeforeCursor = this.getTextBeforeCursor();

        var _getFieldCharIndex = getFieldCharIndex(
            textBeforeCursor,
            this.options.numberFieldDenotationChars,
          ),
          numberFieldChar = _getFieldCharIndex.numberFieldChar,
          numberFieldCharIndex = _getFieldCharIndex.numberFieldCharIndex;

        if (
          hasValidFieldCharIndex(
            numberFieldCharIndex,
            textBeforeCursor,
            this.options.isolateCharacter,
          )
        ) {
          var numberFieldCharPos =
            this.cursorPos - (textBeforeCursor.length - numberFieldCharIndex);
          this.numberFieldCharPos = numberFieldCharPos;
          var textAfter = textBeforeCursor.substring(
            numberFieldCharIndex + numberFieldChar.length,
          );

          if (
            textAfter.length >= this.options.minChars &&
            hasValidChars(textAfter, this.getAllowedCharsRegex(numberFieldChar))
          ) {
            if (this.existingSourceExecutionToken) {
              this.existingSourceExecutionToken.abandoned = true;
            }

            var sourceRequestToken = {
              abandoned: false,
            };
            this.existingSourceExecutionToken = sourceRequestToken;
            this.options.source(
              textAfter,
              function (data, searchTerm) {
                if (sourceRequestToken.abandoned) {
                  return;
                }

                _this5.existingSourceExecutionToken = null;
              },
              numberFieldChar,
            );
          } else {
          }
        } else {
        }
      },
    },
    {
      key: 'getAllowedCharsRegex',
      value: function getAllowedCharsRegex(denotationChar) {
        if (this.options.allowedChars instanceof RegExp) {
          return this.options.allowedChars;
        } else {
          return this.options.allowedChars(denotationChar);
        }
      },
    },
    {
      key: 'onTextChange',
      value: function onTextChange(delta, oldDelta, source) {
        if (source === 'user') {
          this.onSomethingChange();
        }
      },
    },
    {
      key: 'onSelectionChange',
      value: function onSelectionChange(range) {
        if (range && range.length === 0) {
          this.onSomethingChange();
        }
      },
    },
  ]);

  return CalcField;
})();

Quill.register('modules/calcfield', CalcField);
export default CalcField;
