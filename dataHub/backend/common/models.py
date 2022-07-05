from django.db import models
from django.contrib.auth.models import AbstractUser


class User(AbstractUser):
    GUEST = 0
    OPERATOR = 1
    LAWYER = 2
    ADMIN = 99
    ROLE_CHOICES = (
        (GUEST, "guest"),
        (LAWYER, "lawyer"),
        (OPERATOR, "operator"),
        (ADMIN, "admin"),
    )

    phone = models.CharField(help_text="手机号", max_length=16, blank=True, null=True)
    role = models.IntegerField(help_text="角色", choices=ROLE_CHOICES, default=GUEST)
    created_at = models.DateTimeField(help_text="创建时间", auto_now_add=True)
    updated_at = models.DateTimeField(help_text="更新时间", auto_now=True)

    @property
    def name(self):
        return self.last_name + self.first_name
