package io.github.carlosphenomenal.Dtos;

import lombok.Builder;
import lombok.Getter;

/**
 * Internal representation of the request to pay body sent to the MoMo API.
 *
 * @author Carlos Amanya
 */
@Getter
@Builder
public class RequestToPayBody {
    private String amount;
    private String currency;
    private String externalId;
    private PaymentRequest.Payer payer;
    private String payerMessage;
    private String payeeNote;
}
