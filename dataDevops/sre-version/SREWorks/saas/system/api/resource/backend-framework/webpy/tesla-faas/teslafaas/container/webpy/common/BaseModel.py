# coding: utf-8
"""
Model Base
"""
# pylint: disable=unsupported-assignment-operation,unsubscriptable-object,no-member

import copy
import logging
import math
import os
import re
import types

import web
from web.db import SQLLiteral, SQLQuery

from teslafaas.common.exceptions import ModelQueryArgError, FeArgError, ArgError
from teslafaas.common.jsonify import jsondumps as jds
from teslafaas.common.jsonify import jsonloads as jls
from teslafaas.common.util_db_func import get_sql_where
from teslafaas.common.util_func import toint


class BaseModel(object):
    def __init__(self):
        # Add a copy of context value in case of new coroutine
        self.tesla_context = web.ctx.tesla
        # 兼容老的代码
        self.tesla_ctx = web.ctx.tesla
        self.config = self.tesla_ctx.config
        self.logger = logging.getLogger(__name__)
        self.factory = web.ctx.factory
        self.db = self.tesla_ctx["db"]
        self.dbs = self.tesla_ctx["dbs"]
        self.dm = self.tesla_ctx.dm
        self.dbopts = web.ctx.tesla["db_opts"]
        self.logger = logging.getLogger(__name__)
        u = web.ctx.env.get("HTTP_X_AUTH_USER")
        self.user_name = u if u else "unknown"
        empid = web.ctx.env.get("HTTP_X_EMPID")
        self.user_empid = str(empid) if empid else '0'

    def environ(self):
        env = os.environ.get("TESLA")
        if env:
            return self.jsonloads(env)
        return {}

    def jsondumps(self, obj):
        return jds(obj)

    def jsonloads(self, s):
        return jls(s)


class CrudModel(BaseModel):
    """
    Base model for simple CRUD operation model.   -----   Adonis
    """
    PAGE_INDEX = 1
    PAGE_SIZE = 20
    COMMON_UNMUTABLE_FIELDS = [
        'id', 'creator', 'createtime', 'modifier', 'modifytime']

    class FieldType(object):
        all = 0  # All fields of a table.
        editable = 1  # editable fields of a table.
        mandatory = 2  # mandatory fields of a table, deprecated! use required
        required = 2  # required/mandatory fields of a table.
        editable_request = 3  # editable fields for requester. deprecated
        editable_approver = 4  # editable fields for approver. deprecated

    def __init__(self):
        super(CrudModel, self).__init__()
        self._tb_fields = {}

    def register_tb_fields(self, tn, fields, edit_feilds, mandatory_fields,
                           editable_request=None, editable_approver=None):
        self._tb_fields[tn] = {
            self.FieldType.all: fields,
            self.FieldType.editable: edit_feilds,
            self.FieldType.mandatory: mandatory_fields,
            self.FieldType.editable_request: editable_request,
            self.FieldType.editable_approver: editable_approver,
        }

    def register_tb_structures(self, tb_struct={}):
        self._tb_fields.update(tb_struct)

    def get_default_table(self):
        model_tbs = self._tb_fields.keys()
        return model_tbs[0] if model_tbs else None

    def validate_pk(self, pk_name, params, table=None, table_pk='id'):
        """
        Deprecated! use is_valid_pk() instead.
        Validate whether user passed correct table pk field.
        """
        if pk_name not in params:
            return False, "Param '%s' is mandatory" % pk_name
        pk = params.get(pk_name, 0)
        pk = toint(pk)
        if not pk:
            return False, "Param '%s' is invalid, " \
                          "not a valid positive integer" % pk_name
        if table:
            args = {table_pk: pk}
            record = self.get_records(table, **args)
            if not record:
                return False, "Param '%s' invalid, corresponding " \
                              "record doesn't exist" % pk_name
        return True, record[0]

    def validate_pk_with_exception(self, pk, table, table_pk='id'):
        """
        Validate whether user passed correct table pk field.
        """
        pk = toint(pk)
        if not pk:
            raise FeArgError("Invalid primary key '%s'" % pk)
        if table:
            args = {table_pk: pk, }
            record = self.get_records(table, **args)
            if not record:
                raise FeArgError("No record found with id(%s) in table(%s)"
                                 % (pk, table))
            return record[0]
        else:
            raise FeArgError("Empty table name")
        return None

    def is_valid_pk(self, pk, table, table_pk='id'):
        """
        Validate whether user passed correct table pk field.
        """
        pk = toint(pk)
        if not pk:
            return None
        if table:
            args = {table_pk: pk, 'validflag': 1}
            record = self.get_records(table, **args)
            return record[0] if record else None
        return None

    def get_tb_fields(self, tb_name, field_type=FieldType.all):
        """
        Subclass should override this method to meet they needs.
        Get a list of table fields. Used for the case where one Model class
        manages many tables.
        """
        if tb_name not in self._tb_fields:
            raise ModelQueryArgError('Table name(%s) unknown!' % tb_name)
        return self._tb_fields[tb_name][field_type]

    @classmethod
    def add_user_aliww_info(cls, user_model, records, uid_field, new_field):
        """
        Add user aliww(or nickname if aliww is empty) info to records.

        :param user_model: NormalUserModel() instance.
        :param records: records to be add user aliww info to .
        :param uid_field: user id field name.
        :param new_field: new field to be added to record for aliww info.
        :return: None
        """
        user_ids = map(lambda x: x.get(uid_field), records)
        user_ids = set(user_ids)  # delete duplication
        user_info = user_model.get_user_by_ids(user_ids,
                                               fields=['aliww', 'nickname'])
        for record in records:
            if record[uid_field] in user_info:
                aliww = user_info[record[uid_field]].aliww
                nickname = user_info[record[uid_field]].nickname
                record[new_field] = aliww if aliww else nickname
            else:
                record[new_field] = u'无该用户信息'

    def raw_sql_query(self, raw_sql):
        """
        Use this to replace web.db.query() for query without query args

        SQL string with '$' passed to web.db.query() will be parsed as
        param placeholder, causing error

        :param raw_sql: sql string without any param placeholder
        :return: same as web.db.query()
        """
        return self.db.query(SQLQuery(raw_sql), processed=True)

    def _get_pagination_cnt_info(self, sql_template,
                                 distinct_key='',
                                 sql_vars={},
                                 page_size=PAGE_SIZE,
                                 **kwargs):
        """
        Do not use this directly, use get_pagination_info() or
        get_pagination_records().

        :return: total number of records and pages.
        """
        err_title = "Pagination error: "
        sql_distinct_pat = r'\W*SELECT\W+DISTINCT\W+(.*)\W+FROM'
        sql_distinct_groups = re.match(sql_distinct_pat, sql_template,
                                       flags=re.IGNORECASE | re.DOTALL)
        if sql_distinct_groups:
            if not distinct_key:
                raise ModelQueryArgError(
                    err_title + 'no distinct key specified for SELECT DISTINCT')
            cnt_sql = re.sub(
                'SELECT\W*DISTINCT\W*.*\W+FROM',
                'SELECT COUNT(DISTINCT %s) AS cnt FROM' % distinct_key,
                sql_template,
                flags=re.IGNORECASE | re.DOTALL)
        else:
            sql_select_pat = r'\W*SELECT\W+(.*)\W+FROM'
            sql_select_groups = re.match(sql_select_pat, sql_template,
                                         flags=re.IGNORECASE | re.DOTALL)
            if not sql_select_groups:
                raise ModelQueryArgError(
                    "%s invalid SQL template, can not get SQL SELECT items "
                    "using RE pattern: %s" % (err_title, sql_select_pat))
            # FIXME: invalid mysql syntax: COUNT(a.*)
            sql_select_item = sql_select_groups.group(1).strip()
            cnt_sql = re.sub('SELECT\W*.*\W*FROM',
                             'SELECT COUNT(%s) AS cnt FROM' % '*',
                             # 'SELECT COUNT(%s) AS cnt FROM' % sql_select_item,
                             sql_template,
                             flags=re.IGNORECASE | re.DOTALL)
        recs_cnt = int(self.db.query(cnt_sql, vars=sql_vars)[0].cnt)
        pages_cnt = int(math.ceil(recs_cnt / float(page_size)))
        return recs_cnt, pages_cnt

    def get_pagination_info(self, sql_template,
                            distinct_key='',
                            sql_vars={},
                            page_index=PAGE_INDEX,
                            page_size=PAGE_SIZE,
                            **kwargs):
        """

        :param sql_template: sql template for web.db.query()
        :param sql_vars: vars to web.db.query()
        :return: records[list], total number of records, total number of pages
        """
        err_title = "Pagination failed: "
        offset = (page_index - 1) * page_size
        limit = page_size

        sql_query = "%(sql_template)s LIMIT %(limit)s OFFSET %(offset)s " \
                    % locals()
        records = list(self.db.query(sql_query, vars=sql_vars))

        recs_cnt, pages_cnt = self._get_pagination_cnt_info(
            sql_template,
            distinct_key=distinct_key,
            sql_vars=sql_vars,
            page_size=page_size,
            **kwargs)
        return records, recs_cnt, pages_cnt

    def get_pagination_records(self, tb_name,
                               pk='id',
                               get_fields=(),
                               extra_cond='',
                               order_fields=[],
                               desc=True,
                               page_size=PAGE_SIZE,
                               page_index=1,
                               use_index=[],
                               force_index=[],
                               **kwargs):
        """
        :param :  ref args of get_records()
        :return: records(list), total number of records, total number of pages
        """
        search_args = dict(pk=pk,
                           get_fields=get_fields,
                           extra_cond=extra_cond,
                           order_fields=order_fields,
                           desc=desc,
                           page_size=page_size,
                           page_index=page_index,
                           use_index=use_index,
                           force_index=force_index)
        search_args.update(dict(pagination_info=False, pagination_on=True))
        kwargs.update(search_args)
        recs = self.get_records(tb_name, **kwargs)
        kwargs.update(dict(pagination_info=True, pagination_on=True))
        total_rec_cnt, total_pages_cnt = self.get_records(tb_name, **kwargs)
        return recs, total_rec_cnt, total_pages_cnt

    def get_records(self, tb_name,
                    pk='id',
                    get_fields=(),
                    extra_cond='',
                    order_fields=[],
                    desc=True,
                    limit=None,
                    distinct_key='id',
                    pagination_info=False,
                    pagination_on=False,
                    page_size=PAGE_SIZE,
                    page_index=1,
                    use_index=[],
                    force_index=[],
                    allow_empty_cond=True,
                    **kwargs):
        """
        Query DB to get records whose field satisfy kwargs condition.

        Note: select records only with validflag=1 if no 'validflag'
            is in kwargs select condition

        :param tb_name: table name or list/tuple for multiple table join.
        :param pk: primary key field name.
        :param get_fields :type: list/tuple: fields to be get.
        :param extra_cond: Raw SQL query conditions that will be stubbed
                         to SQL query generator for kwargs.
                         ---- use it carefully, cope with SQL-injection
                         by yourself.
        :param order_fields: list, ORDER BY sql statement, default: pk
        :param desc: True/False, records sort order.
        :param limit: int, limit of number of records returned.
        :param pagination_info: True, get pagination info: total records/pages
        instead of records. -----  bad design, deprecated, use get_pagination_records()
        :param distinct_key: used by pagination, to calculate COUNT info for
                            SELECT DISTINCT query
        :param pagination_on: Turn on pagination of records. ----- deprecated
        :param page_index: start from 1
        :param allow_empty_cond: True: allow empty query condition(kwargs and
                                extra_cond are both empty)
                            False: Raise Exception if query condition is empty(
                                kwargs and extra_cond are both empty ).
        :param kwargs: two item tuple for SQL 'between/<=/>=' select
                        (1, 2)/(None, 2)/(1, None) for 'BETWEEN 1 AND 2/<=2/>=1'
                       list/non-2-item-tuple for SQL 'IN' select
                       others types will be SQL '=' select
        """
        # Default: select records only with validflag>=1
        if 'validflag' not in kwargs:
            kwargs['validflag'] = (1, None)
        real_page_size = page_size
        if pagination_on:  # Need pagination.
            offset = (page_index - 1) * page_size
            limit = page_size
        else:
            page_size = offset = None
        if pagination_info:  # 分页信息的优先级比分页数据获取要高
            page_size = offset = None
            limit = page_size

        fields = self.__all_fields(tb_name)
        args = {field: kwargs[field] for field in fields if field in kwargs}
        empty_cond = not args and not extra_cond
        if empty_cond and not allow_empty_cond:
            raise ModelQueryArgError(
                "DB query error: empty query condition with "
                "allow_emtpy_cond=False")
        # web.db.where() doesn't support "WHERE a IN (1, 2, 3)"
        # records = list(self.db.where(tb_name, **args))
        what = '*' if not get_fields else ','.join(get_fields)
        where_str = get_sql_where(**args)
        if extra_cond:
            where_str += " AND %s" % extra_cond
        order_str = ', '.join(order_fields) if order_fields else '%s' % pk
        order_str += ' DESC' if desc else ' ASC'
        sql_where = SQLQuery(where_str) if where_str else where_str
        select_args = dict(what=what, where=sql_where,
                           order=order_str, limit=limit, offset=offset)
        # self.logger.debug('get_records(), table: %s, args: \n%s'
        #                   % (tb_name, select_args))
        if use_index:
            tb_name = "%s USE INDEX (%s)" % (tb_name, ",".join(use_index))
        elif force_index:
            tb_name = "%s force index (%s)" % (tb_name, ",".join(force_index))
        if pagination_info:
            select_args['_test'] = True
            sql = str(self.db.select(tb_name, **select_args))
            return self._get_pagination_cnt_info(
                sql, distinct_key=distinct_key, page_size=real_page_size,
                **kwargs)
        records = list(self.db.select(tb_name, **select_args))
        return records

    def get_one_record(self, tb_name,
                       pk='id',
                       get_fields=(),
                       extra_cond='',
                       **kwargs):
        recs = self.get_records(
            tb_name, pk=pk, get_fields=get_fields, extra_cond=extra_cond,
            **kwargs)
        if not recs:
            raise ModelQueryArgError(
                "Table(%s): no record found with query condition(%s)"
                % (tb_name, kwargs))
        elif len(recs) > 1:
            raise ModelQueryArgError(
                "Table(%s): %d records found with query condition(%s)"
                % (tb_name, len(recs), kwargs))
        return recs[0]

    def get_record_by_id(self, tb_name, rec_id,
                         pk='id',
                         get_fields=(),
                         extra_cond='',
                         **kwargs):
        db_args = copy.deepcopy(kwargs)
        db_args.update({pk: rec_id})
        rec = self.get_one_record(tb_name,
                                  pk=pk,
                                  get_fields=get_fields,
                                  extra_cond=extra_cond,
                                  **db_args)
        return rec

    def _validate_mandatory_kwargs(self, tb_name, **kwargs):
        mandatory_fields = self.__required_fields(tb_name)
        for field in mandatory_fields:
            if field not in kwargs:
                raise ModelQueryArgError(
                    'DB insertion error: Table(%s) field(%s) is required!'
                    % (tb_name, field))

    def add_records(self, user, tb_name, *args):
        """
        Add multiple records at once. Enhancing efficiency.

        :param user: .
        :param tb_name: .
        :param args: list/tuple of record dict. Note: args will be changed.
        :return: list of new record ids.
        """
        new_ids = []
        if not args:
            return new_ids
        rec_args = []
        for rec_arg in args:
            self._validate_mandatory_kwargs(tb_name, **rec_arg)
            db_args = self.filter_creation_kwargs(tb_name, **rec_arg)
            self.add_creation_ext_kwargs(user, tb_name, db_args)
            rec_args.append(db_args)
        # Batch insert, incas of to many records being inserted at once.
        batch_size = 500  # 500 records one time
        batch_start = 0
        batch_end = batch_start + batch_size
        with self.db.transaction():
            while True:
                batch_hosts = rec_args[batch_start:batch_end]
                if not batch_hosts:
                    break
                new_batch_ids = self.db.multiple_insert(tb_name, batch_hosts)
                new_ids.append(new_batch_ids)
                batch_start += batch_size
                batch_end = batch_start + batch_size
        return new_ids

    def filter_creation_kwargs(self, tb_name, **kwargs):
        all_fields = self.__all_fields(tb_name)
        args = {field: kwargs[field]
                for field in all_fields if field in kwargs}
        args.pop('id', None)
        return args

    def add_creation_ext_kwargs(self, user_id, tb_name, kwargs):
        all_fields = self.__all_fields(tb_name)
        if 'createtime' in all_fields:
            kwargs['createtime'] = SQLLiteral('NOW()')
        if 'create_time' in all_fields:
            kwargs['create_time'] = SQLLiteral('NOW()')
        if 'gmt_create' in all_fields:
            kwargs['gmt_create'] = SQLLiteral('NOW()')
        if 'creator' in all_fields:
            kwargs['creator'] = user_id
        return kwargs

    def add_record(self, user, tb_name, **kwargs):
        self._validate_mandatory_kwargs(tb_name, **kwargs)
        db_args = self.filter_creation_kwargs(tb_name, **kwargs)
        self.add_creation_ext_kwargs(user, tb_name, db_args)
        new_id = self.db.insert(tb_name, **db_args)
        return new_id

    def add_non_exist_record(self, user, tb_name, **kwargs):
        """
        Insert into table only if new record doesn't break unique constraint.
        Notes: Add UNIQUE constraint in your table first !!!
        """
        self._validate_mandatory_kwargs(tb_name, **kwargs)
        db_args = self.filter_creation_kwargs(tb_name, **kwargs)
        self.add_creation_ext_kwargs(user, tb_name, db_args)
        field_names = db_args.keys()
        if not field_names:
            raise ModelQueryArgError(
                "Table(%s) insertion with empty fields" % tb_name)
        sql_temp = """
        INSERT IGNORE INTO %s (%s) VALUES (%s)
        """ % (tb_name,
               ', '.join(field_names),
               ', '.join(map(lambda x: "$%s" % x, field_names)))
        return self.db.query(sql_temp, vars=db_args)

    def update_record(self, user, tb_name, pk, pk_name='id', **kwargs):
        return self.update_records_by_pk(user, tb_name,
                                         pks=(pk,),
                                         pk_name=pk_name, **kwargs)

    def update_records_by_pk(self, user, tb_name, pks, pk_name='id', **kwargs):
        condition = {pk_name: pks}
        return self.update_records(user, tb_name, condition, **kwargs)

    def update_records(self, user, tb_name, condition, **kwargs):
        """
        :param condition: dict, query condition
        :param kwargs: fields to be updated.
        :return: count of rows updated.
        """
        args = {field: kwargs[field]
                for field in self.__edit_fields(tb_name) if field in kwargs}
        all_fields = self.__all_fields(tb_name)
        additional_args = {}
        if 'modifytime' in all_fields:
            additional_args['modifytime'] = SQLLiteral('NOW()')
        if 'modify_time' in all_fields:
            additional_args['modify_time'] = SQLLiteral('NOW()')
        if 'gmt_modified' in all_fields:
            additional_args['gmt_modified'] = SQLLiteral('NOW()')
        if 'modifier' in all_fields:
            additional_args['modifier'] = user
        args.update(additional_args)
        if '_test' in kwargs:
            args.update(_test=kwargs['_test'])
        if not args:
            return 0
        where_sql = get_sql_where(**condition)
        if not where_sql:
            raise ModelQueryArgError(
                'Table(%s): update forbidden for empty where condition!'
                % tb_name)
        cnt = self.db.update(tb_name, where=SQLQuery(where_sql), **args)
        return cnt

    def del_records(self, tb_name, pks=None, real_del=False, **kwargs):
        del_cnt = 0
        if pks:
            if isinstance(pks, (basestring, int, float, long)):
                if real_del:
                    del_cnt = self.db.delete(tb_name, long(pks))
                else:
                    del_cnt = self.db.update(tb_name, long(pks), validflag=0)
            elif isinstance(pks, (tuple, list, set)):
                pks = map(lambda pk: str(pk), pks)
                if real_del:
                    del_cnt = self.db.delete(
                        tb_name, where='id IN (%s)' % ','.join(pks))
                else:
                    del_cnt = self.db.update(
                        tb_name, where='id IN (%s)' % ','.join(pks), validflag=0)
            else:
                raise ModelQueryArgError('Del records, pks type error.')
        elif kwargs:
            args = {field: kwargs[field]
                    for field in self.__all_fields(tb_name) if field in kwargs}
            where = get_sql_where(**args)
            if not where:
                raise ModelQueryArgError(
                    'Dangerous del_records() where condition is empty !')
            if real_del:
                del_cnt = self.db.delete(tb_name, where=SQLQuery(where))
            else:
                del_cnt = self.db.update(
                    tb_name, where=SQLQuery(where), validflag=0)
        else:
            raise ModelQueryArgError('del_records() args(pks/kwargs) is empty!')
        return del_cnt

    def del_record(self, tb_name, pk, real_del=False, **kwargs):
        return self.del_records(
            tb_name, pks=(pk,), real_del=real_del, **kwargs)

    def __all_fields(self, tb):
        return self.get_tb_fields(tb, field_type=self.FieldType.all)

    def __edit_fields(self, tb):
        return self.get_tb_fields(tb, field_type=self.FieldType.editable)

    def __required_fields(self, tb):
        return self.get_tb_fields(tb, field_type=self.FieldType.required)


class O2MModel(CrudModel):
    """
    One to Many model. Model foreign keyed by other models.    -----   Adonis

    Note: Subclass must register table structure in __init__()

    Terminology:
    The one be foreign keyed is master, others foreign key to master are slaves
    """

    def __init__(self, master_tb='', **slave_tb_conf):
        """
        :param master_tb: table name for this model. Master table.
        :param slave_tb_conf:
        {
            table name: {
                fk: foreign key field name,
                attr_name: attribute name in master rec for slave records,
            },
        }
        """
        super(O2MModel, self).__init__()
        self._master_tb = master_tb  # master table name
        self._slaves_conf = {}
        for slave_name, slave_conf in slave_tb_conf.iteritems():
            conf_item = {}
            # To be compatible with old code
            if isinstance(slave_conf, types.StringTypes):
                conf_item['fk'] = slave_conf
                conf_item['attr_name'] = slave_name
            elif isinstance(slave_conf, types.DictType):
                if 'fk' not in slave_conf:
                    raise ArgError(u"O2MModel.__init__(), argument "
                                   u"'slave_tb_conf' format invalid, missing "
                                   u"'fk' filed: %r" % slave_tb_conf)
                conf_item['fk'] = slave_conf['fk']
                # attr_name is not required, use slave table name as default.
                conf_item['attr_name'] = slave_conf.get('attr_name', slave_name)
            else:
                raise ArgError(u"O2MModel.__init__(), argument 'slave_tb_conf'"
                               u"type invalid: %r" % slave_tb_conf)
            self._slaves_conf[slave_name] = conf_item

    def set_master_tn(self, tn):
        self._master_tb = tn

    def register_slaves(self, **kwargs):
        """
        Register tables foreign keyed to this model.
        Make sure register table fields of tables in args before doing this.

        :param kwargs: {table names: foreign key field name}
        """
        self._slaves_conf.update(kwargs)

    def unregister_rel_tables(self, *tables):
        """
        Unrester tables foreign keyed from this model.

        :param tables: table names
        """
        # FIXME:
        for tn in tables:
            if tn in self._slaves_conf.keys():
                del (self._slaves_conf[tn])

    def search_records_union(self, pk='id', pagination=False, **kwargs):
        """
        Union searching method: for search master and slave fields.

        :param kwargs: can be any field of master and slave tables. Key in
        kwargs will only update master fields, if it was in master, otherwise
        all slaves will be updated.
        Notes: master and slave table don't have field of the same name.
        """
        master_args, slave_args = self.__parse_union_kwargs(**kwargs)
        if not slave_args:
            return self.get_records(self._master_tb, **master_args)

        table_name = self._master_tb
        sql = """SELECT %(table_name)s.*
            FROM  %(sql_from)s
            WHERE %(sql_table_join)s AND %(sql_cond)s
            ORDER BY %(table_name)s.%(pk)s DESC
        """
        sql_from, sql_table_join, sql_cond = self.__create_sql(master_args,
                                                               slave_args)
        sql = sql % locals()
        print '---- sql is :', sql
        if pagination:
            return self.get_pagination_info(sql, **kwargs)
        else:
            records = list(self.db.query(SQLQuery(sql)))
            return records

    def get_master_slaves(self, pk='id', **master_kwargs):
        """
        :return: list of records
        """
        master_kwargs.pop('pk', None)
        master_recs = self.get_records(self._master_tb, pk=pk, **master_kwargs)
        master_ids = map(lambda x: x[pk], master_recs)
        slave_recs_conf = {}
        for sname, sconf in self._slaves_conf.iteritems():
            slave_args = {sconf['fk']: master_ids}
            slave_recs = self.get_records(sname, **slave_args)
            slave_recs_conf[sname] = slave_recs
        for mrec in master_recs:
            for sname, sconf in self._slaves_conf.iteritems():
                slave_recs = slave_recs_conf[sname]
                attr_name = sconf['attr_name']
                mrec[attr_name] = filter(lambda x: x[sconf['fk']] == mrec[pk],
                                         slave_recs)
        return master_recs

    def update_master_slaves(self, user_id, master_id,
                             master_conf={},
                             **slave_conf):
        """
        Update master and slaves of master, Add/Update/Delete slaves.

        :param master_id: pk(id field) of master table.
        :param master_conf: {arg1: 1, }, fields of master.
        :param slave_conf: {slave_table_name: [{id: 1, f1: 1}, {id: 0, f1:2}]}

        :return : count of master/slave records updated each:
        {
            master: 2,
            slave1: 0,
            slave2: 1
        }
        """
        res = {tn: 0 for tn in self._slaves_conf}
        res[self._master_tb] = 0
        master_args = {
            k: v for k, v in master_conf.iteritems()
            if k in self.get_tb_fields(
            self._master_tb, field_type=self.FieldType.editable)
        }

        with self.db.transaction():
            if master_args:
                res[self._master_tb] = self.update_record(
                    user_id, self._master_tb, master_id, **master_args)
            for tn, slaves in slave_conf.iteritems():
                new_slaves = []
                update_slaves = []
                fk = self._get_slave_fk(tn)
                old_recs = self.get_records(tn,
                                            get_fields=('id',),
                                            allow_empty_cond=False,
                                            **{fk: master_id})
                old_id_set = set(map(lambda x: x.id, old_recs))
                for slave_args in slaves:
                    slave_id = int(slave_args.get('id', 0))
                    slave_args['id'] = slave_id
                    slave_args[fk] = master_id
                    if not slave_id:
                        new_slaves.append(slave_args)
                        continue
                    if slave_id in old_id_set:
                        update_slaves.append(slave_args)
                    else:
                        raise ArgError(
                            "Master table '%s', Slave table '%s', "
                            "args for updating slave invalid: slave rec(%s) "
                            "doesn't belong to master rec(%s)" % (
                                self._master_tb, tn, slave_id, master_id))
                res[tn] += len(self.add_records(user_id, tn, *new_slaves))
                for up_args in update_slaves:
                    res[tn] += self.update_record(
                        user_id, tn, up_args['id'], **up_args)
                new_id_set = set(map(lambda x: x['id'], slaves))
                del_id_set = old_id_set.difference(new_id_set)
                del_cnt = 0
                if del_id_set:
                    del_cnt = self.del_records(
                        tn, pks=del_id_set, real_del=False)
                res[tn] += del_cnt
        return res

    def delete_master_slaves(self, master_id, real_del=False):
        del_cnt = 0
        with self.db.transaction():
            for sname, sconf in self._slaves_conf.iteritems():
                fk = sconf['fk']
                args = {fk: master_id, "validflag": 1}
                del_cnt += self.del_records(sname, real_del=real_del, **args)
            del_cnt += self.del_records(
                self._master_tb, pks=master_id, real_del=real_del)
        return del_cnt

    def update_record_union(self, user_id, master_id, slave_cond={}, **kwargs):
        """
        Deprecated !!!

        Union update method: for update master and slave fields.

        :param master_id: pk(id field) of master table.
        :param slave_cond: condition for search slave records to be updated.
        :param kwargs: valid keys are fields of master/slave table. Key in
        kwargs will be used to update all master and slaves.
        :return : count of master/slave records updated each:
        {
            master: 2,
            slave1: 0,
            slave2: 1
        }
        """
        master_args, slave_args = self.__parse_union_kwargs(shared=True,
                                                            **kwargs)
        res = {self._master_tb: 0}
        for slave_tb in self._slaves_conf:
            res[slave_tb] = 0

        if not slave_args:
            res[self._master_tb] = self.update_record(self._master_tb,
                                                      master_id, **master_args)
            return res

        with self.db.transaction():
            if master_args:
                master_cnt = self.update_record(
                    user_id, self._master_tb, master_id, **master_args)
                res[self._master_tb] = master_cnt

            for slave_tb in self._slaves_conf:
                condition = {self.__fk_name(slave_tb): master_id}
                extra_cond = {key: slave_cond[key] for key in slave_cond
                              if key in self.__all_fields(slave_tb)}
                condition.update(extra_cond)
                slave_args_cp = copy.deepcopy(slave_args)
                for key in condition:
                    if key in slave_args_cp:
                        del (slave_args_cp[key])
                if not slave_args_cp:
                    res[slave_tb] = 0
                    continue
                res[slave_tb] = self.update_records(
                    user_id, slave_tb, condition=condition, **slave_args_cp)
        return res

    def __parse_union_kwargs(self, shared=False, **kwargs):
        """
        :param shared: whether arg in kwargs should be shared by
        master and slaves, or exclusively belongs to master.
        """
        master_args = {}
        slave_args = {}
        for key in kwargs:
            if key in self.__all_fields(self._master_tb):
                master_args[key] = kwargs[key]
                if not shared:
                    continue
            for tb in self._slaves_conf:
                if tb not in slave_args:
                    slave_args[tb] = {}
                if key in self.__all_fields(tb):
                    slave_args[tb][key] = kwargs[key]
        return master_args, slave_args

    def _get_slave_fk(self, tb):
        return self._slaves_conf[tb]['fk']

    def __fk_name(self, tb):
        return tb + '.' + self._slaves_conf[tb]['fk']

    def __create_sql(self, master_args, slave_args):
        sql_from = ','.join(slave_args.keys() + [self._master_tb])
        sql_table_join = []
        master_id = '%s.id' % self._master_tb
        for slave_tn in slave_args:
            slave_fk = self.__fk_name(slave_tn)
            sql_table_join.append("%(slave_fk)s = %(master_id)s" % locals())
        sql_table_join = ' AND '.join(sql_table_join)

        sql_cond = {}
        for slave_tn, slave_kwargs in slave_args.iteritems():
            for k, v in slave_kwargs.iteritems():
                key = slave_tn + '.' + k
                sql_cond[key] = v
        for key, val in master_args.iteritems():
            new_key = self._master_tb + '.' + key
            sql_cond[new_key] = val
        sql_cond = get_sql_where(**sql_cond)
        return sql_from, sql_table_join, sql_cond

    def __all_fields(self, tb):
        return self.get_tb_fields(tb, field_type=self.FieldType.all)


class M2MModel(CrudModel):
    """
    Many to Many model. Two models with rel table as relation. ----- Adonis

    Note: Subclass must register table structure in __init__()

    Terminology: Left table, right table, rel table
    """

    def __init__(self, left_tb, rel_conf, right_tb):
        """
        :param left_tb: .
        :param right_tb: .
        :param rel_conf: config for relation table
        {
            tb_name: 'rel_tb_name',     # 关系表的表名
            left_pk: 'left_tb_pk',      # left table 在rel中的外键名称
            right_pk: 'right_tb_pk',    # right table 在rel中的外键名称
        }
        """
        super(M2MModel, self).__init__()
        self._left_tb = left_tb
        self._right_tb = right_tb
        self._rel_conf = rel_conf
        if 'tb_name' not in rel_conf:
            raise ArgError(u"M2MModel.__init__(), argument 'rel_conf' "
                           u"invalid: missing 'tb_name' attr")
        if 'left_pk' not in rel_conf:
            rel_conf['left_pk'] = left_tb + "_id"
        if 'right_pk' not in rel_conf:
            rel_conf['right_pk'] = right_tb + "_id"
        self._rel_tb = self._rel_conf['tb_name']
        self._rel_left_pk = self._rel_conf['left_pk']
        self._rel_right_pk = self._rel_conf['right_pk']

    def get_right_records(self, left_pks,
                          right_pk_name='id',
                          get_fields=(),
                          rel_flat_fields=[],
                          rel_flat_conf={}):
        """
        Get right recs corresponding to specified left records

        :param right_pk_name: primary key field name of right table
        :param get_fields: fields that will be extracted from right table
        :param rel_flat_fields: fields of relation record that needed to be
                added/flattened to right DB record.
        :param rel_flat_conf: {       # More complicated flat config
            rel_field_name1: field_name_flatted_to_record,
            rel_field_name2: field_name_flatted_to_record,
        }
        :return: {
            left_pk1: [{}, {}], # List of right DB records, with corresponding
                                # DB relation record flattened to it.
            left_pk2: [{}, {}],
        }
        """
        return self.get_related_records(left_pks,
                                        reverse_order=False,
                                        dest_pk_name=right_pk_name,
                                        get_fields=get_fields,
                                        rel_flat_fields=rel_flat_fields,
                                        rel_flat_conf=rel_flat_conf)

    def get_left_records(self, right_pks,
                         left_pk_name='id',
                         get_fields=(),
                         rel_flat_fields=[],
                         rel_flat_conf={}):
        """
        Get left recs corresponding to specified right records

        :param left_pk_name: primary key field name of left table
        :param get_fields: fields that will be extracted from left table
        :param rel_flat_fields: fields of relation record that needed to be
                added/flattened to left DB record.
        :param rel_flat_conf: {       # More complicated flat config
            rel_field_name1: field_name_flatted_to_Db record,
            rel_field_name2: field_name_flatted_to_Db_record,
        }
        :return: {
            right_pk1: [{}, {}], # List of left DB records, with corresponding
                                # DB relation record flattened to it.
            right_pk2: [{}, {}],
        }
        """
        return self.get_related_records(right_pks,
                                        reverse_order=True,
                                        dest_pk_name=left_pk_name,
                                        get_fields=get_fields,
                                        rel_flat_fields=rel_flat_fields,
                                        rel_flat_conf=rel_flat_conf)

    def del_right_record(self, right_pk):
        return self.del_related_records(right_pk, reverse_order=False)

    def del_left_record(self, left_pk):
        return self.del_related_records(left_pk, reverse_order=True)

    def del_related_records(self, pks, reverse_order=False):
        """
        Delete records of left/right table and corresponding relation in
        rel table.

        :param reverse_order: False: delete records of right table.
                            True: delete records of left table.
        :return count of deleted records
        """
        cnt = 0
        src_tb, dest_tb, rel_src_pk, rel_dest_pk = \
            self._get_direction_info(reverse_order=reverse_order)
        with self.db.transaction():
            cnt += self.del_records(dest_tb, pks)
            db_args = {
                rel_dest_pk: pks,
            }
            cnt += self.del_records(self._rel_tb, **db_args)
        return cnt

    def add_new_right_rec(self, left_pk, user_id=0,
                          rel_kwargs={},
                          **right_kwargs):
        """
        :param rel_kwargs: dict of field name and value map of relation table
        :param right_kwargs: dict of field name and value map of right table
        :return: id of newly added right record
        """
        return self.add_new_related_rec(left_pk,
                                        user_id=user_id,
                                        reverse_order=False,
                                        rel_kwargs=rel_kwargs,
                                        **right_kwargs)

    def add_new_left_rec(self, right_pk, user_id=0,
                         rel_kwargs={},
                         **left_kwargs):
        return self.add_new_related_rec(right_pk,
                                        user_id=user_id,
                                        reverse_order=True,
                                        rel_kwargs=rel_kwargs,
                                        **left_kwargs)

    def add_new_related_rec(self, src_pk,
                            user_id=0,
                            reverse_order=False,
                            rel_kwargs={},
                            **rec_kwargs):
        src_tb, dest_tb, rel_src_pk, rel_dest_pk = \
            self._get_direction_info(reverse_order=reverse_order)
        with self.db.transaction():
            dest_id = self.add_record(user_id, dest_tb, **rec_kwargs)
            db_kwargs = dict(rel_kwargs)
            db_kwargs[rel_src_pk] = src_pk
            db_kwargs[rel_dest_pk] = dest_id
            _ = self.add_record(user_id, self._rel_tb, **db_kwargs)
        return dest_id

    def _get_direction_info(self, reverse_order=False):
        if reverse_order:
            src_tb = self._right_tb
            dest_tb = self._left_tb
            rel_src_pk = self._rel_right_pk
            rel_dest_pk = self._rel_left_pk
        else:
            src_tb = self._left_tb
            dest_tb = self._right_tb
            rel_src_pk = self._rel_left_pk
            rel_dest_pk = self._rel_right_pk
        return src_tb, dest_tb, rel_src_pk, rel_dest_pk

    def _get_related_records(self, src_pks,
                             reverse_order=False,
                             dest_pk_name='id',
                             get_fields=()):
        """
        Get right/left recs related to left/right records

        :param reverse_order: bool, False: get right records
        :return:  2-item tuple
        {                      # Map of source record to relation recs
            pk1: [{}, {}],     # List of DB records of relation table
            pk2: [{}, {}],
        },
        [{}, {}, ]                  # List of dest DB recs
        """
        src_rel_map = {src_pk: {} for src_pk in src_pks}
        src_tb, dest_tb, rel_src_pk, rel_dest_pk = \
            self._get_direction_info(reverse_order=reverse_order)
        db_args = {rel_src_pk: src_pks}
        rel_recs = self.get_records(self._rel_tb, **db_args)
        dest_pks = map(lambda x: x.get(rel_dest_pk), rel_recs)
        dest_pks = set(dest_pks)  # filter out duplicated pk
        db_args = {dest_pk_name: dest_pks}
        dest_recs = self.get_records(dest_tb, get_fields=get_fields, **db_args)
        for src_pk, _ in src_rel_map.iteritems():
            src_rel_map[src_pk] = filter(
                lambda x: src_pk == x.get(rel_src_pk), rel_recs)
        return src_rel_map, dest_recs

    def get_related_records(self, src_pks,
                            reverse_order=False,
                            dest_pk_name='id',
                            get_fields=(),
                            rel_flat_fields=[],
                            rel_flat_conf={}):
        """
        Get right/left recs corresponding to left/right records

        :param reverse_order: bool, 是否是进行反向操作, True: get left records
                            of right record
        :param dest_pk_name: primary key field name of dest table
        :param get_fields: fields that will be extracted from dest table
        :param rel_flat_fields: fields of relation record that needed to be
                added/flattened to dest DB record.
        :param rel_flat_conf: {       # More complicated flat config
            rel_field_name1: field_name_flatted_to_Db record,
            rel_field_name2: field_name_flatted_to_Db_record,
        }
        :return: {
            pk1: [{}, {}],  # List of dest DB records, with corresponding
                            # DB relation record flattened to it.
            pk2: [{}, {}],
        }
        """
        res = {x: [] for x in src_pks}
        src_tb, dest_tb, rel_src_pk, rel_dest_pk = \
            self._get_direction_info(reverse_order=reverse_order)
        src_rel_map, dest_recs = self._get_related_records(
            src_pks,
            reverse_order=reverse_order,
            dest_pk_name=dest_pk_name,
            get_fields=get_fields)
        dest_rec_map = {rec.get(dest_pk_name): rec for rec in dest_recs}
        for src_pk, rel_recs in src_rel_map.iteritems():
            for rel_rec in rel_recs:
                dest_pk_value = rel_rec.get(rel_dest_pk)
                dest_rec = copy.deepcopy(dest_rec_map[dest_pk_value])
                for rel_field in rel_flat_fields:
                    dest_rec[rel_field] = rel_rec.get(rel_field, None)
                for k, v in rel_flat_conf.iteritems():
                    dest_rec[v] = rel_rec.get(k, None)
                res[src_pk].append(dest_rec)
        return res
