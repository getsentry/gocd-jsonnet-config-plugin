package cd.go.plugin.config.yaml;

import com.thoughtworks.go.plugin.api.logging.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import cd.go.plugin.config.yaml.transforms.RootTransform;

/**
 * Extends {@link YamlConfigParser} to parse jsonnet files into a JsonConfigCollection.
 */
public class JsonnetConfigParser extends YamlConfigParser {
    private static final String VENDOR_TREE_NAME = "vendor";
    private static final String JSONNET_FILE_NAME = "jsonnetfile.json";
    private static Logger LOGGER = Logger.getLoggerFor(JsonnetConfigParser.class);
    private String jsonnetCommand;
    private String jsonnetFlags;

    /**
     * Create a new JsonnetConfigParser.
     * @param jsonnetCommand The command to run to execute jsonnet
     * @see YamlConfigParser#YamlConfigParser(RootTransform)
     */
    public JsonnetConfigParser(String jsonnetCommand, String jsonnetFlags) {
        super(new RootTransform());
        this.jsonnetCommand = jsonnetCommand;
        this.jsonnetFlags = jsonnetFlags;
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
            if (file.endsWith(JSONNET_FILE_NAME)) {
                // Skip jsonnetfile.json file(s)
                continue;
            } else if (file.endsWith(".yaml") || file.endsWith(".yml")) {
                // Parse YAML files using the superclass
                try {
                    super.parseStream(collection, new FileInputStream(new File(baseDir, file)), file);
                } catch (FileNotFoundException e) {
                    collection.addError("File matching GoCD Jsonnet/YAML pattern disappeared", file);
                }
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
                LOGGER.info("Compiling jsonnet file " + filePath + " with command " + String.join(" ", commands));
                InputStream input = compileJsonnet(commands.toArray(new String[0]));
                // Calling YamlConfigParser's parseStream method (instead of the overridden one below)
                super.parseStream(collection, input, file);
            } catch (NullPointerException e) {
                collection.addError("File matching GoCD Jsonnet pattern disappeared", file);
            } catch (JsonnetEvalException e) {
                collection.addError(e.getMessage(), file);
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
        String[] jsonnetFlagArray = jsonnetFlags != "" ? jsonnetFlags.split(" ") : new String[0];
        String[] commandWithArgs = new String[command.length + jsonnetFlagArray.length + 1];
        commandWithArgs[0] = jsonnetCommand;
        // Copy the command and flags into the new array
        System.arraycopy(command, 0, commandWithArgs, 1, command.length);
        System.arraycopy(jsonnetFlagArray, 0, commandWithArgs, command.length + 1, jsonnetFlagArray.length);
        try {
            ProcessBuilder pb = new ProcessBuilder(commandWithArgs);
            Process p = pb.start();
            int exitCode = p.waitFor();
            if (exitCode != 0) {
                String error = new String(p.getErrorStream().readAllBytes());
                LOGGER.error("Jsonnet exited with an error: " + error + "\n" + "Command: " + String.join(" ", commandWithArgs));
                throw new Exception("Jsonnet exited with an error: " + error + "\n" + "Command: " + String.join(" ", commandWithArgs));
            }
            return p.getInputStream();
        } catch (Exception e) {
            LOGGER.error("Error while evaluating jsonnet: " + e.getMessage());
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
            LOGGER.info("Successfully ran jsonnet-bundler in " + baseDir.toPath());
            LOGGER.info("Bundled dependencies are in " + baseDir.toPath() + File.separator + VENDOR_TREE_NAME);
            return true;
        } catch (Exception e) {
            throw new JsonnetEvalException("Error while bundling jsonnet: " + e.getMessage());
        }
    }
}
