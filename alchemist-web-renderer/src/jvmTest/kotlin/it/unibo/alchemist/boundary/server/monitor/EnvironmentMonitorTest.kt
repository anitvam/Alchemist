/*
 * Copyright (C) 2010-2022, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.server.monitor

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldNotBe
import it.unibo.alchemist.boundary.TestUtility.webRendererTestEnvironments
import it.unibo.alchemist.boundary.webui.common.model.surrogate.EnvironmentSurrogate
import it.unibo.alchemist.boundary.webui.server.monitor.EnvironmentMonitorFactory.makeEnvironmentMonitor
import it.unibo.alchemist.boundary.webui.server.state.ServerStore
import it.unibo.alchemist.boundary.webui.server.state.actions.SetEnvironmentSurrogate
import it.unibo.alchemist.model.Time

class EnvironmentMonitorTest :
    StringSpec({
        fun checkStep(action: () -> Unit) {
            val initialEnvironment = ServerStore.store.state.environmentSurrogate
            action()
            initialEnvironment shouldNotBe ServerStore.store.state.environmentSurrogate
            ServerStore.store.dispatch(SetEnvironmentSurrogate(EnvironmentSurrogate.uninitializedEnvironment()))
        }

        "EnvironmentMonitor stepDone should work as expected" {
            webRendererTestEnvironments<Any, Nothing>().forEach {
                val environmentMonitor = makeEnvironmentMonitor(it.environment)
                checkStep {
                    environmentMonitor.stepDone(it.environment, null, Time.ZERO, 111)
                }
                checkStep {
                    environmentMonitor.initialized(it.environment)
                }
                checkStep {
                    environmentMonitor.finished(it.environment, Time.ZERO, 222)
                }
            }
        }
    })
