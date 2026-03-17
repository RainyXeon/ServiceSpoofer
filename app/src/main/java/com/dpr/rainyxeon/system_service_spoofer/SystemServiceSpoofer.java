package com.dpr.rainyxeon.system_service_spoofer;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.List;
import java.util.ArrayList;

public class SystemServiceSpoofer implements IXposedHookLoadPackage {

    private static final String TAG = "[ServiceSpoofer] ";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedBridge.log(TAG + "Loaded package: " + lpparam.packageName);

        // Only hook system_server (android)
        if (!"android".equals(lpparam.packageName)) return;

        try {
            XposedBridge.log(TAG + "Starting hooks...");

            hookListServices(lpparam.classLoader);
            hookGetService(lpparam.classLoader);

        } catch (Throwable t) {
            XposedBridge.log(TAG + "ERROR during hook:");
            XposedBridge.log(t);
        }
    }

    private void hookListServices(ClassLoader cl) {
        try {
            Class<?> smClass = XposedHelpers.findClass(
                "android.os.ServiceManager",
                cl
            );

            XposedBridge.log(TAG + "ServiceManager class found: " + smClass);

            XposedBridge.hookAllMethods(smClass, "listServices", new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    XposedBridge.log(TAG + "listServices() called");
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    XposedBridge.log(TAG + "listServices() AFTER");

                    String[] services = (String[]) param.getResult();

                    if (services == null) {
                        XposedBridge.log(TAG + "services == null");
                        return;
                    }

                    XposedBridge.log(TAG + "Original size: " + services.length);

                    List<String> filtered = new ArrayList<>();

                    for (String s : services) {
                        XposedBridge.log(TAG + "Service: " + s);

                        if (s != null && !s.contains("lineage")) {
                            filtered.add(s);
                        } else {
                            XposedBridge.log(TAG + "Filtered OUT: " + s);
                        }
                    }

                    param.setResult(filtered.toArray(new String[0]));

                    XposedBridge.log(TAG + "Filtered size: " + filtered.size());
                }
            });

        } catch (Throwable t) {
            XposedBridge.log(TAG + "Failed to hook listServices:");
            XposedBridge.log(t);
        }
    }

    private void hookGetService(ClassLoader cl) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.os.ServiceManager",
                cl,
                "getService",
                String.class,
                new XC_MethodHook() {

                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        String name = (String) param.args[0];
                        XposedBridge.log(TAG + "getService() called: " + name);

                        // Example: block lineage-related services
                        if (name != null && name.contains("lineage")) {
                            XposedBridge.log(TAG + "Blocking service: " + name);

                            // Return null to simulate "service not found"
                            param.setResult(null);
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        String name = (String) param.args[0];
                        Object result = param.getResult();

                        XposedBridge.log(TAG + "getService() result for [" + name + "]: " + result);
                    }
                }
            );

            XposedBridge.log(TAG + "getService hook installed");

        } catch (Throwable t) {
            XposedBridge.log(TAG + "Failed to hook getService:");
            XposedBridge.log(t);
        }
    }
}
