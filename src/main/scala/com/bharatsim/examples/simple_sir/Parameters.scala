package com.bharatsim.examples.simple_sir

object Parameters {
  // Disease parameters
  final val initialInfectedFraction = 0.05  // 1% initially infected
  final val beta: Double = 0.02             // Infection probability per time step
  final val gamma: Double = 0.1             // Recovery probability per time step

  // Population parameters
  final val populationSize: Int = 1000
}
