package io.woodenmill.penstock.report

import de.vandermeer.asciitable.{AT_Context, AsciiTable}
import de.vandermeer.asciithemes.u8.U8_Grids
import io.woodenmill.penstock.Metric


private[report] object AsciiTableFormatter {

  def format(metrics: List[Metric[_]]): String = {
    val asciiTable = new AsciiTable(new AT_Context().setGrid(U8_Grids.borderDoubleLight))
    asciiTable.addRule()
    asciiTable.addRow("Metric Name", "Metric Value")
    asciiTable.addRule()
    metrics.foreach(m => asciiTable.addRow(m.name, m.value.toString))
    asciiTable.addRule()
    asciiTable.render()
  }
}
