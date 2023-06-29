package cd.go.plugin.config.yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import cd.go.plugin.config.yaml.transforms.RootTransform;

public class JsonnetConfigParser extends YamlConfigParser {
    public JsonnetConfigParser() {
        super(new RootTransform());
    }

    @Override
    public JsonConfigCollection parseFiles(File baseDir, String[] files) {
        JsonConfigCollection collection = new JsonConfigCollection();
        for (String file : files) {
            try {
                File filePath = new File(baseDir, file);
                InputStream input = compileJsonnet(filePath.toString());
                // TODO: Properly handle this case when an alternate file pattern is specified
                String updatedFile = file.replace(".jsonnet", ".yaml");
                super.parseStream(collection, input, updatedFile);
            } catch (NullPointerException e) {
                collection.addError("File matching GoCD Jsonnet pattern disappeared", file);
            }
        }
        return collection;   
    }

    @Override
    public void parseStream(JsonConfigCollection result, InputStream input, String location) {
        try {
            InputStream jsonInputStream = compileJsonnet(input);
            super.parseStream(result, jsonInputStream, location);
        } catch (NullPointerException ex) {
            result.addError("File matching GoCD Jsonnet pattern disappeared", location);
        }
    }

    private InputStream compileJsonnet(String filePath) {
        InputStream jsonInputStream = null;
        try {
            ProcessBuilder pb = new ProcessBuilder("jrsonnet", filePath, "--format", "yaml");
            Process p = pb.start();
            int exitCode = p.waitFor();
            if (exitCode != 0) {
                System.out.println("Error compiling jsonnet file: " + filePath);
            }
            jsonInputStream = p.getInputStream();
        } catch (IOException e) {
            System.out.println("Error compiling jsonnet file: " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("Error compiling jsonnet file: " + e.getMessage());
        }
        return jsonInputStream;
    }

    private InputStream compileJsonnet(InputStream input) {
        InputStream jsonInputStream = null;
        try {
            String inputString = new String(input.readAllBytes());
            ProcessBuilder pb = new ProcessBuilder("jrsonnet", "--exec", inputString, "--format", "yaml");
            Process p = pb.start();
            int exitCode = p.waitFor();
            if (exitCode != 0) {
                System.out.println("Error compiling jsonnet: " + inputString);
            }
            jsonInputStream = p.getInputStream();
        } catch (IOException e) {
            System.out.println("Error compiling jsonnet file: " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("Error compiling jsonnet file: " + e.getMessage());
        }
        return jsonInputStream;
    }
}
