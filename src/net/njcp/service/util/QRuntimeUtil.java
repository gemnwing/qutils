package net.njcp.service.util;

import java.io.File;

import net.njcp.service.util.I18N;

public class QRuntimeUtil {
	public static int getCallerIndex() {
		int retInt = 2;
		StackTraceElement[] stackTrace = new Throwable().getStackTrace();
		Class<?> caller = null;
		try {
			caller = Class.forName(stackTrace[1].getClassName());
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

	public static String getDisplayVariable() {
		String display = null;
		display = System.getenv("DISPLAY");
		QLog.info(I18N.tr("Display environment variable<{0}>", display));
		return display;
	}

	public static String getJarPath(Class<?> clazz) {
		return clazz.getProtectionDomain().getCodeSource().getLocation().getPath();
	}

	public static boolean isJarFileInTheRightPlace(String regex, Object... objects) {
		boolean areThose = true;
		if ( objects == null || objects.length == 0 ) {
			QLog.warn(I18N.tr("null or empty"));
			return areThose;
		}
		for ( Object object : objects ) {
			Class<?> clazz = null;
			if ( object != null ) {
				if ( object instanceof Class<?> ) {
					clazz = (Class<?>) object;
				} else if ( object instanceof String ) {
					try {
						clazz = Class.forName((String) object);
					} catch ( ClassNotFoundException e ) {
						QLog.error(I18N.tr("Class<{0}> not found.", (String) object));
						areThose &= false;
						continue;
					}
				}
			} else {
				QLog.warn(I18N.tr("one of class is null"));
				areThose &= false;
				continue;
			}
			try {
				boolean isIt = true;
				String jarPath = getJarPath(clazz);
				String logStr = I18N.tr("Class<{0}>, path<{1}>, ", clazz.getName(), jarPath);
				if ( jarPath.endsWith(".jar") ) {
					isIt = jarPath.matches(regex);
					if ( !isIt ) {
						QLog.error(I18N.tr("{0}not in the right place.", logStr));
					} else {
						QLog.info(I18N.tr("{0}seems correct.", logStr));
					}
				} else {
					QLog.info(I18N.tr("{0}not a jar file.", logStr));
				}
				areThose &= isIt;
			} catch ( Throwable e ) {
				QLog.error(I18N.tr("Failed to get path of Class<{0}>", clazz.getName()), e);
				areThose &= false;
			}
		}
		return areThose;
	}

	public static void checkDir(Object paramDir, boolean createIfNotExist) {
		if ( paramDir == null ) {
			throw new NullPointerException();
		}
		File dir = null;
		if ( paramDir instanceof File ) {
			dir = (File) paramDir;
		} else {
			dir = new File(String.valueOf(paramDir));
		}
		if ( dir.isDirectory() ) {
			QLog.debug(I18N.tr("Directory \"{0}\" exists.", dir.getAbsolutePath()));
			return;
		}
		if ( createIfNotExist ) {
			if ( dir.exists() ) {
				QLog.debug(I18N.tr("\"{0}\" is a file.", dir.getAbsolutePath()));
				if ( !dir.delete() ) {
					QLog.error(I18N.tr("Failed to remove file \"{0}\".", dir.getAbsolutePath()));
					return;
				}
			}
			if ( !dir.mkdirs() ) {
				QLog.error(I18N.tr("Failed to create directory \"{0}\".", dir.getAbsolutePath()));
				return;
			}
			QLog.info(I18N.tr("Directory \"{0}\" created.", dir.getAbsolutePath()));
		}
	}

	public static void main(String[] args) {
		System.out.println(getCallerIndex());
	}

}
