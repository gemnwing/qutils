package net.njcp.service.util;

import java.util.concurrent.TimeUnit;

public class Test {
	public static void main(String[] args) {
		QLog.getRootLogger();
		QLog.setUsingSimpleClassName(false);
		Runnable r = new Runnable() {

			@Override
			public void run() {
				System.out.println(System.out.hashCode());
				QLog.setLog("foobar");
				QLog.setDebugFlag(true);
				QLog.Level.DEBUG.replaceDesc("TMRDEBUG");
				QLog.setSaveDebugAsInfo(false);
				QLog.setSaveStackTraceInLog(false);
				// setCustomPrefix("CUSTOM");
				QLog.println(System.out.hashCode());
				QLog.println("Test " + Thread.currentThread().getName() + "" + Thread.currentThread().getName());
				QLog.trace("Test " + Thread.currentThread().getName() + " trace.");
				QLog.debug("Test " + Thread.currentThread().getName() + " debug.");
				QLog.info("Test " + Thread.currentThread().getName() + " info.");
				QLog.warn("Test " + Thread.currentThread().getName() + " warn.");
				QLog.error("Test " + Thread.currentThread().getName() + " error.", new Exception("Test " + Thread.currentThread().getName() + " exception."));
				try {
					Thread.sleep(2 * 1000);
				} catch ( InterruptedException e ) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				QLog.checkDebugFlag();
				QLog.println(System.out.hashCode());
				QLog.setSaveStackTraceInLog(false);
				QLog.println("Test " + Thread.currentThread().getName() + " 1");
				QLog.trace("Test " + Thread.currentThread().getName() + " trace 1.");
				QLog.debug("Test " + Thread.currentThread().getName() + " debug 1.");
				QLog.info("Test " + Thread.currentThread().getName() + " info 1.");
				QLog.warn("Test " + Thread.currentThread().getName() + " warn 1.");
				QLog.error("Test " + Thread.currentThread().getName() + " error 1.", new Exception("Test " + Thread.currentThread().getName() + " exception 1."));
				QLog.println(System.out.hashCode());
			}

		};
		QLog.setDebugFile("conf/debug");
		QLog.setSaveDebugLogExclusively(true);
		QLog.setOverrunThreshold(2, TimeUnit.SECONDS);
		new Thread(r).start();
		new Thread(r).start();
	}
}
