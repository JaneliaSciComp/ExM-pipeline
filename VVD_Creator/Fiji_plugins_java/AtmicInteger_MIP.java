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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;
import java.nio.*;
import java.util.*;
import java.nio.charset.Charset;

import java.util.List;


public class AtmicInteger_MIP implements PlugIn {
	String Odirectory,Sdirectory,Odirectory2="",Odirectory3="";
	
	int thread_num_ = (int)Prefs.get("thread_num.int",4);
	int MIPnum = (int)Prefs.get("MIPnum.int",3);
	Boolean deleteOri = (Boolean)Prefs.get("deleteOri.boolean",false);
	
	public void run(String arg) {
		if (IJ.versionLessThan("1.49d")) return;
		
		
		System.gc();
		Save_MIP();
		IJ.log("MIP done "+String.valueOf(MIPnum)+"slices");
	}
	
	public void Save_MIP(){
		
		final int interpolationMethod = ImageProcessor.BICUBIC;
	
		GenericDialog gd = new GenericDialog("Pyramid creation");
		gd.addNumericField("Slice number of the MIP", MIPnum, 0);
		gd.addNumericField("Parallel Threads number", thread_num_, 0);
		gd.addCheckbox("Delete original files", deleteOri);
		gd.showDialog();
		if (gd.wasCanceled()) return;
		
		MIPnum = (int)gd.getNextNumber();
		thread_num_ = (int)gd.getNextNumber();
		deleteOri=gd.getNextBoolean();//boolean Returns the state of the next checkbox.
		
		
		DirectoryChooser dir = new DirectoryChooser("choose 2D tiff directory");
		Odirectory = dir.getDirectory();
		DirectoryChooser dirS = new DirectoryChooser("Save directory");
		Sdirectory = dirS.getDirectory();
		
		if(Odirectory == null || Sdirectory == null) return;
		
		File OdirectoryFile = new File(Odirectory);
		final File names[] = OdirectoryFile.listFiles(); 
		Arrays.sort(names);
		
		IJ.log("Odirectory; "+Odirectory+"  names.length; "+String.valueOf(names.length)+"   Sdirectory; "+Sdirectory);
		
		final Boolean deleteOriF=deleteOri;
		
		final String OdirectoryF = Odirectory;
		final String SdirectoryF = Sdirectory;
		
		Prefs.set("deleteOri.boolean",deleteOri);
		Prefs.set("MIPnum.int", MIPnum);
		if(thread_num_ <= 0) thread_num_ = 1;
		Prefs.set("thread_num.int", thread_num_);
		
		final int Fthread_num_=thread_num_;
		final int FMIPnum = MIPnum;
		
		int TifNumSt=0;
		for(int iopen=0; iopen<names.length; iopen++){
			String nameST=names[iopen].getName();
			int tifIndex=nameST.lastIndexOf(".tif");
			
			if(tifIndex!=-1 && TifNumSt==0 && iopen!=0)
			TifNumSt=iopen;
		}
	//	IJ.log("TifNumSt; "+String.valueOf(TifNumSt));
		final AtomicInteger ai = new AtomicInteger(0);
		final Thread[] threads = newThreadArray();
		
		for (int ithread = 0; ithread < threads.length; ithread++) {
			// Concurrently run in as many threads as CPUs
			threads[ithread] = new Thread() {
				
				{ setPriority(Thread.NORM_PRIORITY); }
				
				public void run() {
					int ENDsli=0;
					for (int i = ai.getAndAdd(FMIPnum); i < names.length; i = ai.getAndAdd(FMIPnum)) {
						
						
						ImagePlus imp [] = new ImagePlus[FMIPnum];
						ImageProcessor ip [] = new ImageProcessor[FMIPnum];
						ImageStack stack []= new ImageStack [FMIPnum];
						
						int ImageNum=0;
						ENDsli=i+FMIPnum;
						
						if(ENDsli>names.length)
						ENDsli=names.length;
						
						
						for(int iopen=i; iopen<ENDsli; iopen++){
							imp[ImageNum] = IJ.openImage(OdirectoryF+names[iopen].getName());
							ip[ImageNum] = imp[ImageNum].getProcessor();//.duplicate()
							stack[ImageNum] = imp[ImageNum].getStack();
							ImageNum=ImageNum+1;
						}
						//		Calibration cal = imp[0].getCalibration();
		//				IJ.log("i; "+String.valueOf(i)+"  Names; "+names[i].getName()+"  ImageNum; "+String.valueOf(ImageNum));
						int [] info= imp[0].getDimensions();
						int WW = info[0];
						int HH = info[1];
						int nCh = info[2];
						int SliceN = info[3];
						int nFrame = info[4];
						
						int sumpx=HH*WW;
						
						//	double xPxSize = cal.pixelWidth;
						//	double yPxSize = cal.pixelHeight;
						//	double zPxSize = cal.pixelDepth;
						
						if(ImageNum>1){
							for(int iMIP=1; iMIP<ImageNum; iMIP++){// 0 is ip[0]
								for (int i3=1; i3<=nCh; i3++) {
									ImageProcessor MIP=stack[0].getProcessor(i3);
									ImageProcessor ipSlice=stack[iMIP].getProcessor(i3);
									
									for(int scanpx=0; scanpx<sumpx; scanpx++){
										int pix0=MIP.get(scanpx);
										int pix1=ipSlice.get(scanpx);
										
										if(pix1>pix0)
										MIP.set(scanpx,pix1);
									}
								}
							}
						}//f(imp.length>1){
						
						String ST="_";
						if(i>999 && i<10000)
						ST="_0";
						else if(i>99 && i<1000)
						ST="_00";
						else if(i>9 && i<100)
						ST="_000";
						else if(i<10)
						ST="_0000";
						
					
						//new FileSaver(imp[0]).saveAsTiff(SdirectoryF+ST+String.valueOf(i)+".tif"); 
					
						if(deleteOriF==true){
							for(int idelete=i; idelete<ENDsli; idelete++){
								File fileD = new File(OdirectoryF+names[idelete].getName());
								fileD.delete();
							}
						}
						
						IJ.saveAs(imp[0],"Tiff", SdirectoryF+ST+i+".tif");
						
						for(int iimp=0; iimp<ImageNum; iimp++){
							imp[iimp].flush();
							imp[iimp].close();							
						}
						
					
						
						//			System.gc();
					}//	for (int i = ai.getAndIncrement(); i < names.length; i = ai.getAndIncrement()) {
			}};//threads[ithread] = new Thread() {
		}//	for (int ithread = 0; ithread < threads.length; ithread++) {
		startAndJoin(threads);
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



