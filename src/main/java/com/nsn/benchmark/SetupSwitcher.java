package com.nsn.benchmark;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/**
 * @author zhangxu
 */
public class SetupSwitcher {
    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Class clazz = classLoader.loadClass("com.nsn.benchmark." + args[0]);
        clazz.getMethod("main", args.getClass()).invoke(null, (Object) Arrays.copyOfRange(args, 1, args.length));
    }
}
