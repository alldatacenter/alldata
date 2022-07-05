import logging
from django import forms
from django.db.models import Count
from common.models import (
    User,
)

logger = logging.getLogger("app.common")


class UploadFileForm(forms.Form):
    file = forms.FileField()


def get_users(ctx, kwargs):
    """
    拉取用户列表
    """
    filters = kwargs.get("filters", {})
    order_by = kwargs.get("order_by", "-created_at")
    page, page_size, offset, limit = (1, 10, 0, 9)

    query_set = (
        User.objects.filter(**filters).order_by(order_by).annotate(count=Count("id"))
    )

    total = query_set.count()
    results = query_set[offset: offset + limit]

    return {"page": page, "page_size": page_size, "total": total, "results": results}
