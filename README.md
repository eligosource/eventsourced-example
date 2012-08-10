Info
----

This project re-implements [eventsourcing-example](https://github.com/krasserm/eventsourcing-example) based on the Eligosource [Eventsourced](https://github.com/eligosource/eventsourced) library. *This is currently work in progress. At the moment, only invoice creation and reading works. Stay tuned for more to come soon ...*



Run
---

    sbt 'run-nobootcp org.eligosource.eventsourced.example.server.Webserver'

Then go to [http://localhost:8080](http://localhost:8080) and create some invoices.

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
