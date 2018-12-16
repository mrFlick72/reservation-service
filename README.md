#Reservation-Service

This repo show a extremely cool thing: make a relation database access in non blocking io way embracing the reactive programming paradigm
starting from web layer to data access layer.

## Why consider no-blocking IO
To day the most common use case involve the classic one thread per req  model. 
This model is typical for very famous web and application servers like: Apache httpd, Ngnix, Tomcat and so on. 
However when the load increase too much many this model can be not suitable. If the request per seconds are more then the available threads, we 
can see a decrease of performance or even a deny of service. Another approach that is emerging during these years involve a totally different model. 
Instead to have one thread per request, projects and products like NodeJS, Netty, AKKA embrace the model of event loop and actor model. 
The problem here is that we have take care of never block our pipeline due to we have only one thread per event loop and if we block our execution we will block anything.
However the history show that this model scale very well in high load use cases infact 
we have more lightweight server that consume less resources and use those resources in a very more optimized way. 
A very beautiful metaphor here is the music. In a concert in order to have a good amplification system it is vital that all the pipeline is of high quality. 
If we have a good speaker and microphone but bed sound compressor the result will be less optimal then if we would have even a good sound compressor. 
Even in a no blocking pipeline having all the pieces of the software using no blocking paradigm is very important because we can't permit to have some blocking io software. 
Some framework like RXJava2, Reactror, Akka Stream and many other embrace reactive programming paradigm that provide a event loop paradigm that is very usefull in order to 
compose software pieces.

## The evil: JDBC
Unfortunally JDBC do not embrace the no blocking and reactive programming paradigm. This is a very big problem for use relation database in a reactive noblocking pipeline.
Some projects like rxjava-jdbc, ADBA, R2DBC an lo on attempt to solve the problem of the bloking nature of JDBC. In this sample I how use show R2DBC in a full reactive no blocking io pipeline 
starting from the web layer(Spring WebFlux) to database.


