package org.opentosca.implementationartifacts.model;

/**
 * Model for VM instance data.
 * 
 * @author Christian Endres
 *
 */
public class VMInstance {
	
	private String InstanceID = "";
	private String IP = "";

	public VMInstance(String vMName, String ip) {
		InstanceID = vMName;
		this.IP = ip;
	}

	public String getInstanceID() {
		return InstanceID;
	}

	public void setInstanceID(String instanceID) {
		InstanceID = instanceID;
	}

	public String getIP() {
		return IP;
	}

	public void setIP(String iP) {
		IP = iP;
	}
}
