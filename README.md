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