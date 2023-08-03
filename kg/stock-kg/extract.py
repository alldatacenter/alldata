import os
import csv
from lxml import etree


def extract(stockpage_dir, executive_csv):
    """Extract executive of the comnpany or stock

    Args:
        stockpage_dir: (str) the directory of stock pages
        executive_csv: (str) the full path of the CSV file to be saved
    """
    pages = map(lambda _: os.path.join(stockpage_dir, _), os.listdir(stockpage_dir))
    pages = filter(lambda _: _.endswith('html'), pages)
    headers = ['name', 'gender', 'age', 'code', 'jobs']

    with open(directors_csv, 'w', encoding='utf-8') as file_directors:
        file_directors_csv = csv.DictWriter(file_directors, headers)
        file_directors_csv.writeheader()

        for page in pages:
            print(page)  # the full path of a stock page
            file_name = page.split(r'/')[-1]
            code = file_name.split('.')[0]
            executives = []
            with open(page, 'r', encoding='gbk') as file_page:
                content = file_page.read()
                html = etree.HTML(content)
                divs = html.xpath('//div[@id="ml_001"]//div[contains(@class, "person_table")]')

                for div in divs:
                    item = {}
                    item['name'] = div.xpath('.//thead/tr/td/h3/a/text()')[0].replace(',', '-')
                    item['jobs'] = div.xpath('.//thead/tr[1]/td[2]/text()')[0].replace(',', '/')
                    gender_age_education = div.xpath('.//thead/tr[2]/td[1]/text()')[0].split()
                    try:
                        item['gender'] = gender_age_education[0]
                        if item['gender'] not in ('男', '女'):
                            item['gender'] = 'null'  # null for unknown
                    except IndexError:
                        item['gender'] = 'null'

                    try:
                        item['age'] = gender_age_education[1].strip('岁')
                        try:
                            item['age'] = int(item['age'])
                        except ValueError:
                            item['age'] = -1  # -1 for unknown
                    except IndexError:
                        item['age'] = -1

                    item['code'] = code
                    
                    executives.append(item)
            # write to csv file
            file_directors_csv.writerows(executives)


if __name__ == '__main__':
    stockpage_dir = './data/stockpage'
    directors_csv = './data/executive_prep.csv'
    extract(stockpage_dir, directors_csv)

