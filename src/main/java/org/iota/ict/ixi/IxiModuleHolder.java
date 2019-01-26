package org.iota.ict.ixi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iota.ict.IctInterface;
import org.iota.ict.Main;
import org.iota.ict.ixi.context.IxiContext;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.IOHelper;
import org.iota.ict.utils.RestartableThread;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class IxiModuleHolder extends RestartableThread {

    public static final Logger LOGGER = LogManager.getLogger("IxiMH");
    protected static final File MODULE_DIRECTORY = new File("modules/");


    protected final IctInterface ict;
    protected Map<String, IxiModule> modulesByPath = new HashMap<>();
    protected Map<IxiModule, IxiModuleInfo> modulesWithInfo = new HashMap<>();

    static {
        if (!MODULE_DIRECTORY.exists())
            MODULE_DIRECTORY.mkdirs();
    }

    public IxiModuleHolder(IctInterface ict) {
        super(LOGGER);
        this.ict = ict;
    }

    @Override
    public void run() {
    }

    public boolean uninstall(String path) {

        IxiModule module = modulesByPath.get(path);
        LOGGER.info("Uninstalling module " + path + " ...");

        if (module == null)
            throw new RuntimeException("No module '" + path + "' installed.");

        module.uninstall();

        IxiModuleInfo info = modulesWithInfo.get(module);

        modulesWithInfo.remove(module);
        modulesByPath.remove(path);
        module.terminate();

        File jar = new File(MODULE_DIRECTORY, path);
        File guiDirectory = new File(Constants.WEB_GUI_PATH, "modules/" + info.name + "/");

        if (!jar.exists())
            throw new RuntimeException("Could not find file '" + path + "'.");
        if (jar.isDirectory())
            throw new RuntimeException("'" + path + "' is a directory.");
        if (!path.endsWith(".jar"))
            throw new RuntimeException("'" + path + "' is not a .jar file.");

        boolean jarDeletedSuccess = IOHelper.deleteRecursively(jar);
        boolean guiDirectoryDeletedSuccess = IOHelper.deleteRecursively(guiDirectory);
        return jarDeletedSuccess && guiDirectoryDeletedSuccess;
    }

    public IxiModule install(URL url) throws Throwable {

        String split[] = url.getFile().split("/");
        String fileName = split[split.length - 1];
        Path target = Paths.get("./modules/" + fileName);

        LOGGER.info("Downloading " + target.toString() + " ...");
        try (InputStream in = url.openStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        LOGGER.info("Download of " + target.toString() + " complete. Installing ...");
        IxiModule module = initModule(target);
        module.install();
        LOGGER.info("Installation of " + target.toString() + " complete.");
        return module;
    }

    public void initAllModules() {
        File[] files = MODULE_DIRECTORY.listFiles();
        try {
            if (files == null)
                throw new NullPointerException("failed listing files in directory " + MODULE_DIRECTORY.getAbsolutePath());
        } catch (NullPointerException e) {
            return;
        }
        modulesWithInfo = new HashMap<>();
        initModulesFromFiles(files);
    }

    public void startAllModules() {
        for (IxiModule module : modulesWithInfo.keySet())
            if (!module.isRunning())
                module.start();
    }

    private void initModulesFromFiles(File[] files) {
        for (File file : files) {
            Path jar = Paths.get(file.toURI());
            if (!jar.toString().endsWith(".jar"))
                continue;
            try {
                initModule(jar);
            } catch (Throwable t) {
                LOGGER.error("could not load module " + jar, t);
            }
        }
    }

    private IxiModule initModule(Path jar) throws Exception {
        LOGGER.info("loading IXI module " + jar.getFileName() + "...");
        String path = MODULE_DIRECTORY.toURI().relativize(jar.toUri()).toString();
        URLClassLoader classLoader = new URLClassLoader(new URL[]{jar.toFile().toURI().toURL()}, Main.class.getClassLoader());
        IxiModuleInfo info = readModuleInfoFromJar(classLoader, path);
        if (!info.supportsCurrentVersion())
            LOGGER.warn("IXI module '" + info.name + "' does not specify your Ict's version '" + Constants.ICT_VERSION + "' as supported in module.json.");
        IxiModule module = createInstance(classLoader, info.mainClass);

        modulesWithInfo.put(module, info);
        modulesByPath.put(path, module);
        subWorkers.add(module);

        File configFile = new File(MODULE_DIRECTORY, path+".cfg");
        if(configFile.exists()) {
            JSONObject config = new JSONObject(IOHelper.readFile(configFile));
            module.getContext().tryToUpdateConfiguration(config);
        }

        return module;
    }

    private IxiModule createInstance(URLClassLoader classLoader, String mainClassName) throws Exception {
        Class ixiClass = getIxiClass(classLoader, mainClassName);
        Constructor<?> c = ixiClass.getConstructor(Ixi.class);
        return (IxiModule) c.newInstance(ict);
    }

    private static Class getIxiClass(URLClassLoader classLoader, String mainClassName) {
        try {
            return Class.forName(mainClassName, true, classLoader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Invalid IXI module: Could not find class '" + mainClassName + "'.");
        }
    }

    private static IxiModuleInfo readModuleInfoFromJar(URLClassLoader classLoader, String path) {
        try {
            InputStream is = classLoader.getResourceAsStream("module.json");
            if (is == null)
                throw new RuntimeException("Could not find 'module.json'");
            String str = IOHelper.readInputStream(is);
            JSONObject json = new JSONObject(str);
            return new IxiModuleInfo(json, path);
        } catch (IOException e) {
            throw new RuntimeException("Failed reading 'module.json' file: " + e.getMessage());
        } catch (JSONException e) {
            throw new RuntimeException("Failed parsing 'module.json' file: " + e.getMessage());
        }
    }

    @Override
    public void onTerminate() {

        Map<IxiModule, String> pathesByModule = new HashMap<>();
        for(Map.Entry<String, IxiModule> entry : modulesByPath.entrySet()) {
            pathesByModule.put(entry.getValue(), entry.getKey());
        }

        for(IxiModule module : modulesWithInfo.keySet()) {
            IxiContext context = module.getContext();
            JSONObject configuration = context.getConfiguration();
            String path = pathesByModule.get(module);
            if(configuration != null)
                try {
                    IOHelper.writeToFile(new File(MODULE_DIRECTORY, path+".cfg"), configuration.toString());
                } catch (IOException e) {
                    LOGGER.warn("Failed to store configuration of module '"+path+"'.", e);
                }
        }
    }

    public Set<IxiModule> getModules() {
        return new HashSet<>(modulesWithInfo.keySet());
    }

    public IxiModule getModule(String path) {
        return modulesByPath.get(path);
    }

    public IxiModuleInfo getInfo(IxiModule module) {
        return modulesWithInfo.get(module);
    }
}