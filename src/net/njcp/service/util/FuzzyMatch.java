package net.njcp.service.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class FuzzyMatch {
	public static LinkedHashMap<Character, List<Integer>> countOccurrence(String a) {
		LinkedHashMap<Character, List<Integer>> idxMap = new LinkedHashMap<Character, List<Integer>>();
		for ( int i = 0; i < a.length(); i++ ) {
			char ch = a.charAt(i);
			if ( !idxMap.containsKey(ch) ) {
				idxMap.put(ch, new ArrayList<Integer>());
			}
			idxMap.get(ch).add(i);
		}
		System.out.println(idxMap);
		return idxMap;
	}

	public static List<Integer> getMaxMatchedList(String a, String b) {
		// a = QStringUtil.squeeze(a);
		// b = QStringUtil.squeeze(b);
		List<Integer> l = new ArrayList<Integer>();

		LinkedHashMap<Character, List<Integer>> idxMap = countOccurrence(a);

		int prev = -1;
		for ( int i = 0; i < b.length(); i++ ) {
			char ch = b.charAt(i);
			List<Integer> l1 = idxMap.get(ch);
			if ( l1 != null && !l1.isEmpty() ) {
				int idx = 0;
				for ( int j = 0; j < l1.size(); j++ ) {
					if ( l1.get(j) > prev ) {
						idx = j;
					}
				}
				l.add(l1.get(idx));
				prev = l1.get(idx);
				l1.remove(idx);
			}

		}

		System.out.println(l);

		List<Integer> lis = findLongestIncreasingSubsequence(l);
		return lis;
	}

	public static List<Integer> findLongestIncreasingSubsequence(final List<Integer> D) {
		List<List<Integer>> L = new ArrayList<List<Integer>>(D.size());
		for ( int i = 0; i < D.size(); i++ ) {
			L.add(new ArrayList<Integer>());
			if ( i == 0 ) {
				L.get(0).add(D.get(0));
				continue;
			}
			for ( int j = 0; j < i; j++ ) {
				if ( (D.get(j) < D.get(i)) && (L.get(i).size() < L.get(j).size() + 1) ) {
					L.get(i).clear();
					L.get(i).addAll(L.get(j));
				}
			}
			L.get(i).add(D.get(i));
		}
		List<Integer> lis = null;
		for ( List<Integer> l : L ) {
			System.out.println(l);
			if ( lis == null ) {
				lis = l;
			} else {
				lis = l.size() > lis.size() ? l : lis;
			}
		}
		return lis;
	}

	public static Double matchScore(String a, String b) {
		return getMaxMatchedList(a, b).size() / (double) a.length();
	}

	public static void main(String[] args) {
		String a = "lianyungang.guannanchengbeibian/220kv.chengbeixian";
		String b = "gncbb";
		// System.out.println(matchScore(a, b));
		System.out.println(
				getMaxMatchedList(a, b)
		// findLongestIncreasingSubsequence(Arrays.asList(new Integer[] { 23, 29, 13, 14, 15, 16, 22, 6, 18, 21, 25 }))
		//
		);
	}
}
