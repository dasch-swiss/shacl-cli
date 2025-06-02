package swiss.dasch.shacl.cli

import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.riot.RDFFormat
import zio.*
import zio.cli.*
import zio.cli.HelpDoc.Span.text

import java.io.FileInputStream
import java.io.FileOutputStream

object Main extends ZIOCliDefault {

  private val validator = ShaclValidator()

  private val options =
    Options.boolean("validate-shapes") ++
      Options.boolean("report-details", false) ++
      Options.boolean("add-blank-nodes")
  private val arguments     = Args.text("shacl.ttl") ++ Args.text("data.ttl") ++ Args.text("report.ttl")
  private val help: HelpDoc = HelpDoc.p("Validate a SHACL shape against a data file.")

  private val command =
    Command("shacl").subcommands(Command("validate", options, arguments).withHelp(help))

  // Define val cliApp using CliApp.make
  val cliApp = CliApp.make(
    name = "SHACL CLI",
    version = "0.0.1",
    summary = text("Validate SHACL shapes against data files"),
    command = command,
  ) { case ((validateShapes, reportDetails, addBlankNodes), (shaclPath, dataPath, reportPath)) =>
    ZIO.scoped {
      for {
        shapes     <- ZIO.fromAutoCloseable(ZIO.succeed(new FileInputStream(shaclPath)))
        data       <- ZIO.fromAutoCloseable(ZIO.succeed(new FileInputStream(dataPath)))
        reportPath <- ZIO.fromAutoCloseable(ZIO.succeed(new FileOutputStream(reportPath)))
        report     <- validator.validate(data, shapes, ValidationOptions(validateShapes, reportDetails, addBlankNodes))
        _          <- ZIO.attemptBlockingIO(RDFDataMgr.write(reportPath, report.getModel, RDFFormat.TURTLE))
      } yield 1
    }
  }

  def run(args: List[String]) = cliApp.run(args)
}
