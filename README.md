#  health-actuator-aggregator

Library, welche die `/health` Endpoints von mehreren Systemen zusammenzieht und im `/health` Endpoint des aktuellen Systems darstellt.


### Hintergrund

In der Referenzarchitektur 4 ist vorgesehen, Anwendungen aus mehreren Spring Boot Containern zu erstellen. Diese Container behandeln dabei je einen separaten Aspekt des Gesamtsystems und sind lose über eine ServiceRegistry gekoppelt.   

### Dependencies

Diese Library hat neben der Abhängigkeit auf `spring-boot-actuator` auch eine Abhängigkeit auf `spring-cloud-starter-eureka`. Beide Abhängigkeiten werden transitiv nachgezogen.

### Übersicht

![Übersicht](src/doc/aggregator.png)

Im Rahmen einer Spring Cloud Applikation wird ein Spring Boot Container als Health Aggregator bestimmt. Auf dessen /health-Endpoint wird mit Hilfe dieser Library die Health-Informationen der anderen Services integriert.

### Verwendung 

Um den health-actuator-aggregator zu aktivieren, muss zuerst diese Library als dependency konfiguriert werden:

```xml
[...]
<dependency>
    <groupId>ejpd-spring-servicecheck</groupId>
    <artifactId>health-actuator-aggregator</artifactId>
    <version>0.1-SNAPSHOT</version>
</dependency>
[...]

```

Danach muss die Spring-Boot Applikation mit `@EnableServicesHealthAggregation` annotiert werden, damit der health aggregator aktiv wird:

```java
@SpringBootApplication
@EnableDiscoveryClient
@EnableServicesHealthAggregation
public class SomeSpringBootApp {
     public static void main(String[] args) {
            new SpringApplicationBuilder(SomeSpringBootApp.class)
                    .run(args);
        }
}

```


### Anzeige im Health-Endpoint

##### Hierarchie

Der health aggregator fügt dem eigentlichen /health- Endoint ein neues Objekt names "aggregatedServices" hinzu. AggregatedServices besteht aus einem Unter-Objekt pro Service, welcher überwacht werden soll. 

Dieses Service-Objekt besteht wiederum aus je einem Unter-Objekt pro Service-Instanz. 

Dieses Serivce-Instanz-Objekt beinhaltet die healthCheckURL der Instanz, sowie eine komplette Kopie der gesamten Health-Informationen der entsprechenden Instanz.

Die gesamte Hierarchie sieht also wie folgt aus:


* aggregatedServices
    * SERVICE_A
        * instance-0
    * SERVICE_B
        * instance-0
        * instance-1
        
In diesem Beispiel werden SERVICE_A und SERVICE_B überwacht, wobei ersterer eine, letzterer 2 Instanzen hat. 

##### Propagieren des Status

Ohne weitere Konfiguration (siehe Abschnitt Konfiguration) wird der Status wie folgt propagiert:

1. Eine Service-Instanz hat den Status, welcher vom /health Endpoint der Instanz zurückgegeben worden ist.
2. Ein Service hat **Status UP**, wenn **mindestens eine** Instanz Status UP hat. Sonst hat der Serivce **Status DOWN**.
3. aggregatedServices hat den schlimmsten Status aller Services. Damit hat dieser Abschnitt dasselbe Verhalten wie der [OrderedHealthAggregator](https://github.com/spring-projects/spring-boot/blob/v1.5.6.RELEASE/spring-boot-actuator/src/main/java/org/springframework/boot/actuate/health/OrderedHealthAggregator.java),
welcher Spring Boot per Default verwendet, um den Gesamtstatus aus allen Komponenten zu errechnen. 
            



### Konfiguration 

##### Service-Liste

Da der health aggregator die Informationen zu den konkreten Service-Instanzen aus einer Service-Registry bezieht, muss die Spring-Boot-Applikation, welche die Aggregierung machen soll, natürlich korrekt als DiscoveryClient konfiguriert sein. Als Beispiel dafür kann [example-spring-cloud](https://repo.isc-ejpd.admin.ch/stash/projects/EXAMPLES/repos/example-spring-cloud/browse) dienen.  

Um die Health-Information der Services zu integrieren ist es ausserdem nötig, die Service-Namen als Liste zu konfigurieren. Ohne diese Konfiguration kann der health aggregator allfällige Services, zu welchen es keine aktiven Instanzen gibt, nicht identifizieren und somit keine Aussage machen, ob das Gesamtsystem gesund ist.

Dazu definiert man in der Spring Boot Konfiguration (z.B. application.yml) folgendes:

```yml
healthaggregator:
  neededServices:
    - DATABASE-SERVICE
    - BATCH-SERVICE
    - APPLICATION-EDGE-SERVICE
    - CACHE-SERVICE
```

##### Mindestanzahl Instanzen / Service

Der Status eines Services setzt sich aus den Status seiner Instanzen zusammen: Er ist dann UP, wenn mindestens eine Instanz UP ist. 

In manchen Fällen ist es Sinnvoll, diesen Mechanismus zu übersteuern. Gründe dafür können sein:

- Ein bestimmter Service ist überhaupt nicht wichtig für das Gesamtsystem, und ein Ausfall aller Instanzen ist nicht weiter schlimm.
- Ein Service braucht mindestens n gesunde Instanzen, damit die User Experience nicht furchtbar schlecht wird.

Um solche Fälle abzudecken kann pro überwachter Service sogenannte `threshold`- Werte definiert werden. Dabei handelt es sich um eine Map zwischen einem Status und der Anzahl Instanzen. 
Als Key dieser Map wird der Status abgelegt, der Wert ist die **minimale** Anzahl Instanzen, welche zu diesem Status führen. 


Ein Beispiel:


```yml
healthaggregator:
  neededServices:
    - DATABASE-SERVICE
    - BATCH-SERVICE
    - APPLICATION-EDGE-SERVICE
    - CACHE-SERVICE
    thresholds:
        CACHE-SERVICE:
            UP: 0
        DATABASE-SERVICE:
            OUT_OF_SERVICE: 1
            UP: 3       
        APPLICATION-EDGE-SERVICE:
            DOWN: 0
            UP: 1
            UNKNOWN: 2
          
```

In diesem Beispiel wird der CACHE-SERVICE auf UP bei mindestens 0 Instanzen gesetzt. Das heisst, bei jeder Anzahl Instanzen beeinträchtigt der CACHE-SERVICE den health check nicht.

Der DATABASE-SERVICE hingegen braucht mindestens 3 Instanzen, damit der Service UP ist. Bei 1 oder 2 Instanzen wäre dieser OUT_OF_SERVICE, bei 0 Instanzen DOWN.

Der APPLICATION-EDGE-SERVICE ist DOWN bei 0 Instanzen (dieser Eintrag hätte auch weggelassen werden können, da default), UP bei 1 und UNKNOWN bei 2 Instanzen. 


Das Implizite Default-Mapping aller Services ist also:

* DOWN: 0
* UP: 1


Widersprüchliche Konfigurationen (z.B. Zuordnen von 2 unterschiedlichen Status bei 1 Instanz) werden bereits beim Aufstarten des Spring Boot Containers mit einer Exception quittiert. 



##### Standortübergreifende Applikationnen - Registry Zone

Der Health Aggregator liest nur Services aus, welche in derselben Zone liegen wie er selbst. Wenn nun eine Applikation auf 2 Standorten verteilt ist, werden Service-Instanzen einer anderen Zone ignoriert, selbst wenn die Service-Registry repliziert ist und die anderen Instanzen sichtbar wären.
 

![Standortübergreifende_Zonen](src/doc/zones.png)


Die Zone, für welche der Health Aggregator zuständig ist, wird mit dem Property `eureka.instance.metadataMap.zone` festgelegt:

```yml
eureka;
  instance:
    metadataMap:
      zone: Zone1
```
Um zu entscheiden, ob eine zu prüfende Service-Instanz in derselben Zone ist wie der HealthAggregator selbst, wird die Metadata-Map der Service-Instanz ausgewertet und das Property "zone" ausgelesen. Die Services müssen also **genau gleich** konfiguriert sein (also auch einen Eintrag "zone" in der metadataMap haben) wie der HealthAggregator. 


Mit dem Property  `healthaggregator.registryzone` kann die Zone übersteuert werden: übersteuert werden:
 
 ```yml
 eureka;
   instance:
     metadataMap:
       zone: Zone1
 healthaggregator:
   registryzone: EineAndereZone      
 ```

  
