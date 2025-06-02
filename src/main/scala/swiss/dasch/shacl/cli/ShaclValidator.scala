package swiss.dasch.shacl.cli

import org.apache.jena.ontology.OntModelSpec
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.util.FileUtils
import org.topbraid.shacl.tools.BlankNodeFinder
import org.topbraid.shacl.validation.ValidationEngineConfiguration
import org.topbraid.shacl.validation.ValidationUtil
import zio.*

import java.io.ByteArrayInputStream
import java.io.InputStream

final case class ValidationOptions(validateShapes: Boolean, reportDetails: Boolean, addBlankNodes: Boolean)

final case class ShaclValidator() { self =>

  def validate(data: String, shapes: String, opts: ValidationOptions): Task[Resource] =
    validate(new ByteArrayInputStream(data.getBytes), new ByteArrayInputStream(shapes.getBytes), opts)

  def validate(data: InputStream, shapes: InputStream, opts: ValidationOptions): Task[Resource] = for {
    dataModel   <- readModel(data)
    shapesModel <- readModel(shapes)
    report      <- validate(dataModel, shapesModel, opts)
  } yield report

  private def readModel(data: InputStream) = ZIO.attempt {
    val model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM)
    model.read(data, null, FileUtils.langTurtle)
    model
  }

  private def validate(data: Model, shapes: Model, opts: ValidationOptions): Task[Resource] = {
    val engineOpts = new ValidationEngineConfiguration()
      .setValidateShapes(opts.validateShapes)
      .setReportDetails(opts.reportDetails)
    ZIO.attempt(ValidationUtil.validateModel(data, shapes, engineOpts)).map { report =>
      if (opts.addBlankNodes) {
        val referencedNodes = BlankNodeFinder.findBlankNodes(report.getModel, shapes)
        val _               = report.getModel.add(referencedNodes)
      }
      report
    }
  }
}
