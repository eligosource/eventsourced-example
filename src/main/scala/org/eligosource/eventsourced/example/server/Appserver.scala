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

import java.io.File

import akka.actor._
import akka.util.duration._
import akka.util.Timeout

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
    implicit val timeout = Timeout(10 seconds)

    val journal = LeveldbJournal(new File("target/journal"))
    val extension = EventsourcingExtension(system, journal)

    val invoicesRef = Ref(Map.empty[String, Invoice])
    val statisticsRef = Ref(Map.empty[String, Int])

    val multicastTargets = List(
      system.actorOf(Props(new StatisticsProcessor(statisticsRef) with Receiver)),
      system.actorOf(Props(new PaymentProcess with Emitter)))

    val invoiceProcessor = extension.processorOf(Props(new InvoiceProcessor(invoicesRef) with Emitter with Eventsourced { val id = 1 } ))
    val multicastProcessor = extension.processorOf(Props(new Multicast(multicastTargets, identity) with Confirm with Eventsourced { val id = 2 }))

    val paymentGateway = system.actorOf(Props(new PaymentGateway(invoiceProcessor) with Receiver with Confirm))

    extension.channelOf(DefaultChannelProps(1, paymentGateway).withName("payment"))
    extension.channelOf(DefaultChannelProps(2, multicastProcessor).withName("listeners"))

    extension.recover()
    // wait for processor 1 to complete processing of replayed event messages
    // (ensures that recovery of externally visible state maintained by
    //  invoicesRef is completed when awaitProcessorCompletion returns)
    extension.awaitProcessorCompletion(Set(1))

    val invoiceService = new InvoiceService(invoicesRef, invoiceProcessor)
    val statisticsService = new StatisticsService(statisticsRef)
  }
}