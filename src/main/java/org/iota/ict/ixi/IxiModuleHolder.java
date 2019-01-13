package org.iota.ict.ixi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iota.ict.Ict;
import org.iota.ict.Main;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.IOHelper;
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

public class IxiModuleHolder {

    public static final Logger LOGGER = LogManager.getLogger(IxiModuleHolder.class);
    protected static final File DEFAULT_MODULE_DIRECTORY = new File("modules/");

    protected final Ict ict;
    protected Map<String, IxiModule> modulesByPath = new HashMap<>();
    protected Map<IxiModule, IxiModuleInfo> modulesWithInfo = new HashMap<>();

    static {
        if(!DEFAULT_MODULE_DIRECTORY.exists())
            DEFAULT_MODULE_DIRECTORY.mkdirs();
    }

    public IxiModuleHolder(Ict ict) {
        this.ict = ict;
    }

    public boolean uninstall(String path) {

        IxiModule module = modulesByPath.get(path);
        LOGGER.info("Uninstalling module " + path + " ...");

        if(module == null)
            throw new RuntimeException("No module '"+path+"' installed.");

        IxiModuleInfo info = modulesWithInfo.get(module);

        modulesWithInfo.remove(module);
        modulesByPath.remove(path);
        module.terminate();

        File jar = new File(DEFAULT_MODULE_DIRECTORY, path);
        File guiDirectory = new File(Constants.WEB_GUI_PATH, "modules/" + info.name+"/");

        if(!jar.exists())
            throw new RuntimeException("Could not find file '"+path+"'.");
        if(jar.isDirectory())
            throw new RuntimeException("Path '"+path+"' is a directory.");
        if(!path.endsWith(".jar"))
            throw new RuntimeException("Path '"+path+"' is a directory.");

        boolean jarDeletedSuccess = deleteRecursively(jar);
        boolean guiDirectoryDeletedSuccess = deleteRecursively(guiDirectory);
        return jarDeletedSuccess && guiDirectoryDeletedSuccess;
    }

    private static boolean deleteRecursively(File file) {
        if(!file.exists())
            return true;
        boolean success = true;
        LOGGER.info("Deleting " + file + " ...");
        try {
            if(file.isDirectory())
                for(String subPath : file.list())
                    success = success && deleteRecursively(new File(file.getPath(), subPath));
            success = success && file.delete();
        } catch (Throwable t) {
            LOGGER.error("Could not delete " + file.getAbsolutePath(), t);
            success = false;
        }
        return success;
    }

    public void install(URL url) throws Throwable {

        String split[] = url.getFile().split("/");
        String fileName = split[split.length-1];
        Path target = Paths.get("./modules/"+fileName);

        LOGGER.info("Downloading " + target.toString() + " ...");
        try (InputStream in = url.openStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        LOGGER.info("Download of " + target.toString() + " complete.");

        IxiModule module = initModule(target);
        new Thread(module).start();
    }

    public void initAllModules() {
        File[] files = DEFAULT_MODULE_DIRECTORY.listFiles();
        try {
            if(files == null)
                throw new NullPointerException("failed listing files in directory " + DEFAULT_MODULE_DIRECTORY.getAbsolutePath());
        } catch (NullPointerException e) { return;  }
        modulesWithInfo = new HashMap<>();
        initModulesFromFiles(files);
    }

    public void initModulesFromFiles(File[] files) {
        for(File file : files) {
            Path jar = Paths.get(file.toURI());
            if(!jar.toString().endsWith(".jar"))
                continue;
            try {
                initModule(jar);
            } catch (Throwable t) {
                LOGGER.error("could not load module " + jar, t);
            }
        }
    }

    public IxiModule initModule(Path jar) throws Exception {
        LOGGER.info("loading IXI module "+jar.getFileName()+"...");
        String path = DEFAULT_MODULE_DIRECTORY.toURI().relativize(jar.toUri()).toString();
        URLClassLoader classLoader = new URLClassLoader (new URL[] {jar.toFile().toURI().toURL()}, Main.class.getClassLoader());
        IxiModuleInfo info = readModuleInfoFromJar(classLoader, path);
        IxiModule module = initModule(classLoader, info.mainClass);
        modulesWithInfo.put(module, info);
        modulesByPath.put(path, module);
        return module;
    }

    private IxiModule initModule(URLClassLoader classLoader, String mainClassName) throws Exception {
        Class ixiClass = getIxiClass(classLoader, mainClassName);
        Constructor<?> c = ixiClass.getConstructor(Ixi.class);
        return (IxiModule) c.newInstance(new IctProxy(ict));
    }

    private static Class getIxiClass(URLClassLoader classLoader, String mainClassName) {
        try {
            return Class.forName(mainClassName, true, classLoader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Invalid IXI module: Could not find class '"+mainClassName+"'.");
        }
    }

    public static IxiModuleInfo readModuleInfoFromJar(URLClassLoader classLoader, String path) {
        try {
            InputStream is = classLoader.getResourceAsStream("module.json");
            if(is == null)
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

    public void start() {
        for(IxiModule module : modulesWithInfo.keySet())
            new Thread(module).start();
    }

    public void terminate() {
        for(IxiModule module: modulesWithInfo.keySet())
            module.terminate();
    }

    public Set<IxiModule> getModules() {
        return new HashSet<>(modulesWithInfo.keySet());
    }

    public IxiModuleInfo getInfo(IxiModule module) {
        return modulesWithInfo.get(module);
    }
}