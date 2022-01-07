package tools.sapcx.commerce.toolkit.testing.testdoubles.config;

import de.hybris.platform.servicelayer.config.ConfigurationService;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;

public class ConfigurationServiceFake implements ConfigurationService {
	private BaseConfiguration configuration = new BaseConfiguration();

	@Override
	public Configuration getConfiguration() {
		return configuration;
	}

	public void setProperty(String key, Object value) {
		configuration.setProperty(key, value);
	}

	public void clearProperty(String key) {
		configuration.clearProperty(key);
	}
}
