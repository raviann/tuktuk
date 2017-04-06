package com.tuktuk

import akka.actor.ActorSystem

object GlobalActorSystem {
  val actorSystem: ActorSystem = ActorSystem("tuktuk")
}
