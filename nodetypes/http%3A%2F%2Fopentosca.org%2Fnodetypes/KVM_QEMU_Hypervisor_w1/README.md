
# KVM/QEMU Hypervisor Node Type

## Prerequisite

The hypervisor host must be configured for username/password SSH login. 

Install the following packages on your KVM/QEMU host:

```shell
sudo apt-get install libosinfo-bin qemu-kvm libvirt-bin virtinst bridge-utils cpu-checker arpwatch genisoimage cloud-utils cloud-image-utils dos2unix
```

Further, it is required that there is bridge interface available, e.g., use the following configuration:

```shell
sudo /etc/network/interfaces /etc/network/interfaces.save
sudo bash -c "cat > /etc/network/interfaces <<EOF
auto lo
iface lo inet loopback

auto br0
iface br0 inet dhcp
        bridge_ports eth0
        # bridge_stp off
        # bridge_fd 0
        # bridge_maxwait 0
EOF"
sudo /etc/init.d/networking restart
```

In addition, it is recommended to enable `arpwatch` for the interfaces:

```shell
sudo arpwatch -i br0
sudo arpwatch -i eth0
```

## Properties

* `HypervisorEndpoint`: FQDN or IP address of the hypervisor host
* `HypervisorUser`: Username for SSH login
* `HypervisorPassword`: User's password for SSH login
* `HypervisorBridgeInterface`: Name of the configured bridge interface

## Interfaces

* `createVM`
  * Input parameters:
    * `HypervisorEndpoint`: FQDN or IP address of the hypervisor host
    * `HypervisorUser`: Username for SSH login
    * `HypervisorPassword`: User's password for SSH login
    * `HypervisorBridgeInterface`: Name of the configured bridge interface
    * `VMUserName`: Username for the newly created virtual machine
    * `VMUserPassword`: User's password for the newly created virtual machine
    * `VMDiskSize`: Disk size for the newly created virtual machine
    * `VMVCPUS`: Virtual CPUs for the newly created virtual machine
    * `VMRAM`: Virtual memory  for the newly created virtual machine, e.g., 1024
    * `VMOSType`: OS type for the newly created virtual machine, e.g., "linux"
    * `VMOSVariant`: OS variant for the newly created virtual machine, e.g., "ubuntu16.04"
  * Output parameters:    
    * `VMIP`: The IP address of the newly created virtual machine
    * `VMInstanceID`: The instance ID of the newly created virtual machine
    * `VMMAC`: The MAC address of the newly created virtual machine
    * `CreateResult`: Result as string representation 

* `terminateVM`
  * Input parameters
    * `HypervisorEndpoint`: FQDN or IP address of the hypervisor host
    * `HypervisorUser`: Username for SSH login
    * `HypervisorPassword`: User's password for SSH login
    * `VMInstanceID`: The instance ID to terminate                    
  * Output parameters
    * `TerminateResult`: Result as string representation
                    
## Limitations

* The download of the Ubuntu cloud image is currently hard coded into the respective IA
