package cd.go.plugin.config.yaml;

import java.util.Map;

class PluginSettings {
    static final String PLUGIN_SETTINGS_FILE_PATTERN = "file_pattern";
    static final String PLUGIN_SETTINGS_ROOT_DIRECTORY = "root_directory";
    static final String DEFAULT_FILE_PATTERN = "**/*.gocd.jsonnet";
    static final String DEFAULT_ROOT_DIRECTORY = "./gocd/templates";

    private String filePattern;
    private String rootDirectory;

    PluginSettings() {
    }

    PluginSettings(String filePattern, String rootDirectory) {
        this.filePattern = filePattern;
        this.rootDirectory = rootDirectory;
    }

    static PluginSettings fromJson(String json) {
        Map<String, String> raw = JSONUtils.fromJSON(json);
        return new PluginSettings(raw.get(PLUGIN_SETTINGS_FILE_PATTERN), raw.get(PLUGIN_SETTINGS_ROOT_DIRECTORY));
    }

    String getFilePattern() {
        return filePattern;
    }

    String getRootDirectory() {
        return rootDirectory;
    }
}