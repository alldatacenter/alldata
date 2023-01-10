SELECT * FROM system.numbers_mt WHERE concat(materialize('1'), '...', toString(number)) = '1...10000000' LIMIT 1
