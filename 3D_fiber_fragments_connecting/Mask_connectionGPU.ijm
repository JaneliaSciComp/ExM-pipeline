
run("Misc...", "divide=Infinity save");
List.clear();
setBatchMode(true);

testArg=0;

//testArg="/Guest/josh/22t_small.zip,/Guest/josh/smooth_packbits,15932,8316,8518,10,11";

//testArg="H:\\josh_exm\\22t_small.zip,H:\\josh_exm\\out,15932,8316,8518,10,15,29";

if(testArg!=0)
args = split(testArg,",");
else
args = split(getArgument(),",");

input = args[0];// small mask.tif
connectionVX = args[1];
connectiontime = args[2];

dotIndexend = lastIndexOf(input, ".tif");
if(dotIndexend==-1)
dotIndexend = lastIndexOf(input, ".zip");

print("input; "+input);
print("connectionVX; "+connectionVX);
print("connectiontime; "+connectiontime);


if(dotIndexend!=-1){	
	open(input);

	input_title = getTitle();
	print(input_title);
	
	success=0; failnum=300;

	mingaptime=(8000/400)*nSlices;

	start=getTime();
	run("Connect Flagments", "radius="+connectionVX+" iteration="+connectiontime);
		
	end=getTime();
	gap=end-start;
	
	print("duration: "+gap/1000+" sec"+"   "+input);

	pathindex=lastIndexOf(input,"/");
	parentsdir=substring(input,0,pathindex);
	
	origidir=substring(input,0,pathindex+1);
	
	dotindex= lastIndexOf(input,".");
	filename=substring(input,pathindex+1,dotindex);
	
	pathindex=lastIndexOf(parentsdir,"/");
	parentsdir=substring(parentsdir,0,pathindex+1);
	
	print("parentsdir; "+parentsdir);

	output_title = getTitle();
	
	if(input_title != output_title){
		saveAs("ZIP", input);
	}else{
		File.saveString("fail", origidir+filename+"_fail.txt"); //- Saves string as a file. 
	}
	run("Close All");
}


run("Misc...", "divide=Infinity save");

run("Quit");



