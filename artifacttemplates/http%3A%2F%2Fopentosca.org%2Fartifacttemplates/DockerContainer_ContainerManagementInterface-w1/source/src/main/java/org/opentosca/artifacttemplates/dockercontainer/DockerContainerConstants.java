package org.opentosca.artifacttemplates.dockercontainer;

public abstract class DockerContainerConstants {

    // namespace under which the SOAP service operates
    public static final String NAMESPACE_URI = "http://artifacttemplates.opentosca.org";

    // port type to use for the SOAP service
    public static final String PORT_TYPE_NAME = "org_opentosca_artifactTemplates_DockerContainer_ContainerManagementInterfacePort";

    // name of the XML Schema file to use as basis for the WSDL generation
    public static final String XSD_NAME = "containerManagementInterface.xsd";
}
