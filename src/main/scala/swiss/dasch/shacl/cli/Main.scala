package swiss.dasch.shacl.cli

import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.riot.RDFFormat
import zio.*
import zio.cli.*
import zio.cli.HelpDoc.Span.text
import zio.logging.slf4j.bridge.Slf4jBridge

import java.io.FileInputStream
import java.io.FileOutputStream

object Main extends ZIOCliDefault {

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Unit] = Runtime.removeDefaultLoggers >>> Slf4jBridge.initialize

  private val validator = ShaclValidator()

  private val options =
    Options.boolean("validate-shapes") ?? "If present shapes must be validated" ++
      Options.boolean("do-not-report-details", false) ?? "If present report does not produce sh:details" ++
      Options.boolean(
        "add-blank-nodes",
      ) ?? "If present finds all blank nodes in the report that references the shapes graph" ++
      Options.file("shacl", Exists.Yes) ?? "Path to the shacl shapes file (in turtle)" ++
      Options.file("data", Exists.Yes) ?? "Path to the data file (in turtle)" ++
      Options.file("report", zio.cli.Exists.Either) ?? "Path to the report file (in turtle)"
  private val help: HelpDoc =
    HelpDoc.p("Validate a SHACL shape against a data file. Currently supports Turtle format only.")

  private val command =
    Command("shacl").subcommands(Command("validate", options).withHelp(help))

  // Define val cliApp using CliApp.make
  val cliApp = CliApp.make(
    name = "SHACL CLI",
    version = "0.0.1",
    summary = text("Validate SHACL shapes against data files"),
    command = command,
  ) { case (validateShapes, reportDetails, addBlankNodes, shaclFile, dataFile, reportFile) =>
    ZIO.scoped {
      for {
        shapes     <- ZIO.fromAutoCloseable(ZIO.succeed(new FileInputStream(shaclFile.toFile)))
        data       <- ZIO.fromAutoCloseable(ZIO.succeed(new FileInputStream(dataFile.toFile)))
        reportPath <- ZIO.fromAutoCloseable(ZIO.succeed(new FileOutputStream(reportFile.toFile)))
        report     <- validator.validate(data, shapes, ValidationOptions(validateShapes, reportDetails, addBlankNodes))
        _          <- ZIO.attemptBlockingIO(RDFDataMgr.write(reportPath, report.getModel, RDFFormat.TURTLE))
      } yield 1
    }
  }
}
