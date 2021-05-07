package org.opentosca.NodeTypes;

import org.openstack4j.model.compute.Server;
import org.openstack4j.openstack.compute.domain.actions.FloatingIpActions;
import org.openstack4j.openstack.compute.internal.ComputeFloatingIPServiceImpl;


/**
 * This is an extension of ComputeFloatingIPServiceImpl to fix adding Floating
 * IPs, which does not work (at least for our OpenStack configuration).
 */
public class ExtendedFloatingIPServiceImpl extends ComputeFloatingIPServiceImpl {

	@Override
	public org.openstack4j.model.common.ActionResponse addFloatingIP(Server server, String fixedIpAddress, String ipAddress) {

        return invokeAction(server.getId(), FloatingIpActions.Add.create(ipAddress, fixedIpAddress));

	}

	// Old: THIS IS COPIED AS IS from https://github.com/gondor/openstack4j/blob/17c5fe1b56df57d5eeef2ce0c147426c33d25b3b/src/main/java/org/openstack4j/openstack/compute/internal/ComputeFloatingIPServiceImpl.java
	// THIS IS COPIED AS IS from 
	// 		- https://github.com/gondor/openstack4j/blob/master/core/src/main/java/org/openstack4j/openstack/compute/internal/BaseComputeServices.java
	//		- https://github.com/gondor/openstack4j/blob/master/core/src/main/java/org/openstack4j/openstack/compute/internal/ComputeFloatingIPServiceImpl.java
//	private ActionResponse invokeAction(String serverId, ServerAction  action,
//			String innerJson) {
//		
//        return ToActionResponseFunction.INSTANCE.apply(invokeActionWithResponse(serverId, action), action.getClass().getName());
//		
//	}

}
