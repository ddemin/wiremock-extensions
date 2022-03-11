# wiremock-extensions
Collection wiremock extensions (matchers, transformers)
## Transformers
### com.github.ddemin.wext.AsyncStateResponseTemplateTransformer
   Can switch response states using defined delay (in milliseconds) between steps
#### Configuration example
```json
TODO
```
### com.github.ddemin.wext.StatisticDelayedResponseTemplateTransformer
   Can perform delay with specific value statistic (in milliseconds)
#### Configuration example
```json
{
  "transformerParameters": {
    "dynamic-delay": {
      "delayMin": 357,
      "delay50": 357,
      "delay90": 357,
      "delay95": 357,
      "delayMax": 666,
      "parameters": {}
    }
  },
  "transformers": [
    "delayed-response-template"
  ]
}
```
   
##How To Build
1) Make sure that you have Java 11 and above (you can check it using ```java --version```)
2) Make sure that you have access to the Internet
3) Execute ```./gradlew clean test shadowJar```

##How To Install (WireMock standalone)
1) Copy ```wext.jar``` from ```wext/build/libs``` to your wiremock folder
2) To use specific transformer - please use one of instruction above
3) Launch standalone wiremock using classpath and ```--extension``` flag:
```bash
java -cp "wext.jar;wiremock-jre8-standalone-2.32.0.jar" com.github.tomakehurst.wiremock.standalone.WireMockServerRunner --extensions com.github.ddemin.wext.StatisticDelayedResponseTemplateTransformer,com.github.ddemin.wext.AsyncStateResponseTemplateTransformer 
```
