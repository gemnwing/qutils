package net.njcp.service.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import net.njcp.service.util.I18N;

public class QFileUtil {
	public static boolean copyFile(File source, File dest) {
		return copyFile(source, dest, false);
	}

	public static boolean copyFile(File source, File dest, boolean removeDestOnExistance) {
		if ( source == null || dest == null ) {
			return false;
		}
		if ( !source.exists() ) {
			QLog.error(I18N.tr("Source \"{0}\" does not exist.", source.getAbsolutePath()));
			return false;
		}
		if ( !source.isFile() ) {
			QLog.error(I18N.tr("Source \"{0}\" is not a file.", source.getAbsolutePath()));
			return false;
		}
		if ( dest.exists() ) {
			if ( removeDestOnExistance ) {
				QLog.debug(I18N.tr("Destination \"{0}\" exists, removing...", dest.getAbsolutePath()));
				if ( !dest.delete() ) {
					QLog.error(I18N.tr("Failed to remove existing destination \"{0}\".", dest.getAbsolutePath()));
					return false;
				}
			} else {
				QLog.error(I18N.tr("Destination \"{0}\" exists.", dest.getAbsolutePath()));
				return false;
			}
		}
		boolean retFlag = true;
		FileInputStream fis = null;
		FileOutputStream fos = null;
		String mark = I18N.tr("Copying \"{0}\" to \"{1}\"", source.getAbsolutePath(), dest.getAbsolutePath());
		QTimer.setTimeMark(mark);
		try {
			fis = new FileInputStream(source);
			fos = new FileOutputStream(dest);
			byte[] buff = new byte[1024];
			int bytesRead;
			while ( (bytesRead = fis.read(buff)) > 0 ) {
				fos.write(buff, 0, bytesRead);
			}
		} catch ( Throwable t ) {
			retFlag = false;
		} finally {
			try {
				fis.close();
			} catch ( Throwable t ) {
				retFlag = false;
			}
			try {
				fos.close();
			} catch ( Throwable t ) {
				retFlag = false;
			}
		}
		QTimer.showTimeElapsed(mark);
		return retFlag;
	}
}
