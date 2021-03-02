
import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
//import ij.plugin.*;
import ij.plugin.PlugIn;
import ij.plugin.frame.*; 
//import ij.plugin.Macro_Runner.*;
import ij.gui.GenericDialog.*;
import ij.macro.*;
//import java.awt.event.ActionListener; 
//import java.util.concurrent.atomic.AtomicInteger;

public class CPU_NUM implements PlugIn{
	
	static String result_ = new String();
	int n_cpus = Runtime.getRuntime().availableProcessors();

	String ip1;
	
	public int setup(String arg)
	{
		IJ.register (CPU_NUM.class);
		if (IJ.versionLessThan("1.32c")){
			IJ.showMessage("Error", "Please Update ImageJ.");
			return 0;
		}
		return 1;
	}
	
	public void run(String ip){
		result_ = String.valueOf(n_cpus);
	}
	
	public static String getResult() { 
		return result_; 
	}
}