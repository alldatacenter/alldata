# Format Plugin for SPSS (SAV) Files
This format plugin enables Apache Drill to read and query Statistical Package for the Social Sciences 
(SPSS) (or Statistical Product and Service Solutions) data files. According 
to Wikipedia: (https://en.wikipedia.org/wiki/SPSS)
 ***
 SPSS is a widely used program for statistical analysis in social science. It is also used by market 
 researchers, health researchers, survey companies, government, education researchers, marketing 
 organizations, data miners, and others. The original SPSS manual (Nie, Bent & Hull, 1970) has been 
 described as one of "sociology's most influential books" for allowing ordinary researchers to do their 
 own statistical analysis. In addition to statistical analysis, data management (case selection, file 
 reshaping, creating derived data) and data documentation (a metadata dictionary is stored in the 
 datafile) are features of the base software.
 ***
 
## Configuration 
To configure Drill to read SPSS files, simply add the following code to the formats section of your 
file-based storage plugin.  This should happen automatically for the default
 `cp`, `dfs`, and `S3` storage plugins.
 
Other than the file extensions, there are no variables to configure.
 
```json
"spss": {         
  "type": "spss",
  "extensions": ["sav"]
 }
```

## Data Model
SPSS only supports two data types: Numeric and Strings.  Drill maps these to `DOUBLE` and `VARCHAR` 
respectively. However, for some numeric columns, SPSS maps these numbers to 
text, similar to an `enum` field in Java.
 
For instance, a field called `Survey` might have labels as shown below:
 
 <table>
    <tr>
        <th>Value</th>
        <th>Text</th>
    </tr>
    <tr>
        <td>1</td>
        <td>Yes</td>
    </tr>
    <tr>
        <td>2</td>
        <td>No</td>
    </tr>
    <tr>
        <td>99</td>
        <td>No Answer</td>
    </tr>
 </table>

For situations like this, Drill will create two columns. In the example above you would get a column 
called `Survey` which has the numeric value (1,2 or 99) as well as a column called `Survey_value` which 
will map the integer to the appropriate value. Thus, the results would look something like this:
 
 <table>
    <tr>
        <th>`Survey`</th>
        <th>`Survey_value`</th>
    </tr>
    <tr>
        <td>1</td>
        <td>Yes</td>
    </tr>
    <tr>
        <td>1</td>
        <td>Yes</td>
    </tr>
    <tr>
        <td>1</td>
        <td>Yes</td>
    </tr>
    <tr>
        <td>2</td>
        <td>No</td>
    </tr>
    <tr>
        <td>1</td>
        <td>Yes</td>
    </tr>
    <tr>
        <td>2</td>
        <td>No</td>
    </tr>
    <tr>
        <td>99</td>
        <td>No Answer</td>
    </tr>
 </table>
 