package cd.go.plugin.config.yaml;

import java.util.Map;

class PluginSettings {
    static final String PLUGIN_SETTINGS_FILE_PATTERN = "file_pattern";
    static final String PLUGIN_SETTINGS_JSONNET_COMMAND = "jsonnet_command";
    static final String DEFAULT_FILE_PATTERN = "**/*.jsonnet,**/jsonnetfile.json";
    static final String DEFAULT_JSONNET_COMMAND = "jsonnet";

    private String filePattern;
    private String jsonnetCommand;

    PluginSettings() {
    }

    PluginSettings(String filePattern, String jsonnetCommand) {
        this.filePattern = filePattern;
        this.jsonnetCommand = jsonnetCommand;
    }

    static PluginSettings fromJson(String json) {
        Map<String, String> raw = JSONUtils.fromJSON(json);
        return new PluginSettings(
            raw.get(PLUGIN_SETTINGS_FILE_PATTERN),
            raw.get(PLUGIN_SETTINGS_JSONNET_COMMAND)
        );
    }

    String getFilePattern() {
        return filePattern;
    }

    String getJsonnetCommand() {
        return jsonnetCommand;
    }
}