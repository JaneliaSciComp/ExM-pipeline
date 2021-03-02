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
import ij.plugin.filter.GaussianBlur;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.imageplus.ImagePlusImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

import java.awt.*;
import java.awt.image.*;
import javax.imageio.*;
import javax.imageio.ImageIO;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.ImageIO;


import java.io.DataOutputStream;
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
import java.util.Iterator;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

import loci.formats.ImageReader;
import loci.formats.meta.IMetadata;
import loci.formats.out.TiffWriter;
import loci.formats.services.OMEXMLService;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.FormatTools;
import loci.formats.codec.CompressionType;
import ome.units.UNITS;
import ome.units.quantity.Length;
import ome.units.quantity.Power;
import ome.units.unit.Unit;
import ome.xml.model.enums.DimensionOrder;
import ome.xml.model.enums.EnumerationException;
import ome.xml.model.enums.PixelType;
import ome.xml.model.primitives.PositiveInteger;

public class Stack3D_build_from_brick implements PlugIn {
	//int wList [] = WindowManager.getIDList();
	
	private static boolean averageWhenDownsizing = true;
	int xorisize=0; int yorisize=0; int zorisize=0; int expand=0; int graythre=0;
	int thread_num_=0;
	
	
	public void run(String arg) {
		
		
		xorisize=(int)Prefs.get("xorisize.int",15932);
		yorisize=(int)Prefs.get("yorisize.int",8316);
		zorisize=(int)Prefs.get("zorisize.int",8518);
		expand=(int)Prefs.get("expand.int",10);
		graythre=(int)Prefs.get("graythre.int",185);
		//	String Sdirectory=Prefs.get("Sdirectory.String", "");
		
		String saveF = (String)Prefs.get("saveF.String", "TIFF");
		
		thread_num_=(int)Prefs.get("thread_num_.int",4);
		
		GenericDialog gd = new GenericDialog("3D volume building from block");
		
		gd.addNumericField("X size of original image", xorisize, 0);
		gd.addNumericField("Y size of original image", yorisize, 0);
		gd.addNumericField("Z size of original image", zorisize, 0);
		
		gd.addNumericField("GrayThreshold", graythre, 0);
		
		String [] saveformat = {"ZIP", "TIFF","TIFFPackBits","LZW"};
		gd.addRadioButtonGroup("in macro or not", saveformat, 2, 3, saveF);
		gd.addNumericField("xtimes expand", expand, 0);
		
		gd.addNumericField("CPU number", thread_num_, 0);
		
		//		gd.addStringField("SaveDirectory: ", Sdirectory,60);
		
		gd.showDialog();
		if(gd.wasCanceled()){
			return;
		}
		
		
		xorisize = (int)gd.getNextNumber();
		yorisize = (int)gd.getNextNumber();
		zorisize = (int)gd.getNextNumber();
		
		graythre = (int)gd.getNextNumber();
		
		saveF =(String)gd.getNextRadioButton();
		expand = (int)gd.getNextNumber();
		
		thread_num_ = (int)gd.getNextNumber();
		//		Sdirectory=gd.getNextString();
		
		Prefs.set("saveF.String", saveF);
		Prefs.set("xorisize.int", xorisize);
		Prefs.set("yorisize.int", yorisize);
		Prefs.set("zorisize.int", zorisize);
		Prefs.set("graythre.int", graythre);
		Prefs.set("expand.int", expand);
		//		Prefs.set("Sdirectory.String", Sdirectory);
		
		if(thread_num_ <= 0) thread_num_ = 1;
		Prefs.set("thread_num_.int", thread_num_);
		
		
		
		DirectoryChooser dirO = new DirectoryChooser("Brick directory");
		String Odirectory = dirO.getDirectory();
		
		DirectoryChooser dirs = new DirectoryChooser("Save directory");
		String Sdirectory = dirs.getDirectory();
		
		IJ.log("graythre; "+graythre+" expand; "+expand);
		IJ.log("Odirectory; "+Odirectory+"   Sdirectory; "+Sdirectory);
		
		File OdirectoryFile = new File(Odirectory);
		final File names[] = OdirectoryFile.listFiles(); 
		Arrays.sort(names);
		
		
		IJ.log("names length;"+names.length);
		String txtpath = Odirectory+"xyz.txt";
		
		String contents0="";
		try {
			contents0 = new String(Files.readAllBytes(Paths.get(txtpath))); 
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String[] lines0 = contents0.split("\n");// original body synapse list
		
		
		
		final int Finthread_num_=thread_num_;
		brickfunction(xorisize, yorisize,zorisize, Finthread_num_,expand,Odirectory,lines0,names,Sdirectory,saveF,graythre);
		
		IJ.log("Slice creation Done");
		
	} //public void run(String arg) {
	
	public void brickfunction(int Fxorisize, int Fyorisize, int Fzorisize, int FFinthread_num_, int Fexpand, final String FOdirectory, String [] lines0, File names[],final String SdirectoryF, final String saveFF, final int graythreF){
		
		IJ.log("saveFF; "+saveFF);		
		
		final int xbricknum = Integer.parseInt(lines0[3]);
		final int ybricknum = Integer.parseInt(lines0[4]);
		final int zbricknum = Integer.parseInt(lines0[5]);
		
		final int overlap = Integer.parseInt(lines0[10]);
		final int xsize = Integer.parseInt(lines0[7]);
		final int ysize = Integer.parseInt(lines0[8]);
		final int zsize = Integer.parseInt( lines0[9]);
		
		
		IJ.log("xsize; "+xsize+"  ysize; "+ysize+"  zsize; "+zsize+"  Fexpand; "+Fexpand);
		IJ.log("Fxorisize; "+Fxorisize+"  Fyorisize; "+Fyorisize+"  Fzorisize; "+Fzorisize);
		
		
		int totalbrick=xbricknum * ybricknum*zbricknum;
		int XYtotalbrick=xbricknum * ybricknum;
		
		IJ.log("xbricknum; "+xbricknum+"  ybricknum; "+ybricknum+"  zbricknum; "+zbricknum+"  totalbrick; "+totalbrick);
		
		
		final int FXYtotalbrick = XYtotalbrick;
		
		String [] brickpositionarray = new String [totalbrick];
		
		int zstart=1, ibricknum=0;
		
		for(int zposi=1; zposi<=zbricknum; zposi++){
			
			//		IJ.log("zstart; "+zstart);
			
			int xstart=Fexpand/2;// Imagej bicubig has 1px shift = x Fexpand shift
			for(int xposi=1; xposi<=xbricknum; xposi++){
				//			IJ.log("xstart; "+xstart);
				int ystart=Fexpand/2;
				for(int yposi=1; yposi<=ybricknum; yposi++){
					
					brickpositionarray[ibricknum]=xstart+","+ystart+","+zstart;
					
					ibricknum=ibricknum+1;
					ystart=ystart+ysize*Fexpand;
					
				}
				xstart=xstart+xsize*Fexpand;
				
			}
			zstart=zstart+zsize*Fexpand-(overlap/2);
			
		}//for(int zposi=1; zposi<=xbricknum; zposi++){
		
		final int FFzorisize=Fzorisize;
		final int FFxorisize=Fxorisize;
		final int FFyorisize=Fyorisize;
		final int FFoverlapori=overlap*Fexpand;
		int nextstartnum=0;
		
		for(int izbrick=1; izbrick<=zbricknum; izbrick++){
			
			final int Fizbrick = izbrick;
			
			final AtomicInteger ai = new AtomicInteger(0);
			
			//final AtomicInteger ai = new AtomicInteger(544); // for test
			
			final Thread[] threads = newThreadArray();
			
			final ImageStack bigstack =  IJ.createImage("", Fxorisize,Fyorisize,zsize*Fexpand, 8).getStack();
			//ImageStack bigstack1 = new ImageStack (Fxorisize,Fyorisize,zsize*Fexpand);
			
			
			IJ.log(izbrick+" times, threads.length; "+threads.length+"  ai; "+ai+"  Fxorisize; "+Fxorisize+"  Fyorisize; "+Fyorisize+"  zsize*Fexpand; "+zsize*Fexpand);
			
			//	for(int iz=1; iz<=zsize*Fexpand; iz++){
			//		ImageProcessor Fip2 = bigstack1.getProcessor(iz);
			
			//			for(int ixy=0; ixy<Fxorisize*Fexpand*Fyorisize*Fexpand; ixy++){
			//			Fip2.set(ixy,0);
			//		}
			//	}
			
			//	final ImageStack bigstack = bigstack1;
			
			
			for (int ithread = 0; ithread < threads.length; ithread++) {
				// Concurrently run in as many threads as CPUs
				threads[ithread] = new Thread() {
					
					{ setPriority(Thread.NORM_PRIORITY); }
					
					public void run() {
						
						
						for(int ii=ai.getAndIncrement(); ii<names.length; ii = ai.getAndIncrement()){
							
							IJ.showProgress((double)ii/(double)names.length);
							
							//	ImagePlus imp = new ImagePlus[1];
							//	ImageProcessor ip [] = new ImageProcessor[1];
							
							int ImageNum=0;
							ImagePlus imp=null;
							
							File iiFile = new File(FOdirectory+names[ii]);
							
							String filenameST = names[ii].toString();
							
							int zipindex =filenameST.lastIndexOf(".zip");
							//			int txtindex =filenameST.lastIndexOf(".txt");
							int nslice=0;
							
							int startpointx = 0, startpointy=0, startpointz=0;
							String [] contents = brickpositionarray[ii].split(",");
							
							int halfoverlap = (int) (overlap*Fexpand)/2;// e.x. 10
							int izend =0;
							
							if(zipindex!=-1){
								//		IJ.log("filenameST; "+filenameST);
								while(imp==null){
									
									imp = IJ.openImage(filenameST);
								}
								
								ImageStack stack=imp.getStack();
								
								int [] info= imp.getDimensions();
								final int width = info[0];//52
								final int height = info[1];//52
								//	int nChannels = info[2];
								nslice = info[3];//52
								
								
								
								//	IJ.log("width; "+width+"  height; "+height);
								//imp.unlock();
								//	imp.show();
								
								//CanvasResizer resizer = new CanvasResizer();
								//	stack = resizer.expandStack(stack,width*Fexpand,height*Fexpand,0,0);
								
								ImageStack expandstack=null;
								if(Fexpand!=1){
									expandstack =  IJ.createImage("expand", width*Fexpand,height*Fexpand,nslice, 8).getStack();
									for(int izresize=1; izresize<=nslice; izresize++){
										ImageProcessor ip = stack.getProcessor(izresize);
										
										//	ImageProcessor expandip = expandstack.getProcessor(izresize);
										ip.setInterpolationMethod( ImageProcessor.NONE );//BICUBIC,BILINEAR
										ImageProcessor expandip=ip.resize(width*Fexpand,height*Fexpand,true);
										expandstack.setProcessor(expandip, izresize);
										
									}
									imp = new ImagePlus ("expand",expandstack);
									imp = resizeZ(imp, nslice*Fexpand, ImageProcessor.NONE);//BILINEAR,BICUBIC
									stack=imp.getStack();
								}
								
								info= imp.getDimensions();
								
								int Exwidth = info[0];//52
								int Exheight = info[1];//52
								
								//			IJ.log( filenameST+"  318 after resized nslices; "+info[3]+" HEIGHT; "+info[1]);
								
								int binarize=0;
								
								if(binarize!=0){
									for(int ithrez=1; ithrez<=(int) info[3]; ithrez++){
										
										ImageProcessor ip = stack.getProcessor(ithrez);
										
										for (int ixypix=0; ixypix<Exwidth*Exheight; ixypix++){
											
											int pix0=ip.get(ixypix);
											
											if(pix0>2)
											ip.set(ixypix,255);
											
										}
									}
								}//if(binarize!=0){
								
								//		IJ.runMacroFile("/Users/otsunah/test/josh.ijm",String.valueOf(Fexpand));
								//	IJ.showStatus("3x3 x3 time Gaussian filteres...");
								
								int fijibuler=0;
								
								if(fijibuler==1){
									for(int itry=1; itry<=3; itry++){
										for(int ithrez=1; ithrez<=(int) info[3]; ithrez++){
											
											GaussianBlur blurimg = new GaussianBlur(); 
											ImageProcessor ip = stack.getProcessor(ithrez);
											blurimg.blurGaussian(ip, 3);
										}
									}
								}//	if(fijibuler==1){
								
								//			ImagePlus in = null;
								
								//		imp = gaussBlur( imp, new double[]{ 3, 3, 3 }, 1 );
								//		imp = gaussBlur( imp, new double[]{ 3, 3, 3 }, 1 );
								//		imp = gaussBlur( imp, new double[]{ 3, 3, 3 }, 1 );
								
								//		stack=impout.getStack();
								
								//	stack=imp.getStack();
								//		imp.flush();
								//		imp.close();
								
								info= imp.getDimensions();
								
								Exwidth = info[0];//520
								Exheight = info[1];//520
								
								//			IJ.log( ST+ii+"  362 after resized nslices; "+info[3]+" HEIGHT; "+info[1]);
								
								int doBinarize=1;
								
								if(doBinarize==1){
									for(int ithrez2=1; ithrez2<=(int) info[3]; ithrez2++){
										
										ImageProcessor ip = stack.getProcessor(ithrez2);
										
										for (int ixypix=0; ixypix<Exwidth*Exheight; ixypix++){
											
											int pix0=ip.get(ixypix);
											
											if(pix0<graythreF)
											ip.set(ixypix,0);
											else
											ip.set(ixypix,255);
											
										}
									}
								}
								int xposi= filenameST.lastIndexOf("_xs");
								int xeposi= filenameST.lastIndexOf("_xe");
								
								int yposi= filenameST.lastIndexOf("_ys");
								int yeposi= filenameST.lastIndexOf("_ye");
								
								int zposi= filenameST.lastIndexOf("_zs");
								int zeposi= filenameST.lastIndexOf("_ze");
								
								zipindex = filenameST.lastIndexOf(".zip");
								
								startpointx = Integer.parseInt (filenameST.substring(xposi+3,xeposi));
								int endpointx = Integer.parseInt (filenameST.substring(xeposi+3,yposi));
								
								startpointy = Integer.parseInt (filenameST.substring(yposi+3,yeposi));
								int endpointy = Integer.parseInt (filenameST.substring(yeposi+3,zposi));
								
								startpointz = Integer.parseInt (filenameST.substring(zposi+3,zeposi));
								int endpointz = Integer.parseInt (filenameST.substring(zeposi+3,zipindex));
								izend= nslice*Fexpand;//-halfoverlap
								
								int bigstackz=1;//startpointz
								
								//	IJ.log("startpointx; "+startpointx+"  startpointy; "+startpointy+" startpointz; "+startpointz+"  izend; "+izend+"  halfoverlap; "+halfoverlap);
								
								//	if(ii<FXYtotalbrick)
								//	halfoverlap=1;
								
								for(int izscan=1; izscan<=zsize*Fexpand; izscan++){
									
									
									ImageProcessor Fip = stack.getProcessor(izscan);
									
									int brickx=startpointx;
									for(int ixscan=0; ixscan<=xsize*Fexpand; ixscan++){//	for(int ixscan=halfoverlap+1; ixscan<=xsize*Fexpand+halfoverlap; ixscan++){
										
										int bricky=startpointy;
										for(int iyscan=0; iyscan<=ysize*Fexpand; iyscan++){//for(int iyscan=halfoverlap+1; iyscan<=ysize*Fexpand+halfoverlap; iyscan++){
											
											if(iyscan<height*Fexpand && ixscan<width*Fexpand){
												int pix= Fip.get(ixscan,iyscan);
												
												if(pix>0){
													//		IJ.log(ii+"  pix is more than 0; "+ixscan+"  iyscan; "+iyscan+"  izscan; "+izscan+"  brickx; "+brickx+"  bricky; "+bricky);
													if(Fxorisize>brickx && Fyorisize>bricky){
														ImageProcessor ipbig = bigstack.getProcessor(bigstackz);
														ipbig.set(brickx,bricky,pix);
													}
												}
											}//if(iyscan<height){
											bricky=bricky+1;
										}
										brickx=brickx+1;
									}
									bigstackz=bigstackz+1;
									
								}//for(int izscan=startpointz; izscan<startpointz+FFzorisize; izscan++){
								
								//		Calibration cal = imp[0].getCalibration();
								
								//	impout.flush();
								imp.flush();
								imp.close();
								
								
							}//if(exists && zipindex!=-1 || txtindex!=-1){
						}//for(int ii=ai.getAndIncrement(); ii<FXYtotalbrick; ii = ai.getAndIncrement()){
				}};
			}//	for (int ithread = 0; ithread < threads.length; ithread++) {
			startAndJoin(threads);
			
			
			ImagePlus impnew = new ImagePlus ("Brick_",bigstack);
			ImageStack stacksave = impnew.getStack();
			
			
			int [] info= impnew.getDimensions();
			int width = info[0];
			int height = info[1];
			int nslice = info[3];
			
			IJ.showStatus("Writing files...");
			
			IJ.log("width; "+String.valueOf(width)+"  height; "+String.valueOf(height)+"  nslice; "+String.valueOf(nslice));
			
			final AtomicInteger ai2 = new AtomicInteger(1);
			final int Fnextstartnum = nextstartnum;
			
			
			for (int ithread = 0; ithread < threads.length; ithread++) {
				// Concurrently run in as many threads as CPUs
				threads[ithread] = new Thread() {
					
					{ setPriority(Thread.NORM_PRIORITY); }
					
					public void run() {
						
						
						for(int islicesave=ai2.getAndIncrement(); islicesave<=nslice; islicesave = ai2.getAndIncrement()){
							
							String addst="";
							
							int totalslicenum=Fnextstartnum+islicesave;
							
							if(totalslicenum<=Fzorisize){
								
								if(totalslicenum<10000 && 999<totalslicenum)
								addst="0";
								else if(totalslicenum<1000 && 99<totalslicenum)
								addst="00";
								else if(totalslicenum<100 && 9<totalslicenum)
								addst="000";
								else if(totalslicenum<10)
								addst="0000";
								
								ImageProcessor saveip = stacksave.getProcessor(islicesave);
								ImagePlus saveimp = new ImagePlus (addst+totalslicenum,saveip);
								
								if(saveFF.equals("ZIP")){
									FileSaver saver = new FileSaver(saveimp);
									saver.saveAsZip(SdirectoryF+addst+totalslicenum+".zip");
								}else if(saveFF.equals("TIFF")){
									FileSaver saver = new FileSaver(saveimp);
									saver.saveAsTiff(SdirectoryF+addst+totalslicenum+".tif");
								}else if(saveFF.equals("TIFFPackBits")){
									
									BufferedImage image = saveimp.getBufferedImage(); // Your input image
									
									Iterator<ImageWriter> writer1 = ImageIO.getImageWritersByFormatName("TIFF"); // // Assuming a TIFF plugin is installed
									ImageWriter writer = writer1.next();
									
									try{ // Your output file or stream
										ImageOutputStream out = ImageIO.createImageOutputStream(new File(SdirectoryF+addst+totalslicenum+".tif"));//new FileOutputStream(SdirectoryF+addst+totalslicenum+".tif")
										writer.setOutput(out);
										
										//		IJ.log("file save; "+SdirectoryF+addst+totalslicenum+".tif");
										
										ImageWriteParam param = writer.getDefaultWriteParam();
										param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
										param.setCompressionType("PackBits");
										
										writer.write(null, new IIOImage(image, null, null), param);//
										
									} catch (IOException e) {
										e.printStackTrace();
									}
									
									writer.dispose();
									
									
								}
								else if(saveFF.equals("LZW")){
									
									
									try{
										String inFile = addst+islicesave;
										String outFile = SdirectoryF+addst+islicesave+".tif";
										
										BufferedImage image = saveip.getBufferedImage();
										
										IMetadata newMetadata;
										TiffWriter writer = new TiffWriter();
										if (saveimp.getType() == saveimp.GRAY8)
										{
											newMetadata = initializeMetadata(width, height, 1, FormatTools.UINT8);
											byte[] plane = (byte[])saveip.getPixels();
											
											writer.setMetadataRetrieve(newMetadata);
											writer.setCompression(CompressionType.LZW.getCompression());
											writer.setWriteSequentially(true);
											writer.setInterleaved(false);
											writer.setBigTiff(false);
											writer.setId(outFile);
											
											writer.saveBytes(0, plane, 0, 0, width, height);
										}
										else if (saveimp.getType() == saveimp.GRAY16)
										{
											newMetadata = initializeMetadata(width, height, 1, FormatTools.UINT16);
											ByteBuffer buffer = ByteBuffer.allocate(width * height * 2);
											buffer.order(ByteOrder.LITTLE_ENDIAN);
											short[] data = (short[])saveip.getPixels();
											for (int yy = 0; yy < height; yy++) {
												for (int xx = 0; xx < width; xx++) {
													buffer.putShort(data[yy*width+xx]);
												}
											}
											
											writer.setMetadataRetrieve(newMetadata);
											writer.setCompression(CompressionType.LZW.getCompression());
											writer.setWriteSequentially(true);
											writer.setInterleaved(false);
											writer.setBigTiff(false);
											writer.setId(outFile);
											
											writer.saveBytes(0, buffer.array(), 0, 0, width, height);
										}
										writer.close();
									} catch  (Exception e) {
										e.printStackTrace();
									}
									
									
								}
								saveimp.flush();
								saveimp.close();
							}//if(totalslicenum<=Fzorisize){
							
						}//for(int ii=ai.getAndIncrement(); ii<FXYtotalbrick; ii = ai.getAndIncrement()){
				}};
			}//	for (int ithread = 0; ithread < threads.length; ithread++) {
			startAndJoin(threads);
			
			nextstartnum = nextstartnum+nslice;
			
			impnew.flush();
			impnew.close();
			System.gc();
			System.gc();
		}//for(int izbrick=1; izbrick<=zbricknum; izbrick++){
		
	}
	
	private IMetadata initializeMetadata(int width, int height, int zdepth, int pixelType) {
		Exception exception = null;
		try {
			// create the OME-XML metadata storage object
			ServiceFactory factory = new ServiceFactory();
			OMEXMLService service = factory.getInstance(OMEXMLService.class);
			IMetadata meta = service.createOMEXMLMetadata();
			meta.createRoot();
			
			
			// define each stack of images - this defines a single stack of images
			meta.setImageID("Image:0", 0);
			meta.setPixelsID("Pixels:0", 0);
			
			// specify that the pixel data is stored in big-endian format
			// change 'TRUE' to 'FALSE' to specify little-endian format
			//            for (int i=0;i<zdepth;i++){
			meta.setPixelsBinDataBigEndian(Boolean.FALSE, 0, 0);
			//            }
			
			meta.setPixelsBigEndian(Boolean.FALSE,0);
			// specify that the images are stored in ZCT order
			meta.setPixelsDimensionOrder(DimensionOrder.XYZCT, 0);
			
			// specify that the pixel type of the images
			meta.setPixelsType(
			PixelType.fromString(FormatTools.getPixelTypeString(pixelType)), 0);
			
			// specify the dimensions of the images
			meta.setPixelsSizeX(new PositiveInteger(width), 0);
			meta.setPixelsSizeY(new PositiveInteger(height), 0);
			meta.setPixelsSizeZ(new PositiveInteger(zdepth), 0);
			meta.setPixelsSizeC(new PositiveInteger(1), 0);
			meta.setPixelsSizeT(new PositiveInteger(1), 0);
			// specify the dimensions of the pixels
			meta.setPixelsPhysicalSizeX(new Length(1.0f,UNITS.MICROMETER),0);
			meta.setPixelsPhysicalSizeY(new Length(1.0f,UNITS.MICROMETER),0);
			meta.setPixelsPhysicalSizeZ(new Length(1.0f,UNITS.MICROMETER),0);
			// define each channel and specify the number of samples in the channel
			// the number of samples is 3 for RGB images and 1 otherwise
			meta.setChannelID("Channel:0:0", 0, 0);
			meta.setChannelSamplesPerPixel(new PositiveInteger(1), 0, 0);
			
			return meta;
		}
		catch (DependencyException e) {
			exception = e;
		}
		catch (ServiceException e) {
			exception = e;
		}
		catch (EnumerationException e) {
			exception = e;
		}
		
		System.err.println("Failed to populate OME-XML metadata object.");
		exception.printStackTrace();
		return null;
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
	
	private ImagePlus resizeZ(ImagePlus imp, int newDepth, int interpolationMethod) {
		ImageStack stack1 = imp.getStack();
		int width = stack1.getWidth();
		int height = stack1.getHeight();
		int depth = stack1.getSize();
		int bitDepth = imp.getBitDepth();
		ImagePlus imp2 = IJ.createImage(imp.getTitle(), bitDepth+"-bit", width, height, newDepth);
		if (imp2==null) return null;
		ImageStack stack2 = imp2.getStack();
		ImageProcessor ip = imp.getProcessor();
		ImageProcessor xzPlane1 = ip.createProcessor(width, depth);
		xzPlane1.setInterpolationMethod(interpolationMethod);
		ImageProcessor xzPlane2;		
		Object xzpixels1 = xzPlane1.getPixels();
		IJ.showStatus("Z Scaling...");
		for (int y=0; y<height; y++) {
			IJ.showProgress(y, height-1);
			for (int z=0; z<depth; z++) { // get xz plane at y
				Object pixels1 = stack1.getPixels(z+1);
				System.arraycopy(pixels1, y*width, xzpixels1, z*width, width);
			}
			xzPlane2 = xzPlane1.resize(width, newDepth, averageWhenDownsizing);
			Object xypixels2 = xzPlane2.getPixels();
			for (int z=0; z<newDepth; z++) {
				Object pixels2 = stack2.getPixels(z+1);
				System.arraycopy(xypixels2, z*width, pixels2, y*width, width);
			}
		}
		return imp2;
	}
	
	public static <T extends RealType<T> & NativeType<T>> ImagePlus gaussBlur( 
		final ImagePlus in,
		final double[] sigma, 
	final int numThreads )
	{
		RandomAccessibleInterval<T> img = ImageJFunctions.wrap( in );
		ImagePlusImgFactory<T> factory = new ImagePlusImgFactory<>( Util.getTypeFromInterval( img ) );
		ImagePlusImg< T, ? > output = factory.create( img );
		
		Gauss3.gauss( sigma, Views.extendZero( img ), output, numThreads );
		
		in.close();
		
		return output.getImagePlus();
	}
	
	public void setAverageWhenDownsizing(boolean averageWhenDownsizing) {
		this. averageWhenDownsizing = averageWhenDownsizing;
	}
	
}




