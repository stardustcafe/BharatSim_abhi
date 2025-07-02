package com.bharatsim.examples.simple_sir

import com.bharatsim.engine.basicConversions.StringValue
import com.bharatsim.engine.basicConversions.decoders.BasicDecoder
import com.bharatsim.engine.basicConversions.encoders.BasicEncoder

object InfectionStatus extends Enumeration {
  type InfectionStatus = Value
  val Susceptible, Infected, Recovered = Value

  implicit val infectionStatusDecoder: BasicDecoder[InfectionStatus] = {
    case StringValue(v) => withName(v)
    case _ => throw new RuntimeException("Infection status was not stored as a string")
  }

  implicit val infectionStatusEncoder: BasicEncoder[InfectionStatus] = {
    case Susceptible => StringValue("Susceptible")
    case Infected => StringValue("Infected")
    case Recovered => StringValue("Recovered")
  }
}
