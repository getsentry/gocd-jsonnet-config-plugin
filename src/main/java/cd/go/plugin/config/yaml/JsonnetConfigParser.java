package cd.go.plugin.config.yaml;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.go.plugin.api.logging.Logger;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cd.go.plugin.config.yaml.transforms.RootTransform;

/**
 * Extends {@link YamlConfigParser} to parse jsonnet files into a JsonConfigCollection.
 */
public class JsonnetConfigParser extends YamlConfigParser {
    private static final String VENDOR_TREE_NAME = "vendor";
    private static final String JSONNET_FILE_NAME = "jsonnetfile.json";
    private static Logger LOGGER = Logger.getLoggerFor(JsonnetConfigParser.class);
    private String jsonnetCommand;

    /**
     * Create a new JsonnetConfigParser.
     * @param jsonnetCommand The command to run to execute jsonnet
     * @see YamlConfigParser#YamlConfigParser(RootTransform)
     */
    public JsonnetConfigParser(String jsonnetCommand) {
        super(new RootTransform());
        this.jsonnetCommand = jsonnetCommand;
    }

    /**
     * Parse a list of jsonnet files into a JsonConfigCollection.
     * 
     * @param baseDir The base directory to resolve relative paths against
     * @param files The list of jsonnet files to parse
     * @return The JsonConfigCollection containing the parsed config
     */
    @Override
    public JsonConfigCollection parseFiles(File baseDir, String[] files) {
        LOGGER.info("Parsing jsonnet files " + baseDir + File.separator + String.join(", ", files));
        JsonConfigCollection collection = new JsonConfigCollection();
        for (String file : files) {
            if (!file.endsWith(".jsonnet")) {
                LOGGER.info("Skipping non-jsonnet file " + file);
                continue;
            }
            try {
                File filePath = new File(baseDir, file);
                boolean bundled = bundleJsonnet(new File(filePath.getParent()));
                List<String> commands = new ArrayList<>();
                commands.add(filePath.toString());
                if (bundled) {
                    commands.add("-J");
                    commands.add(filePath.getParent() + File.separator + VENDOR_TREE_NAME);
                }
                InputStream input = compileJsonnet(commands.toArray(new String[0]));
                // Mark the input stream so we can reset it after parsing the jsonnet file
                input.mark(Integer.MAX_VALUE);
                String inputString = new String(input.readAllBytes());
                input.reset();
                Map<String, Object> inputMap = new Gson().fromJson(inputString, new TypeToken<Map<String, Object>>(){}.getType());
                boolean isNested = false;
                for (Map.Entry<String, Object> pe : inputMap.entrySet()) {
                    // If the jsonnet file contains a nested yaml file, parse it
                    if (pe.getKey().endsWith(".yaml") || pe.getKey().endsWith(".yml")) {
                        Gson gson = new Gson();
                        String json = gson.toJson(pe.getValue());
                        // Calling YamlConfigParser's parseStream method (instead of the overridden one below)
                        super.parseStream(collection, new ByteArrayInputStream(json.getBytes()), pe.getKey());
                        isNested = true;
                    }
                }
                // If the jsonnet file contains a nested yaml file, skip the rest of the loop
                if (isNested) continue;
                // Calling YamlConfigParser's parseStream method (instead of the overridden one below)
                super.parseStream(collection, input, file);
            } catch (NullPointerException e) {
                collection.addError("File matching GoCD Jsonnet pattern disappeared", file);
            } catch (JsonnetEvalException e) {
                collection.addError(e.getMessage(), file);
            } catch (IOException e) {
                collection.addError("Error while reading Jsonnet file", file);
            }
        }
        return collection;   
    }
    
    /** 
     * Parse a stream of jsonnet into a JsonConfigCollection.
     * 
     * @param result The JsonConfigCollection to add the parsed config to
     * @param input The InputStream containing the jsonnet to parse
     * @param location The location of the jsonnet file
     */
    @Override
    public void parseStream(JsonConfigCollection result, InputStream input, String location) {
        if (location.endsWith(".yaml") || location.endsWith(".yml")) {
            super.parseStream(result, input, location);
            return;
        }
        try {
            InputStream jsonInputStream = compileJsonnet(input);
            super.parseStream(result, jsonInputStream, location);
        } catch (NullPointerException ex) {
            result.addError("File matching GoCD Jsonnet pattern disappeared", location);
        } catch (IOException ex) {
            result.addError("Error while reading Jsonnet file", location);
        } catch (JsonnetEvalException ex) {
            result.addError(ex.getMessage(), location);
        }
    }

    /**
     * Compile jsonnet using the jrsonnet command line tool.
     * 
     * @param input InputStream containing the jsonnet to compile
     * @return InputStream containing the compiled jsonnet
     * @throws IOException if there is an error reading the input stream
     * @throws JsonnetEvalException if jrsonnet exits with an error
     */
    private InputStream compileJsonnet(InputStream input) throws IOException, JsonnetEvalException {
        String inputString = new String(input.readAllBytes());
        return compileJsonnet("--exec", inputString);
    }
    
    /** 
     * Compile jsonnet using the jrsonnet command line tool.
     * 
     * @param command The command to pass to jrsonnet
     * @return InputStream containing the compiled jsonnet
     * @throws JsonnetEvalException if jrsonnet exits with an error
     */
    private InputStream compileJsonnet(String... command) throws JsonnetEvalException {
        String[] commandWithArgs = new String[command.length + 1];
        commandWithArgs[0] = jsonnetCommand;
        System.arraycopy(command, 0, commandWithArgs, 1, command.length);
        try {
            ProcessBuilder pb = new ProcessBuilder(commandWithArgs);
            Process p = pb.start();
            int exitCode = p.waitFor();
            if (exitCode != 0) {
                String error = new String(p.getErrorStream().readAllBytes());
                throw new Exception("Jsonnet exited with an error: " + error + "\n" + "Command: " + String.join(" ", commandWithArgs));
            }
            return p.getInputStream();
        } catch (Exception e) {
            throw new JsonnetEvalException("Error while evaluating jsonnet: " + e.getMessage());
        }
    }

    /**
     * Run the jsonnet-bundler to bundle the jsonnet dependencies.
     * 
     * @param baseDir The base directory to run the bundler in
     * @return Whether the bundler was run
     * @throws JsonnetEvalException if the bundler exits with an error
     */
    private boolean bundleJsonnet(File baseDir) throws JsonnetEvalException {
        // Check if <baseDir>/jsonnetfile.json exists
        File jsonnetFile = new File(baseDir + File.separator + JSONNET_FILE_NAME);
        LOGGER.info("Checking for " + jsonnetFile.toPath() + " in " + baseDir.toPath());
        if (!jsonnetFile.exists()) {
            LOGGER.info("No jsonnetfile.json found, skipping jsonnet-bundler");
            // If the jsonnetfile.json doesn't exist, don't run the bundler
            return false;
        }
        try {
            ProcessBuilder pb = new ProcessBuilder("jb", "install");
            pb.directory(baseDir);
            Process p = pb.start();
            int exitCode = p.waitFor();
            if (exitCode != 0) {
                String error = new String(p.getErrorStream().readAllBytes());
                throw new Exception(error);
            }
            LOGGER.info("Successfully ran jsonnet-bundler");
            return true;
        } catch (Exception e) {
            throw new JsonnetEvalException("Error while bundling jsonnet: " + e.getMessage());
        }
    }
}
