package com.dapa1.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/** Per-project state used by MyChat components. */
@Service(Service.Level.PROJECT)
class MyChatProjectState {
    /** Set to true once post-startup initialization is complete
     *  and JCEF can safely be used.
     */
    @Volatile
    var jcefReady: Boolean = false

    companion object {
        /** Convenience accessor. */
        fun getInstance(project: Project): MyChatProjectState = project.service()
    }
}
