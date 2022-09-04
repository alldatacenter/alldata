#!/usr/bin/env python
# encoding: utf-8

import hashlib
# import hmac
#
#
# class HMACUtil(object):
#
#     def create_hmac_sha1_digest(self, text, key):
#         return self._create_hmac_digest(text, key, hashlib.sha1)
#
#     def create_hmac_md5_digest(self, text, key):
#         return self._create_hmac_digest(text, key, hashlib.md5)
#
#     def create_hmac_sha256_digest(self, text, key):
#         return self._create_hmac_digest(text, key, hashlib.sha256)
#
#     def _create_hmac_digest(self, text, key, digestmod):
#         hmac_ins = hmac.new(key, text, digestmod)
#         return hmac_ins.digest()


class DigestUtil(object):

    @staticmethod
    def create_sha1_digest(text):
        return DigestUtil.__create_digest(text, hashlib.sha1)

    @staticmethod
    def create_md5_digest(text):
        return DigestUtil.__create_digest(text, hashlib.md5)

    @staticmethod
    def create_sha256_digest(text):
        return DigestUtil.__create_digest(text, hashlib.sha256)

    @staticmethod
    def __create_digest(text, digestmod, encode_type='utf-8'):
        digest_ins = digestmod()
        digest_ins.update(text.encode(encode_type))
        return digest_ins.hexdigest()
