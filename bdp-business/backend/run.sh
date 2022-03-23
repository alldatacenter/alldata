#!/bin/bash

# 记录发布日志
function record_release_log() {
    now=$(date +'%Y-%m-%d %H:%M:%S')
    commit=$(git log --format=oneline -n 1)
    echo "$now $commit" >> release.log
}

case $1 in
    start)
        echo "gunicorn -c gunicorn.conf.py --env DJANGO_SETTINGS_MODULE=main.settings.production main.wsgi"
        record_release_log
        gunicorn -c gunicorn.conf.py --env DJANGO_SETTINGS_MODULE=main.settings.production main.wsgi
        ;;
    start:test)
        echo "gunicorn -c gunicorn.conf.py --env DJANGO_SETTINGS_MODULE=main.settings.testing main.wsgi"
        record_release_log
        gunicorn -c gunicorn.conf.py --env DJANGO_SETTINGS_MODULE=main.settings.testing main.wsgi
        ;;
    stop)
        echo "xargs<logs/gunicorn.pid kill -9"
        xargs<logs/gunicorn.pid kill -9
        ;;
    reload)
        echo "./run.sh stop && ./run.sh start"
        ./run.sh stop && ./run.sh start
        ;;
    reload:test)
        echo "./run.sh stop && ./run.sh start:test"
        ./run.sh stop && ./run.sh start:test
        ;;
    makemigrations)
        echo "DJANGO_SETTINGS_MODULE=main.settings.production python manage.py makemigrations"
        DJANGO_SETTINGS_MODULE=main.settings.production python manage.py makemigrations
        ;;
    makemigrations:test)
        echo "DJANGO_SETTINGS_MODULE=main.settings.testing python manage.py makemigrations"
        DJANGO_SETTINGS_MODULE=main.settings.testing python manage.py makemigrations
        ;;
    migrate)
        echo "DJANGO_SETTINGS_MODULE=main.settings.production python manage.py migrate"
        DJANGO_SETTINGS_MODULE=main.settings.production python manage.py migrate
        ;;
    migrate:test)
        echo "DJANGO_SETTINGS_MODULE=main.settings.testing python manage.py migrate"
        DJANGO_SETTINGS_MODULE=main.settings.testing python manage.py migrate
        ;;
    collectstatic)
        echo "DJANGO_SETTINGS_MODULE=main.settings.production python manage.py collectstatic"
        DJANGO_SETTINGS_MODULE=main.settings.production python manage.py collectstatic
        ;;
    collectstatic:test)
        echo "DJANGO_SETTINGS_MODULE=main.settings.testing python manage.py collectstatic"
        DJANGO_SETTINGS_MODULE=main.settings.testing python manage.py collectstatic
        ;;
    cron.add)
        echo "DJANGO_SETTINGS_MODULE=main.settings.production python manage.py crontab add"
        DJANGO_SETTINGS_MODULE=main.settings.production python manage.py crontab add
        ;;
    cron.add:test)
        echo "DJANGO_SETTINGS_MODULE=main.settings.testing python manage.py crontab add"
        DJANGO_SETTINGS_MODULE=main.settings.testing python manage.py crontab add
        ;;
    cron.show)
        echo "DJANGO_SETTINGS_MODULE=main.settings.production python manage.py crontab show"
        DJANGO_SETTINGS_MODULE=main.settings.production python manage.py crontab show
        ;;
    cron.show:test)
        echo "DJANGO_SETTINGS_MODULE=main.settings.testing python manage.py crontab show"
        DJANGO_SETTINGS_MODULE=main.settings.testing python manage.py crontab show
        ;;
    cron.remove)
        echo "DJANGO_SETTINGS_MODULE=main.settings.production python manage.py crontab remove"
        DJANGO_SETTINGS_MODULE=main.settings.production python manage.py crontab remove
        ;;
    cron.remove:test)
        echo "DJANGO_SETTINGS_MODULE=main.settings.testing python manage.py crontab remove"
        DJANGO_SETTINGS_MODULE=main.settings.testing python manage.py crontab remove
        ;;
    *)
        echo ""
        echo "Use: ./run.sh <command>"
        echo ""
        echo "👉  You must specify a parameter, available parameters:"
        echo ""
        echo "   start"
        echo "   start:test"
        echo "   stop"
        echo "   reload"
        echo "   reload:test"
        echo "   makemigrations"
        echo "   makemigrations:test"
        echo "   migrate"
        echo "   migrate:test"
        echo "   collectstatic"
        echo "   collectstatic:test"
        echo "   cron.add"
        echo "   cron.add:test"
        echo "   cron.show"
        echo "   cron.show:test"
        echo "   cron.remove"
        echo "   cron.remove:test"
        echo ""
        echo "Example: ./run.sh start"
        echo ""
        ;;
esac
