package net.njcp.service.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QWebUtil {
	public static List<String> getKeyFromParam(Map<String, String[]> paramMap, String... keyMatchers) {
		List<String> retIds = new ArrayList<String>();
		for ( String keyMatcher : keyMatchers ) {
			for ( String key : paramMap.keySet() ) {
				if ( key.toLowerCase().matches(keyMatcher + "(\\[\\])*") ) {
					Object val = paramMap.get(key);
					if ( val instanceof String ) {
						retIds.add(String.valueOf(val));
					} else if ( val instanceof String[] ) {
						String[] regionArr = (String[]) val;
						for ( String region : regionArr ) {
							retIds.add(region);
						}
					}
				}
			}
		}
		return retIds;
	}
}
