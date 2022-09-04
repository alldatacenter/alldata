# coding: utf-8
"""
Common exceptions
"""
import teslafaas.common.return_code as CODE



class UnicodeException(Exception):
    """
    Exception doesn't support unicode argument.
    """

    def __init__(self, mesg='', code=None, data=None):
        self.mesg = mesg
        self.code = code
        self.data = data

    def get_ret_code(self):
        return self.code

    def get_ret_message(self):
        return self.mesg

    def get_ret_data(self):
        return self.data

    def __str__(self):
        return unicode(self.mesg).encode('utf-8')

    def __repr__(self):
        return 'UnicodeException(%s)' % repr(self.mesg)

    def __unicode__(self):
        return unicode(self.mesg)


class TeslaBaseException(UnicodeException):
    """
    Exception doesn't support unicode argument.
    """
    def __init__(self, mesg='', code=CODE.INTERNAL_SERVER_ERROR, data=None):
        UnicodeException.__init__(self, mesg, code, data)


class FeArgError(TeslaBaseException):
    """
    Raise this if FE/user passed params are illegal.
    """
    def __init__(self, mesg, code=CODE.BAD_REQ):
        TeslaBaseException.__init__(self, mesg, code)


class ArgError(TeslaBaseException):
    """
    Raise this if arguments passed is illegal.

    Used by Model method for internal invocation arg error.
    """
    def __init__(self, mesg, code=CODE.BAD_REQ):
        TeslaBaseException.__init__(self, mesg, code)


class BeConfError(TeslaBaseException):
    """
    Raise this if backend config invalid, i.e. work flow config error.
    """
    def __init__(self, mesg, code=CODE.BAD_REQ):
        TeslaBaseException.__init__(self, mesg, code)


class NoPermission(TeslaBaseException):
    """
    Raise this if user permission check failed.
    """
    def __init__(self, mesg, code=CODE.FORBIDDEN):
        TeslaBaseException.__init__(self, mesg, code)


class ModelQueryArgError(ArgError):
    """
    Check query arguments before doing DB SQL query,
    raise this error if argument checking failed.
    """
    def __init__(self, mesg, code=CODE.CONFLICT):
        TeslaBaseException.__init__(self, mesg, code)


class ModelQueryEmptyError(TeslaBaseException):
    """
    Raise this in situations where there should not be empty result
    for a SQL query.
    """
    def __init__(self, mesg, code=CODE.PRECONDITION_FAILED):
        TeslaBaseException.__init__(self, mesg, code)


class ModelOperationError(TeslaBaseException):
    """
    Raise this in situations where Model operation can not fulfilled.
    """
    def __init__(self, mesg, code=CODE.PRECONDITION_FAILED):
        TeslaBaseException.__init__(self, mesg, code)


class ActionError(TeslaBaseException):
    """
    Raise this if have some unexpected data.
    """
    def __init__(self, mesg, code=CODE.BAD_REQ, data=None):
        TeslaBaseException.__init__(self, mesg, code, data)
