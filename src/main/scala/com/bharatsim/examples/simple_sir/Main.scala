package com.bharatsim.examples.simple_sir

import java.util.Date

import com.bharatsim.engine.ContextBuilder._
import com.bharatsim.engine._
import com.bharatsim.engine.actions.StopSimulation
import com.bharatsim.engine.basicConversions.decoders.DefaultDecoders._
import com.bharatsim.engine.basicConversions.encoders.DefaultEncoders._
import com.bharatsim.engine.dsl.SyntaxHelpers._
import com.bharatsim.engine.execution.Simulation
import com.bharatsim.engine.graph.ingestion.{GraphData, Relation}
import com.bharatsim.engine.graph.patternMatcher.MatchCondition._
import com.bharatsim.engine.listeners.{CsvOutputGenerator, SimulationListenerRegistry}
import com.bharatsim.engine.models.Agent
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
      createSyntheticPopulation()
      logger.debug("Ingestion done")
    })

    // Define simulation
    simulation.defineSimulation(implicit context => {
      createSchedules()

      // Stop simulation when no infected individuals remain
      registerAction(
        StopSimulation,
        (c: Context) => {
          getInfectedCount(c) == 0
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

  // Create simple schedules - everyone stays at home
  private def createSchedules()(implicit context: Context): Unit = {
    val homeSchedule = (myDay, myTick)
      .add[House](0, 0)

    registerSchedules(
      (homeSchedule, (agent: Agent, _: Context) => true, 1)
    )
  }

  // Create synthetic population
  private def createSyntheticPopulation()(implicit context: Context): Unit = {
    for (i <- 1 to populationSize) {
      val citizenId = i.toLong
      val age = 25 + (i % 50) // Ages between 25-74
      val initialInfectionState = if (biasedCoinToss(initialInfectedFraction)) "Infected" else "Susceptible"
      val homeId = ((i - 1) / 4) + 1 // 4 people per household

      val citizen: Person = Person(
        id = citizenId,
        age = age,
        infectionState = InfectionStatus.withName(initialInfectionState),
        daysInfected = 0
      )

      val home = House(homeId)
      val staysAt = Relation[Person, House](citizenId, "STAYS_AT", homeId)
      val memberOf = Relation[House, Person](homeId, "HOUSES", citizenId)

      val graphData = GraphData()
      graphData.addNode(citizenId, citizen)
      graphData.addNode(homeId, home)
      graphData.addRelations(staysAt, memberOf)

      ingestGraphData(graphData)
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
