package org.eligosource.eventsourced.example.service

import akka.actor._
import akka.dispatch.Future

import org.eligosource.eventsourced.core._
import org.eligosource.eventsourced.example.domain._

/**
 * Payment service that interacts with customer to request payment (mocked).
 */
class PaymentService(invoiceComponent: Component) extends Actor {
  implicit val executor = context.system.dispatcher

  def receive = {
    case msg: Message => msg.event match {
      case InvoicePaymentRequested(invoiceId, amount, to) => {
        // don't use a replay channel but acknowledge immediately
        sender ! Ack

        // because payments may take several days to arrive ...
        Future { invoiceComponent.inputChannel ! Message(InvoicePaymentReceived(invoiceId, amount)) }
      }
    }
  }
}

/**
 * (Long-running) payment process.
 */
class PaymentProcess(outputChannels: Map[String, ActorRef]) extends Actor {
  var pendingPayments = Map.empty[String, InvoicePaymentRequested]

  def receive = {
    case msg: Message => msg.event match {
      case InvoiceSent(invoiceId, invoice, to) => {
        val outEvent = InvoicePaymentRequested(invoiceId, invoice.getTotal, to)
        outputChannels("payment") ! msg.copy(event = outEvent)
        pendingPayments = pendingPayments + (invoiceId -> outEvent)
      }
      case InvoicePaid(invoiceId) => {
        pendingPayments = pendingPayments - invoiceId
      }
      case _ =>
    }
  }
}