package io.github.carlosphenomenal;

import lombok.Builder;
import lombok.Getter;

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
