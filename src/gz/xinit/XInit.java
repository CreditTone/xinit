package gz.xinit;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Method;

import android.app.Application;
import android.content.pm.PackageManager;
import dalvik.system.DexClassLoader;

public class XInit {

	public static Application application;
	public static final XInitActivityLifecycleCallbacks xActivityLifecycleCallbacks = new XInitActivityLifecycleCallbacks();
	public static boolean writeSdcardPermission;
	public static String processName;
	public static String xinitDir;
	public static String xinitLogFile;
	public static DexClassLoader xInitClassLoader;

	public static void onApplicationCreate(Application application) {
		application.registerActivityLifecycleCallbacks(xActivityLifecycleCallbacks);
		PackageManager pm = application.getPackageManager();
		writeSdcardPermission = (PackageManager.PERMISSION_GRANTED == pm
				.checkPermission("android.permission.WRITE_EXTERNAL_STORAG", application.getPackageName()));
		XInit.application = application;
		XInit.processName = application.getApplicationInfo().processName;
		XInit.xinitDir = "/sdcard/x" + application.getPackageName();
		XInit.xinitLogFile = "/sdcard/x" + application.getPackageName() + "/xinit.log";
		XinitLog.XINIT_LOG_FILE = XInit.xinitLogFile;
		if (writeSdcardPermission) {
			File packageDirFile = new File(XInit.xinitDir);
			if (!packageDirFile.exists()) {
				packageDirFile.mkdirs();
			}
			File logf = new File(XInit.xinitLogFile);
			if (logf.exists()) {
				logf.delete();
			}
		}
		loadFridaGadget();
		loadXinitDexes();
	}

	private static void loadFridaGadget() {
		if (application == null) {
			return;
		}
		// É¨Ãè/data/local/tmp/$packageÄ¿Â¼frida gadget
		File gadgetElfFile = new File(xinitDir + "/libgadget.so");
		if (gadgetElfFile.exists()) {
			try {
				XinitLog.appendText("start init " + gadgetElfFile.getAbsolutePath());
				Runtime.getRuntime().load(gadgetElfFile.getAbsolutePath());
				XinitLog.appendText("init " + gadgetElfFile.getAbsolutePath() + " successful.");
			} catch (Exception e) {
				XinitLog.appendText(e);
			}
		} else {
			XinitLog.appendText("Not found any gadget library.");
		}
	}

	private static void loadXinitDexes() {
		if (!isMainProcess()) {
			return;
		}
		File patchDir = new File(xinitDir);
		String patchDexPath = "";
		if (patchDir.isDirectory()) {
			File[] patchDexes = patchDir.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					return pathname.getName().endsWith(".dex") || pathname.getName().endsWith(".apk");
				}
			});
			if (patchDexes.length > 0) {
				for (int i = 0; patchDexes != null && i < patchDexes.length; i++) {
					patchDexPath += ":" + patchDexes[i].getAbsolutePath();
				}
			}
		}
		XinitLog.appendText("patchDexPath:"+patchDexPath);
		
		if (!patchDexPath.isEmpty()) {
			ClassLoader applicationClassLoader = application.getClassLoader();
			xInitClassLoader = new DexClassLoader(patchDexPath, XInit.application.getApplicationInfo().dataDir, null, applicationClassLoader);
			// ¼ÓÔØgz.xinit.Spider
			try {
				Class xinitSpiderClass = xInitClassLoader.loadClass("gz.xinit.Spider");
				Method method = xinitSpiderClass.getDeclaredMethod("start");
				new Thread() {
					public void run() {
						try {
							method.invoke(null);
							XinitLog.appendText("flag_spider_start successfull!");
						} catch (Exception e) {
							XinitLog.appendText(e);
						}
					};
				}.start();
			} catch (ClassNotFoundException e) {
				XinitLog.appendText("ClassNotFoundException>>>gz.xinit.Spider");
			} catch (NoSuchMethodException e) {
				XinitLog.appendText("NoSuchMethodException>>>gz.xinit.Spider with start()");
			} catch (Exception e) {
				XinitLog.appendText(e);
			}
		}
	}

	private static boolean isMainProcess() {
		if (application != null && application.getPackageName().equals(processName)) {
			return true;
		} else if (application == null) {
			XinitLog.appendText("application == null");
		}
		return false;
	}

}
