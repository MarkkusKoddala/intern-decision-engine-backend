package ee.taltech.inbankbackend.endpoint;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

/**
 * Holds the response data of the REST endpoint.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DecisionResponse {
    private Integer loanAmount;
    private Integer loanPeriod;
    private String errorMessage;
}
