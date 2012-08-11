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

import java.util.{List => JList}
import javax.annotation.Resource
import javax.ws.rs._
import javax.ws.rs.core.MediaType._
import javax.xml.bind.annotation._

import com.sun.jersey.api.view.Viewable

import org.springframework.stereotype.Component

import org.eligosource.eventsourced.example.service.StatisticsService
import org.eligosource.eventsourced.example.util.Binding._

@Component
@Path("/statistics")
class StatisticsResource {
  @Resource
  var statisticsService: StatisticsService = _

  @GET
  @Produces(Array(TEXT_XML, APPLICATION_XML, APPLICATION_JSON))
  def statisticsXmlJson =
    sc200(Statistics(InvoiceUpdate(statisticsService)))

  @GET
  @Produces(Array(TEXT_HTML))
  def statisticsHtml =
    sc200(new Viewable(webPath("Statistics"), Statistics(InvoiceUpdate(statisticsService))))
}

@XmlRootElement(name = "statistics")
case class Statistics(invoiceUpdates: List[InvoiceUpdate]) {
  private def this() = this(null)

  def invoiceUpdatesSorted =
    invoiceUpdates.sortWith { (a1, a2) => a1.invoiceId < a2.invoiceId }

  @XmlElementRef
  @XmlElementWrapper(name = "invoice-updates")
  def getInvoiceUpdates: JList[InvoiceUpdate] = {
    import scala.collection.JavaConverters._
    invoiceUpdates.asJava
  }
}

@XmlRootElement(name = "invoice-update")
@XmlAccessorType(XmlAccessType.FIELD)
case class InvoiceUpdate(
  @xmlAttribute(name = "invoice-id") invoiceId: String,
  @xmlElement(name = "update-count") updateCount: Int) {

  private def this() = this(null, 0)
}

object InvoiceUpdate {
  def apply(statisticsService: StatisticsService): List[InvoiceUpdate] =
    statisticsService.statistics.map(kv => InvoiceUpdate(kv._1, kv._2)).toList
}