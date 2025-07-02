package com.bharatsim.examples.simple_sir

import com.bharatsim.engine.Context
import com.bharatsim.engine.basicConversions.decoders.DefaultDecoders._
import com.bharatsim.engine.basicConversions.encoders.DefaultEncoders._
import com.bharatsim.engine.models.Agent
import com.bharatsim.engine.utils.Probability.toss
import com.bharatsim.examples.simple_sir.Parameters._
import com.bharatsim.examples.simple_sir.InfectionStatus._

case class Person(id: Long, infectionState: InfectionStatus) extends Agent {

  // Simple stochastic infection behavior - susceptible individuals become infected with probability beta
  private val checkForInfection: Context => Unit = (context: Context) => {
    if (isSusceptible && toss(beta, 1)) {
      updateParam("infectionState", Infected)
    }
  }

  // Simple stochastic recovery behavior - infected individuals recover with probability gamma
  private val checkForRecovery: Context => Unit = (context: Context) => {
    if (isInfected && toss(gamma, 1)) {
      updateParam("infectionState", Recovered)
    }
  }

  // Helper methods to check infection status
  def isSusceptible: Boolean = infectionState == Susceptible
  def isInfected: Boolean = infectionState == Infected
  def isRecovered: Boolean = infectionState == Recovered

  // Add behaviors to the agent
  addBehaviour(checkForInfection)
  addBehaviour(checkForRecovery)
}
