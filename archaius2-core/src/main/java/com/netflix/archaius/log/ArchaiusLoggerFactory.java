package com.netflix.archaius.log;

import static com.google.common.base.Preconditions.checkNotNull;

public class ArchaiusLoggerFactory {
	
	private static volatile ArchaiusLoggerService service = new NoOpLoggerFactory();
	
	public static ArchaiusLogger getLogger(
			Class<?> clazz) {
		return service.getLogger(clazz);
	}
	
	public static void setLoggerService(final ArchaiusLoggerService s) {
		checkNotNull(s, "service");
		service = s;
	}

}
