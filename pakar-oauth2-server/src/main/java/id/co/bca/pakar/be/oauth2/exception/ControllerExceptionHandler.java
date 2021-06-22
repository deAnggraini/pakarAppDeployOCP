package id.co.bca.pakar.be.oauth2.exception;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import id.co.bca.pakar.be.oauth2.common.Constant;

@ControllerAdvice
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler {
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {

		Map<String, Object> body = new LinkedHashMap<>();

		// Get all errors
		List<String> errors = ex.getBindingResult().getFieldErrors().stream().map(x -> x.getDefaultMessage())
				.collect(Collectors.toList());
//
//		body.put("errors", errors);

		logger.info("validate failed "+errors);
        HashMap<String, String> responseStatus = new HashMap<>();
        responseStatus.put("code", Constant.LoginStatus.INCORRECT_USER_PASSWORD_CODE);
        responseStatus.put("message", Constant.LoginStatus.INCORRECT_USER_PASSWORD_MESSAGE);

        body.put("data", 0);
        body.put("status", responseStatus);

		return new ResponseEntity<>(body, headers, HttpStatus.OK);
	}
}
