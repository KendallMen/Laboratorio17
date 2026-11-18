package cr.ac.ucr.if0009.insecurepay.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccesoDenegadoException.class)
    public ProblemDetail handleAccesoDenegado(AccesoDenegadoException exception) {
        return problem(HttpStatus.FORBIDDEN, "Acceso denegado");
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail handleRecursoNoEncontrado(RecursoNoEncontradoException exception) {
        return problem(HttpStatus.NOT_FOUND, "Recurso no encontrado");
    }

    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail handleErrorInterno(RuntimeException exception) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible procesar la solicitud");
    }

    private ProblemDetail problem(HttpStatus status, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        return problem;
    }
}
