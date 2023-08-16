package cd.go.plugin.config.yaml;

import com.google.gson.*;
import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.exceptions.UnhandledRequestTypeException;
import com.thoughtworks.go.plugin.api.request.DefaultGoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collections;

import static cd.go.plugin.config.yaml.ConfigRepoMessages.REQ_PLUGIN_SETTINGS_CHANGED;
import static cd.go.plugin.config.yaml.PluginSettings.DEFAULT_FILE_PATTERN;
import static cd.go.plugin.config.yaml.TestUtils.getResourceAsStream;
import static cd.go.plugin.config.yaml.TestUtils.readJsonObject;
import static com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class YamlConfigPluginIntegrationTest {
    @TempDir
    Path tempDir;
    private YamlConfigPlugin plugin;
    private GoApplicationAccessor goAccessor;
    private JsonParser parser;

    @BeforeEach
    public void setUp() {
        plugin = new YamlConfigPlugin();
        goAccessor = mock(GoApplicationAccessor.class);
        plugin.initializeGoApplicationAccessor(goAccessor);
        GoApiResponse settingsResponse = DefaultGoApiResponse.success("{}");
        when(goAccessor.submit(any())).thenReturn(settingsResponse);
        parser = new JsonParser();
    }

    @Test
    public void respondsToParseContentRequest() throws Exception {
        final Gson gson = new Gson();
        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest("configrepo", "2.0", ConfigRepoMessages.REQ_PARSE_CONTENT);

        StringWriter w = new StringWriter();
        IOUtils.copy(getResourceAsStream("examples/simple.gocd.jsonnet"), w);
        request.setRequestBody(gson.toJson(
                Collections.singletonMap("contents",
                        Collections.singletonMap("simple.gocd.jsonnet", w.toString())
                )
        ));

        GoPluginApiResponse response = plugin.handle(request);
        assertEquals(SUCCESS_RESPONSE_CODE, response.responseCode());
        JsonObject responseJsonObject = getJsonObjectFromResponse(response);
        assertNoError(responseJsonObject);

        JsonArray pipelines = responseJsonObject.get("pipelines").getAsJsonArray();
        assertThat(pipelines.size(), is(1));
        JsonObject expected = (JsonObject) readJsonObject("examples.out/simple.gocd.json");
        assertThat(responseJsonObject, is(new JsonObjectMatcher(expected)));
    }

    @Test
    public void respondsToGetConfigFiles() throws Exception {
        final Gson gson = new Gson();
        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest("configrepo", "3.0", ConfigRepoMessages.REQ_CONFIG_FILES);
        FileUtils.copyInputStreamToFile(
                getResourceAsStream("/examples/simple.gocd.jsonnet"), Files.createFile(tempDir.resolve("valid.gocd.jsonnet")).toFile()
        );
        FileUtils.copyInputStreamToFile(
                getResourceAsStream("/examples/simple-invalid.gocd.jsonnet"), Files.createFile(tempDir.resolve("invalid.gocd.jsonnet")).toFile()
        );

        request.setRequestBody(gson.toJson(
                Collections.singletonMap("directory", tempDir.toFile().toString())
        ));

        GoPluginApiResponse response = plugin.handle(request);
        assertEquals(SUCCESS_RESPONSE_CODE, response.responseCode());

        JsonArray files = getJsonObjectFromResponse(response).get("files").getAsJsonArray();
        assertThat(files.size(), is(2));
        assertTrue(files.contains(new JsonPrimitive("valid.gocd.jsonnet")));
        assertTrue(files.contains(new JsonPrimitive("invalid.gocd.jsonnet")));
    }

    @Test
    public void shouldRespondSuccessToGetConfigurationRequest() throws UnhandledRequestTypeException {
        DefaultGoPluginApiRequest getConfigRequest = new DefaultGoPluginApiRequest("configrepo", "1.0", "go.plugin-settings.get-configuration");

        GoPluginApiResponse response = plugin.handle(getConfigRequest);
        assertThat(response.responseCode(), is(SUCCESS_RESPONSE_CODE));
    }

    @Test
    public void shouldContainFilePatternInResponseToGetConfigurationRequest() throws UnhandledRequestTypeException {
        DefaultGoPluginApiRequest getConfigRequest = new DefaultGoPluginApiRequest("configrepo", "1.0", "go.plugin-settings.get-configuration");

        GoPluginApiResponse response = plugin.handle(getConfigRequest);
        assertThat(response.responseCode(), is(SUCCESS_RESPONSE_CODE));
        JsonObject responseJsonObject = getJsonObjectFromResponse(response);
        JsonElement pattern = responseJsonObject.get("file_pattern");
        assertNotNull(pattern);
        JsonObject patternAsJsonObject = pattern.getAsJsonObject();
        assertThat(patternAsJsonObject.get("display-name").getAsString(), is("Go Jsonnet files pattern"));
        assertThat(patternAsJsonObject.get("default-value").getAsString(), is("**/*.jsonnet,**/jsonnetfile.json"));
        assertThat(patternAsJsonObject.get("required").getAsBoolean(), is(false));
        assertThat(patternAsJsonObject.get("secure").getAsBoolean(), is(false));
        assertThat(patternAsJsonObject.get("display-order").getAsInt(), is(0));
    }

    @Test
    public void shouldRespondSuccessToGetViewRequest() throws UnhandledRequestTypeException {
        DefaultGoPluginApiRequest getConfigRequest = new DefaultGoPluginApiRequest("configrepo", "1.0", "go.plugin-settings.get-view");

        GoPluginApiResponse response = plugin.handle(getConfigRequest);
        assertThat(response.responseCode(), is(SUCCESS_RESPONSE_CODE));
    }

    @Test
    public void shouldRespondSuccessToValidateConfigRequest() throws UnhandledRequestTypeException {
        DefaultGoPluginApiRequest validateRequest = new DefaultGoPluginApiRequest("configrepo", "1.0", "go.plugin-settings.validate-configuration");

        GoPluginApiResponse response = plugin.handle(validateRequest);
        assertThat(response.responseCode(), is(SUCCESS_RESPONSE_CODE));
    }

    @Test
    public void shouldRespondSuccessToParseDirectoryRequestWhenEmpty() throws UnhandledRequestTypeException {
        GoPluginApiResponse response = parseAndGetResponseForDir(tempDir.toFile());

        assertThat(response.responseCode(), is(SUCCESS_RESPONSE_CODE));
        JsonObject responseJsonObject = getJsonObjectFromResponse(response);
        assertNoError(responseJsonObject);
    }

    @Test
    public void shouldRespondSuccessToParseDirectoryRequestWhenLongCaseFile() throws UnhandledRequestTypeException, IOException {
        File rootDir = setupCase("long");

        GoPluginApiResponse response = parseAndGetResponseForDir(rootDir);
        assertThat(response.responseCode(), is(200));
        JsonObject responseJsonObject = getJsonObjectFromResponse(response);
        assertNoError(responseJsonObject);
    }

    @Test
    public void shouldRespondSuccessToParseDirectoryRequestWhenSimpleCaseFile() throws UnhandledRequestTypeException, IOException {
        GoPluginApiResponse response = parseAndGetResponseForDir(setupCase("simple"));

        assertThat(response.responseCode(), is(SUCCESS_RESPONSE_CODE));
        JsonObject responseJsonObject = getJsonObjectFromResponse(response);
        assertNoError(responseJsonObject);
        JsonArray pipelines = responseJsonObject.get("pipelines").getAsJsonArray();
        assertThat(pipelines.size(), is(1));
        JsonObject expected = (JsonObject) readJsonObject("examples.out/simple.gocd.json");
        assertThat(responseJsonObject, is(new JsonObjectMatcher(expected)));
    }

    @Test
    public void shouldRespondSuccessToParseDirectoryRequestWhenSimpleCaseFileYaml() throws UnhandledRequestTypeException, IOException {
        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest("configrepo", "2.0", REQ_PLUGIN_SETTINGS_CHANGED);
        request.setRequestBody("{\"file_pattern\": \"*.gocd.yaml\"}");

        GoPluginApiResponse yamlFilePatternResponse = plugin.handle(request);

        assertThat(yamlFilePatternResponse.responseCode(), is(SUCCESS_RESPONSE_CODE));

        GoPluginApiResponse response = parseAndGetResponseForDir(setupCaseYaml("simple"));

        assertThat(response.responseCode(), is(SUCCESS_RESPONSE_CODE));
        JsonObject responseJsonObject = getJsonObjectFromResponse(response);
        assertNoError(responseJsonObject);
        JsonArray pipelines = responseJsonObject.get("pipelines").getAsJsonArray();
        assertThat(pipelines.size(), is(1));
        JsonObject expected = (JsonObject) readJsonObject("examples.out/simple-yaml.gocd.json");
        assertThat(responseJsonObject, is(new JsonObjectMatcher(expected)));
    }

    @Test
    public void shouldRespondSuccessToParseDirectoryRequestWhenSimpleCaseFileWithFlags() throws UnhandledRequestTypeException, IOException {
        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest("configrepo", "2.0", REQ_PLUGIN_SETTINGS_CHANGED);
        request.setRequestBody("{\"jsonnet_flags\": \"--ext-code output-files=false\"}");

        assertEquals(DEFAULT_FILE_PATTERN, plugin.getFilePattern());
        GoPluginApiResponse flagsResponse = plugin.handle(request);

        assertThat(flagsResponse.responseCode(), is(SUCCESS_RESPONSE_CODE));

        GoPluginApiResponse response = parseAndGetResponseForDir(setupCase("simple"));

        assertThat(response.responseCode(), is(SUCCESS_RESPONSE_CODE));
        JsonObject responseJsonObject = getJsonObjectFromResponse(response);
        assertNoError(responseJsonObject);
        JsonArray pipelines = responseJsonObject.get("pipelines").getAsJsonArray();
        assertThat(pipelines.size(), is(1));
        JsonObject expected = (JsonObject) readJsonObject("examples.out/simple.gocd.json");
        assertThat(responseJsonObject, is(new JsonObjectMatcher(expected)));
    }

    @Test
    public void shouldRespondSuccessToParseDirectoryRequestWhenSimpleCaseFileJsonnetAndYaml() throws UnhandledRequestTypeException, IOException {
        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest("configrepo", "2.0", REQ_PLUGIN_SETTINGS_CHANGED);
        request.setRequestBody("{\"file_pattern\": \"**/*.jsonnet,**/jsonnetfile.json,*.gocd.yaml\"}");

        GoPluginApiResponse yamlFilePatternResponse = plugin.handle(request);

        assertThat(yamlFilePatternResponse.responseCode(), is(SUCCESS_RESPONSE_CODE));

        GoPluginApiResponse response = parseAndGetResponseForDir(setupCaseJsonnetAndYaml("simple"));

        assertThat(response.responseCode(), is(SUCCESS_RESPONSE_CODE));
        JsonObject responseJsonObject = getJsonObjectFromResponse(response);
        assertNoError(responseJsonObject);
        JsonArray pipelines = responseJsonObject.get("pipelines").getAsJsonArray();
        assertThat(pipelines.size(), is(2));
        JsonObject expected = (JsonObject) readJsonObject("examples.out/simple-both.gocd.json");
        assertThat(responseJsonObject, is(new JsonObjectMatcher(expected)));
    }

    @Test
    public void shouldRespondSuccessToParseDirectoryRequestWhenRichCaseFile() throws UnhandledRequestTypeException, IOException {
        GoPluginApiResponse response = parseAndGetResponseForDir(setupCase("rich"));

        assertThat(response.responseCode(), is(SUCCESS_RESPONSE_CODE));
        JsonObject responseJsonObject = getJsonObjectFromResponse(response);
        assertNoError(responseJsonObject);
        JsonArray pipelines = responseJsonObject.get("pipelines").getAsJsonArray();
        assertThat(pipelines.size(), is(1));
        JsonObject expected = (JsonObject) readJsonObject("examples.out/rich.gocd.json");
        assertThat(responseJsonObject, is(new JsonObjectMatcher(expected)));
    }

    @Test
    public void shouldRespondSuccessToParseDirectoryRequestWhenFormat9WithWhitelistAndIncludes() throws UnhandledRequestTypeException, IOException {
        GoPluginApiResponse response = parseAndGetResponseForDir(setupCase("format-version-9"));

        assertThat(response.responseCode(), is(SUCCESS_RESPONSE_CODE));
        JsonObject responseJsonObject = getJsonObjectFromResponse(response);
        assertNoError(responseJsonObject);
        JsonArray pipelines = responseJsonObject.get("pipelines").getAsJsonArray();
        assertThat(pipelines.size(), is(2));
        JsonObject expected = (JsonObject) readJsonObject("examples.out/format-version-9.gocd.json");
        assertThat(responseJsonObject, is(new JsonObjectMatcher(expected)));
    }

    @Test
    public void shouldRespondSuccessToParseDirectoryRequestWhenFormat10WithWhitelistAndIncludes() throws UnhandledRequestTypeException, IOException {
        GoPluginApiResponse response = parseAndGetResponseForDir(setupCase("format-version-10"));

        assertThat(response.responseCode(), is(SUCCESS_RESPONSE_CODE));
        JsonObject responseJsonObject = getJsonObjectFromResponse(response);
        assertNoError(responseJsonObject);
        JsonArray pipelines = responseJsonObject.get("pipelines").getAsJsonArray();
        assertThat(pipelines.size(), is(2));
        JsonObject expected = (JsonObject) readJsonObject("examples.out/format-version-10.gocd.json");
        assertThat(responseJsonObject, is(new JsonObjectMatcher(expected)));
    }

    @Test
    public void shouldRespondSuccessWithErrorMessagesToParseDirectoryRequestWhenSimpleInvalidCaseFile() throws UnhandledRequestTypeException, IOException {
        GoPluginApiResponse response = parseAndGetResponseForDir(setupCase("simple-invalid"));

        assertThat(response.responseCode(), is(SUCCESS_RESPONSE_CODE));
        JsonObject responseJsonObject = getJsonObjectFromResponse(response);
        JsonArray pipelines = responseJsonObject.get("pipelines").getAsJsonArray();
        assertThat(pipelines.size(), is(0));
        assertFirstError(responseJsonObject, "Failed to parse pipeline pipe1; expected a hash of pipeline materials", "simple-invalid.gocd.jsonnet");
    }

    @Test
    public void shouldRespondSuccessWithErrorMessagesToParseDirectoryRequestWhenParsingErrorCaseFile() throws UnhandledRequestTypeException, IOException {
        GoPluginApiResponse response = parseAndGetResponseForDir(setupCase("invalid-materials"));

        assertThat(response.responseCode(), is(SUCCESS_RESPONSE_CODE));
        JsonObject responseJsonObject = getJsonObjectFromResponse(response);
        JsonArray pipelines = responseJsonObject.get("pipelines").getAsJsonArray();
        assertThat(pipelines.size(), is(0));
        assertFirstErrorContains(responseJsonObject, "Could not lex the character '`'", "invalid-materials.gocd.jsonnet");
    }

    @Test
    public void shouldRespondBadRequestToParseDirectoryRequestWhenDirectoryIsNotSpecified() throws UnhandledRequestTypeException {
        DefaultGoPluginApiRequest parseDirectoryRequest = new DefaultGoPluginApiRequest("configrepo", "1.0", "parse-directory");
        String requestBody = "{\n" +
                "    \"configurations\":[]\n" +
                "}";
        parseDirectoryRequest.setRequestBody(requestBody);

        GoPluginApiResponse response = plugin.handle(parseDirectoryRequest);
        assertThat(response.responseCode(), is(DefaultGoPluginApiResponse.BAD_REQUEST));
    }

    @Test
    public void shouldRespondBadRequestToParseDirectoryRequestWhenRequestBodyIsNull() throws UnhandledRequestTypeException {
        DefaultGoPluginApiRequest parseDirectoryRequest = new DefaultGoPluginApiRequest("configrepo", "1.0", "parse-directory");
        String requestBody = null;
        parseDirectoryRequest.setRequestBody(requestBody);

        GoPluginApiResponse response = plugin.handle(parseDirectoryRequest);
        assertThat(response.responseCode(), is(DefaultGoPluginApiResponse.BAD_REQUEST));
    }

    @Test
    public void shouldRespondBadRequestToParseDirectoryRequestWhenRequestBodyIsEmpty() throws UnhandledRequestTypeException {
        DefaultGoPluginApiRequest parseDirectoryRequest = new DefaultGoPluginApiRequest("configrepo", "1.0", "parse-directory");
        parseDirectoryRequest.setRequestBody("{}");

        GoPluginApiResponse response = plugin.handle(parseDirectoryRequest);
        assertThat(response.responseCode(), is(DefaultGoPluginApiResponse.BAD_REQUEST));
    }

    @Test
    public void shouldRespondBadRequestToParseDirectoryRequestWhenRequestBodyIsNotJson() throws UnhandledRequestTypeException {
        DefaultGoPluginApiRequest parseDirectoryRequest = new DefaultGoPluginApiRequest("configrepo", "1.0", "parse-directory");
        parseDirectoryRequest.setRequestBody("{bla");

        GoPluginApiResponse response = plugin.handle(parseDirectoryRequest);
        assertThat(response.responseCode(), is(DefaultGoPluginApiResponse.BAD_REQUEST));
    }

    @Test
    public void shouldConsumePluginSettingsOnConfigChangeRequest() throws UnhandledRequestTypeException {
        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest("configrepo", "2.0", REQ_PLUGIN_SETTINGS_CHANGED);
        request.setRequestBody("{\"file_pattern\": \"*.foo.gocd.yaml\"}");

        assertEquals(DEFAULT_FILE_PATTERN, plugin.getFilePattern());
        GoPluginApiResponse response = plugin.handle(request);

        assertEquals("*.foo.gocd.yaml", plugin.getFilePattern());
        assertThat(response.responseCode(), is(SUCCESS_RESPONSE_CODE));
    }

    @Test
    public void shouldConsumeJsonnetFlagsOnConfigChangeRequest() throws UnhandledRequestTypeException {
        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest("configrepo", "2.0", REQ_PLUGIN_SETTINGS_CHANGED);
        request.setRequestBody("{\"jsonnet_flags\": \"--ext-code output-files=false\"}");

        assertEquals(DEFAULT_FILE_PATTERN, plugin.getFilePattern());
        GoPluginApiResponse response = plugin.handle(request);

        assertEquals("--ext-code output-files=false", plugin.getJsonnetFlags());
        assertThat(response.responseCode(), is(SUCCESS_RESPONSE_CODE));
    }

    @Test
    public void shouldRespondSuccessToParseDirectoryRequestWhenPluginHasConfiguration() throws UnhandledRequestTypeException {
        GoApiResponse settingsResponse = DefaultGoApiResponse.success("{}");
        when(goAccessor.submit(any())).thenReturn(settingsResponse);

        GoPluginApiResponse response = parseAndGetResponseForDir(tempDir.toFile());

        verify(goAccessor, times(1)).submit(any());
        assertThat(response.responseCode(), is(SUCCESS_RESPONSE_CODE));
    }

    @Test
    public void shouldContainValidFieldsInResponseMessage() throws UnhandledRequestTypeException {
        GoApiResponse settingsResponse = DefaultGoApiResponse.success("{}");
        when(goAccessor.submit(any())).thenReturn(settingsResponse);

        GoPluginApiResponse response = parseAndGetResponseForDir(tempDir.toFile());

        assertThat(response.responseCode(), is(SUCCESS_RESPONSE_CODE));
        final JsonParser parser = new JsonParser();
        JsonElement responseObj = parser.parse(response.responseBody());
        assertTrue(responseObj.isJsonObject());
        JsonObject obj = responseObj.getAsJsonObject();
        assertTrue(obj.has("errors"));
        assertTrue(obj.has("pipelines"));
        assertTrue(obj.has("environments"));
        assertTrue(obj.has("target_version"));
    }

    @Test
    public void shouldRespondSuccessToParseDirectoryRequestWhenAliasesCaseFile() throws UnhandledRequestTypeException,
            IOException {
        GoPluginApiResponse response = parseAndGetResponseForDir(setupCase("aliases"));

        assertThat(response.responseCode(), is(SUCCESS_RESPONSE_CODE));
        JsonObject responseJsonObject = getJsonObjectFromResponse(response);
        assertNoError(responseJsonObject);
        JsonArray pipelines = responseJsonObject.get("pipelines").getAsJsonArray();
        assertThat(pipelines.size(), is(1));
        JsonObject expected = (JsonObject) readJsonObject("examples.out/aliases.gocd.json");
        assertThat(responseJsonObject, is(new JsonObjectMatcher(expected)));
    }

    @Test
    public void shouldUpdateTargetVersionWhenItIsTheSameAcrossAllFiles() throws Exception {
        FileUtils.copyInputStreamToFile(getResourceAsStream("/parts/roots/version_2.jsonnet"), Files.createFile(tempDir.resolve("v2_1.gocd.jsonnet")).toFile());
        FileUtils.copyInputStreamToFile(getResourceAsStream("/parts/roots/version_2.jsonnet"), Files.createFile(tempDir.resolve("v2_2.gocd.jsonnet")).toFile());

        GoPluginApiResponse response = parseAndGetResponseForDir(tempDir.toFile());
        assertNoError(getJsonObjectFromResponse(response));
    }

    @Test
    public void shouldUpdateTargetVersionWhenItIsTheDefaultOrMissingAcrossAllPipelinesAndEnvironments() throws Exception {
        FileUtils.copyInputStreamToFile(getResourceAsStream("/parts/roots/version_1.jsonnet"), Files.createFile(tempDir.resolve("v1_1.gocd.jsonnet")).toFile());
        FileUtils.copyInputStreamToFile(getResourceAsStream("/parts/roots/version_not_present.jsonnet"), Files.createFile(tempDir.resolve("v1_not_present.gocd.jsonnet")).toFile());
        FileUtils.copyInputStreamToFile(getResourceAsStream("/parts/roots/version_1.jsonnet"), Files.createFile(tempDir.resolve("v1_2.gocd.jsonnet")).toFile());

        GoPluginApiResponse response = parseAndGetResponseForDir(tempDir.toFile());
        assertNoError(getJsonObjectFromResponse(response));
    }

    @Test
    public void shouldFailToUpdateTargetVersionWhenItIs_NOT_TheSameAcrossAllFiles() throws Exception {
        FileUtils.copyInputStreamToFile(getResourceAsStream("/parts/roots/version_1.jsonnet"), Files.createFile(tempDir.resolve("v1_1.gocd.jsonnet")).toFile());
        FileUtils.copyInputStreamToFile(getResourceAsStream("/parts/roots/version_1.jsonnet"), Files.createFile(tempDir.resolve("v1_2.gocd.jsonnet")).toFile());
        FileUtils.copyInputStreamToFile(getResourceAsStream("/parts/roots/version_2.jsonnet"), Files.createFile(tempDir.resolve("v2_1.gocd.jsonnet")).toFile());

        GoPluginApiResponse response = parseAndGetResponseForDir(tempDir.toFile());
        String expectedFailureMessage = "java.lang.RuntimeException: Versions across files are not unique. Found" +
                " versions: [1, 2]. There can only be one version across the whole repository.";
        assertFirstError(getJsonObjectFromResponse(response), expectedFailureMessage, "Jsonnet config plugin");
    }

    @Test
    public void shouldRespondWithCapabilities() throws UnhandledRequestTypeException {
        String expected = new Gson().toJson(new Capabilities());
        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest("configrepo", "2.0", "get-capabilities");

        GoPluginApiResponse response = plugin.handle(request);

        assertThat(response.responseCode(), is(SUCCESS_RESPONSE_CODE));
        assertThat(response.responseBody(), is(expected));
    }

    @Test
    public void shouldRespondWithGetIcon() throws UnhandledRequestTypeException, IOException {
        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest("configrepo", "2.0", "get-icon");

        GoPluginApiResponse response = plugin.handle(request);
        assertThat(response.responseCode(), is(SUCCESS_RESPONSE_CODE));
        JsonObject jsonObject = getJsonObjectFromResponse(response);
        assertEquals(jsonObject.entrySet().size(), 2);
        assertEquals(jsonObject.get("content_type").getAsString(), "image/svg+xml");
        byte[] actualData = Base64.getDecoder().decode(jsonObject.get("data").getAsString());
        byte[] expectedData = IOUtils.toByteArray(getClass().getResourceAsStream("/yaml.svg"));
        assertArrayEquals(expectedData, actualData);
    }

    @Test
    public void shouldCreateVendorDirectory() throws UnhandledRequestTypeException, IOException {
        File rootDir = setupCase("imported");
        File jsonnetFile = new File(rootDir, "jsonnetfile.json");
        FileUtils.copyInputStreamToFile(getResourceAsStream("examples/jsonnetfile.json"), jsonnetFile);

        GoPluginApiResponse response = parseAndGetResponseForDir(rootDir);
        assertThat(response.responseCode(), is(SUCCESS_RESPONSE_CODE));
        assertNoError(getJsonObjectFromResponse(response));

        File vendorDirectory = new File(rootDir, "vendor");
        assertTrue(vendorDirectory.exists());
    }

    @Test
    public void shouldFailToCreateVendorDirectoryFromOutsideRoot() throws UnhandledRequestTypeException, IOException {
        File rootDir = setupCaseNested("imported", "nested");
        File jsonnetFile = new File(rootDir, "jsonnetfile.json");
        FileUtils.copyInputStreamToFile(getResourceAsStream("examples/jsonnetfile.json"), jsonnetFile);

        GoPluginApiResponse response = parseAndGetResponseForDir(rootDir);
        assertThat(response.responseCode(), is(SUCCESS_RESPONSE_CODE));
        assertFirstErrorContains(getJsonObjectFromResponse(response), "RUNTIME ERROR: couldn't open import", "nested/imported.gocd.jsonnet");

        File vendorDirectory = new File(rootDir, "vendor");
        assertFalse(vendorDirectory.exists());
    }

    @Test
    public void shouldRespondSuccessToParseDirectoryRequestWhenImportedCaseFile() throws UnhandledRequestTypeException, IOException {
        File rootDir = setupCase("imported");
        File jsonnetFile = new File(rootDir, "jsonnetfile.json");
        FileUtils.copyInputStreamToFile(getResourceAsStream("examples/jsonnetfile.json"), jsonnetFile);

        GoPluginApiResponse response = parseAndGetResponseForDir(rootDir);
        assertThat(response.responseCode(), is(SUCCESS_RESPONSE_CODE));
        JsonObject responseJsonObject = getJsonObjectFromResponse(response);
        assertNoError(responseJsonObject);

        JsonObject expected = (JsonObject) readJsonObject("examples.out/imported.gocd.json");
        assertThat(responseJsonObject, is(new JsonObjectMatcher(expected)));
    }

    @Test
    public void shouldRespondSuccessRuntimeErrorToParseNestedDirectoryRequestWhenImportedCaseFile() throws UnhandledRequestTypeException, IOException {
        File rootDir = setupCaseNested("imported", "nested");
        File jsonnetFile = new File(rootDir, "jsonnetfile.json");
        FileUtils.copyInputStreamToFile(getResourceAsStream("examples/jsonnetfile.json"), jsonnetFile);

        GoPluginApiResponse response = parseAndGetResponseForDir(rootDir);
        assertThat(response.responseCode(), is(SUCCESS_RESPONSE_CODE));
        JsonObject responseJsonObject = getJsonObjectFromResponse(response);
        assertFirstErrorContains(responseJsonObject, "RUNTIME ERROR: couldn't open import", "nested/imported.gocd.jsonnet");
    }

    @Test
    public void shouldRespondSuccessWithRuntimeErrorMessageWhenJsonnetFileIsMissing() throws UnhandledRequestTypeException, IOException {
        File rootDir = setupCase("imported");

        GoPluginApiResponse response = parseAndGetResponseForDir(rootDir);
        assertThat(response.responseCode(), is(SUCCESS_RESPONSE_CODE));
        assertFirstErrorContains(getJsonObjectFromResponse(response), "RUNTIME ERROR: couldn't open import", "imported.gocd.jsonnet");
    }

    @Test
    public void shouldHandleMultiplePipelinesInOneFile() throws UnhandledRequestTypeException, IOException {
        File rootDir = setupCase("multiple-pipelines");

        GoPluginApiResponse response = parseAndGetResponseForDir(rootDir);
        assertThat(response.responseCode(), is(DefaultGoPluginApiResponse.INTERNAL_ERROR));
        JsonObject responseJsonObject = getJsonObjectFromResponse(response);
        assertFirstError(responseJsonObject, "cd.go.plugin.config.yaml.YamlConfigException: simple-1.yaml is invalid, expected format_version, pipelines, environments, or common", "Jsonnet config plugin");
    }

    @Test
    public void shouldHandleNoMatchingFiles() throws UnhandledRequestTypeException, IOException {
        File rootDir = new File(tempDir.toFile(), "no-matching-files");
        GoPluginApiResponse response = parseAndGetResponseForDir(rootDir);
        assertThat(response.responseCode(), is(DefaultGoPluginApiResponse.INTERNAL_ERROR));
        JsonObject responseJsonObject = getJsonObjectFromResponse(response);
        assertFirstError(responseJsonObject, "java.lang.IllegalStateException: basedir " + rootDir.toPath() + " does not exist.", "Jsonnet config plugin");
    }

    private File setupCaseYaml(String caseName) throws IOException {
        File simpleFile = Files.createFile(tempDir.resolve(caseName + ".gocd.yaml")).toFile();
        FileUtils.copyInputStreamToFile(getResourceAsStream("examples/" + caseName + ".gocd.yaml"), simpleFile);
        return tempDir.toFile();
    }

    private File setupCaseJsonnetAndYaml(String caseName) throws IOException {
        File simpleFileYaml = Files.createFile(tempDir.resolve(caseName + ".gocd.yaml")).toFile();
        FileUtils.copyInputStreamToFile(getResourceAsStream("examples/" + caseName + ".gocd.yaml"), simpleFileYaml);
        File simpleFileJsonnet = Files.createFile(tempDir.resolve(caseName + ".gocd.jsonnet")).toFile();
        FileUtils.copyInputStreamToFile(getResourceAsStream("examples/" + caseName + ".gocd.jsonnet"), simpleFileJsonnet);
        return tempDir.toFile();
    }

    private File setupCase(String caseName) throws IOException {
        return setupCase(caseName, "gocd.jsonnet");
    }

    private File setupCase(String caseName, String extension) throws IOException {
        File simpleFile = Files.createFile(tempDir.resolve(caseName + "." + extension)).toFile();
        FileUtils.copyInputStreamToFile(getResourceAsStream("examples/" + caseName + ".gocd.jsonnet"), simpleFile);
        return tempDir.toFile();
    }

    private File setupCaseNested(String caseName, String nestedDir) throws IOException {
        File nestedDirFile = Files.createDirectory(tempDir.resolve(nestedDir)).toFile();
        File simpleFile = Files.createFile(nestedDirFile.toPath().resolve(caseName + ".gocd.jsonnet")).toFile();
        FileUtils.copyInputStreamToFile(getResourceAsStream("examples/" + caseName + ".gocd.jsonnet"), simpleFile);
        return tempDir.toFile();
    }

    private GoPluginApiResponse parseAndGetResponseForDir(File directory) throws UnhandledRequestTypeException {
        DefaultGoPluginApiRequest parseDirectoryRequest = new DefaultGoPluginApiRequest("configrepo", "1.0", "parse-directory");
        String requestBody = "{\n" +
                "    \"directory\":\"" + directory + "\",\n" +
                "    \"configurations\":[]\n" +
                "}";
        parseDirectoryRequest.setRequestBody(requestBody);

        return plugin.handle(parseDirectoryRequest);
    }

    private void assertNoError(JsonObject responseJsonObject) {
        assertThat(responseJsonObject.get("errors"), Is.<JsonElement>is(new JsonArray()));
    }

    private void assertFirstError(JsonObject responseJsonObject, String expectedMessage, String expectedLocation) {
        JsonArray errors = (JsonArray) responseJsonObject.get("errors");
        assertThat(errors.get(0).getAsJsonObject().getAsJsonPrimitive("message").getAsString(), is(expectedMessage));
        assertThat(errors.get(0).getAsJsonObject().getAsJsonPrimitive("location").getAsString(), is(expectedLocation));
    }

    private void assertFirstErrorContains(JsonObject responseJsonObject, String expectedMessage, String expectedLocation) {
        JsonArray errors = (JsonArray) responseJsonObject.get("errors");
        assertTrue(errors.get(0).getAsJsonObject().getAsJsonPrimitive("message").getAsString().contains(expectedMessage));
        assertThat(errors.get(0).getAsJsonObject().getAsJsonPrimitive("location").getAsString(), is(expectedLocation));
    }

    private JsonObject getJsonObjectFromResponse(GoPluginApiResponse response) {
        String responseBody = response.responseBody();
        return parser.parse(responseBody).getAsJsonObject();
    }
}
