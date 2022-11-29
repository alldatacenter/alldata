package cn.datax.common.exception;

import cn.datax.common.core.R;
import cn.datax.common.utils.ThrowableUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * 处理自定义异常
	 */
	@ExceptionHandler(DataException.class)
	public R handleWithException(DataException e) {
		log.error("自定义异常信息 ex={},StackTrace={}", e.getMessage(), ThrowableUtil.getStackTrace(e));
		return R.error(e.getMessage());
	}

	/**
	 * 请求的 JSON 参数在请求体内的参数校验
	 */
	@ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
	public R handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
		String message = StringUtils.collectionToCommaDelimitedString(
				e.getBindingResult().getFieldErrors()
						.stream()
						.map(FieldError::getDefaultMessage)
						.collect(Collectors.toList()));
		log.error("参数校验异常信息 ex={},StackTrace={}", e.getMessage(), ThrowableUtil.getStackTrace(e));
		return R.error(message);
	}

	/**
	 * 请求的 URL 参数检验
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	public R handleConstraintViolationException(ConstraintViolationException e) {
		String message = StringUtils.collectionToCommaDelimitedString(
				e.getConstraintViolations()
						.stream()
						.map(ConstraintViolation::getMessage)
						.collect(Collectors.toList()));
		log.error("参数校验异常信息 ex={},StackTrace={}", e.getMessage(), ThrowableUtil.getStackTrace(e));
		return R.error(message);
	}

	@ExceptionHandler(Exception.class)
	public R handleException(Exception e) {
		log.error("全局异常信息ex={},StackTrace={}", e.getMessage(), ThrowableUtil.getStackTrace(e));
		return R.error(e.getMessage());
	}

}