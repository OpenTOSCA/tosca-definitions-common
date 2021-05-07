package org.opentosca.nodetypeimplementations;

import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.ws.WebServiceContext;

import org.eclipse.winery.generators.ia.jaxws.Headers;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Mockito.when;

// @Ignore
@RunWith(PowerMockRunner.class)
@PrepareForTest(
        {WebServiceContext.class, Headers.class}
)
@PowerMockIgnore("javax.*")
public class CloudProviderInterfaceTests {

    @Mock
    WebServiceContext ctx;

    @InjectMocks
    private org_opentosca_nodetypes_KVM_QEMU_Hypervisor__CloudProviderInterface service
            = new org_opentosca_nodetypes_KVM_QEMU_Hypervisor__CloudProviderInterface();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(Headers.class);
        when(Headers.asList(ctx)).thenReturn(new ArrayList<>());
        when(Headers.asMap(ctx)).thenReturn(new HashMap<>());
    }

    @Test
    public void testCreateVM() {
        service.createVM("129.69.214.215",
                "wursteml",
                "installed",
                "br0",
                "wursteml",
                "installed",
                "8G",
                "2",
                "1024",
                "linux",
                "ubuntu16.04");
    }

    @Test
    public void testTerminateVM() {
        service.terminateVM("129.69.214.250",
                "wursteml",
                "installed",
                "256d5a4c-6f2c-4f11-8ed2-fd18ce5e33fd");
    }

    @Test
    public void testCanConnectAndLogin() {
        SshTemplate ssh = new SshTemplate("129.69.214.255", "wursteml", "installed");
        Assert.assertTrue(ssh.canConnect());
        Assert.assertTrue(ssh.canLogin());
    }
}
