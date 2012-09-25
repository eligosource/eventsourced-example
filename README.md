Overview
--------

This project re-implements [eventsourcing-example](https://github.com/krasserm/eventsourcing-example) based on the Eligosource [Eventsourced](https://github.com/eligosource/eventsourced) library. Regarding library usage, it demonstrates how to 

- implement command-sourcing (for managing state of invoices)
- implement event-sourcing (for managing state of payment processes and for recording usage statistics)
- implement domain services (to handle requests via UI and XML/JSON API)
- implement business processes (to deal with long-running background activities)
- recover from crashes (incl. recovery of business processes)

Compared to the [old implementation](https://github.com/krasserm/eventsourcing-example), the whole service and persistence layer are re-written and domain events are now decoupled from the immutable domain model. The web UI and XML/JSON API remain unchanged and are built on top of [Jersey](http://jersey.java.net/), [Scalate](http://scalate.fusesource.org/) and [JAXB](http://jcp.org/en/jsr/detail?id=222). A [Play](http://www.playframework.org/)-based version will follow (which supports asynchronous responses in contrast to Jersey).

Architecture
------------

![Architecture](https://raw.github.com/eligosource/eventsourced-example/master/doc/images/invoice-example.png)

### State management

- Command and event-sourced processors manage state via [STM](http://en.wikipedia.org/wiki/Software_transactional_memory) references.
- For reading current state, services access STM references directly (concurrent reads).
- For updating current state, services access STM references via processors (actors) where a single STM reference is updated by a single processor ([single-writer-principle](http://mechanical-sympathy.blogspot.de/2011/09/single-writer-principle.html)).

Build
-----

First checkout, build and publish the [Eventsourced](https://github.com/eligosource/eventsourced) library to Ivy cache.

    git clone git://github.com/eligosource/eventsourced.git
    cd eventsourced
    sbt publish-local

Then checkout the example application and compile it

    git clone git://github.com/eligosource/eventsourced-example.git
    cd eventsourced-example
    sbt compile

Run
---

To start the example application enter

    sbt 'run-nobootcp org.eligosource.eventsourced.example.server.Webserver'

Finally go to [http://localhost:8080](http://localhost:8080) and create some invoices.

Web API
-------

The example application's RESTful service interface supports HTML, XML and JSON as representation formats. Content negotiation is done via the `Accept` HTTP header. The following examples show how to get different representations of `invoice-3`

### HTML

Enter [http://localhost:8080/invoice/invoice-3](http://localhost:8080/invoice/invoice-3) into your browser. Provided you have created an invoice with id `invoice-3` before you should see something like

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
