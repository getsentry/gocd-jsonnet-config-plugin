package cd.go.plugin.config.yaml;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class PluginSettingsTest {
    @Test
    public void shouldGetFilePattern() {
        PluginSettings pluginSettings = new PluginSettings("file-pattern", null, null);

        assertThat(pluginSettings.getFilePattern(), is(equalTo("file-pattern")));
    }

    @Test
    public void shouldGetRootDirectory() {
        PluginSettings pluginSettings = new PluginSettings(null, "root-directory", null);

        assertThat(pluginSettings.getRootDirectory(), is(equalTo("root-directory")));
    }

    @Test
    public void shouldGetJsonnetCommand() {
        PluginSettings pluginSettings = new PluginSettings(null, null, "jsonnet-command");

        assertThat(pluginSettings.getJsonnetCommand(), is(equalTo("jsonnet-command")));
    }

    @Test
    public void shouldGetFilePatternAndRootDirectory() {
        PluginSettings pluginSettings = new PluginSettings("file-pattern", "root-directory", null);

        assertThat(pluginSettings.getFilePattern(), is(equalTo("file-pattern")));
        assertThat(pluginSettings.getRootDirectory(), is(equalTo("root-directory")));
    }

    @Test
    public void shouldGetFilePatternAndJsonnetCommand() {
        PluginSettings pluginSettings = new PluginSettings("file-pattern", null, "jsonnet-command");

        assertThat(pluginSettings.getFilePattern(), is(equalTo("file-pattern")));
        assertThat(pluginSettings.getJsonnetCommand(), is(equalTo("jsonnet-command")));
    }

    @Test
    public void shouldGetRootDirectoryAndJsonnetCommand() {
        PluginSettings pluginSettings = new PluginSettings(null, "root-directory", "jsonnet-command");

        assertThat(pluginSettings.getRootDirectory(), is(equalTo("root-directory")));
        assertThat(pluginSettings.getJsonnetCommand(), is(equalTo("jsonnet-command")));
    }

    @Test
    public void shouldGetFilePatternAndRootDirectoryAndJsonnetCommand() {
        PluginSettings pluginSettings = new PluginSettings("file-pattern", "root-directory", "jsonnet-command");

        assertThat(pluginSettings.getFilePattern(), is(equalTo("file-pattern")));
        assertThat(pluginSettings.getRootDirectory(), is(equalTo("root-directory")));
        assertThat(pluginSettings.getJsonnetCommand(), is(equalTo("jsonnet-command")));
    }
}