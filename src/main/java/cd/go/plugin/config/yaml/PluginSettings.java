package cd.go.plugin.config.yaml;

import java.util.Map;

class PluginSettings {
    static final String PLUGIN_SETTINGS_FILE_PATTERN = "file_pattern";
    static final String PLUGIN_SETTINGS_JSONNET_COMMAND = "jsonnet_command";
    static final String PLUGIN_SETTINGS_JB_COMMAND = "jb_command";
    static final String DEFAULT_FILE_PATTERN = "**/*.gocd.jsonnet,**/jsonnetfile.json";
    static final String DEFAULT_JSONNET_COMMAND = "jsonnet";
    static final String DEFAULT_JB_COMMAND = "jb";

    private String filePattern;
    private String jsonnetCommand;
    private String jbCommand;

    PluginSettings() {
    }

    PluginSettings(String filePattern, String jsonnetCommand, String jbCommand) {
        this.filePattern = filePattern;
        this.jsonnetCommand = jsonnetCommand;
        this.jbCommand = jbCommand;
    }

    static PluginSettings fromJson(String json) {
        Map<String, String> raw = JSONUtils.fromJSON(json);
        return new PluginSettings(
            raw.get(PLUGIN_SETTINGS_FILE_PATTERN),
            raw.get(PLUGIN_SETTINGS_JSONNET_COMMAND),
            raw.get(PLUGIN_SETTINGS_JB_COMMAND)
        );
    }

    String getFilePattern() {
        return filePattern;
    }

    String getJsonnetCommand() {
        return jsonnetCommand;
    }

    String getJbCommand() {
        return jbCommand;
    }
}