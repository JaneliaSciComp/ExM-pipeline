import ij.*;
import ij.plugin.PlugIn;
import ij.process.*;
import ij.io.*;
import ij.plugin.filter.*;
import java.util.concurrent.atomic.AtomicInteger;
import ij.io.Opener;
import ij.gui.*;
import ij.measure.*;
import ij.io.FileInfo.*;
import ij.io.FileOpener;
import ij.macro.*;
import ij.plugin.Macro_Runner;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;
import java.nio.*;
import java.util.*;
import java.nio.charset.Charset;

import java.util.List;


public class AtmicInteger_8bitconv implements PlugIn {
	String Odirectory,Sdirectory,Odirectory2="",Odirectory3="";
	
	int thread_num_ = (int)Prefs.get("thread_num.int",4);
	double lowgre = (double)Prefs.get("lowgre.double",200);
	double highgre = (double)Prefs.get("highgre.double",2000);
	String exportformat=(String)Prefs.get("exportformat.String","Nrrd_uncompressed");
	
	public void run(String arg) {
		if (IJ.versionLessThan("1.49d")) return;
		
		
		//	System.gc();
		//	String timeStamp1 = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
		
		
		Save_MIP();
		
		//	String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
	}
	
	public void Save_MIP(){
		
		final int interpolationMethod = ImageProcessor.BICUBIC;
		
		String format [] = new String[3];
		format[0]="Nrrd_uncompressed"; format[1]="Compressed_Nrrd"; format[2]="Tif"; 
		
		GenericDialog gd = new GenericDialog("bid depth conversion");
		
		gd.addRadioButtonGroup("export format: ", format, 1, 3, exportformat);
		
		gd.addNumericField("Lowest gray value", lowgre, 0);
		gd.addNumericField("Highest gray value", highgre, 0);
		
		gd.addNumericField("Parallel Threads number", thread_num_, 0);
		
		
		gd.showDialog();
		if (gd.wasCanceled()) return;
		
		exportformat=gd.getNextRadioButton();
		lowgre = (double)gd.getNextNumber();
		highgre = (double)gd.getNextNumber();
		thread_num_ = (int)gd.getNextNumber();
		
		DirectoryChooser dir = new DirectoryChooser("choose 16bit 2D tiff directory");
		Odirectory = dir.getDirectory();
		DirectoryChooser dirS = new DirectoryChooser("Save directory");
		Sdirectory = dirS.getDirectory();
		
		if(Odirectory == null || Sdirectory == null) return;
		
		File OdirectoryFile = new File(Odirectory);
		final File names[] = OdirectoryFile.listFiles(); 
		Arrays.sort(names);
		
		Prefs.set("exportformat.String",exportformat);
		
		double test1 = System.currentTimeMillis();
		IJ.log(String.valueOf(thread_num_)+" CPU  Odirectory; "+Odirectory+"  names.length; "+String.valueOf(names.length)+"   Sdirectory; "+Sdirectory);
		
		
		final String OdirectoryF = Odirectory;
		final String SdirectoryF = Sdirectory;
		final double lowgreF = lowgre;
		final double highgreF = highgre;
		
		final double gappercent = 255/(highgre-lowgre);
		final String exportformatF = exportformat;
		
		IJ.log("exportformatF; "+exportformatF);
		
		if(thread_num_ <= 0) thread_num_ = 1;
		Prefs.set("thread_num.int", thread_num_);
		
		final int Fthread_num_=thread_num_;
		
		//	IJ.log("TifNumSt; "+String.valueOf(TifNumSt));
		final AtomicInteger ai = new AtomicInteger(0);
		final Thread[] threads = newThreadArray();
		
		for (int ithread = 0; ithread < threads.length; ithread++) {
			// Concurrently run in as many threads as CPUs
			threads[ithread] = new Thread() {
				
				{ setPriority(Thread.NORM_PRIORITY); }
				
				public void run() {
					
					for (int i = ai.getAndIncrement(); i < names.length; i = ai.getAndIncrement()) {
						
						
						ImagePlus imp [] = new ImagePlus[1];
						ImageProcessor ip [] = new ImageProcessor[1];
						ImageStack stack []= new ImageStack [1];
						
						imp[0]=null;
						ip[0]=null;
						
						int tifposi = names[i].getName().lastIndexOf(".tif");
						if(tifposi==-1)
						tifposi = names[i].getName().lastIndexOf(".zip");
						//		IJ.log("i; "+String.valueOf(i)+"  Names; "+names[i].getName()+"   tifposi; "+String.valueOf(tifposi));
						if(tifposi>0){
							while(imp[0]==null){
								imp[0] = IJ.openImage(OdirectoryF+names[i].getName());
							}
							ImagePlus imp8 =imp[0];
							
							if(imp[0].getType()==imp[0].GRAY16){
								
								while(ip[0]==null){
									ip[0] = imp[0].getProcessor();//.duplicate()
								}
								
								stack[0]=imp[0].getStack();
								
								//		Calibration cal = imp[0].getCalibration();
								
								imp8 = imp[0].duplicate();
								int [] info= imp[0].getDimensions();
								int WW = info[0];
								int HH = info[1];
								int nCh = info[2];
								int SliceN = info[3];
								int nFrame = info[4];
								ImageStack stackbyte = new ImageStack (WW,HH,nCh);
							
								ImageConverter ic = new ImageConverter(imp8);
								ic.convertToGray8();
								
								ImageProcessor EightIMG = imp8.getProcessor();
								//= new ByteProcessor (WW,HH);
								//	BufferedImage EightIMG = new BufferedImage(WW, HH, BufferedImage.TYPE_BYTE_GRAY);
								int sumpx=HH*WW;
								
								
								if(nCh==1){
									
									ImageProcessor ori16=imp[0].getProcessor();
									
									for(int scanpx=0; scanpx<sumpx; scanpx++){
										double pix0=ori16.get(scanpx);
										
										if(pix0!=0){
											double finalval;
											
											if(pix0<highgreF){
												double pureval = pix0-lowgreF;
												
												if(pureval>0)
												finalval = gappercent*pureval;
												else
												finalval=0;
											}else
											finalval=255;
											
											int c = (int) Math.round(finalval); 
											
											EightIMG.set(scanpx,c);
											
										}
									}
									
								}else{
									for (int i3=1; i3<=nCh; i3++) {
										ImageProcessor ori16=stack[0].getProcessor(i3);
										
										//	ImageStack stackbyte=EightIMG.getStack(i3);
										ImageProcessor ipbyte=stackbyte.getProcessor(i3);
										
										for(int scanpx=0; scanpx<sumpx; scanpx++){
											double pix0=ori16.get(scanpx);
											
											
											if(pix0!=0){
												double finalval;
												
												if(pix0<highgreF){
													double pureval = pix0-lowgreF;
													
													if(pureval>0)
													finalval = gappercent*pureval;
													else
													finalval=0;
												}else
												finalval=255;
												
												int c = (int) Math.round(finalval); 
												
												EightIMG.set(scanpx,c);
												
											}
										}
									}
								}//if(nCh>1){
							}//		if(imp[0].getType()==imp.GRAY16){
							String truename = names[i].getName().substring(0, names[i].getName().lastIndexOf('.'));
							
							//int filenum = Integer.parseInt(truename);
							int filenum;
							
							try {
								filenum = Integer.parseInt(truename);
							} catch (NumberFormatException e) {
								filenum = -1;
							}
							
							
							String ST=truename;
							//	IJ.log("filenum; "+String.valueOf(filenum));
							if(filenum!=-1){
								ST="_";
								if(filenum>999 && filenum<10000)
								ST="_0";
								else if(filenum>99 && filenum<1000)
								ST="_00";
								else if(filenum>9 && filenum<100)
								ST="_000";
								else if(filenum<10)
								ST="_0000";
								
								IJ.log("filenum; "+ST+String.valueOf(filenum));
								
								if(exportformatF.equals("Compressed_Nrrd")){
									imp8.show();
									IJ.run("Nrrd Writer", "compressed nrrd="+SdirectoryF+ST+filenum+".nrrd");
								}
								if(exportformatF.equals("Nrrd_uncompressed")){
									imp8.show();
									IJ.run("Nrrd Writer", "nrrd="+SdirectoryF+ST+filenum+".nrrd");
								}
								
								if(exportformatF.equals("Tif"))
								IJ.saveAs(imp8,"Tiff", SdirectoryF+ST+filenum+".tif");
							}else{
								
								if(exportformatF.equals("Compressed_Nrrd")){
									imp8.show();
									IJ.run("Nrrd Writer", "compressed nrrd="+SdirectoryF+ST+".nrrd");
								}
								if(exportformatF.equals("Nrrd_uncompressed")){
									imp8.show();
									IJ.run("Nrrd Writer", "nrrd="+SdirectoryF+ST+".nrrd");
								}
								
								if(exportformatF.equals("Tif"))
								IJ.saveAs(imp8,"Tiff", SdirectoryF+ST+".tif");
								
							}
							
							//new FileSaver(imp[0]).saveAsTiff(SdirectoryF+ST+String.valueOf(i)+".tif"); 
							
							imp8.close();
							//				imp[0].flush();
							imp[0].close();
						}
						
						//			System.gc();
					}//	for (int i = ai.getAndIncrement(); i < names.length; i = ai.getAndIncrement()) {
			}};//threads[ithread] = new Thread() {
		}//	for (int ithread = 0; ithread < threads.length; ithread++) {
		startAndJoin(threads);
		
		double test = System.currentTimeMillis();
		double gap = (test - test1)/1000;
		
		IJ.log(" processing ; "+String.valueOf(gap)+" sec");
	}//	public void Save_Pyramid(){
	
	
	/** Create a Thread[] array as large as the number of processors available.
		* From Stephan Preibisch's Multithreading.java class. See:
		* http://repo.or.cz/w/trakem2.git?a=blob;f=mpi/fruitfly/general/MultiThreading.java;hb=HEAD
		*/
	private Thread[] newThreadArray() {
		int n_cpus = Runtime.getRuntime().availableProcessors();
		if (n_cpus > thread_num_) n_cpus = thread_num_;
		if (n_cpus <= 0) n_cpus = 1;
		return new Thread[n_cpus];
	}
	
	/** Start all given threads and wait on each of them until all are done.
		* From Stephan Preibisch's Multithreading.java class. See:
		* http://repo.or.cz/w/trakem2.git?a=blob;f=mpi/fruitfly/general/MultiThreading.java;hb=HEAD
		*/
	public static void startAndJoin(Thread[] threads)
	{
		for (int ithread = 0; ithread < threads.length; ++ithread)
		{
			threads[ithread].setPriority(Thread.NORM_PRIORITY);
			threads[ithread].start();
		}
		
		try
		{   
			for (int ithread = 0; ithread < threads.length; ++ithread)
			threads[ithread].join();
		} catch (InterruptedException ie)
		{
			throw new RuntimeException(ie);
		}
	}
	
}



