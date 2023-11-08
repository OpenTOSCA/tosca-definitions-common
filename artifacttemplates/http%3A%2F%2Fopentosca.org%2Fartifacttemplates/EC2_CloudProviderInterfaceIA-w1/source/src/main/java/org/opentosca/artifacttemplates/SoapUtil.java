package org.opentosca.artifacttemplates;

import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.xml.messaging.saaj.client.p2p.HttpSOAPConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.soap.saaj.SaajSoapMessage;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public abstract class SoapUtil {

    // name of the header containing the message ID to send results to the OpenTOSCA Container
    public static final String MESSAGE_ID_HEADER = "MessageID";

    // name of the header containing the return address to send results to the OpenTOSCA Container
    public static final String REPLY_TO_HEADER = "ReplyTo";

    // name of the header containing deployment artifacts
    public static final String DEPLOYMENT_ARTIFACTS_STRING = "DEPLOYMENT_ARTIFACTS_STRING";

    private static final Logger LOG = LoggerFactory.getLogger(SoapUtil.class);

    /**
     * Send SOAP response to the OpenTOSCA Container
     *
     * @param invokeResponse the response object to add as SOAP body
     * @param replyTo        the address to send the reply to
     */
    public static <T> void sendSoapResponse(T invokeResponse, Class<T> invokeResponseClass, String replyTo) {
        try {
            SOAPConnection connection = new HttpSOAPConnectionFactory().createConnection();
            MessageFactory factory = MessageFactory.newInstance();
            SOAPMessage message = factory.createMessage();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            Document doc = dbf.newDocumentBuilder().newDocument();
            JAXBContext.newInstance(invokeResponseClass)
                    .createMarshaller()
                    .marshal(
                            new JAXBElement<>(new QName("", "invokeResponse"), invokeResponseClass, invokeResponse),
                            doc
                    );
            // Log must be done before adding, because the doc seems to be empty afterwards
            LOG.debug("Sending response to OpenTOSCA Container at URL: {}\n{}", replyTo, SoapUtil.docToString(doc));

            URL endpoint = new URL(replyTo);
            message.getSOAPBody().addDocument(doc);
            SOAPMessage response = connection.call(message, endpoint);

            LOG.debug("Response to OpenTOSCA Container returned message: {}", response.toString());
        } catch (SOAPException | ParserConfigurationException | JAXBException | MalformedURLException e) {
            LOG.error("Failed to send SOAP response to address: {}", replyTo, e);
        }
    }

    public static OpenToscaHeaders parseHeaders(MessageContext messageContext) {
        // Retrieve the always required SOAP headers, e.g., the messageID and replyTo headers.
        Node messageIdNode = getHeaderFieldByName(messageContext, MESSAGE_ID_HEADER);
        Node replyToNode = getHeaderFieldByName(messageContext, REPLY_TO_HEADER);
        if (Objects.isNull(messageIdNode) || Objects.isNull(replyToNode)) {
            LOG.error("Unable to retrieve message ID and reply to headers from received SOAP request!");
            throw new IllegalArgumentException("Required header fields are not set!");
        }

        String messageId = messageIdNode.getTextContent();
        String replyTo = replyToNode.getFirstChild().getTextContent();
        // region *** additional header elements ***

        /* Just for reference, the DEPLOYMENT_ARTIFACTS_STRING element contains something like this:
         * {
         *     "{http://opentosca.org/artifacttypes}CloudImage": {
         *         "focal-server-cloudimg-amd64.img": "http://[ip]:1337/csars/.../files/focal-server-cloudimg-amd64.img"
         *     }
         * }
         */
        Node deploymentArtifactsNode = getHeaderFieldByName(messageContext, DEPLOYMENT_ARTIFACTS_STRING);
        Map<QName, Map<String, String>> deploymentArtifactsMap = new HashMap<>();

        if (deploymentArtifactsNode != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Map<String, String>> artifactTypesMap = objectMapper.readValue(deploymentArtifactsNode.getTextContent(), Map.class);

                if (!artifactTypesMap.isEmpty()) {
                    for (Map.Entry<String, Map<String, String>> entry : artifactTypesMap.entrySet()) {
                        deploymentArtifactsMap.put(
                                QName.valueOf(entry.getKey()),
                                entry.getValue()
                        );
                    }
                }
            } catch (JsonProcessingException e) {
                LOG.error("Cannot parse attached deployment artifacts!", e);
            }
        }

        // endregion

        OpenToscaHeaders openToscaHeaders = new OpenToscaHeaders(
                messageId,
                replyTo,
                deploymentArtifactsMap
        );
        LOG.info("Got headers:\n{}", openToscaHeaders);

        return openToscaHeaders;
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
            return writer.getBuffer().toString().replaceAll("[\n\r]", "");
        } catch (TransformerException e) {
            LOG.error("Failed to transform document to string:", e);
            return null;
        }
    }

    /**
     * Get the SOAP header with the given name from the current SOAP message
     *
     * @param messageContext the context to access the SOAP message
     * @return the Node representing the content of the header if the given name, or null if the corresponding header is
     * not defined
     */
    protected static Node getHeaderFieldByName(MessageContext messageContext, String headerName) {
        SaajSoapMessage soapRequest = (SaajSoapMessage) messageContext.getRequest();

        try {
            Iterator<SOAPHeaderElement> itr = soapRequest.getSaajMessage().getSOAPHeader().examineAllHeaderElements();
            while (itr.hasNext()) {
                SOAPHeaderElement header = itr.next();
                LOG.debug("Found header with name '{}'", header.getNodeName());

                if (header.getNodeName().equals(headerName)) {
                    return header.getFirstChild();
                }
            }
        } catch (SOAPException e) {
            LOG.error("Error while parsing SOAP header!", e);
        }

        // no header with the given name found
        return null;
    }

    public static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
