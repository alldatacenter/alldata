from django.contrib import admin
from django.contrib.auth.admin import UserAdmin as SuperUserAdmin
from django.utils.translation import gettext_lazy as _
from common.models import (
    User,
)


@admin.register(User)
class UserAdmin(SuperUserAdmin):
    fieldsets = (
        (None, {"fields": ("username", "password")}),
        (
            _("Personal info"),
            {
                "fields": (
                    "first_name",
                    "last_name",
                    "email",
                    "phone",
                    "role",
                )
            },
        ),
        (
            _("Permissions"),
            {
                "fields": (
                    "is_active",
                    "is_staff",
                    "is_superuser",
                    "groups",
                    "user_permissions",
                ),
            },
        ),
        (_("Important dates"), {"fields": ("last_login", "date_joined")}),
    )
    list_display = (
        "id",
        "username",
        "role",
        "email",
        "phone",
        "first_name",
        "last_name",
        "is_staff",
    )
    ordering = ("id",)