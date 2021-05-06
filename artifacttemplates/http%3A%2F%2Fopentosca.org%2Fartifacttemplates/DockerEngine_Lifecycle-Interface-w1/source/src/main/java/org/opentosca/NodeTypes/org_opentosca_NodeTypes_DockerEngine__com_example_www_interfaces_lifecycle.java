package org.opentosca.NodeTypes;

import java.util.HashMap;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlElement;

@WebService
public class org_opentosca_NodeTypes_DockerEngine__com_example_www_interfaces_lifecycle extends AbstractIAService {

	@WebMethod
	@SOAPBinding
	@Oneway
	public void configure(
		@WebParam(name="VMIP", targetNamespace="http://NodeTypes.opentosca.org/") String VMIP
	) {
		// This HashMap holds the return parameters of this operation.
		final HashMap<String,String> returnParameters = new HashMap<String, String>();

		// Output Parameter 'DockerEngineURL' (required)
		// Do NOT delete the next line of code. Set "" as value if you want to return nothing or an empty result!
		returnParameters.put("DockerEngineURL", "tcp://" + VMIP + ":2375");

		sendResponse(returnParameters);
	}



}
