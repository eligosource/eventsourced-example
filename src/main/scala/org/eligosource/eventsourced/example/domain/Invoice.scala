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
package org.eligosource.eventsourced.example.domain

import javax.xml.bind.annotation._
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter

import org.eligosource.eventsourced.example.domain.Adapter._
import org.eligosource.eventsourced.example.util.Binding._

import scalaz._
import Scalaz._

@XmlType(propOrder = Array("total", "sum"))
@XmlAccessorType(XmlAccessType.PROPERTY)
sealed abstract class Invoice {
  def id: String

  def version: Long
  def versionOption = if (version == -1L) None else Some(version)

  def items: List[InvoiceItem]
  def discount: BigDecimal

  def total: BigDecimal = sum - discount

  def sum: BigDecimal = items.foldLeft(BigDecimal(0)) {
    (sum, item) => sum + item.amount * item.count
  }

  @XmlElement
  @XmlJavaTypeAdapter(classOf[BigDecimalAdapter])
  def getTotal = total

  @XmlElement
  @XmlJavaTypeAdapter(classOf[BigDecimalAdapter])
  def getSum = sum
}

object Invoice {
  def create(id: String): DomainValidation[DraftInvoice] = DraftInvoice(id, version = 0L).success
}

@XmlRootElement(name = "draft-invoice")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = Array("discount", "items"))
case class DraftInvoice(
  @xmlAttribute(required = true) id: String,
  @xmlAttribute(required = false) version: Long = -1,
  @xmlJavaTypeAdapter(classOf[InvoiceItemsAdapter]) items: List[InvoiceItem] = Nil,
  @xmlJavaTypeAdapter(classOf[BigDecimalAdapter]) discount: BigDecimal = 0) extends Invoice {

  private def this() = this(id = null) // needed by JAXB

  def addItem(item: InvoiceItem): DomainValidation[DraftInvoice] =
    copy(version = version + 1, items = items :+ item).success

  def setDiscount(discount: BigDecimal): DomainValidation[DraftInvoice] =
    if (sum <= 100) DomainError("discount only on orders with sum > 100").fail
    else copy(version = version + 1, discount = discount).success

  def sendTo(address: InvoiceAddress): DomainValidation[SentInvoice] =
    if (items.isEmpty) DomainError("cannot send empty invoice").fail
    else SentInvoice(id, version + 1, items, discount, address).success
}

@XmlRootElement(name = "sent-invoice")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = Array("discount", "items", "address"))
case class SentInvoice(
  @xmlAttribute(required = true) id: String,
  @xmlAttribute(required = false) version: Long = -1,
  @xmlJavaTypeAdapter(classOf[InvoiceItemsAdapter]) items: List[InvoiceItem] = Nil,
  @xmlJavaTypeAdapter(classOf[BigDecimalAdapter]) discount: BigDecimal = 0,
  @xmlElement(required = true) address: InvoiceAddress) extends Invoice {

  private def this() = this(id = null, address = null) // needed by JAXB

  def pay(amount: BigDecimal): DomainValidation[PaidInvoice] =
    if (amount < total) DomainError("paid amount less than total amount").fail
    else                PaidInvoice(id, version + 1, items, discount, address).success
}

@XmlRootElement(name = "paid-invoice")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = Array("discount", "items", "address"))
case class PaidInvoice(
  @xmlAttribute(required = true) id: String,
  @xmlAttribute(required = false) version: Long = -1,
  @xmlJavaTypeAdapter(classOf[InvoiceItemsAdapter]) items: List[InvoiceItem] = Nil,
  @xmlJavaTypeAdapter(classOf[BigDecimalAdapter]) discount: BigDecimal = 0,
  @xmlElement(required = true) address: InvoiceAddress) extends Invoice {

  private def this() = this(id = null, address = null) // needed by JAXB

  def paid = true
}

@XmlRootElement(name = "item")
@XmlAccessorType(XmlAccessType.FIELD)
case class InvoiceItem(
  @xmlElement(required = true) description: String,
  @xmlElement(required = true) count: Int,
  @xmlElement(required = true) @xmlJavaTypeAdapter(classOf[BigDecimalAdapter]) amount: BigDecimal) {

  private def this() = this(null, 0, 0)
}

/**
 * Needed to support conditional updates via XML/JSON Web API,
 */
@XmlRootElement(name = "item")
@XmlAccessorType(XmlAccessType.FIELD)
case class InvoiceItemVersioned(
  @xmlElement(required = true) description: String,
  @xmlElement(required = true) count: Int,
  @xmlElement(required = true) @xmlJavaTypeAdapter(classOf[BigDecimalAdapter]) amount: BigDecimal,
  @xmlAttribute(required = false, name = "invoice-version") invoiceVersion: Long = -1) {

  private def this() = this(null, 0, 0)

  def toInvoiceItem = InvoiceItem(description, count, amount)
  def invoiceVersionOption = if(invoiceVersion == -1L) None else Some(invoiceVersion)
}

@XmlElement
@XmlAccessorType(XmlAccessType.FIELD)
case class InvoiceAddress(
  @xmlElement(required = true) name: String,
  @xmlElement(required = true) street: String,
  @xmlElement(required = true) city: String,
  @xmlElement(required = true) country: String) {

  private def this() = this(null, null, null, null)
}

// Events
case class InvoiceCreated(invoiceId: String)
case class InvoiceItemAdded(invoiceId: String, item: InvoiceItem)
case class InvoiceDiscountSet(invoiceId: String, discount: BigDecimal)
case class InvoiceSent(invoiceId: String, invoice: Invoice, to: InvoiceAddress)
case class InvoicePaid(invoiceId: String)

// Commands
case class CreateInvoice(invoiceId: String)