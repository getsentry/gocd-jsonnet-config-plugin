package cd.go.plugin.config.yaml;

import cd.go.plugin.config.yaml.transforms.RootTransform;
import com.esotericsoftware.yamlbeans.YamlConfig;
import com.esotericsoftware.yamlbeans.YamlReader;
import com.thoughtworks.go.plugin.api.logging.Logger;

import java.io.*;

public class YamlConfigParser {
    private RootTransform rootTransform;
    private static Logger LOGGER = Logger.getLoggerFor(YamlConfigParser.class);

    public YamlConfigParser() {
        this(new RootTransform());
    }

    public YamlConfigParser(RootTransform rootTransform) {
        this.rootTransform = rootTransform;
    }

    public JsonConfigCollection parseFiles(File baseDir, String[] files) {
        JsonConfigCollection collection = new JsonConfigCollection();

        for (String file : files) {
            try {
                parseStream(collection, new FileInputStream(new File(baseDir, file)), file);
            } catch (FileNotFoundException ex) {
                collection.addError("File matching GoCD YAML pattern disappeared", file);
            }
        }

        return collection;
    }

    public void parseStream(JsonConfigCollection result, InputStream input, String location) {
        LOGGER.info("Parsing YAML from " + location);
        try (InputStreamReader contentReader = new InputStreamReader(input)) {
            if (input.available() < 1) {
                result.addError("File is empty", location);
                return;
            }

            YamlConfig config = new YamlConfig();
            config.setAllowDuplicates(false);
            YamlReader reader = new YamlReader(contentReader, config);
            Object rootObject = reader.read();
            JsonConfigCollection filePart = rootTransform.transform(rootObject, location);
            LOGGER.info("Transformed pipelines: " + filePart.getPipelines());
            LOGGER.info("Transformed environments: " + filePart.getEnvironments());
            LOGGER.info("Errors: " + filePart.getErrors());
            result.append(filePart);
        } catch (YamlReader.YamlReaderException e) {
            result.addError(e.getMessage(), location);
        } catch (IOException e) {
            result.addError(e.getMessage() + " : " + e.getCause().getMessage() + " : ", location);
        }
    }
}
