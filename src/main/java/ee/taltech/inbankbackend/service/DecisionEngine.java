package ee.taltech.inbankbackend.service;

import com.github.vladislavgoltjajev.personalcode.locale.estonia.EstonianPersonalCodeValidator;
import ee.taltech.inbankbackend.config.DecisionEngineConstants;
import ee.taltech.inbankbackend.exceptions.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

import static ee.taltech.inbankbackend.config.DecisionEngineConstants.MAX_AGE;
import static ee.taltech.inbankbackend.config.DecisionEngineConstants.MIN_AGE;

/**
 * A service class that provides a method for calculating an approved loan amount and period for a customer.
 * The loan amount is calculated based on the customer's credit modifier,
 * which is determined by the last four digits of their ID code.
 */
@Service
public class DecisionEngine {

    // Used to check for the validity of the presented ID code.
    private final EstonianPersonalCodeValidator validator = new EstonianPersonalCodeValidator();
    private int creditModifier = 0;

    /**
     * Calculates the maximum loan amount and period for the customer based on their ID code,
     * the requested loan amount and the loan period.
     * The loan period must be between 12 and 60 months (inclusive).
     * The loan amount must be between 2000 and 10000€ months (inclusive).
     *
     * @param personalCode ID code of the customer that made the request.
     * @param loanAmount Requested loan amount
     * @param loanPeriod Requested loan period
     * @return A Decision object containing the approved loan amount and period, and an error message (if any)
     * @throws InvalidPersonalCodeException If the provided personal ID code is invalid
     * @throws InvalidLoanAmountException If the requested loan amount is invalid
     * @throws InvalidLoanPeriodException If the requested loan period is invalid
     * @throws NoValidLoanException If there is no valid loan found for the given ID code, loan amount and loan period
     */
    public Decision calculateApprovedLoan(String personalCode, Long loanAmount, int loanPeriod)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException,
            NoValidLoanException, InvalidAgeException {
        try {
            verifyInputs(personalCode, loanAmount, loanPeriod);
        } catch (Exception e) {
            return new Decision(null, null, e.getMessage());
        }

        int outputLoanAmount;
        creditModifier = getCreditModifier(personalCode);

        if (creditModifier == 0) {
            throw new NoValidLoanException("No valid loan found!");
        }

        while (highestValidLoanAmount(loanPeriod) < DecisionEngineConstants.MINIMUM_LOAN_AMOUNT) {
            loanPeriod++;
        }

        if (loanPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD) {
            outputLoanAmount = Math.min(DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT, highestValidLoanAmount(loanPeriod));
        } else {
            throw new NoValidLoanException("No valid loan found!");
        }

        return new Decision(outputLoanAmount, loanPeriod, null);
    }

    /**
     * Calculates the largest valid loan for the current credit modifier and loan period.
     *
     * @return Largest valid loan amount
     */
    private int highestValidLoanAmount(int loanPeriod) {
        return creditModifier * loanPeriod;
    }

    /**
     * Calculates the credit modifier of the customer to according to the last four digits of their ID code.
     * Debt - 0000...2499
     * Segment 1 - 2500...4999
     * Segment 2 - 5000...7499
     * Segment 3 - 7500...9999
     *
     * @param personalCode ID code of the customer that made the request.
     * @return Segment to which the customer belongs.
     */
    private int getCreditModifier(String personalCode) {
        int segment = Integer.parseInt(personalCode.substring(personalCode.length() - 4));

        if (segment < 2500) {
            return 0;
        } else if (segment < 5000) {
            return DecisionEngineConstants.SEGMENT_1_CREDIT_MODIFIER;
        } else if (segment < 7500) {
            return DecisionEngineConstants.SEGMENT_2_CREDIT_MODIFIER;
        }

        return DecisionEngineConstants.SEGMENT_3_CREDIT_MODIFIER;
    }

    /**
     * Verify that all inputs are valid according to business rules.
     * If inputs are invalid, then throws corresponding exceptions.
     *
     * @param personalCode Provided personal ID code
     * @param loanAmount Requested loan amount
     * @param loanPeriod Requested loan period
     * @throws InvalidPersonalCodeException If the provided personal ID code is invalid
     * @throws InvalidLoanAmountException If the requested loan amount is invalid
     * @throws InvalidLoanPeriodException If the requested loan period is invalid
     */
    private void verifyInputs(String personalCode, Long loanAmount, int loanPeriod)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException, InvalidAgeException {

        if (!validator.isValid(personalCode)) {
            throw new InvalidPersonalCodeException("Invalid personal ID code!");
        }
        if (!(DecisionEngineConstants.MINIMUM_LOAN_AMOUNT <= loanAmount)
                || !(loanAmount <= DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT)) {
            throw new InvalidLoanAmountException("Invalid loan amount!");
        }
        if (!(DecisionEngineConstants.MINIMUM_LOAN_PERIOD <= loanPeriod)
                || !(loanPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD)) {
            throw new InvalidLoanPeriodException("Invalid loan period!");
        }
        if (!isEligibleByAge(personalCode)) {
            throw new InvalidAgeException("Invalid age!");
        }
    }

    /**
     * Determines if a customer is eligible based on their age derived from their personal code.
     * This method parses the birthdate from the personal code, calculates the age, and checks it against defined minimum and maximum age limits.
     *
     * @param personalCode The personal code of the customer, from which the birthdate is extracted.
     * @return true if the customer's age is within the eligible range, false otherwise.
     */
    private boolean isEligibleByAge(String personalCode) {
        LocalDate birthdate = parseBirthdateFromId(personalCode);
        System.out.println(birthdate);
        Period age = Period.between(birthdate, LocalDate.now());
        return age.getYears() >= MIN_AGE && age.getYears() <= MAX_AGE;
    }

    /**
     * Parses the birthdate from the given personal code by determining the century and extracting the date.
     * The method first extracts the identifier to determine the century, parses the year, month, and day,
     * and then adjusts the birth year based on the century before returning the complete birthdate.
     *
     * @param personalCode The personal code from which to extract the birthdate.
     * @return A LocalDate representing the customer's birthdate.
     */
    public LocalDate parseBirthdateFromId(String personalCode) {
        int identifier = Integer.parseInt(personalCode.substring(0, 1));
        String birthdateStr = personalCode.substring(1, 7);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuMMdd");

        LocalDate birthdate = LocalDate.parse(birthdateStr, formatter);

        int centuryAdjustment = determineCenturyAdjustment(identifier, birthdate.getYear());
        birthdate = birthdate.minusYears(centuryAdjustment);

        return birthdate;
    }

    /**
     * Determines the number of years to adjust the parsed year based on the century identifier.
     * The adjustment corrects for century changes as the identifier signifies different centuries.
     *
     * @param identifier The first digit of the personal code, indicating the century and sometimes gender.
     * @param yearParsed The initially parsed year, which may need adjustment to reflect the correct century.
     * @return The number of years to subtract from the parsed year to obtain the correct birth year.
     * @throws IllegalArgumentException if the identifier does not correspond to a valid century marker.
     */
    private int determineCenturyAdjustment(int identifier, int yearParsed) {
        if (identifier >= 1 && identifier <= 2) {
            return (yearParsed >= 2000) ? 200 : 100;
        } else if (identifier >= 3 && identifier <= 4) {
            return (yearParsed >= 2000) ? 100 : 0;
        } else if (identifier >= 5 && identifier <= 6) {
            return 0;
        } else {
            throw new IllegalArgumentException("Invalid identifier for century in Estonian personal ID code!");
        }
    }

}
