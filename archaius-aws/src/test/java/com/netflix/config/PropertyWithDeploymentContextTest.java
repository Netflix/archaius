package com.netflix.config;

import org.junit.Test;

public class PropertyWithDeploymentContextTest {
	@Test
	public void verifyHashCodeSucceedsForGlobalProperty() {
		PropertyWithDeploymentContext property = new PropertyWithDeploymentContext(null, null, "key", "value");
		property.hashCode();
	}
}