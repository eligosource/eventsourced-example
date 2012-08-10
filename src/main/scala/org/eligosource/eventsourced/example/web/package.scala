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
package org.eligosource.eventsourced.example

import javax.ws.rs.core._
import javax.ws.rs.core.Response.Status._
import javax.xml.bind.annotation._

import com.sun.jersey.api.representation.Form
import com.sun.jersey.api.view.Viewable

import org.eligosource.eventsourced.example.domain.DomainError

package object web {
  @XmlRootElement(name = "app-error")
  case class AppError(errors: DomainError) {
    import scala.collection.JavaConverters._

    def this() = this(null)

    @XmlElement
    def getMessage: java.util.List[String] = errors.asJava
  }

  @XmlRootElement(name = "sys-error")
  @XmlAccessorType(XmlAccessType.FIELD)
  case class SysError(message: String) {
    def this() = this(null)
  }

  object SysError {
    val NotFound = SysError(NOT_FOUND.getReasonPhrase)
  }

  val rootPath = "/org/eligosource/eventsourced/example"

  def errorPath(templateName: String) = "%s/error/%s" format (rootPath, templateName)
  def homePath(templateName: String) = "%s/home/%s" format (rootPath, templateName)
  def webPath(templateName: String) = "%s/web/%s" format (rootPath, templateName)

  def uri(path: String) = UriBuilder.fromPath(path).build()

  def sc200(entity: AnyRef) = Response.ok(entity).build()
  def sc201(entity: AnyRef, path: String) = Response.created(uri(path)).entity(entity).build()
  def sc404(entity: AnyRef) = Response.status(NOT_FOUND).entity(entity).build()
  def sc409(entity: AnyRef) = Response.status(CONFLICT).entity(entity).build()

  class RichForm(form: Form) {
    import scala.collection.JavaConverters._

    def toMap: Map[String, String] =
      form.asScala.foldLeft(Map.empty[String, String]) { (m, kv) => m + (kv._1 -> kv._2.get(0)) }
  }

  implicit def form2RichForm(form: Form) = new RichForm(form)
}