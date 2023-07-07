package cd.go.plugin.config.yaml;

import java.util.Map;

class PluginSettings {
    static final String PLUGIN_SETTINGS_FILE_PATTERN = "file_pattern";
    static final String PLUGIN_SETTINGS_ROOT_DIRECTORY = "root_directory";
    static final String PLUGIN_SETTINGS_JSONNET_COMMAND = "jsonnet_command";
    static final String DEFAULT_FILE_PATTERN = "**/*.gocd.jsonnet";
    static final String DEFAULT_ROOT_DIRECTORY = "./gocd/templates";
    static final String DEFAULT_JSONNET_COMMAND = "jsonnet";

    private String filePattern;
    private String rootDirectory;
    private String jsonnetCommand;

    PluginSettings() {
    }

    PluginSettings(String filePattern, String rootDirectory, String jsonnetCommand) {
        this.filePattern = filePattern;
        this.rootDirectory = rootDirectory;
        this.jsonnetCommand = jsonnetCommand;
    }

    static PluginSettings fromJson(String json) {
        Map<String, String> raw = JSONUtils.fromJSON(json);
        return new PluginSettings(
            raw.get(PLUGIN_SETTINGS_FILE_PATTERN),
            raw.get(PLUGIN_SETTINGS_ROOT_DIRECTORY),
            raw.get(PLUGIN_SETTINGS_JSONNET_COMMAND)
        );
    }

    String getFilePattern() {
        return filePattern;
    }

    String getRootDirectory() {
        return rootDirectory;
    }

    String getJsonnetCommand() {
        return jsonnetCommand;
    }
}