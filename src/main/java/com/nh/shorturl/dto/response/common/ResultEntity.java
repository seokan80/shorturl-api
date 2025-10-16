package com.nh.shorturl.dto.response.common;

import com.nh.shorturl.type.ApiResult;
import org.springframework.http.HttpStatus;

import java.io.Serializable;
import java.util.Map;

/**
 * Rest Controller Json 데이터 리턴용
 */
public class ResultEntity<T> implements Serializable {

	private static final long serialVersionUID = -3104101773843743888L;

	private String code = ApiResult.SUCCESS.getCode();
	private String message = ApiResult.SUCCESS.getMessage();
	private T data;

	public ResultEntity() {
	}

	public ResultEntity(String code, String message) {
		this.code = code;
		this.message = message;
	}

	public ResultEntity(T data) {
		this.data = data;
	}

	public ResultEntity(String code, String message, final T data) {
		this(code, message);
		this.data = data;
	}

	public static ResultEntity create() {
		return new ResultEntity();
	}

	public static ResultEntity of(String code, String message) {
		return new ResultEntity(code, message);
	}

	public static ResultEntity of(ApiResult apiResult) {
		return create().apiResult(apiResult);
	}

	public static ResultEntity ok() {
		return new ResultEntity(HttpStatus.OK);
	}

	public static ResultEntity True() {
		return ResultEntity.ok(new Result<>(Boolean.TRUE));
	}

	public static ResultEntity False() {
		return ResultEntity.ok(new Result<>(Boolean.FALSE));
	}

	public static ResultEntity ok(Object data) {
		return new ResultEntity(data);
	}

	public static ResultEntity badRequest() {
		return new ResultEntity().setCode(ApiResult.CODE_BAD_REQUEST);
	}

	public ResultEntity apiResult(ApiResult apiResult) {
		this.code = apiResult.getCode();
		this.message = apiResult.getMessage();
		return this;
	}

	public ResultEntity data(T data) {
		this.data = data;
		return this;
	}

	public static ResultEntity fail(Map<String, Object> data) {
		return new ResultEntity()
			.setCode(ApiResult.FAIL.getCode())
			.setData(data);
	}

	public ResultEntity fail(String message) {
		this.code = ApiResult.FAIL.getCode();
		this.message = message;
		return this;
	}

	public ResultEntity fail(ApiResult apiResult, String message) {
		this.code = apiResult.getCode();
		this.message = message;
		return this;
	}

	public ResultEntity invalid(String message) {
		this.code = ApiResult.CODE_INVALID;
		this.message = message;
		return this;
	}

	public ResultEntity notFound(String message) {
		this.code = ApiResult.CODE_NOT_FOUND;
		this.message = message;
		return this;
	}

	public ResultEntity noAccess(String message) {
		this.code = ApiResult.FORBIDDEN.getCode();
		this.message = message;
		return this;
	}

	public String getCode() {
		return code;
	}

	public ResultEntity<T> setCode(String code) {
		this.code = code;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public ResultEntity<T> setMessage(String message) {
		this.message = message;
		return this;
	}

	public T getData() {
		return data;
	}

	public ResultEntity<T> setData(T data) {
		this.data = data;
		return this;
	}
}
