package io.woodenmill.penstock.testutils

import java.net.ServerSocket

object Ports {

  def nextAvailablePort(): Int = {
    val socket = new ServerSocket(0)
    socket.setReuseAddress(true)
    val port = socket.getLocalPort
    socket.close()
    port
  }
}
