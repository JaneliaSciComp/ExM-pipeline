
run("Misc...", "divide=Infinity save");
List.clear();
setBatchMode(true);


testArg=0;

//testArg="/Users/otsunah/test/josh_ExM_Brick_mask_connection/Josh_bigMask,/Users/otsunah/test/josh_ExM_Brick_mask_connection/out,11";
//testArg="C:\\Josh_Big\\mask\\22t_30t_substack,C:\\Josh_Big\\5500_22t_30t_substack\\out_before_connected_22t_30t_substack,23";
//testArg="A:\\Guest\\Josh\\ch1_3t_10vx_15t_substack_crop,A:\\Guest\\Josh\\out_connected,15,400,true";

//testArg="H:\\josh_exm\\22t_small.zip,H:\\josh_exm\\out,15932,8316,8518,10,15,29";


    slicecreationON=1;
    deleteON=1;
	renameON=1;
   deleteBrick=1;

if(testArg!=0)
args = split(testArg,",");
else
args = split(getArgument(),",");

input2Ddir = args[0];// small mask.tif
outputdir = args[1];// out dir
CPUnum= round(args[2]);

openz=400;


print("input2Ddir; "+input2Ddir);
print("outputdir; "+outputdir);


outlist=getFileList(outputdir);
Array.sort(outlist);

tiffnum=0; brickfolderExt=0;
for(iout=0; iout<outlist.length; iout++){
	if (endsWith(outlist[iout],".tif"))
	tiffnum=tiffnum+1;
	
	if( lastIndexOf(outlist[iout],"small_bricks")!=-1){
		createbrick=false;
		brickfolderExt=1;
	}
}

list=getFileList(input2Ddir);
Array.sort(list);
oriz=0;
for(i=0; i<list.length; i++){
	
	if(endsWith(list[i],".tif"))
	oriz=oriz+1;
}

if(oriz==tiffnum){
	print("TIFF files are already exist");
	run("Quit");
}

if(tiffnum==0 && brickfolderExt==0)
createbrick=true;
	
print("createbrick; "+createbrick);

outEXT=File.exists(outputdir);

if(outEXT!=1){
  File.makeDirectory(outputdir);
  
}


outnum=1;

repeatnum=floor(oriz/(openz+round(openz*0.025)));
amari=oriz/(openz+round(openz*0.025));

print("  oriz; "+oriz+"  repeatnum; "+repeatnum+"  amari; "+amari);

start=getTime();

if(createbrick==true){
  
  realOpen=1;
  startslice=1;
  for(iz=0; iz<=repeatnum; iz++){
    
    print("number; "+iz);
    
    openumber=openz+round(openz*0.05);//420
    overlapnum=round(openz*0.05);//20
    
    if(iz==0){
      openumber=openz+round(openz*0.025);//410
      overlapnum=round(openz*0.025);
    }
    if(iz==repeatnum){
      openumber=oriz-startslice;
      overlapnum=round(openz*0.025);
    }
		
		ADDST="/";
		if(endsWith(input2Ddir,"/"))
		ADDST="";
		
		if(realOpen==1){
			run("ImageSequence loader multithread", "start="+startslice+" open="+openumber+" cpu=12 serial="+input2Ddir+ADDST+"");
 //     run("Image Sequence...", "open="+input2Ddir+" number="+openumber+" starting="+startslice+" sort");
      
      brickdir=outputdir+"/small_bricks"+iz+"/";
      File.makeDirectory(brickdir);
      orix=getWidth();
      oriy=getHeight();
    }
    print("startslice; "+startslice+"  end; "+startslice+openumber);
    startslice=startslice+openumber-round(openz*0.025);
    
    if(realOpen==1){
      run("Brick creator3D", "x=800 y=800 z="+openumber+" zis overlap_vol="+overlapnum+" cpu="+CPUnum+" save="+brickdir+"");
      run("Close All");
    }
  }
}else{
  
  for(iz=0; iz<=repeatnum; iz++){
    
    brickdir=outputdir+"/small_bricks"+iz+"/";
    
    print("brickdir; "+brickdir);
    
    pram=File.openAsString(brickdir+"xyz.txt");
    paramST=split(pram,"\n");
    orix=round(paramST[0]);
    oriy=round(paramST[1]);
    oriz=round(paramST[2]);
    
    
    preoutputdir=outputdir+"/preout"+iz+"/";
		File.makeDirectory(preoutputdir);
		
		print("orix; "+orix+"  oriy; "+oriy+"  oriz; "+oriz+"   openz; "+openz+"  preoutputdir; "+preoutputdir);
  
    
    if(slicecreationON==1){//TIFFPackBits, ZIP
      if(iz!=repeatnum)//oriz-round(openz*0.05)
			run("Stack3D build from brick", "x="+orix+" y="+oriy+" z="+oriz+" graythreshold="+10+" in=TIFFPackBits xtimes=1 cpu="+CPUnum+" brick=["+brickdir+"] save="+preoutputdir);
      else
			run("Stack3D build from brick", "x="+orix+" y="+oriy+" z="+oriz+" graythreshold="+10+" in=TIFFPackBits xtimes=1 cpu="+CPUnum+" brick=["+brickdir+"] save="+preoutputdir);
    }
    
    preoutlist=getFileList(preoutputdir);
    deleteslices=round((openz*0.025)/2);
    

    
    if(deleteON==1){
      print("deleteslices; "+deleteslices);
      
      if(iz!=0 && iz!=repeatnum){
				for(idel=0; idel<deleteslices; idel++){
					exist=File.exists(preoutputdir+preoutlist[idel]);
					if(exist!=1)
					print("File not exist; "+preoutputdir+preoutlist[idel]);
					
					File.delete(preoutputdir+preoutlist[idel]);
					filedeletion (preoutputdir+preoutlist[idel]);
					wait(50);
        }
        
				for(idel2=preoutlist.length-1; idel2>preoutlist.length-deleteslices-1; idel2--){
					exist=File.exists(preoutputdir+preoutlist[idel2]);
					if(exist!=1)
					print("File not exist; "+preoutputdir+preoutlist[idel2]);
					
					File.delete(preoutputdir+preoutlist[idel2]);
					
					filedeletion (preoutputdir+preoutlist[idel2]);
					wait(50);
        }
      }
      if(iz==0){
        for(idel2=preoutlist.length-1; idel2>preoutlist.length-deleteslices-1; idel2--){
					File.delete(preoutputdir+preoutlist[idel2]);
					filedeletion (preoutputdir+preoutlist[idel2]);
					wait(50);
        }
      }
      
			if(iz==repeatnum){
			
				for(idel=0; idel<deleteslices-1; idel++){
					exist=File.exists(preoutputdir+preoutlist[idel]);
					if(exist!=1)
					print("iz==repeatnum, files"+preoutputdir+preoutlist[idel]);
					File.delete(preoutputdir+preoutlist[idel]);
					filedeletion (preoutputdir+preoutlist[idel]);
					wait(50);
      	}
			}
    }//if(deleteON==1){
    
    preoutlist=getFileList(preoutputdir);
    
    print("preoutlist.length; "+preoutlist.length);
    for(imov=0; imov<preoutlist.length; imov++){
      
      numST="";
      if(outnum<10000){
        if(outnum>999)
        numST="0";
        if(outnum<1000 && outnum>99)
        numST="00";
        else if(outnum<100 && outnum>9)
        numST="000";
        else if(outnum<10)
        numST="0000";
      }
      
      if(renameON==1)
      File.rename(preoutputdir+preoutlist[imov], outputdir+"/"+numST+outnum+".tif"); // - Renames, or moves, a file or directory. Returns "1" (true) if successful. 
      outnum=outnum+1;
    }//for(imov=0; imov<preoutlist.length; imov++){
    
 
    
    if(deleteBrick==1){
      bricklist=getFileList(brickdir);
      
      for(ibri=0; ibri<bricklist.length; ibri++){
        File.delete(brickdir+bricklist[ibri]);
      }
      File.delete(brickdir);
      File.delete(preoutputdir);
    }
  }
}

end=getTime();

print(round((end-start)/1000)+" sec");

print("Done macro");

//run("Quit");


function filedeletion (path){
	logsum=getInfo("log");
	zeroindex=lastIndexOf(logsum,"0");
	oneindex=lastIndexOf(logsum,"1");
	
	
	while(zeroindex>oneindex){
		File.delete(path);
		logsum=getInfo("log");
		zeroindex=lastIndexOf(logsum,"0");
		oneindex=lastIndexOf(logsum,"1");
	}
	
}
run("Misc...", "divide=Infinity save");
run("Quit");