package utility

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DescriptionParserTest {

    private val validCoSimXml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <fmiModelDescription>
          <CoSimulation modelIdentifier="BouncingBall">
            <SourceFiles>
              <File name="BouncingBall.c"/>
              <File name="util.c"/>
            </SourceFiles>
          </CoSimulation>
        </fmiModelDescription>
    """.trimIndent()

    private val validModelExchangeXml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <fmiModelDescription>
          <ModelExchange modelIdentifier="SpringMass">
            <SourceFiles>
              <File name="SpringMass.c"/>
            </SourceFiles>
          </ModelExchange>
        </fmiModelDescription>
    """.trimIndent()

    private val missingKindXml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <fmiModelDescription>
        </fmiModelDescription>
    """.trimIndent()

    private val missingSourceFilesXml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <fmiModelDescription>
          <CoSimulation modelIdentifier="Empty">
          </CoSimulation>
        </fmiModelDescription>
    """.trimIndent()

    private val missingModelIdXml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <fmiModelDescription>
          <CoSimulation>
            <SourceFiles><File name="a.c"/></SourceFiles>
          </CoSimulation>
        </fmiModelDescription>
    """.trimIndent()

    @Test
    fun `findModelId returns correct identifier for CoSimulation`() {
        val parser = DescriptionParser(validCoSimXml)
        assertEquals("BouncingBall", parser.findModelId())
    }

    @Test
    fun `findModelId returns correct identifier for ModelExchange`() {
        val parser = DescriptionParser(validModelExchangeXml)
        assertEquals("SpringMass", parser.findModelId())
    }

    @Test
    fun `findModelId throws when modelIdentifier is missing`() {
        val parser = DescriptionParser(missingModelIdXml)
        assertFailsWith<IllegalStateException> {
            parser.findModelId()
        }
    }

    @Test
    fun `findSourceFiles returns all listed files`() {
        val parser = DescriptionParser(validCoSimXml)
        val files = parser.findSourceFiles()
        assertEquals(listOf("BouncingBall.c", "util.c"), files)
    }

    @Test
    fun `findSourceFiles returns single file`() {
        val parser = DescriptionParser(validModelExchangeXml)
        val files = parser.findSourceFiles()
        assertEquals(listOf("SpringMass.c"), files)
    }

    @Test
    fun `findSourceFiles throws when source files are missing`() {
        val parser = DescriptionParser(missingSourceFilesXml)
        assertFailsWith<IllegalStateException> {
            parser.findSourceFiles()
        }
    }

    @Test
    fun `constructor throws when neither CoSimulation nor ModelExchange present`() {
        assertFailsWith<IllegalStateException> {
            DescriptionParser(missingKindXml)
        }
    }

    @Test
    fun `findSourceFiles result is not empty for valid xml`() {
        val parser = DescriptionParser(validCoSimXml)
        assertTrue(parser.findSourceFiles().isNotEmpty())
    }
}
