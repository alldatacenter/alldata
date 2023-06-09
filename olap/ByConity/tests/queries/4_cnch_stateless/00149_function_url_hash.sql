select URLHash('') = URLHash(appendTrailingCharIfAbsent('', '/'));
select URLHash('http://ya.ru') = URLHash(appendTrailingCharIfAbsent('http://ya.ru', '/'));
select URLHash('http://ya.ru') = URLHash(appendTrailingCharIfAbsent('http://ya.ru', '?'));
select URLHash('http://ya.ru') = URLHash(appendTrailingCharIfAbsent('http://ya.ru', '#'));

select URLHash('', 0) = URLHash('');
select URLHash('', 1) = URLHash('');
select URLHash('', 1000) = URLHash('');

select URLHash('http://ya.ru/a', 0 ) = URLHash(URLHierarchy('http://ya.ru/a')[0 + 1]);
select URLHash('http://ya.ru/a', 1 ) = URLHash(URLHierarchy('http://ya.ru/a')[1 + 1]);

select URLHash(url, 0) = URLHash(URLHierarchy(url)[0 + 1]) from system.one array join ['', 'http://ya.ru', 'http://ya.ru/', 'http://ya.ru/a', 'http://ya.ru/a/', 'http://ya.ru/a/b', 'http://ya.ru/a/b?'] as url;
select URLHash(url, 1) = URLHash(URLHierarchy(url)[1 + 1]) from system.one array join ['', 'http://ya.ru', 'http://ya.ru/', 'http://ya.ru/a', 'http://ya.ru/a/', 'http://ya.ru/a/b', 'http://ya.ru/a/b?'] as url;
select URLHash(url, 2) = URLHash(URLHierarchy(url)[2 + 1]) from system.one array join ['', 'http://ya.ru', 'http://ya.ru/', 'http://ya.ru/a', 'http://ya.ru/a/', 'http://ya.ru/a/b', 'http://ya.ru/a/b?'] as url;
select URLHash(url, 3) = URLHash(URLHierarchy(url)[3 + 1]) from system.one array join ['', 'http://ya.ru', 'http://ya.ru/', 'http://ya.ru/a', 'http://ya.ru/a/', 'http://ya.ru/a/b', 'http://ya.ru/a/b?'] as url;
select URLHash(url, 4) = URLHash(URLHierarchy(url)[4 + 1]) from system.one array join ['', 'http://ya.ru', 'http://ya.ru/', 'http://ya.ru/a', 'http://ya.ru/a/', 'http://ya.ru/a/b', 'http://ya.ru/a/b?'] as url;
