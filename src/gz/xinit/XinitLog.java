package gz.xinit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class XinitLog {

	private final String xinit_log_file;
	
    public XinitLog(String xinit_log_file) {
		this.xinit_log_file = xinit_log_file;
	}

	public void appendText(String content) {
    	if (xinit_log_file.startsWith("/sdcard/") && !XInit.writeSdcardPermission) {
    		return;
    	}
        FileWriter writer = null;
        try {
            File parentFile = new File(xinit_log_file).getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdir();
            }
            String datetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            content = android.os.Process.myPid() + "_" + datetime  +">>>" + content + "\n";
            writer = new FileWriter(xinit_log_file, true);
            writer.write(content);
        } catch (IOException e) {
            System.out.println("e = " + e);
        }finally {
            if (writer != null) {
                try {
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public void appendText(Exception ex) {
        appendText(getException(ex));
    }

    public static String getException(Exception e) {
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
