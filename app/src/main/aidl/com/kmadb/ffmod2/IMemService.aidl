package com.kmadb.ffmod2;
interface IMemService {
    void destroy() = 16777114;
    int getPid(String packageName) = 1;
    byte[] readMem(int pid, long address, int size) = 2;
    int writeMem(int pid, long address, in byte[] data) = 3;
    long findPattern(int pid, String pattern, String mask) = 4;
    String getMapBase(int pid, String libName) = 5;
    int blockDomain(String domain) = 6;
}
