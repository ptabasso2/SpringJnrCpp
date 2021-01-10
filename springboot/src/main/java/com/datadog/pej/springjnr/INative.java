package com.datadog.pej.springjnr;

/**
 * interface for native language
 */
public interface INative {
        // function name, arguments and return value
        // should be same with those of native module.
        int sum(int m, int n);
        int runcppkv(int m, int n, String[] keys, String[] values);
}

