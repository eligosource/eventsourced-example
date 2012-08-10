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
package org.eligosource.eventsourced.example.util

import java.math.{BigDecimal => JBigDecimal}
import java.util.{ArrayList, List => JList}

import scala.annotation.target.field

import javax.xml.bind.annotation._
import javax.xml.bind.annotation.adapters._

object Binding {
  type xmlAnyElement      = XmlAnyElement @field
  type xmlAttribute       = XmlAttribute @field
  type xmlElement         = XmlElement @field
  type xmlElementRef      = XmlElementRef @field
  type xmlElementRefs     = XmlElementRefs @field
  type xmlElementWrapper  = XmlElementWrapper @field
  type xmlJavaTypeAdapter = XmlJavaTypeAdapter @field
  type xmlTransient       = XmlTransient @field

  class BigDecimalAdapter extends XmlAdapter[JBigDecimal, BigDecimal] {
    import BigDecimal.javaBigDecimal2bigDecimal
    def unmarshal(v: JBigDecimal): BigDecimal = v // implicit conversion
    def marshal(v: BigDecimal): JBigDecimal = v.underlying
  }

  abstract class AbstractListAdapter[A, B <: AbstractList[A]] extends XmlAdapter[B, List[A]] {
    import scala.collection.JavaConverters._

    def marshal(v: List[A]) = if (v == null) create(new ArrayList[A]) else create(v.asJava)
    def unmarshal(v: B) = v.elem.asScala.toList
    def create(l: JList[A]): B
  }

  trait AbstractList[A] {
    def elem: JList[A]
  }
}