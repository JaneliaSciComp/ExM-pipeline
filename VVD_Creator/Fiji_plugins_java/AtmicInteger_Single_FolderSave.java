import ij.*;
import ij.plugin.PlugIn;
import ij.process.*;
import ij.io.*;
import ij.plugin.filter.*;
import java.util.concurrent.atomic.AtomicInteger;
import ij.io.*;
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


public class AtmicInteger_Single_FolderSave implements PlugIn {
	String Odirectory,Sdirectory;
	
	int thread_num_ = (int)Prefs.get("thread_num.int",4);
	
	public void run(String arg) {
		if (IJ.versionLessThan("1.49d")) return;
		
		
		System.gc();
		Save_Pyramid();
		IJ.log("Save done");
	}
	
	public void Save_Pyramid(){
		DirectoryChooser dir=null,dir2=null;
		final int interpolationMethod = ImageProcessor.BICUBIC;
		
		double XYendratio=Prefs.get("XYendratio.double",20);
		double ResizingSteps=Prefs.get("ResizingSteps.double",4);
		int CHnum=(int)Prefs.get("CHnum.int",1);
		
		GenericDialog gd = new GenericDialog("Pyramid creation");
		int G=0,B=0;
		
		//gd.setInsets(20, 300, 0);
		gd.addNumericField("Final smallest %",  XYendratio, 0);
		gd.addNumericField("Pyramid steps",  ResizingSteps, 0);
		gd.addNumericField("Parallel Threads number", thread_num_, 0);
		gd.addNumericField("Channel Number", CHnum, 0);
		
		gd.showDialog();
		if (gd.wasCanceled()) return;
		
		XYendratio = (double)gd.getNextNumber();
		ResizingSteps = (double)gd.getNextNumber();
		thread_num_ = (int)gd.getNextNumber();
		CHnum = (int)gd.getNextNumber();
		
		dir = new DirectoryChooser("choose 2Dtiff directory");
		Odirectory = dir.getDirectory();
		
		dir2 = new DirectoryChooser("Save directory");
		Sdirectory = dir2.getDirectory();
		
		if(Odirectory == null || Sdirectory == null) return;
		
		File OdirectoryFile = new File(Odirectory);
		final File names[] = OdirectoryFile.listFiles(); 
		IJ.log("names[]; "+String.valueOf(names.length));
		Arrays.sort(names);
		
		File SdirectoryFile = new File(Sdirectory+"resized1"+File.separator);
		
		if(SdirectoryFile.exists()==false){
			File  f1 = new File(Sdirectory+"resized1"+File.separator);
			f1.mkdirs();
			SdirectoryFile = new File(Sdirectory+"resized1"+File.separator);
		}
		
		final File Snames[] =  SdirectoryFile.listFiles();
		IJ.log("Snames[]; "+String.valueOf(Snames.length));	
		
		
		final int CHnumF=CHnum;
		final String OdirectoryF = Odirectory;
		final String SdirectoryF = Sdirectory;
		
		
		final int ResizingStepsF=(int)ResizingSteps;
		
		Prefs.set("XYendratio.double",XYendratio);
		Prefs.set("ResizingSteps.double", ResizingSteps);
		Prefs.set("CHnum.int",CHnum);
		
		if(thread_num_ <= 0) thread_num_ = 1;
		Prefs.set("thread_num.int", thread_num_);
		
		final double [] xySizeRatioArray = new double[(int)ResizingSteps-1];
		final int Fthread_num_=thread_num_;
		
		int EndFile=0;
		if(CHnum==3)
		EndFile=names.length-2;
		else if(CHnum==2)
		EndFile=names.length-1;
		else if(CHnum==1)
		EndFile=names.length;
		final int FendFile=EndFile;
		
		double xySizeRatio=XYendratio;
		double gapRatio=100-XYendratio;//80
		double StepRatio=(gapRatio/(ResizingSteps-1));
		
		final int [] OpenFileNumber1=new int[names.length];final int [] OpenFileNumber2=new int[names.length];
		final int [] OpenFileNumber3=new int[names.length];final int [] OpenFileNumber4=new int[names.length];
		final int [] OpenFileNumber5=new int[names.length];final int [] OpenFileNumber6=new int[names.length];
		final int [] OpenFileNumber7=new int[names.length];final int [] OpenFileNumber8=new int[names.length];
		final int [] OpenFileNumber9=new int[names.length];final int [] OpenFileNumber10=new int[names.length];
		
		int TifNumber=0, firsttif=-1;
		for(int itif=0; itif<names.length; itif++){
			String filename=names[itif].getName();
			int tifIndex=filename.lastIndexOf(".tif");
			if(tifIndex==-1)
			tifIndex=filename.lastIndexOf(".nrrd");
			
			if(tifIndex!=-1){
				TifNumber=TifNumber+1;
				if(firsttif==-1)
				firsttif=itif;
			}
		}
		final int ch2start=TifNumber/3, ch3start=TifNumber/3+TifNumber/3;
		
		
		
		for(int iistep=0; iistep<ResizingSteps-1; iistep++){
			
			xySizeRatioArray[iistep]=xySizeRatio;
			xySizeRatio=xySizeRatio+StepRatio;
			int resizestep=iistep+1;
			File  f = new File(Sdirectory+"resized"+resizestep+File.separator);
			f.mkdirs();
			
			double IncriSliceRatio=100/xySizeRatioArray[iistep];
			double OpenSlice=1;
			
			int ArrayLength=(int)((double)names.length*(xySizeRatioArray[iistep]/100)+1);
			//	IJ.log("ArrayLength ; "+String.valueOf(ArrayLength)+"  names.length; "+String.valueOf(names.length));
			if(resizestep==1){
				
				for(int ii3=0; ii3<ArrayLength; ii3++){
					OpenFileNumber1[ii3]=(int)OpenSlice;
					OpenSlice=OpenSlice+IncriSliceRatio;
				}
			}else if(resizestep==2){
				
				for(int ii3=0; ii3<ArrayLength; ii3++){
					OpenFileNumber2[ii3]=(int)OpenSlice;
					OpenSlice=OpenSlice+IncriSliceRatio;
				}
			}else if(resizestep==3){
				
				for(int ii3=0; ii3<ArrayLength; ii3++){
					//			IJ.log("OpenSlice3 ; "+String.valueOf(OpenSlice));
					OpenFileNumber3[ii3]=(int)OpenSlice;
					OpenSlice=OpenSlice+IncriSliceRatio;
				}
			}else if(resizestep==4){
				
				for(int ii3=0; ii3<ArrayLength; ii3++){
					OpenFileNumber4[ii3]=(int)OpenSlice;
					OpenSlice=OpenSlice+IncriSliceRatio;
				}
			}else if(resizestep==5){
				
				for(int ii3=0; ii3<ArrayLength; ii3++){
					OpenFileNumber5[ii3]=(int)OpenSlice;
					OpenSlice=OpenSlice+IncriSliceRatio;
				}
			}else if(resizestep==6){
				
				for(int ii3=0; ii3<ArrayLength; ii3++){
					OpenFileNumber6[ii3]=(int)OpenSlice;
					OpenSlice=OpenSlice+IncriSliceRatio;
				}
			}else if(resizestep==7){
				
				for(int ii3=0; ii3<ArrayLength; ii3++){
					OpenFileNumber7[ii3]=(int)OpenSlice;
					OpenSlice=OpenSlice+IncriSliceRatio;
				}
			}else if(resizestep==8){
				
				for(int ii3=0; ii3<ArrayLength; ii3++){
					OpenFileNumber8[ii3]=(int)OpenSlice;
					OpenSlice=OpenSlice+IncriSliceRatio;
				}
			}else if(resizestep==9){
				
				for(int ii3=0; ii3<ArrayLength; ii3++){
					OpenFileNumber9[ii3]=(int)OpenSlice;
					OpenSlice=OpenSlice+IncriSliceRatio;
				}
			}else if(resizestep==10){
				
				for(int ii3=0; ii3<ArrayLength; ii3++){
					OpenFileNumber10[ii3]=(int)OpenSlice;
					OpenSlice=OpenSlice+IncriSliceRatio;
				}
			}//	if(resizestep==1){
		}
		
		final AtomicInteger ai = new AtomicInteger(firsttif);
		final Thread[] threads = newThreadArray();
		
		for (int ithread = 0; ithread < threads.length; ithread++) {
			// Concurrently run in as many threads as CPUs
			threads[ithread] = new Thread() {
				
				{ setPriority(Thread.NORM_PRIORITY); }
				
				public void run() {
					
					ImagePlus impB,impC;
					int B=0,G=0;
					
					int OpendSlice1=0,OpendSlice2=0,OpendSlice3=0,OpendSlice4=0,OpendSlice5=0,OpendSlice6=0,OpendSlice7=0,OpendSlice8=0,OpendSlice9=0,OpendSlice10=0;
					int StartScan=0;
					for (int i = ai.getAndAdd(CHnumF); i <FendFile; i = ai.getAndAdd(CHnumF)) {
						ImageProcessor ipB,ipC;
						
						if(IJ.escapePressed())
						return;
						
						String filename=null;
						
						while(filename==null){
							//		IJ.wait(5);
							filename=names[i].getName();
						}
						int tifIndex=-1;
						tifIndex=filename.lastIndexOf(".tif");
						if(tifIndex==-1){
							IJ.wait(5);
							tifIndex=filename.lastIndexOf(".nrrd");
						}
						
						if(tifIndex==-1){
							IJ.wait(5);
							tifIndex=filename.lastIndexOf(".tif");
						}
						
						if(tifIndex==-1){
							IJ.wait(5);
							tifIndex=filename.lastIndexOf(".zip");
						}
						if(tifIndex!=-1){
							String truname = filename.substring(0, tifIndex);
							
					//		IJ.log("truname  "+truname);
							
							String savedname=null, Trusavedname=null;
							int DUP=0;
							if(i<Snames.length)	{
								for(int iii=0; iii<Snames.length; iii++){
									savedname = Snames[iii].getName();
									
									//			IJ.log("savedname  "+savedname);
									if(savedname!=null){
										
										int tifIndex2=savedname.lastIndexOf(".tif");
										if(tifIndex2!=-1)
										Trusavedname = savedname.substring(0, tifIndex2);
										if(Trusavedname!=null){
											int posi = -1;
											posi = truname.lastIndexOf(Trusavedname);
											
											//				IJ.log("  Trusavedname  "+Trusavedname+" ; "+String.valueOf(posi));
											if(posi!=-1)
											DUP=1;
										}
									}//if(savedname!=null){
								}
							}//if(i<Snames.length)	{
							if(DUP==0){
								ImagePlus imp=null;
								
								while(imp==null){
									IJ.wait(5);
									imp= IJ.openImage(OdirectoryF+filename);
									IJ.wait(5);
								}
								
								ImageProcessor ip = imp.getProcessor();//.duplicate()
								double WW = imp.getWidth();  
								double HH = imp.getHeight();  
								Calibration cal = imp.getCalibration();
								
								double xPxSize = cal.pixelWidth;
								double yPxSize = cal.pixelHeight;
								double zPxSize = cal.pixelDepth;
								
								ImageStack stack = new ImageStack((int)WW, (int)HH);
								
								if(CHnumF>1){
									//		IJ.log("CHnumF; "+CHnumF);
									stack.addSlice("Red", ip);
									
									if(CHnumF==2 || CHnumF==3){
										//	IJ.log("Ch2 start; "+String.valueOf(ch2start));
										String filename2=null;
										while(filename2==null){
											filename2=names[i+1].getName();
										}
										
										impB = IJ.openImage(OdirectoryF+filename2);
										ipB = impB.getProcessor();//.duplicate()
										stack.addSlice("Green", ipB);
										G=1;
									}
									if(CHnumF==3){
										
										String filename3=null;
										while(filename3==null){
											filename3=names[i+2].getName();
										}
										
										//					IJ.log("CHnumF3; "+String.valueOf(CHnumF));
										impC = IJ.openImage(OdirectoryF+filename3);
										ipC = impC.getProcessor();//.duplicate()
										stack.addSlice("Blue", ipC);
										B=1;
									}
									if(B==1 || G==1){
										//	IJ.log("B; "+B+"   G; "+G);
										imp.setStack(null, stack);
										imp.setDimensions(CHnumF, 1, 1);
									}
								}//if(CHnumF>1){
								
								cal.pixelWidth=xPxSize;
								cal.pixelHeight=yPxSize;
								cal.pixelDepth=zPxSize;
								
								ip.setInterpolationMethod(interpolationMethod);
								
								for(int istep=1; istep<ResizingStepsF; istep++){
									
									double ZMIPnum=100/(int)(xySizeRatioArray[istep-1]);
									
									if(ZMIPnum>2)
									ZMIPnum=(int)(100/xySizeRatioArray[istep-1]);
									
									//			IJ.log("Resizing step; "+String.valueOf(istep)+"  ZMIPnum; "+String.valueOf(ZMIPnum));
									
									if(ZMIPnum>=2){
										
										ImagePlus imp2 = new ImagePlus("DUP.tif", ip);
										
										//	IJ.log("ZMIPnum>=2, imp2 duplicate");
										
										double ySize=HH*xySizeRatioArray[istep-1]/100;
										double xSize=WW*xySizeRatioArray[istep-1]/100;
										
										int intYsize=(int)ySize;
										int intXsize=(int)xSize;
										
										ImageStack stack2 = new ImageStack(intXsize, intYsize);
										ImageProcessor ip2;
										
										if(CHnumF>1){
											for (int i3=1; i3<=CHnumF; i3++) {
												ip.setPixels(stack.getPixels(i3));
												String label = stack.getSliceLabel(i3);
												//	stack.deleteSlice(1);
												ip2 = ip.resize(intXsize, intYsize,true);
												if (ip2!=null){
													
													//		ImageProcessor ip3 = GrayResize(ip2);
													
													stack2.addSlice(label, ip2);
												}
											}
										}else if (CHnumF==1){
											ip2 = ip.resize(intXsize, intYsize,true);
											if (ip2!=null){
												//		ImageProcessor ip3 = GrayResize(ip2);
												
												stack2.addSlice(null, ip2);
											}
										}
										
										imp2.setStack(imp.getTitle(), stack2); // UPDATE the imp2 ImagePlus  
										
										
										Calibration cal2 = imp2.getCalibration();
										double NewxPxSize=xPxSize*(1/(xySizeRatioArray[istep-1]/100));
										cal2.pixelWidth=NewxPxSize;
										
										double NewyPxSize=yPxSize*(1/(xySizeRatioArray[istep-1]/100));
										cal2.pixelHeight=NewyPxSize;
										
										double NewzPxSize=zPxSize*(1/(xySizeRatioArray[istep-1]/100));
										cal2.pixelDepth = NewzPxSize;
										cal2.setUnit("microns");
										
										IJ.saveAs(imp2,"Tiff", SdirectoryF+"resized"+istep+File.separator+filename);
										//new FileSaver(imp).saveAsTiff(SdirectoryF+"resized"+istep+File.separator+names[i]); 
										
										imp2.flush();
										imp2.close();
										
									}else if(ZMIPnum<2){
										int SliceNoForOpen=0, PosiSlice=0,ArrayLength=0;
										int[] copied = new int[names.length];
										
										if(istep==1){
											ArrayLength=OpenFileNumber1.length;
											copied = Arrays.copyOf(OpenFileNumber1, OpenFileNumber1.length);
											StartScan=OpendSlice1-Fthread_num_*2;
										}else if(istep==2){
											ArrayLength=OpenFileNumber2.length;
											copied = Arrays.copyOf(OpenFileNumber2, OpenFileNumber2.length);
											StartScan=OpendSlice2-Fthread_num_*2;
										}else if(istep==3){
											ArrayLength=OpenFileNumber3.length;
											copied = Arrays.copyOf(OpenFileNumber3, OpenFileNumber3.length);
											StartScan=OpendSlice3-Fthread_num_*2;
										}else if(istep==4){
											ArrayLength=OpenFileNumber4.length;
											copied = Arrays.copyOf(OpenFileNumber4, OpenFileNumber4.length);
											StartScan=OpendSlice4-Fthread_num_*2;
										}else if(istep==5){
											ArrayLength=OpenFileNumber5.length;
											copied = Arrays.copyOf(OpenFileNumber5, OpenFileNumber5.length);
											StartScan=OpendSlice5-Fthread_num_*2;
										}else if(istep==6){
											ArrayLength=OpenFileNumber6.length;
											copied = Arrays.copyOf(OpenFileNumber6, OpenFileNumber6.length);
											StartScan=OpendSlice6-Fthread_num_*2;
										}else if(istep==7){
											ArrayLength=OpenFileNumber7.length;
											copied = Arrays.copyOf(OpenFileNumber7, OpenFileNumber7.length);
											StartScan=OpendSlice7-Fthread_num_*2;
										}else if(istep==8){
											ArrayLength=OpenFileNumber8.length;
											copied = Arrays.copyOf(OpenFileNumber8, OpenFileNumber8.length);
											StartScan=OpendSlice8-Fthread_num_*2;
										}else if(istep==9){
											ArrayLength=OpenFileNumber9.length;
											copied = Arrays.copyOf(OpenFileNumber9, OpenFileNumber9.length);
											StartScan=OpendSlice9-Fthread_num_*2;
										}else if(istep==10){
											ArrayLength=OpenFileNumber10.length;
											copied = Arrays.copyOf(OpenFileNumber10, OpenFileNumber10.length);
											StartScan=OpendSlice10-Fthread_num_*2;
										}
										if(StartScan<0)
										StartScan=0;
										
										for(int SliceOpen=StartScan; SliceOpen<ArrayLength; SliceOpen++){
											
											SliceNoForOpen=copied[SliceOpen];
											
											if(SliceNoForOpen==i){
												PosiSlice=1;
												
												if(istep==1){
													OpendSlice1=SliceOpen;
												}else if(istep==2){
													OpendSlice2=SliceOpen;
												}else if(istep==3){
													OpendSlice3=SliceOpen;
												}else if(istep==4){
													OpendSlice4=SliceOpen;
												}else if(istep==5){
													OpendSlice5=SliceOpen;
												}else if(istep==6){
													OpendSlice6=SliceOpen;
												}else if(istep==7){
													OpendSlice7=SliceOpen;
												}else if(istep==8){
													OpendSlice8=SliceOpen;
												}else if(istep==9){
													OpendSlice9=SliceOpen;
												}else if(istep==10){
													OpendSlice10=SliceOpen;
												}
												SliceOpen=ArrayLength;
											}else if(SliceNoForOpen>i)
											SliceOpen=ArrayLength;
										}
										
										if(PosiSlice==1){// if need to open and resize
											
											ImagePlus imp2 = new ImagePlus("DUP.tif", ip);
											//		else
											//ImagePlus imp2 =imp.duplicate();
											
											double ySize=HH*xySizeRatioArray[istep-1]/100;
											double xSize=WW*xySizeRatioArray[istep-1]/100;
											
											int intYsize=(int)ySize;
											int intXsize=(int)xSize;
											
											
											ImageStack stack2 = new ImageStack(intXsize, intYsize);
											ImageProcessor ip2;
											
											if (CHnumF>1){
												for (int i3=1; i3<=CHnumF; i3++) {
													ip.setPixels(stack.getPixels(i3));
													String label = stack.getSliceLabel(i3);
													ip2 = ip.resize(intXsize, intYsize,true);
													if (ip2!=null){
														//	ImageProcessor ip3 = GrayResize(ip2);
														stack2.addSlice(label, ip2);
													}
												}
											}else if (CHnumF==1){
												ip2 = ip.resize(intXsize, intYsize,true);
												if (ip2!=null){
													//		ImageProcessor ip3 = GrayResize(ip2);
													stack2.addSlice(null, ip2);
												}
											}
											
											imp2.setStack(imp.getTitle(), stack2); // UPDATE the imp2 ImagePlus  
											
											Calibration cal2 = imp2.getCalibration();
											double NewxPxSize=xPxSize*(1/(xySizeRatioArray[istep-1]/100));
											cal2.pixelWidth=NewxPxSize;
											
											double NewyPxSize=yPxSize*(1/(xySizeRatioArray[istep-1]/100));
											cal2.pixelHeight=NewyPxSize;
											
											double NewzPxSize=zPxSize*(1/(xySizeRatioArray[istep-1]/100));
											cal2.pixelDepth = NewzPxSize;
											cal2.setUnit("microns");
											
											IJ.saveAs(imp2,"Tiff", SdirectoryF+"resized"+istep+File.separator+filename);
											//new FileSaver(imp2).saveAsTiff(SdirectoryF+"resized"+istep+File.separator+filename); 
											
											imp2.flush();
											imp2.close();
										}//if(PosiSlice==1){// if need to open and resize
									}//		if(ZMIPnum<2){
									
								}//for(int istep=1; istep<ResizingStepsF; istep++){
								imp.flush();
								imp.close();
							}//if(Trusavedname!=truname){
						}//if(tifIndex!=-1){
						//		stack.flush();
						//		stack.close();
						//			System.gc();
					}//	for (int i = ai.getAndIncrement(); i < names.length; i = ai.getAndIncrement()) {
			}};//threads[ithread] = new Thread() {
		}//	for (int ithread = 0; ithread < threads.length; ithread++) {
		startAndJoin(threads);
	}//	public void Save_Pyramid(){
	
	ImageProcessor GrayResize(ImageProcessor Fip2){
		
		int maxValIP=0, minValIP=100000, sumpxIP=Fip2.getPixelCount(); 
		
		int bitd=Fip2.getBitDepth();
		double MaxVal=0;
		if(bitd==8)
		MaxVal=(double)255;
		else if (bitd==16)
		MaxVal=(double)65535;
		
		for(int ipscan=0; ipscan<sumpxIP; ipscan++){
			
			int pixip2=Fip2.get(ipscan);
			
			if(pixip2>maxValIP)
			maxValIP=pixip2;
			
			if(pixip2<minValIP)
			minValIP=pixip2;
		}
		int realMaxip=maxValIP-minValIP;
		//	double Exkusu=MaxVal/(double)realMaxip;
		double Exkusu=16;// 65536/4095;
		for(int ipscan2=0; ipscan2<sumpxIP; ipscan2++){
			
			int pixip2=Fip2.get(ipscan2);
			
			double altPxVal=(double)(pixip2-minValIP)*Exkusu;
			
			if(altPxVal>MaxVal)
			altPxVal=MaxVal;
			else if (altPxVal<0)
			altPxVal=0;
			
			Fip2.set(ipscan2, (int)altPxVal);
		}
		return Fip2;
	}//ImageProcessor GrayResize(ImageProcessor Fip2){
	
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



