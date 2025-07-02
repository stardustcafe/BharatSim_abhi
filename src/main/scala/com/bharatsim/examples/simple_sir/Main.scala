package com.bharatsim.examples.simple_sir

import java.util.Date

import com.bharatsim.engine.ContextBuilder._
import com.bharatsim.engine._
import com.bharatsim.engine.actions.StopSimulation
import com.bharatsim.engine.basicConversions.decoders.DefaultDecoders._
import com.bharatsim.engine.basicConversions.encoders.DefaultEncoders._
import com.bharatsim.engine.execution.Simulation
import com.bharatsim.engine.graph.patternMatcher.MatchCondition._
import com.bharatsim.engine.listeners.{CsvOutputGenerator, SimulationListenerRegistry}
import com.bharatsim.engine.utils.Probability.biasedCoinToss
import com.bharatsim.examples.simple_sir.InfectionStatus._
import com.bharatsim.examples.simple_sir.Parameters._
import com.typesafe.scalalogging.LazyLogging

object Main extends LazyLogging {

  def main(args: Array[String]): Unit = {
    var beforeCount = 0
    val simulation = Simulation()

    // Ingest synthetic population data
    simulation.ingestData(implicit context => {
      createSyntheticPopulation(context)
      logger.debug("Ingestion done")
    })

    // Define simulation
    simulation.defineSimulation(implicit context => {
      // Stop simulation when no infected individuals remain or after 200 steps
      registerAction(
        StopSimulation,
        (c: Context) => {
          getInfectedCount(c) == 0 || c.getCurrentStep >= 200
        }
      )

      beforeCount = getInfectedCount(context)

      registerAgent[Person]

      val currentTime = new Date().getTime

      SimulationListenerRegistry.register(
        new CsvOutputGenerator("src/main/resources/sir_output_" + currentTime + ".csv", new SIROutputSpec(context))
      )
    })

    // On simulation completion
    simulation.onCompleteSimulation { implicit context =>
      printStats(beforeCount)
      teardown()
    }

    val startTime = System.currentTimeMillis()
    simulation.run()
    val endTime = System.currentTimeMillis()
    logger.info("Total time: {} s", (endTime - startTime) / 1000)
  }

  // Create synthetic population - just agents, no networks
  private def createSyntheticPopulation(context: Context): Unit = {
    for (i <- 1 to populationSize) {
      val citizenId = i.toLong
      val initialInfectionState = if (biasedCoinToss(initialInfectedFraction)) "Infected" else "Susceptible"

      // Create node directly using graph provider
      context.graphProvider.createNode("Person",
        ("id", citizenId),
        ("infectionState", initialInfectionState)
      )
    }
  }

  // Helper functions
  private def getInfectedCount(context: Context): Int = {
    context.graphProvider.fetchCount("Person", "infectionState" equ Infected)
  }

  private def getSusceptibleCount(context: Context): Int = {
    context.graphProvider.fetchCount("Person", "infectionState" equ Susceptible)
  }

  private def getRecoveredCount(context: Context): Int = {
    context.graphProvider.fetchCount("Person", "infectionState" equ Recovered)
  }

  private def printStats(beforeCount: Int)(implicit context: Context): Unit = {
    val afterCountSusceptible = getSusceptibleCount(context)
    val afterCountInfected = getInfectedCount(context)
    val afterCountRecovered = getRecoveredCount(context)

    logger.info("Simulation completed")
    logger.info("Initial infected: {}", beforeCount)
    logger.info("Final susceptible: {}", afterCountSusceptible)
    logger.info("Final infected: {}", afterCountInfected)
    logger.info("Final recovered: {}", afterCountRecovered)
    logger.info("Total population: {}", afterCountSusceptible + afterCountInfected + afterCountRecovered)
  }
}
