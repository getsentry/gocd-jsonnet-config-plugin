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
    public void shouldGetRootDirectory() {
        PluginSettings pluginSettings = new PluginSettings(null, "root-directory");

        assertThat(pluginSettings.getRootDirectory(), is(equalTo("root-directory")));
    }

    @Test
    public void shouldGetFilePatternAndRootDirectory() {
        PluginSettings pluginSettings = new PluginSettings("file-pattern", "root-directory");

        assertThat(pluginSettings.getFilePattern(), is(equalTo("file-pattern")));
        assertThat(pluginSettings.getRootDirectory(), is(equalTo("root-directory")));
    }
}