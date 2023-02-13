import os
import csv
import hashlib


def get_md5(string):
    """Get md5 according to the string
    """
    byte_string = string.encode("utf-8")
    md5 = hashlib.md5()
    md5.update(byte_string)
    result = md5.hexdigest()
    return result


def build_executive(executive_prep, executive_import):
    """Create an 'executive' file in csv format that can be imported into Neo4j.
    format -> person_id:ID,name,gender,age:int,:LABEL
    label -> Person
    """
    print('Writing to {} file...'.format(executive_import.split('/')[-1]))
    with open(executive_prep, 'r', encoding='utf-8') as file_prep, \
        open(executive_import, 'w', encoding='utf-8') as file_import:
        file_prep_csv = csv.reader(file_prep, delimiter=',')
        file_import_csv = csv.writer(file_import, delimiter=',')

        headers = ['person_id:ID', 'name', 'gender', 'age:int', ':LABEL']
        file_import_csv.writerow(headers)
        for i, row in enumerate(file_prep_csv):
            if i == 0 or len(row) < 3:
                continue
            info = [row[0], row[1], row[2]]
            # generate md5 according to 'name' 'gender' and 'age'
            info_id = get_md5('{},{},{}'.format(row[0], row[1], row[2]))
            info.insert(0, info_id)
            info.append('Person')
            file_import_csv.writerow(info)
    print('- done.')


def build_stock(stock_industry_prep, stock_concept_prep, stock_import):
    """Create an 'stock' file in csv format that can be imported into Neo4j.
    format -> company_id:ID,name,code,:LABEL
    label -> Company,ST
    """
    print('Writing to {} file...'.format(stock_import.split('/')[-1]))
    stock = set()  # 'code,name'

    with open(stock_industry_prep, 'r', encoding='utf-8') as file_prep:
        file_prep_csv = csv.reader(file_prep, delimiter=',')
        for i, row in enumerate(file_prep_csv):
            if i == 0:
                continue
            code_name = '{},{}'.format(row[0], row[1].replace(' ', ''))
            stock.add(code_name)

    with open(stock_concept_prep, 'r', encoding='utf-8') as file_prep:
        file_prep_csv = csv.reader(file_prep, delimiter=',')
        for i, row in enumerate(file_prep_csv):
            if i == 0:
                continue
            code_name = '{},{}'.format(row[0], row[1].replace(' ', ''))
            stock.add(code_name)

    with open(stock_import, 'w', encoding='utf-8') as file_import:
        file_import_csv = csv.writer(file_import, delimiter=',')
        headers = ['stock_id:ID', 'name', 'code', ':LABEL']
        file_import_csv.writerow(headers)
        for s in stock:
            split = s.split(',')
            ST = False  # ST flag
            states = ['*ST', 'ST', 'S*ST', 'SST']
            info = []
            for state in states:
                if split[1].startswith(state):
                    ST = True
                    split[1] = split[1].replace(state, '')
                    info = [split[0], split[1], split[0], 'Company;ST']
                    break
                else:
                    info = [split[0], split[1], split[0], 'Company']
            file_import_csv.writerow(info)
    print('- done.')


def build_concept(stock_concept_prep, concept_import):
    """Create an 'concept' file in csv format that can be imported into Neo4j.
    format -> concept_id:ID,name,:LABEL
    label -> Concept
    """
    print('Writing to {} file...'.format(concept_import.split('/')[-1]))
    with open(stock_concept_prep, 'r', encoding='utf-8') as file_prep, \
        open(concept_import, 'w', encoding='utf-8') as file_import:
        file_prep_csv = csv.reader(file_prep, delimiter=',')
        file_import_csv = csv.writer(file_import, delimiter=',')

        headers = ['concept_id:ID', 'name', ':LABEL']
        file_import_csv.writerow(headers)
        concepts = set()
        for i, row in enumerate(file_prep_csv):
            if i == 0:
                continue
            concepts.add(row[2])
        for concept in concepts:
            concept_id = get_md5(concept)
            new_row = [concept_id, concept, 'Concept']
            file_import_csv.writerow(new_row)
    print('- done.')


def build_industry(stock_industry_prep, industry_import):
    """Create an 'industry' file in csv format that can be imported into Neo4j.
    format -> industry_id:ID,name,:LABEL
    label -> Industry
    """
    print('Write to {} file...'.format(industry_import.split('/')[-1]))
    with open(stock_industry_prep, 'r', encoding="utf-8") as file_prep, \
        open(industry_import, 'w', encoding='utf-8') as file_import:
        file_prep_csv = csv.reader(file_prep, delimiter=',')
        file_import_csv = csv.writer(file_import, delimiter=',')
        headers = ['industry_id:ID', 'name', ':LABEL']
        file_import_csv.writerow(headers)

        industries = set()
        for i, row in enumerate(file_prep_csv):
            if i == 0:
                continue
            industries.add(row[2])
        for industry in industries:
            industry_id = get_md5(industry)
            new_row = [industry_id, industry, 'Industry']
            file_import_csv.writerow(new_row)
    print('- done.')


def build_executive_stock(executive_prep, relation_import):
    """Create an 'executive_stock' file in csv format that can be imported into Neo4j.
    format -> :START_ID,title,:END_ID,:TYPE
               person          stock
    type -> employ_of
    """
    with open(executive_prep, 'r', encoding='utf-8') as file_prep, \
        open(relation_import, 'w', encoding='utf-8') as file_import:
        file_prep_csv = csv.reader(file_prep, delimiter=',')
        file_import_csv = csv.writer(file_import, delimiter=',')
        headers = [':START_ID', 'jobs', ':END_ID', ':TYPE']
        file_import_csv.writerow(headers)

        for i, row in enumerate(file_prep_csv):
            if i == 0:
                continue
            # generate md5 according to 'name' 'gender' and 'age'
            start_id = get_md5('{},{},{}'.format(row[0], row[1], row[2]))
            end_id = row[3]  # code
            relation = [start_id, row[4], end_id, 'employ_of']
            file_import_csv.writerow(relation)


def build_stock_industry(stock_industry_prep, relation_import):
    """Create an 'stock_industry' file in csv format that can be imported into Neo4j.
    format -> :START_ID,:END_ID,:TYPE
               stock   industry
    type -> industry_of
    """
    with open(stock_industry_prep, 'r', encoding='utf-8') as file_prep, \
        open(relation_import, 'w', encoding='utf-8') as file_import:
        file_prep_csv = csv.reader(file_prep, delimiter=',')
        file_import_csv = csv.writer(file_import, delimiter=',')
        headers = [':START_ID', ':END_ID', ':TYPE']
        file_import_csv.writerow(headers)

        for i, row in enumerate(file_prep_csv):
            if i == 0:
                continue
            industry = row[2]
            start_id = row[0]  # code
            end_id = get_md5(industry)
            relation = [start_id, end_id, 'industry_of']
            file_import_csv.writerow(relation)


def build_stock_concept(stock_concept_prep, relation_import):
    """Create an 'stock_industry' file in csv format that can be imported into Neo4j.
    format -> :START_ID,:END_ID,:TYPE
               stock   concept
    type -> concept_of
    """
    with open(stock_concept_prep, 'r', encoding='utf-8') as file_prep, \
        open(relation_import, 'w', encoding='utf-8') as file_import:
        file_prep_csv = csv.reader(file_prep, delimiter=',')
        file_import_csv = csv.writer(file_import, delimiter=',')
        headers = [':START_ID', ':END_ID', ':TYPE']
        file_import_csv.writerow(headers)

        for i, row in enumerate(file_prep_csv):
            if i == 0:
                continue
            concept = row[2]
            start_id = row[0]  # code
            end_id = get_md5(concept)
            relation = [start_id, end_id, 'concept_of']
            file_import_csv.writerow(relation)


if __name__ == '__main__':
    import_path = 'data/import'
    if not os.path.exists(import_path):
        os.makedirs(import_path)
    build_executive('data/executive_prep.csv', 'data/import/executive.csv')
    build_stock('data/stock_industry_prep.csv', 'data/stock_concept_prep.csv', 
        'data/import/stock.csv')
    build_concept('data/stock_concept_prep.csv', 'data/import/concept.csv')
    build_industry('data/stock_industry_prep.csv', 'data/import/industry.csv')

    build_executive_stock('data/executive_prep.csv', 'data/import/executive_stock.csv')
    build_stock_industry('data/stock_industry_prep.csv', 'data/import/stock_industry.csv')
    build_stock_concept('data/stock_concept_prep.csv', 'data/import/stock_concept.csv')

