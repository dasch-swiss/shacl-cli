package swiss.dasch.shacl.cli

import zio.test.ZIOSpecDefault
import zio.test.assertTrue
import org.topbraid.shacl.vocabulary.SH

object ShaclValidatorSpec extends ZIOSpecDefault {

  private val shaclShapes = """
    @prefix sh: <http://www.w3.org/ns/shacl#> .
    @prefix ex: <http://example.org/wonderland#> .
    @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

    ex:CharacterShape
        a sh:NodeShape ;
        sh:targetClass ex:Character ;
        sh:property [
            sh:path ex:name ;
            sh:datatype xsd:string ;
            sh:minCount 1 ;
            sh:maxCount 1 ;
            sh:message "Character must have exactly one name"
        ] ;
        sh:property [
            sh:path ex:age ;
            sh:datatype xsd:integer ;
            sh:minInclusive 0 ;
            sh:maxCount 1 ;
            sh:message "Age must be a non-negative integer"
        ] ;
        sh:property [
            sh:path ex:location ;
            sh:datatype xsd:string ;
            sh:minCount 1 ;
            sh:maxCount 1 ;
            sh:message "Character must have exactly one location"
        ] .
  """

  private val validAliceData = """
    @prefix ex: <http://example.org/wonderland#> .

    ex:Alice
        a ex:Character ;
        ex:name "Alice" ;
        ex:age 7 ;
        ex:location "Wonderland" .
  """

  private val invalidMadHatterData = """
    @prefix ex: <http://example.org/wonderland#> .

    ex:MadHatter
        a ex:Character ;
        ex:name "Mad Hatter" ;
        ex:age 42 .
  """

  private val defaultValidationOptions =
    ValidationOptions(validateShapes = false, reportDetails = true, addBlankNodes = false)
  private val validator = ShaclValidator()

  def spec = suite("SHACL CLI Tests")(
    test("Alice validates successfully against Character shape") {
      for {
        report   <- validator.validate(validAliceData, shaclShapes, defaultValidationOptions)
        conforms  = report.getProperty(SH.conforms).getBoolean
      } yield assertTrue(conforms)
    },
    test("Mad Hatter fails validation due to missing location") {
      for {
        report   <- validator.validate(invalidMadHatterData, shaclShapes, defaultValidationOptions)
        conforms  = report.getProperty(SH.conforms).getBoolean
      } yield assertTrue(!conforms)
    },
  )
}
