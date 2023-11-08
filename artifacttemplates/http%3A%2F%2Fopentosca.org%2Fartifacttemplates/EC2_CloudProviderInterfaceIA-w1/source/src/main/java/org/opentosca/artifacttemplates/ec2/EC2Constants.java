package org.opentosca.artifacttemplates.ec2;

public abstract class EC2Constants {

    // namespace under which the SOAP service operates
    public static final String NAMESPACE_URI = "http://artifacttemplates.opentosca.org";

    // port type to use for the SOAP service
    public static final String PORT_TYPE_NAME = "org_opentosca_artifactTemplates_EC2_CloudProviderInterfacePort";

    // name of the XML Schema file to use as basis for the WSDL generation
    public static final String XSD_NAME = "cloudProviderInterface.xsd";
}
