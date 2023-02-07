# Syslog Format Plugin
This format plugin enables Drill to query syslog formatted data as specified in RFC-5424, as shown below.

```
<165>1 2003-10-11T22:14:15.003Z mymachine.example.com evntslog - ID47 [exampleSDID@32473 iut="3" eventSource="Application" eventID="1011"][examplePriority@32473 class="high"]
```

## Configuration Options
This format pluin has the following configuration options:

* **`maxErrors`**: Sets the maximum number of malformatted lines that the format plugin will tolerate before throwing an error and halting execution
* **`flattenStructuredData`**: Syslog data optionally contains a series of key/value pairs known as the structured data.  By default, Drill will parse these into a `map`.

```
"syslog": {
   "type": "syslog",
   "extensions": [ "syslog" ],
   "maxErrors": 10,
   "flattenStructuredData": false
}
```

## Fields
Since the structure of the data contained in a syslog is well known.  In terms of data types, the `event_date` field is a datetime, the `severity_code`, `facility_code`, and `proc_id` are integers and all other fields are VARCHARs.

** Note:  All fields, with the exception of the `event_date`, are not required, so not all fields may be present at all times. **

* `event_date`: This is the time of the event
* `severity_code`:  The severity code of the event
* `facility_code`:  The facility code of the incident
* `severity`:  The severity of the event
* `facility`:
* `ip`:  The IP address or hostname of the source machine
* `app_name`:  The name of the application that is generating the event
* `proc_id`:  The process ID of the event that generated the event
* `msg_id`:  The identifier of the message
* `message`:  The actual message text of the event
* `_raw`:  The full text of the event

### Structured Data
Syslog data can contain a list of key/value pairs which Drill will extract in a field called `structured_data`.  This field is a Drill Map.