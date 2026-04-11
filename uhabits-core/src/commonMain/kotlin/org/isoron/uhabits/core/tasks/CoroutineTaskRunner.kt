/*
 * Copyright (C) 2016-2025 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.isoron.uhabits.core.tasks

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CoroutineTaskRunner(
    mainDispatcher: CoroutineDispatcher,
    private val ioDispatcher: CoroutineDispatcher
) : TaskRunner {

    private val scope = CoroutineScope(SupervisorJob() + mainDispatcher)
    private val listeners = mutableListOf<TaskRunner.Listener>()
    private val jobs = mutableListOf<Job>()
    private var activeCount = 0

    override val activeTaskCount: Int get() = activeCount

    override fun addListener(listener: TaskRunner.Listener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: TaskRunner.Listener) {
        listeners.remove(listener)
    }

    override fun execute(task: Task) {
        task.onAttached(this)
        val job = scope.launch {
            activeCount++
            listeners.forEach { it.onTaskStarted(task) }
            task.onPreExecute()
            if (!task.isCanceled()) {
                withContext(ioDispatcher) {
                    task.doInBackground()
                }
            }
            task.onPostExecute()
            activeCount--
            listeners.forEach { it.onTaskFinished(task) }
        }
        job.invokeOnCompletion { jobs.remove(job) }
        jobs.add(job)
    }

    override suspend fun await() {
        jobs.joinAll()
        jobs.clear()
    }

    override fun publishProgress(task: Task, progress: Int) {
        scope.launch {
            task.onProgressUpdate(progress)
        }
    }
}
