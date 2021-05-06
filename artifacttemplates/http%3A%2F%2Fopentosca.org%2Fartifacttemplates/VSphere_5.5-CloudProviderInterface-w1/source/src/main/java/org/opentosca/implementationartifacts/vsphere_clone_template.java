package org.opentosca.implementationartifacts;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.logging.Logger;

import org.opentosca.implementationartifacts.model.VMInstance;

import com.vmware.vim25.CustomizationFault;
import com.vmware.vim25.FileFault;
import com.vmware.vim25.GuestOperationsFault;
import com.vmware.vim25.GuestProgramSpec;
import com.vmware.vim25.InsufficientResourcesFault;
import com.vmware.vim25.InvalidDatastore;
import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.InvalidState;
import com.vmware.vim25.MigrationFault;
import com.vmware.vim25.NamePasswordAuthentication;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.TaskInProgress;
import com.vmware.vim25.VimFault;
import com.vmware.vim25.VirtualMachineCloneSpec;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.VirtualMachineRelocateSpec;
import com.vmware.vim25.VmConfigFault;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.GuestOperationsManager;
import com.vmware.vim25.mo.GuestProcessManager;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ResourcePool;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

/**
 * @author Andreas Bader
 * @author Christian Endres
 * @author Benjamin Weder
 *
 *         Code snippets from
 *         https://github.com/yavijava/yavijava-samples/blob/gradle/src/main/
 *         java/ samples/vm/CreateVM.java
 *         https://github.com/yavijava/yavijava-samples/blob/gradle/src/main/
 *         java/ samples/vm/VMpowerOps.java
 *         http://www.doublecloud.org/2012/02/run-program-in-guest-operating-
 *         system-on- vmware/
 *         https://github.com/yavijava/yavijava-samples/blob/gradle/src/main/
 *         java/ samples/vm/CloneVM.java
 */
public class vsphere_clone_template {

	private static final Logger log = Logger.getLogger(vsphere_clone_template.class.getName());

	// timeout for waiting for the guest tools on a created vm
	private final int waitTime = 3 * 60 * 1000;

	/**
	 * Method for creating a VM by cloning a template.
	 * 
	 * @param regionEndpoint
	 *            URL to the vSphere server, i.e.,
	 *            https://iaasvc.informatik.uni-stuttgart.de/sdk
	 * @param apiUser
	 *            <userId>
	 * @param apiPassword
	 *            <password>
	 * @param vsBasePath
	 *            the basepath that vSphere requires
	 * @param vsResourcePool
	 *            the resource pool
	 * @param templateID
	 *            Name of the template to clone, i.e., m1.medium.template
	 * @param vmUsername
	 *            the username to create or use
	 * @param vmUserPassword
	 *            the password for the vmUsername
	 * @param vmPublicKey
	 *            The public key to inject
	 * 
	 * @throws RemoteException
	 * @throws MalformedURLException
	 * @throws InterruptedException
	 * @throws CredentialsFormatException
	 */
	public VMInstance create(String regionEndpoint, String apiUser, String apiPassword, String vsBasePath,
			String vsResourcePool, String templateID, String vmUsername, String vmUserPassword, String vmPublicKey)
			throws RemoteException, MalformedURLException, InterruptedException, CredentialsFormatException {

		// create connection to VSphere and generate VM id
		log.info("Connecting to " + regionEndpoint);
		ServiceInstance si = new ServiceInstance(new URL(regionEndpoint.trim()), apiUser.trim(), apiPassword.trim(), true);
		String vmName = "OT-ProvInstance-" + apiUser + "_" + System.currentTimeMillis();
		
		// search the template that has to be cloned
		log.info("Searching VM Template.");
		VirtualMachine template = searchVM(si, templateID, vsBasePath);
		if (template == null) {
			// template not found --> exit 
			log.severe("VM template '" + templateID + "' does not exist in '" + vsBasePath + "'.");
			si.getServerConnection().logout();
			return null;
		}
		log.info("Found template " + template.getName());
		
		// terminate VM with same name if it exists
		terminateIfExists(si, vmName, vsBasePath);

		// search the resource pool that is defined and clone the VM
		log.info("Cloning VM with name '" + vmName + "'.");
		ResourcePool rp = getResourcePool(si, vsResourcePool);
		VirtualMachine vm = cloneVM(vsBasePath, vmName, si, rp, template);

		waitForGuestTools(si, vm);

		// retrieve the IP of the created VM to enable connections
		String ip = vm.getSummary().getGuest().getIpAddress();
		log.info("VM has the name \"" + vmName + "\" at IP \"" + ip + "\" and the status of the guest tools is \""
				+ vm.getGuest().toolsRunningStatus + "\".");

		configureVM(si, vm, vmUsername, vmUserPassword, vmPublicKey);

		// return created VM
		si.getServerConnection().logout();
		return new VMInstance(vmName, ip);
	}

	private void configureVM(ServiceInstance vSphereConnection, VirtualMachine vm, String vmUsername,
			String vmUserPassword, String vmPublicKey)
			throws GuestOperationsFault, InvalidState, TaskInProgress, FileFault, RuntimeFault, RemoteException {

		// create the user with password
		executeCommand(vSphereConnection, vm, vmUsername, vmUserPassword,
				"if ! id \"" + vmUsername + "\" >/dev/null 2>&1; then \n" + "useradd -D " + vmUsername + "\n" + "fi\n"
						+ "echo -e \"#!/bin/bash \necho \\\"" + vmUsername + ":" + vmUserPassword
						+ "\\\" | sudo chpasswd\" > change.sh \n" + "echo \"ubuntu\" | sudo -S sh change.sh \n"
						+ "rm change.sh");

		// change sudo group so that the given user can sudo without typing the
		// password
		// sed -i '/%sudo/c\%sudo ALL=(ALL:ALL) NOPASSWD:ALL'

		String replacementString = "%sudo ALL=(ALL:ALL) NOPASSWD:ALL";
		String replacedString = "%sudo";
		executeCommand(vSphereConnection, vm, vmUsername, vmUserPassword, "echo \"ubuntu\" | sudo -S sed -i '/"
				+ replacedString + "/c\\" + replacementString + "' /etc/sudoers;");

		// inject the public key
		executeCommand(vSphereConnection, vm, vmUsername, vmUserPassword,
				"echo -e \"" + vmPublicKey + "\" > ~/.ssh/authorized_keys;\n");
	}

	private void executeCommand(ServiceInstance serviceInstance, VirtualMachine vm, String vmUsername,
			String vmUserPassword, String bashCommand)
			throws GuestOperationsFault, InvalidState, TaskInProgress, FileFault, RuntimeFault, RemoteException {

		GuestOperationsManager gom = serviceInstance.getGuestOperationsManager();

		NamePasswordAuthentication npa = new NamePasswordAuthentication();
		npa.username = "ubuntu";
		npa.password = "ubuntu";

		GuestProgramSpec spec = new GuestProgramSpec();
		spec.programPath = "/";
		spec.arguments = "#!/bin/bash \n" + bashCommand;// + " >>
		// ~./management.log";
		log.info("Invoke script on provisioned system: \n" + spec.getArguments());

		GuestProcessManager gpm = gom.getProcessManager(vm);

		try {
			long pid = gpm.startProgramInGuest(npa, spec);
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			// "Standard username and password does not work (ubuntu/ubuntu),
			// thus, try with the credentials from the parameters");
			try {

				NamePasswordAuthentication npa2 = new NamePasswordAuthentication();
				npa.username = vmUsername;
				npa.password = vmUserPassword;

				long pid = gpm.startProgramInGuest(npa2, spec);
			} catch (Exception e2) {
				log.severe(e2.getLocalizedMessage());
			}
		} finally {

		}
	}

	/**
	 * Method for terminating a VM, if the VM exists.
	 * 
	 * @param regionEndpoint
	 *            URL to the vSphere server, i.e.,
	 *            https://iaasvc.informatik.uni-stuttgart.de/sdk
	 * @param apiUser
	 *            username for the endpoint
	 * @param apiPassword
	 *            password for the endpoint
	 * @param vsBasePath
	 *            the basepath that vSphere requires	 
	 * @param vmName
	 * 			  the name of the VM to terminate
	 * @throws InvalidProperty
	 * @throws RuntimeFault
	 * @throws RemoteException
	 * @throws MalformedURLException
	 * @throws CredentialsFormatException
	 * @throws InterruptedException
	 */
	public void terminate(String regionEndpoint, String apiUser, String apiPassword, String vsBasePath, String vmName)
			throws InvalidProperty, RuntimeFault, RemoteException, MalformedURLException, InterruptedException,
			CredentialsFormatException {

		log.info("Terminate the VM: " + vmName);
		ServiceInstance si = new ServiceInstance(new URL(regionEndpoint.trim()), apiUser.trim(), apiPassword.trim(), true);

		// search the VM and terminate it if it exists
		if (!terminateIfExists(si, vmName, vsBasePath)) {
			log.warning("The VM \"" + vmName + "\" did not exist, but should have to!");
		}
		si.getServerConnection().logout();
	}

	private void waitForGuestTools(ServiceInstance vSphereConnection, VirtualMachine vm) throws InterruptedException {
		log.info("Entering wait loop until either: (1) the guest tools are available or (2) a timeout of "
				+ waitTime / 1000 + " seconds has passed.");
		boolean run = true;
		long millis = System.currentTimeMillis();
		while (run) {

			Thread.sleep(1000);

			if ("guestToolsRunning".equals(vm.getGuest().toolsRunningStatus)) {
				log.info("Guest tools are available.");
				run = false;
			} else if (System.currentTimeMillis() - millis > waitTime) {
				log.severe("Wait time exceeded, logging out and stopping the creation of the VM prematurely.");
				run = false;
				vSphereConnection.getServerConnection().logout();
			}

		}
		log.info("Stopped waiting.");
	}

	/**
	 * Search a VM with a certain name that is located in a certain folder.
	 * 
	 * @param si the service instance where the VM shall be searched
	 * @param searchTarget the name of the target VM
	 * @param vsBasePath the folder name where the target VM has to be stored
	 * @return The VM if it was found or null otherwise
	 * @throws RemoteException
	 */
	private static VirtualMachine searchVM(ServiceInstance si, String searchTarget, String vsBasePath) throws RemoteException {
		VirtualMachine vm = null;
		
		// search all virtual machines of the service instance
		ManagedEntity[] elements = new InventoryNavigator(si.getRootFolder()).searchManagedEntities("VirtualMachine");

		for (ManagedEntity entity : elements){
			VirtualMachine testVm = (VirtualMachine) entity;

			// search for VM with given name in the given folder
			if (testVm.getName().equals(searchTarget) && entity.getParent().getName().equals(vsBasePath)){
				vm = testVm;
				break;
			}
		}
		
		return vm;
	}

	private VirtualMachine cloneVM(String vsBasePath, String vMName, ServiceInstance si, ResourcePool rp, 
			VirtualMachine template) throws VmConfigFault, TaskInProgress, CustomizationFault,
			FileFault, InvalidState, InsufficientResourcesFault, MigrationFault, InvalidDatastore, RuntimeFault,
			RemoteException, InterruptedException, InvalidProperty {
		VirtualMachineCloneSpec cloneSpec = new VirtualMachineCloneSpec();
		VirtualMachineRelocateSpec vmrs = new VirtualMachineRelocateSpec();
		vmrs.setPool(rp.getMOR());
		cloneSpec.setLocation(vmrs);
		cloneSpec.setPowerOn(true);
		cloneSpec.setTemplate(false);

		Task task = template.cloneVM_Task((Folder) template.getParent(), vMName, cloneSpec);

		if (task.waitForTask() == Task.SUCCESS) {
			log.info("Succesfully cloned VM template to '" + vsBasePath + "/" + vMName + "'.");
		} else {
			log.severe("Could not clone VM template to'" + vsBasePath + "/" + vMName + "'. Error: "
					+ task.getTaskInfo().getError().getLocalizedMessage());
			si.getServerConnection().logout();
		}

		VirtualMachine vm = searchVM(si, vMName, vsBasePath);
		return vm;
	}

	private boolean terminateIfExists(ServiceInstance si, String vMName, String vsBasePath)
			throws RemoteException, InvalidProperty, RuntimeFault, TaskInProgress, InvalidState, InterruptedException,
			VimFault {

		log.info("Checking if VM \"" + vMName + "\" already exists.");
		VirtualMachine vm = searchVM(si, vMName, vsBasePath);

		if (vm != null) {

			log.info("VM '" + vsBasePath + "/" + vMName + "' already exists. Trying to delete...");

			// stop the VM
			if (vm.getRuntime().getPowerState() != VirtualMachinePowerState.poweredOff) {
				log.info("VM '" + vsBasePath + "/" + vMName + "' is running. Trying to stop...");
				Task task = vm.powerOffVM_Task();
				if (task.waitForTask() == Task.SUCCESS) {
					log.info("Succesfully stopped VM '" + vsBasePath + "/" + vMName + "'.");
				} else {
					log.severe("Could not stop VM '" + vsBasePath + "/" + vMName + "'.");
					return false;
				}
			}

			// delete the VM
			Task task = vm.destroy_Task();
			if (task.waitForTask() == Task.SUCCESS) {
				log.info("Succesfully destroyed VM '" + vsBasePath + "/" + vMName + "'.");
				return true;
			} else {
				log.severe("Could not destroy VM '" + vsBasePath + "/" + vMName + "'.");
				return false;
			}
		} else {
			log.info("VM \"" + vMName + "\" did not exist.");
			return true;
		}
	}

	private ResourcePool getResourcePool(ServiceInstance si, String vsResourcePool)
			throws InvalidProperty, RuntimeFault, RemoteException {
		log.info("Searching resource pool.");
		ResourcePool rp = (ResourcePool) new InventoryNavigator(si.getRootFolder()).searchManagedEntity("ResourcePool", vsResourcePool);
		if (rp == null) {
			log.severe("Could not find '" + vsResourcePool + "' as resource pool.");
			si.getServerConnection().logout();
		} else {
			log.info("Found resource pool " + vsResourcePool);
		}
		return rp;
	}
}