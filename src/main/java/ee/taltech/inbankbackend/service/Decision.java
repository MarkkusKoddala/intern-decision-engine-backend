package ee.taltech.inbankbackend.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Holds the response data of the REST endpoint.
 */
@Getter
@AllArgsConstructor
public class Decision {
    private final Integer loanAmount;
    private final Integer loanPeriod;
    private final String errorMessage;
}
