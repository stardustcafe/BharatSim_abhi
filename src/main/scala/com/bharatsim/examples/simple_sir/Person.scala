package com.bharatsim.examples.simple_sir

import com.bharatsim.engine.Context
import com.bharatsim.engine.basicConversions.decoders.DefaultDecoders._
import com.bharatsim.engine.basicConversions.encoders.DefaultEncoders._
import com.bharatsim.engine.graph.GraphNode
import com.bharatsim.engine.models.{Agent, Node}
import com.bharatsim.engine.utils.Probability.toss
import com.bharatsim.examples.simple_sir.Parameters._
import com.bharatsim.examples.simple_sir.InfectionStatus._

case class Person(id: Long, age: Int, infectionState: InfectionStatus, daysInfected: Int) extends Agent {

  // Behavior to increment days infected for infected individuals
  private val incrementInfectedDuration: Context => Unit = (context: Context) => {
    if (isInfected && context.getCurrentStep % numberOfTicksInADay == 0) {
      updateParam("daysInfected", daysInfected + 1)
    }
  }

  // Behavior to check for new infections
  private val checkForInfection: Context => Unit = (context: Context) => {
    if (isSusceptible) {
      val infectionRate = beta * dt

      val schedule = context.fetchScheduleFor(this).get
      val currentStep = context.getCurrentStep
      val placeType: String = schedule.getForStep(currentStep)

      val places = getConnections(getRelation(placeType).get).toList
      if (places.nonEmpty) {
        val place = places.head
        val decodedPlace = decodeNode(placeType, place)

        val infectedNeighbourCount = decodedPlace
          .getConnections(decodedPlace.getRelation[Person]().get)
          .count(x => x.as[Person].isInfected)

        val toBeInfected = toss(infectionRate, infectedNeighbourCount)

        if (toBeInfected) {
          updateParam("infectionState", Infected)
          updateParam("daysInfected", 0)
        }
      }
    }
  }

  // Behavior to check for recovery
  private val checkForRecovery: Context => Unit = (context: Context) => {
    if (isInfected && toss(gamma * dt, 1)) {
      updateParam("infectionState", Recovered)
    }
  }

  // Helper methods to check infection status
  def isSusceptible: Boolean = infectionState == Susceptible
  def isInfected: Boolean = infectionState == Infected
  def isRecovered: Boolean = infectionState == Recovered

  // Decode network nodes
  private def decodeNode(classType: String, node: GraphNode): Node = {
    classType match {
      case "House" => node.as[House]
    }
  }

  // Add behaviors to the agent
  addBehaviour(incrementInfectedDuration)
  addBehaviour(checkForInfection)
  addBehaviour(checkForRecovery)

  // Add relations
  addRelation[House]("STAYS_AT")
}
