package org.opentosca.artifacttemplates;

public class Constants {

    // namespace under which the SOAP service operates
    protected static final String NAMESPACE_URI = "http://artifacttemplates.opentosca.org";

    // port type to use for the SOAP service
    protected static final String PORT_TYPE_NAME = "org_opentosca_artifactTemplates_DockerEngine_DockerEngineInterfacePort";

    // name of the XML Schema file to use as basis for the WSDL generation
    protected static final String XSD_NAME = "dockerEngineInterface.xsd";

    // name of the header containing the message ID to send results to the OpenTOSCA Container
    protected static final String MESSAGE_ID_HEADER = "MessageID";

    // name of the header containing the return address to send results to the OpenTOSCA Container
    protected static final String REPLY_TO_HEADER = "ReplyTo";
}
