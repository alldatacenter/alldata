from django.apps import AppConfig


class CommonConfig(AppConfig):
    name = "common"

    def ready(self):
        import common.signals
