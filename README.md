
# Building an aggregation server with consistency management and a RESTful API.

## Table of Contents

- ***Components overview***
- ***Features***
- ***Project Structure***
- ***Requirements***
    - _Software_
    - _Libraries_ 
- ***Notes***
- ***How to run***
- ***Test your code***
- ***Authors***

## Components overview

#### Aggregation Server: 
Obtains weather information through HTTP PUT requests, store it, and then uses HTTP GET requests to deliver the saved information in JSON format. For the first successful PUT, the server returns status 201; for subsequent updates, it returns status 200. It oversees a Lamport clock for event synchronisation and verifies inputs. By default, port 4567 is used by the server to process GET and PUT requests. The server removes expired data periodically (After 30 seconds).

#### Content Server:
It reads the weather data from file and converts it into JSON format also send it to the Aggregation Server using PUT request. It will read two parameters from the command line, first one is server name and port number and second is file location in your system.

#### GETClient 
It displays weather data in JSON format key-value pair one line at a time. It reads two command server name and port number.

#### Lamport clock
At every event in this system Lamport Clock will be updated or increment. It is very important for synchronization and coordination purposes.

## Features

- ***Lamport Clock Implementation:*** Both the Content Server and Aggregation Server must maintain a Lamport clock to synchronize events and manage distributed system consistency.
- ***Data Expiration:*** After 30 seconds expired data automatically removed.
- ***PUT & GET Operations:*** System supports GET operation for getting JSON format data and PUT requests for update it.
- ***Concurrent Client Handling:*** Server handle multiple clients allowing them to send PUT and GET requests.
- ***Error Handling:*** System supports error handling mechanism.

## Project Structure

DS-Assignment-2/

├── AggregationServer.java             
├── ContentServer.java                 
├── GETClient.java                     
├── LamportClock.java                 
├── AggregationServerTest.java         
├── ContentServerTest.java             
├── GETClientTest.java                 
│
├── lib/                               
│   ├── junit-4.13.2.jar               
│   ├── hamcrest-core-1.3.jar         
│   └── json-20210307.jar              
├── README.md                           
├── Changes.pdf                       
├── Design Sketch Assignment 2.pdf    
├── weather_1.txt                   
├── weather_2.txt                     
├── weather_3.txt   
├── weather_4.txt                        
├── weather_5.txt                     
├── weather_6.txt      
├── weather_7.txt     
              

## Requirements

### Software 

- Java 8 or later
- JUnit 4.13.2 for testing
- JSON Library for JSON handling (json-20210307.jar)

### Libraries

Make sure the following JAR files are added to your project classpath:

- junit-4.13.2.jar
- hamcrest-core-1.3.jar
- json-20210307.jar

You can find them in the lib folder.

## Notes

- ***JSON format data will be display in arbitary or unordered manner, this could be because JSON objects are inherently unordered collections of key-value pairs. Unlike lists or arrays, the order of key-value pairs in a JSON object is not guaranteed to be preserved across systems or processes.***

- ***In AggregationServer testing you will see bind exception, all test cases are passed successfully but the server is already running and it tries to connect server again and again.***

-***In ContentServer testing you will also see connection exception, all test cases are passed successfully but the port is already in use so that it shows connection exception.***

## How to run

(1) Compile all java files
```
javac -cp ".;lib/json-20210307.jar" AggregationServer.java ContentServer.java GETClient.java
```
(2) Run java files in order to get output

```
java -cp ".;lib/json-20210307.jar" AggregationServer
```
_Do not close AggregationServer terminal just open new terminal_

***(3) Take any weather file*** - if you want to see expired weather data remove functionality then please run command ***(3)*** by taking different weather files.

```
java -cp ".;lib/json-20210307.jar" ContentServer.java localhost 4567 weather_6.txt
```

```
java -cp ".;lib/json-20210307.jar" GETClient localhost 4567
```

## Test your code

_Before testing close all the terminal and open a new one_

#### AggregationServerTest

```
javac -cp ".;lib/json-20210307.jar;lib/junit-4.13.2.jar;lib/hamcrest-core-1.3.jar" AggregationServer.java AggregationServerTest.java
```
```
java -cp ".;lib/json-20210307.jar;lib/junit-4.13.2.jar;lib/hamcrest-core-1.3.jar" org.junit.runner.JUnitCore AggregationServerTest
```

#### ContentServerTest

```
javac -cp ".;lib/json-20210307.jar;lib/junit-4.13.2.jar;lib/hamcrest-core-1.3.jar" ContentServer.java ContentServerTest.java

```

```
java -cp ".;lib/json-20210307.jar;lib/junit-4.13.2.jar;lib/hamcrest-core-1.3.jar" org.junit.runner.JUnitCore ContentServerTest
```

#### GETClientTest
```
javac -cp ".;lib/junit-4.13.2.jar;lib/hamcrest-core-1.3.jar;lib/mockito-core-3.12.4.jar" GETClient.java GETClientTest.java
```

```
java -cp ".;lib/junit-4.13.2.jar;lib/hamcrest-core-1.3.jar" org.junit.runner.JUnitCore GETClientTest
```

## Authors

- Jaykumar Natubhai Parekh
- ID : a1945801
- Distributed System Assignment 2