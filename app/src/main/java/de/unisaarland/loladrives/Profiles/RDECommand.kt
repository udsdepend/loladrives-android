package de.unisaarland.loladrives.profiles

import pcdfEvent.events.obdEvents.OBDCommand

data class RDECommand(val command: OBDCommand, val update_frequency: Int) {
    companion object {
        fun toRDECommand(c: OBDCommand): RDECommand {
            return RDECommand(c, 0)
        }
    }
}
