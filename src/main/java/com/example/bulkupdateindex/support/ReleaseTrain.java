package com.example.bulkupdateindex.support;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Stephane Nicoll
 */
public class ReleaseTrain {

	private final String name;

	private final String qualifier;

	private static final Pattern RELEASE_TRAIN_REGEX = Pattern
			.compile("([A-Za-z]*)(_|-|.)([A-Za-z0-9_-]*)");

	public ReleaseTrain(String name, String qualifier) {
		this.name = name;
		this.qualifier = qualifier;
	}

	public String getName() {
		return this.name;
	}

	public String getQualifier() {
		return this.qualifier;
	}

	public static ReleaseTrain safeParse(String text) {
		Matcher matcher = RELEASE_TRAIN_REGEX.matcher(text.trim());
		if (!matcher.matches()) {
			return null;
		}
		String name = matcher.group(1);
		String qualifier = matcher.group(3);
		return new ReleaseTrain(name, qualifier);
	}

}
