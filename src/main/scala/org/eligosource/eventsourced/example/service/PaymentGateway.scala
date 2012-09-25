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
package org.eligosource.eventsourced.example.service

import akka.actor._
import akka.dispatch.Future

import org.eligosource.eventsourced.core._
import org.eligosource.eventsourced.example.domain._

/**
 * Payment gateway that interacts with customer to request payment (mocked).
 */
class PaymentGateway(invoiceProcessor: ActorRef) extends Actor { this: Receiver =>
  implicit val executor = context.system.dispatcher

  def receive = {
    case InvoicePaymentRequested(invoiceId, amount, to) => {
      // payments may take several days to arrive ...
      Future { invoiceProcessor ! Message(InvoicePaymentReceived(invoiceId, amount)) }
    }
  }
}

/**
 * (Long-running) payment process.
 */
class PaymentProcess extends Actor { this: Emitter =>
  var pendingPayments = Map.empty[String, InvoicePaymentRequested]

  def receive = {
    case InvoiceSent(invoiceId, invoice, to) => {
      val outEvent = InvoicePaymentRequested(invoiceId, invoice.getTotal, to)
      emitter("payment") sendEvent outEvent
      pendingPayments = pendingPayments + (invoiceId -> outEvent)
    }
    case InvoicePaid(invoiceId) => {
      pendingPayments = pendingPayments - invoiceId
    }
  }
}