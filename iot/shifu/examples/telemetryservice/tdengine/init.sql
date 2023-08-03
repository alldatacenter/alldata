Create database shifu;
Use shifu;
Create STable testTable (ts TIMESTAMP, rawData varchar(255)) TAGS (defaultTag varchar(255));
Create Table testSubTable Using testTable TAGS('Shifu');
Select * From testSubTable;