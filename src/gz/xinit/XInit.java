package gz.xinit;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Application;

public class XInit {
	
	public static boolean hasPatchedDexPath = false;
	
	
	public static void onCreateInit(Application application) {
		String logFile = "/data/local/tmp/" + application.getPackageName() + "/xinit.log";
		File logf = new File(logFile);
		if (logf.exists()) {
			logf.delete();
		}
		String packageDir = "/data/local/tmp/" + application.getPackageName();
		try {
			File packageDirFile = new File(packageDir);
			if (!packageDirFile.exists()) {
				packageDirFile.mkdirs();
			}
			// 扫描/data/local/tmp/$package目录frida gadget
			File gadgetElfFile = new File(packageDir + "/libgadget.so");
			if (gadgetElfFile.exists()) {
				try {
					Runtime.getRuntime().load(gadgetElfFile.getAbsolutePath());
					appendText(logFile, "init " + gadgetElfFile.getAbsolutePath() + " successful.");
				} catch (Exception e) {
					appendText(logFile, "flag_failure "+getException(e));
				}
			} else {
				appendText(logFile, "Not found any gadget library.");
			}
		} catch (Exception e) {
			appendText(logFile, getException(e));
		}
		//加载gz.xinit.Spider
		appendText(logFile, "init patchDexs "+hasPatchedDexPath);
		try {
			Class xinitSpiderClass = application.getClassLoader().loadClass("gz.xinit.Spider");
			Method method = xinitSpiderClass.getDeclaredMethod("start");
	        method.invoke(null);
	        appendText(logFile, "flag_spider_started");
		} catch (ClassNotFoundException e) {
			appendText(logFile, getException(e));
		} catch (Exception e) {
			e.printStackTrace();
			appendText(logFile, getException(e));
		}
	}
	
	public static void appendText(String fileName, String content) {
    	FileWriter writer = null;
        try {
            // 打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件
            writer = new FileWriter(fileName, true);
            writer.write("\n"+content);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
        	if (writer != null) {
        		try {
					writer.close();
				} catch (IOException e) {
				}
        	}
        }
    }

	public static String patchDexPath(String dexPath) {
		String patchDexPath = dexPath;
		synchronized (XInit.class) {
			//每個進程都有多個dexload所以，判斷一個主要的dexload加載補丁即可
			Matcher matcher = Pattern.compile("/data/app/([^\\-]+)\\-\\d/base\\.apk").matcher(dexPath);
			if (dexPath != null && !hasPatchedDexPath && matcher.find()) {
				String packageName = matcher.group(1);
				File patchDir = new File("/data/local/tmp/" + packageName);
				if (patchDir.isDirectory()) {
					File[] patchDexes = patchDir.listFiles(new FileFilter() {
						@Override
						public boolean accept(File pathname) {
							return pathname.getName().endsWith(".dex");
						}
					});
					for (int i = 0;patchDexes != null && i < patchDexes.length; i++) {
						patchDexPath += ":" + patchDexes[i].getAbsolutePath();
					}
					hasPatchedDexPath = true;
				}
			}
		}
		return patchDexPath;
	}
	

    /**
     * 返回当前的进程名
     */
    public static String getCurrentProcessName()throws Exception {
        //1. 通过ActivityThread中的currentActivityThread()方法得到ActivityThread的实例对象
        Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
        Method method = activityThreadClass.getDeclaredMethod("currentActivityThread", activityThreadClass);
        Object activityThread = method.invoke(null);
        //2. 通过activityThread的getProcessName() 方法获取进程名
        Method getProcessNameMethod = activityThreadClass.getDeclaredMethod("getProcessName", activityThreadClass);
        Object processName = getProcessNameMethod.invoke(activityThread);
        return processName.toString();
    }
    
    
    private static String getException(Exception e) {
    	Writer writer = null;
    	PrintWriter printWriter = null;
    	try {
    		writer = new StringWriter();
    		printWriter = new PrintWriter(writer);
    		e.printStackTrace(printWriter);
    		return writer.toString();
    	} finally {
    		try {
    			if (writer != null)
    				writer.close();
    			if (printWriter != null)
    				printWriter.close();
    		} catch (IOException e1) { }
    	}
    }
}
