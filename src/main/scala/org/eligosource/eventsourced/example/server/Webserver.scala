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

import java.net.URL
import java.util.EnumSet

import javax.servlet._
import javax.servlet.http.HttpServlet
import javax.ws.rs.ext._
import javax.xml.bind.JAXBContext

import com.sun.jersey.api.json.{JSONConfiguration, JSONJAXBContext}
import com.sun.jersey.spi.spring.container.servlet.SpringServlet

import org.eclipse.jetty.server.{Server => JettyServer}
import org.eclipse.jetty.servlet._
import org.eclipse.jetty.util.resource.FileResource
import org.fusesource.scalate.servlet.TemplateEngineFilter
import org.springframework.web.context.ContextLoaderListener


object Webserver extends App {
  val server = new JettyServer(8080);
  val context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);

  val jerseyHolder = new FilterHolder(new SpringServlet)
  val scalateHolder = new FilterHolder(new TemplateEngineFilter)

  jerseyHolder.setInitParameter("com.sun.jersey.config.property.packages", "org.fusesource.scalate.console;org.eligosource.eventsourced.example.server")
  jerseyHolder.setInitParameter("com.sun.jersey.config.feature.Trace", "true")
  jerseyHolder.setInitParameter("com.sun.jersey.config.feature.Redirect", "true")
  jerseyHolder.setInitParameter("com.sun.jersey.config.feature.Formatted", "true")
  jerseyHolder.setInitParameter("com.sun.jersey.config.feature.ImplicitViewables", "true")

  context.setContextPath("/")
  context.setBaseResource(new FileResource(new URL("file:src/main/webapp")))
  context.setInitParameter("contextConfigLocation", "/WEB-INF/context.xml")
  context.addEventListener(new ContextLoaderListener)

  context.addFilter(jerseyHolder, "/*", EnumSet.noneOf(classOf[DispatcherType]))
  context.addFilter(scalateHolder, "/*", EnumSet.noneOf(classOf[DispatcherType]))
  context.addServlet(new ServletHolder(new HttpServlet {}), "/*")

  server.setHandler(context)
  server.start()
  server.join()

  class ApplicationInitializer extends ServletContextListener {
    def contextInitialized(sce: ServletContextEvent) { Appserver.boot() }
    def contextDestroyed(sce: ServletContextEvent) {}
  }

  @Provider
  class JaxbContextResolver extends ContextResolver[JAXBContext] {
    val paths = "org.eligosource.eventsourced.example.domain:org.eligosource.eventsourced.example.web"
    val config = JSONConfiguration.mapped().rootUnwrapping(false).build()
    val context = new JSONJAXBContext(config, paths)
    def getContext(clazz : Class[_]) = context
  }
}
