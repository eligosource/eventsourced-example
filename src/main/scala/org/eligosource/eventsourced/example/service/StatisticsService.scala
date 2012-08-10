package org.eligosource.eventsourced.example.service

import scala.concurrent.stm.Ref

import akka.actor.Actor

import org.eligosource.eventsourced.core._
import org.eligosource.eventsourced.example.domain._

class StatisticsProcessor(statisticsRef: Ref[Map[String, Int]]) extends Actor {
  def receive = {
  case msg: Message => msg.event match {
      case InvoiceItemAdded(id, _) => statisticsRef.single.transform { statistics =>
        statistics.get(id) match {
          case Some(count) => statistics + (id -> (count + 1))
          case None        => statistics + (id -> 1)
        }
      }
    }
  }
}

class StatisticsService(statisticsRef: Ref[Map[String, Int]]) {
  def statistics = statisticsRef.single.get
}
