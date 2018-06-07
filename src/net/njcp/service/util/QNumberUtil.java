package net.njcp.service.util;

public class QNumberUtil {
	public static Integer toInteger(Object o) {
		Integer ret = null;
		if ( o != null ) {
			try {
				if ( o instanceof Number ) {
					ret = ((Number) o).intValue();
				} else {
					ret = Integer.valueOf(String.valueOf(o));
				}
			} catch ( Throwable t ) {

			}
		}
		return ret;
	}
}
