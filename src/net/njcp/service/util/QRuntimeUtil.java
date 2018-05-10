package net.njcp.service.util;

public class QRuntimeUtil {
	public static int getCallerIndex() {
		int retInt = 3;
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		Class<?> caller = null;
		try {
			caller = Class.forName(stackTrace[2].getClassName());
		} catch ( ClassNotFoundException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for ( int i = 1; i < stackTrace.length; i++ ) {
			StackTraceElement element = stackTrace[i];
			if ( element.getClass().equals(caller) ) {
				return Math.min(stackTrace.length - 1, i + 1);
			}
		}
		return retInt;
	}

	public static void main(String[] args) {
		System.out.println(getCallerIndex());
	}
}
