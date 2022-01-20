package org.opentosca.artifacttemplates;

public class Constants {

    protected static final String NAMESPACE_URI = "http://spring.io/guides/gs-producing-web-service";
    protected static final String PORT_TYPE_NAME = "CountriesPort";
    protected static final String LOCATION_URI = "/ws";
    protected static final String XSD_NAME = "model.xsd";

    // under this name the WSDL of the SOAP service can be accessed, i.e., IP:PORT/LOCATION_URI/WSDL_NAME.wsdl
    protected static final String WSDL_NAME = "model";
}
