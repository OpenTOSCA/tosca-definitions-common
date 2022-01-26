package org.opentosca.artifacttemplates;

import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPConnection;
import jakarta.xml.soap.SOAPConnectionFactory;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;

public abstract class SoapUtil {

    private static final Logger LOG = LoggerFactory.getLogger(SoapUtil.class);

    /**
     * Send SOAP response to the OpenTOSCA Container
     *
     * @param invokeResponse the response object to add as SOAP body
     * @param replyTo the address to send the reply to
     */
    public static void sendSoapResponse(Object invokeResponse, String replyTo) {
        try {
            SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
            SOAPConnection connection = soapConnectionFactory.createConnection();
            MessageFactory factory = MessageFactory.newInstance();
            SOAPMessage message = factory.createMessage();
            SOAPBody body = message.getSOAPBody();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            Document doc = dbf.newDocumentBuilder().newDocument();
            JAXBContext context = JAXBContext.newInstance(invokeResponse.getClass());
            context.createMarshaller().marshal(invokeResponse, doc);
            body.addDocument(doc);

            URL endpoint = new URL(replyTo);
            LOG.debug("Sending response to OpenTOSCA Container at URL: {}\n{}", replyTo, SoapUtil.docToString(doc));
            SOAPMessage response = connection.call(message, endpoint);
            LOG.debug("Response to OpenTOSCA Container returned message: {}", response.toString());
        } catch (SOAPException | ParserConfigurationException | JAXBException | MalformedURLException e) {
            LOG.error("Failed to send SOAP response to address: {}", replyTo, e);
        }
    }

    /**
     * Transform the given XML Document to a String
     *
     * @param document the document to transform
     * @return the transformed document as String
     */
    public static String docToString(Document document) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            return writer.getBuffer().toString().replaceAll("\n|\r", "");
        } catch (TransformerException e) {
            LOG.error("Failed to transform document to string:", e);
            return null;
        }
    }
}
