#!/usr/bin/env python
# encoding: utf-8
""" """

__author__ = 'adonis'


class orm(object):
    _select = None
    _where = None
    _insert = None
    _inserts = None
    _update = None
    _table = None
    _joins = None
    _prefix = ""
    _db = None

    def __init__(self, table, prefix="", db=None):
        self._table = prefix + table
        if prefix != "": self._prefix = prefix
        self._db = db

    def select(self, *args):
        self._select = args
        return self

    def where(self, *args, **kwargs):
        if len(args) == 1:
            data = args[0]
            if type(data).__name__ == "dict":
                self._where = [data]
            elif len(data[0]) > 0:
                self._where = data
        elif len(kwargs) > 0:
            self._where = [kwargs]
        else:
            raise Exception("unknown where args")
        return self

    def insert(self, **kwargs):
        self._insert = kwargs
        return self

    def inserts(self, data):
        self._inserts = data
        return self

    def update(self, **kwargs):
        self._update = kwargs
        return self

    def join(self, mode, table, data):
        if self._joins == None: self._joins = []
        self._joins.append(
            {"mode": mode, "table": self._prefix + table, "data": [data]})
        return self

    def _cond_generate(self, data, with_quote=False, with_in=True):
        varList = []
        sql = ""
        if with_quote:
            # place = "'%s'"
            place = "%s"
        else:
            place = "%s"
        orList = []
        for ands in data:
            andList = []
            for k, v in ands.items():
                if len(k.split("[")) > 1:
                    (k, op) = k.strip("]").split("[")
                else:
                    op = None
                if type(v).__name__ == "list":
                    if with_in == True:
                        if op == None: op = "in"
                        replaceList = []
                        for i in v:
                            replaceList.append(place)
                            if with_quote:
                                varList.append(repr(i))
                            else:
                                varList.append(i)
                        andList.append(
                            k + " " + op + " (" + ','.join(replaceList) + ")")
                    else:
                        replaceList = []
                        for i in v:
                            if with_quote:
                                varList.append(repr(i))
                            else:
                                varList.append(i)
                            replaceList.append(k + " = " + place)
                        andList.append("(" + " OR ".join(replaceList) + ")")
                else:
                    if op == None:
                        if v == None:
                            op = "is"
                        else:
                            op = "="
                    if type(v).__name__ in ('str') and v.startswith(
                            "*") and v.endswith("*"):
                        andList.append(k + " " + op + " " + v[1:-1])
                    else:
                        andList.append(k + " " + op + " " + place)
                        if with_quote:
                            varList.append(repr(v))
                        else:
                            varList.append(v)

            orList.append(' AND '.join(andList))
        sql += ' OR '.join(orList)
        return (sql, varList)

    def generate(self, escape=False, with_in=True):
        varList = []
        sql = ""
        if self._insert != None:
            keys = []
            replaceList = []
            for k, v in self._insert.items():
                keys.append(k)
                replaceList.append("%s")
                varList.append(v)
            sql += "INSERT INTO " + self._table + " (" + ",".join(
                keys) + ") VALUES (" + ",".join(replaceList) + ")"

        if self._inserts != None:
            keys = []
            insertList = []
            sql += "INSERT INTO " + self._table
            for k in self._inserts[0]:
                keys.append(k)
            sql += " (" + ",".join(keys) + ") VALUES "
            for line in self._inserts:
                insertList.append(" (" + ','.join(["%s" for k in keys]) + ")")
                varList += [line[k] for k in keys]
            sql += ",".join(insertList)

        if self._select != None:
            sql += "SELECT %s FROM " % (','.join(self._select))
            if len(self._table.split("#")) == 2:
                (tn, ta) = self._table.split("#")
                sql += "%s as %s" % (tn, ta)
            else:
                sql += self._table

        if self._joins != None and len(self._joins) > 0:
            for j in self._joins:
                if len(j["table"].split("#")) == 2:
                    (tn, ta) = j["table"].split("#")
                    sql += " " + j[
                        "mode"] + " join " + tn + " as " + ta + " on "
                else:
                    sql += " " + j["mode"] + " join " + j["table"] + " on "
                (csql, cvar) = self._cond_generate(j["data"])
                sql += csql
                varList += cvar

        if self._update != None:
            sql += "UPDATE " + self._table + " SET "
            setList = []
            for k, v in self._update.items():
                setList.append(k + " = %s")
                varList.append(v)
            sql += " , ".join(setList)

        if self._where != None:
            sql += " WHERE "
            (csql, cvar) = self._cond_generate(self._where, escape, with_in)
            sql += csql
            varList += cvar

        if escape == True:
            return sql % tuple(varList)
        else:
            return (sql, varList)

    def execute(self):
        return self._db.query(self.generate(True))


class DomainModel(object):
    """
    Usage:
    >>> dm = self.dm(self.dbs.db1)  # use mysql
    >>> dm.obj("cluster").select('column1','column2').where(condtion1=123,condtion2=['4','5','6'])
    """
    _db = None

    def __init__(self, db):
        self._db = db

    def obj(self, table):
        return orm(table, db=self._db)