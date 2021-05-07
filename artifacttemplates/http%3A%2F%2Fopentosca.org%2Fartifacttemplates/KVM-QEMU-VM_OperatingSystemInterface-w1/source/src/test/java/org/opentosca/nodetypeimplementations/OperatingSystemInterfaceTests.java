package org.opentosca.nodetypeimplementations;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.ws.WebServiceContext;

import org.eclipse.winery.generators.ia.jaxws.Headers;

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

@Ignore
@RunWith(PowerMockRunner.class)
@PrepareForTest(
        {WebServiceContext.class, Headers.class}
)
@PowerMockIgnore("javax.*")
public class OperatingSystemInterfaceTests {

    @Mock
    WebServiceContext ctx;

    @InjectMocks
    private org_opentosca_nodetypes_KVM_QEMU_VM__OperatingSystemInterface service
            = new org_opentosca_nodetypes_KVM_QEMU_VM__OperatingSystemInterface();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(Headers.class);
        when(Headers.asList(ctx)).thenReturn(new ArrayList<>());
        when(Headers.asMap(ctx)).thenReturn(new HashMap<>());
    }

    @Test
    public void testRunScriptAndTransferFile() throws Exception {
        String VMIP = "129.69.214.224";
        String VMUserName = "wursteml";
        String VMUserPassword = "installed";
        service.waitForAvailability(VMIP, VMUserName, VMUserPassword);
        service.runScript(VMIP, VMUserName, VMUserPassword, "cd /tmp && pwd && cd && mkdir -p test");
        File file = File.createTempFile("test", "test");
        service.transferFile(VMIP, VMUserName, VMUserPassword, file.getAbsolutePath(), "~/test");
    }
}
