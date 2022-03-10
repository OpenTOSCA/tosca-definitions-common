package org.opentosca.artifacttemplates.openstack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class OpenStackCloudProviderInterfaceApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(OpenStackCloudProviderInterfaceApplication.class, args);
    }
}
