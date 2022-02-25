Python PKCS#5 v2.0 PBKDF2 Module
--------------------------------

This module implements the password-based key derivation function, PBKDF2,
specified in `RSA PKCS#5 v2.0 <http://www.rsa.com/rsalabs/node.asp?id=2127>`_.

Example PBKDF2 usage
====================

::

 from pbkdf2 import PBKDF2
 from Crypto.Cipher import AES
 import os

 salt = os.urandom(8)    # 64-bit salt
 key = PBKDF2("This passphrase is a secret.", salt).read(32) # 256-bit key
 iv = os.urandom(16)     # 128-bit IV
 cipher = AES.new(key, AES.MODE_CBC, iv)
 # ...

Example crypt() usage
=====================

::

 from pbkdf2 import crypt
 pwhash = crypt("secret")
 alleged_pw = raw_input("Enter password: ")
 if pwhash == crypt(alleged_pw, pwhash):
     print "Password good"
 else:
     print "Invalid password"

Example crypt() output
======================

::

 >>> from pbkdf2 import crypt
 >>> crypt("secret")
 '$p5k2$$hi46RA73$aGBpfPOgOrgZLaHGweSQzJ5FLz4BsQVs'
 >>> crypt("secret", "XXXXXXXX")
 '$p5k2$$XXXXXXXX$L9mVVdq7upotdvtGvXTDTez3FIu3z0uG'
 >>> crypt("secret", "XXXXXXXX", 400)  # 400 iterations (the default for crypt)
 '$p5k2$$XXXXXXXX$L9mVVdq7upotdvtGvXTDTez3FIu3z0uG'
 >>> crypt("spam", iterations=400)
 '$p5k2$$FRsH3HJB$SgRWDNmB2LukCy0OTal6LYLHZVgtOi7s'
 >>> crypt("spam", iterations=1000)    # 1000 iterations
 '$p5k2$3e8$H0NX9mT/$wk/sE8vv6OMKuMaqazCJYDSUhWY9YB2J'


Resources
=========

Homepage
    https://www.dlitz.net/software/python-pbkdf2/

Source Code
    https://github.com/dlitz/python-pbkdf2/

PyPI package name
    `pbkdf2 <http://pypi.python.org/pypi/pbkdf2>`_

License
=======
Copyright (C) 2007-2011 Dwayne C. Litzenberger <dlitz@dlitz.net>

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
