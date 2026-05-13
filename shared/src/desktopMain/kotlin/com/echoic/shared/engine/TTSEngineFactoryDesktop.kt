package com.echoic.shared.engine

internal actual fun createLocalEngineImpl(): LocalTTSEngine = DesktopLocalEngine()
