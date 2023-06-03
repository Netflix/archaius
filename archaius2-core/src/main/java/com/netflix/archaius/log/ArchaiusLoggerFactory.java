package com.netflix.archaius.log;


public class ArchaiusLoggerFactory {
	
	private static volatile ArchaiusLoggerService service = new NoOpLoggerFactory();
	
	public static ArchaiusLogger getLogger(
			Class<?> clazz) {
		return service.getLogger(clazz);
	}
	
	public static void setLoggerService(final ArchaiusLoggerService s) {
		service = s;
	}

}
