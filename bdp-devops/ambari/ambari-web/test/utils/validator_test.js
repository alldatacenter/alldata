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

var validator = require('utils/validator');

describe('validator', function () {

  describe('#isValidEmail(value)', function () {
    it('should return false if value is null', function () {
      expect(validator.isValidEmail(null)).to.equal(false);
    });
    it('should return false if value is ""', function () {
      expect(validator.isValidEmail('')).to.equal(false);
    });
    it('should return false if value is "a.com"', function () {
      expect(validator.isValidEmail('a.com')).to.equal(false);
    });
    it('should return false if value is "@a.com"', function () {
      expect(validator.isValidEmail('@a.com')).to.equal(false);
    });
    it('should return false if value is "a@.com"', function () {
      expect(validator.isValidEmail('a@.com')).to.equal(false);
    });
    it('should return true if value is "a@a.com"', function () {
      expect(validator.isValidEmail('a@a.com')).to.equal(true);
    });
    it('should return true if value is "user@a.b.com"', function () {
      expect(validator.isValidEmail('user@a.b.com')).to.equal(true);
    })
  });

  describe('#isValidInt(value)', function () {
    it('should return false if value is null', function () {
      expect(validator.isValidInt(null)).to.equal(false);
    });
    it('should return false if value is ""', function () {
      expect(validator.isValidInt('')).to.equal(false);
    });
    it('should return false if value is "abc"', function () {
      expect(validator.isValidInt('abc')).to.equal(false);
    });
    it('should return false if value is "0xff"', function () {
      expect(validator.isValidInt('0xff')).to.equal(false);
    });
    it('should return false if value is " 1""', function () {
      expect(validator.isValidInt(' 1')).to.equal(false);
    });
    it('should return false if value is "1 "', function () {
      expect(validator.isValidInt('1 ')).to.equal(false);
    });
    it('should return true if value is "10"', function () {
      expect(validator.isValidInt('10')).to.equal(true);
    });
    it('should return true if value is "-123"', function () {
      expect(validator.isValidInt('-123')).to.equal(true);
    });
    it('should return true if value is "0"', function () {
      expect(validator.isValidInt('0')).to.equal(true);
    });
    it('should return true if value is 10', function () {
      expect(validator.isValidInt(10)).to.equal(true);
    });
    it('should return true if value is -123', function () {
      expect(validator.isValidInt(10)).to.equal(true);
    });
    it('should return true if value is 0', function () {
      expect(validator.isValidInt(10)).to.equal(true);
    })
  });

  describe('#isValidFloat(value)', function () {
    it('should return false if value is null', function () {
      expect(validator.isValidFloat(null)).to.equal(false);
    });
    it('should return false if value is ""', function () {
      expect(validator.isValidFloat('')).to.equal(false);
    });
    it('should return false if value is "abc"', function () {
      expect(validator.isValidFloat('abc')).to.equal(false);
    });
    it('should return false if value is "0xff"', function () {
      expect(validator.isValidFloat('0xff')).to.equal(false);
    });
    it('should return false if value is " 1""', function () {
      expect(validator.isValidFloat(' 1')).to.equal(false);
    });
    it('should return false if value is "1 "', function () {
      expect(validator.isValidFloat('1 ')).to.equal(false);
    });
    it('should return true if value is "10"', function () {
      expect(validator.isValidFloat('10')).to.equal(true);
    });
    it('should return true if value is "-123"', function () {
      expect(validator.isValidFloat('-123')).to.equal(true);
    });
    it('should return true if value is "0"', function () {
      expect(validator.isValidFloat('0')).to.equal(true);
    });
    it('should return true if value is 10', function () {
      expect(validator.isValidFloat(10)).to.equal(true);
    });
    it('should return true if value is -123', function () {
      expect(validator.isValidFloat(10)).to.equal(true);
    });
    it('should return true if value is 0', function () {
      expect(validator.isValidFloat(10)).to.equal(true);
    });
    it('should return true if value is "0.0"', function () {
      expect(validator.isValidFloat("0.0")).to.equal(true);
    });
    it('should return true if value is "10.123"', function () {
      expect(validator.isValidFloat("10.123")).to.equal(true);
    });
    it('should return true if value is "-10.123"', function () {
      expect(validator.isValidFloat("-10.123")).to.equal(true);
    });
    it('should return true if value is 10.123', function () {
      expect(validator.isValidFloat(10.123)).to.equal(true);
    });
    it('should return true if value is -10.123', function () {
      expect(validator.isValidFloat(-10.123)).to.equal(true);
    })

  });
  describe('#isIpAddress(value)', function () {
    it('"127.0.0.1" - valid IP', function () {
      expect(validator.isIpAddress('127.0.0.1')).to.equal(true);
    })
    it('"227.3.67.196" - valid IP', function () {
      expect(validator.isIpAddress('227.3.67.196')).to.equal(true);
    })
    it('"327.0.0.0" - invalid IP', function () {
      expect(validator.isIpAddress('327.0.0.0')).to.equal(false);
    })
    it('"127.0.0." - invalid IP', function () {
      expect(validator.isIpAddress('127.0.0.')).to.equal(false);
    })
    it('"127.0." - invalid IP', function () {
      expect(validator.isIpAddress('127.0.')).to.equal(false);
    })
    it('"127" - invalid IP', function () {
      expect(validator.isIpAddress('127')).to.equal(false);
    })
    it('"127.333.0.1" - invalid IP', function () {
      expect(validator.isIpAddress('127.333.0.1')).to.equal(false);
    })
    it('"127.0.333.1" - invalid IP', function () {
      expect(validator.isIpAddress('127.0.333.1')).to.equal(false);
    })
    it('"127.0.1.333" - invalid IP', function () {
      expect(validator.isIpAddress('127.0.1.333')).to.equal(false);
    })
    it('"127.0.0.0:45555" - valid IP', function () {
      expect(validator.isIpAddress('127.0.0.0:45555')).to.equal(true);
    })
    it('"327.0.0.0:45555" - invalid IP', function () {
      expect(validator.isIpAddress('327.0.0.0:45555')).to.equal(false);
    })
  });

  describe('#isDomainName(value)', function () {
    it('"google.com" - valid Domain Name', function () {
      expect(validator.isDomainName('google.com')).to.equal(true);
    });
    it('"google" - invalid Domain Name', function () {
      expect(validator.isDomainName('google')).to.equal(false);
    });
    it('"123.123" - invalid Domain Name', function () {
      expect(validator.isDomainName('123.123')).to.equal(false);
    });
    it('"4goog.le" - valid Domain Name', function () {
      expect(validator.isDomainName('4goog.le')).to.equal(true);
    });
    it('"55454" - invalid Domain Name', function () {
      expect(validator.isDomainName('55454')).to.equal(false);
    })
  });

  describe('#hasSpaces()', function(){
    var testable = [
      { str: ' hello', detect: true },
      { str: 'hello world', detect: true },
      { str: 'hello ', detect: true },
      { str: 'hello', detect: false }
    ];
    testable.forEach(function(value){
      it('should ' + (value.detect ? '' : 'not') + ' detects spaces in `' + value.str + '`', function(){
        expect(validator.hasSpaces(value.str)).to.eql(value.detect);
      });
    });
  });
  describe('#isNotTrimmed', function(){
    var testable = [
      { str: ' hello world', detect: true },
      { str: ' hello world ', detect: true },
      { str: 'hello world ', detect: true },
      { str: 'hello world', detect: false },
      { str: 'hello world !', detect: false }
    ];
    testable.forEach(function(value){
      it('should ' + (value.detect ? '' : 'not') + 'trimmed string', function() {
        expect(validator.isNotTrimmed(value.str)).to.eql(value.detect);
      });
    });
  });
  describe('#empty()', function(){
    var testable = [
      { obj: "", detect: true },
      { obj: 0, detect: true },
      { obj: "0", detect: true },
      { obj: null, detect: true },
      { obj: undefined, detect: true },
      { obj: 'hello', detect: false },
      { obj: {}, detect: false },
      { obj: [], detect: false },
      { obj: ['a'], detect: false },
      { obj: 1, detect: false },
      { obj: true, detect: false }
    ];
    testable.forEach(function(value) {
      var detect = value.detect ? '' : 'not';
      it('should {0} detect empty value in `{1}`'.format(detect, JSON.stringify(value.obj)), function() {
        expect(validator.empty(value.obj)).to.eql(value.detect);
      });
    });
  });
  describe('#isValidUserName(value)', function() {
    var tests = [
      {m:'"" - invalid',i:'',e:false},
      {m:'"abc123" - valid',i:'abc123',e:true},
      {m:'"1abc123" - invalid',i:'1abc123',e:false},
      {m:'"abc123$" - invalid',i:'abc123$',e:false},
      {m:'"~1abc123" - invalid',i: '~1abc123',e:false},
      {m:'"abc12345679abc1234567890abc1234567890$" - invalid',i:'abc12345679abc1234567890abc1234567890$',e:false},
      {m:'"1abc123$$" - invalid',i:'1abc123$$',e:false},
      {m:'"a" - valid',i:'a',e:true},
      {m:'"!" - invalid',i:'!',e:false},
      {m:'"root$" - invalid',i:'root$',e:false},
      {m:'"rootU" - invalid',i:'rootU',e:false},
      {m:'"rUoot" - invalid',i:'rUoot',e:false}
    ];
    tests.forEach(function(test) {
      it(test.m + ' ', function () {
        expect(validator.isValidUserName(test.i)).to.equal(test.e);
      })
    });
  });
  describe('#isValidUNIXUser(value)', function() {
    var tests = [
      {m:'"" - invalid',i:'',e:false},
      {m:'"abc123" - valid',i:'abc123',e:true},
      {m:'"1abc123" - invalid',i:'1abc123',e:false},
      {m:'"abc123$" - invalid',i:'abc123$',e:false},
      {m:'"~1abc123" - invalid',i: '~1abc123',e:false},
      {m:'"abc12345679abc1234567890abc1234567890$" - invalid',i:'abc12345679abc1234567890abc1234567890$',e:false},
      {m:'"1abc123$$" - invalid',i:'1abc123$$',e:false},
      {m:'"a" - valid',i:'a',e:true},
      {m:'"!" - invalid',i:'!',e:false},
      {m:'"abc_" - valid',i:'abc_',e:true},
      {m:'"_abc" - valid',i:'_abc',e:true},
      {m:'"abc_abc" - valid',i:'_abc',e:true}
    ];
    tests.forEach(function(test) {
      it(test.m + ' ', function () {
        expect(validator.isValidUNIXUser(test.i)).to.equal(test.e);
      })
    });
  });
  describe('#isValidDir(value)', function() {
    var tests = [
      {m:'"dir" - invalid',i:'dir',e:false},
      {m:'" /dir" - invalid',i:' /dir',e:false},
      {m:'"/dir" - valid',i:'/dir',e:true},
      {m:'"/dir1,dir2" - invalid',i:'/dir1,dir2',e:false},
      {m:'"/dir1, /dir2" - invalid',i:'/dir1,dir2',e:false},
      {m:'"/dir1,/dir2" - valid',i:'/dir1,/dir2',e:true},
      {m:'"/123" - valid',i:'/111',e:true},
      {m:'"/abc" - valid',i:'/abc',e:true},
      {m:'"/1a2b3c" - valid',i:'/1a2b3c',e:true}
    ];
    tests.forEach(function(test) {
      it(test.m + ' ', function () {
        expect(validator.isValidDir(test.i)).to.equal(test.e);
      })
    });
  });

  describe('#isConfigValueLink', function() {
    var tests = [
      {m:'link valid',i:'${asd}',e:true},
      {m:'empty link ${} -invalid',i:'${}',e:false},
      {m:'${ just wrong',i:'${',e:false},
      {m:'anything  just wrong',i:'anything',e:false}
    ];
    tests.forEach(function(test) {
      it(test.m + ' ', function () {
        expect(validator.isConfigValueLink(test.i)).to.equal(test.e);
      })
    });
  });

  describe('#isValidDataNodeDir(value)', function() {
    var tests = [
      {m:'"dir" - invalid',i:'dir',e:false},
      {m:'"/dir" - valid',i:'/dir',e:true},
      {m:'"/dir1,dir2" - invalid',i:'/dir1,dir2',e:false},
      {m:'"/dir1,/dir2" - valid',i:'/dir1,/dir2',e:true},
      {m:'" /dir1,/dir2" - valid',i:' /dir1,/dir2',e:false},
      {m:'"/dir1, /dir2" - valid',i:' /dir1,/dir2',e:false},
      {m:'"/123" - valid',i:'/111',e:true},
      {m:'"/abc" - valid',i:'/abc',e:true},
      {m:'"/1a2b3c" - valid',i:'/1a2b3c',e:true},
      {m:'"[ssd]/1a2b3c" - valid',i:'[ssd]/1a2b3c',e:true},
      {m:'"[DISK]/1a2b3c" - valid',i:'[DISK]/1a2b3c',e:true},
      {m:'"[DISK]file:///1a2b3c" - valid',i:'[DISK]file:///1a2b3c',e:true},
      {m:'"[] /1a2b3c" - invalid',i:'[] /1a2b3c',e:false},
      {m:'"[ssd] /1a2b3c" - invalid',i:'[ssd] /1a2b3c',e:false},
      {m:'"[/1a2b3c]" - invalid',i:'[/1a2b3c]',e:false},
      {m:'"[s]ss /sd" - invalid',i:'[s]ss /sd',e:false},
      {m:'" [s]ss/sd" - invalid',i:' [s]ss/sd',e:false},
      {m:'"[RAM_DISK]/1a2b3c" - valid',i:'[RAM_DISK]/1a2b3c',e:true},
      {m:'"[RAMDISK_]/1a2b3c" - invalid',i:'[RAMDISK_]/1a2b3c',e:false},
      {m:'"[_RAMDISK]/1a2b3c" - invalid',i:'[_RAMDISK]/1a2b3c',e:false}
    ];
    tests.forEach(function(test) {
      it(test.m + ' ', function () {
        expect(validator.isValidDataNodeDir(test.i)).to.equal(test.e);
      })
    });
  });
  describe('#isAllowedDir(value)', function() {
    var tests = [
      {m:'"/home" - not allowed',i:'/home',e:false},
      {m:'"/homes" - not allowed',i:'/homes',e:false},
      {m:'"/home/" - not allowed',i:'/home/',e:false},
      {m:'"/homes/" - not allowed',i:'/homes/',e:false},
      {m:'"/dir" - allowed',i:'/dir',e:true},
      {m:'"/dir/home" - allowed',i:'/dir/home',e:true},
      {m:'"/dir/homes" - allowed',i:'/dir/homes',e:true},
      {m:'"/dir/home/" - allowed',i:'/dir/home/',e:true},
      {m:'"/dir/homes/" - allowed',i:'/dir/homes/',e:true}
    ];
    tests.forEach(function(test) {
      it(test.m + ' ', function () {
        expect(validator.isAllowedDir(test.i)).to.equal(test.e);
      })
    });
  });
  describe('#isValidConfigKey(value)', function() {
    var tests = [
      {m:'"123" - valid',i:'123',e:true},
      {m:'"abc" - valid',i:'abc',e:true},
      {m:'"abc123" - valid',i:'abc123',e:true},
      {m:'".abc." - valid',i:'.abc.',e:true},
      {m:'"_abc_" - valid',i:'_abc_',e:true},
      {m:'"-abc-" - valid',i:'-abc-',e:true},
      {m:'"abc 123" - invalid',i:'abc 123',e:false},
      {m:'"a"b" - invalid',i:'a"b',e:false},
      {m:'"a\'b" - invalid',i:'a\'b',e:false},
      {m:'" a " - valid', i: ' a ', e: true},
      {m:'" a" - valid', i: ' a', e: true},
      {m:'"a " - valid', i: 'a ', e: true}
    ];
    tests.forEach(function(test) {
      it(test.m + ' ', function () {
        expect(validator.isValidConfigKey(test.i)).to.equal(test.e);
      })
    });
  });
  describe('#isValidConfigGroupName(value)', function() {
    var tests = [
      {m:'"123" - valid',i:'123',e:true},
      {m:'"abc" - valid',i:'abc',e:true},
      {m:'"abc123" - valid',i:'abc123',e:true},
      {m:'".abc." - invalid',i:'.abc.',e:false},
      {m:'"_abc_" - valid',i:'_abc_',e:true},
      {m:'"-abc-" - valid',i:'-abc-',e:true},
      {m:'" abc  123 " - valid',i:' abc  123 ',e:true},
      {m:'"a"b" - invalid',i:'a"b',e:false},
      {m:'"a\'b" - invalid',i:'a\'b',e:false}
    ];
    tests.forEach(function(test) {
      it(test.m + ' ', function () {
        expect(validator.isValidConfigGroupName(test.i)).to.equal(test.e);
      })
    });
  });

  describe('#isValidMatchesRegexp()', function() {
    var message = '`{0}` should be {1}',
      tests = [
        { value: '.*', expected: true },
        { value: '..', expected: true },
        { value: '.a1', expected: true },
        { value: '.*a1', expected: true },
        { value: '.*a1.*', expected: true },
        { value: '.*a1.a2', expected: true },
        { value: '.*a1.*.a2', expected: true },
        { value: '.*a1.*.a2.*.a3.a4.*.*', expected: true },
        { value: '*', expected: false },
        { value: '1>1', expected: false },
        //{ value: '.*a1,*', expected: false },
        { value: '?a1[1]asd[1]', expected: false },
        { value: 'a1[1]asd[1]', expected: true },
        { value: 'a1[1]asd[1][', expected: false },
        { value: 'a1[1|1]asd[1]', expected: true },
        { value: '/a1[1|1]asd[1]', expected: true },
        { value: 'a1-2!', expected: true },
        { value: '|a1-2', expected: false },
        { value: '[a1', expected: false },
        { value: 'a{1}', expected: true },
        { value: 'a{1,2}', expected: true },
        { value: 'a{1,2}{', expected: false },
        { value: 'a(1)', expected: true }
      ];
    tests.forEach(function(test) {
      it(message.format(test.value, test.expected ? 'valid' : 'not valid'), function() {
        expect(validator.isValidMatchesRegexp(test.value)).to.equal(test.expected);
      })
    });
  });

  describe('#isValidURL', function() {
    var tests = [
      {m:'"http://apache.org" - valid',i:'http://apache.org',e:true},
      {m:'"http://ambari.apache.org" - valid',i:'http://ambari.apache.org',e:true},
      {m:'"https://ambari.apache.org" - valid',i:'https://ambari.apache.org',e:true},
      {m:'"htp://ambari.apache.org." - invalid',i:'.htp://ambari.apache.org.',e:false},
      {m:'"ambari.apache.org" - invalid',i:'ambari.apache.org',e:false},
      {m:'"www.ambari.apache.org" - invalid',i:'www.ambari.apache.org',e:false},
      {m:'"" - invalid',i:'',e:false}
    ];
    tests.forEach(function(test) {
      it(test.m + ' ', function () {
        expect(validator.isValidURL(test.i)).to.equal(test.e);
      })
    });
  });

  describe('#isHostname()', function() {
    var tests = [
      {m:'"localhost" - valid',i:'localhost',e:true},
      {m:'"c6401.apache.ambari.org" - valid',i:'c6401.apache.ambari.org',e:true},
      {m:'"c6401.org" - valid',i:'c6401.org',e:true},
      {m:'"c6401" - invalid',i:'c6401',e:false},
      {m:'"c6401." - invalid',i:'c6401.',e:false}
    ];
    tests.forEach(function(test) {
      it(test.m + ' ', function () {
        expect(validator.isHostname(test.i)).to.equal(test.e);
      })
    });
  });

  describe('#isValidBaseUrl()', function() {
    var tests = [
      {m: '"" - valid', i: '', e: true},
      {m: '"http://" - valid', i: 'http://', e: true},
      {m: '"https://" - valid', i: 'https://', e: true},
      {m: '"ftp://" - valid', i: 'ftp://', e: true},
      {m: '"file:///" - valid', i: 'file:///', e: true},
      {m: '"file3" - invalid', i: 'file3', e: false},
      {m: '"3" - invalid', i: '3', e: false},
      {m: '"a" - invalid', i: 'a', e: false}
    ];
    tests.forEach(function(test) {
      it(test.m + ' ', function () {
        expect(validator.isValidBaseUrl(test.i)).to.equal(test.e);
      })
    });
  });

  describe('#isValidLdapsURL()', function() {
    var tests = [
      {m: '"" - invalid', i: '', e: false},
      {m: '"http://example.com" - invalid', i: 'http://example.com', e: false},
      {m: '"ldap://example.com" - invalid', i: 'ldap://example.com', e: false},
      {m: '"ldaps://example.com" - valid', i: 'ldaps://example.com', e: true},
      {m: '"ldaps://example.com:636" - valid', i: 'ldaps://example.com:636', e: true},
      {m: '"ldaps://example.com:636/path" - valid', i: 'ldaps://example.com:636/path', e: true},
      {m: '"ldaps://example.com:6eeee36/path" - valid', i: 'ldaps://example.com:6eee36/path', e: false}
    ];
    tests.forEach(function(test) {
      it(test.m + ' ', function () {
        expect(validator.isValidLdapsURL(test.i)).to.equal(test.e);
      })
    });
  });

  describe('#isValidRackId()', function () {

    [
      {v: '', e: false},
      {v: 'a', e: false},
      {v: '1', e: false},
      {v: '/', e: false},
      {v: '/a', e: true},
      {v: '/1', e: true},
      {v: '/-', e: true},
      {v: '/' + (new Array(255)).join('a'), m: 'Value bigger than 255 symbols', e: false}
    ].forEach(function (test) {

      it(test.m || test.v, function () {
        expect(validator.isValidRackId(test.v)).to.be.equal(test.e);
      })

    });

  });

  describe('#isValidAlertName', function () {

    [
      {v: '', e: false},
      {v: 'a', e: true},
      {v: 'a b', e: true},
      {v: '/', e: false},
      {v: '/>1', e: false},
      {v: 'a 1%', e: true},
      {v: 'a (b)', e: true}
    ].forEach(function (test) {

      it(test.m || test.v, function () {
        expect(validator.isValidAlertName(test.v)).to.be.equal(test.e);
      })

    });


  });

  describe('#isValidAlertName', function () {

    [
      {v: '', e: false},
      {v: 'test', e: true},
      {v: 'Test', e: true},
      {v: 'TEST', e: true},
      {v: 'te-st', e: true},
      {v: '-test', e: false},
      {v: 'test-', e: false},
      {v: '1', e: true},
      {v: 'test1', e: true},
      {v: '1-test', e: true}
    ].forEach(function (test) {

      it(test.m || test.v, function () {
        expect(validator.isValidNameServiceId(test.v)).to.be.equal(test.e);
      })

    });


  });

});
