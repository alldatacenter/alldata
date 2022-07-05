from django.db import models


class BaseModel(models.Model):
    STATUS_INVALID = -1
    STATUS_VALID = 0
    STATUS_CHOICES = (
        (STATUS_INVALID, '失效'),
        (STATUS_VALID, '生效'),
    )

    status = models.SmallIntegerField(help_text='状态', choices=STATUS_CHOICES, default=STATUS_VALID, blank=True)
    created_at = models.DateTimeField(help_text='创建时间', auto_now_add=True)
    updated_at = models.DateTimeField(help_text='更新时间', auto_now=True)

    class Meta:
        abstract = True
