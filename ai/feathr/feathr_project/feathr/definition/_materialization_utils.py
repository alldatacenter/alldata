from jinja2 import Template 
from feathr.definition.materialization_settings import MaterializationSettings


def _to_materialization_config(settings: MaterializationSettings):
    # produce materialization config
    tm = Template("""
            operational: {
            name: {{ settings.name }}
            endTime: "{{ settings.backfill_time.end.strftime('%Y-%m-%d %H:%M:%S') }}"
            endTimeFormat: "yyyy-MM-dd HH:mm:ss"
            resolution: {{ settings.resolution }}
            {% if settings.has_hdfs_sink == True %}
            enableIncremental = true
            {% endif %}
            output:[
                    {% for sink in settings.sinks %}
                        {{sink.to_feature_config()}}
                    {% endfor %}
                ]
            }
        features: [{{','.join(settings.feature_names)}}]
    """)
    msg = tm.render(settings=settings)
    return msg