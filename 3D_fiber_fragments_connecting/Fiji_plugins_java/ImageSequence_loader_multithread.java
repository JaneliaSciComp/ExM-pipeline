//************************************************
// 
// Written by Hideo Otsuna (HHMI Janelia inst.)
// Oct 2019
// 
//**************************************************

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.io.DirectoryChooser;
import ij.io.FileSaver;
import ij.plugin.PlugIn;
import ij.plugin.frame.*; 
import ij.plugin.filter.*;
//import ij.plugin.Macro_Runner.*;
import ij.gui.GenericDialog.*;
import ij.macro.*;
import ij.measure.Calibration;
import ij.plugin.CanvasResizer;
import ij.plugin.Resizer;
import ij.util.Tools;
import ij.io.FileInfo;
import ij.io.TiffEncoder;

import java.io.IOException;
import java.io.File;
import java.nio.*;
import java.util.*;
import java.util.Iterator;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;


public class ImageSequence_loader_multithread implements PlugIn {
	
	private static boolean averageWhenDownsizing = true;
	int thread_num_=0, startslice=0,openslice=0;
	
	
	public void run(String arg) {
		
		
		thread_num_=(int)Prefs.get("thread_num_.int",11);
		startslice=(int)Prefs.get("startslice.int",1);
		openslice=(int)Prefs.get("openslice.int",400);
		
		
		GenericDialog gd = new GenericDialog("3D volume building from block");
		
		gd.addNumericField("Start slice number", startslice, 0);
		
		gd.addNumericField("Open slice number", openslice, 0);
		
		gd.addNumericField("CPU number", thread_num_, 0);
		
		gd.showDialog();
		if(gd.wasCanceled()){
			return;
		}
		
		startslice= (int)gd.getNextNumber();
		openslice= (int)gd.getNextNumber();
		thread_num_ = (int)gd.getNextNumber();
		
		if(thread_num_ <= 0) thread_num_ = 1;
		Prefs.set("thread_num_.int", thread_num_);
		Prefs.set("startslice.int", startslice);
		Prefs.set("openslice.int", openslice);
		
		
		DirectoryChooser dirO = new DirectoryChooser("serial tiff directory");
		String Odirectory = dirO.getDirectory();
		
		
		IJ.log("Odirectory; "+Odirectory);
		
		File OdirectoryFile = new File(Odirectory);
		final File names[] = OdirectoryFile.listFiles(); 
		Arrays.sort(names);
		
		IJ.log("startslice;"+startslice+"  openslice; "+openslice);
		
		
		long timestart = System.currentTimeMillis();

		MIPfunction(Odirectory,names,thread_num_,startslice,openslice);
		
		long timeend = System.currentTimeMillis();
		
		long gapS = (timeend-timestart)/1000;
		
		IJ.log("Done "+gapS+" second");
		
	} //public void run(String arg) {
	
	public void MIPfunction (final String FOdirectory, File names[],final int thread_num_F, final int startsliceF, final int opensliceF){
		
		final AtomicInteger ai = new AtomicInteger(startsliceF);
		final Thread[] threads = newThreadArray();
		ImagePlus imptest=null;
		
		int tifpositest = names[5].getName().lastIndexOf("tif");
		if(tifpositest==-1)
		tifpositest = names[5].getName().lastIndexOf("zip");
		IJ.log("FOdirectory+names[5].getName();  "+FOdirectory+names[5].getName());
		
		if(tifpositest>0)
		imptest = IJ.openImage(FOdirectory+names[5].getName());
		else{
			tifpositest = names[10].getName().lastIndexOf("tif");
			if(tifpositest==-1)
			tifpositest = names[10].getName().lastIndexOf("zip");
			if(tifpositest==-1)
			tifpositest = names[10].getName().lastIndexOf("png");
			
			if(tifpositest>0)
			imptest = IJ.openImage(FOdirectory+names[10].getName());
		}
		if(imptest==null){
			IJ.log("please eliminate other than tif files!");
			return;
		}
		
		int [] infotest= imptest.getDimensions();
		final int width = infotest[0];
		final int height = infotest[1];
		
		int handlingslice=opensliceF;
		if(opensliceF+startsliceF>names.length){
			handlingslice=names.length-startsliceF;
		}
		
		IJ.log("handlingslice; "+handlingslice);
		
		final int handlingsliceF=handlingslice;
		final ImageStack bigstack =  IJ.createImage("bigMIP", width,height,handlingslice, 8).getStack();
		
	//	IJ.log("thread_num_F; "+thread_num_F+"  FOdirectory; "+FOdirectory);
		
		imptest.flush();
		imptest.close();
		
		for (int ithread = 0; ithread < threads.length; ithread++) {
			// Concurrently run in as many threads as CPUs
			threads[ithread] = new Thread() {
				
				{ setPriority(Thread.NORM_PRIORITY); }
				
				public void run() {
					
					for(int iMIP=ai.getAndIncrement(); iMIP<handlingsliceF+startsliceF; iMIP = ai.getAndIncrement()){
						
						ImageProcessor ipbig = bigstack.getProcessor(iMIP-startsliceF+1);
						
						//IJ.showProgress((double)iMIP/(double) names.length);
						IJ.showStatus(String.valueOf(iMIP));
						
						ImagePlus imp =null;// new ImagePlus();
						ImageProcessor ip=null;
						
						int tifposi = names[iMIP-1].getName().lastIndexOf("tif");
						if(tifposi==-1)
						tifposi = names[iMIP-1].getName().lastIndexOf("zip");
						if(tifposi==-1)
						tifposi = names[iMIP-1].getName().lastIndexOf("png");
						
						if(tifposi>0){
							while(imp==null){
								imp = IJ.openImage(FOdirectory+names[iMIP-1].getName());
							}
							
							while(ip==null){
								ip = imp.getProcessor();//.duplicate()
							}
							
							//		int [] info= imp.getDimensions();
							//		final int width = info[0];//52
							//		final int height = info[1];//52
							
							int sumpx= width*height;
							
							//		IJ.log("width; "+width+"  height; "+height);
							
							for(int ixypix=0; ixypix<sumpx; ixypix++){// big MIPcreation per a thread
								
								int pix0=ip.get(ixypix);
								
								if(pix0>0){
									int pixbig=ipbig.get(ixypix);
									
									if(pixbig<pix0)
									ipbig.set(ixypix,pix0);
									
								}
							}//for(int ixypix=0; ixypix<=sumpx; ixypix++){// big MIPcreation per a thread
							
							
							imp.flush();
							imp.close();
						}//	if(tifposi>0){
						
					}//for(int ii=ai.getAndIncrement(); ii<FXYtotalbrick; ii = ai.getAndIncrement()){
			}};
		}//	for (int ithread = 0; ithread < threads.length; ithread++) {
		startAndJoin(threads);
		
		String FOdirectorySub = FOdirectory.substring(0,FOdirectory.length()-1);
		int dirindex=FOdirectorySub.lastIndexOf("/");
		if(dirindex==-1)
		dirindex=FOdirectorySub.lastIndexOf("\\");
		String MIPname = FOdirectory.substring(dirindex+1,FOdirectorySub.length());
		
		ImagePlus impbigstack = new ImagePlus (MIPname,bigstack);
		impbigstack.show();
		
	}
	
	private Thread[] newThreadArray() {
		int n_cpus = Runtime.getRuntime().availableProcessors();
		if (n_cpus > thread_num_) n_cpus = thread_num_;
		if (n_cpus <= 0) n_cpus = 1;
		return new Thread[n_cpus];
	}
	
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




