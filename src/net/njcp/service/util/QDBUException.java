package net.njcp.service.util;

@SuppressWarnings("serial")
public class QDBUException extends Exception {
	private Throwable cause = null;

	public QDBUException() {
		super();
	}

	public QDBUException(String s) {
		super(s);
	}

	public QDBUException(String s, String s1) {
		super(s);
		this.cause = new QDBUException(s1);
	}

	public QDBUException(String s, Throwable e) {
		super(s);
		this.cause = e;
	}

	public Throwable getCause() {
		return this.cause;
	}
}
