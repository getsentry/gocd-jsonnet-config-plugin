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
    public void shouldGetJbCommand() {
        PluginSettings pluginSettings = new PluginSettings(null, null, "jb-command");

        assertThat(pluginSettings.getJbCommand(), is(equalTo("jb-command")));
    }

    @Test
    public void shouldGetFilePatternAndJsonnetCommand() {
        PluginSettings pluginSettings = new PluginSettings("file-pattern", "jsonnet-command", null);

        assertThat(pluginSettings.getFilePattern(), is(equalTo("file-pattern")));
        assertThat(pluginSettings.getJsonnetCommand(), is(equalTo("jsonnet-command")));
    }

    @Test
    public void shouldGetFilePatternAndJbCommand() {
        PluginSettings pluginSettings = new PluginSettings("file-pattern", null, "jb-command");

        assertThat(pluginSettings.getFilePattern(), is(equalTo("file-pattern")));
        assertThat(pluginSettings.getJbCommand(), is(equalTo("jb-command")));
    }

    @Test
    public void shouldGetJsonnetCommandAndJbCommand() {
        PluginSettings pluginSettings = new PluginSettings(null, "jsonnet-command", "jb-command");

        assertThat(pluginSettings.getJsonnetCommand(), is(equalTo("jsonnet-command")));
        assertThat(pluginSettings.getJbCommand(), is(equalTo("jb-command")));
    }

    @Test
    public void shouldGetFilePatternAndJsonnetCommandAndJbCommand() {
        PluginSettings pluginSettings = new PluginSettings("file-pattern", "jsonnet-command", "jb-command");

        assertThat(pluginSettings.getFilePattern(), is(equalTo("file-pattern")));
        assertThat(pluginSettings.getJsonnetCommand(), is(equalTo("jsonnet-command")));
        assertThat(pluginSettings.getJbCommand(), is(equalTo("jb-command")));
    }
}