package dalvik.system;

import android.system.ErrnoException;
import gz.xinit.XInit;
import gz.xinit.XinitLog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import libcore.io.Libcore;

public final class DexFile {
    public static final byte DEXOPT_NEEDED = 2;
    public static final byte PATCHOAT_NEEDED = 1;
    public static final byte UP_TO_DATE = 0;
    private final CloseGuard guard;
    /* access modifiers changed from: private */
    public long mCookie;
    private final String mFileName;

    private class DFEnum implements Enumeration<String> {
        private int mIndex = 0;
        private String[] mNameList;

        DFEnum(DexFile dexFile) {
            this.mNameList = DexFile.getClassNameList(DexFile.this.mCookie);
        }

        public boolean hasMoreElements() {
            return this.mIndex < this.mNameList.length;
        }

        public String nextElement() {
            String[] strArr = this.mNameList;
            int i = this.mIndex;
            this.mIndex = i + 1;
            return strArr[i];
        }
    }

    public DexFile(File file) throws IOException {
        this(file.getPath());
    }

    public DexFile(String str) throws IOException {
        this.guard = CloseGuard.get();
        this.mCookie = openDexFile(str, null, 0);
        this.mFileName = str;
        this.guard.open("close");
    }

    private DexFile(String str, String str2, int i) throws IOException {
        this.guard = CloseGuard.get();
        if (str2 != null) {
            try {
                String parent = new File(str2).getParent();
                if (Libcore.os.getuid() != Libcore.os.stat(parent).st_uid) {
                    throw new IllegalArgumentException("Optimized data directory " + parent + " is not owned by the current user. Shared storage cannot protect" + " your application from code injection attacks.");
                }
            } catch (ErrnoException e) {
            }
        }
        this.mCookie = openDexFile(str, str2, i);
        this.mFileName = str;
        this.guard.open("close");
    }

    private static native void closeDexFile(long j);

    private static Class defineClass(String str, ClassLoader classLoader, long j, List<Throwable> list) {
        try {
            return defineClassNative(str, classLoader, j);
        } catch (NoClassDefFoundError e) {
            if (list == null) {
                return null;
            }
            list.add(e);
            return null;
        } catch (ClassNotFoundException e2) {
            if (list == null) {
                return null;
            }
            list.add(e2);
            return null;
        }
    }

    private static native Class defineClassNative(String str, ClassLoader classLoader, long j) throws ClassNotFoundException, NoClassDefFoundError;

    /* access modifiers changed from: private */
    public static native String[] getClassNameList(long j);

    public static native boolean isDexOptNeeded(String str) throws FileNotFoundException, IOException;

    public static native byte isDexOptNeededInternal(String str, String str2, String str3, boolean z) throws FileNotFoundException, IOException;

    public static DexFile loadDex(String str, String str2, int i) throws IOException {
        return new DexFile(str, str2, i);
    }

    private static long openDexFile(String str, String str2, int i) throws IOException {
    	String arg1 = new File(str).getAbsolutePath();
    	String arg2 = str2 == null ? null : new File(str2).getAbsolutePath();
    	if (XInit.xinitLog != null) {
    		XInit.xinitLog.appendText("openDexFileNative---->1:"+arg1+" 2:"+arg2+" 3:"+i+"\n"+XinitLog.getException(new Exception()));
    	}
        return openDexFileNative(arg1, arg2, i);
    }

    private static native long openDexFileNative(String str, String str2, int i);

    public void close() throws IOException {
        if (this.mCookie != 0) {
            this.guard.close();
            closeDexFile(this.mCookie);
            this.mCookie = 0;
        }
    }

    public Enumeration<String> entries() {
        return new DFEnum(this);
    }

    /* access modifiers changed from: protected */
    public void finalize() throws Throwable {
        try {
            if (this.guard != null) {
                this.guard.warnIfOpen();
            }
            close();
        } finally {
            super.finalize();
        }
    }

    public String getName() {
        return this.mFileName;
    }

    /* JADX DEBUG: Failed to find minimal casts for resolve overloaded methods, cast all args instead
     method: java.lang.String.replace(char, char):java.lang.String
     arg types: [int, int]
     candidates:
      java.lang.String.replace(java.lang.CharSequence, java.lang.CharSequence):java.lang.String
      java.lang.String.replace(char, char):java.lang.String */
    public Class loadClass(String str, ClassLoader classLoader) {
        return loadClassBinaryName(str.replace('.', '/'), classLoader, null);
    }

    public Class loadClassBinaryName(String str, ClassLoader classLoader, List<Throwable> list) {
        return defineClass(str, classLoader, this.mCookie, list);
    }

    public String toString() {
        return getName();
    }
}