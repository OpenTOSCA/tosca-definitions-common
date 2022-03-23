package org.opentosca.artifacttemplates.openstack;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

@EnableWs
@Configuration
public class WebServiceConfig extends WsConfigurerAdapter {

    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(ApplicationContext applicationContext) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/*");
    }

    @Bean(name = OpenStackConstants.PORT_TYPE_NAME)
    public DefaultWsdl11Definition defaultWsdl11Definition(XsdSchema openstackCloudProviderInterfaceSchema) {
        DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
        wsdl11Definition.setPortTypeName(OpenStackConstants.PORT_TYPE_NAME);
        wsdl11Definition.setLocationUri("/");
        wsdl11Definition.setTargetNamespace(OpenStackConstants.NAMESPACE_URI);
        wsdl11Definition.setSchema(openstackCloudProviderInterfaceSchema);
        return wsdl11Definition;
    }

    @Bean
    public XsdSchema openstackCloudProviderInterfaceSchema() {
        return new SimpleXsdSchema(new ClassPathResource(OpenStackConstants.XSD_NAME));
    }
}
