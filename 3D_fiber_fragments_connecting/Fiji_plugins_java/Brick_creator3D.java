//************************************************
// n 
// Written by Hideo Otsuna (HHMI Janelia inst.)
// Aug 2020
// 
//**************************************************

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.io.*;
import ij.plugin.PlugIn;
import ij.plugin.frame.*; 
import ij.plugin.filter.*;
//import ij.plugin.Macro_Runner.*;
import ij.gui.GenericDialog.*;
import ij.macro.*;
import ij.measure.Calibration;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.zip.GZIPOutputStream;
import java.io.IOException;
import java.io.File;
import java.nio.*;
import java.util.*;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;

public class Brick_creator3D implements PlugInFilter {
	//int wList [] = WindowManager.getIDList();
	
	ImagePlus imp, nimp;
	ImageProcessor ip;
	
	public int setup(String arg, ImagePlus imp)
	{
		IJ.register (Brick_creator3D.class);
		if (IJ.versionLessThan("1.32c")){
			IJ.showMessage("Error", "Please Update ImageJ.");
			return 0;
		}
		
		this.imp = imp;
		if(imp.getType()!=imp.GRAY8 && imp.getType()!=imp.GRAY16 && imp.getType()!=imp.GRAY32){
			IJ.showMessage("Error", "Plugin requires 8-, 16, 32-bit image");
			return 0;
		}
		return DOES_8G+DOES_16+DOES_32;
	}
	
	
	
	int xsize=0; int ysize=0; int zsize=0; int overlap=0;
	int thread_num_=0;
	String Sdirectory="";
	Boolean zsingle = false;
	
	
	public void run(ImageProcessor ip) {
		imp = WindowManager.getCurrentImage();
		
		
		int bdepth = imp.getBitDepth();
		
		xsize=(int)Prefs.get("xsize.int",52);
		ysize=(int)Prefs.get("ysize.int",52);
		zsize=(int)Prefs.get("zsize.int",52);
		overlap=(int)Prefs.get("overlap.int",2);
		zsingle = (Boolean)Prefs.get("zsingle.boolean",false);
		
		thread_num_=(int)Prefs.get("thread_numG.int",4);
		
		GenericDialog gd = new GenericDialog("3Dbrick creation");
		
		gd.addNumericField("X size of brick", xsize, 0);
		gd.addNumericField("Y size of brick", ysize, 0);
		gd.addNumericField("Z size of brick", zsize, 0);
		
		gd.addCheckbox("Zis single brick", zsingle);
		
		gd.addNumericField("overlap_vol", overlap, 0);
		
		gd.addNumericField("CPU number", thread_num_, 0);
		
		gd.showDialog();
		if(gd.wasCanceled()){
			return;
		}
		
		
		xsize = (int)gd.getNextNumber();
		ysize = (int)gd.getNextNumber();
		zsize = (int)gd.getNextNumber();
		
		zsingle = gd.getNextBoolean();
		overlap = (int)gd.getNextNumber();
		
		thread_num_ = (int)gd.getNextNumber();
		
		
		Prefs.set("xsize.int", xsize);
		Prefs.set("ysize.int", ysize);
		Prefs.set("zsize.int", zsize);
		Prefs.set("overlap.int", overlap);
		Prefs.set("zsingle.boolean", zsingle);
		
		if(thread_num_ <= 0) thread_num_ = 1;
		Prefs.set("thread_num.int", thread_num_);
		
		
		DirectoryChooser dirS = new DirectoryChooser("Save directory");
		Sdirectory = dirS.getDirectory();
		
		final int Finthread_num_=thread_num_;
		brickfunction(xsize, ysize,zsize,imp, Finthread_num_,overlap,zsingle);
		
		IJ.log("Done");
		
		imp.unlock();
		imp.show();
		
		imp.updateImage();
		
	} //public void run(String arg) {
	
	public void brickfunction(int Fxsize, int Fysize, int Fzsize, ImagePlus Fimp, int FFinthread_num_, int Foverlap, Boolean zsingleF){
		final ImageStack stack1 = Fimp.getStack();
		
		IJ.log("Fimp; "+String.valueOf(Fimp));
		//	IJ.log("  Dgamma;"+String.valueOf(Dgamma));
		
		int [] info= Fimp.getDimensions();
		final int width = info[0];
		final int height = info[1];
		int nChannels = info[2];
		final int nslice = info[3];
		int nFrames = info[4];
		
		IJ.log("Fxsize; "+Fxsize+"  Fysize; "+Fysize+"  Fzsize; "+Fzsize);
		IJ.log("width; "+width+"  height; "+height+"  nslice; "+nslice);
		
		int xbricknum= (int) Math.ceil(Double.valueOf((double) width/ ((double)Fxsize-(double)Foverlap)));
		int ybricknum=(int) Math.ceil(Double.valueOf( (double ) height/ ((double) Fysize-(double)Foverlap)));
		int zbricknum=1;
		if(zsingleF==false)
		zbricknum=(int) Math.ceil(Double.valueOf( (double) nslice/ ((double) Fzsize-(double)Foverlap)));
		
		
		int totalbrick=xbricknum * ybricknum * zbricknum;
		
		IJ.log("xbricknum; "+xbricknum+"  ybricknum; "+ybricknum+"  zbricknum; "+zbricknum+"  totalbrick; "+totalbrick);
		
		
		final int Ftotalbrick = totalbrick;
		
		String [] brickpositionarray = new String [totalbrick];
		
		int zstart=1, ibricknum=0;
		
		for(int zposi=1; zposi<=zbricknum; zposi++){
			
			//		IJ.log("zstart; "+zstart);
			
			int xstart=0;
			for(int xposi=1; xposi<=xbricknum; xposi++){
				IJ.log("xstart; "+xstart);
				int ystart=0;
				for(int yposi=1; yposi<=ybricknum; yposi++){
					
					brickpositionarray[ibricknum]=xstart+","+ystart+","+zstart;
					
					ibricknum=ibricknum+1;
					
					ystart=ystart+Fysize-(Foverlap/2);
					
				}
				
				xstart=xstart+Fxsize-(Foverlap/2);
			}
			
			if(zposi!=1)
			zstart=zstart+Fzsize-Foverlap;
			else
			zstart=zstart+Fzsize-(Foverlap/2);
			
		}//for(int zposi=1; zposi<=xbricknum; zposi++){
		
		final int FFzsize=Fzsize;//52
		final int FFxsize=Fxsize;
		final int FFysize=Fysize;
		final String SdirectoryF=Sdirectory;
		final int FFoverlap=Foverlap;
		
		final AtomicInteger ai = new AtomicInteger(0);
		final Thread[] threads = newThreadArray();
		
		for (int ithread = 0; ithread < threads.length; ithread++) {
			// Concurrently run in as many threads as CPUs
			threads[ithread] = new Thread() {
				
				{ setPriority(Thread.NORM_PRIORITY); }
				
				public void run() {
					
					
					for(int ii=ai.getAndIncrement(); ii<Ftotalbrick; ii = ai.getAndIncrement()){
						
						IJ.showProgress((double)ii/(double)Ftotalbrick);
						
						//ImageStack brick = new ImageStack (FFxsize,FFysize,FFzsize);
						ImageStack brick = ImageStack.create(FFxsize, FFysize, FFzsize, 8);
						String [] contents = brickpositionarray[ii].split(",");
						
						int startpointx = Integer.parseInt (contents[0]);
						int startpointy = Integer.parseInt (contents[1]);
						int startpointz = Integer.parseInt (contents[2]);
						
						int endx=0, endy=0, endz=0;
						
						if(startpointx!=0){
							endx=startpointx+FFxsize;
						}else{
							endx=startpointx+(FFxsize-(FFoverlap/2));
						}
						
						
						if(startpointy!=0){
							endy=startpointy+FFysize;
						}else{
							endy=startpointy+(FFysize-(FFoverlap/2));
						}
						
						if(zsingleF==false){
							if(startpointz!=1){
								//		startpointz=startpointz-(FFoverlap/2);
								endz=startpointz+FFzsize;
							}else{
								endz=startpointz+(FFzsize-(FFoverlap/2));
							}
						}else//if(zsingleF==false){
						endz=startpointz+FFzsize;
						//	IJ.log("endz; "+endz+"  FFzsize; "+FFzsize+"  FFoverlap; "+FFoverlap+"  startpointz; "+startpointz);
						
						int brickz=1;
						int posisignal=0;
						for(int izscan=startpointz; izscan<endz; izscan++){
							
							if(izscan<=nslice){
								ImageProcessor Fip = stack1.getProcessor(izscan);
								
							
								
								int brickx=0;
								for(int ixscan=startpointx; ixscan<endx; ixscan++){
									
									int bricky=0;
									for(int iyscan=startpointy; iyscan<endy; iyscan++){
										
										if(iyscan<height && ixscan<width){
											int pix= Fip.get(ixscan,iyscan);
											
											if(pix>0){
												ImageProcessor ipbrick = brick.getProcessor(brickz);
												//			IJ.log(ii+"  pix is more than 0; "+ixscan+"  iyscan; "+iyscan+"  izscan; "+izscan+"  brickx; "+brickx+"  bricky; "+bricky);
												ipbrick.set(brickx,bricky,pix);
												posisignal=1;
											}
										}//if(iyscan<height){
										bricky=bricky+1;
									}
									brickx=brickx+1;
								}
								brickz=brickz+1;
							}//if(izscan<=nslice){
						}//for(int izscan=startpointz; izscan<startpointz+FFzsize; izscan++){
						
						//		IJ.log("ii; "+String.valueOf(ii)+"startpointx; "+startpointx+"  startpointy; "+startpointy+"  startpointz; "+startpointz+"  posisignal; "+posisignal);
						
						String addst="";
						if(ii<10000){
							if(ii>999)
							addst="0";
							else{
								if(ii>99)
								addst="00";
								else if(ii>9)
								addst="000";
								else if(ii<10)
								addst="0000";
							}
						}
						
						if(posisignal==1){// if there is a positive signal in the brick, save
							ImagePlus impnew = new ImagePlus ("Brick_"+addst+String.valueOf(ii),brick);
							
							//		IJ.runMacro("setBatchMode(true);");
							//		impnew.show();
							//		IJ.log(ii+" brick");
							FileSaver saver = new FileSaver(impnew);
							saver.saveAsZip(SdirectoryF+addst+ii+"_xs"+startpointx+"_xe"+endx+"_ys"+startpointy+"_ye"+endy+"_zs"+startpointz+"_ze"+endz+".zip");
							//				IJ.run("Nrrd Writer", "compressed nrrd="+SdirectoryF+addst+ii+".nrrd");
							
							impnew.close();
							
						}
						
					}//for(int ii=ai.getAndIncrement(); ii<Ftotalbrick; ii = ai.getAndIncrement()){
			}};
		}//	for (int ithread = 0; ithread < threads.length; ithread++) {
		startAndJoin(threads);
		
		String txtfile=width+"\n"+height+"\n"+nslice+"\n"+xbricknum+"\n"+ybricknum+"\n"+zbricknum+"\n"+totalbrick+"\n"+xsize+"\n"+ysize+"\n"+zsize+"\n"+overlap;
		IJ.saveString(txtfile, SdirectoryF+"xyz.txt");
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




