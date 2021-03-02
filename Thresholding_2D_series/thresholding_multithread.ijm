
run("Misc...", "divide=Infinity save");
List.clear();
setBatchMode(true);


testArg=0;

//testArg="12,/Guest/josh/tes,/Guest/josh";

//testArg="H:\\josh_exm\\22t_small.zip,H:\\josh_exm\\out,15932,8316,8518,10,15,29";

if(testArg!=0)
args = split(testArg,",");
else
args = split(getArgument(),",");


print(getDir("plugins"));
CPUnum = round(args[0]);// 
inputdir = args[1];//no / at last
outputdir= args[2];//no / at last
threval=parseFloat(args[3]);

outEXE=File.exists(outputdir);
if(outEXE!=1)
File.makeDirectory(outputdir);

run("thresholding multithread", "cpu="+CPUnum+" thresholding="+threval+" serial="+inputdir+" save="+outputdir+"");
run("Quit");
