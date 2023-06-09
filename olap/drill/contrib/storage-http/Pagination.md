# Auto Pagination in Drill
Remote APIs frequently implement some sort of pagination as a way of limiting results.  However, if you are performing bulk data analysis, it is necessary to reassemble the 
data into one larger dataset.  Drill's auto-pagination features allow this to happen in the background, so that the user will get clean data back.

To use a paginator, you simply have to configure the paginator in the connection for the particular API.  

## Words of Caution
While extremely powerful, the auto-pagination feature has the potential to run afoul of APIs rate limits and even potentially DDOS an API. 


## Offset Pagination
Offset Pagination uses commands similar to SQL which has a `LIMIT` and an `OFFSET`.  With an offset paginator, let's say you want 200 records and the  page size is 50 records, the offset paginator will break up your query into 4 requests as shown below:

* myapi.com?limit=50&offset=0
* myapi.com?limit=50?offset=50
* myapi.com?limit=50&offset=100
* myapi.com?limit=50&offset=150

### Configuring Offset Pagination
To configure an offset paginator, simply add the following to the configuration for your connection. 

```json
"paginator": {
   "limitParam": "<limit>",
   "offsetParam": "<offset>",
   "pageSize": 100,
   "method": "OFFSET"
}
```

## Page Pagination
Page pagination is very similar to offset pagination except instead of using an `OFFSET` it uses a page number. 

```json
 "paginator": {
        "pageParam": "page",
        "pageSizeParam": "per_page",
        "pageSize": 100,
        "method": "PAGE"
      }
```
In either case, the `pageSize` parameter should be set to the maximum page size allowable by the API.  This will minimize the number of requests Drill is making.
