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

import java.util.{List => JList}
import javax.xml.bind.annotation._

import scala.collection.JavaConverters._

import org.eligosource.eventsourced.example.util.Binding._

object Adapter {
  class InvoiceItemsAdapter extends AbstractListAdapter[InvoiceItem, InvoiceItems] {
    def create(l: JList[InvoiceItem]) = new InvoiceItems(l)
  }

  @XmlRootElement(name = "items")
  @XmlAccessorType(XmlAccessType.FIELD)
  case class InvoiceItems(@xmlElementRef(name = "items") elem: JList[InvoiceItem]) extends AbstractList[InvoiceItem] {
    def this() = this(null)
  }

  @XmlRootElement(name = "invoices")
  @XmlAccessorType(XmlAccessType.FIELD)
  case class Invoices(@xmlElementRef(name = "invoices") elem: JList[Invoice]) extends AbstractList[Invoice] {
    def this() = this(null)
  }

  object InvoiceItems {
    def apply(l: Iterable[InvoiceItem]) = new InvoiceItems(l.toList.asJava)
  }

  object Invoices {
    def apply(l: Iterable[Invoice]) = new Invoices(l.toList.asJava)
  }
}