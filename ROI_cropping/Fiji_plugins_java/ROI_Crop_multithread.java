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
//import ij.plugin.filter.GaussianBlur;

import java.awt.*;
import java.awt.image.*;
import javax.imageio.*;
import javax.imageio.ImageIO;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi.*;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.ImageWriteParam;


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


import loci.common.services.ServiceFactory;
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

public class ROI_Crop_multithread implements PlugInFilter {
	
	int thread_num_=0, StartSlice=0, endslice=0;
	ImagePlus imp_;
	
	public int setup(String arg, ImagePlus imp) {
		this.imp_ = imp;
		
		int wList [] = WindowManager.getIDList();
		if (wList==null) {
			IJ.showMessage("Please open a ROI file");
			return DOES_8G + DOES_16 + DOES_32;
		}
		
		thread_num_=(int)Prefs.get("thread_num_.int",4);
		StartSlice=(int)Prefs.get("StartSlice.int",1);
		endslice=(int)Prefs.get("endslice.int",10);
		String saveF = (String)Prefs.get("saveF.String", "uncompressedTIFF");
		
		GenericDialog gd = new GenericDialog("ROI cropping multi 2D files (tif, png, zip)");
		
		String [] saveformat = {"ZIP", "uncompressedTIFF","TIFFPackBits_8bit","LZW"};
		gd.addRadioButtonGroup("Output format", saveformat, 1, 4, saveF);
		
		gd.addNumericField("CPU number", thread_num_, 0);
		gd.addNumericField("Start slice 1~", StartSlice, 0);
		gd.addNumericField("End slice", endslice, 0);
		
		
		gd.showDialog();
		if(gd.wasCanceled()){
			return DOES_8G + DOES_16 + DOES_32;
		}
		
		saveF =(String)gd.getNextRadioButton();
		thread_num_ = (int)gd.getNextNumber();
		StartSlice = (int)gd.getNextNumber();
		endslice = (int)gd.getNextNumber();
		
		Prefs.set("saveF.String", saveF);
		
		if(thread_num_ <= 0) thread_num_ = 1;
		Prefs.set("thread_num.int", thread_num_);
		
		DirectoryChooser dirO = new DirectoryChooser("serial tiff for cropping directory");
		String Odirectory = dirO.getDirectory();
		
		DirectoryChooser dirs = new DirectoryChooser("Save directory");
		String Sdirectory = dirs.getDirectory();
		
		IJ.log("Odirectory; "+Odirectory+"   Sdirectory; "+Sdirectory);
		
		File OdirectoryFile = new File(Odirectory);
		File namespre[] = OdirectoryFile.listFiles(); 
		Arrays.sort(namespre);
		
		//rename to 5 digit///
		
		for(int iname=0; iname<namespre.length; iname++){
			
			if(namespre[iname].getName().lastIndexOf(".tif")!=-1){
				
				String filename = namespre[iname].getName().substring(0,namespre[iname].getName().lastIndexOf(".tif"));
				
				int filenum =Integer.parseInt(filename);
				
				String ST="";
				if(filenum>999 && filenum<10000)
				ST="0";
				else if(filenum>99 && filenum<1000)
				ST="00";
				else if(filenum>9 && filenum<100)
				ST="000";
				else if(filenum<10)
				ST="0000";
				
				File prepath = new File(Odirectory+namespre[iname].getName());
				File newpath = new File(Odirectory+ST+filenum+".tif");
				
				prepath.renameTo(newpath);
				
			}
		}
		
		OdirectoryFile = new File(Odirectory);
		final File names[] = OdirectoryFile.listFiles(); 
		Arrays.sort(names);
		
		IJ.log("names length;"+names.length);
		
		Prefs.set("thread_num_.int", thread_num_);
		Prefs.set("StartSlice.int", StartSlice);
		Prefs.set("endslice.int", endslice);
		Prefs.set("saveF.String", saveF);
		
		long timestart = System.currentTimeMillis();
		Threfunction(Odirectory,names,Sdirectory,thread_num_,StartSlice,endslice,saveF);
		
		long timeend = System.currentTimeMillis();
		
		long gapS = (timeend-timestart)/1000;
		
		IJ.log("Done "+gapS+" second");
		return DOES_8G + DOES_16 + DOES_32;
	} //public void run(String arg) {
	
	public void run(ImageProcessor ip){
		
	}
	
	public void Threfunction (final String FOdirectory, File names[],final String SdirectoryF, final int thread_num_F, final int StartSliceF, final int endsliceF, final String saveFF){
		
		final AtomicInteger ai = new AtomicInteger(StartSliceF-1);
		final Thread[] threads = newThreadArray();
		
		//IJ.log("FOdirectory+names[5].getName();  "+FOdirectory+names[5].getName());
		
		
		
		IJ.log("thread_num_F; "+thread_num_F+"  FOdirectory; "+FOdirectory+"  SdirectoryF; "+SdirectoryF);
		IJ.log("StartSliceF; "+StartSliceF+"  endsliceF; "+endsliceF);
		
		ImagePlus impmask = WindowManager.getCurrentImage();
		final Roi roi = impmask.getRoi();
		
		if ((roi==null||!roi.isArea())) {
			IJ.error("Area selection required");
		}
		
		for (int ithread = 0; ithread < threads.length; ithread++) {
			threads[ithread] = new Thread() {
				
				{ setPriority(Thread.NORM_PRIORITY); }
				
				public void run() {
					
					for(int ii=ai.getAndIncrement(); ii<endsliceF+1; ii = ai.getAndIncrement()){
						
						
						IJ.showStatus(String.valueOf(ii));
						
						ImagePlus imp =null;// new ImagePlus();
						ImageProcessor ip=null;
						
						int tifposi = names[ii].getName().lastIndexOf("tif");
						if(tifposi==-1)
						tifposi = names[ii].getName().lastIndexOf("png");
						if(tifposi==-1)
						tifposi = names[ii].getName().lastIndexOf("zip");
						
						if(tifposi>0){
							while(imp==null){
								imp = IJ.openImage(FOdirectory+names[ii].getName());
							}
							
							String savename= names[ii].getName().substring(0,tifposi-1);
							
							while(ip==null){
								ip = imp.getProcessor();//.duplicate()
							}
							
							int [] info= imp.getDimensions();
							final int width = info[0];//52
							final int height = info[1];//52
							
							int sumpx= width*height;
							
							ip.setRoi(roi);//cropX, cropY, targetWidth, targetHeight
							ImageProcessor ipcrop = ip.crop();
							
							//		IJ.log("width; "+width+"  height; "+height);
							ImagePlus croppedimp= new ImagePlus ("cropped",ipcrop);
							
							if(saveFF.equals("ZIP")){
								FileSaver saver = new FileSaver(croppedimp);
								saver.saveAsZip(SdirectoryF+savename+".zip");
								
							}else if(saveFF.equals("uncompressedTIFF")){
								
								FileSaver saver = new FileSaver(croppedimp);
								saver.saveAsTiff(SdirectoryF+savename+".tif");
							}else if(saveFF.equals("TIFFPackBits_8bit")){
								BufferedImage image = croppedimp.getBufferedImage(); // Your input image
								
								Iterator<ImageWriter> writer1 = ImageIO.getImageWritersByFormatName("TIFF"); // // Assuming a TIFF plugin is installed
								ImageWriter writer = writer1.next();
								
								try{ // Your output file or stream
									ImageOutputStream out = ImageIO.createImageOutputStream(new File(SdirectoryF+savename+".tif"));//new FileOutputStream(SdirectoryF+addst+totalslicenum+".tif")
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
							}else if(saveFF.equals("LZW")){//else if(saveFF.equals("TIFFPackBits")){
								try{
									
									String outFile = SdirectoryF+savename+".tif";
									
									BufferedImage image = ipcrop.getBufferedImage();
									
									IMetadata newMetadata;
									TiffWriter writer = new TiffWriter();
									if (croppedimp.getType() == croppedimp.GRAY8)
									{
										newMetadata = initializeMetadata(width, height, 1, FormatTools.UINT8);
										byte[] plane = (byte[])ipcrop.getPixels();
										
										writer.setMetadataRetrieve(newMetadata);
										writer.setCompression(CompressionType.LZW.getCompression());
										writer.setWriteSequentially(true);
										writer.setInterleaved(false);
										writer.setBigTiff(false);
										writer.setId(outFile);
										
										writer.saveBytes(0, plane, 0, 0, width, height);
									}
									else if (croppedimp.getType() == croppedimp.GRAY16)
									{
										newMetadata = initializeMetadata(width, height, 1, FormatTools.UINT16);
										ByteBuffer buffer = ByteBuffer.allocate(width * height * 2);
										buffer.order(ByteOrder.LITTLE_ENDIAN);
										short[] data = (short[])ipcrop.getPixels();
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
							
							imp.flush();
							imp.close();
							
							croppedimp.flush();
							croppedimp.close();
							
						}//	if(tifposi>0){
						
					}//for(int ii=ai.getAndIncrement(); ii<FXYtotalbrick; ii = ai.getAndIncrement()){
			}};
		}//	for (int ithread = 0; ithread < threads.length; ithread++) {
		startAndJoin(threads);
		
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
}




