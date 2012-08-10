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
package org.eligosource.eventsourced.example.web

import javax.annotation.Resource
import javax.ws.rs._
import javax.ws.rs.core.MediaType._

import akka.dispatch.Await
import akka.util.duration._

import scalaz._
import Scalaz._

import com.sun.jersey.api.representation.Form
import com.sun.jersey.api.view.Viewable

import org.springframework.stereotype.Component

import org.eligosource.eventsourced.example.domain._
import org.eligosource.eventsourced.example.domain.Adapter._
import org.eligosource.eventsourced.example.service.InvoiceService

// ----------------------------------------------------
//  Invoice resources (HTML/XML/JSON web interface)
// ----------------------------------------------------

// TODO: make all timeouts configurable

@Component
@Path("/invoice")
class InvoicesResource {
  @Resource
  var service: InvoiceService = _

  @POST
  @Consumes(Array(APPLICATION_FORM_URLENCODED))
  @Produces(Array(TEXT_HTML))
  def createInvoiceHtml(form: Form) = {
    val idForm = new InvoiceIdForm(form.toMap)
    val validation = for {
      invoiceId <- idForm.toInvoiceId
      invoice   <- Await.result(service.createInvoice(invoiceId), 5.seconds)
    } yield invoice
    validation match {
      case Success(di) =>
        sc201(new Viewable(webPath("Invoices"), InvoicesInfo(service.getInvoices)), di.id)
      case Failure(err) =>
        sc409(new Viewable(webPath("Invoices"), InvoicesInfo(service.getInvoices, err, idForm)))
    }
  }

  @POST
  @Consumes(Array(TEXT_XML, APPLICATION_XML, APPLICATION_JSON))
  @Produces(Array(TEXT_XML, APPLICATION_XML, APPLICATION_JSON))
  def createInvoiceXmlJson(invoice: DraftInvoice) = Await.result(service.createInvoice(invoice.id), 5.seconds) match {
    case Success(di)  => sc201(Invoices(service.getInvoices), di.id)
    case Failure(err) => sc409(AppError(err))
  }

  @GET
  @Produces(Array(TEXT_HTML))
  def invoicesHtml =
    sc200(new Viewable(webPath("Invoices"), InvoicesInfo(service.getInvoices)))

  @GET
  @Produces(Array(TEXT_XML, APPLICATION_XML, APPLICATION_JSON))
  def invoicesXmlJson =
    sc200(Invoices(service.getInvoices))

  @Path("{id}")
  def invoice(@PathParam("id") id: String) =
    new InvoiceResource(service.getInvoice(id), service)
}

class InvoiceResource(invoiceOption: Option[Invoice], service: InvoiceService) {
  @POST
  @Consumes(Array(APPLICATION_FORM_URLENCODED))
  @Produces(Array(TEXT_HTML))
  def sendInvoiceHtml(form: Form) = invoiceOption match {
    case None          => sc404(new Viewable(errorPath("404")))
    case Some(invoice) => {
      val addressForm = new InvoiceAddressForm(form.toMap)
      val validation = for {
        address <- addressForm.toInvoiceAddress
        updated <- Await.result(service.sendInvoiceTo(invoice.id, addressForm.versionOption, address), 5.seconds)
      } yield updated
      validation match {
        case Success(si)  => sc200(new Viewable(webPath("Invoice.sent"), InvoiceInfo(si)))
        case Failure(err) => service.getInvoice(invoice.id) match {
          case None =>
            sc404(new Viewable(errorPath("404")))
          case Some(refreshed) =>
            sc409(new Viewable(webPath("Invoice.draft"), InvoiceInfo(refreshed, err, addressForm)))
        }
      }
    }
  }

  @PUT
  @Consumes(Array(TEXT_XML, APPLICATION_XML, APPLICATION_JSON))
  @Produces(Array(TEXT_XML, APPLICATION_XML, APPLICATION_JSON))
  def sendInvoiceXmlJson(sentInvoice: SentInvoice) = invoiceOption match {
    case None          => sc404(SysError.NotFound)
    case Some(invoice) => Await.result(service.sendInvoiceTo(invoice.id, sentInvoice.versionOption, sentInvoice.address), 5.seconds) match {
      case Success(si)  => sc200(si)
      case Failure(err) => service.getInvoice(invoice.id) match {
        case None            => sc404(SysError.NotFound)
        case Some(refreshed) => sc409(AppError(err))
      }
    }
  }

  @GET
  @Produces(Array(TEXT_HTML))
  def invoiceHtml = invoiceOption match {
    case None          => sc404(new Viewable(errorPath("404")))
    case Some(invoice) => invoice match {
      case di: DraftInvoice => sc200(new Viewable(webPath("Invoice.draft"), InvoiceInfo(di)))
      case si: SentInvoice  => sc200(new Viewable(webPath("Invoice.sent"), InvoiceInfo(si)))
      case pi: PaidInvoice  => sc200(new Viewable(webPath("Invoice.paid"), InvoiceInfo(pi)))
    }
  }

  @GET
  @Produces(Array(TEXT_XML, APPLICATION_XML, APPLICATION_JSON))
  def invoiceXmlJson = invoiceOption match {
    case None          => sc404(SysError.NotFound)
    case Some(invoice) => sc200(invoice)
  }

  @Path("item")
  def items = new InvoiceItemsResource(invoiceOption, service, this)
}

class InvoiceItemsResource(invoiceOption: Option[Invoice], service: InvoiceService, parent: InvoiceResource) {
  @POST
  @Consumes(Array(APPLICATION_FORM_URLENCODED))
  @Produces(Array(TEXT_HTML))
  def addInvoiceItemHtml(form: Form) = invoiceOption match {
    case None          => sc404(new Viewable(errorPath("404")))
    case Some(invoice) => {
      val itemForm = new InvoiceItemForm(form.toMap)
      val validation = for {
        item    <- itemForm.toInvoiceItem
        updated <- Await.result(service.addInvoiceItem(invoice.id, itemForm.versionOption, item), 5.seconds)
      } yield updated
      validation match {
        case Success(di) =>
          sc201(new Viewable(webPath("Invoice.draft"), InvoiceInfo(di)), lastItemPath(di))
        case Failure(err) => service.getInvoice(invoice.id) match {
          case None =>
            sc404(new Viewable(errorPath("404")))
          case Some(refreshed) =>
            sc409(new Viewable(webPath("Invoice.draft"), InvoiceInfo(refreshed, err, itemForm)))
        }
      }
    }
  }

  @POST
  @Consumes(Array(TEXT_XML, APPLICATION_XML, APPLICATION_JSON))
  @Produces(Array(TEXT_XML, APPLICATION_XML, APPLICATION_JSON))
  def addInvoiceItemXmlJson(itemv: InvoiceItemVersioned) = invoiceOption match {
    case None          => sc404(SysError.NotFound)
    case Some(invoice) => Await.result(service.addInvoiceItem(invoice.id, itemv.invoiceVersionOption, itemv.toInvoiceItem), 5.seconds) match {
      case Success(di)  => sc201(InvoiceItems(di.items), lastItemPath(di))
      case Failure(err) => service.getInvoice(invoice.id) match {
        case None            => sc404(SysError.NotFound)
        case Some(refreshed) => sc409(AppError(err))
      }
    }
  }

  @GET
  @Produces(Array(TEXT_HTML))
  def invoiceItemsHtml = parent.invoiceHtml

  @GET
  @Produces(Array(TEXT_XML, APPLICATION_XML, APPLICATION_JSON))
  def invoiceItemsXmlJson = invoiceOption match {
    case None          => sc404(SysError.NotFound)
    case Some(invoice) => sc200(InvoiceItems(invoice.items))
  }

  @Path("{index}")
  def invoice(@PathParam("index") index: Int) = {
    val invoiceItemOption = for {
      invoice     <- invoiceOption
      invoiceItem <- item(invoice, index)
    } yield invoiceItem
    new InvoiceItemResource(invoiceOption, invoiceItemOption, service, this)
  }

  private def item(invoice: Invoice, index: Int): Option[InvoiceItem] =
    if (index < invoice.items.length) Some(invoice.items(index)) else None

  private def lastItemPath(invoice: Invoice): String =
    "%s" format invoice.items.length - 1
}

class InvoiceItemResource(invoiceOption: Option[Invoice], invoiceItemOption: Option[InvoiceItem], service: InvoiceService, parent: InvoiceItemsResource) {
  @GET
  @Produces(Array(TEXT_HTML))
  def invoiceHtml = invoiceItemOption match {
    case None    => sc404(new Viewable(errorPath("404")))
    case Some(_) => parent.invoiceItemsHtml
  }

  @GET
  @Produces(Array(TEXT_XML, APPLICATION_XML, APPLICATION_JSON))
  def invoiceXmlJson = invoiceItemOption match {
    case None          => sc404(SysError.NotFound)
    case Some(invoice) => sc200(invoice)
  }
}

// ----------------------------------------------------
//  Invoice infos (used by Scalate templates)
// ----------------------------------------------------

trait Info {
  def formOption: Option[InvoiceForm]
  def uncommitted(key: String) = formOption.map(_.get(key)) match {
    case Some(Some(value)) => value
    case _                 => ""
  }
}

case class InvoicesInfo(
  invoices: Iterable[Invoice],
  errorsOption:  Option[DomainError] = None,
  formOption:    Option[InvoiceForm] = None) extends Info {

  def invoicesSorted = invoices.toList.sortWith { (a1, a2) => a1.id < a2.id }
}

object InvoicesInfo {
  def apply(invoices: Iterable[Invoice], errors: DomainError, form: InvoiceForm): InvoicesInfo =
    new InvoicesInfo(invoices, Some(errors), Some(form))
}

case class InvoiceInfo(
  invoiceOption: Option[Invoice],
  errorsOption:  Option[DomainError] = None,
  formOption:    Option[InvoiceForm] = None) extends Info {

  def draftInvoiceOption = concreteInvoiceOption[DraftInvoice]
  def sentInvoiceOption = concreteInvoiceOption[SentInvoice]
  def paidInvoiceOption = concreteInvoiceOption[PaidInvoice]

  private def concreteInvoiceOption[A](implicit m: Manifest[A]): Option[A] = invoiceOption match {
    case Some(invoice) if (m.erasure.isInstance(invoice)) => Some(invoice.asInstanceOf[A])
    case _                                                => None
  }
}

object InvoiceInfo {
  def apply(invoice: Invoice): InvoiceInfo =
    new InvoiceInfo(Some(invoice))

  def apply(invoice: Invoice, errors: DomainError, form: InvoiceForm): InvoiceInfo =
    new InvoiceInfo(Some(invoice), Some(errors), Some(form))

  def status(invoice: Invoice) = invoice match {
    case _: DraftInvoice => "draft"
    case _: SentInvoice  => "sent"
    case _: PaidInvoice  => "paid"
  }
}

// ----------------------------------------------------
//  Invoice forms (used for input validation)
// ----------------------------------------------------

private[web] trait InvoiceForm {
  def data: Map[String, String]
  def versionOption = get("version").map(_.toLong)

  def apply(key: String) = data(key)
  def get(key: String) = data.get(key)
}

private[web] class InvoiceIdForm(val data: Map[String, String]) extends InvoiceForm {
  def toInvoiceId: DomainValidation[String] = data("id") match {
    case "" => Failure(DomainError("id must not be empty"))
    case id => Success(id)
  }
}

private[web] class InvoiceItemForm(val data: Map[String, String]) extends InvoiceForm {
  def toInvoiceItem: DomainValidation[InvoiceItem] =
    (description ⊛ count ⊛ amount) (InvoiceItem.apply)

  private def description: DomainValidation[String] = data("description") match {
    case "" => Failure(DomainError("description must not be empty"))
    case d  => Success(d)
  }

  private def count: DomainValidation[Int] = data("count") match {
    case "" => Failure(DomainError("count must not be empty"))
    case c  => try {
      Success(c.toInt)
    } catch {
      case e => Failure(DomainError("count must be an int"))
    }
  }

  private def amount: DomainValidation[BigDecimal] = data("amount") match {
    case "" => Failure(DomainError("amount must not be empty"))
    case a  => try {
      Success(BigDecimal(a))
    } catch {
      case e => Failure(DomainError("amount must be a number"))
    }
  }
}

private[web] class InvoiceAddressForm(val data: Map[String, String]) extends InvoiceForm {
  def toInvoiceAddress: DomainValidation[InvoiceAddress] =
    (name ⊛ street ⊛ city ⊛ country) (InvoiceAddress.apply)

  private def name: DomainValidation[String] = data("name") match {
    case "" => Failure(DomainError("name must not be empty"))
    case s  => Success(s)
  }

  private def street: DomainValidation[String] = data("street") match {
    case "" => Failure(DomainError("street must not be empty"))
    case s  => Success(s)
  }

  private def city: DomainValidation[String] = data("city") match {
    case "" => Failure(DomainError("city must not be empty"))
    case c  => Success(c)
  }

  private def country: DomainValidation[String] = data("country") match {
    case "" => Failure(DomainError("country must not be empty"))
    case c  => Success(c)
  }
}