/*
 * Copyright 2012 Eligotech BV.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eligosource.eventsourced.example.server

import scala.concurrent.stm.Ref

import akka.actor._

import org.eligosource.eventsourced.core._
import org.eligosource.eventsourced.journal.LeveldbJournal

import org.eligosource.eventsourced.example.domain.Invoice
import org.eligosource.eventsourced.example.service._

trait Appserver {
  val invoiceService: InvoiceService
  val statisticsService: StatisticsService
}

object Appserver {
  def boot(): Appserver = new Appserver {
    implicit val system = ActorSystem("eventsourced")

    val journalDir = new java.io.File("target/journal")
    val journal = system.actorOf(Props(new LeveldbJournal(journalDir)))

    val invoicesRef = Ref(Map.empty[String, Invoice])
    val statisticsRef = Ref(Map.empty[String, Int])

    val invoiceComponent = Component(1, journal)
    val listenersComponent = Component(2, journal)

    val invoiceService = new InvoiceService(invoicesRef, invoiceComponent)
    val statisticsService = new StatisticsService(statisticsRef)
    val paymentGateway = system.actorOf(Props(new PaymentGateway(invoiceComponent)))

    listenersComponent
      .addDefaultOutputChannelToActor("payment", paymentGateway)
      .setProcessors(outputChannels => List(
        system.actorOf(Props(new StatisticsProcessor(statisticsRef))),
        system.actorOf(Props(new PaymentProcess(outputChannels)))))

    invoiceComponent
      .addDefaultOutputChannelToComponent("listeners", listenersComponent)
      .setProcessor(outputChannels => system.actorOf(Props(new InvoiceProcessor(invoicesRef, outputChannels))))

    Composite.init(invoiceComponent)

  }
}