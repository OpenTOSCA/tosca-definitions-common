#!/bin/bash

VMIDDefault=`uuidgen`
VMDiskSizeDefault="4G"
VMUserNameDefault="opentosca"
VMUserPasswordDefault="opentosca"
VMVCPUSDefault="1"
VMRAMDefault="512"
VMOSTypeDefault="linux"
VMOSVariantDefault="ubuntu16.04"
HypervisorBridgeInterfaceDefault="br0"

VMID=${VMID:-$VMIDDefault}
VMDiskSize=${VMDiskSize:-$VMDiskSizeDefault}
VMUserName=${VMUserName:-$VMUserNameDefault}
VMUserPassword=${VMUserPassword:-$VMUserPasswordDefault}
VMVCPUS=${VMVCPUS:-$VMVCPUSDefault}
VMRAM=${VMRAM:-$VMRAMDefault}
VMOSType=${VMOSType:-$VMOSTypeDefault}
VMOSVariant=${VMOSVariant:-$VMOSVariantDefault}
HypervisorBridgeInterface=${HypervisorBridgeInterface:-$HypervisorBridgeInterfaceDefault}

wget https://cloud-images.ubuntu.com/xenial/current/xenial-server-cloudimg-amd64-disk1.img
qemu-img convert -O qcow2 xenial-server-cloudimg-amd64-disk1.img ${VMID}.qcow2
qemu-img resize ${VMID}.qcow2 +${VMDiskSize}
qemu-img create -f qcow2 -b ${VMID}.qcow2 ${VMID}.img

cat > user-data <<EOF
#cloud-config
password: ${VMUserPassword}
chpasswd: { expire: False }
ssh_pwauth: True
users:
- default
- name: ${VMUserName}
  gecos: OpenTOSCA User
  lock_passwd: false
  sudo: ALL=(ALL) NOPASSWD:ALL
  passwd: `openssl passwd -1 -salt opentosca ${VMUserPassword}`
EOF

cat > meta-data <<EOF
instance-id: ${VMID}
network-interfaces: |
  auto ens2
  iface ens2 inet dhcp
hostname: ubuntu
local-hostname: ubuntu
EOF

cloud-localds ${VMID}-config.img user-data meta-data

virt-install \
    --connect=qemu:///system \
    --name ${VMID} \
    --vcpus ${VMVCPUS} \
    --cpu host-model-only \
    --ram ${VMRAM} \
    --clock offset=utc \
    --network bridge=${HypervisorBridgeInterface} \
    --os-type ${VMOSType} \
    --os-variant ${VMOSVariant} \
    --disk path=${VMID}.img,device=disk,bus=virtio \
    --disk path=${VMID}-config.img,device=cdrom \
    --graphics none \
    --import \
    --noautoconsole \
    --autostart
