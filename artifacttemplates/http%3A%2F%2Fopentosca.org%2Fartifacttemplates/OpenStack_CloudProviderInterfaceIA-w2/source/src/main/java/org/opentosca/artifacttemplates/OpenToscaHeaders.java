package org.opentosca.artifacttemplates;

import java.util.Map;

import javax.xml.namespace.QName;

public record OpenToscaHeaders(String messageId,
                               String replyTo,
                               Map<QName, Map<String, String>> deploymentArtifactsMap) {
}
