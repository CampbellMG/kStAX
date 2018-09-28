package org.cmgcode.xml.test

import com.cmgcode.xml.XmlParser
import org.junit.Test
import org.kxml2.io.KXmlParser
import org.xmlpull.v1.XmlPullParser
import java.io.InputStreamReader
import kotlin.test.assertEquals
import kotlin.test.fail


class KotlinJunitTest {

    @Test
    fun compareBookXml() {

        val bookXml = getBookXml()

        val isr = InputStreamReader(bookXml.byteInputStream())
        var currentNodeName = ""
        var currentElementValue = StringBuilder()
        var currentAttributes = mutableMapOf<String, String>()

        try {

            val javaXmlParser = KXmlParser()
            javaXmlParser.setInput(isr)

            var elementValue = StringBuilder()
            val kotlinXmlParser = XmlParser()
            kotlinXmlParser.setInput(bookXml)
            while (javaXmlParser.next() != XmlPullParser.END_DOCUMENT) {
                when {
                    javaXmlParser.eventType == XmlPullParser.START_TAG -> {
                        currentNodeName = javaXmlParser.name
                        for(i in 0 until javaXmlParser.attributeCount){
                            currentAttributes[javaXmlParser.getAttributeName(i)] = javaXmlParser.getAttributeValue(i)
                        }
                    }
                    javaXmlParser.eventType == XmlPullParser.TEXT -> currentElementValue.append(javaXmlParser.text)
                    javaXmlParser.eventType == XmlPullParser.END_TAG -> {
                        currentAttributes.clear()
                        currentElementValue = StringBuilder()
                    }
                }

                when {
                    kotlinXmlParser.eventType == XmlPullParser.START_TAG -> {
                        assertEquals(kotlinXmlParser.name, currentNodeName)
                        val attributes = mutableMapOf<String, String>()
                        for(i in 0 until kotlinXmlParser.attributeCount){
                            attributes[kotlinXmlParser.getAttributeName(i)] = kotlinXmlParser.getAttributeValue(i)
                        }
                        assertEquals(currentAttributes, attributes)
                    }
                    kotlinXmlParser.eventType == XmlPullParser.TEXT -> elementValue.append(kotlinXmlParser.text)
                    kotlinXmlParser.eventType == XmlPullParser.END_TAG -> {
                        assertEquals(elementValue, currentElementValue)
                        elementValue = StringBuilder()
                    }
                }
            }
        } catch (ex: Exception) {
            fail(ex.message)
        }
    }

    fun getBookXml(): String{
        return "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
                "<FieldWorkers>\n" +
                "<FieldWorker id='728'>\n" +
                "<FirstName>Test</FirstName>\n" +
                "<LastName>Fieldworker 1</LastName>\n" +
                "<Mobile>3958948220</Mobile>\n" +
                "<Address>5 Testar Grove</Address>\n" +
                "<Postcode>3161</Postcode>\n" +
                "<Email>support@dataforce.com.au</Email>\n" +
                "<Fax></Fax>\n" +
                "<Preferred_Method>Email</Preferred_Method>\n" +
                "<AssignedWorkTypes><WorkType id=\"1\" /><WorkType id=\"4\" /><WorkType id=\"7\" /><WorkType id=\"14\" /><WorkType id=\"15\" /><WorkType id=\"12\" /><WorkType id=\"16\" /></AssignedWorkTypes>\n" +
                "</FieldWorker>\n" +
                "<FieldWorker id='829'>\n" +
                "<FirstName>Test</FirstName>\n" +
                "<LastName>Fieldworker 10</LastName>\n" +
                "<Mobile>4083263797</Mobile>\n" +
                "<Address>8 Testar Grove</Address>\n" +
                "<Postcode>3161</Postcode>\n" +
                "<Email>Raymond@dataforce.com.au</Email>\n" +
                "<Fax></Fax>\n" +
                "<Preferred_Method>Email</Preferred_Method>\n" +
                "<AssignedWorkTypes><WorkType id=\"1\" /><WorkType id=\"4\" /><WorkType id=\"7\" /><WorkType id=\"14\" /><WorkType id=\"15\" /><WorkType id=\"12\" /><WorkType id=\"16\" /></AssignedWorkTypes>\n" +
                "</FieldWorker>\n" +
                "<FieldWorker id='761'>\n" +
                "<FirstName>Test</FirstName>\n" +
                "<LastName>Fieldworker 102</LastName>\n" +
                "<Mobile>4982935507</Mobile>\n" +
                "<Address>16 Testar Grove</Address>\n" +
                "<Postcode>3161</Postcode>\n" +
                "<Email>support@dataforce.com.au</Email>\n" +
                "<Fax></Fax>\n" +
                "<Preferred_Method>Email</Preferred_Method>\n" +
                "<AssignedWorkTypes><WorkType id=\"1\" /><WorkType id=\"4\" /><WorkType id=\"7\" /><WorkType id=\"14\" /><WorkType id=\"15\" /><WorkType id=\"12\" /><WorkType id=\"16\" /></AssignedWorkTypes>\n" +
                "</FieldWorker>\n" +
                "<FieldWorker id='796'>\n" +
                "<FirstName>Test</FirstName>\n" +
                "<LastName>Fieldworker 108</LastName>\n" +
                "<Mobile>4311842153</Mobile>\n" +
                "<Address>10 Testar Grove</Address>\n" +
                "<Postcode>3161</Postcode>\n" +
                "<Email>troy@dataforce.com.au</Email>\n" +
                "<Fax></Fax>\n" +
                "<Preferred_Method>Email</Preferred_Method>\n" +
                "<AssignedWorkTypes><WorkType id=\"1\" /><WorkType id=\"4\" /><WorkType id=\"7\" /><WorkType id=\"14\" /><WorkType id=\"15\" /><WorkType id=\"12\" /><WorkType id=\"16\" /></AssignedWorkTypes>\n" +
                "</FieldWorker>\n" +
                "<FieldWorker id='723'>\n" +
                "<FirstName>Test</FirstName>\n" +
                "<LastName>Fieldworker 64</LastName>\n" +
                "<Mobile>1837197508</Mobile>\n" +
                "<Address>6 Testar Grove</Address>\n" +
                "<Postcode>3161</Postcode>\n" +
                "<Email>campbell@dataforce.com.au</Email>\n" +
                "<Fax></Fax>\n" +
                "<Preferred_Method>Email</Preferred_Method>\n" +
                "<AssignedWorkTypes><WorkType id=\"1\" /><WorkType id=\"4\" /><WorkType id=\"7\" /><WorkType id=\"14\" /><WorkType id=\"15\" /><WorkType id=\"12\" /><WorkType id=\"16\" /></AssignedWorkTypes>\n" +
                "</FieldWorker>\n" +
                "</FieldWorkers>"
    }

}