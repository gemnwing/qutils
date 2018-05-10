package net.njcp.service.util;

@SuppressWarnings("serial")
public class QSFtpException extends Exception {
	private Throwable cause = null;

	public QSFtpException() {
		super();
	}

	public QSFtpException(String s) {
		super(s);
	}

	public QSFtpException(String s, String s1) {
		super(s);
		this.cause = new QSFtpException(s1);
	}

	public QSFtpException(String s, Throwable e) {
		super(s);
		this.cause = e;
	}

	public Throwable getCause() {
		return this.cause;
	}
}
