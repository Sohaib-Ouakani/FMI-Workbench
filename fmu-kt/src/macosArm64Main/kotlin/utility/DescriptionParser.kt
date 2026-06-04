package utility

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parser.Parser

/**
 * Parses FMU modelDescription.xml files to extract model metadata and source file information.
 * Uses Ksoup to parse XML and select specific elements.
 *
 * @param xmlDescription The XML content of the modelDescription.xml file as a string.
 * @throws IllegalStateException if no CoSimulation/ModelExchange tag is found in the XML.
 */
class DescriptionParser(xmlDescription: String) {
    /**
     * The parsed XML document as a [Document] object.
     */
    val doc: Document = Ksoup.parse(xmlDescription, parser = Parser.xmlParser())

    /**
     * The CoSimulation or ModelExchange element from the XML.
     * Contains the model identifier and source file references.
     */
    val kindElement = doc.selectFirst("CoSimulation, ModelExchange")
        ?: error("No CoSimulation/ModelExchange tag found — invalid modelDescription.xml")


    /**
     * Extracts the list of source files referenced in the modelDescription.xml.
     *
     * @return A list of source file names found in the <SourceFiles> section.
     * @throws IllegalStateException if no source files are found.
     */
    fun findSourceFiles(): List<String> {
        val sourceFiles = kindElement.select("SourceFiles > File")
            .map { it.attr("name") }
            .also { check(it.isNotEmpty()) { "No source files found in <SourceFiles>" } }

        println("KSOUP source files: $sourceFiles")

        return sourceFiles
    }

    /**
     * Extracts the model identifier from the modelDescription.xml.
     *
     * @return The model identifier string.
     * @throws IllegalStateException if the modelIdentifier attribute is not found or is blank.
     */
    fun findModelId(): String {
        val modelId = kindElement.attr("modelIdentifier")
            .also { check(it.isNotBlank()) { "modelIdentifier not found" } }

        println("KSOUP model identifier: $modelId")

        return modelId
    }
}
