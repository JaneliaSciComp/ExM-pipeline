
run("Misc...", "divide=Infinity save");
List.clear();
setBatchMode(true);


testArg=0;

//testArg="TIFFPackBits_8bit,10,5,15,/Users/otsunah/test/josh_cropping/tiff,/Users/otsunah/test/josh_cropping/cropped,//Users/otsunah/test/josh_cropping/ROI/P1_5520-5994.roi";

if(testArg!=0)
args = split(testArg,",");
else
args = split(getArgument(),",");


print(getDir("plugins"));
saveformat = args[0];//"ZIP", "uncompressedTIFF","TIFFPackBits_8bit","LZW"
CPUnum = round(args[1]);// 
startslice = round(args[2]);
endslice = round(args[3]);
inputdir = args[4];//no / at last
outputdir = args[5];//no / at last
ROIfullfilepath = args[6];//no / at last

print("saveformat; "+saveformat+"   CPUnum; "+CPUnum+"   startslice; "+startslice+"   endslice; "+endslice);
print("inputdir; "+inputdir);
print("outputdi; "+outputdir);
print("ROIfullfilepath; "+ROIfullfilepath);

startslice=startslice+1;
runROI=1;

if(File.exists(ROIfullfilepath))
open(ROIfullfilepath);
else{
	print ("There is no input ROI file; "+ROIfullfilepath);
	runROI=0;
}
if(File.exists(inputdir)!=1){
	print("There is no input Dir; "+inputdir);
	runROI=0;
}
outEXE=File.exists(outputdir);
if(outEXE!=1)
File.makeDirectory(outputdir);

if(runROI==1)
run("ROI Crop multithread", "output="+saveformat+" cpu="+CPUnum+" start="+startslice+" end="+endslice+" serial="+inputdir+" save="+outputdir+"");
run("Quit");
