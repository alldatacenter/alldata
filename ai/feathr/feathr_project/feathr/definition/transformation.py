import enum
from typing import Type, Union, List, Optional
from abc import ABC, abstractmethod
from jinja2 import Template
from feathr.definition.feathrconfig import HoconConvertible

class Transformation(HoconConvertible):
    """Base class for all transformations that produce feature values."""
    pass


class RowTransformation(Transformation):
    """Base class for all row-level transformations."""
    pass


class ExpressionTransformation(RowTransformation):
    """A row-level transformations that is defined with the Feathr expression language.

    Attributes:
        expr: expression that transforms the raw value into a new value, e.g. amount * 10.
    """
    def __init__(self, expr: str) -> None:
        super().__init__()
        self.expr = expr

    def to_feature_config(self, with_def_field_name: Optional[bool] = True) -> str:
        tm = Template("""
{% if with_def_field_name %}
def.sqlExpr: "{{expr}}"
{% else %}
"{{expr}}"
{% endif %}
        """)
        return tm.render(expr=self.expr, with_def_field_name=with_def_field_name)


class WindowAggTransformation(Transformation):
    """Aggregate the value of an expression over a fixed time window. E.g. sum(amount*10) over last 3 days.

    Attributes:
        agg_expr: expression that transforms the raw value into a new value, e.g. amount * 10
        agg_func: aggregation function. Available values: `SUM`, `COUNT`, `MAX`, `MIN`, `AVG`, `MAX_POOLING`, `MIN_POOLING`, `AVG_POOLING`, `LATEST`
        window: Time window length to apply the aggregation. support 4 type of units: d(day), h(hour), m(minute), s(second). The example value are "7d' or "5h" or "3m" or "1s"
        group_by: Feathr expressions applied after the `agg_expr` transformation as groupby field, before aggregation, same as 'group by' in SQL
        filter: Feathr expression applied to each row as a filter before aggregation. This should be a string and a valid Spark SQL Expression. For example: filter = 'age > 3'. This is similar to PySpark filter operation and more details can be learned here: https://spark.apache.org/docs/latest/api/python/reference/pyspark.sql/api/pyspark.sql.DataFrame.filter.html
    """
    def __init__(self, agg_expr: str, agg_func: str, window: str, group_by: Optional[str] = None, filter: Optional[str] = None, limit: Optional[int] = None) -> None:
        super().__init__()
        self.def_expr = agg_expr
        self.agg_func = agg_func
        self.window = window
        self.group_by = group_by
        self.filter = filter
        self.limit = limit

    def to_feature_config(self, with_def_field_name: Optional[bool] = True) -> str:
        tm = Template("""
def:"{{windowAgg.def_expr}}"
window: {{windowAgg.window}}
aggregation: {{windowAgg.agg_func}}
{% if windowAgg.group_by is not none %}
    groupBy: {{windowAgg.group_by}}
{% endif %}
{% if windowAgg.filter is not none %}
    filter: {{windowAgg.filter}}
{% endif %}
{% if windowAgg.limit is not none %}
    limit: {{windowAgg.limit}}
{% endif %}
        """)
        return tm.render(windowAgg = self)


class UdfTransform(Transformation):
    """User defined transformation. To be supported.

    Attributes:
        name: name of the user defined function
    """
    def __init__(self, name: str) -> None:
        """

        :param name:
        """
        super().__init__()
        self.name = name
    def to_feature_config(self) -> str:
        pass