import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.io.File;
import java.nio.ByteOrder;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import org.bridj.Pointer;
import static org.bridj.Pointer.*;
import com.nativelibs4java.opencl.*;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.opencl.CLImageFormat.*;
import com.nativelibs4java.util.*;

public class Connect_Flagments implements PlugInFilter {
	ImagePlus imp_;
	int iteration_ = (int)Prefs.get("iteration.int",1);
	int r_max_  = (int)Prefs.get("r_max.int",10);
	int r_min_  = (int)Prefs.get("r_min.int",10);
	int r_step_  = (int)Prefs.get("r_step.int",1);
	int quality_ = (int)Prefs.get("quality.int",10);
	int filtertype = (int)Prefs.get("ftype.int",0);
	int threshold_ = (int)Prefs.get("th.int",50);
	String ftype_;
	int thread_num_ = (int)Prefs.get("thread_num.int",8);

	public int setup(String arg, ImagePlus imp) {
		this.imp_ = imp;
		return DOES_8G + DOES_16 + DOES_32;
	}

	private boolean showDialog() {
		GenericDialog gd = new GenericDialog("Connect_Flagments parameter");
		
		String[] types = {"GAUSSIAN", "MEAN"};
		gd.addNumericField("Radius",  r_max_, 0);
		gd.addNumericField("Iteration",  iteration_, 0);
		gd.showDialog();
		
		if (gd.wasCanceled()) return false;
		r_max_ = (int)gd.getNextNumber();
		iteration_ = (int)gd.getNextNumber();
		
		r_min_ = r_max_;
		r_step_ = 1;
		quality_ = 10;
		ftype_ = "GAUSSIAN";
		threshold_ = 50;

		Prefs.set("iteration.int", iteration_);
		Prefs.set("r_max.int", r_max_);
		Prefs.set("r_min.int", r_min_);
		Prefs.set("r_step.int", r_step_);
		Prefs.set("quality.int", quality_);
		Prefs.set("th.int", threshold_);
		
		if(ftype_ == "GAUSSIAN")  filtertype = 0;
		else if(ftype_ == "MEAN") filtertype = 1;
		Prefs.set("ftype.int", filtertype);
		
		return true;
	}
		
	public void run(ImageProcessor ip) {
		if (!showDialog()) return;

		if(quality_ < 1) return;
		if(r_max_ < 1) return;
		if(r_min_ > r_max_) return;
		if(r_step_ < 1) return;
		
		int[] dims = imp_.getDimensions();
		int imageW = dims[0];
		int imageH = dims[1];
		int nCh    = dims[2];
		int imageD = dims[3];
		int nFrame = dims[4];
		int bdepth = imp_.getBitDepth();

		ImagePlus newimp = IJ.createHyperStack("Connected_"+imp_.getTitle(), imageW, imageH, nCh, imageD, nFrame, 8);
		ImageStack istack = imp_.getStack();
		ImageStack ostack = newimp.getStack();

		CLContext context = JavaCL.createBestContext();
        CLQueue queue = context.createDefaultQueue();
        ByteOrder byteOrder = context.getByteOrder();
        int imagesize = imageW*imageH*imageD;
		IJ.log("====");
        IJ.log("Platform: " + context.getPlatform().getName());
        IJ.log("Device: " + context.getDevices()[0].getName());
        IJ.log("MaxDim: " + context.getDevices()[0].getMaxWorkItemDimensions());
        IJ.log("MaxW: " + context.getDevices()[0].getImage3DMaxWidth());
        IJ.log("MaxH: " + context.getDevices()[0].getImage3DMaxHeight());
        IJ.log("MaxD: " + context.getDevices()[0].getImage3DMaxDepth());
        IJ.log("W: " + imageW);
        IJ.log("H: " + imageH);
        IJ.log("D: " + imageD);
        long maxalsize = context.getDevices()[0].getGlobalMemSize();
        long alsize = (long)imagesize*3*4;
        double memrate = (double)alsize / (double)maxalsize * 100.0;
        IJ.log("MaxMemAllocSize: "+String.format("%,d", maxalsize)+" byte");
        IJ.log("MemAllocSize: "+String.format("%,d", alsize)+" byte  ("+String.format("%.2f", memrate)+"%)");

		if (alsize > maxalsize) {
			IJ.log("Insufficient GPU memory!");
			return;
		}
        
		FloatBuffer in_buf  = FloatBuffer.allocate(imagesize);
		CLBuffer<Float> out_cl_buf = context.createFloatBuffer(Usage.InputOutput, imagesize);
		CLBuffer<Integer> n_cl_buf = context.createIntBuffer(Usage.InputOutput, imagesize);
		CLImageFormat imformat = new CLImageFormat(ChannelOrder.R, ChannelDataType.Float);

		String cl_code_fname = IJ.getDirectory("plugins") + File.separator + "dslt.cl";
		//IJ.log(cl_code_fname);
		String cl_code = new String();
		try {
			cl_code = FileUtils.readFileToString(new File(cl_code_fname));
			//cl_code = IOUtils.readText(DSLT3D_CL.class.getResource("dslt.cl"));
		} catch(IOException e) {
			e.printStackTrace();
		}
	    CLProgram program = context.createProgram(cl_code);
	    CLKernel dsltKernel = program.createKernel("dslt_max2");
	    CLKernel dsltL2Kernel = program.createKernel("dslt_l2_2");
	    CLKernel dsltBKernel = program.createKernel("dslt_binarize");
	    CLKernel dsltEMKernel = program.createKernel("dslt_elem_min");

	    double cdim = 1.0;
	    if (bdepth == 8)  cdim = 255.0;
	    if (bdepth == 16) cdim = 65535.0;

	    final int rd = quality_;
	    final double a_interval = (Math.PI / 2.0) / (double)rd;
		final double[] slatitable  = new double [rd*2*(rd*2-1)+1];
		final double[] clatitable  = new double [rd*2*(rd*2-1)+1];
		final double[] slongitable = new double [rd*2*(rd*2-1)+1];
		final double[] clongitable = new double [rd*2*(rd*2-1)+1];
		final double[] sintable = new double [rd*2];
		final double[] costable = new double [rd*2];
		final int knum = rd*2*(rd*2-1)+1;

		Pointer<Float> sctptr = Pointer.allocateFloats((rd*2*(rd*2-1)+1)*4).order(byteOrder);

		slatitable[0] = 0.0; clatitable[0] = 1.0; slongitable[0] = 0.0; clongitable[0] = 0.0;
		for(int b = 0; b < rd*2; b++){
			for(int a = 1; a < rd*2; a++){
				int id = b*(rd*2-1) + (a-1) + 1;
				slatitable[id] = Math.sin(a_interval*a);
				clatitable[id] = Math.cos(a_interval*a);
				slongitable[id] = Math.sin(a_interval*b);
				clongitable[id] = Math.cos(a_interval*b);

				sctptr.set(4*id,   (float)slatitable[id]);
				sctptr.set(4*id+1, (float)clatitable[id]);
				sctptr.set(4*id+2, (float)slongitable[id]);
				sctptr.set(4*id+3, (float)clongitable[id]);
			}
		}
		CLBuffer<Float> cl_sctable = context.createFloatBuffer(Usage.Input, sctptr);
		
		for(int a = 0; a < rd*2; a++){
			sintable[a] = Math.sin(a_interval*a);
			costable[a] = Math.cos(a_interval*a);
		}
		
		for(int f = 0; f < nFrame; f++){
			for(int ch = 0; ch < nCh; ch++){
				for(int ite = 0; ite < iteration_; ite++){
					ImageProcessor[] iplist = new ImageProcessor[imageD];
					ImageProcessor[] oplist = new ImageProcessor[imageD];
					for(int s = 0; s < imageD; s++){
						iplist[s] =  ite == 0 ? istack.getProcessor(imp_.getStackIndex(ch+1, s+1, f+1)) : ostack.getProcessor(newimp.getStackIndex(ch+1, s+1, f+1));
						oplist[s] =  ostack.getProcessor(newimp.getStackIndex(ch+1, s+1, f+1));
					}
					
					for(int z = 0; z < imageD; z++)
						for(int i = 0; i < imageW*imageH; i++)
							in_buf.put(iplist[z].getf(i));

					CLImage3D in_tex = context.createImage3D(Usage.InputOutput, imformat,
															 imageW, imageH, imageD,
															 0, 0,
															 in_buf, true);

					Pointer<Float> pattern_max = pointerToFloats(Float.MAX_VALUE);
					Pointer<Float> pattern_zero = pointerToFloats(0.0f);
					CLEvent dsltEvt = out_cl_buf.fillBuffer(queue, pattern_zero);
					queue.finish();

					long start, end;
					for(int r = r_max_; r >= r_min_ && r > 0; r = (r != r_min_ && r-r_step_ < r_min_) ? r_min_ : r-r_step_){
						final double[] filter = (filtertype == 1) ? mean1D(r) : gaussian1D(r);
						if(filter == null) continue;
					
						Pointer<Float> fptr = Pointer.allocateFloats(r+1).order(byteOrder);
						for (int i = 0; i <= r; i++)
							fptr.set(i, (float)filter[r+i]);

						CLBuffer<Float> cl_filter = context.createFloatBuffer(Usage.Input, fptr);

						start = System.nanoTime();

						for(int n = 0; n < knum; n++){
							final float drx = (float)(slatitable[n]*clongitable[n]);
							final float dry = (float)(slatitable[n]*slongitable[n]);
							final float drz = (float)clatitable[n];

							dsltKernel.setArgs(in_tex, out_cl_buf, n_cl_buf, cl_filter, r, n, drx, dry, drz, imageW, imageH*imageW);
							dsltEvt = dsltKernel.enqueueNDRange(queue, new int[]{ imageW, imageH, imageD  }, dsltEvt);
							queue.finish();
						}
	
						end = System.nanoTime();
						IJ.log("r: "+r+"  time: "+((float)(end-start)/1000000.0)+"msec");
	
						cl_filter.release();
						fptr.release();
					}

					Pointer<Float> outPtr = out_cl_buf.read(queue, dsltEvt);
					queue.finish();
					in_buf.position(0);
					for(int idx = 0; idx < imagesize; idx++)
						 in_buf.put(outPtr.get(idx));
					dsltEvt = in_tex.write(queue, 0L, 0L, 0L, (long)imageW, (long)imageH, (long)imageD, (long)imageW*4L, (long)imageH*(long)imageW*4L, in_buf, false, dsltEvt);
					queue.finish();
					dsltL2Kernel.setArgs(in_tex, out_cl_buf, n_cl_buf, cl_sctable, r_max_, imageW, imageH, imageD, imageW, imageH*imageW);
					dsltEvt = dsltL2Kernel.enqueueNDRange(queue, new int[]{ imageW, imageH, imageD  }, dsltEvt);
					queue.finish();
				
					outPtr = out_cl_buf.read(queue, dsltEvt);
					float maxval = 0.0f;
					for(int idx = 0; idx < imagesize; idx++)
						 maxval = (maxval < outPtr.get(idx)) ? outPtr.get(idx) : maxval;
					for(int z = 0; z < imageD; z++) {
						for(int i = 0; i < imageW*imageH; i++) {
							float val = outPtr.get(z*imageW*imageH+i);
							int rslt = (val/maxval*255.0 > threshold_ || iplist[z].getf(i) > 0) ? 255 : 0;
							oplist[z].setf(i, rslt);
						}
					}
	
					in_buf.position(0);
				}
			}
		}
		
		newimp.show();

	} //public void run(ImageProcessor ip) {

	private Thread[] newThreadArray() {  
		int n_cpus = Runtime.getRuntime().availableProcessors();  
		if (n_cpus > thread_num_) n_cpus = thread_num_;
		if (n_cpus <= 0) n_cpus = 1;
		return new Thread[n_cpus]; 
	}  
  
    /* Start all given threads and wait on each of them until all are done. 
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

	public double[] gaussian1D(int r){
		if(r < 1)return null;
		int ksize = 2*r + 1;
		double[] filter = new double[ksize];
		double sigma = 0.3*(ksize/2 - 1) + 0.8;
		double denominator = 2.0*sigma*sigma;
		double sum;
		double xx, d;
		int x;
		
		sum = 0.0;
		for(x = 0; x < ksize; x++){
			xx = x - (ksize - 1)/2;
			d = xx*xx;
			filter[x] = Math.exp(-1.0*d/denominator);
			sum += filter[x];
		}
	
		for(x = 0; x < ksize; x++)filter[x] /= sum;
		
		return filter;
	}
	
	public double[] mean1D(int r){
		if(r < 1)return null;
		int ksize = 2*r + 1;
		double[] filter = new double[ksize];
		
		for(int x = 0; x < ksize; x++)filter[x] = 1.0/(double)ksize;

		return filter;
	}
	

	
}
