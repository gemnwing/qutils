package net.njcp.service.util;

@SuppressWarnings("serial")
public class QFtpException extends Exception {
	private Throwable cause = null;

	public QFtpException() {
		super();
	}

	public QFtpException(String s) {
		super(s);
	}

	public QFtpException(String s, String s1) {
		super(s);
		this.cause = new QFtpException(s1);
	}

	public QFtpException(String s, Throwable t) {
		super(s);
		this.cause = t;
	}

	public Throwable getCause() {
		return this.cause;
	}
}
