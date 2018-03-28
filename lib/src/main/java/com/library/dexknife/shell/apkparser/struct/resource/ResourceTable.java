package com.library.dexknife.shell.apkparser.struct.resource;


import com.library.dexknife.shell.apkparser.utils.ResourceLoader;

import java.util.HashMap;
import java.util.Map;

/**
 * @author dongliu
 */
public class ResourceTable {
    private Map<Short, ResourcePackage> packageMap = new HashMap<>();
    private com.library.dexknife.shell.apkparser.struct.StringPool stringPool;

    public static Map<Integer, String> sysStyle = ResourceLoader.loadSystemStyles();

    public void addPackage(ResourcePackage resourcePackage) {
        this.packageMap.put(resourcePackage.getId(), resourcePackage);
    }

    public ResourcePackage getPackage(short id) {
        return this.packageMap.get(id);
    }

    public com.library.dexknife.shell.apkparser.struct.StringPool getStringPool() {
        return stringPool;
    }

    public void setStringPool(com.library.dexknife.shell.apkparser.struct.StringPool stringPool) {
        this.stringPool = stringPool;
    }
}
