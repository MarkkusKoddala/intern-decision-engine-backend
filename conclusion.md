# Conclusion for TICKET-101 Review

## Issues

### Shared "DecisionResponse" instance
In the DecisionEngineController, the DecisionResponse is autowired as a single instance shared across all requests. 
This approach is problematic as it could lead to data leakage, especially in a multithreaded environment. 
A more secure and reliable solution would be to instantiate a new DecisionResponse for every request, ensuring data integrity and isolation between requests.

### Decision engine does not return maximum possible loan amount

The current implementation of the decision engine fails to always return the maximum possible loan amount that the engine could approve based on the customer's profile and credit history. Instead, it only considers the requested amount, potentially underestimating what the customer might be eligible for. This could mislead customers about their borrowing capabilities and affect their decision-making.

### Overloaded calculateApprovedLoan method
The calculateApprovedLoan method in the DecisionEngine class is responsible for too many tasks, 
which affects readability and maintainability. Refactoring this method to delegate specific tasks to helper 
methods would improve readability and make the codebase easier to manage.

### Inefficient while loop in calculateApprovedLoan
The loop in calculateApprovedLoan increments the loanPeriod until a valid loan amount is found. This loop can be 
inefficient and potentially run many times, especially when approaching the upper boundary of the valid period. 
Such design could lead to performance degradation under heavy load or with large data sets.

### Use of @Component in DecisionRespone
Avoid using @Component and similar annotations on DTOs like DecisionResponse. These objects are intended only to transfer data and should not be 
managed within Spring's application context. Keeping DTOs simple and outside of Spring management reduces complexity and enhances performance by adhering to the principle of separation of concerns.
## Well done aspects

### Strong error handling strategy
The implementation demonstrates a strong error handling strategy where specific exceptions are thrown for different error conditions (e.g., InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException). 

### Comments
The code is well-documented with clear and descriptive comments that effectively explain the purpose and functionality of each component and method. 

### Adherence to SOLID Principles
Overall, the code adheres well to the SOLID principles, making it readable and simple.

### Clear Separation of Internal and External Data Handling
The use of Decision for internal business logic and DecisionResponse for API outputs ensures a clear separation of concerns. This distinct structure prevents data leaks and supports scalability and adaptability, allowing internal logic to evolve without impacting how data is presented to users. 
This separation enhances system maintainability and the ease of introducing updates.


# Most Important Shortcoming: Shared "DecisionResponse" Instance
Explanation:

Data Integrity Risks: Autowiring a single DecisionResponse instance shared across all incoming HTTP requests in a controller is highly problematic. In a multi-threaded environment, such as that of a web server handling simultaneous requests, this shared instance can lead to severe data leaks where one user's response data gets overwritten by another's. This not only breaches user data privacy but can also lead to incorrect data being sent to users, undermining the reliability of the application. <br>
Concurrency Issues: The shared instance can suffer from race conditions, where multiple threads attempt to modify this instance at the same time, leading to unpredictable results and making the system unstable and unreliable.