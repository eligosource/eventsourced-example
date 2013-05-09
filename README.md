Overview
--------

This project demonstrates how to implement an [event-sourced](http://martinfowler.com/eaaDev/EventSourcing.html) web application based on the Eligosource [Eventsourced](https://github.com/eligosource/eventsourced) library. It is based on [former work](https://github.com/krasserm/eventsourcing-example) that has been described in the articles

- [Building an Event-Sourced Web Application - Part 1: Domain Model, Events and State](http://krasserm.blogspot.com/2011/11/building-event-sourced-web-application.html)
- [Building an Event-Sourced Web Application - Part 2: Projections, Persistence, Consumers and Web Interface](http://krasserm.blogspot.com/2012/01/building-event-sourced-web-application.html)

Compared to the [old implementation](https://github.com/krasserm/eventsourcing-example), the whole service and persistence layer have been re-written and the domain events are now decoupled from the immutable domain model. The web UI and the web API remain unchanged.

The example web application uses the [Eventsourced](https://github.com/eligosource/eventsourced) library for both event-sourcing and command-sourcing. Furthermore, it implements the [CQRS](http://martinfowler.com/bliki/CQRS.html) pattern and follows [domain-driven design](http://domaindrivendesign.org/resources/what_is_ddd) principles.

![Architecture](https://raw.github.com/eligosource/eventsourced-example/master/doc/images/architecture.png)

Legend:

![Legend](https://raw.github.com/eligosource/eventsourced/master/doc/images/legend.png)

- The [domain model](https://github.com/eligosource/eventsourced-example/blob/master/src/main/scala/org/eligosource/eventsourced/example/domain/Invoice.scala) is a rich, immutable domain model. It is annotated with [JAXB](http://jcp.org/en/jsr/detail?id=222) annotations for XML and JSON binding.
- The [service layer](https://github.com/eligosource/eventsourced-example/tree/master/src/main/scala/org/eligosource/eventsourced/example/service) implements the event-sourced and command-sourced actors (processors) together with service facades for asynchronous writes and synchronous, non-blocking reads of application state. 
  - Processors manage application state via [STM](http://nbronson.github.com/scala-stm/) references.
  - For reading current state, services facades access STM references directly (concurrent reads).
  - For updating current state, services facades access STM references via processors where a single STM reference is updated by a single processor ([single-writer-principle](http://mechanical-sympathy.blogspot.de/2011/09/single-writer-principle.html))
  - On the command-side of the implemented CQRS pattern is the [InvoiceService](https://github.com/eligosource/eventsourced-example/blob/master/src/main/scala/org/eligosource/eventsourced/example/service/InvoiceService.scala) that manages application state and serves consistent reads of invoices.
  - On the query side of the implemented CQRS pattern is the [StatisticsService](https://github.com/eligosource/eventsourced-example/blob/master/src/main/scala/org/eligosource/eventsourced/example/service/StatisticsService.scala), a read model that serves eventually consistent reads about invoice update statistics.
  - The [PaymentProcess](https://github.com/eligosource/eventsourced-example/blob/master/src/main/scala/org/eligosource/eventsourced/example/service/PaymentGateway.scala) is an example of a stateful, potentially long-running and event-sourced business process.
- The [web layer](https://github.com/eligosource/eventsourced-example/tree/master/src/main/scala/org/eligosource/eventsourced/example/web) provides a RESTful service interface to application resources and supports HTML, XML and JSON representation formats. It is built on top of [Jersey](http://jersey.java.net/) and [Scalate](http://scalate.fusesource.org/). A [Play](http://www.playframework.org/)-based version will follow (which supports asynchronous responses in contrast to Jersey).
- The [application configuration](https://github.com/eligosource/eventsourced-example/blob/master/src/main/scala/org/eligosource/eventsourced/example/server/Appserver.scala) wires the pieces together.

Build
-----

Clone the example application and compile it

    git clone git://github.com/eligosource/eventsourced-example.git
    cd eventsourced-example
    sbt compile

Run
---

To start the example application run

    sbt 'run-main org.eligosource.eventsourced.example.server.Webserver'

Then go to [http://localhost:8080](http://localhost:8080) and create some invoices. 

… 

The example application's RESTful service interface supports HTML, XML and JSON representation formats. Content negotiation is done via the `Accept` HTTP header. The following examples show how to get HTML and XML representations for `invoice-3`.

### HTML

Click on [http://localhost:8080/invoice/invoice-3](http://localhost:8080/invoice/invoice-3). Provided you have created an invoice with id `invoice-3` before, you should see something like

![invoice-3](https://github.com/krasserm/eventsourcing-example/raw/master/doc/images/invoice-3.png)

### XML

    curl -H "Accept: application/xml" http://localhost:8080/invoice/invoice-3

yields

    <draft-invoice id="invoice-3" version="2">
        <total>12.8</total>
        <sum>12.8</sum>
        <discount>0</discount>
        <items>
            <item>
                <description>item-1</description>
                <count>1</count>
                <amount>4.1</amount>
            </item>
            <item>
                <description>item-2</description>
                <count>3</count>
                <amount>2.9</amount>
            </item>
        </items>
    </draft-invoice>
