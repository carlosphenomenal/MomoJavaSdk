package io.github.carlosphenomenal.Dtos;

import lombok.Builder;
import lombok.Getter;

/**
 * Represents a refund request.
 *
 * @author Carlos Amanya
 */
@Getter
@Builder
public class RefundRequest {
    private String amount;
    private String externalId;
    private String payerMessage;
    private String payeeNote;
    private String referenceIdToRefund;
}
