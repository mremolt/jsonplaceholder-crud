package gmbh.tantive.jsonplaceholder_crud.api;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import jakarta.validation.ConstraintViolationException;
import gmbh.tantive.jsonplaceholder_crud.shared.InvalidPageException;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceNotFoundException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(InvalidPageException.class)
    public ProblemDetail handleInvalidPage(InvalidPageException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(InvalidPatchException.class)
    public ProblemDetail handleInvalidPatch(InvalidPatchException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(DataAccessResourceFailureException.class)
    public ProblemDetail handleUnavailableDatabase(DataAccessResourceFailureException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "The database is temporarily unavailable");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request content.");
        List<Map<String, String>> errors = exception.getConstraintViolations().stream()
                .map(violation -> Map.of("field", violation.getPropertyPath().toString(),
                        "message", violation.getMessage()))
                .sorted(Comparator.comparing(error -> error.get("field")))
                .toList();
        problem.setProperty("errors", errors);
        return problem;
    }
}
