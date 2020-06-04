package gz.xinit;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Method;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import dalvik.system.DexClassLoader;

public class XInit {

	public static Application application;
	public static final XInitActivityLifecycleCallbacks xActivityLifecycleCallbacks = new XInitActivityLifecycleCallbacks();
	public static boolean writeSdcardPermission;
	public static String processName;
	public static String xinitDir;
	public static String appDataDir;
	public static XinitLog xinitLog;
	public static DexClassLoader xInitClassLoader;

	public static void onApplicationCreate(Application application) {
		int flags = application.getApplicationInfo().flags;
		if ((flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM) {
			return;
		}
		application.registerActivityLifecycleCallbacks(xActivityLifecycleCallbacks);
		PackageManager pm = application.getPackageManager();
		writeSdcardPermission = (PackageManager.PERMISSION_GRANTED == pm
				.checkPermission("android.permission.WRITE_EXTERNAL_STORAG", application.getPackageName()));
		XInit.application = application;
		XInit.processName = application.getApplicationInfo().processName;
		XInit.appDataDir = XInit.application.getApplicationInfo().dataDir;
		String xinitLogAddress = "/sdcard/x" + application.getPackageName() + "/xinit.log";
		if (!writeSdcardPermission) {
			xinitLogAddress = XInit.appDataDir +"/xinit.log";
		}
		File logf = new File(xinitLogAddress);
		if (logf.exists()) {
			logf.delete();
		}
		XInit.xinitLog = new XinitLog(xinitLogAddress);
		XInit.xinitDir = "/sdcard/x" + application.getPackageName();
		if (writeSdcardPermission) {
			File packageDirFile = new File(XInit.xinitDir);
			if (!packageDirFile.exists()) {
				packageDirFile.mkdirs();
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
				xinitLog.appendText("start init " + gadgetElfFile.getAbsolutePath());
				Runtime.getRuntime().load(gadgetElfFile.getAbsolutePath());
				xinitLog.appendText("init " + gadgetElfFile.getAbsolutePath() + " successful.");
			} catch (Exception e) {
				xinitLog.appendText(e);
			}
		} else {
			xinitLog.appendText("Not found any gadget library.");
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
		xinitLog.appendText("patchDexPath:" + patchDexPath);

		if (!patchDexPath.isEmpty()) {
			ClassLoader applicationClassLoader = application.getClassLoader();
			xInitClassLoader = new DexClassLoader(patchDexPath, appDataDir, null, applicationClassLoader);
			// ¼ÓÔØgz.xinit.Spider
			try {
				Class xinitSpiderClass = xInitClassLoader.loadClass("gz.xinit.Spider");
				Method method = xinitSpiderClass.getDeclaredMethod("start");
				new Thread() {
					public void run() {
						try {
							method.invoke(null);
							xinitLog.appendText("flag_spider_start successfull!");
						} catch (Exception e) {
							xinitLog.appendText(e);
						}
					};
				}.start();
			} catch (ClassNotFoundException e) {
				xinitLog.appendText("ClassNotFoundException>>>gz.xinit.Spider");
			} catch (NoSuchMethodException e) {
				xinitLog.appendText("NoSuchMethodException>>>gz.xinit.Spider with start()");
			} catch (Exception e) {
				xinitLog.appendText(e);
			}
		}
	}

	private static boolean isMainProcess() {
		if (application != null && application.getPackageName().equals(processName)) {
			return true;
		} else if (application == null) {
			xinitLog.appendText("application == null");
		}
		return false;
	}

}
