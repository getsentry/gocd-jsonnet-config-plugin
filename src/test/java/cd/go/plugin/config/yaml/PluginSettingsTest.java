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
    public void shouldGetJsonnetCommand() {
        PluginSettings pluginSettings = new PluginSettings(null, "jsonnet-command", null);

        assertThat(pluginSettings.getJsonnetCommand(), is(equalTo("jsonnet-command")));
    }

    @Test
    public void shouldGetJsonnetFlags() {
        PluginSettings pluginSettings = new PluginSettings(null, null, "jsonnet-flags");

        assertThat(pluginSettings.getJsonnetFlags(), is(equalTo("jsonnet-flags")));
    }

    @Test
    public void shouldGetFilePatternAndJsonnetCommand() {
        PluginSettings pluginSettings = new PluginSettings("file-pattern", "jsonnet-command", null);

        assertThat(pluginSettings.getFilePattern(), is(equalTo("file-pattern")));
        assertThat(pluginSettings.getJsonnetCommand(), is(equalTo("jsonnet-command")));
    }

    @Test
    public void shouldGetFilePatternAndJsonnetFlags() {
        PluginSettings pluginSettings = new PluginSettings("file-pattern", null, "jsonnet-flags");

        assertThat(pluginSettings.getFilePattern(), is(equalTo("file-pattern")));
        assertThat(pluginSettings.getJsonnetFlags(), is(equalTo("jsonnet-flags")));
    }

    @Test
    public void shouldGetJsonnetCommandAndJsonnetFlags() {
        PluginSettings pluginSettings = new PluginSettings(null, "jsonnet-command", "jsonnet-flags");

        assertThat(pluginSettings.getJsonnetCommand(), is(equalTo("jsonnet-command")));
        assertThat(pluginSettings.getJsonnetFlags(), is(equalTo("jsonnet-flags")));
    }

    @Test
    public void shouldGetFilePatternAndJsonnetCommandAndJsonnetFlags() {
        PluginSettings pluginSettings = new PluginSettings("file-pattern", "jsonnet-command", "jsonnet-flags");

        assertThat(pluginSettings.getFilePattern(), is(equalTo("file-pattern")));
        assertThat(pluginSettings.getJsonnetCommand(), is(equalTo("jsonnet-command")));
        assertThat(pluginSettings.getJsonnetFlags(), is(equalTo("jsonnet-flags")));
    }
}