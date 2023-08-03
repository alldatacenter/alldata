SELECT
    '{"a":"1","b":"2","c":"","d":"4"}' AS json,
    extractAll('{"a":"1","b":"2","c":"","d":"4"}', '"([^"]*)":') AS keys,
    extractAll('{"a":"1","b":"2","c":"","d":"4"}', ':"([^"]*)"') AS values;
