package cd.go.plugin.config.yaml;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class PluginSettingsTest {
    @Test
    public void shouldGetFilePattern() {
        PluginSettings pluginSettings = new PluginSettings("file-pattern", null);

        assertThat(pluginSettings.getFilePattern(), is(equalTo("file-pattern")));
    }

    @Test
    public void shouldGetJsonnetCommand() {
        PluginSettings pluginSettings = new PluginSettings(null, "jsonnet-command");

        assertThat(pluginSettings.getJsonnetCommand(), is(equalTo("jsonnet-command")));
    }

    @Test
    public void shouldGetFilePatternAndJsonnetCommand() {
        PluginSettings pluginSettings = new PluginSettings("file-pattern", "jsonnet-command");

        assertThat(pluginSettings.getFilePattern(), is(equalTo("file-pattern")));
        assertThat(pluginSettings.getJsonnetCommand(), is(equalTo("jsonnet-command")));
    }
}