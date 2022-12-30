| 16gmaster                      | port | ip             |
|--------------------------------| ---- | -------------- |
| eladmin-system                 | 8613 | 16gmaster  |
| datax-config                   | 8611 | 16gmaster  |
| data-market-service      | 8822 | 16gmaster  |
| datax-service-data-integration | 8824 | 16gmaster  |
| data-metadata-service    | 8820 | 16gmaster  |

| 16gslave                      | port | ip             |
|-------------------------------| ---- | -------------- |
| datax-eureka                  | 8610 | 16gslave    |
| datax-gateway                 | 9538 | 16gslave    |
| datax-service-workflow        | 8814 | 16gslave    |
| data-metadata-service-console    | 8821 | 16gslave    |
| datax-service-data-mapping    | 8823 | 16gslave    |
| data-masterdata-service | 8828 | 16gslave    |
| data-quality-service    | 8826 | 16gslave    |

| 16gdata               | port | ip             |
|-----------------------| ---- | -------------- |
| data-standard-service | 8825 | 16gdata |
| data-visual-service   | 8827 | 16gdata |
| email-service         | 8812 | 16gdata |
| file-service          | 8811 | 16gdata |
| quartz-service        | 8813 | 16gdata |
| system-service        | 8810 | 16gdata |
| datax-tool-monitor    | 8711 | 16gdata |


#### 启动顺序

> 1、启动eureka
> 
> 2、启动config
>
> 3、启动gateway
>
> 4、启动masterdata
>
> 5、启动metadata
>
> 6、启动其他Jar
