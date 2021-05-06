package org.opentosca.implementationartifacts;

import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.HashMap;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlElement;

import org.opentosca.implementationartifacts.model.VMInstance;

import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.RuntimeFault;

@WebService(targetNamespace = "http://implementationartifacts.opentosca.org/")
public class org_opentosca_nodetypes_VMWare5_5__CloudProviderInterface extends AbstractIAService {

	@WebMethod
	@SOAPBinding
	@Oneway
	public void createVM(
			@WebParam(name = "HypervisorEndpoint", targetNamespace = "http://implementationartifacts.opentosca.org/") String HypervisorEndpoint,
			@WebParam(name = "HypervisorUserName", targetNamespace = "http://implementationartifacts.opentosca.org/") String HypervisorUserName,
			@WebParam(name = "HypervisorUserPassword", targetNamespace = "http://implementationartifacts.opentosca.org/") String HypervisorUserPassword,
			@WebParam(name = "HypervisorTenantID", targetNamespace = "http://implementationartifacts.opentosca.org/") String HypervisorTenantID,
			@WebParam(name = "VMType", targetNamespace = "http://implementationartifacts.opentosca.org/") String VMType,
			@WebParam(name = "VMImageID", targetNamespace = "http://implementationartifacts.opentosca.org/") String VMImageID,
			@WebParam(name = "VMUserName", targetNamespace = "http://implementationartifacts.opentosca.org/") String VMUserName,
			@WebParam(name = "VMUserPassword", targetNamespace = "http://implementationartifacts.opentosca.org/") String VMUserPassword,
			@WebParam(name = "VMPrivateKey", targetNamespace = "http://implementationartifacts.opentosca.org/") String VMPrivateKey,
			@WebParam(name = "VMPublicKey", targetNamespace = "http://implementationartifacts.opentosca.org/") String VMPublicKey,
			@WebParam(name = "VMKeyPairName", targetNamespace = "http://implementationartifacts.opentosca.org/") String VMKeyPairName)
			throws RemoteException, MalformedURLException, InterruptedException, CredentialsFormatException {
		// This HashMap holds the return parameters of this operation.
		final HashMap<String, String> returnParameters = new HashMap<String, String>();

		VMInstance instance = new vsphere_clone_template().create(HypervisorEndpoint, HypervisorUserName, HypervisorUserPassword,
				HypervisorTenantID, HypervisorTenantID, VMType, VMUserName, VMUserPassword, VMPublicKey);

		if (null != instance) {
			returnParameters.put("VMInstanceID", instance.getInstanceID());
			returnParameters.put("VMIP", instance.getIP());
		} else

		{
			returnParameters.put("VMInstanceID", "Could not create VM");
			returnParameters.put("VMIP", "Could not create VM");
		}

		sendResponse(returnParameters);
	}

	@WebMethod
	@SOAPBinding
	@Oneway
	public void terminateVM(
			@WebParam(name = "HypervisorEndpoint", targetNamespace = "http://implementationartifacts.opentosca.org/") String HypervisorEndpoint,
			@WebParam(name = "HypervisorUserName", targetNamespace = "http://implementationartifacts.opentosca.org/") String HypervisorUserName,
			@WebParam(name = "HypervisorUserPassword", targetNamespace = "http://implementationartifacts.opentosca.org/") String HypervisorUserPassword,
			@WebParam(name = "HypervisorTenantID", targetNamespace = "http://implementationartifacts.opentosca.org/") String HypervisorTenantID,
			@WebParam(name = "VMInstanceID", targetNamespace = "http://implementationartifacts.opentosca.org/") String VMID)
			throws InvalidProperty, RuntimeFault, RemoteException, MalformedURLException, InterruptedException,
			CredentialsFormatException {
		// This HashMap holds the return parameters of this operation.
		final HashMap<String, String> returnParameters = new HashMap<String, String>();
		
		new vsphere_clone_template().terminate(HypervisorEndpoint, HypervisorUserName, HypervisorUserPassword,
				HypervisorTenantID, VMID);
		
		returnParameters.put("Result", "true");
		
		sendResponse(returnParameters);
	}

}
