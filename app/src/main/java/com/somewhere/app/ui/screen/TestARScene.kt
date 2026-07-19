package com.somewhere.app.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.sceneview.ar.ARScene
import io.github.sceneview.node.Node
import com.google.ar.core.Config

@Composable
fun TestARScene() {
    ARScene(
        modifier = Modifier,
        childNodes = listOf<Node>(),
        sessionConfiguration = { session, config ->
            config.geospatialMode = Config.GeospatialMode.ENABLED
        },
        onViewCreated = { 
            val view = this
        },
        onViewUpdated = { 
            val view = this
        },
        onSessionUpdated = { session, frame ->
            
        }
    )
}
