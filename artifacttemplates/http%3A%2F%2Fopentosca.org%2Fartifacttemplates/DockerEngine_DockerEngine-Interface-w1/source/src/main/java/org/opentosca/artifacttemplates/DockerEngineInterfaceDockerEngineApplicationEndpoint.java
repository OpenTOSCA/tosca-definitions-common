package org.opentosca.artifacttemplates;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import org.opentosca.nodetypes.Country;
import org.opentosca.nodetypes.Currency;
import org.opentosca.nodetypes.GetCountryRequest;
import org.opentosca.nodetypes.GetCountryResponse;

@Endpoint
public class DockerEngineInterfaceDockerEngineApplicationEndpoint {

	@PayloadRoot(namespace = Constants.NAMESPACE_URI, localPart = "getCountryRequest")
	@ResponsePayload
	public GetCountryResponse getCountry(@RequestPayload GetCountryRequest request) {
		GetCountryResponse response = new GetCountryResponse();

        Country poland = new Country();
        poland.setName("Poland");
        poland.setCapital("Warsaw");
        poland.setCurrency(Currency.PLN);
        poland.setPopulation(38186860);

		response.setCountry(poland);

		return response;
	}
}
