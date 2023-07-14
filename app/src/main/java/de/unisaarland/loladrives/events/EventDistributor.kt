/*
 * Copyright (c) 2021.
 */

package de.unisaarland.loladrives.events

import kotlinx.coroutines.channels.Channel
import pcdfEvent.PCDFEvent
import java.util.concurrent.locks.ReentrantLock

/**
 * Serves as a central distributor for [PCDFEvent]s.
 * Can be started in a coroutine. Receives events over its [inputChannel] and distributes them to
 * registered consumers over its [outputChannels].
 */
class EventDistributor {
    val inputChannel = Channel<PCDFEvent>(10000)
    private val outputChannels: MutableList<Channel<PCDFEvent>> = mutableListOf()
    private val lock = ReentrantLock()

    fun stop() {
        inputChannel.close()
        for (channel in outputChannels) {
            channel.close()
        }
    }

    /**
     * Main function for event distribution, to be started in a coroutine.
     */
    suspend fun start() {
        for (event in inputChannel) {
            lock.lock()
            for (channel in outputChannels) {
                channel.offer(event)
            }
            lock.unlock()
        }
    }

    fun registerReceiver(): Channel<PCDFEvent> {
        val newChannel = Channel<PCDFEvent>(1000)
        lock.lock()
        outputChannels.add(newChannel)
        lock.unlock()
        return newChannel
    }

    fun unregisterReceiver(channel: Channel<PCDFEvent>) {
        lock.lock()
        outputChannels.remove(channel)
        lock.unlock()
    }

    fun getOutputChannels(): MutableList<Channel<PCDFEvent>> {
        return outputChannels
    }
}
