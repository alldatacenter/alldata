import io
import logging
import os
import tempfile
from datetime import datetime
from zipfile import ZipFile

from pymongo.collection import Collection
from pymongo.database import Database
import gridfs
from print_color import print as print_color

from crawlab.actions.upload import create_spider, upload_file, is_ignored, exists_spider_by_name
from crawlab.errors.upload import HttpException


def migrate(mongo_db: Database):
    print_color('start migrating', tag='info', tag_color='cyan', color='white')

    # mongo spiders collection
    mongo_col_spiders = mongo_db.spiders

    # spiders
    spiders = _get_spiders(mongo_col_spiders)
    print_color(f'found {len(spiders)} spiders', tag='info', tag_color='cyan', color='white')

    # mongo grid fs
    mongo_grid_fs = gridfs.GridFS(mongo_db, collection='files')

    # mongo grid fs buckets
    mongo_grid_fs_bucket = gridfs.GridFSBucket(mongo_db, bucket_name='files')

    # stats
    stats = {
        'success': 0,
        'error': 0,
        'skipped': 0,
    }

    # iterate spiders
    for spider in spiders:
        # download and extract zip file
        zip_file_extract_path = _download_and_extract_zip_file(spider['name'], mongo_grid_fs, mongo_grid_fs_bucket)

        # migrated spider name
        migrated_spider_name = f'{spider["name"]}_{spider["_id"]}'

        # create spider if not exists
        if exists_spider_by_name(migrated_spider_name):
            print_color(f'spider "{spider["name"]}" already migrated', tag='info', tag_color='cyan', color='white')
            stats['skipped'] += 1
            continue

        try:
            spider_id = create_spider(name=migrated_spider_name,
                                      description=f'migrated from older version {datetime.now()}')
            print_color(f'created spider {spider["name"]} (id: {spider_id})', tag='success', tag_color='green',
                        color='white')
        except HttpException:
            print_color(f'create spider {spider["name"]} failed', tag='error', tag_color='red', color='white')
            stats['error'] += 1
            return

        # upload spider files to api
        for root, dirs, files in os.walk(zip_file_extract_path):
            for file_name in files:
                # file path
                file_path = os.path.join(root, file_name)

                # ignored file
                if is_ignored(file_path):
                    continue

                # target path
                target_path = file_path.replace(zip_file_extract_path, '')

                # upload file
                upload_file(spider_id, file_path, target_path)

        stats['success'] += 1

        # spider logging
        print_color(f'uploaded spider {spider["name"]}', tag='success', tag_color='green', color='white')

    # logging
    print_color(f'migration finished', tag='info', tag_color='cyan', color='white')
    print_color(f'success: {stats["success"]}', tag='info', tag_color='cyan', color='white')
    print_color(f'failed: {stats["error"]}', tag='info', tag_color='cyan', color='white')
    print_color(f'skipped: {stats["skipped"]}', tag='info', tag_color='cyan', color='white')


def _get_spiders(mongo_col_spiders: Collection):
    spiders = []
    for spider in mongo_col_spiders.find():
        spiders.append(spider)
    return spiders


def _download_and_extract_zip_file(spider_name: str, mongo_grid_fs: gridfs.GridFS,
                                   mongo_grid_fs_bucket: gridfs.GridFSBucket):
    tmp_dir = tempfile.gettempdir()
    root_dir = os.path.join(tmp_dir, spider_name)
    os.makedirs(root_dir, exist_ok=True)

    # validate zip file exists
    zip_file_info = mongo_grid_fs.find_one({'filename': f'{spider_name}.zip'})
    if zip_file_info is None:
        return
    zip_file_name = f'{spider_name}.zip'
    zip_file_path = f'{root_dir}/{zip_file_name}'

    with open(zip_file_path, 'wb') as f:
        mongo_grid_fs_bucket.download_to_stream_by_name(zip_file_name, f)

    zip_file = ZipFile(zip_file_path)

    zip_file_extract_path = f'{root_dir}/{spider_name}'
    os.makedirs(zip_file_extract_path, exist_ok=True)

    zip_file.extractall(zip_file_extract_path)

    return zip_file_extract_path
