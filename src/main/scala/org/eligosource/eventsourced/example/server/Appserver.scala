package org.eligosource.eventsourced.example.server

import scala.concurrent.stm.Ref

import akka.actor._

import org.eligosource.eventsourced.core._
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
    val journal = system.actorOf(Props(new Journal(journalDir)))

    val invoicesRef = Ref(Map.empty[String, Invoice])
    val statisticsRef = Ref(Map.empty[String, Int])

    val listenersComponent = Component(2, journal)
      .setProcessor(outputChannels => system.actorOf(Props(new StatisticsProcessor(statisticsRef))))

    val invoiceComponent = Component(1, journal)
      .addDefaultOutputChannelToComponent("listeners", listenersComponent)
      .setProcessor(outputChannels => system.actorOf(Props(new InvoiceProcessor(invoicesRef, outputChannels))))

    Composite.init(invoiceComponent)

    val invoiceService = new InvoiceService(invoicesRef, invoiceComponent)
    val statisticsService = new StatisticsService(statisticsRef)
  }
}