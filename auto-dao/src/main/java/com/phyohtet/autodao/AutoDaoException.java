package com.phyohtet.autodao;

public class AutoDaoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public AutoDaoException() {
		super();
	}

	public AutoDaoException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public AutoDaoException(String message, Throwable cause) {
		super(message, cause);
	}

	public AutoDaoException(Throwable cause) {
		super(cause);
	}
	
	public AutoDaoException(String message) {
		super(message);
	}

}
