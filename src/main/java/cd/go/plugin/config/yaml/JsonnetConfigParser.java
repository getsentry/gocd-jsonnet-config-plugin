package cd.go.plugin.config.yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import cd.go.plugin.config.yaml.transforms.RootTransform;

/**
 * Extends {@link YamlConfigParser} to parse jsonnet files into a JsonConfigCollection.
 */
public class JsonnetConfigParser extends YamlConfigParser {
    private static final String VENDOR_TREE_NAME = "vendor";
    private static final String JSONNET_FILE_NAME = "jsonnetfile.json";
    private String jsonnetCommand;
    private String rootDirectory;

    /**
     * Create a new JsonnetConfigParser.
     * @param jsonnetCommand The command to run to execute jsonnet
     * @param rootDirectory The root directory to resolve relative paths against
     * @see YamlConfigParser#YamlConfigParser(RootTransform)
     */
    public JsonnetConfigParser(String jsonnetCommand, String rootDirectory) {
        super(new RootTransform());
        this.jsonnetCommand = jsonnetCommand;
        this.rootDirectory = rootDirectory;
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
        JsonConfigCollection collection = new JsonConfigCollection();
        for (String file : files) {
            try {
                bundleJsonnet();
                File filePath = new File(baseDir, file);
                InputStream input = compileJsonnet(filePath.toString());
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
        try {
            bundleJsonnet();
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
        String[] commandWithArgs = new String[command.length + 3];
        commandWithArgs[0] = jsonnetCommand;
        commandWithArgs[commandWithArgs.length - 2] = "-J";
        commandWithArgs[commandWithArgs.length - 1] = rootDirectory + File.separator + VENDOR_TREE_NAME;
        System.arraycopy(command, 0, commandWithArgs, 1, command.length);
        try {
            ProcessBuilder pb = new ProcessBuilder(commandWithArgs);
            Process p = pb.start();
            int exitCode = p.waitFor();
            if (exitCode != 0) {
                String error = new String(p.getErrorStream().readAllBytes());
                throw new Exception("Jsonnet exited with an error: " + error);
            }
            return p.getInputStream();
        } catch (Exception e) {
            throw new JsonnetEvalException("Error while evaluating jsonnet: " + e.getMessage());
        }
    }

    /**
     * Run the jsonnet-bundler to bundle the jsonnet dependencies.
     */
    private void bundleJsonnet() {
        // Check if <rootDirectory>/jsonnetfile.json exists
        File jsonnetFile = new File(rootDirectory + File.separator + JSONNET_FILE_NAME);
        if (!jsonnetFile.exists()) {
            // If the jsonnetfile.json doesn't exist, don't run the bundler
            return;
        }
        try {
            ProcessBuilder pb = new ProcessBuilder("jb", "install");
            pb.directory(new File(rootDirectory));
            Process p = pb.start();
            int exitCode = p.waitFor();
            if (exitCode != 0) {
                String error = new String(p.getErrorStream().readAllBytes());
                throw new Exception(error);
            }
        } catch (Exception e) {
            throw new JsonnetEvalException("Error while bundling jsonnet: " + e.getMessage());
        }
    }
}
