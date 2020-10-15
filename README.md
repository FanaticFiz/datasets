### Консольная тула для datasets

Собрать `mvn package`

Запустить с настройками по умолчанию: `java -jar datasets.jar`

#### Переопределение настроек

##### Указать новый файл  
`java -jar datasets.jar --spring.config.location=classpath:/another-location/some-name.yml`

##### Заменить одно/несколько из свойств, например:  
укажем иной исходный spring.datasource.url и целевой crg.datasource.target.url

`java -jar datasets.jar --spring.datasource.url=jdbc:postgresql://10.10.10.201:5434/crg_gis_service --crg.datasource.target.url=jdbc:postgresql://10.10.10.201:5434/`

