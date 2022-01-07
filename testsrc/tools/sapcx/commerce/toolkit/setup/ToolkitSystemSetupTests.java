package tools.sapcx.commerce.toolkit.setup;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.initialization.SystemSetup;
import de.hybris.platform.core.initialization.SystemSetupContext;
import de.hybris.platform.core.initialization.SystemSetupParameter;

import org.junit.Before;
import org.junit.Test;

import tools.sapcx.commerce.toolkit.constants.ToolkitConstants;
import tools.sapcx.commerce.toolkit.impex.executor.ImpExDataImportExecutor;
import tools.sapcx.commerce.toolkit.setup.importer.PrefixBasedDataImporter;
import tools.sapcx.commerce.toolkit.setup.importer.ProjectDataImporter;
import tools.sapcx.commerce.toolkit.setup.importer.ReleasePatchesImporter;
import tools.sapcx.commerce.toolkit.testing.testdoubles.config.ConfigurationServiceFake;
import tools.sapcx.commerce.toolkit.testing.testdoubles.core.ValidationServiceSpy;

@UnitTest
public class ToolkitSystemSetupTests {
	private List<String> importedFiles = new ArrayList<>();
	private ValidationServiceSpy validationService;
	private SystemSetupEnvironment environment;
	private ConfigurationServiceFake configurationServiceFake;

	private PrefixBasedDataImporter elementaryDataImporter;
	private PrefixBasedDataImporter essentialDataImporter;
	private ReleasePatchesImporter releasePatchesImporter;
	private ProjectDataImporter sampleDataImporter;
	private ProjectDataImporter testDataImporter;
	private ProjectDataImporter releasePatchReRunImporter;

	private ToolkitSystemSetup systemSetup;

	@Before
	public void setUp() throws Exception {
		validationService = new ValidationServiceSpy();
		environment = new SystemSetupEnvironment();
		configurationServiceFake = new ConfigurationServiceFake();
		environment.setConfigurationService(configurationServiceFake);

		setupImpexDataImporterForTesting(environment);
		addEnvironmentConfiguration(false, false, false);

		systemSetup = new ToolkitSystemSetup();
		systemSetup.setValidationService(validationService);
		systemSetup.setElementaryDataImporter(elementaryDataImporter);
		systemSetup.setEssentialDataImporter(essentialDataImporter);
		systemSetup.setReleasePatchesImporter(releasePatchesImporter);
		systemSetup.setProjectDataImporters(Arrays.asList(sampleDataImporter, testDataImporter, releasePatchReRunImporter));
	}

	@Test
	public void whenInit_verifyValidationEngineIsReloaded() {
		addEnvironmentConfiguration(false, false, false);
		addConfigurationForImpExFiles();

		SystemSetupContext context = contextFor(SystemSetup.Process.INIT, SystemSetup.Type.ALL);
		systemSetup.reliableSetupPhases(context);

		assertThat(validationService.reloadValidationEngineCount).isEqualTo(1);
	}

	@Test
	public void whenUpdate_verifyValidationEngineIsReloaded() {
		addEnvironmentConfiguration(false, false, false);
		addConfigurationForImpExFiles();

		SystemSetupContext context = contextFor(SystemSetup.Process.UPDATE, SystemSetup.Type.ALL);
		systemSetup.reliableSetupPhases(context);

		assertThat(validationService.reloadValidationEngineCount).isEqualTo(1);
	}

	@Test
	public void whenProjectDataOnly_verifyValidationEngineIsReloaded() {
		addEnvironmentConfiguration(false, false, false);
		addConfigurationForImpExFiles();

		SystemSetupContext context = contextFor(SystemSetup.Process.ALL, SystemSetup.Type.PROJECT);
		systemSetup.reliableSetupPhases(context);

		assertThat(validationService.reloadValidationEngineCount).isZero();
	}

	@Test
	public void whenInitForDevelopment_verifyAllImpExImportsArePerformed() {
		addEnvironmentConfiguration(true, false, false);
		addConfigurationForImpExFiles();

		SystemSetupContext context = contextFor(SystemSetup.Process.INIT, SystemSetup.Type.ALL);
		systemSetup.reliableSetupPhases(context);

		assertThat(importedFiles).contains(
				"/elementary/file0001.impex",
				"/elementary/file0002.impex",
				"/elementary/file0003.impex",
				"/essential/file0001.impex",
				"/essential/file0002.impex",
				"/essential/file0003.impex",
				"/sampledata/file0001.impex",
				"/sampledata/file0002.impex",
				"/sampledata/file0003.impex",
				"/testdata/file0001.impex",
				"/testdata/file0002.impex",
				"/testdata/file0003.impex");
	}

	@Test
	public void whenInitForNonDevelopment_verifyOnlyElementaryAndEssentialImpExImportsArePerformed() {
		addConfigurationForImpExFiles();

		SystemSetupContext context = contextFor(SystemSetup.Process.INIT, SystemSetup.Type.ALL);
		systemSetup.reliableSetupPhases(context);

		assertThat(importedFiles).contains(
				"/elementary/file0001.impex",
				"/elementary/file0002.impex",
				"/elementary/file0003.impex",
				"/essential/file0001.impex",
				"/essential/file0002.impex",
				"/essential/file0003.impex");
	}

	@Test
	public void whenInitForNonDevelopment_andFlagsForSampleAndTestData_verifyAllImpExImportsArePerformed() {
		addEnvironmentConfiguration(false, true, true);
		addConfigurationForImpExFiles();

		SystemSetupContext context = contextFor(SystemSetup.Process.INIT, SystemSetup.Type.ALL);
		systemSetup.reliableSetupPhases(context);

		assertThat(importedFiles).contains(
				"/elementary/file0001.impex",
				"/elementary/file0002.impex",
				"/elementary/file0003.impex",
				"/essential/file0001.impex",
				"/essential/file0002.impex",
				"/essential/file0003.impex",
				"/sampledata/file0001.impex",
				"/sampledata/file0002.impex",
				"/sampledata/file0003.impex",
				"/testdata/file0001.impex",
				"/testdata/file0002.impex",
				"/testdata/file0003.impex");
	}

	@Test
	public void whenInit_verifyThatReleasePatchesAreNotPerformed() throws Exception {
		addConfigurationForImpExFiles();

		SystemSetupContext context = contextFor(SystemSetup.Process.INIT, SystemSetup.Type.ALL);
		systemSetup.reliableSetupPhases(context);

		assertThat(importedFiles).doesNotContain(
				"/releasepatches/release1x0-0001.impex",
				"/releasepatches/release1x0-0002.impex",
				"/releasepatches/release2x0-0001.impex",
				"/releasepatches/release2x0-0002.impex",
				"/releasepatches/release2x1-0001.impex",
				"/releasepatches/release2x1-0002.impex");
	}

	@Test
	public void whenUpdateAfterInit_verifyThatReleasePatchesAreNotPerformed() throws Exception {
		addConfigurationForImpExFiles();
		performSilently(SystemSetup.Process.INIT);

		SystemSetupContext contextForUpdate = contextFor(SystemSetup.Process.UPDATE, SystemSetup.Type.ALL);
		systemSetup.reliableSetupPhases(contextForUpdate);

		assertThat(importedFiles).doesNotContain(
				"/releasepatches/release1x0-0001.impex",
				"/releasepatches/release1x0-0002.impex",
				"/releasepatches/release2x0-0001.impex",
				"/releasepatches/release2x0-0002.impex",
				"/releasepatches/release2x1-0001.impex",
				"/releasepatches/release2x1-0002.impex");
	}

	@Test
	public void whenUpdateAfterInit_verifySelectedReleasePatchesAreReRun() {
		addConfigurationForImpExFiles();
		performSilently(SystemSetup.Process.INIT);

		Map<String, String[]> parameters = new HashMap<>();
		parameters.put(releasePatchReRunImporter.getPrefix(), new String[] { "release2x0.0001", "release2x0.0002" });
		SystemSetupContext context = contextFor(SystemSetup.Process.UPDATE, SystemSetup.Type.ALL, parameters);
		systemSetup.reliableSetupPhases(context);

		assertThat(importedFiles).doesNotContain(
				"/releasepatches/release1x0-0001.impex",
				"/releasepatches/release1x0-0002.impex",
				"/releasepatches/release2x1-0001.impex",
				"/releasepatches/release2x1-0002.impex").containsSequence(
						"/releasepatches/release2x0-0001.impex",
						"/releasepatches/release2x0-0002.impex");
	}

	@Test
	public void whenUpdate_verifyEssentialImportsArePerformed() {
		addConfigurationForImpExFiles();

		SystemSetupContext context = contextFor(SystemSetup.Process.UPDATE, SystemSetup.Type.ALL);
		systemSetup.reliableSetupPhases(context);

		assertThat(importedFiles).containsSequence(
				"/essential/file0001.impex",
				"/essential/file0002.impex",
				"/essential/file0003.impex");
	}

	@Test
	public void whenUpdate_verifyReleasePatchesArePerformed() {
		addConfigurationForImpExFiles();

		SystemSetupContext context = contextFor(SystemSetup.Process.UPDATE, SystemSetup.Type.ALL);
		systemSetup.reliableSetupPhases(context);

		assertThat(importedFiles).containsSequence(
				"/releasepatches/release1x0-0001.impex",
				"/releasepatches/release1x0-0002.impex",
				"/releasepatches/release2x0-0001.impex",
				"/releasepatches/release2x0-0002.impex",
				"/releasepatches/release2x1-0001.impex",
				"/releasepatches/release2x1-0002.impex");
	}

	@Test
	public void whenIncrementalUpdate_verifyOnlyNewerReleasePatchesArePerformed() {
		addConfigurationForImpExFiles();
		performSilently(SystemSetup.Process.UPDATE);

		configurationServiceFake.setProperty("namoperation.impeximport.releasepatch.release2x1.0003", "/releasepatches/release2x1-0003.impex");
		configurationServiceFake.setProperty("namoperation.impeximport.releasepatch.release2x1.0004", "/releasepatches/release2x1-0004.impex");

		SystemSetupContext contextForSecondRun = contextFor(SystemSetup.Process.UPDATE, SystemSetup.Type.ALL);
		systemSetup.reliableSetupPhases(contextForSecondRun);

		assertThat(importedFiles).doesNotContain(
				"/releasepatches/release1x0-0001.impex",
				"/releasepatches/release1x0-0002.impex",
				"/releasepatches/release2x0-0001.impex",
				"/releasepatches/release2x0-0002.impex",
				"/releasepatches/release2x1-0001.impex",
				"/releasepatches/release2x1-0002.impex").containsSequence(
						"/releasepatches/release2x1-0003.impex",
						"/releasepatches/release2x1-0004.impex");
	}

	@Test
	public void whenUpdate_verifyElementaryImportsAreNotPerformed() {
		addConfigurationForImpExFiles();

		SystemSetupContext context = contextFor(SystemSetup.Process.UPDATE, SystemSetup.Type.ALL);
		systemSetup.reliableSetupPhases(context);

		assertThat(importedFiles).doesNotContain(
				"/elementary/file0001.impex",
				"/elementary/file0002.impex",
				"/elementary/file0003.impex");
	}

	@Test
	public void whenSystemSetupParametersIsCalled_itContainsNoSystemSetupParametersThatHaveNoOptions() {
		List<SystemSetupParameter> systemSetupParameters = systemSetup.getSystemSetupParameters();
		assertThat(systemSetupParameters.stream().map(SystemSetupParameter::getKey)).isEmpty();
	}

	@Test
	public void whenSystemSetupParametersIsCalled_itContainsAllSystemSetupParametersAsMultiSelectOptions() {
		addConfigurationForImpExFiles();

		List<SystemSetupParameter> systemSetupParameters = systemSetup.getSystemSetupParameters();

		assertThat(systemSetupParameters.stream().map(SystemSetupParameter::getKey)).containsExactlyInAnyOrder(
				"namoperation.impeximport.sampledata",
				"namoperation.impeximport.testdata",
				"namoperation.impeximport.releasepatch");
		assertThat(systemSetupParameters.stream().map(SystemSetupParameter::isMultiSelect).allMatch(Boolean.TRUE::equals)).isTrue();
	}

	@Test
	public void whenSystemSetupParametersIsCalled_verifyAllValuesAreDeactivatedByDefault() {
		List<SystemSetupParameter> systemSetupParameters = systemSetup.getSystemSetupParameters();
		assertThat(systemSetupParameters.stream()
				.map(SystemSetupParameter::getValues)
				.flatMap(map -> map.values().stream())
				.allMatch(Boolean.FALSE::equals)).isTrue();
	}

	@Test
	public void whenSystemSetupParametersIsCalled_verifySelectableOptionsArePresent() {
		addConfigurationForImpExFiles();

		List<SystemSetupParameter> systemSetupParameters = systemSetup.getSystemSetupParameters();
		assertThat(systemSetupParameters.stream()
				.map(SystemSetupParameter::getValues)
				.flatMap(map -> map.keySet().stream())).containsExactlyInAnyOrder(
						"release1x0.0001.optionaltext",
						"release1x0.0002",
						"release2x0.0002",
						"release2x0.0001",
						"release2x1.0001",
						"release2x1.0002",
						"0001",
						"0002.optionaltext",
						"0003",
						"0001.optionaltext",
						"0002",
						"0003");
	}

	private void setupImpexDataImporterForTesting(SystemSetupEnvironment environment) {
		ImpExDataImportExecutor executor = (context, file, encoding) -> importedFiles.add(file);

		elementaryDataImporter = new PrefixBasedDataImporter();
		elementaryDataImporter.setEnvironment(environment);
		elementaryDataImporter.setImpExDataImportExecutor(executor);
		elementaryDataImporter.setTitle("Elementary Data");
		elementaryDataImporter.setPrefix("namoperation.impeximport.elementarydata");

		essentialDataImporter = new PrefixBasedDataImporter();
		essentialDataImporter.setEnvironment(environment);
		essentialDataImporter.setImpExDataImportExecutor(executor);
		essentialDataImporter.setTitle("Essential Data");
		essentialDataImporter.setPrefix("namoperation.impeximport.essentialdata");

		releasePatchesImporter = new ReleasePatchesImporter();
		releasePatchesImporter.setEnvironment(environment);
		releasePatchesImporter.setImpExDataImportExecutor(executor);
		releasePatchesImporter.setTitle("Release Patches (automatically)");
		releasePatchesImporter.setPrefix("namoperation.impeximport.releasepatch");

		sampleDataImporter = new ProjectDataImporter();
		sampleDataImporter.setEnvironment(environment);
		sampleDataImporter.setImpExDataImportExecutor(executor);
		sampleDataImporter.setTitle("Sample Data");
		sampleDataImporter.setPrefix("namoperation.impeximport.sampledata");

		testDataImporter = new ProjectDataImporter();
		testDataImporter.setEnvironment(environment);
		testDataImporter.setImpExDataImportExecutor(executor);
		testDataImporter.setTitle("Test Data");
		testDataImporter.setPrefix("namoperation.impeximport.testdata");

		releasePatchReRunImporter = new ProjectDataImporter();
		releasePatchReRunImporter.setEnvironment(environment);
		releasePatchReRunImporter.setImpExDataImportExecutor(executor);
		releasePatchReRunImporter.setTitle("Release Patches (manual)");
		releasePatchReRunImporter.setPrefix("namoperation.impeximport.releasepatch");
	}

	private void addEnvironmentConfiguration(boolean isDevelopment, boolean shouldImportSampleData, boolean shouldImportTestData) {
		configurationServiceFake.setProperty(SystemSetupEnvironment.ISDEVELOPMENTKEY, isDevelopment);
		configurationServiceFake.setProperty("namoperation.impeximport.environment.importsampledata", shouldImportSampleData);
		sampleDataImporter.setImportOnInitialization(shouldImportSampleData);
		configurationServiceFake.setProperty("namoperation.impeximport.environment.importtestdata", shouldImportTestData);
		testDataImporter.setImportOnInitialization(shouldImportTestData);
		configurationServiceFake.setProperty("namoperation.impeximport.environment.importtestdata", shouldImportTestData);
	}

	private void addConfigurationForImpExFiles() {
		configurationServiceFake.setProperty("namoperation.impeximport.elementarydata.0001.optionaltext", "/elementary/file0001.impex");
		configurationServiceFake.setProperty("namoperation.impeximport.elementarydata.0002", "/elementary/file0002.impex");
		configurationServiceFake.setProperty("namoperation.impeximport.elementarydata.0003.optionaltext", "/elementary/file0003.impex");
		configurationServiceFake.setProperty("namoperation.impeximport.essentialdata.0002.something", "/essential/file0002.impex");
		configurationServiceFake.setProperty("namoperation.impeximport.essentialdata.0001.something", "/essential/file0001.impex");
		configurationServiceFake.setProperty("namoperation.impeximport.essentialdata.0003.something", "/essential/file0003.impex");
		configurationServiceFake.setProperty("namoperation.impeximport.testdata.0001.optionaltext", "/testdata/file0001.impex");
		configurationServiceFake.setProperty("namoperation.impeximport.testdata.0003", "/testdata/file0003.impex");
		configurationServiceFake.setProperty("namoperation.impeximport.testdata.0002", "/testdata/file0002.impex");
		configurationServiceFake.setProperty("namoperation.impeximport.sampledata.0003", "/sampledata/file0003.impex");
		configurationServiceFake.setProperty("namoperation.impeximport.sampledata.0002.optionaltext", "/sampledata/file0002.impex");
		configurationServiceFake.setProperty("namoperation.impeximport.sampledata.0001", "/sampledata/file0001.impex");

		configurationServiceFake.setProperty("namoperation.impeximport.releasepatch.release1x0.0001.optionaltext", "/releasepatches/release1x0-0001.impex");
		configurationServiceFake.setProperty("namoperation.impeximport.releasepatch.release1x0.0002", "/releasepatches/release1x0-0002.impex");
		configurationServiceFake.setProperty("namoperation.impeximport.releasepatch.release2x1.0002", "/releasepatches/release2x1-0002.impex");
		configurationServiceFake.setProperty("namoperation.impeximport.releasepatch.release2x1.0001", "/releasepatches/release2x1-0001.impex");
		configurationServiceFake.setProperty("namoperation.impeximport.releasepatch.release2x0.0002", "/releasepatches/release2x0-0002.impex");
		configurationServiceFake.setProperty("namoperation.impeximport.releasepatch.release2x0.0001", "/releasepatches/release2x0-0001.impex");
	}

	private void performSilently(SystemSetup.Process process) {
		SystemSetupContext contextForFirstRun = contextFor(process, SystemSetup.Type.ALL);
		systemSetup.reliableSetupPhases(contextForFirstRun);
		importedFiles.clear();
	}

	private SystemSetupContext contextFor(SystemSetup.Process process, SystemSetup.Type type) {
		return new SystemSetupContext(Collections.emptyMap(), type, process, ToolkitConstants.EXTENSIONNAME);
	}

	private SystemSetupContext contextFor(SystemSetup.Process process, SystemSetup.Type type, Map<String, String[]> parameters) {
		SystemSetupContext context = contextFor(process, type);

		HashMap<String, String[]> uniqueParameters = new HashMap<>(parameters.size());
		for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
			uniqueParameters.put(context.getExtensionName() + "_" + entry.getKey(), entry.getValue());
		}
		context.setParameterMap(uniqueParameters);

		return context;
	}
}
