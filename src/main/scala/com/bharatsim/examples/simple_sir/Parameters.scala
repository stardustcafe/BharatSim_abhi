package com.bharatsim.examples.simple_sir

import com.bharatsim.engine.ScheduleUnit

object Parameters {
  // Time parameters
  final val numberOfTicksInADay: Int = 1
  final val dt: Double = 1.0 / numberOfTicksInADay

  final val myTick: ScheduleUnit = new ScheduleUnit(1)
  final val myDay: ScheduleUnit = new ScheduleUnit(myTick * numberOfTicksInADay)

  // Disease parameters
  final val initialInfectedFraction = 0.01  // 1% initially infected
  final val beta: Double = 0.3              // Transmission rate
  final val gamma: Double = 1.0 / 14        // Recovery rate (14 days average)
  
  // Population parameters
  final val populationSize: Int = 1000
}
