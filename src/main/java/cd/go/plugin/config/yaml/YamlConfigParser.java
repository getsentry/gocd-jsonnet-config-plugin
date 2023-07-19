package cd.go.plugin.config.yaml;

import cd.go.plugin.config.yaml.transforms.RootTransform;
import com.esotericsoftware.yamlbeans.YamlConfig;
import com.esotericsoftware.yamlbeans.YamlReader;
import com.google.gson.Gson;

import java.io.*;
import java.util.Map;

public class YamlConfigParser {
    private RootTransform rootTransform;

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
        try (InputStreamReader contentReader = new InputStreamReader(input)) {
            if (input.available() < 1) {
                result.addError("File is empty", location);
                return;
            }

            YamlConfig config = new YamlConfig();
            config.setAllowDuplicates(false);
            YamlReader reader = new YamlReader(contentReader, config);
            Object rootObject = reader.read();
            Map<String, Object> rootMap = (Map<String, Object>) rootObject;
            boolean isNested = false;
            for (Map.Entry<String, Object> pe : rootMap.entrySet()) {
                if (pe.getKey().endsWith(".yaml")) {
                    Gson gson = new Gson();
                    String json = gson.toJson(pe.getValue());
                    parseStream(result, new ByteArrayInputStream(json.getBytes()), pe.getKey());
                    isNested = true;
                }
            }
            if (isNested) {
                return;
            }
            JsonConfigCollection filePart = rootTransform.transform(rootObject, location);
            result.append(filePart);
        } catch (YamlReader.YamlReaderException e) {
            result.addError(e.getMessage(), location);
        } catch (IOException e) {
            result.addError(e.getMessage() + " : " + e.getCause().getMessage() + " : ", location);
        }
    }
}
