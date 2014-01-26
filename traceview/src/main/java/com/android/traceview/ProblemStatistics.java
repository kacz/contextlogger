package com.android.traceview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public abstract class ProblemStatistics {

	public static String computeLongStats(List<Long> values) {
		StringBuilder result = new StringBuilder();
		Map<Long, Integer> intMap = new HashMap<Long, Integer>();
		for (Long val : values) {
			if (intMap.containsKey(val)) {
				int count = intMap.get(val);
				intMap.put(val, count + 1);
			} else {
				intMap.put(val, 1);
			}
		}
		List<Map.Entry<Long, Integer>> sorted = new ArrayList<Map.Entry<Long, Integer>>(
				intMap.entrySet());
		Collections.sort(sorted,
				new Comparator<Map.Entry<Long, Integer>>() {

					@Override
					public int compare(Entry<Long, Integer> e1,
							Entry<Long, Integer> e2) {
						if (e1.getValue() < e2.getValue()) {
							return 1;
						}
						if (e1.getValue() > e2.getValue()) {
							return -1;
						}
						return 0;
						// return e1.getKey().compareTo(e2.getKey());
					}

				});

		// Collections.reverse(sorted);

		int counter = 0;
		int lastPercentage = 0;
		for (Map.Entry<Long, Integer> e : sorted) {

			int percentage = 100 * e.getValue()
					/ values.size();
			counter += 1;
			if (counter > 5 && lastPercentage != percentage) {
				break;
			}
			lastPercentage = percentage;
			result.append("\t" + percentage + "% " + e.getKey()
					+ "\n");
		}
		return result.toString();
	}

	public static String computeDoubleStats(List<Double> values) {
		StringBuilder result = new StringBuilder();
		List<Double> sorted = new ArrayList<Double>();
		for (Double val : values) {

			if (val != null) {
				sorted.add(val);
			}
		}
		Collections.sort(sorted);

		// 90% interval (without top,bottom 5%)
		if (sorted.size() != 0) {
			int first = (int) (sorted.size() * 0.05);
			int last = (int) (sorted.size() * 0.95);
			System.out
					.println(sorted.size() + " " + first + " " + last);
			result.append("\t90% interval: (" + sorted.get(first)
					+ "-" + sorted.get(last) + ")\n");
		}
		
		return result.toString();
	}

	public static String computeStringStats(List<String> values) {
		StringBuilder result = new StringBuilder();
		Map<String, Integer> stringMap = new HashMap<String, Integer>();
		for (String val : values) {
			if (stringMap.containsKey(val)) {
				int count = stringMap.get(val);
				stringMap.put(val, count + 1);
			} else {
				stringMap.put(val, 1);
			}
		}
		List<Map.Entry<String, Integer>> sorted = new ArrayList<Map.Entry<String, Integer>>(stringMap.entrySet());
		Collections.sort(sorted, new Comparator<Map.Entry<String, Integer>>() {
	
			@Override
			public int compare(Entry<String, Integer> e1,
					Entry<String, Integer> e2) {
				if(e1.getValue() < e2.getValue()) {
							return 1;
				}
				if(e1.getValue() > e2.getValue()) {
							return -1;
				}
						return 0;
						// return e1.getKey().compareTo(e2.getKey());
			}
			
		}
			);
	
		Collections.reverse(sorted);
	
		for (Map.Entry<String, Integer> e : sorted) {
			int percentage = 100 * e.getValue()
					/ values.size();
			result.append("\t" + percentage + "% " + e.getKey()
					+ "\n");
		}
		
		return result.toString();
	}
}
