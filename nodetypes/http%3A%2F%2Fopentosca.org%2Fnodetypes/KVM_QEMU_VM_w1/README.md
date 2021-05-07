
# KVM/QEMU Virtual Machine Node Type

## Prerequisite

There must be a `hosted_on` relation to a KVM/QEMU Hypervisor node.

The created virtual machine will be configured for username/password SSH authentication. 

## Properties

* `VMIP`: IP address of the virtual machine (instance property) 
* `VMInstanceID`: Instance ID of the virtual machine (instance property)
* `VMMAC`: MAC address of the virtual machine (instance property)
* `VMUserName`: Username for SSH login into the virtual machine
* `VMUserPassword`: User's password for SSH login into the virtual machine
* `VMDiskSize`: Disk size for the newly created virtual machine
* `VMVCPUS`: Virtual CPUs for the newly created virtual machine
* `VMRAM`: Virtual memory  for the newly created virtual machine, e.g., 1024
* `VMOSType`: OS type for the newly created virtual machine, e.g., "linux"
* `VMOSVariant`: OS variant for the newly created virtual machine, e.g., "ubuntu16.04"

## Interfaces

* `transferFile`
  * Input parameters
    * `VMIP`: IP address of the virtual machine (instance property)
    * `VMUserName`: Username for SSH login into the virtual machine
    * `VMUserPassword`: User's password for SSH login into the virtual machine
    * `SourcePath`: Location of the file that will be transferred to the virtual machine 
    * `TargetPath`: Target location inside the virtual machine
  * Output parameters
    * `TransferFileResult`: Result as string representation
* `runScript`
  * Input parameters
    * `VMIP`: IP address of the virtual machine (instance property)
    * `VMUserName`: Username for SSH login into the virtual machine
    * `VMUserPassword`: User's password for SSH login into the virtual machine
    * `Script`: Script value to be executed on the virtual machine
  * Output parameters
    * `ScriptResult`: Result as string representation
* `waitForAvailability`
  * Input parameters
    * `VMIP`: IP address of the virtual machine (instance property)
    * `VMUserName`: Username for SSH login into the virtual machine
    * `VMUserPassword`: User's password for SSH login into the virtual machine
  * Output parameters
    * `WaitResult`: Result as string representation

## Limitations

* Currently no deployment artifact can be specified to supply a custom image.
