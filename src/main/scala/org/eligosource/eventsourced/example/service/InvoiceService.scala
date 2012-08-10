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

import scala.concurrent.stm.Ref

import akka.actor._
import akka.pattern.ask
import akka.dispatch._
import akka.util.duration._
import akka.util.Timeout

import org.eligosource.eventsourced.core._
import org.eligosource.eventsourced.example.domain._

import scalaz._
import Scalaz._

class InvoiceProcessor(invoicesRef: Ref[Map[String, Invoice]], outputChannels: Map[String, ActorRef]) extends Actor {
  def receive = {
    case msg: Message => msg.event match {
      case CreateInvoice(id) => updateInvoicesAndReply(createInvoice(id), msg.sender)
    }
  }

  def createInvoice(invoiceId: String): DomainValidation[DraftInvoice] = {
    readInvoices.get(invoiceId) match {
      case Some(invoice) => DomainError("invoice %s: already exists" format invoiceId).fail
      case None          => Invoice.create(invoiceId)
    }
  }

  private def updateInvoicesAndReply(validation: DomainValidation[Invoice], sender: Option[ActorRef]) {
    validation.foreach(updateInvoices)
    sender.foreach(_ ! validation)
  }

  private def updateInvoices(invoice: Invoice) =
    invoicesRef.single.transform(invoices => invoices + (invoice.id -> invoice))

  private def readInvoices =
    invoicesRef.single.get
}

class InvoiceService(invoicesRef: Ref[Map[String, Invoice]], invoiceComponent: Component) {
  import InvoiceService._

  //
  // Consistent reads
  //

  def getInvoicesMap = invoicesRef.single.get
  def getInvoice(invoiceId: String): Option[Invoice] = getInvoicesMap.get(invoiceId)
  def getInvoices: Iterable[Invoice] = getInvoicesMap.values

  def getDraftInvoices = getInvoices.filter(_.isInstanceOf[DraftInvoice])
  def getSentInvoices = getInvoices.filter(_.isInstanceOf[SentInvoice])
  def getPaidInvoices = getInvoices.filter(_.isInstanceOf[PaidInvoice])

  //
  // Updates
  //

  implicit val timeout = Timeout(5 seconds)

  def createInvoice(invoiceId: String): Future[DomainValidation[DraftInvoice]] =
    invoiceComponent.inputProducer ? CreateInvoice(invoiceId) map(_.asInstanceOf[DomainValidation[DraftInvoice]])

  def addInvoiceItem(invoiceId: String, expectedVersion: Option[Long], invoiceItem: InvoiceItem): Future[DomainValidation[DraftInvoice]] =
    throw new UnsupportedOperationException("coming soon")

  def setInvoiceDiscount(invoiceId: String, expectedVersion: Option[Long], discount: BigDecimal): Future[DomainValidation[DraftInvoice]] =
    throw new UnsupportedOperationException("coming soon")

  def sendInvoiceTo(invoiceId: String, expectedVersion: Option[Long], to: InvoiceAddress): Future[DomainValidation[SentInvoice]] =
    throw new UnsupportedOperationException("coming soon")

  def payInvoice(invoiceId: String, expectedVersion: Option[Long], amount: BigDecimal): Future[DomainValidation[PaidInvoice]] =
    throw new UnsupportedOperationException("coming soon")
}

object InvoiceService {
  private[service] def notDraftError(invoiceId: String) =
    DomainError("invoice %s: not a draft invoice" format invoiceId)

  private[service] def notSentError(invoiceId: String) =
    DomainError("invoice %s: not a sent invoice" format invoiceId)
}